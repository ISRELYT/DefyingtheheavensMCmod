package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.SwordItem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * The war arts won in the secret realms (2026-10-31), techniques switched on as abilities, each for {@link #cost} qi a second while on (one
 * with too little left goes out):
 * <ul>
 *   <li><b>Sword Intent</b> (Ability#SWORD_INTENT, the Sunken Sword Tomb's Great Inheritance): every blow one lands with a blade is followed,
 *   {@link #INTENT_DELAY} ticks on, by a second cut of lingering intent {@link #INTENT_SHARE} of it; the tomb's own intent spares one
 *   (SwordTombWorks#intentLit).</li>
 *   <li><b>Blood Battle</b> (Ability#BLOOD_BATTLE, the Ancient Battlefield's): every blow one lands heals one {@link #BLOOD_HEAL} of it, and
 *   with more than one foe set on one within {@link #BLOOD_REACH} blocks their blows harm {@link #BLOOD_GUARD} less for each past the first,
 *   at most {@link #BLOOD_GUARD_MOST} less ({@link #guard}, HeavenSpells#incoming).</li>
 * </ul>
 */
public final class WarArts {
	public static final double INTENT_SHARE = 0.5;
	static final int INTENT_DELAY = 8;
	public static final double BLOOD_HEAL = 0.10, BLOOD_GUARD = 0.10, BLOOD_GUARD_MOST = 0.5;
	public static final int BLOOD_REACH = 8;
	/** What either costs a second: this share of one's whole qi pool. */
	static final double COST_SHARE = 0.008;

	/** A second cut of lingering intent, due at {@code at} (game time). */
	private record Cut(UUID by, LivingEntity target, float amount, long at) {}

	private static final List<Cut> CUTS = new ArrayList<>();
	/** While a cut of lingering intent lands (it isn't a blow of its own to follow with another). */
	private static boolean cutting;

	/** What either war art costs {@code c} a second. */
	public static double cost(PlayerCultivation c) {
		return Math.max(1, c.maxQi() * COST_SHARE);
	}

	static boolean lit(ServerPlayer p, Ability ability) {
		PlayerCultivation c = CultivationManager.get(p);
		return c.isAbilityEnabled(ability) && ability.isUnlocked(c);
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(WarArts::tick);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			landed(entity, source, amount);
			return true;
		});
	}

	private static void tick(MinecraftServer server) {
		long now = server.overworld().getGameTime();
		for (Iterator<Cut> it = CUTS.iterator(); it.hasNext(); ) {
			Cut cut = it.next();
			if (now < cut.at) continue;
			it.remove();
			LivingEntity target = cut.target;
			if (!target.isAlive() || target.isRemoved() || !(target.level() instanceof ServerLevel level)) continue;
			ServerPlayer by = server.getPlayerList().getPlayer(cut.by);
			target.invulnerableTime = 0;
			cutting = true;
			try {
				target.hurt(ModDamageTypes.swordIntent(level, by), cut.amount);
			} finally {
				cutting = false;
			}
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 1, 0, 0, 0, 0);
			level.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
			level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 1.6f);
		}
		if (server.getTickCount() % 20 != 0) return;
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			PlayerCultivation c = CultivationManager.get(p);
			for (Ability art : new Ability[] {Ability.SWORD_INTENT, Ability.BLOOD_BATTLE}) {
				if (!c.isAbilityEnabled(art) || !art.isUnlocked(c)) continue;
				double cost = cost(c);
				if (c.getQi() < cost) {
					c.setAbilityEnabled(art, false);
					CultivationManager.sync(p);
					p.displayClientMessage(Component.translatable(ModLang.MSG_ART_FADES, art.getDisplayName()).withStyle(ChatFormatting.GRAY), true);
					continue;
				}
				c.setQi(c.getQi() - cost);
				// Its sign about one: glints of sword light, or a red haze.
				p.serverLevel().sendParticles(art == Ability.SWORD_INTENT ? ParticleTypes.ENCHANTED_HIT : ParticleTypes.CRIMSON_SPORE, p.getX(), p.getY() + 1, p.getZ(),
						4, 0.4, 0.6, 0.4, 0.02);
			}
		}
	}

	/** A blow landing (ALLOW_DAMAGE): a player's own with a blade, Sword Intent lit, followed by a second cut; Blood Battle lit, a share healed. */
	static void landed(LivingEntity target, DamageSource source, float amount) {
		if (cutting || amount <= 0 || !(source.getEntity() instanceof ServerPlayer p) || source.getDirectEntity() != p || target == p) return;
		if (!source.is(DamageTypes.PLAYER_ATTACK)) return;
		if (lit(p, Ability.SWORD_INTENT) && p.getMainHandItem().getItem() instanceof SwordItem) {
			CUTS.add(new Cut(p.getUUID(), target, (float) (amount * INTENT_SHARE), p.server.overworld().getGameTime() + INTENT_DELAY));
		}
		if (lit(p, Ability.BLOOD_BATTLE)) p.heal((float) (amount * BLOOD_HEAL));
	}

	/** Blood Battle's guard (HeavenSpells#incoming): the harm {@code target} takes lessened for each foe set on them past the first. */
	static float guard(LivingEntity target, DamageSource source, float amount) {
		if (!(target instanceof ServerPlayer p) || !lit(p, Ability.BLOOD_BATTLE)) return amount;
		int foes = foesAbout(p);
		if (foes <= 1) return amount;
		return (float) (amount * (1 - Math.min(BLOOD_GUARD_MOST, BLOOD_GUARD * (foes - 1))));
	}

	/** How many are set on {@code p} within {@link #BLOOD_REACH}: creatures and cultivators after them, and whoever struck them last if near. */
	static int foesAbout(ServerPlayer p) {
		int n = p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(BLOOD_REACH), m -> m.isAlive() && m.getTarget() == p).size();
		LivingEntity last = p.getLastHurtByMob();
		if (last instanceof ServerPlayer other && other.isAlive() && other.distanceTo(p) <= BLOOD_REACH) n++;
		return n;
	}

	public static void clear() {
		CUTS.clear();
	}

	private WarArts() {}
}
