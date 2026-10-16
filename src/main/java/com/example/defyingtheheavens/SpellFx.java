package com.example.defyingtheheavens;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * A spell's passing moment, drawn once by every client near (ClientSpellFx): a burst of flame where a bolt struck, steam where fire
 * met water, a crescent of sword light, a shockwave. Each is a {@link Kind} at a point, with a way (a direction, or a second point)
 * and two numbers of its own (a size, a strength). Nothing in the world comes of it: it is only seen.
 */
public final class SpellFx {
	/** What is seen. Synced by ordinal: add new ones at the end. */
	public enum Kind {
		/** A burst of flame petals and embers (size). */
		FLAME_BURST,
		/** Steam hissing up where fire met water (size). */
		STEAM,
		/** Ash and cinders: a wooden thing burnt up against fire. */
		ASH,
		/** A Golden Bell tolling as it takes a blow (how much it took). */
		BELL_TOLL,
		/** A Golden Bell breaking apart. */
		BELL_SHATTER,
		/** Water splashing out (size). */
		SPLASH,
		/** Earth and stone bursting apart (size; b: the block's state id for its colour). */
		EARTH_BURST,
		/** Splinters and leaves (size). */
		SPLINTERS,
		/** Sparks of struck metal (size). */
		METAL_SPARKS,
		/** A ring of force running out over the ground (radius). */
		SHOCKWAVE,
		/** A flash of light (size; b: colour 0xRRGGBB). */
		FLASH,
		/** A great explosion of fire (radius). */
		FIRE_EXPLOSION,
		/** A streak of light from the point along the way to its end (b: colour), lingering a moment. */
		STREAK,
		/** Life motes flowing from the point to the end of the way (a: green 1 drain, 0 gain). */
		LIFE_MOTES,
		/** Dust rising from the ground (size). */
		DUST,
		/** Frost and ice shards bursting (size). */
		ICE_BURST,
		/** A ripple ring on water underfoot (size). */
		RIPPLE,
		/** Thorns and bark splintering (a cage broken). */
		THORNS_BREAK,
		/** A golden core bursting: the great flash and fire of a self-detonation (radius). */
		CORE_DETONATION,
		/** A tear in space opening (a: size). */
		RIFT_TEAR,
		/** Leaves bursting out round a point (size). */
		LEAVES,
		/** A rock spike's dust and chips (size). */
		ROCK_CHIPS;

		public static Kind byIndex(int index) {
			return index >= 0 && index < values().length ? values()[index] : FLAME_BURST;
		}
	}

	/** How far off a moment is seen. */
	public static final double REACH = 128;

	/** Shows {@code kind} at {@code at} (with {@code way}, {@code a}, {@code b}) to everyone within {@link #REACH}. */
	public static void send(ServerLevel level, Kind kind, Vec3 at, Vec3 way, float a, float b) {
		send(level, kind, at, way, a, b, REACH);
	}

	/** Shows {@code kind} to everyone within {@code reach}. */
	public static void send(ServerLevel level, Kind kind, Vec3 at, Vec3 way, float a, float b, double reach) {
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceToSqr(at) <= reach * reach) ModPackets.sendSpellFx(player, kind, at, way, a, b);
		}
	}

	private SpellFx() {}
}
