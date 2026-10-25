package com.example.defyingtheheavens;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * How a spell's shape is held on the being it is worn by or cast from (SpellConstruct#anchor, SpellField#anchor; the user's ask, 2026-10-25:
 * "if you have stuff that appears around or on the player and tracks their position ... when I move it lags behind and looks stupid"). Its
 * spell still moves it on the server tick by tick, but every watcher draws it where its being is drawn that very frame (its own interpolated
 * place and turn), so it never trails a step behind it, however fast it goes or turns.
 * <ul>
 *   <li>{@link #POSITION}: at its being's feet (and its offset, as the world's axes have it), turned as its spell turns it;</li>
 *   <li>{@link #BODY}: its offset turned with its being's body, and it faces as the body does (a ward of bone, a wheel at its back);</li>
 *   <li>{@link #LOOK}: its offset turned with where its being looks, and it faces that way, its pitch held to {@link #PITCH_HELD} (a mirror
 *       behind the head); {@link #LOOK_LEVEL} the same, but kept level (a book over the head);</li>
 *   <li>{@link #HANDS}: where a cast from the hands sets out (HeavenSpells#fromHands), facing along the look (a lance of water).</li>
 * </ul>
 * Synced by ordinal: add at the end.
 */
public enum SpellAnchor {
	NONE, POSITION, BODY, LOOK, LOOK_LEVEL, HANDS;

	/** The most a {@link #LOOK} shape tips up or down with its being's look (degrees). */
	public static final float PITCH_HELD = 30;

	public static SpellAnchor byIndex(int index) {
		return index >= 0 && index < values().length ? values()[index] : NONE;
	}

	/** The yaw it turns its offset by and faces, {@code partial} of the way through this tick; {@code own} where it keeps its spell's. */
	public float yaw(Entity on, float partial, float own) {
		return switch (this) {
			case BODY -> on instanceof LivingEntity living ? Mth.rotLerp(partial, living.yBodyRotO, living.yBodyRot) : Mth.rotLerp(partial, on.yRotO, on.getYRot());
			case LOOK, LOOK_LEVEL, HANDS -> on instanceof LivingEntity living ? Mth.rotLerp(partial, living.yHeadRotO, living.yHeadRot) : Mth.rotLerp(partial, on.yRotO, on.getYRot());
			default -> own;
		};
	}

	/** The pitch it faces, {@code partial} of the way through this tick; {@code own} where it keeps its spell's. */
	public float pitch(Entity on, float partial, float own) {
		return switch (this) {
			case LOOK -> Mth.clamp(Mth.lerp(partial, on.xRotO, on.getXRot()), -PITCH_HELD, PITCH_HELD);
			case LOOK_LEVEL -> 0;
			case HANDS -> Mth.lerp(partial, on.xRotO, on.getXRot());
			default -> own;
		};
	}

	/**
	 * Where it is, {@code partial} of the way through this tick: its being's place (its feet), and {@code offset} there (x to its right, y up,
	 * z ahead, turned by {@link #yaw}; as the world's axes for {@link #POSITION}); for {@link #HANDS}, before its chest along its look.
	 */
	public Vec3 at(Entity on, Vec3 offset, float partial, float own) {
		Vec3 feet = on.getPosition(partial);
		if (this == HANDS) {
			Vec3 look = Vec3.directionFromRotation(Mth.lerp(partial, on.xRotO, on.getXRot()), yaw(on, partial, own));
			return feet.add(0, on.getEyeHeight() - 0.45, 0).add(look.scale(1.2)).add(offset);
		}
		if (this == POSITION || this == NONE) return feet.add(offset);
		return feet.add(turned(offset, yaw(on, partial, own)));
	}

	/** {@code offset} (x to the right, y up, z ahead) turned to face {@code yaw}. */
	public static Vec3 turned(Vec3 offset, float yaw) {
		double rad = Math.toRadians(yaw), sin = Math.sin(rad), cos = Math.cos(rad);
		// Ahead is (-sin, 0, cos) (Vec3#directionFromRotation); to its right (-cos, 0, -sin).
		return new Vec3(-sin * offset.z - cos * offset.x, offset.y, cos * offset.z - sin * offset.x);
	}
}
