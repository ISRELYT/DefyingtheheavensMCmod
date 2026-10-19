package com.example.defyingtheheavens;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Heavenly Tribulation: the trial a player must survive to cross into the next major realm,
 * and, from Nascent Soul on, into every minor stage.
 * The trial itself is runtime-only and never survives a server restart or a disconnect, but leaving is no way out: the
 * player's cultivation is marked (and saved at once) while a trial runs, and a player who returns still marked has failed
 * it (see {@link #onJoin}).
 * <p>
 * <b>The Netherworld's tribulation</b> (the user's ask, 2026-10-19) is the ghost path's own ({@link #startGhost}; GhostMethods' bar
 * at its bottleneck): the same strikes at the same breakthroughs, of yin lightning (ModDamageTypes#YIN_LIGHTNING and #YIN_TRIBULATION)
 * from a dark teal storm, cyan bolts. It comes wherever its cultivator stands, the Netherworld too (it is the dead's realm's, not the
 * heavens'), and it reaches the dead, each strike the same share of their hearts as of a living body's in the same armour (the common
 * dead, with no hands for armour, as if in iron: {@link #GHOST_FORM_ARMOR}). Failed, the ghost path falls a stage: the living die,
 * the dead scatter ({@link #onScattered}).
 */
public final class TribulationManager {
	private static final int FIRST_STRIKE_DELAY = 60;  // 3 s for the local cloud to gather before the first bolt
	private static final int STRIKE_INTERVAL = 60;     // 3 s between strikes
	private static final int FINISH_DELAY = 20;        // 1 s after the last strike before the breakthrough completes
	/** All a tribulation's strikes drunk in by a Heavenly Thunder Body (Physique): this share of the new stage's cultivation. */
	public static final double THUNDER_SHARE = 0.6;
	/**
	 * The armour one of the common dead (no hands for armour: Incorporeal#isRuledGhost) turns the Netherworld's tribulation's bolt with
	 * at least: a full iron set's, as the balance table asks of the living at Core Formation, the last tribulation one of them meets
	 * (a ghost path of Core Formation is a Ghost King's, whose hands are its own).
	 */
	public static final float GHOST_FORM_ARMOR = 15.0f;

	/** One tribulation: a set number of identical strikes, each heavenly lightning (armor applies) plus magic (it doesn't). */
	public record Strikes(int count, float bolt, float magic) {}

	private static final class Trial {
		final Realm targetRealm;
		final Stage targetStage;
		/** The ghost path's (the Netherworld's tribulation), not the heaven path's. */
		final boolean ghost;
		final Strikes strikes;
		int strikesLeft;
		int nextStrikeIn = FIRST_STRIKE_DELAY;
		int finishIn = FINISH_DELAY;
		int age;
		/** A Heavenly Thunder Body's share of the strikes drunk in (Physique): {@link #THUNDER_SHARE} of the new stage's cultivation at most. */
		double absorbed;
		TribulationCloud cloud;

		void removeCloud() {
			if (cloud != null) cloud.discard();
			cloud = null;
		}

		Trial(Realm targetRealm, Stage targetStage, boolean ghost) {
			this.targetRealm = targetRealm;
			this.targetStage = targetStage;
			this.ghost = ghost;
			this.strikes = strikes(targetRealm, targetStage);
			this.strikesLeft = strikes.count();
		}

		Component targetName() {
			return PlayerCultivation.rankName(targetRealm, targetStage);
		}
	}

	private static final Map<UUID, Trial> ACTIVE = new HashMap<>();

	public static boolean isActive(UUID id) {
		return ACTIVE.containsKey(id);
	}

	/** Whether {@code id} faces the Netherworld's tribulation (the ghost path's) right now. */
	public static boolean isGhostTrial(UUID id) {
		Trial t = ACTIVE.get(id);
		return t != null && t.ghost;
	}

	/*
	 * Balance. A set number of strikes lands 3 s apart, after 3 s of gathering clouds. From Foundation Building on, the Qi
	 * Sustenance ability (if left switched on) regenerates 6 health between strikes, so only damage above that wears the player down. Lightning is cut by armor
	 * (up to 80%); the magic part ignores armor but not Protection enchantments, Resistance or absorption.
	 * Every strike of a tribulation is the same, so the outcome is certain. Tuned (deterministic simulation, starting at full
	 * health, no potions/apples/totems) so a mortal survives with this full armor set and dies with the step below
	 * (netherite counts as diamond):
	 *   -> Foundation Building   1 strike    nothing needed at full health (16 lightning against 22.6 health)
	 *   -> Core Formation        3 strikes   iron (leather dies)
	 *   -> Nascent Soul          6 strikes   diamond, or iron with Protection II (plain iron dies) - also NS Mid
	 *   -> NS Late               6 strikes   diamond with Protection I, or iron with Protection II (plain diamond dies)
	 *   -> NS Grand Perfection   6 strikes   diamond with Protection II (Protection I dies)
	 *   -> Heavenly Being        9 strikes   Protection III (II dies) - also HB Mid and Late
	 *   -> HB Grand Perfection   9 strikes   Protection IV (III dies)
	 * Protection IV is the best gear there is, so Four Axis asks for a beacon on top of it:
	 *   -> Four Axis            12 strikes   Protection IV + Resistance I (Protection IV alone dies) - also FA Mid and Late
	 *   -> FA Grand Perfection  12 strikes   Protection IV + Resistance I + Regeneration I, or Resistance II (Resistance I dies)
	 * Golden apples, Resistance/Regeneration potions and totems let a player get through a step below these,
	 * and each worn Ring of Transcendence halves all strike damage on top of the table.
	 * The Netherworld's tribulation strikes as this table says for the living (its yin lightning turned by armour as the heavens'
	 * bolts are, the yin in it passing armour as magic does); the dead take the same share of their hearts (GhostSystem#livingShare).
	 */

	/** The tribulation for breaking through into this realm and stage. Before Nascent Soul only Early is ever a target. */
	public static Strikes strikes(Realm target, Stage stage) {
		return switch (target) {
			case QI_REFINING -> new Strikes(0, 0.0f, 0.0f); // never a breakthrough target
			case FOUNDATION_BUILDING -> new Strikes(1, 16.0f, 0.0f);
			case CORE_FORMATION -> new Strikes(3, 13.0f, 5.0f);
			case NASCENT_SOUL -> switch (stage) {
				case EARLY -> new Strikes(6, 18.0f, 7.5f);
				case MID -> new Strikes(6, 23.0f, 10.0f);
				case LATE -> new Strikes(6, 28.0f, 12.0f);
				case GRAND_PERFECTION -> new Strikes(6, 36.5f, 15.5f);
			};
			case HEAVENLY_BEING -> switch (stage) {
				case EARLY -> new Strikes(9, 23.5f, 19.0f);
				case MID -> new Strikes(9, 29.5f, 23.5f);
				case LATE -> new Strikes(9, 31.0f, 25.0f);
				case GRAND_PERFECTION -> new Strikes(9, 45.0f, 36.0f);
			};
			case FOUR_AXIS -> switch (stage) { // even at Grand Perfection, armor still reaches its 80% cap
				case EARLY -> new Strikes(12, 26.5f, 47.5f);
				case MID -> new Strikes(12, 34.5f, 62.0f);
				case LATE -> new Strikes(12, 36.5f, 66.0f);
				case GRAND_PERFECTION -> new Strikes(12, 44.5f, 80.5f);
			};
		};
	}

	/** Entry point from the Breakthrough button. */
	public static void start(ServerPlayer player) {
		if (!player.isAlive()) return; // e.g. a modified client sending the packet from the death screen
		if (isActive(player.getUUID())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_TRIB_BUSY), true);
			return;
		}
		if (ModDimensions.isInnerRealm(player.level().dimension())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_INNER_TRIBULATION), true);
			return;
		}
		// The heavens send their lightning only where they see: not into the realm of the dead.
		if (ModDimensions.isUnseenByHeaven(player.level().dimension())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_HEAVENS_BLIND), true);
			return;
		}
		// The heavens strike the cultivator, body and soul together: not a soul out of its body, nor one possessing another's.
		if (SoulProjection.isActive(player.getUUID())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_BUSY), true);
			return;
		}
		PlayerCultivation c = CultivationManager.get(player);
		if (c.isAtBottleneck() && c.isBreakthroughLocked()) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_REALM_LOCKED,
					PlayerCultivation.rankName(PlayerCultivation.LOWER_REALM_CAP_REALM, PlayerCultivation.LOWER_REALM_CAP_STAGE)), true);
			return;
		}
		// A new realm waits on a heaven method of it too (HeavenMethods#opensWay): said in chat, so a pill also wanting is said as well.
		boolean held = c.isHeldByMethod();
		if (held) player.sendSystemMessage(HeavenMethods.gateLine(c).copy().withStyle(net.minecraft.ChatFormatting.GOLD));
		if (c.isAtBottleneck() && c.isMissingBreakthroughPill()) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_NEED_PILL,
					PillItem.breakthroughPillFor(c.breakthroughRealm()).getDescription(), c.breakthroughRealm().getDisplayName()), true);
			return;
		}
		if (held) return;
		if (!c.canBreakthrough()) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_NOT_READY), true);
			return;
		}

		MeditationManager.stop(player, false); // no-op if not meditating
		Trial trial = new Trial(c.breakthroughRealm(), c.breakthroughStage(), false);
		ACTIVE.put(player.getUUID(), trial);
		// Written to disk now, not at the next autosave, so not even closing the game this instant gets out of the trial.
		c.setInTribulation(true);
		CultivationManager.markDirty(player.server);
		player.server.overworld().getDataStorage().save();

		updateCloud(player, trial);
		ModPackets.sendTribulation(player, true, trial.strikesLeft, trial.targetRealm, trial.targetStage, false);
		player.playNotifySound(SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.0f, 0.4f);
		player.sendSystemMessage(trial.strikesLeft == 1
				? Component.translatable(ModLang.MSG_TRIB_START_SINGLE, trial.targetName())
				: Component.translatable(ModLang.MSG_TRIB_START, trial.targetName(), trial.strikesLeft));
	}

	/**
	 * Entry point from the Breakthrough button with the ghost path drawn in (TwoPaths#cultivatesGhost): the Netherworld's
	 * tribulation, wherever its cultivator stands (the Netherworld too).
	 */
	public static void startGhost(ServerPlayer player) {
		if (!player.isAlive()) return;
		if (isActive(player.getUUID())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_TRIB_BUSY), true);
			return;
		}
		if (ModDimensions.isInnerRealm(player.level().dimension())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_INNER_TRIBULATION), true);
			return;
		}
		if (SoulProjection.isActive(player.getUUID())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_BUSY), true);
			return;
		}
		PlayerCultivation c = CultivationManager.get(player);
		if (GhostMethods.heldByMethod(c)) {
			player.sendSystemMessage(GhostMethods.gateLine(c).copy().withStyle(net.minecraft.ChatFormatting.GOLD));
			return;
		}
		if (!GhostMethods.canBreakthrough(c)) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_NOT_READY), true);
			return;
		}

		MeditationManager.stop(player, false);
		int target = GhostMethods.ghostRank(c.netherworld()) + 1;
		int stages = Stage.values().length;
		Trial trial = new Trial(Realm.byIndex(target / stages), Stage.byIndex(target % stages), true);
		ACTIVE.put(player.getUUID(), trial);
		c.netherworld().setInGhostTribulation(true); // saved at once, as the heaven path's mark is: leaving fails it
		CultivationManager.markDirty(player.server);
		player.server.overworld().getDataStorage().save();

		updateCloud(player, trial);
		ModPackets.sendTribulation(player, true, trial.strikesLeft, trial.targetRealm, trial.targetStage, true);
		player.playNotifySound(SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.0f, 0.3f);
		player.playNotifySound(SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.5f, 0.5f);
		player.sendSystemMessage((trial.strikesLeft == 1
				? Component.translatable(ModLang.MSG_GHOST_TRIB_START_SINGLE, trial.targetName())
				: Component.translatable(ModLang.MSG_GHOST_TRIB_START, trial.targetName(), trial.strikesLeft)).withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
	}

	public static void tick(MinecraftServer server) {
		if (ACTIVE.isEmpty()) return;
		List<ServerPlayer> survivors = new ArrayList<>();

		// Iterate a copy: a strike can kill the player, and the death handler removes the trial.
		for (Map.Entry<UUID, Trial> entry : new ArrayList<>(ACTIVE.entrySet())) {
			UUID id = entry.getKey();
			Trial t = entry.getValue();
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			if (p == null) {
				forget(id);
				continue;
			}
			if (!p.isAlive()) continue; // onDeath() handles failure

			t.age++;
			updateCloud(p, t);

			if (t.strikesLeft > 0) {
				if (--t.nextStrikeIn <= 0) {
					strike(p, t);
					if (ACTIVE.get(id) != t) continue; // died (or scattered) just now
					t.strikesLeft--;
					t.nextStrikeIn = STRIKE_INTERVAL;
					ModPackets.sendTribulation(p, true, t.strikesLeft, t.targetRealm, t.targetStage, t.ghost);
				}
			} else if (--t.finishIn <= 0) {
				survivors.add(p); // the last strike has faded
				continue;
			}

			if (t.age % 20 == 0) {
				ModPackets.sendTribulation(p, true, t.strikesLeft, t.targetRealm, t.targetStage, t.ghost);
			}
		}
		survivors.forEach(TribulationManager::succeed);
	}

	/**
	 * How much thicker a tribulation's bolt is drawn than vanilla's: as its cloud grows with the realm (TribulationCloud#stormScale,
	 * {@link TribulationCloud#SCALE_PER_TIER} a realm past Foundation Building), and a quarter of that more for each stage within
	 * the realm (the user's ask, 2026-10-19). Foundation Building x1, Core Formation x1.45, Nascent Soul Early x1.9 to Grand
	 * Perfection x2.24, Four Axis Grand Perfection x3.14. Only its thickness: its zig-zag stays vanilla's (the user's word, the
	 * same day, after the whole bolt scaled up sprawled across the sky).
	 */
	public static float boltScale(Realm target, Stage stage) {
		int tier = Math.max(0, target.ordinal() - Realm.FOUNDATION_BUILDING.ordinal());
		return 1.0f + TribulationCloud.SCALE_PER_TIER * (tier + stage.ordinal() / (float) Stage.values().length);
	}

	/**
	 * A strike: the one bolt the cloud sends, at the cultivator (the only lightning a tribulation has: the scattered bolts it once
	 * threw about for show were taken out at the user's word, 2026-10-19).
	 */
	private static void strike(ServerPlayer p, Trial t) {
		ServerLevel level = p.serverLevel();
		TribulationLightning bolt = ModEntities.TRIBULATION_LIGHTNING.create(level); // drawn bright blue (cyan: the Netherworld's) by the client
		if (bolt == null) return;
		bolt.moveTo(p.getX(), p.getY(), p.getZ());
		bolt.setCloudBaseY(t.cloud == null ? p.getY() + TribulationCloud.BASE_HEIGHT_ABOVE_PLAYER : t.cloud.getY());
		bolt.setScale(boltScale(t.targetRealm, t.targetStage));
		bolt.setGhost(t.ghost);
		bolt.setVisualOnly(true); // 1.20.1 bolts always deal a flat 5, so the tribulation's hit is applied by hand below
		level.addFreshEntity(bolt);
		if (t.cloud != null) t.cloud.flash();

		// A Heavenly Thunder Body drinks the heavens' bolt in: no harm, the qi pool filled, and a share of the new stage's cultivation
		// banked for the breakthrough. (The Netherworld's yin is no thunder of its.)
		PlayerCultivation c = CultivationManager.get(p);
		if (!t.ghost && c.physique() == Physique.HEAVENLY_THUNDER_BODY) {
			t.absorbed += 1.0 / Math.max(1, t.strikes.count());
			c.setQi(c.maxQi());
			p.heal(t.strikes.bolt() * 0.25f);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1.0, p.getZ(), 60, 0.5, 0.9, 0.5, 0.3);
			level.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 1.6f);
			CultivationManager.sync(p);
			return;
		}
		if (t.ghost) level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 1.0, p.getZ(), 30, 0.4, 0.9, 0.4, 0.05);

		// Both parts land in the same tick as the bolt. Clearing the hurt cooldown in between stops vanilla from
		// discarding the second hit; magic goes first so a killing strike usually reads "struck by lightning".
		// It is cleared before the first part too: within half a second of another hit, vanilla only deals what exceeds
		// that hit, so taking a big blow on purpose (an explosion against Blast Protection, say) would blunt the strike.
		// The dead take each strike as the same share of their hearts as the living take of theirs, in the same armour (the user's ask,
		// 2026-10-19): the bolt as their armour turns it, then the whole of it by their hearts against the living body's they are
		// reckoned from (GhostSystem#livingShare), so a ghost's hearts, more or fewer, make it no easier or harder than a life.
		boolean dead = GhostSystem.isGhost(p);
		float multiplier = RingOfTranscendenceItem.damageMultiplier(p);
		float share = dead ? (float) GhostSystem.livingShare(p) : 1.0f;
		float magic = t.strikes.magic() * multiplier * share;
		p.invulnerableTime = 0;
		if (magic > 0) {
			p.hurt(t.ghost ? ModDamageTypes.yinTribulation(level) : level.damageSources().magic(), magic);
			p.invulnerableTime = 0;
		}
		if (p.isAlive() && ACTIVE.get(p.getUUID()) == t) {
			float harm = t.strikes.bolt() * multiplier;
			if (dead) {
				// Turned by armour here, before the share (the turning is no straight share of the blow), and dealt past it: one of the
				// common dead, with no hands for armour, turns it as a ghostly form does, as an iron set would (GHOST_FORM_ARMOR).
				float armour = Incorporeal.isRuledGhost(p) ? Math.max(p.getArmorValue(), GHOST_FORM_ARMOR) : p.getArmorValue();
				harm = CombatRules.getDamageAfterAbsorb(harm, armour, (float) p.getAttributeValue(Attributes.ARMOR_TOUGHNESS)) * share;
				p.hurt(ModDamageTypes.yinTribulation(level), harm);
			} else {
				p.hurt(t.ghost ? ModDamageTypes.yinLightning(level) : level.damageSources().lightningBolt(), harm);
			}
		}
	}

	private static void succeed(ServerPlayer p) {
		Trial t = ACTIVE.remove(p.getUUID());
		if (t == null) return;

		t.removeCloud();
		ModPackets.sendTribulation(p, false, 0, t.targetRealm, t.targetStage, t.ghost);

		PlayerCultivation c = CultivationManager.get(p);
		if (t.ghost) {
			succeedGhost(p, c, t);
			return;
		}
		c.setInTribulation(false);
		CultivationManager.markDirty(p.server);
		boolean sameTarget = c.breakthroughRealm() == t.targetRealm && c.breakthroughStage() == t.targetStage;
		if (!sameTarget || !c.breakthrough()) { // state changed mid-trial (e.g. admin command)
			p.displayClientMessage(Component.translatable(ModLang.MSG_NOT_READY), true);
			return;
		}
		if (t.absorbed > 0) { // a Heavenly Thunder Body's banked lightning becomes cultivation in the new stage
			double gained = Math.min(1, t.absorbed) * THUNDER_SHARE * c.cultivationRequired();
			c.addCultivation(gained);
			p.displayClientMessage(Component.translatable(ModLang.MSG_THUNDER_ABSORBED, Math.round(gained)).withStyle(net.minecraft.ChatFormatting.AQUA), true);
		}
		CultivationManager.refresh(p);
		p.setHealth(p.getMaxHealth());
		p.clearFire();

		ServerLevel level = p.serverLevel();
		level.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
		level.playSound(null, p.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.6f, 1.5f);
		level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 80, 0.6, 1.0, 0.6, 0.15);
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.5, 0.8, 0.5, 0.4);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_BREAKTHROUGH, t.targetName()));
	}

	/** The Netherworld's tribulation survived: the ghost path on to the stage it was struck for. */
	private static void succeedGhost(ServerPlayer p, PlayerCultivation c, Trial t) {
		NetherworldState s = c.netherworld();
		s.setInGhostTribulation(false);
		CultivationManager.markDirty(p.server);
		int before = GhostMethods.ghostRank(s);
		boolean sameTarget = before + 1 == PlayerCultivation.rank(t.targetRealm, t.targetStage);
		if (!sameTarget || !GhostMethods.canBreakthrough(c) || !GhostMethods.breakthrough(s)) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_NOT_READY), true);
			return;
		}
		GhostMethods.afterRank(p, before);
		p.setHealth(p.getMaxHealth());
		p.clearFire();
		Spells.sync(p);
		NetherworldManager.refresh(p);

		ServerLevel level = p.serverLevel();
		level.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 0.8f);
		level.playSound(null, p.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.2f, 0.6f);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 1.0, p.getZ(), 80, 0.6, 1.0, 0.6, 0.1);
		level.sendParticles(ParticleTypes.GLOW, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.5, 0.8, 0.5, 0.2);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_GHOST_BREAKTHROUGH, t.targetName()).withStyle(net.minecraft.ChatFormatting.AQUA));
	}

	/** Called from the AFTER_DEATH event (totems of undying prevent that event, so they save the player). */
	public static void onDeath(ServerPlayer p) {
		Trial t = ACTIVE.remove(p.getUUID());
		if (t == null) return;
		fail(p, t, false);
		// The respawn event re-applies the lowered stats and re-syncs the client.
	}

	/** One of the dead scattered (GhostSystem#disperse): mid-way through the Netherworld's tribulation, it has failed it. */
	public static void onScattered(ServerPlayer p) {
		Trial t = ACTIVE.get(p.getUUID());
		if (t == null || !t.ghost) return;
		ACTIVE.remove(p.getUUID());
		fail(p, t, false);
		CultivationManager.refresh(p);
	}

	/** A trial failed (struck down, or fled: {@code fled}): its path falls one stage, its cultivation there lost. */
	private static void fail(ServerPlayer p, Trial t, boolean fled) {
		PlayerCultivation c = CultivationManager.get(p);
		t.removeCloud();
		ModPackets.sendTribulation(p, false, 0, t.targetRealm, t.targetStage, t.ghost);
		p.server.getPlayerList().broadcastSystemMessage(Component.translatable(ModLang.MSG_TRIB_FAILED, p.getDisplayName()), false);
		if (t.ghost) {
			NetherworldState s = c.netherworld();
			s.setInGhostTribulation(false);
			GhostMethods.fallOneStage(s);
			CultivationManager.markDirty(p.server);
			p.sendSystemMessage(Component.translatable(fled ? ModLang.MSG_GHOST_TRIB_FLED : ModLang.MSG_GHOST_TRIB_FALL, GhostMethods.rankName(GhostMethods.ghostRank(s))));
			Spells.sync(p);
			return;
		}
		c.setInTribulation(false);
		c.fallOneStage();
		CultivationManager.markDirty(p.server);
		p.sendSystemMessage(Component.translatable(fled ? ModLang.MSG_TRIB_FLED : ModLang.MSG_TRIB_FALL, PlayerCultivation.rankName(c.getRealm(), c.getStage())));
	}

	/**
	 * Disconnect: the running trial and its cloud go. The player's cultivation stays marked as mid-tribulation, so they fail
	 * it when they return ({@link #onJoin}).
	 */
	public static void forget(UUID id) {
		Trial trial = ACTIVE.remove(id);
		if (trial != null) trial.removeCloud();
	}

	/**
	 * A player has joined. One who left mid-tribulation (logged out, quit, or the game closed) has failed it, just as if
	 * struck down: they fall one minor stage and lose their cultivation (the ghost path's, for the Netherworld's).
	 *
	 * @return true if they fell
	 */
	public static boolean onJoin(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		if (isActive(p.getUUID())) return false;
		boolean fell = false;
		if (c.isInTribulation()) {
			c.setInTribulation(false);
			c.fallOneStage();
			p.server.getPlayerList().broadcastSystemMessage(Component.translatable(ModLang.MSG_TRIB_FAILED, p.getDisplayName()), false);
			p.sendSystemMessage(Component.translatable(ModLang.MSG_TRIB_FLED, PlayerCultivation.rankName(c.getRealm(), c.getStage())));
			fell = true;
		}
		NetherworldState s = c.netherworld();
		if (s.inGhostTribulation()) {
			s.setInGhostTribulation(false);
			GhostMethods.fallOneStage(s);
			p.server.getPlayerList().broadcastSystemMessage(Component.translatable(ModLang.MSG_TRIB_FAILED, p.getDisplayName()), false);
			p.sendSystemMessage(Component.translatable(ModLang.MSG_GHOST_TRIB_FLED, GhostMethods.rankName(GhostMethods.ghostRank(s))));
			fell = true;
		}
		if (fell) CultivationManager.markDirty(p.server);
		return fell;
	}

	/**
	 * Entering the Spatial Gap mid-trial, through the rift or by falling out of the Upper Realm: cancelled without reward or
	 * penalty (see SpatialTrialHandler#enterGap).
	 */
	public static void abandon(ServerPlayer p) {
		Trial t = ACTIVE.remove(p.getUUID());
		if (t == null) return;
		PlayerCultivation c = CultivationManager.get(p);
		if (t.ghost) c.netherworld().setInGhostTribulation(false);
		else c.setInTribulation(false);
		CultivationManager.markDirty(p.server);
		t.removeCloud();
		ModPackets.sendTribulation(p, false, 0, t.targetRealm, t.targetStage, t.ghost);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_TRIB_ABANDONED));
	}

	/** Follows movement and recreates the tracked entity if the player changes dimensions. */
	private static void updateCloud(ServerPlayer player, Trial trial) {
		if (trial.cloud == null || trial.cloud.isRemoved() || trial.cloud.level() != player.level()) {
			trial.removeCloud();
			trial.cloud = ModEntities.TRIBULATION_CLOUD.create(player.serverLevel());
			if (trial.cloud == null) return;
			trial.cloud.configure(trial.targetRealm, trial.ghost);
			trial.cloud.setPos(player.getX(), trial.cloud.cloudBaseY(player.getY()), player.getZ());
			player.serverLevel().addFreshEntity(trial.cloud);
		}
		trial.cloud.follow(player.position());
	}

	/**
	 * Clear references before a world/server is closed, including singleplayer world switches. Anyone mid-trial stays marked
	 * and fails it on their return ({@link #onJoin}).
	 */
	public static void clear() {
		ACTIVE.values().forEach(Trial::removeCloud);
		ACTIVE.clear();
	}

	private TribulationManager() {}
}
