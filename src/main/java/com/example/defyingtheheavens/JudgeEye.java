package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Eye in the void over the Soul River: the eye of the Chief Judge of the Netherworld, a True Immortal. It hangs off to one
 * side of the river, square to its flow, so a traveller has to turn to see it; it watches the dead drift by, now and then looks
 * straight at the living, and blinks. To meet its gaze is death: the longer a traveller looks at it ({@link #LOOK_CONE}), the
 * more their sight warps and the louder the whispers grow (the client's, see ClientJudgeGaze), until after
 * {@link #GAZE_SECONDS} the gaze unmakes them: their cultivation is stripped back to Qi Refining Early, whatever their realm, and
 * they die. Looking away lets it ebb ({@link #RECOVER_SECONDS} from the brink to nothing). Nothing below a True Immortal is
 * spared ({@link #isImmune}); creative and spectator players aren't touched.
 * <p>
 * Where it is and when it blinks or attends are pure functions of the game time, so the server judging and the client
 * drawing agree.
 */
public final class JudgeEye {
	/** Its place on the sky: square to the river (which runs along X), on its +Z side, a little above the horizon. */
	public static final Vec3 DIRECTION = new Vec3(0, Math.sin(Math.toRadians(14)), Math.cos(Math.toRadians(14)));
	/** Looking within this many degrees of its centre is meeting its gaze (it spans about 50 degrees by 20). */
	public static final double LOOK_CONE = 22;
	/** Seconds of meeting its gaze, from nothing, that kill. */
	public static final double GAZE_SECONDS = 10;
	/** Seconds, looking away, for a gaze at the brink to ebb to nothing. */
	public static final double RECOVER_SECONDS = 4;
	/** At these shares of the gaze: the warning; the warping (Nausea); the dark closing in (Darkness). */
	public static final double WARN_AT = 0.2, NAUSEA_AT = 0.35, DARKNESS_AT = 0.65;
	/** It blinks about this often (ticks), each blink this long; while closed, it sees nobody. */
	public static final int BLINK_EVERY = 260, BLINK_TICKS = 10;
	/** Every this many ticks it turns to look at the traveller, for the last part of the cycle. */
	public static final int ATTENTION_EVERY = 440;

	private static final Map<UUID, Double> GAZE = new HashMap<>();
	private static final Map<UUID, Integer> SENT = new HashMap<>();

	/** How open it is, 0 to 1: open but for a blink now and then, at a different moment each time. */
	public static float openness(double time) {
		int cycle = Mth.floor(time / BLINK_EVERY);
		float offset = Mth.frac(Mth.sin(cycle * 12.9898f) * 43758.547f) * (BLINK_EVERY - BLINK_TICKS * 3);
		double t = time - (double) cycle * BLINK_EVERY - offset;
		if (t < 0 || t > BLINK_TICKS) return 1;
		return 1 - Mth.sin((float) (Math.PI * t / BLINK_TICKS));
	}

	/** How fully it is looking at the traveller of its own accord, 0 to 1: once a cycle, for a few seconds. */
	public static float attention(double time) {
		double t = time % ATTENTION_EVERY;
		if (t < 300) return 0;
		if (t < 330) return smooth((float) (t - 300) / 30);
		if (t < 410) return 1;
		return 1 - smooth((float) (t - 410) / 30);
	}

	private static float smooth(float t) {
		t = Mth.clamp(t, 0, 1);
		return t * t * (3 - 2 * t);
	}

	/** Whether someone looking along {@code view} (a unit vector) is meeting its gaze. */
	public static boolean meets(Vec3 view) {
		return view.dot(DIRECTION) >= Math.cos(Math.toRadians(LOOK_CONE));
	}

	/**
	 * Who it spares. Only a True Immortal could meet a True Immortal's gaze, and nobody can cultivate that far yet; when they
	 * can, this is where they become immune.
	 */
	public static boolean isImmune(PlayerCultivation c) {
		return false;
	}

	/** The gaze's share, 0 to 1, after one tick of looking ({@code looking}) or not. */
	public static double step(double gaze, boolean looking) {
		return Mth.clamp(gaze + (looking ? 1.0 / (GAZE_SECONDS * 20) : -1.0 / (RECOVER_SECONDS * 20)), 0, 1);
	}

	/** One tick of it for a traveller on the river (SoulRiver). */
	static void tick(ServerPlayer p) {
		UUID id = p.getUUID();
		double before = GAZE.getOrDefault(id, 0.0);
		boolean exempt = p.isCreative() || p.isSpectator() || isImmune(CultivationManager.get(p));
		boolean looking = !exempt && openness(p.level().getGameTime()) > 0.3f && meets(p.getViewVector(1.0f));
		double gaze = step(before, looking);
		if (gaze <= 0 && before <= 0) return;
		GAZE.put(id, gaze);
		if (before < WARN_AT && gaze >= WARN_AT) {
			p.sendSystemMessage(Component.translatable(ModLang.MSG_JUDGE_WARNING).withStyle(ChatFormatting.DARK_RED));
		}
		if (gaze >= NAUSEA_AT) p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false, false));
		if (gaze >= DARKNESS_AT) p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false, false));
		int shown = (int) Math.round(gaze * 100);
		if (!SENT.containsKey(id) || SENT.get(id) != shown || p.tickCount % 10 == 0) {
			SENT.put(id, shown);
			ModPackets.sendJudgeGaze(p, (float) gaze);
		}
		if (gaze >= 1.0) judge(p);
	}

	/**
	 * The gaze has unmade them: their cultivation stripped back to Qi Refining Early, their heaven methods' layers with it (a mortal
	 * has none to lose), and death.
	 */
	private static void judge(ServerPlayer p) {
		forget(p);
		PlayerCultivation c = CultivationManager.get(p);
		boolean mortal = c.isMortal();
		if (!mortal) {
			c.setBackTo(Realm.QI_REFINING, Stage.EARLY);
			c.prepareBreakthrough(null, 0);
			c.setQi(0);
			CultivationManager.refresh(p);
		}
		p.sendSystemMessage(Component.translatable(mortal ? ModLang.MSG_JUDGE_MORTAL : ModLang.MSG_JUDGE_STRIPPED).withStyle(ChatFormatting.DARK_RED));
		p.hurt(ModDamageTypes.judgeGaze(p.level()), Float.MAX_VALUE);
		DefyingTheHeavens.LOGGER.info("{} met the Chief Judge's gaze on the Soul River{}", p.getScoreboardName(), mortal ? "" : "; stripped to Qi Refining Early");
	}

	/** Leaving the river, or dying: the gaze is gone (and the client told so). */
	static void forget(ServerPlayer p) {
		if (GAZE.remove(p.getUUID()) != null) ModPackets.sendJudgeGaze(p, 0);
		SENT.remove(p.getUUID());
	}

	public static void forget(UUID id) {
		GAZE.remove(id);
		SENT.remove(id);
	}

	/** The share of the gaze on this player (tests). */
	public static double gaze(UUID id) {
		return GAZE.getOrDefault(id, 0.0);
	}

	private JudgeEye() {}
}
