package com.example.defyingtheheavens;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LightningBolt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Heavenly Tribulation: the trial a player must survive to cross into the next major realm,
 * and, from Nascent Soul on, into every minor stage.
 * State is runtime-only; a tribulation never survives a server restart or a disconnect.
 */
public final class TribulationManager {
	private static final int FIRST_STRIKE_DELAY = 60;  // 3 s of darkening sky before the first bolt
	private static final int STRIKE_INTERVAL = 60;     // 3 s between strikes
	private static final int FINISH_DELAY = 20;        // 1 s after the last strike before the breakthrough completes

	/** One tribulation: a set number of identical strikes, each heavenly lightning (armor applies) plus magic (it doesn't). */
	public record Strikes(int count, float bolt, float magic) {}

	private static final class Trial {
		final Realm targetRealm;
		final Stage targetStage;
		final Strikes strikes;
		int strikesLeft;
		int nextStrikeIn = FIRST_STRIKE_DELAY;
		int finishIn = FINISH_DELAY;
		int age;

		Trial(Realm targetRealm, Stage targetStage) {
			this.targetRealm = targetRealm;
			this.targetStage = targetStage;
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

	/*
	 * Balance. A set number of strikes lands 3 s apart, after 3 s of darkening sky. From Foundation Building on, the hunger
	 * perk regenerates 6 health between strikes, so only damage above that wears the player down. Lightning is cut by armor
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
		PlayerCultivation c = CultivationManager.get(player);
		if (c.isAtBottleneck() && c.isBreakthroughLocked()) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_REALM_LOCKED,
					PlayerCultivation.rankName(PlayerCultivation.LOWER_REALM_CAP_REALM, PlayerCultivation.LOWER_REALM_CAP_STAGE)), true);
			return;
		}
		if (!c.canBreakthrough()) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_NOT_READY), true);
			return;
		}

		MeditationManager.stop(player, false); // no-op if not meditating
		Trial trial = new Trial(c.breakthroughRealm(), c.breakthroughStage());
		ACTIVE.put(player.getUUID(), trial);

		forceStorm(player);
		ModPackets.sendTribulation(player, true, trial.strikesLeft, trial.targetRealm, trial.targetStage);
		player.playNotifySound(SoundEvents.ENDER_DRAGON_GROWL, SoundSource.AMBIENT, 1.0f, 0.5f);
		player.playNotifySound(SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.0f, 0.4f);
		player.sendSystemMessage(trial.strikesLeft == 1
				? Component.translatable(ModLang.MSG_TRIB_START_SINGLE, trial.targetName())
				: Component.translatable(ModLang.MSG_TRIB_START, trial.targetName(), trial.strikesLeft));
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
				ACTIVE.remove(id);
				continue;
			}
			if (!p.isAlive()) continue; // onDeath() handles failure

			t.age++;

			if (t.strikesLeft > 0) {
				if (--t.nextStrikeIn <= 0) {
					strike(p, t);
					if (ACTIVE.get(id) != t) continue; // died just now
					t.strikesLeft--;
					t.nextStrikeIn = STRIKE_INTERVAL;
					ModPackets.sendTribulation(p, true, t.strikesLeft, t.targetRealm, t.targetStage);
				}
			} else if (--t.finishIn <= 0) {
				survivors.add(p); // the last strike has faded
				continue;
			}

			if (t.age % 20 == 0) {
				forceStorm(p); // re-assert in case real weather tried to override us
				ModPackets.sendTribulation(p, true, t.strikesLeft, t.targetRealm, t.targetStage);
			}
			if (t.age % 100 == 0) {
				p.playNotifySound(SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 0.8f, 0.5f);
			}
		}
		survivors.forEach(TribulationManager::succeed);
	}

	private static void strike(ServerPlayer p, Trial t) {
		ServerLevel level = p.serverLevel();
		LightningBolt bolt = ModEntities.TRIBULATION_LIGHTNING.create(level); // drawn bright blue by the client
		if (bolt == null) return;
		bolt.moveTo(p.getX(), p.getY(), p.getZ());
		bolt.setVisualOnly(true); // 1.20.1 bolts always deal a flat 5, so the tribulation's hit is applied by hand below
		level.addFreshEntity(bolt);

		// Both parts land in the same tick as the bolt. Clearing the hurt cooldown in between stops vanilla from
		// discarding the second hit; magic goes first so a killing strike usually reads "struck by lightning".
		float multiplier = RingOfTranscendenceItem.damageMultiplier(p);
		float magic = t.strikes.magic() * multiplier;
		if (magic > 0) {
			p.hurt(level.damageSources().magic(), magic);
			p.invulnerableTime = 0;
		}
		if (p.isAlive()) {
			p.hurt(level.damageSources().lightningBolt(), t.strikes.bolt() * multiplier);
		}
	}

	private static void succeed(ServerPlayer p) {
		Trial t = ACTIVE.remove(p.getUUID());
		if (t == null) return;

		restoreWeather(p);
		ModPackets.sendTribulation(p, false, 0, t.targetRealm, t.targetStage);

		PlayerCultivation c = CultivationManager.get(p);
		boolean sameTarget = c.breakthroughRealm() == t.targetRealm && c.breakthroughStage() == t.targetStage;
		if (!sameTarget || !c.breakthrough()) { // state changed mid-trial (e.g. admin command)
			p.displayClientMessage(Component.translatable(ModLang.MSG_NOT_READY), true);
			return;
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

	/** Called from the AFTER_DEATH event (totems of undying prevent that event, so they save the player). */
	public static void onDeath(ServerPlayer p) {
		Trial t = ACTIVE.remove(p.getUUID());
		if (t == null) return;

		PlayerCultivation c = CultivationManager.get(p);
		c.fallOneStage();
		CultivationManager.markDirty(p.server);

		restoreWeather(p);
		ModPackets.sendTribulation(p, false, 0, t.targetRealm, t.targetStage);
		p.server.getPlayerList().broadcastSystemMessage(Component.translatable(ModLang.MSG_TRIB_FAILED, p.getDisplayName()), false);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_TRIB_FALL, PlayerCultivation.rankName(c.getRealm(), c.getStage())));
		// The respawn event re-applies the lowered stats and re-syncs the client.
	}

	/** Disconnect: the trial is cancelled without reward or penalty. */
	public static void forget(UUID id) {
		ACTIVE.remove(id);
	}

	/** Falling out of the Upper Realm mid-trial: cancelled without reward or penalty, like a disconnect. */
	public static void abandon(ServerPlayer p) {
		Trial t = ACTIVE.remove(p.getUUID());
		if (t == null) return;
		restoreWeather(p);
		ModPackets.sendTribulation(p, false, 0, t.targetRealm, t.targetStage);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_TRIB_ABANDONED));
	}

	/** Local-to-the-player weather: vanilla game-event packets only reach this one client. */
	private static void forceStorm(ServerPlayer p) {
		p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 1.0F));
		p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 1.0F));
	}

	private static void restoreWeather(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, level.getRainLevel(1.0F)));
		p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, level.getThunderLevel(1.0F)));
	}

	private TribulationManager() {}
}
