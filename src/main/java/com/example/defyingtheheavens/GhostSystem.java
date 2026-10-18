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
import net.minecraft.world.entity.LivingEntity;
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
 * respawn point, as vanilla would choose it ({@link #useWheel}), leaving behind at the Wheel anything it still carried (a
 * visitor overtaken by yin: {@link #leaveBelongings}); in hardcore the Wheel refuses.
 * <p>
 * A <b>Ghost King</b> ({@link #isKing}) is one of the dead at Core Formation or above: one who died there, or whose ghost path
 * rose to it (GhostMethods#ghostRank, the Ghost Core Canon; never a mortal's ghost, whose meridians are closed). Its hands are its own again: it breaks and places, uses and
 * picks up, sets off plates, takes the rifts, the Soul River and portals down to the living world, and strikes as the living do
 * (Incorporeal#is leaves it out). It stays one of the dead in all else: drawn as their light (crowned), harmed only as they
 * are, hunted by no mob, passed through by arrows, cultivating only its method; outside the Netherworld only those who see
 * souls see it (SoulProjection#broadcastsTo).
 * <p>
 * How a player is drawn is told apart from what it is (TwoPaths#look): the dead are always drawn as the dead (a king crowned),
 * whatever their paths; one of the living whose ghost path has reached Core Formation has a human form and a Ghost King's, and
 * shows its true form or the other. In a Ghost King's form the living are seen as the dead are (outside the Netherworld, only by
 * those who perceive souls), and keep every other rule of the living.
 * <p>
 * A creative ghost keeps the look and none of the rules.
 */
public final class GhostSystem {
	/** Set by the client: whether it has been told a player is a ghost. */
	public static Predicate<Player> clientGhost = player -> false;
	/** Set by the client: whether it has been told a ghost is a Ghost King. */
	public static Predicate<Player> clientKing = player -> false;
	/** One of the dead this strong is a Ghost King: Core Formation Early. */
	public static final int KING_RANK = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY);

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

	/** A Ghost King (see the class notes): both sides, as {@link #isGhost}. */
	public static boolean isKing(Player player) {
		if (player == null) return false;
		if (player.level().isClientSide) return clientKing.test(player);
		return player instanceof ServerPlayer sp && isKing(CultivationManager.get(sp));
	}

	/**
	 * One of the dead whose true cultivation, or whose ghost path (TwoPaths#ghostRank: a mortal's lies dormant), is Core Formation
	 * or above.
	 */
	public static boolean isKing(PlayerCultivation c) {
		if (!c.netherworld().ghost()) return false;
		if (c.physique() == Physique.GHOST_DEVIL_BODY && !c.isMortal()) return true; // a Ghost King from birth (Physique)
		return TwoPaths.heavenRank(c, false) >= KING_RANK || TwoPaths.ghostRank(c, false) >= KING_RANK;
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
		// A Ghost King's forms start from its true one, the King's: a Form Shift left on from life would show it human at once.
		CultivationManager.get(p).setAbilityEnabled(Ability.FORM_SHIFT, false);
		applyBody(p);
		p.setHealth(p.getMaxHealth());
		broadcast(p);
		NetherworldManager.refresh(p);
		CultivationManager.markDirty(p.server);
	}

	/**
	 * Yin corruption is full (see YinCorruption): the living body gives way and the visitor walks on as a ghost where they
	 * stand, keeping everything they carry, until the Wheel returns them to life without it ({@link #leaveBelongings}). Off the
	 * gamerule, there are no ghosts: the realm casts them down the river instead.
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

	/** The foundations' layers (the Netherworld Breathing Art's and those after it): a heart more for each. */
	private static int breathingLayers(ServerPlayer p) {
		return GhostMethods.foundationLayers(CultivationManager.get(p));
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
				reincarnate(p);
			}
		}
		boolean second = server.getTickCount() % 20 == 0;
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (second) {
				watchKing(p);
				TwoPaths.watch(p); // the forms, and the spells of both paths
			}
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

	/** The Ghost Kings among the online players, as they were last told (a king's crown and hands come and go with its realm). */
	private static final Set<UUID> KINGS = new HashSet<>();

	/**
	 * Once a second: a ghost risen to Core Formation (or one that died there) is told it is a Ghost King, and its clients told too;
	 * one stripped of its might (the Chief Judge's gaze) is told it is one of the common dead again. Born again, it simply isn't.
	 */
	private static void watchKing(ServerPlayer p) {
		boolean king = isKing(p);
		if (king == KINGS.contains(p.getUUID())) return;
		if (king) {
			KINGS.add(p.getUUID());
			p.sendSystemMessage(Component.translatable(ModLang.MSG_GHOST_KING).withStyle(ChatFormatting.LIGHT_PURPLE));
			p.playNotifySound(SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, 0.5f);
			p.playNotifySound(SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 0.6f);
			sparks(p, 60);
			Journal.note(p, "ghost_king", ModLang.JOURNAL_GHOST_KING, Component.translatable(ModLang.JOURNAL_GHOST_KING_TEXT));
		} else {
			KINGS.remove(p.getUUID());
			if (isGhost(p)) p.sendSystemMessage(Component.translatable(ModLang.MSG_GHOST_KING_LOST).withStyle(ChatFormatting.GRAY));
		}
		broadcast(p);
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

	/**
	 * A player the living world's mobs and NPCs don't see, and the dead's own kind do (Hungry Ghosts, ghost cultivators,
	 * {@link #deadMayAttack}): one of the dead in earnest, or one of the living in a Ghost King's form (TwoPaths#inGhostForm).
	 * Server side (a client's mobs choose no targets).
	 */
	public static boolean seenAsDead(Player player) {
		if (!(player instanceof ServerPlayer sp)) return false;
		return Incorporeal.isGhost(sp) || TwoPaths.inGhostForm(sp);
	}

	/**
	 * Whether one of the dead's kind ({@code attacker}, a Ghostly mob or NPC) may take {@code target} for prey: the dead, and the
	 * living in a Ghost King's form, as anyone else may be taken (vanilla asks Player#canBeSeenAsEnemy, which the living world's
	 * mobs are answered no for these: SoulPlayerMixin); null to leave it to vanilla.
	 */
	public static Boolean deadMayAttack(LivingEntity attacker, LivingEntity target) {
		if (!(target instanceof Player player) || !seenAsDead(player)) return null;
		return attacker.level().getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL && !player.getAbilities().invulnerable && player.canBeSeenByAnyone();
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
		if (isDead(target) || isKing(ghost)) return true; // a Ghost King strikes as the living do
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

	/**
	 * Out of health, or into the abyss: scattered in cyan sparks, the screen gone dark; it reforms at the Gate. Those that fought
	 * it let it go (CultivatorNpc#letGoOf): the scattering is the end of that fight.
	 */
	public static void disperse(ServerPlayer p) {
		p.setHealth(1.0f);
		if (DISPERSED.containsKey(p.getUUID())) return;
		sparks(p, 80);
		p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ALLAY_DEATH, SoundSource.PLAYERS, 1.0f, 0.5f);
		p.setInvisible(true);
		p.setInvulnerable(true);
		p.addTag(SCATTERED_TAG);
		state(p).setReforming(true);
		CultivationManager.markDirty(p.server);
		DISPERSED.put(p.getUUID(), DISPERSE_TICKS);
		// Whatever scattered it has done with it: the dead reform at the Gate, before its wardens, and a warden still set on a
		// ghost it had scattered cut it down again the moment it reformed, over and over.
		for (CultivatorNpc npc : CultivatorNpc.loaded(p.serverLevel())) {
			if (npc.getTarget() == p || npc.getLastHurtByMob() == p) npc.letGoOf(p);
		}
		for (net.minecraft.world.entity.Mob mob : p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, p.getBoundingBox().inflate(64),
				m -> m.getTarget() == p && !(m instanceof CultivatorNpc))) {
			mob.setTarget(null);
		}
		ModPackets.sendGhostFade(p, 0x062428, 10, DISPERSE_TICKS - 5, 25);
		p.displayClientMessage(Component.translatable(ModLang.MSG_GHOST_DISPERSED), true);
	}

	/** Back together at the Ghost Gate, with half its health and half its ghostly qi. */
	static void reform(ServerPlayer p) {
		NetherworldState s = state(p);
		s.setReforming(false);
		gatherBody(p);
		toGate(p);
		applyBody(p);
		p.setHealth(p.getMaxHealth() / 2);
		s.setGhostQi(s.ghostQi() / 2);
		CultivationManager.markDirty(p.server);
		NetherworldManager.refresh(p);
		p.displayClientMessage(Component.translatable(ModLang.MSG_GHOST_REFORMED).withStyle(ChatFormatting.AQUA), true);
	}

	/**
	 * Marks a scattered body (in its own saved data, the Tags), so that what the scattering did to it (unseen, untouchable) is
	 * undone whatever becomes of the ghost's record: vanilla saves the untouchable, and carries a singleplayer body over to a
	 * player of another name, whose records (by its UUID) are new (a development client's name is new each launch).
	 */
	static final String SCATTERED_TAG = "defying-the-heavens.scattered";

	/** Seen and touchable again, its scattering's mark gone. */
	private static void gatherBody(ServerPlayer p) {
		p.setInvisible(false);
		p.setInvulnerable(false);
		p.removeTag(SCATTERED_TAG);
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
		CultivationManager.get(p).setAbilityEnabled(Ability.FORM_SHIFT, false); // alive again in its true form (TwoPaths)
		removeBody(p);
		gatherBody(p);
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
		reincarnate(p);
	}

	/**
	 * The Wheel has turned: the ghost leaves behind what it carried ({@link #leaveBelongings}) and is born again where vanilla
	 * would respawn it (the respawn copies the rest across: cultivation, experience, its state; finishRevival does the waking).
	 * The respawn makes a new player; the connection is handed to it, as vanilla's own callers do (left on the old one, the
	 * server would go on moving and breaking for the ghost's removed body at the Gate while the client stood in the world).
	 */
	private static void reincarnate(ServerPlayer p) {
		leaveBelongings(p);
		REVIVING.add(p.getUUID());
		p.connection.player = p.server.getPlayerList().respawn(p, true);
	}

	/**
	 * Born again, a ghost brings nothing back from the Netherworld: everything it carries (its inventory, armour and rings) falls
	 * where it stands, as a death's things fall where it died, and what bears the Curse of Vanishing is gone. Only a visitor
	 * overtaken by yin still carries anything as a ghost (a death dropped its things already, and the dead pick nothing up), so
	 * the yin is no free way home with a full pack. With keepInventory on, as at a death, everything is kept; a creative ghost
	 * keeps the look and none of the rules.
	 */
	static void leaveBelongings(ServerPlayer p) {
		if (p.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY) || p.getAbilities().instabuild
				|| p.isSpectator()) return;
		net.minecraft.world.entity.player.Inventory inventory = p.getInventory();
		RingContainer rings = RingContainer.of(p);
		boolean carried = !inventory.isEmpty() || !rings.isEmpty();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (net.minecraft.world.item.enchantment.EnchantmentHelper.hasVanishingCurse(inventory.getItem(i))) inventory.removeItemNoUpdate(i);
		}
		inventory.dropAll();
		for (int i = 0; i < rings.getContainerSize(); i++) {
			net.minecraft.world.item.ItemStack ring = rings.removeItemNoUpdate(i);
			if (!ring.isEmpty() && !net.minecraft.world.item.enchantment.EnchantmentHelper.hasVanishingCurse(ring)) p.drop(ring, true, false);
		}
		if (carried) p.sendSystemMessage(Component.translatable(ModLang.MSG_BELONGINGS_LEFT).withStyle(ChatFormatting.GRAY));
	}

	/** A ghost at once, where they stand, things kept (commands, tests). */
	public static void makeGhost(ServerPlayer p) {
		if (isGhost(p)) return;
		state(p).setDeathPlace(p.level().dimension().location().toString(), p.blockPosition());
		become(p);
	}

	// --- Joining, and telling clients ---

	/**
	 * A ghost logging in wakes a ghost (reforming at the Gate if it left while dispersed). A body that left scattered with no
	 * reforming ghost's record to go with it (SCATTERED_TAG) is simply seen and touchable again.
	 */
	public static void onJoin(ServerPlayer p) {
		NetherworldState s = state(p);
		if (p.getTags().contains(SCATTERED_TAG) && !(s.ghost() && s.reforming())) gatherBody(p);
		TwoPaths.onJoin(p);
		if (!s.ghost()) {
			// One of the living in a Ghost King's form (TwoPaths) is drawn so, to itself too (its hand in first person).
			if (TwoPaths.look(CultivationManager.get(p)) != TwoPaths.Look.HUMAN) broadcast(p);
			return;
		}
		if (s.reforming()) reform(p);
		if (isKing(p)) KINGS.add(p.getUUID()); // a king already: no need to tell it again
		applyBody(p);
		broadcast(p);
	}

	/**
	 * Tells the player and everyone tracking them whether they are a ghost, and a Ghost King (and the player what they can cast
	 * as one, or not).
	 */
	public static void broadcast(ServerPlayer p) {
		ModPackets.broadcastGhost(p);
		Spells.sync(p);
	}

	public static void forget(UUID id) {
		DISPERSED.remove(id);
		TURNING.remove(id);
		KINGS.remove(id);
		TwoPaths.forget(id);
	}

	public static void clear() {
		DISPERSED.clear();
		TURNING.clear();
		REVIVING.clear();
		KINGS.clear();
		TwoPaths.clear();
	}

	private GhostSystem() {}
}
