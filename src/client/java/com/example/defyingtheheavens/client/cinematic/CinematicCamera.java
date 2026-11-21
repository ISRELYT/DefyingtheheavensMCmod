package com.example.defyingtheheavens.client.cinematic;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The breakthrough player's camera (the brief §6.2): the shot on screen at each moment, worked out from the elapsed ticks, the start
 * place and the facing, so every shot frames the same wherever it plays. Blended in from the player's own view over the sequence's
 * blend-in and back out to it at the end. Never inside a block: if anything stands between what a shot looks at and where it would
 * stand, the camera is pulled in to just before it. Set after Camera#setup (CinematicCameraMixin); the roll through
 * GameRenderer#bobHurt and the FOV through #getFov, as the mod's other camera effects are.
 */
public final class CinematicCamera {
	/** Where the camera stands and how it turns, at one moment. */
	public record View(Vec3 pos, float yaw, float pitch, float roll, float fov) {}

	/** The view of {@code p} at {@code t} ticks, the partial tick included. */
	public static View view(CinematicClient.Playing p, float t) {
		Sequence seq = p.seq;
		View shot = shotView(p, seq.shotAt(t), t);
		// In from the player's own camera as it was when the film began (where they looked, however far behind them it stood: no cut), and
		// out to the camera they are handed back to.
		if (t < seq.blendIn) return blend(p, p.from != null ? p.from : handBackView(p), shot, Ease.SMOOTH.apply(t / seq.blendIn));
		if (t > p.length - seq.blendOut) return blend(p, shot, handBackView(p), Ease.SMOOTH.apply((t - (p.length - seq.blendOut)) / seq.blendOut));
		return shot;
	}

	/**
	 * The camera the player is handed back to: their eyes, facing as they sat and looking level (the hold keeps them so), or the third-person
	 * camera behind them (or in front, looking back) when that is the one they had, pulled in short of anything in its way as vanilla's is.
	 */
	private static View handBackView(CinematicClient.Playing p) {
		Minecraft mc = Minecraft.getInstance();
		double eye = mc.player == null ? 1.62 : mc.player.getEyeHeight();
		Vec3 eyes = p.at.add(0, eye, 0);
		float fov = mc.options.fov().get();
		if (p.returnTo == null || p.returnTo.isFirstPerson()) return new View(eyes, p.facing, 0, 0, fov);
		boolean front = p.returnTo.isMirrored();
		Vec3 back = clear(eyes, local(p, eyes, new float[] {0, 0, front ? 4 : -4}));
		return new View(back, front ? p.facing + 180 : p.facing, 0, 0, fov);
	}

