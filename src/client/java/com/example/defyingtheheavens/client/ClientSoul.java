package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Formation;
import com.example.defyingtheheavens.Formations;
import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.Realm;
import com.example.defyingtheheavens.SoulProjection;
import com.example.defyingtheheavens.Stage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * The client half of {@link SoulProjection}: the local player's own soul (from the server's SOUL_SELF packet), and the souls of
 * others this client perceives (SOUL_SEEN, which the server sends only to those who do).
 * <p>
 * Out of the body the soul flies free of gravity, along the camera's whole line of sight (pitch included: look up and press
 * forward to rise), with its own momentum and damping: forward/back and the strafe keys steer it, jump and sneak lift and lower
 * it, sprint doubles its pace. Its tether lets it go only so far ({@link SoulProjection#TETHER_RADII}), stretching as it nears its
 * end; a formation's barrier it can't pass stops it at the shell. Coming home, it is drawn back along the tether to the body.
 * While its soul gathers to leave, the cultivator sits still: the movement keys do nothing.
 * <p>
 * A Heavenly Being (or above) with Qi Sense on strikes at a soul's tether by swinging at it ({@link #onPreAttack}).
 */
public final class ClientSoul {
	/** Top speed through the air (blocks a tick), about creative flight's; sprinting doubles it. */
	private static final double SPEED = 0.55;
	private static final double SPRINT = 2.0;
	/** Share of its speed the soul keeps each tick: its momentum. */
	private static final double DAMPING = 0.86;
	/** From this share of the tether on, it stretches against the soul going further. */
	private static final double STRETCH_FROM = 0.85;
	/** Perceived souls are forgotten this long after the server last spoke of them. */
	private static final long SEEN_EXPIRY_MS = 1500;

	/**
	 * A soul this client perceives: its body's place, the beast it rides, its tether's strength (-1: none to draw, its body in another
	 * realm), and whether it is in a person's body ({@code inside}: that body is drawn veiled in violet, SoulRenderer#drawVeil, and the
	 * player is no soul to this client).
	 */
	public record Seen(Vec3 body, int host, float tether, boolean inside, long at) {}

	private static SoulProjection.State state = SoulProjection.State.NONE;
	private static int ticksLeft;
	private static Vec3 body = Vec3.ZERO;
	private static int radius;
	private static float bodyHealth, bodyMax, tether = 1;
	/** The soul's own pool and its whole (its path's: qi, or ghostly qi when {@code ownGhost}), and whether it rose at the last word. */
	private static float ownQi, ownMax;
	private static boolean ownGhost, ownRising;
	/** Whether its own body sits in this realm (a person's body it is in may have carried it to another: no tether to draw). */
	private static boolean bodyHere = true;
	/** Whether the soul itself can tear space (in a body that can't, it tears with its own: Possession#soulTears). */
	private static boolean soulTears;
	private static int host = -1;
	private static Vec3 velocity = Vec3.ZERO;
	private static final Map<Integer, Seen> SEEN = new HashMap<>();

	public static void register() {
		SoulProjection.clientSoul = ClientSoul::isSoul;
		SoulProjection.clientChanneling = player -> player == Minecraft.getInstance().player && state == SoulProjection.State.PROJECTING;
		// In another's body (Possession), the synced record is the body's: Soul Projection is open on it, to leave by.
		com.example.defyingtheheavens.Possession.clientBorrowed = c -> state == SoulProjection.State.INHABITING && c == ClientCultivationData.get();
		com.example.defyingtheheavens.Possession.clientSoulTears = c -> soulTears && state == SoulProjection.State.INHABITING && c == ClientCultivationData.get();
		ClientPreAttackCallback.EVENT.register(ClientSoul::onPreAttack);
	}

	// --- State ---

	/** SOUL_SELF: the local player's soul. */
	public static void self(SoulProjection.State newState, int ticks, Vec3 newBody, int newRadius, float health, float max, int newHost, float newTether,
							float newOwnQi, float newOwnMax, boolean newOwnGhost, boolean newBodyHere, boolean newSoulTears) {
		bodyHere = newBodyHere;
		soulTears = newSoulTears;
		boolean wasOut = isOut();
		state = newState;
		ticksLeft = ticks;
		body = newBody;
		radius = newRadius;
		bodyHealth = health;
		bodyMax = max;
		host = newHost;
		tether = newTether;
		ownRising = newOwnQi > ownQi + 1.0e-3f;
		ownQi = newOwnQi;
		ownMax = newOwnMax;
		ownGhost = newOwnGhost;
		if (isOut() != wasOut) velocity = Vec3.ZERO;
		Minecraft mc = Minecraft.getInstance();
		if (!isOut() && mc.player != null) mc.player.noPhysics = false; // back in the body: solid again at once
	}

	/** SOUL_SEEN: another soul this client perceives (or, {@code out} false, one that went home). */
	public static void seen(int entityId, boolean out, Vec3 seenBody, int seenHost, float seenTether, boolean inside) {
		if (out) SEEN.put(entityId, new Seen(seenBody, seenHost, seenTether, inside, System.currentTimeMillis()));
		else SEEN.remove(entityId);
	}

	/** The local soul is out of the body (flying, possessing, or coming back). */
	public static boolean isOut() {
		return state == SoulProjection.State.PROJECTED || state == SoulProjection.State.RECALLING;
	}

	public static SoulProjection.State state() { return state; }
	public static int ticksLeft() { return ticksLeft; }
	public static Vec3 body() { return body; }
	public static int radius() { return radius; }
	public static float bodyHealth() { return bodyHealth; }
	public static float bodyMax() { return bodyMax; }
	public static float tether() { return tether; }
	public static float ownQi() { return ownQi; }
	public static float ownMax() { return ownMax; }
	/** The soul's own pool is ghostly qi (a soul born of the ghost path). */
	public static boolean ownGhost() { return ownGhost; }
	public static boolean ownRising() { return ownRising; }
	public static boolean bodyHere() { return bodyHere; }
	public static int host() { return host; }

	/** The local soul is in a person's body (Possession): it plays it as a player, solid, on the ground. */
	public static boolean isInside() {
		return state == SoulProjection.State.INHABITING;
	}

	/** Whether {@code player} is a projected soul on this client: the local player's own, or one the server says is perceived. */
	public static boolean isSoul(Player player) {
		Minecraft mc = Minecraft.getInstance();
		if (player == mc.player) return isOut();
		Seen seen = seenSoul(player.getId());
		return seen != null && !seen.inside(); // one in a person's body is that body here (only veiled)
	}

	/** What this client knows of another's soul, or null (not perceived, or gone home). */
	public static Seen seenSoul(int entityId) {
		Seen seen = SEEN.get(entityId);
		if (seen != null && System.currentTimeMillis() - seen.at() > SEEN_EXPIRY_MS) {
			SEEN.remove(entityId);
			return null;
		}
		return seen;
	}

	public static Map<Integer, Seen> seenSouls() {
		return SEEN;
	}

	/**
	 * How clearly the local player sees {@code soul}: whole for their own and for Heavenly Beings and above, a haze for Nascent
	 * Soul cultivators that clears with each stage (a quarter at Early, whole at Grand Perfection); 0 without Qi Sense open. The
	 * Ghost Eye sees them too, outside the Netherworld (GhostEye#soulClarity), whichever is clearer.
	 */
	public static float clarity(Player soul) {
		Minecraft mc = Minecraft.getInstance();
		if (soul == mc.player) return 1;
		if (mc.player == null || mc.player.isSpectator()) return 1;
		float eye = GhostEye.soulClarity();
		if (!QiSenseClientHandler.isActive()) return eye;
		PlayerCultivation c = ClientCultivationData.get();
		int nascent = PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY);
		int rank = c.effectiveRank();
		if (c.isMortal() || rank < nascent) return eye;
		return Math.max(eye, Math.min(1, (rank - nascent + 1) / 4.0f));
	}

	/** Whether the local player sees souls' tethers: their own always; others' as a Heavenly Being or above with Qi Sense open. */
	public static boolean seesTethers() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return false;
		if (mc.player.isSpectator()) return true;
		PlayerCultivation c = ClientCultivationData.get();
		return !c.isMortal() && c.effectiveRank() >= PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.EARLY) && QiSenseClientHandler.isActive();
	}

	// --- Every tick ---

	public static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null) return;
		if (ticksLeft > 0 && (state == SoulProjection.State.PROJECTING || state == SoulProjection.State.RECALLING)) ticksLeft--;
		long now = System.currentTimeMillis(); // no word for a while (gone into another world, say): forgotten
		SEEN.values().removeIf(seen -> now - seen.at() > SEEN_EXPIRY_MS);
		// A soul carries nothing in hand and opens nothing: no inventory, no chest. Only a seized body's own things, from inside it
		// (VesselScreen).
		if (isOut() && mc.screen instanceof AbstractContainerScreen<?> && !(mc.screen instanceof VesselScreen)) mc.player.closeContainer();
	}

	/**
	 * The local soul's flight (from SoulTravelMixin, in place of Player#travel): free of gravity, along the line of sight, with
	 * momentum, held within the tether and outside barriers it can't pass; coming home, drawn back to the body.
	 */
	public static void fly(LocalPlayer player) {
		Minecraft mc = Minecraft.getInstance();
		Vec3 home = body.add(0, 1, 0);
		if (state == SoulProjection.State.RECALLING) {
			Vec3 to = body.add(0, 0.1, 0).subtract(player.position());
			double steps = Math.max(1, ticksLeft);
			velocity = to.scale(1 / steps);
			if (velocity.length() > 2.5) velocity = velocity.normalize().scale(2.5);
		} else {
			float forward = player.input.forwardImpulse, strafe = player.input.leftImpulse;
			float up = (player.input.jumping ? 1 : 0) - (player.input.shiftKeyDown ? 1 : 0);
			Vec3 look = player.getViewVector(1.0f);
			float yaw = player.getYRot() * ((float) Math.PI / 180f);
			Vec3 left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
			Vec3 wish = look.scale(forward).add(left.scale(strafe)).add(0, up, 0);
			if (wish.lengthSqr() > 1) wish = wish.normalize();
			double speed = SPEED * (mc.options.keySprint.isDown() ? SPRINT : 1);
			velocity = velocity.scale(DAMPING).add(wish.scale(speed * (1 - DAMPING)));
			velocity = keepToTether(player.position().add(0, 1, 0), home, velocity);
			velocity = keepToBarriers(player, velocity);
		}
		// Never into the void below the world.
		if (player.getY() + velocity.y < player.level().getMinBuildHeight() - 2 && velocity.y < 0) velocity = new Vec3(velocity.x, 0, velocity.z);
		player.setDeltaMovement(velocity);
		player.move(MoverType.SELF, velocity);
		player.fallDistance = 0;
		player.setOnGround(false);
		player.calculateEntityAnimation(false);
	}

	/** The tether stretches against the soul past {@link #STRETCH_FROM} of its reach, and holds it at the end. */
	private static Vec3 keepToTether(Vec3 at, Vec3 home, Vec3 v) {
		if (radius <= 0) return v;
		Vec3 next = at.add(v);
		Vec3 out = next.subtract(home);
		double distance = out.length();
		if (distance < radius * STRETCH_FROM || distance < 1.0e-4) return v;
		out = out.scale(1 / distance);
		double outward = v.dot(out);
		double strain = Math.min(1, (distance - radius * STRETCH_FROM) / (radius * (1 - STRETCH_FROM)));
		if (outward > 0) v = v.subtract(out.scale(outward * strain));
		if (distance > radius) v = v.subtract(out.scale((distance - radius) * 0.5)); // past the end: drawn back
		return v;
	}

	/** A barrier the soul may not pass and can't break (see SoulBarriers) stops it at the shell, sliding along it. */
	private static Vec3 keepToBarriers(LocalPlayer player, Vec3 v) {
		Vec3 at = player.position().add(0, 1, 0);
		for (Formation formation : ClientFormations.nearby()) {
			if (formation.allows(player) || Formations.canBreak(player, formation)) continue;
			Vec3 center = Vec3.atCenterOf(formation.center);
			double shell = formation.getRadius() + 0.5;
			double now = at.distanceTo(center);
			double next = at.add(v).distanceTo(center);
			boolean inside = now < shell;
			double margin = 1.2;
			if (inside ? next > shell - margin : next < shell + margin) {
				Vec3 out = at.subtract(center);
				if (out.lengthSqr() < 1.0e-4) continue;
				out = out.normalize();
				double radial = v.dot(out);
				// Stop the part of the motion that would cross; keep the rest (it slides along the shell).
				if (inside ? radial > 0 : radial < 0) v = v.subtract(out.scale(radial));
			}
		}
		return v;
	}

	// --- Striking a tether ---

	/**
	 * A swing (a click, not a held button) by a Heavenly Being or above with Qi Sense open that passes through a soul's tether
	 * before anything else in its path strikes the tether (the server checks the reach and aim again). Anything aimed at
	 * nearer is struck as usual.
	 */
	private static boolean onPreAttack(Minecraft mc, LocalPlayer player, int clicks) {
		if (clicks == 0 || isSoul(player) || !seesTethers() || player.isSpectator()) return false;
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		double nearest = mc.hitResult != null && mc.hitResult.getType() != HitResult.Type.MISS ? mc.hitResult.getLocation().distanceTo(eye) : Double.MAX_VALUE;
		int target = -1;
		for (Map.Entry<Integer, Seen> entry : SEEN.entrySet()) {
			if (entry.getValue().tether() < 0 || !(mc.level.getEntity(entry.getKey()) instanceof Player soul)) continue; // no tether here
			Vec3[] ends = tetherEnds(soul, entry.getValue(), 1.0f);
			if (SoulProjection.rayToSegment(eye, look, SoulProjection.TETHER_STRIKE_REACH, ends[0], ends[1]) > SoulProjection.TETHER_STRIKE_WIDTH) continue;
			// Only if the tether is nearer than whatever the crosshair is on (a wall, the soul itself, anyone else).
			if (strikeDistance(eye, look, ends[0], ends[1]) < nearest - 0.3) {
				target = entry.getKey();
				break;
			}
		}
		if (target < 0) return false;
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(target);
		ClientPlayNetworking.send(ModPackets.SOUL_TETHER_STRIKE, buf);
		player.swing(InteractionHand.MAIN_HAND);
		player.resetAttackStrengthTicker();
		return true;
	}

	/** How far along the line of sight the tether a-b passes closest to it. */
	private static double strikeDistance(Vec3 eye, Vec3 look, Vec3 a, Vec3 b) {
		double best = Double.MAX_VALUE, bestAlong = SoulProjection.TETHER_STRIKE_REACH;
		for (int i = 0; i <= 24; i++) {
			Vec3 p = a.add(b.subtract(a).scale(i / 24.0));
			double along = Math.max(0, Math.min(SoulProjection.TETHER_STRIKE_REACH, p.subtract(eye).dot(look)));
			double d = eye.add(look.scale(along)).distanceTo(p);
			if (d < best) {
				best = d;
				bestAlong = along;
			}
		}
		return bestAlong;
	}

	/** Where a soul's tether runs: from its body's chest to the soul (or the body it possesses), at {@code partial}. */
	public static Vec3[] tetherEnds(Player soul, Seen seen, float partial) {
		Vec3 from = (seen == null ? body : seen.body()).add(0, 1.0, 0);
		Entity holder = soul.isPassenger() && soul.getVehicle() != null ? soul.getVehicle() : soul;
		Vec3 to = holder.getPosition(partial).add(0, holder.getBbHeight() * 0.55, 0);
		return new Vec3[] {from, to};
	}

	/** Disconnect: nothing carries over to the next world. */
	public static void clear() {
		state = SoulProjection.State.NONE;
		ticksLeft = 0;
		body = Vec3.ZERO;
		radius = 0;
		host = -1;
		ownQi = ownMax = 0;
		ownGhost = ownRising = false;
		bodyHere = true;
		soulTears = false;
		velocity = Vec3.ZERO;
		SEEN.clear();
	}

	private ClientSoul() {}
}
