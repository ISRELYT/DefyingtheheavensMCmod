package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DragonFlight;
import com.example.defyingtheheavens.SpellConstruct;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationProcessor;

/**
 * The Azure Dragon's body laid along its flight (DragonFlight; the user's ask, 2026-10-25: it "looks like its spinning instead of flying"):
 * each frame, each of its body's twenty segments (its model's seg_0 to seg_19, a chain, each its own length back from the last) is turned
 * to lie along the path its head has flown, where the head was as far back along it as that segment is from the head; so its body pours
 * up out of the land after its head and winds after it through the sky as a serpent's does, rather than its whole shape being turned
 * about. A slow ripple runs down it besides. Its head, legs, whiskers and mane keep their own swimming moves (its swim).
 */
final class DragonPose {
	/** Its model's body: this many segments, each this many pixels long (tools/spell-art Wood#azureDragon). */
	private static final int SEGMENTS = 20;
	private static final double SEG_PIXELS = 7;
	/** The ripple down its body: how far it turns (radians), how quick, how many ripples along it. */
	private static final float RIPPLE = 0.05f, RIPPLE_SPEED = 0.22f, RIPPLE_ALONG = 0.55f;

	/** Poses {@code dragon}'s body ({@code bones}, after its animations) along its flight, {@code partial} of this tick in. */
	static void pose(AnimationProcessor<SpellConstruct> bones, SpellConstruct dragon, float partial) {
		float size = dragon.size();
		DragonFlight.Path path = DragonFlight.of(dragon.value(), size);
		// (Drawn where its spell put it a tick ago, eased on toward where it is now: so its body is laid as of then.)
		double t = dragon.flown(partial) - 1;
		double segment = SEG_PIXELS / 16 * dragon.kind().scale * size;
		// The world turned into its model's frame: undoing SpellConstructRenderer#applyRotations (its yaw, then its pitch).
		float yaw = dragon.drawnYaw(partial), pitch = dragon.drawnPitch(partial);
		Matrix3f toModel = new Matrix3f().rotationX((float) Math.toRadians(pitch)).rotateY((float) Math.toRadians(yaw - 180));
		Vec3[] along = new Vec3[SEGMENTS + 1];
		for (int i = 0; i <= SEGMENTS; i++) along[i] = path.behind(t, i * segment);
		CoreGeoBone root = bones.getBone("root");
		if (root != null) {
			root.setRotX(0);
			root.setRotY(0);
			root.setRotZ(0);
		}
		// Down the chain: each segment turned (yaw then pitch, as GeckoLib turns a bone) within the one before it to lie along the path.
		Matrix3f frame = new Matrix3f();
		Vector3f last = new Vector3f(0, 0, 1);
		float age = dragon.tickCount + partial;
		for (int i = 0; i < SEGMENTS; i++) {
			CoreGeoBone bone = bones.getBone("seg_" + i);
			if (bone == null) return;
			Vec3 toward = along[i + 1].subtract(along[i]);
			Vector3f way = toward.lengthSqr() < 1.0e-8 ? new Vector3f(last) : toModel.transform(new Vector3f((float) toward.x, (float) toward.y, (float) toward.z).normalize());
			last.set(way);
			Vector3f local = new Matrix3f(frame).transpose().transform(new Vector3f(way));
			float turn = (float) Math.atan2(local.x, local.z) + RIPPLE * Mth.sin(age * RIPPLE_SPEED - i * RIPPLE_ALONG);
			float lift = (float) Math.asin(Mth.clamp(-local.y, -1, 1));
			bone.setRotX(lift);
			bone.setRotY(turn);
			bone.setRotZ(0);
			frame.rotateY(turn).rotateX(lift);
		}
	}

	private DragonPose() {}
}