	private static View shotView(CinematicClient.Playing p, Sequence.Shot s, float t) {
		Minecraft mc = Minecraft.getInstance();
		float u = s.to() <= s.from() ? 1 : Mth.clamp((t - s.from()) / (s.to() - s.from()), 0, 1);
		if (s.whip() && CinematicSettings.reduceMotion) u = 1; // no whips: a cut to where it ends
		u = s.ease().apply(u);
		Vec3 anchor = anchor(p, s.anchor(), t);
		float k = s.scale() ? heightScale(p) : 1;
		Vec3 pos, look;
		if (s.orbit() != null) {
			Sequence.Orbit o = s.orbit();
			float angle = (float) Math.toRadians(Mth.lerp(u, o.angle()[0], o.angle()[1]));
			float radius = Mth.lerp(u, o.radius()[0], o.radius()[1]) * k, height = Mth.lerp(u, o.height()[0], o.height()[1]) * k;
			Vec3 centre = local(p, anchor, scaled(o.center(), k));
			pos = local(p, centre, new float[] {(float) Math.sin(angle) * radius, height - o.center()[1] * k, (float) Math.cos(angle) * radius});
			look = centre;
		} else {
			pos = local(p, anchor, scaled(spline(s.pos(), u), k));
			look = local(p, anchor, scaled(spline(s.look(), u), k));
		}
		pos = clear(look, pos);
		Vec3 d = look.subtract(pos);
		double flat = Math.sqrt(d.x * d.x + d.z * d.z);
		float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
		float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.max(1.0E-4, flat)));
		float roll = CinematicSettings.reduceMotion ? 0 : Mth.clamp(Mth.lerp(u, s.roll()[0], s.roll()[1]), -10, 10);
		float fov = (s.fov() != null ? s.fov() : mc.options.fov().get()) + fovOffset(p.seq, t);
		return new View(pos, yaw, pitch, roll, fov);
	}

	/** What a shot's places are measured from at tick {@code t}. */
	static Vec3 anchor(CinematicClient.Playing p, String name, float t) {
		return switch (name) {
			case "soul" -> GreyBox.soulFeet(p, t);
			case "star" -> GreyBox.starPos(p, t);
			case "top" -> new Vec3(p.at.x, p.probe.topY(), p.at.z);
			default -> p.at;
		};
	}

	/** The pillar's height against the 128 blocks a scaled shot is written for (see Sequence.Shot#scale), held to 0.25..1.5. */
	static float heightScale(CinematicClient.Playing p) {
		return Mth.clamp((float) ((p.probe.topY() - p.probe.groundY()) / 128), 0.25f, 1.5f);
	}

	private static float[] scaled(float[] v, float k) {
		return k == 1 ? v : new float[] {v[0] * k, v[1] * k, v[2] * k};
	}

	/** [side, up, forward] in the player's start frame, from {@code base}: side is to their right, forward where they faced. */
	static Vec3 local(CinematicClient.Playing p, Vec3 base, float[] v) {
		double yaw = Math.toRadians(p.facing);
		double fx = -Math.sin(yaw), fz = Math.cos(yaw); // forward
		double rx = -Math.cos(yaw), rz = -Math.sin(yaw); // the player's right
		return base.add(rx * v[0] + fx * v[2], v[1], rz * v[0] + fz * v[2]);
	}

	/** A Catmull-Rom curve through the keys, evenly spread over 0..1. */
	private static float[] spline(float[][] keys, float u) {
		if (keys.length == 1) return keys[0];
		float f = u * (keys.length - 1);
		int i = Math.min(keys.length - 2, (int) Math.floor(f));
		float s = f - i;
		float[] a = keys[Math.max(0, i - 1)], b = keys[i], c = keys[i + 1], d = keys[Math.min(keys.length - 1, i + 2)];
		float[] out = new float[3];
		for (int k = 0; k < 3; k++) {
			out[k] = 0.5f * (2 * b[k] + (-a[k] + c[k]) * s + (2 * a[k] - 5 * b[k] + 4 * c[k] - d[k]) * s * s + (-a[k] + 3 * b[k] - 3 * c[k] + d[k]) * s * s * s);
		}
		return out;
	}

	/** The camera pulled in toward what it looks at when a block stands between them, so it is never inside one. */
	private static Vec3 clear(Vec3 look, Vec3 pos) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return pos;
		BlockHitResult hit = mc.level.clip(new ClipContext(look, pos, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mc.player));
		if (hit.getType() == HitResult.Type.MISS) return pos;
		Vec3 d = pos.subtract(look);
		double len = d.length();
		return len < 1.0E-3 ? pos : look.add(d.scale(Math.max(0.2, hit.getLocation().distanceTo(look) - 0.3) / len));
	}

	/** The sequence's FOV changes at {@code t} (degrees added to the shot's): a straight line between its keys. */
	private static float fovOffset(Sequence seq, float t) {
		if (seq.fov.isEmpty()) return 0;
		Sequence.Key prev = null;
		for (Sequence.Key k : seq.fov) {
			if (k.tick() >= t) {
				if (prev == null) return k.value();
				float f = (t - prev.tick()) / Math.max(1.0E-3f, k.tick() - prev.tick());
				return Mth.lerp(f, prev.value(), k.value());
			}
			prev = k;
		}
		return prev.value();
	}

	/**
	 * From one view to another, the place swung round the seated player on an arc (its distance from them and its height eased between the
	 * two), never straight through them: from behind them to in front of them a straight line crosses the body. From or to their eyes
	 * (on the axis) it runs straight out along the other's side. The turn goes the short way round.
	 */
	private static View blend(CinematicClient.Playing p, View a, View b, float f) {
		Vec3 c = p.at;
		double ax = a.pos().x - c.x, az = a.pos().z - c.z, bx = b.pos().x - c.x, bz = b.pos().z - c.z;
		double ra = Math.sqrt(ax * ax + az * az), rb = Math.sqrt(bx * bx + bz * bz);
		double angA = Math.atan2(az, ax), angB = Math.atan2(bz, bx);
		if (ra < 0.05) angA = angB;
		if (rb < 0.05) angB = angA;
		double ang = angA + Math.toRadians(Mth.wrapDegrees(Math.toDegrees(angB - angA))) * f, r = Mth.lerp(f, ra, rb);
		Vec3 pos = new Vec3(c.x + Math.cos(ang) * r, Mth.lerp(f, a.pos().y, b.pos().y), c.z + Math.sin(ang) * r);
		return new View(pos, a.yaw() + Mth.wrapDegrees(b.yaw() - a.yaw()) * f, Mth.lerp(f, a.pitch(), b.pitch()),
				Mth.lerp(f, a.roll(), b.roll()), Mth.lerp(f, a.fov(), b.fov()));
	}

	private CinematicCamera() {}
}
