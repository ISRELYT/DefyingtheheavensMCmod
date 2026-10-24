package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The lifespan tribulation (the user's ask, 2026-10-20): a spent life (Lifespan) isn't simply ended. The heavens come to collect it,
 * a storm over the cultivator that grows for {@link #FULL_POWER_TICKS} (five minutes) to its full power, its strikes ever faster
 * (one every 3 s at first, ten a second at the last) and ever heavier (a share of the target's max health: half a percent at
 * first, two fifths at the last), its storm and bolts swelling to {@link #FULL_GROWTH} times their size. Nothing bears it at full
 * power. Its strikes pass armour, enchantments, effects, wards and totems alike (ModDamageTypes#OLD_AGE, and {@link #isStriking}
 * for the mod's own wards); healing holds it off for a while, and a cultivator struck by it can go on meditating.
 * <ul>
 *   <li><b>Where</b>: everywhere, the Netherworld too, but the <b>Spatial Gap</b>: there the heavens lose sight of one, and it waits,
 *   as strong as it was, until one comes out. Logging out, it waits the same (saved on the record, PlayerCultivation#lifespanTrial).</li>
 *   <li><b>Its end</b>: years again. A breakthrough into a new realm (Grand Perfection into the next realm's Early: a stage's gives
 *   none), or years taken from the grateful (the Axis of Longevity's wishes) end it; no Longevity Pill may be taken under it
 *   (PillItem: a pill's few years would end it, to begin again from nothing). A breakthrough's own tribulation strikes on top of it.</li>
 *   <li><b>Its death</b>: any death while it runs, by its strike or anything else, ends the life whole: a mortal of 16 again,
 *   born at one's own spawn point (the record is made anew at the respawn: {@link #onRespawn}).</li>
 *   <li><b>The dead</b> face the Netherworld's version (yin lightning from a dark teal storm); struck down, or scattered by anything
 *   else, the soul scatters into the Soul River and is born again (Lifespan#soulScatters). One reborn at the Wheel mid-way is still
 *   spent: the heavens take over at the same power.</li>
 *   <li><b>The physiques</b>: a Heaven-Defying Body dies to it (no heart is left it here); a Heavenly Thunder Body drinks its strikes
 *   whole up to half power (no harm, its pool full, cultivation besides), less and less after, and nothing at the last; an Ancient
 *   Holy Body and Rings of Transcendence soften it as they do any tribulation; a Primordial Chaos Body is struck half as often again.</li>
 *   <li><b>A soul out of its body</b> (Soul Projection, Possession): the strikes fall on the body where it sits, and its death is the
 *   whole life's. None goes out under it, and one whose body is held has it back: the soul in it is driven out.</li>
 * </ul>
 * The age stops counting at the span's end (Lifespan#tick): any years won later are won in full. Announced to the whole server.
 */
public final class LifespanTribulation {
	/** Its full power, this long after it began (ticks of it: it doesn't grow while it waits). */
	public static final int FULL_POWER_TICKS = 6000;
	/** Its storm gathers this long before the first strike, and again whenever it finds its cultivator anew. */
	public static final int GATHER_TICKS = 60;
	/** Between strikes: 3 s at first, a tenth of a second at full power (ticks). */
	public static final int FIRST_GAP = 60, LAST_GAP = 2;
	/** A strike's share of its target's max health: half a percent at first, two fifths at full power. */
	public static final float FIRST_SHARE = 0.005f, LAST_SHARE = 0.40f;
	/** At full power its storm and its bolts are this many times their size as it gathered. */
	public static final float FULL_GROWTH = 3.0f;
	/** A Heavenly Thunder Body drinks the strikes whole up to this power, less and less after it, nothing at full power. */
	public static final double THUNDER_WHOLE_TO = 0.5;
	/** A strike drunk whole gives a Heavenly Thunder Body this share of its stage's cultivation (and a quarter of the strike healed). */
	public static final double THUNDER_CULTIVATION = 0.01;

	/** One running. Its power is saved on the record each second, so it waits through a logout as strong as it was. */
	private static final class Run {
		int elapsed;
		int nextStrike = GATHER_TICKS;
		int sinceFlash = TribulationManager.FLASH_EVERY;
		boolean paused;
		/** The Netherworld's version: its cultivator is one of the dead. */
		boolean ghost;
		TribulationCloud cloud;

		void removeCloud() {
			if (cloud != null) cloud.discard();
			cloud = null;
		}
	}

	private static final Map<UUID, Run> RUNNING = new HashMap<>();
	/** The dead struck down under it, to scatter at the next tick (not from inside the blow: the scattering makes a new player). */
	private static final java.util.Set<UUID> SCATTERING = new java.util.HashSet<>();
	/** The target its strike is landing on this moment (a cultivator, or the body a soul left), or null. */
	private static UUID striking;

	/** Whether {@code player}'s lifespan tribulation is running (online). */
	public static boolean isRunning(UUID player) {
		return RUNNING.containsKey(player);
	}

	/** Whether {@code entity} is taking one of its strikes this moment: past every ward and blow-softener of the mod's (SoulLivingEntityMixin). */
	public static boolean isStriking(Entity entity) {
		return striking != null && entity != null && striking.equals(entity.getUUID());
	}

	/** Its power for {@code elapsed} ticks of it, 0-1. */
	public static double power(int elapsed) {
		return Math.min(1.0, Math.max(0, elapsed) / (double) FULL_POWER_TICKS);
	}

	/** The time to its next strike at {@code power} (ticks); a Primordial Chaos Body ({@code hated}) is struck half as often again. */
	public static int gap(double power, boolean hated) {
		double gap = FIRST_GAP - (FIRST_GAP - LAST_GAP) * Math.pow(power, 1.5);
		if (hated) gap /= TribulationManager.CHAOS_STRIKES;
		return Math.max(LAST_GAP, (int) Math.round(gap));
	}

	/** A strike's share of its target's max health at {@code power}. */
	public static double share(double power) {
		return FIRST_SHARE + (LAST_SHARE - FIRST_SHARE) * power * power * power;
	}

	/** How many times its first size its storm and bolts are at {@code power}. */
	public static float growth(double power) {
		return (float) (1.0 + (FULL_GROWTH - 1.0) * power);
	}

	/** What share of a strike a Heavenly Thunder Body drinks at {@code power} (no harm from it): whole to half power, none at full. */
	public static double drunk(double power) {
		return Math.max(0, Math.min(1, (1.0 - power) / (1.0 - THUNDER_WHOLE_TO)));
	}

	// --- Beginning and ending (Lifespan#check) ---

	/** {@code p}'s lifespan is spent: the heavens come (or, coming back to a world it waited in, find them again). */
	static void begin(ServerPlayer p) {
		if (RUNNING.containsKey(p.getUUID())) return;
		PlayerCultivation c = CultivationManager.own(p);
		int saved = c.lifespanTrial();
		Run run = new Run();
		run.elapsed = Math.max(0, saved);
		run.ghost = c.netherworld().ghost();
		RUNNING.put(p.getUUID(), run);
		c.setLifespanTrial(run.elapsed);
		CultivationManager.markDirty(p.server);
		// One whose body another soul holds (Possession): the heavens drive it out.
		ServerPlayer tenant = Possession.tenantOf(p.getUUID());
		if (tenant != null) {
			tenant.sendSystemMessage(Component.translatable(ModLang.MSG_POSSESS_HEAVENS, p.getDisplayName()).withStyle(ChatFormatting.DARK_RED));
			SoulProjection.letGo(tenant, null);
		}
		if (InnerRealm.isInside(p)) InnerRealm.leave(p);
		if (saved > 0) {
			p.sendSystemMessage(Component.translatable(ModLang.MSG_LIFE_TRIB_RESUMED).withStyle(ChatFormatting.DARK_RED));
		} else {
			p.sendSystemMessage(Component.translatable(run.ghost ? ModLang.MSG_LIFE_TRIB_START_DEAD : ModLang.MSG_LIFE_TRIB_START)
					.withStyle(run.ghost ? ChatFormatting.DARK_AQUA : ChatFormatting.DARK_RED));
			p.server.getPlayerList().broadcastSystemMessage(Component.translatable(ModLang.MSG_LIFE_TRIB_BROADCAST, p.getDisplayName())
					.withStyle(ChatFormatting.DARK_RED), false);
		}
		p.playNotifySound(SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.0f, 0.3f);
		send(p, run);
	}

	/** {@code p} has years again (a new realm's, the grateful's: Lifespan#check): the heavens withdraw. */
	static void end(ServerPlayer p) {
		Run run = RUNNING.remove(p.getUUID());
		PlayerCultivation c = CultivationManager.own(p);
		c.setLifespanTrial(-1);
		CultivationManager.markDirty(p.server);
		if (run == null) return;
		run.removeCloud();
		ModPackets.sendLifespanTribulation(p, false, 0, false, false);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_LIFE_TRIB_RENEWED).withStyle(ChatFormatting.GOLD));
		p.server.getPlayerList().broadcastSystemMessage(Component.translatable(ModLang.MSG_LIFE_TRIB_RENEWED_BROADCAST, p.getDisplayName())
				.withStyle(ChatFormatting.GOLD), false);
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 0.9f);
	}

	// --- Every tick ---

	public static void tick(MinecraftServer server) {
		if (!SCATTERING.isEmpty()) {
			for (UUID id : new ArrayList<>(SCATTERING)) {
				SCATTERING.remove(id);
				ServerPlayer p = server.getPlayerList().getPlayer(id);
				if (p != null && GhostSystem.isGhost(p)) Lifespan.soulScatters(p); // gone, it scatters as it comes back (onJoin)
			}
		}
		if (RUNNING.isEmpty()) return;
		for (Map.Entry<UUID, Run> entry : new ArrayList<>(RUNNING.entrySet())) {
			UUID id = entry.getKey();
			Run run = entry.getValue();
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			if (p == null) {
				forget(id);
				continue;
			}
			if (!p.isAlive()) continue; // its death is seen to (onDeath)
			// Creative and spectating players don't age (Lifespan#tick): it neither grows nor strikes for them (but for one whose body
			// is held, who only watches).
			if (p.isCreative() || p.isSpectator() && !Possession.isHeld(id)) continue;
			PlayerCultivation c = CultivationManager.own(p);
			boolean dead = c.netherworld().ghost();
			if (dead != run.ghost) { // born again at the Wheel (or dead) mid-way: the other storm takes over, at the same power
				run.ghost = dead;
				run.removeCloud();
			}
			LivingEntity body = SoulProjection.ownBodyEntity(p);
			LivingEntity target = body != null ? body : p;
			if (ModDimensions.isSpatialGap(target.level().dimension())) {
				if (!run.paused) {
					run.paused = true;
					run.removeCloud();
					p.sendSystemMessage(Component.translatable(ModLang.MSG_LIFE_TRIB_PAUSED).withStyle(ChatFormatting.GRAY));
					send(p, run);
				}
				continue;
			}
			if (run.paused) {
				run.paused = false;
				run.nextStrike = GATHER_TICKS;
				p.sendSystemMessage(Component.translatable(ModLang.MSG_LIFE_TRIB_RESUMED).withStyle(ChatFormatting.DARK_RED));
			}
			if (InnerRealm.isInside(p)) InnerRealm.leave(p); // no hiding in one's own Inner Realm from it
			if (run.elapsed < FULL_POWER_TICKS) run.elapsed++;
			if (p.tickCount % 20 == 0) {
				c.setLifespanTrial(run.elapsed);
				CultivationManager.markDirty(server);
			}
			run.sinceFlash++;
			updateCloud(p, target, run, c);
			if (--run.nextStrike <= 0) {
				strike(p, target, run, c);
				if (RUNNING.get(id) != run) continue; // its strike ended the life just now
				run.nextStrike = gap(power(run.elapsed), c.hasPhysique(Physique.PRIMORDIAL_CHAOS_BODY));
			}
			if (p.tickCount % 10 == 0) send(p, run);
		}
	}

	/** A strike: its bolt on {@code target} (the cultivator, or the body its soul left), and its share of the target's max health. */
	private static void strike(ServerPlayer p, LivingEntity target, Run run, PlayerCultivation c) {
		if (!(target.level() instanceof ServerLevel level)) return;
		double power = power(run.elapsed);
		TribulationLightning bolt = ModEntities.TRIBULATION_LIGHTNING.create(level);
		if (bolt != null) {
			bolt.moveTo(target.getX(), target.getY(), target.getZ());
			bolt.setCloudBaseY(run.cloud == null ? target.getY() + TribulationCloud.BASE_HEIGHT_ABOVE_PLAYER : run.cloud.getY());
			int rank = Math.max(0, TwoPaths.higherRank(c, false));
			Realm realm = Realm.byIndex(rank / Stage.values().length);
			bolt.setScale(TribulationManager.boltScale(realm, Stage.byIndex(rank % Stage.values().length)) * growth(power));
			bolt.setGhost(run.ghost);
			bolt.setVisualOnly(true);
			bolt.setDim(run.sinceFlash < TribulationManager.FLASH_EVERY);
			if (!bolt.isDim()) run.sinceFlash = 0;
			level.addFreshEntity(bolt);
		}
		if (run.cloud != null) run.cloud.flash();

		// Softened as any tribulation is (an Ancient Holy Body, Rings of Transcendence), never turned aside.
		float multiplier = RingOfTranscendenceItem.damageMultiplier(p) * (c.hasPhysique(Physique.ANCIENT_HOLY_BODY) ? TribulationManager.HOLY_STRIKES : 1.0f);
		double harm = target.getMaxHealth() * share(power) * multiplier;
		// A Heavenly Thunder Body drinks what it can of it: no harm from that, its pool full, cultivation besides.
		if (!run.ghost && c.hasPhysique(Physique.HEAVENLY_THUNDER_BODY)) {
			double drink = drunk(power);
			if (drink > 0) {
				c.setQi(c.maxQi());
				// Into the path drawn in: the Demon Path's bar with it drawn (the heaven path's always, before the sweep of 2026-10-23).
				if (TwoPaths.cultivatesDemon(c)) DemonPath.addCultivation(c, drink * THUNDER_CULTIVATION * DemonPath.required(DemonPath.rank(c)));
				else c.addCultivation(drink * THUNDER_CULTIVATION * c.cultivationRequired());
				target.heal((float) (harm * drink * 0.25));
				harm *= 1 - drink;
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.0, target.getZ(), 30, 0.5, 0.9, 0.5, 0.3);
				CultivationManager.sync(p);
			}
		}
		if (run.ghost) level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + 1.0, target.getZ(), 12, 0.4, 0.9, 0.4, 0.05);
		if (harm <= 0) return;
		striking = target.getUUID();
		try {
			target.invulnerableTime = 0;
			target.hurt(ModDamageTypes.oldAge(level), (float) harm);
		} finally {
			striking = null;
		}
	}

	/**
	 * The life taken whole at once: its strike's death on {@code p} itself, past every ward as its strikes are (the body {@code p}'s
	 * soul left slain under it: SoulProjection#onBodySlain). The strike landing on the body is put back after.
	 */
	public static void takeLife(ServerPlayer p) {
		UUID landing = striking;
		striking = p.getUUID();
		try {
			p.invulnerableTime = 0;
			p.hurt(ModDamageTypes.oldAge(p.level()), Float.MAX_VALUE);
		} finally {
			striking = landing;
		}
	}

	/** Its storm over {@code target}, following it, grown with its power. */
	private static void updateCloud(ServerPlayer p, LivingEntity target, Run run, PlayerCultivation c) {
		if (!(target.level() instanceof ServerLevel level)) return;
		if (run.cloud == null || run.cloud.isRemoved() || run.cloud.level() != level) {
			run.removeCloud();
			run.cloud = ModEntities.TRIBULATION_CLOUD.create(level);
			if (run.cloud == null) return;
			int rank = Math.max(0, TwoPaths.higherRank(c, false));
			run.cloud.configure(Realm.byIndex(rank / Stage.values().length), run.ghost);
			run.cloud.setGrowth(growth(power(run.elapsed)));
			run.cloud.setPos(target.getX(), run.cloud.cloudBaseY(target.getY()), target.getZ());
			level.addFreshEntity(run.cloud);
		}
		run.cloud.setGrowth(growth(power(run.elapsed)));
		run.cloud.follow(target.position());
	}

	private static void send(ServerPlayer p, Run run) {
		ModPackets.sendLifespanTribulation(p, true, (float) power(run.elapsed), run.paused, run.ghost);
	}

	// --- Deaths ---

	/**
	 * ALLOW_DEATH, before anything else may hold a death off: its own strike's killing blow can't be (a phoenix's rebirth, a
	 * Heaven-Defying Body's last heart: none of it here); one of the dead struck down by anything while it runs scatters into the Soul
	 * River and is born again, the life ended whole (Lifespan#soulScatters). @return the answer, or null to leave it to the rest
	 */
	public static Boolean allowDeath(ServerPlayer p, DamageSource source) {
		if (!RUNNING.containsKey(p.getUUID())) return null;
		if (GhostSystem.isGhost(p)) {
			// Held at a breath till the next tick, when it scatters (marked on the record: leaving first doesn't save it, onJoin).
			Run run = RUNNING.remove(p.getUUID());
			run.removeCloud();
			ModPackets.sendLifespanTribulation(p, false, 0, false, false);
			PlayerCultivation c = CultivationManager.own(p);
			c.setLifeEnded(true);
			c.setLifespanTrial(-1);
			CultivationManager.markDirty(p.server);
			p.setHealth(1.0f);
			SCATTERING.add(p.getUUID());
			return false;
		}
		return source.is(ModDamageTypes.OLD_AGE) ? Boolean.TRUE : null;
	}

	/**
	 * AFTER_DEATH, first: a death while it ran, however it came, ends the life whole. Marked on the record (saved), and the record is
	 * made anew at the respawn ({@link #onRespawn}); not bound for the Netherworld. A breakthrough's tribulation under way goes with it
	 * unremarked (no stage to fall from in a life that is over). @return whether the life is over
	 */
	public static boolean onDeath(ServerPlayer p) {
		Run run = RUNNING.remove(p.getUUID());
		if (run == null) return false;
		run.removeCloud();
		ModPackets.sendLifespanTribulation(p, false, 0, false, false);
		PlayerCultivation c = CultivationManager.own(p);
		c.setLifeEnded(true);
		c.setLifespanTrial(-1);
		if (TribulationManager.isActive(p.getUUID())) {
			TribulationManager.forget(p.getUUID());
			ModPackets.sendTribulation(p, false, 0, Realm.QI_REFINING, Stage.EARLY, false);
		}
		CultivationManager.markDirty(p.server);
		p.server.overworld().getDataStorage().save(); // not even closing the game this instant keeps the life
		return true;
	}

	/** AFTER_RESPAWN: born again after a spent life's death: the record made anew, a mortal of 16 (Lifespan#wipe). */
	public static void onRespawn(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.own(p);
		if (!c.lifeEnded() || c.netherworld().ghost()) return;
		Lifespan.wipe(p, false);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_LIFESPAN_END).withStyle(ChatFormatting.DARK_RED));
	}

	/**
	 * JOIN: one marked as ended by a spent life who is somehow still about (a crash between the death and the respawn, one of the dead
	 * gone before it could scatter) is born anew now.
	 */
	public static void onJoin(ServerPlayer p) {
		if (!p.isAlive() || !CultivationManager.own(p).lifeEnded()) return;
		if (GhostSystem.isGhost(p)) Lifespan.soulScatters(p);
		else onRespawn(p);
	}

	// --- Bookkeeping ---

	/** Logged out, or its life wiped: its storm goes (the record keeps its power). */
	public static void forget(UUID id) {
		Run run = RUNNING.remove(id);
		if (run != null) run.removeCloud();
	}

	public static void clear() {
		RUNNING.values().forEach(Run::removeCloud);
		RUNNING.clear();
		SCATTERING.clear();
	}

	private LifespanTribulation() {}
}
