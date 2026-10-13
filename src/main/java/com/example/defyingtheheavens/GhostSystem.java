package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * The dead (see the README's "The Netherworld"). While the gamerule netherworldAfterDeath is on, a player who dies wakes at the
 * Ghost Gate as a ghost: the vanilla death goes ahead (its message, its drops, every hook), only the respawn is the Gate's (a
 * forced respawn point standing in for the player's own, which is set aside and put back, see {@link #onDeath}). A living
 * visitor overtaken by the Netherworld's yin becomes one where they stand, with everything they carry ({@link #overtakenByYin}).
 * <p>
 * A ghost walks the Netherworld with its gravity and walls, but takes no fall, drowning, burning, suffocation, freezing or
 * hunger, makes no footsteps, and is drawn as cyan light. It has half its living hearts. Nothing touches it but the dead and
 * the living who can see it ({@link #allowDamage}); at no health it disperses in cyan sparks and reforms at the Gate five
 * seconds later with half its health and half its ghostly qi ({@link #disperse}). It can't break or place, use or drop or pick
 * up, ride, take a rift or portal, meditate on the main track, break through, fly on qi or a sword, press with its realm, turn
 * inward or project its soul (Incorporeal; the systems' own checks). The Wheel of Reincarnation returns it to life at its own
 * respawn point, as vanilla would choose it ({@link #useWheel}); in hardcore the Wheel refuses.
 * <p>
 * A creative ghost keeps the look and none of the rules.
 */
public final class GhostSystem {
	/** Set by the client: whether it has been told a player is a ghost. */
	public static Predicate<Player> clientGhost = player -> false;

	private static final UUID GHOST_HEALTH_ID = UUID.fromString("5b1e0c3a-7d4f-4c8e-9a61-2f0d8e3b6c71");
	/** A dispersed ghost reforms at the Gate this long after. */
	public static final int DISPERSE_TICKS = 5 * 20;
	/** The Wheel turns, and the screen fades to gold, over this long before the ghost wakes alive. */
	public static final int WHEEL_TICKS = 40;
	/** A ghost's hearts: this share of its living maximum, and one heart more for each layer of Netherworld Breathing Art. */
	public static final double HEALTH_SHARE = 0.5;
	public static final double HEALTH_PER_BREATHING_LAYER = 2.0;
	/** In the Netherworld a ghost mends this much health a second. */
	public static final float REGEN_PER_SECOND = 1.0f;
	/** A ghost's blow on the living: this much harm, and this much of the Netherworld's yin driven into them. */
	public static final float GHOSTLY_BLOW = 1.0f;

	private static final Map<UUID, Integer> DISPERSED = new HashMap<>();
	private static final Map<UUID, Integer> TURNING = new HashMap<>();
	private static final Set<UUID> REVIVING = new HashSet<>();

	// --- Who is a ghost ---

	/** Both sides: on the server, the saved state; on a client, what it has been told (ClientGhosts). */
	public static boolean isGhost(Player player) {
		if (player == null) return false;
		if (player.level().isClientSide) return clientGhost.test(player);
		return player instanceof ServerPlayer sp && CultivationManager.get(sp).netherworld().ghost();
	}

	private static NetherworldState state(ServerPlayer p) {
		return CultivationManager.get(p).netherworld();
	}

	public static boolean afterDeathOn(ServerPlayer p) {
		return p.server.getGameRules().getBoolean(ModGameRules.NETHERWORLD_AFTER_DEATH);
	}

	// --- Death and the respawn ---

	/**
	 * AFTER_DEATH: bound for the Netherworld. The Gate stands in as a forced respawn point, so the respawn goes straight there
	 * with no flash of the bed or the world spawn; the player's own point is set aside (saved, so a crash can't lose it) and put
	 * back the moment they respawn.
	 */
	public static void onDeath(ServerPlayer p) {
		if (!afterDeathOn(p) || state(p).ghost()) return;
		ServerLevel netherworld = p.server.getLevel(ModDimensions.NETHERWORLD);
		if (netherworld == null) return;
		NetherworldState s = state(p);
		s.setBoundBelow(true);
		s.setDeathPlace(p.level().dimension().location().toString(), p.blockPosition());
		if (s.savedRespawn() == null) s.setSavedRespawn(respawnTag(p));
		p.setRespawnPosition(ModDimensions.NETHERWORLD, GhostGate.at(netherworld, GhostGate.ARRIVAL), 0.0f, true, false);
		CultivationManager.markDirty(p.server);
	}

	/** AFTER_RESPAWN: a death's respawn wakes a ghost at the Gate; the Wheel's wakes the living. */
	public static void onRespawn(ServerPlayer p, boolean alive) {
		NetherworldState s = state(p);
		if (!alive && s.boundBelow()) {
			restoreRespawn(p, s);
			s.setBoundBelow(false);
			if (!ModDimensions.isNetherworld(p.level().dimension())) toGate(p); // the Gate couldn't be had: go there now
			become(p);
			p.sendSystemMessage(Component.translatable(ModLang.MSG_GHOST_WAKE).withStyle(ChatFormatting.AQUA));
			p.playNotifySound(SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, 0.6f);
		} else if (REVIVING.remove(p.getUUID())) {
			finishRevival(p, s);
		} else if (s.ghost()) {
			applyBody(p); // any other respawn (the End's exit, say) keeps a ghost a ghost
			broadcast(p);
		}
	}

	private static CompoundTag respawnTag(ServerPlayer p) {
		CompoundTag tag = new CompoundTag();
		BlockPos pos = p.getRespawnPosition();
		tag.putBoolean("Has", pos != null);
		if (pos != null) {
			tag.putString("Dimension", p.getRespawnDimension().location().toString());
			tag.putInt("X", pos.getX());
			tag.putInt("Y", pos.getY());
			tag.putInt("Z", pos.getZ());
			tag.putFloat("Angle", p.getRespawnAngle());
			tag.putBoolean("Forced", p.isRespawnForced());
		}
		return tag;
	}

	/** Puts the player's own respawn point back (none, if they had none). */
	private static void restoreRespawn(ServerPlayer p, NetherworldState s) {
		CompoundTag tag = s.savedRespawn();
		s.setSavedRespawn(null);
		if (tag == null) return;
		if (!tag.getBoolean("Has")) {
			p.setRespawnPosition(Level.OVERWORLD, null, 0.0f, false, false);
			return;
		}
		ResourceLocation id = ResourceLocation.tryParse(tag.getString("Dimension"));
		ResourceKey<Level> dimension = id == null ? Level.OVERWORLD : ResourceKey.create(Registries.DIMENSION, id);
		p.setRespawnPosition(dimension, new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z")), tag.getFloat("Angle"),
				tag.getBoolean("Forced"), false);
	}

	// --- Becoming one of the dead ---

	/** Now a ghost: its hearts, its sync, its state saved. */
	static void become(ServerPlayer p) {
		NetherworldState s = state(p);
		s.setGhost(true);
		s.setReforming(false);
		s.setYinCorruption(0); // the yin has nothing left to take
		MeditationManager.stop(p, false);
		applyBody(p);
		p.setHealth(p.getMaxHealth());
		broadcast(p);
		NetherworldManager.refresh(p);
		CultivationManager.markDirty(p.server);
	}

	/**
	 * Yin corruption is full (see YinCorruption): the living body gives way and the visitor walks on as a ghost where they
	 * stand, keeping everything they carry. Off the gamerule, there are no ghosts: the realm casts them down the river instead.
	 */
	public static void overtakenByYin(ServerPlayer p) {
		if (state(p).ghost()) return;
		if (!afterDeathOn(p)) {
			state(p).setYinCorruption(0.99);
			SoulRiver.beginDescent(p, false);
			return;
		}
		state(p).setDeathPlace(p.level().dimension().location().toString(), p.blockPosition());
		sparks(p, 60);
		p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, 0.5f);
		become(p);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_YIN_OVERTAKEN).withStyle(ChatFormatting.DARK_AQUA));
	}

	// --- The ghost's body ---

	/** Its hearts (a transient modifier), and nothing of the living body's needs. */
	static void applyBody(ServerPlayer p) {
		updateHearts(p);
		p.getFoodData().setFoodLevel(20);
		p.getFoodData().setSaturation(5.0f);
		p.setAirSupply(p.getMaxAirSupply());
		p.clearFire();
		p.setTicksFrozen(0);
	}

	/** Keeps a ghost's maximum health at its share of the living one, whatever else changes the attribute. */
	private static void updateHearts(ServerPlayer p) {
		AttributeInstance health = p.getAttribute(Attributes.MAX_HEALTH);
		if (health == null) return;
		AttributeModifier current = health.getModifier(GHOST_HEALTH_ID);
		double living = health.getValue() - (current == null ? 0 : current.getAmount());
		double wanted = Math.max(2.0, living * HEALTH_SHARE + HEALTH_PER_BREATHING_LAYER * breathingLayers(p)) - living;
		if (current != null && Math.abs(current.getAmount() - wanted) < 1.0e-6) return;
		health.removeModifier(GHOST_HEALTH_ID);
		health.addTransientModifier(new AttributeModifier(GHOST_HEALTH_ID, "Ghost's health", wanted, AttributeModifier.Operation.ADDITION));
		if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
	}

	private static void removeBody(ServerPlayer p) {
		AttributeInstance health = p.getAttribute(Attributes.MAX_HEALTH);
		if (health != null) health.removeModifier(GHOST_HEALTH_ID);
	}

	/** Netherworld Breathing Art's layers: a heart more for each. */
	private static int breathingLayers(ServerPlayer p) {
		return GhostMethods.breathingLayers(CultivationManager.get(p));
	}

	public static void tick(net.minecraft.server.MinecraftServer server) {
		for (Iterator<Map.Entry<UUID, Integer>> it = DISPERSED.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Integer> entry = it.next();
			ServerPlayer p = server.getPlayerList().getPlayer(entry.getKey());
			if (p == null) {
				it.remove(); // logged out mid-dispersal: reforms on their return (the Reforming flag)
				continue;
			}
			p.setDeltaMovement(Vec3.ZERO);
			entry.setValue(entry.getValue() - 1);
			if (entry.getValue() <= 0) {
				it.remove();
				reform(p);
			}
		}
		for (Iterator<Map.Entry<UUID, Integer>> it = TURNING.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Integer> entry = it.next();
			ServerPlayer p = server.getPlayerList().getPlayer(entry.getKey());
			if (p == null || !isGhost(p)) {
				it.remove();
				continue;
			}
			entry.setValue(entry.getValue() - 1);
			if (entry.getValue() <= 0) {
				it.remove();
				REVIVING.add(p.getUUID());
				server.getPlayerList().respawn(p, true); // as vanilla would place them, everything they carry kept
			}
		}
		boolean second = server.getTickCount() % 20 == 0;
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (!isGhost(p)) continue;
			p.setAirSupply(p.getMaxAirSupply());
			p.clearFire();
			p.setTicksFrozen(0);
			p.fallDistance = 0;
			if (second) {
				applyBody(p);
				if (ModDimensions.isNetherworld(p.level().dimension()) && p.getHealth() < p.getMaxHealth() && !DISPERSED.containsKey(p.getUUID())) {
					p.heal(REGEN_PER_SECOND);
				}
				GhostGate.terrace(p); // atop the Terrace for Gazing Homeward: the place it died
			}
		}
	}

	// --- Harm ---

	/**
	 * ALLOW_DAMAGE for a ghost: only what can touch the dead does. Ghostly harm (the dead's blows, ghost fire, the Peach Wood
	 * Sword), another ghost's hand, the living who can see it (SoulProjection#harmFactor), and what touches anything (/kill, the
	 * Chief Judge). Lava, falls, arrows, blasts and ordinary mobs pass straight through.
	 */
	public static boolean allowDamage(ServerPlayer ghost, DamageSource source) {
		if (DISPERSED.containsKey(ghost.getUUID())) return false; // nothing left to strike
		if (ghost.isCreative()) return true; // a creative ghost: the look without the rules (vanilla's own then apply)
		return reaches(source);
	}

	/** The rule itself: whether {@code source} is something that reaches the dead at all. */
	public static boolean reaches(DamageSource source) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(ModDamageTypes.GHOSTLY)) return true;
		Entity attacker = source.getEntity();
		if (isDead(attacker) || peachWood(source)) return true;
		return attacker != null && source.getDirectEntity() == attacker && SoulProjection.harmFactor(attacker) > 0;
	}

	/** One of the dead: a player's ghost, or a ghostly mob or NPC ({@link Ghostly}). */
	public static boolean isDead(Entity entity) {
		return entity instanceof Ghostly || entity instanceof Player player && isGhost(player);
	}

	/** A Peach Wood Sword in hand, struck with: on the dead it always lands, and hard ({@link PeachWoodSwordItem#ON_THE_DEAD}). */
	public static boolean peachWood(DamageSource source) {
		Entity attacker = source.getEntity();
		return attacker instanceof net.minecraft.world.entity.LivingEntity living && source.getDirectEntity() == attacker
				&& living.getMainHandItem().is(ModItems.PEACH_WOOD_SWORD);
	}

	/** How much of a blow on a ghost lands: all of a ghostly one, the living's by how clearly they see it. */
	public static float scaleDamage(ServerPlayer ghost, DamageSource source, float amount) {
		return ghost.isCreative() ? amount : scaled(source, amount);
	}

	/** The rule itself: how much of {@code amount} from {@code source} lands on one of the dead. */
	public static float scaled(DamageSource source, float amount) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(ModDamageTypes.GHOSTLY)) return amount;
		if (peachWood(source)) return amount * PeachWoodSwordItem.ON_THE_DEAD;
		Entity attacker = source.getEntity();
		if (attacker == null || isDead(attacker)) return amount;
		return amount * SoulProjection.harmFactor(attacker);
	}

	/**
	 * A ghost strikes: other ghosts, and the dead's own kind among mobs and NPCs, as anyone would; the living take a ghostly blow
	 * ({@link #GHOSTLY_BLOW}) and a share of the Netherworld's yin; anything else its hand passes through. Returns false to
	 * cancel the ordinary attack.
	 */
	public static boolean attack(ServerPlayer ghost, Entity target) {
		if (isDead(target)) return true;
		if (target instanceof ServerPlayer living && YinCorruption.affects(living)) {
			living.hurt(ModDamageTypes.ghostly(ghost.level(), ghost), GHOSTLY_BLOW);
			Spells.onMeleeHit(ghost, living); // Ghost Fire on its hands sets them burning
			NetherworldState s = CultivationManager.get(living).netherworld();
			s.setYinCorruption(s.yinCorruption() + YinCorruption.GHOSTLY_BLOW);
			NetherworldManager.refresh(living);
		}
		return false;
	}

	/** ALLOW_DEATH for a ghost: it doesn't die, it disperses. */
	public static boolean allowDeath(ServerPlayer p) {
		if (!isGhost(p)) return true;
		disperse(p);
		return false;
	}

	/** Out of health, or into the abyss: scattered in cyan sparks, the screen gone dark; it reforms at the Gate. */
	public static void disperse(ServerPlayer p) {
		p.setHealth(1.0f);
		if (DISPERSED.containsKey(p.getUUID())) return;
		sparks(p, 80);
		p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ALLAY_DEATH, SoundSource.PLAYERS, 1.0f, 0.5f);
		p.setInvisible(true);
		p.setInvulnerable(true);
		state(p).setReforming(true);
		CultivationManager.markDirty(p.server);
		DISPERSED.put(p.getUUID(), DISPERSE_TICKS);
		ModPackets.sendGhostFade(p, 0x062428, 10, DISPERSE_TICKS - 5, 25);
		p.displayClientMessage(Component.translatable(ModLang.MSG_GHOST_DISPERSED), true);
	}

	/** Back together at the Ghost Gate, with half its health and half its ghostly qi. */
	static void reform(ServerPlayer p) {
		NetherworldState s = state(p);
		s.setReforming(false);
		p.setInvisible(false);
		p.setInvulnerable(false);
		toGate(p);
		applyBody(p);
		p.setHealth(p.getMaxHealth() / 2);
		s.setGhostQi(s.ghostQi() / 2);
		CultivationManager.markDirty(p.server);
		NetherworldManager.refresh(p);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_GHOST_REFORMED).withStyle(ChatFormatting.AQUA));
	}

	private static void sparks(ServerPlayer p, int count) {
		if (p.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 1, p.getZ(), count, 0.4, 0.8, 0.4, 0.08);
			level.sendParticles(ParticleTypes.GLOW, p.getX(), p.getY() + 1, p.getZ(), count / 2, 0.5, 0.9, 0.5, 0.15);
		}
	}

	/** To the Ghost Gate's plaza. */
	static void toGate(ServerPlayer p) {
		ServerLevel netherworld = p.server.getLevel(ModDimensions.NETHERWORLD);
		if (netherworld == null) return;
		BlockPos at = GhostGate.at(netherworld, GhostGate.ARRIVAL);
		p.teleportTo(netherworld, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0f, 0.0f);
		SoulRiver.settle(p);
	}

	// --- The Wheel of Reincarnation ---

	/** Someone used the Wheel's hub at {@code pos}: a ghost is returned to life; to the living it is only a wheel. */
	public static void useWheel(ServerPlayer p, BlockPos pos) {
		if (!isGhost(p)) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_WHEEL_LIVING), true);
			return;
		}
		if (p.server.isHardcore()) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_WHEEL_HARDCORE), true);
			return;
		}
		if (TURNING.containsKey(p.getUUID()) || DISPERSED.containsKey(p.getUUID())) return;
		TURNING.put(p.getUUID(), WHEEL_TICKS);
		ServerLevel level = p.serverLevel();
		level.blockEvent(pos, level.getBlockState(pos).getBlock(), WheelOfReincarnationBlock.EVENT_TURN, WHEEL_TICKS);
		level.playSound(null, pos, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 1.5f, 0.8f);
		level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.4f);
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 0.6f);
		level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 60, 2.5, 2.5, 0.6, 0.05);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 1, p.getZ(), 40, 0.5, 1.0, 0.5, 0.03);
		ModPackets.sendGhostFade(p, 0xFFF1C8, WHEEL_TICKS, 15, 35);
		p.displayClientMessage(Component.translatable(ModLang.MSG_WHEEL_TURNS), true);
	}

	/** Alive again, at full health; the ghost's state and hearts gone; Meng Po's soup honoured. Cultivation and ghostly qi kept. */
	private static void finishRevival(ServerPlayer p, NetherworldState s) {
		s.setGhost(false);
		s.setReforming(false);
		removeBody(p);
		p.setInvisible(false);
		p.setInvulnerable(false);
		RealmSuppressionHandler.update(p);
		p.setHealth(p.getMaxHealth());
		p.getFoodData().setFoodLevel(20);
		if (s.memoryCleansed()) {
			CultivationManager.get(p).setPillResistance(0);
			s.setMemoryCleansed(false);
		}
		broadcast(p);
		NetherworldManager.refresh(p);
		CultivationManager.markDirty(p.server);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_REVIVED).withStyle(ChatFormatting.GOLD));
		p.playNotifySound(SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.6f, 1.3f);
	}

	/** Back to life at once, as at the Wheel (commands, tests). */
	public static void revive(ServerPlayer p) {
		if (!isGhost(p)) return;
		REVIVING.add(p.getUUID());
		p.server.getPlayerList().respawn(p, true);
	}

	/** A ghost at once, where they stand, things kept (commands, tests). */
	public static void makeGhost(ServerPlayer p) {
		if (isGhost(p)) return;
		state(p).setDeathPlace(p.level().dimension().location().toString(), p.blockPosition());
		become(p);
	}

	// --- Joining, and telling clients ---

	/** A ghost logging in wakes a ghost (reforming at the Gate if it left while dispersed). */
	public static void onJoin(ServerPlayer p) {
		NetherworldState s = state(p);
		if (!s.ghost()) return;
		if (s.reforming()) reform(p);
		applyBody(p);
		broadcast(p);
	}

	/** Tells the player and everyone tracking them whether they are a ghost (and the player what they can cast as one, or not). */
	public static void broadcast(ServerPlayer p) {
		ModPackets.broadcastGhost(p, state(p).ghost());
		Spells.sync(p);
	}

	public static void forget(UUID id) {
		DISPERSED.remove(id);
		TURNING.remove(id);
	}

	public static void clear() {
		DISPERSED.clear();
		TURNING.clear();
		REVIVING.clear();
	}

	private GhostSystem() {}
}
