package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Souls in puppets (Puppetry, phase 2a: the user's "i want all puppets to be able to harbour souls of players or NPCs and animals and
 * stuff, or be hollow", and "continue with all phases", 2026-11-17). Every puppet has a Soul-Binding Ring; empty, the puppet is hollow.
 * <ul>
 *   <li><b>Caught</b> ({@link #onDeath}): a beast (an animal or a demon beast) or a cultivator slain by a puppeteer or one of their puppets
 *   within {@link #CATCH_RADIUS} of one of that puppeteer's puppets with an empty ring: its soul is pulled into the nearest such. Binding a
 *   cultivator's soul is devilish ({@link #BIND_SHIFT}).</li>
 *   <li><b>What it gives</b> (PuppetEntity): a beast's instincts (farther sight, a dodge, a swifter pace); a cultivator's name, a charge more
 *   of the core's art, and one or two of its own arts, cast in a fight every {@link #ART_EVERY} ticks.</li>
 *   <li><b>Copied</b>: while its puppeteer is in the world with it and it can act, the puppet's circuits copy the soul; after
 *   {@link #COPY_TICKS} the copy is whole, the real soul goes on, and the puppet keeps all it gave.</li>
 *   <li><b>At a Puppet Workbench</b> (the puppet within {@link #BENCH_REACH} of one, its board open): the real soul released before its
 *   copy is whole ({@link #release}: a cultivator's, {@link #RELEASE_SHIFT} back toward the righteous), moved into another of its
 *   puppeteer's puppets with an empty ring there ({@link #move}: its copying begun anew), or a whole copy wiped ({@link #wipe}).</li>
 * </ul>
 * A soul is never an item: it goes only from ring to ring. Players' souls are phase 2d's.
 */
public final class PuppetSouls {
	/** A soul is caught by a puppet of its slayer's this near where it fell (one with its ring empty). */
	public static final double CATCH_RADIUS = 16;
	/** Copying a soul: an hour of its puppeteer's control (three of the game's days). */
	public static final int COPY_TICKS = 20 * 60 * 60;
	/** Binding a cultivator's soul draws its binder this far toward the devilish path; letting one go before its copy is whole, back this far. */
	public static final int BIND_SHIFT = 10, RELEASE_SHIFT = 5;
	/** A beast's soul: this much farther sight, this chance to slip a blow, this much swifter. */
	public static final int BEAST_SIGHT = 8;
	public static final double BEAST_DODGE = 0.10, BEAST_PACE = 0.10;
	/** A cultivator's soul: its own arts cast every this many ticks in a fight, this far at most; the most it keeps. */
	public static final int ART_EVERY = 200, MOST_ARTS = 2;
	public static final double ART_REACH = 16;
	/** How near a Puppet Workbench a puppet stands for its soul to be released, moved or wiped; and the puppet moved into, from the bench. */
	public static final int BENCH_REACH = 8;

	// --- A mind of its own (phase 2c: PuppetSoul#grow, PuppetEntity) ---

	/** A Saint's puppet, its copy whole, grows a mind of its own after this long under control: three more of the game's days. */
	public static final int GROW_TICKS = 20 * 60 * 60;
	/** What weighs toward each bent while it grows: a blow landed, a second on watch (guarding or holding), a point of harm borne, a block covered, an art cast. */
	public static final double MARK_BLOW = 1, MARK_WATCH = 1.0 / 30, MARK_HARM = 0.5, MARK_BLOCK = 1.0 / 32, MARK_ART = 3;
	/**
	 * What each bent gives: Fierce blows, Swift pace and flight, Steadfast health (shares more); Watchful sight (blocks) and a quicker eye (its
	 * wait to take up a foe this share of what it was); Canny heat shed and its art's charges back this many times as fast.
	 */
	public static final double FIERCE_BLOWS = 0.15, SWIFT_PACE = 0.15, STEADFAST_HEALTH = 0.20, WATCHFUL_EYE = 0.5, CANNY_COOL = 2, CANNY_RECHARGE = 1.5;
	public static final int WATCHFUL_SIGHT = 8;

	// --- Caught ---

	/** AFTER_DEATH: {@code dead}'s soul pulled into its slayer's nearest puppet with an empty ring, if one stands near enough. */
	public static void onDeath(LivingEntity dead, DamageSource source) {
		if (!(dead.level() instanceof ServerLevel level)) return;
		// A player's soul: phase 2d's rules (PuppetPlayers).
		if (dead instanceof ServerPlayer victim) {
			PuppetPlayers.onDeath(victim, source);
			return;
		}
		PuppetSoul.Kind kind = kindOf(dead);
		if (kind == null) return;
		ServerPlayer slayer = slayer(source);
		if (slayer == null) return;
		PuppetEntity ring = nearestEmpty(level, slayer, dead.position());
		if (ring == null) return;
		PuppetSoul soul = soulOf(dead, kind);
		ring.bindSoul(soul);
		// The soul drawn across to its new ring.
		Vec3 from = dead.position().add(0, dead.getBbHeight() * 0.6, 0), to = ring.position().add(0, ring.getBbHeight() * 0.6, 0);
		Vec3 step = to.subtract(from);
		for (int k = 0; k <= 8; k++) {
			Vec3 at = from.add(step.scale(k / 8.0));
			level.sendParticles(ParticleTypes.SOUL, at.x, at.y, at.z, 2, 0.1, 0.1, 0.1, 0.01);
		}
		level.playSound(null, ring.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.NEUTRAL, 1.0f, 0.8f);
		if (kind == PuppetSoul.Kind.CULTIVATOR) {
			PlayerCultivation c = CultivationManager.own(slayer);
			c.setAlignment(c.getAlignment() - BIND_SHIFT);
		}
		slayer.sendSystemMessage(Component.translatable(kind == PuppetSoul.Kind.CULTIVATOR ? ModLang.SOUL_BOUND_CULTIVATOR : ModLang.SOUL_BOUND_BEAST,
				soul.name, ring.getDisplayName()).withStyle(kind == PuppetSoul.Kind.CULTIVATOR ? ChatFormatting.DARK_RED : ChatFormatting.AQUA));
	}

	/** What soul {@code dead} leaves for a ring: a beast's (an animal, a demon beast), a cultivator's; null for anything else. */
	static PuppetSoul.@Nullable Kind kindOf(LivingEntity dead) {
		if (dead instanceof GhostCultivatorNpc) return null; // the Netherworld's dead are souls already, not bodies holding one
		if (dead instanceof CultivatorNpc npc) return npc.isVacant() ? null : PuppetSoul.Kind.CULTIVATOR; // a body set aside holds no soul
		if (dead instanceof Animal || dead instanceof DemonBeast) return PuppetSoul.Kind.BEAST;
		return null;
	}

	/** Whose kill {@code source} is: a player's, or their puppet's; null for anyone else's. */
	static @Nullable ServerPlayer slayer(DamageSource source) {
		if (source.getEntity() instanceof ServerPlayer player) return player;
		if (source.getEntity() instanceof PuppetEntity puppet && puppet.getOwner() instanceof ServerPlayer owner) return owner;
		return null;
	}

	/** {@code owner}'s puppet nearest {@code at}, within {@link #CATCH_RADIUS}, its ring empty and its heart fed; null if none. */
	static @Nullable PuppetEntity nearestEmpty(ServerLevel level, ServerPlayer owner, Vec3 at) {
		return level.getEntitiesOfClass(PuppetEntity.class, new AABB(at, at).inflate(CATCH_RADIUS),
						p -> p.isAlive() && p.isOwnedBy(owner) && p.soul() == null && !p.isInert() && p.position().distanceTo(at) <= CATCH_RADIUS)
				.stream().min(Comparator.comparingDouble(p -> p.position().distanceToSqr(at))).orElse(null);
	}

	/** The soul {@code dead} leaves: its name, its kind, and a cultivator's element, rank and its deepest one or two striking arts. */
	static PuppetSoul soulOf(LivingEntity dead, PuppetSoul.Kind kind) {
		String type = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType()).toString();
		if (kind == PuppetSoul.Kind.CULTIVATOR && dead instanceof CultivatorNpc npc) {
			HeavenMethod method = npc.heavenMethod();
			List<Spell> arts = new ArrayList<>();
			for (Spell spell : CultivatorSpells.repertoire(npc)) {
				CultivatorSpells.Use use = CultivatorSpells.use(spell);
				if (spell.isHeaven() && (use == CultivatorSpells.Use.AT_FOE || use == CultivatorSpells.Use.ON_FOE)) arts.add(spell);
			}
			arts.sort((a, b) -> Integer.compare(b.layer(), a.layer()));
			if (arts.size() > MOST_ARTS) arts = new ArrayList<>(arts.subList(0, MOST_ARTS));
			// (Its own name, as its body is named: CorpseBlockEntity#of.)
			String name = npc.hasCustomName() ? npc.getCustomName().getString() : npc.getPersonalName();
			return new PuppetSoul(kind, name, type, method == null ? null : method.element(), npc.getRank(), arts);
		}
		return new PuppetSoul(kind, dead.getType().getDescription().getString(), type, null, 0, List.of());
	}

	// --- At the bench ---

	/** The Puppet Workbench within {@link #BENCH_REACH} of {@code puppet} nearest it, or null. */
	public static @Nullable BlockPos benchNear(PuppetEntity puppet) {
		BlockPos at = puppet.blockPosition(), best = null;
		double bestDistance = Double.MAX_VALUE;
		for (BlockPos pos : BlockPos.betweenClosed(at.offset(-BENCH_REACH, -BENCH_REACH / 2, -BENCH_REACH), at.offset(BENCH_REACH, BENCH_REACH / 2, BENCH_REACH))) {
			if (!puppet.level().getBlockState(pos).is(ModBlocks.PUPPET_WORKBENCH)) continue;
			double d = pos.distSqr(at);
			if (d < bestDistance) {
				bestDistance = d;
				best = pos.immutable();
			}
		}
		return best;
	}

	/** {@code owner}'s other puppet by {@code bench} with an empty ring a soul could be moved into (the nearest the bench), or null. */
	public static @Nullable PuppetEntity moveTarget(ServerPlayer owner, PuppetEntity from, BlockPos bench) {
		Vec3 at = Vec3.atCenterOf(bench);
		return owner.serverLevel().getEntitiesOfClass(PuppetEntity.class, new AABB(bench).inflate(BENCH_REACH),
						p -> p != from && p.isAlive() && p.isOwnedBy(owner) && p.soul() == null && p.position().distanceTo(at) <= BENCH_REACH)
				.stream().min(Comparator.comparingDouble(p -> p.position().distanceToSqr(at))).orElse(null);
	}

	/** The real soul in {@code puppet} let go before its copy is whole: the ring hollow again. What to tell its puppeteer. */
	public static Component release(ServerPlayer owner, PuppetEntity puppet) {
		PuppetSoul soul = puppet.soul();
		if (soul == null || !soul.real()) return Component.translatable(ModLang.SOUL_NONE_REAL).withStyle(ChatFormatting.RED);
		if (benchNear(puppet) == null) return Component.translatable(ModLang.SOUL_NOT_AT_BENCH, BENCH_REACH).withStyle(ChatFormatting.RED);
		PuppetPlayers.lostWith(puppet, ModLang.PUPPET_SOUL_RELEASED_AT_BENCH); // a player's goes on (phase 2d)
		puppet.bindSoul(null);
		if (soul.kind == PuppetSoul.Kind.CULTIVATOR || soul.kind == PuppetSoul.Kind.PLAYER) {
			PlayerCultivation c = CultivationManager.own(owner);
			c.setAlignment(c.getAlignment() + RELEASE_SHIFT);
		}
		if (puppet.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.SOUL, puppet.getX(), puppet.getY(0.7), puppet.getZ(), 16, 0.2, 0.6, 0.2, 0.04);
			level.playSound(null, puppet.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.NEUTRAL, 1.0f, 1.2f);
		}
		return Component.translatable(ModLang.SOUL_RELEASED, soul.name).withStyle(ChatFormatting.AQUA);
	}

	/** The real soul in {@code puppet} moved into another of its puppeteer's puppets by the same bench, its copying begun anew there. */
	public static Component move(ServerPlayer owner, PuppetEntity puppet) {
		PuppetSoul soul = puppet.soul();
		if (soul == null || !soul.real()) return Component.translatable(ModLang.SOUL_NONE_REAL).withStyle(ChatFormatting.RED);
		BlockPos bench = benchNear(puppet);
		if (bench == null) return Component.translatable(ModLang.SOUL_NOT_AT_BENCH, BENCH_REACH).withStyle(ChatFormatting.RED);
		PuppetEntity into = moveTarget(owner, puppet, bench);
		if (into == null) return Component.translatable(ModLang.SOUL_NO_RING, BENCH_REACH).withStyle(ChatFormatting.RED);
		puppet.bindSoul(null);
		PuppetSoul moved = soul.movedAnew();
		into.bindSoul(moved);
		PuppetPlayers.movedTo(into, moved); // a player's binding follows it (phase 2d)
		if (puppet.level() instanceof ServerLevel level) {
			Vec3 from = puppet.position().add(0, 1, 0), step = into.position().add(0, 1, 0).subtract(from);
			for (int k = 0; k <= 8; k++) {
				Vec3 at = from.add(step.scale(k / 8.0));
				level.sendParticles(ParticleTypes.SOUL, at.x, at.y, at.z, 2, 0.1, 0.1, 0.1, 0.01);
			}
		}
		return Component.translatable(ModLang.SOUL_MOVED, soul.name, into.getDisplayName()).withStyle(ChatFormatting.AQUA);
	}

	/** A whole copy in {@code puppet} wiped: its mind lost, the ring hollow and free again. */
	public static Component wipe(ServerPlayer owner, PuppetEntity puppet) {
		PuppetSoul soul = puppet.soul();
		if (soul == null || !soul.whole()) return Component.translatable(ModLang.SOUL_NO_COPY).withStyle(ChatFormatting.RED);
		if (benchNear(puppet) == null) return Component.translatable(ModLang.SOUL_NOT_AT_BENCH, BENCH_REACH).withStyle(ChatFormatting.RED);
		puppet.bindSoul(null);
		puppet.playSound(SoundEvents.AMETHYST_BLOCK_BREAK, 1.0f, 0.6f);
		return Component.translatable(ModLang.SOUL_WIPED, soul.name).withStyle(ChatFormatting.GOLD);
	}

	private PuppetSouls() {}
}
