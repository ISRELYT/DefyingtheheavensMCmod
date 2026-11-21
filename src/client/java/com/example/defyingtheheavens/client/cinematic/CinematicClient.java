package com.example.defyingtheheavens.client.cinematic;

import com.example.defyingtheheavens.Cinematics;
import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.Realm;
import com.example.defyingtheheavens.Stage;
import com.example.defyingtheheavens.TwoPaths;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The breakthrough cinematics on this client (the VFX plan, step B1): the ones it has been told of (Cinematics, on the server), each
 * played from the server's start tick, so a late-comer joins it mid-way and the same seed always plays the same. Everything is worked
 * out from the elapsed ticks and the partial tick (Playing#elapsed): smooth at any frame rate, still while a single-player game is
 * paused.
 *
 * This client's own player's cinematic (FULL or SHORT) takes the camera (CinematicCamera, through the camera mixin), the HUD (the
 * letterbox, the title and the skip ring: CinematicHud), the music and the controls, and holds the player where they sat; anyone
 * else's is drawn in the world only (GreyBox), from wherever this player stands. Any failure ends them all and puts everything back.
 */
public final class CinematicClient {
	/** One cinematic this client knows of. */
	public static final class Playing {
		public final int entityId;
		public final UUID player;
		public final Realm realm;
		public final Stage stage;
		public final TwoPaths.Path path;
		public final Cinematics.Version version;
		public final long seed;
		public final Vec3 at;
		public final float facing;
		public final long start;
		public final int length;
		public final Sequence seq;
		public final Probe probe;
		/** Whether it takes this client's camera: the local player's own, FULL or SHORT. */
		public final boolean own;
		/** The world this client saw it begin in: a client that moves to another world drops it. */
		public final ResourceKey<Level> world;
		/** Its own player's view as it began (the camera blends from it, so the take-over never cuts), and the camera they go back to. */
		CinematicCamera.View from;
		CameraType returnTo;

		Playing(int entityId, UUID player, Realm realm, Stage stage, TwoPaths.Path path, Cinematics.Version version, long seed, Vec3 at,
				float facing, long start, int length, Sequence seq, Probe probe, boolean own, ResourceKey<Level> world) {
			this.world = world;
			this.entityId = entityId;
			this.player = player;
			this.realm = realm;
			this.stage = stage;
			this.path = path;
			this.version = version;
			this.seed = seed;
			this.at = at;
			this.facing = facing;
			this.start = start;
			this.length = length;
			this.seq = seq;
			this.probe = probe;
			this.own = own;
		}

		/** Ticks since it began, with the partial tick: everything shown is a function of this. */
		public float elapsed(float partial) {
			Minecraft mc = Minecraft.getInstance();
			return mc.level == null ? 0 : Math.max(0, (mc.level.getGameTime() - start) + partial);
		}
	}

	/** What the world round the start is like (the brief §6.7), read once as it begins. */
	public record Probe(double groundY, double topY, boolean openSky) {
		/** The pillar goes up to the ceiling over the player, or to the clouds (or 100 blocks up where there are none). */
		static Probe of(Level level, Vec3 at) {
			double ceiling = Double.NaN;
			for (int dy = 3; dy < 160 && at.y + dy < level.getMaxBuildHeight(); dy++) {
				if (!level.getBlockState(net.minecraft.core.BlockPos.containing(at.x, at.y + dy, at.z)).isAir()) {
					ceiling = Math.floor(at.y + dy);
					break;
				}
			}
			float clouds = Minecraft.getInstance().level == null ? Float.NaN : Minecraft.getInstance().level.effects().getCloudHeight();
			double sky = Float.isNaN(clouds) ? at.y + 100 : Math.max(at.y + 40, Math.min(clouds, at.y + 140));
			return Double.isNaN(ceiling) ? new Probe(at.y, sky, true) : new Probe(at.y, Math.max(at.y + 4, ceiling), false);
		}
	}

	private static final Map<UUID, Playing> PLAYING = new HashMap<>();
	/** This client's own player's cinematic, while it takes the camera. */
	private static Playing own;
	/** The last cinematic begun here, ended or not (the capture tool's record of which shots it played: VfxRecorder). */
	private static Playing lastBegun;
	private static CameraType cameraBefore;
	/** Ticks Sneak has been held toward a skip. */
	private static int skipHeld;

	public static void register() {
		CinematicSettings.load();
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.CINEMATIC_START, (client, handler, buf, sender) -> {
			int entityId = buf.readVarInt();
			UUID player = buf.readUUID();
			Realm realm = Realm.values()[buf.readVarInt()];
			Stage stage = Stage.values()[buf.readVarInt()];
			TwoPaths.Path path = TwoPaths.Path.values()[buf.readVarInt()];
			Cinematics.Version version = Cinematics.Version.values()[buf.readVarInt()];
			long seed = buf.readLong();
			Vec3 at = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
			float facing = buf.readFloat();
			long start = buf.readLong();
			int length = buf.readVarInt();
			client.execute(() -> safely(() -> begin(entityId, player, realm, stage, path, version, seed, at, facing, start, length)));
		});
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.CINEMATIC_END, (client, handler, buf, sender) -> {
			UUID id = buf.readUUID();
			client.execute(() -> end(id));
		});
		ClientTickEvents.START_CLIENT_TICK.register(client -> safely(CinematicClient::swallowKeys));
		ClientTickEvents.END_CLIENT_TICK.register(client -> safely(CinematicClient::tick));
		// What is drawn in the world: the effects into their own layer (client/fx; the grey box's stand-ins, for what isn't built yet, with
		// them), the torn-up ground's blocks into the world itself. The layer goes over the frame from GameRendererMixin (FxPost).
		com.example.defyingtheheavens.client.fx.Fx.register();
		com.example.defyingtheheavens.client.fx.FxLayer.painter(HeroFx::wants, HeroFx::paint);
		WorldRenderEvents.END.register(com.example.defyingtheheavens.client.fx.FxLayer::onWorldEnd);
		WorldRenderEvents.AFTER_ENTITIES.register(HeroFx::renderWorld);
		// The effects' layer or the post chain failing part-way (logged there): every cinematic ended and everything put back, as here.
		com.example.defyingtheheavens.client.fx.Fx.onFailure(e -> {
			DefyingTheHeavens.LOGGER.error("[DTH-CINEMATIC] the effects failed: every cinematic ended and everything put back");
			boolean mine = own != null;
			clear();
			if (mine) sendSkip();
		});
		// Their textures, layer and post chain made ready on joining (over the first ticks, one a tick) and let go on leaving, so the first
		// cinematic seen never waits on them. While the films are a development feature (Cinematics: -Ddth.dev=true), only a development
		// game makes them ready: no one else holds the targets for a film that can't come.
		ClientTickEvents.END_CLIENT_TICK.register(client -> com.example.defyingtheheavens.client.fx.FxLayer.warmTick());
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			sendPref();
			if (com.example.defyingtheheavens.DevTools.ENABLED) com.example.defyingtheheavens.client.fx.FxLayer.warmUp();
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
			clear();
			com.example.defyingtheheavens.client.fx.FxLayer.close();
			com.example.defyingtheheavens.client.fx.FxPost.close();
		}));
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public ResourceLocation getFabricId() {
				return new ResourceLocation(DefyingTheHeavens.MOD_ID, "breakthroughs");
			}

			@Override
			public void onResourceManagerReload(ResourceManager manager) {
				Sequence.reload(manager);
				// The effects' textures given their mipmaps again, the post chain's programs and targets made anew: ahead of need once the
				// reload is done (or on first use, if a cinematic plays before).
				com.example.defyingtheheavens.client.fx.FxTextures.reloaded();
				com.example.defyingtheheavens.client.fx.FxPost.reloaded();
				com.example.defyingtheheavens.client.fx.FxLayer.close();
				if (com.example.defyingtheheavens.DevTools.ENABLED) com.example.defyingtheheavens.client.fx.FxLayer.warmUp();
			}
		});
	}

	/** The player's setting told to the server, which picks their version (or none) from it. */
	public static void sendPref() {
		var buf = PacketByteBufs.create();
		buf.writeVarInt(CinematicSettings.cinematics.ordinal());
		if (ClientPlayNetworking.canSend(ModPackets.CINEMATIC_PREF)) ClientPlayNetworking.send(ModPackets.CINEMATIC_PREF, buf);
	}

	private static void begin(int entityId, UUID player, Realm realm, Stage stage, TwoPaths.Path path, Cinematics.Version version, long seed,
							  Vec3 at, float facing, long start, int length) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		if (PLAYING.containsKey(player)) end(player); // a second one for the same player: it replaces the first
		boolean local = mc.player != null && mc.player.getUUID().equals(player);
		Sequence seq = Sequence.get(realm.getId(), version == Cinematics.Version.SHORT ? "short" : "full");
		if (seq == null || seq.length != length) {
			DefyingTheHeavens.LOGGER.error("[DTH-CINEMATIC] no playable sequence for {} {} (length {}): handed straight back", realm.getId(), version, length);
			if (local) sendSkip();
			return;
		}
		Playing p = new Playing(entityId, player, realm, stage, path, version, seed, at, facing, start, length, seq, Probe.of(mc.level, at),
				local && version != Cinematics.Version.OBSERVER, mc.level.dimension());
		PLAYING.put(player, p);
		lastBegun = p;
		if (p.own) {
			own = p;
			cameraBefore = mc.options.getCameraType();
			// Where the player's own camera is this moment, whatever they look at or however far behind them it stands: the film starts there.
			net.minecraft.client.Camera camera = mc.gameRenderer.getMainCamera();
			p.from = new CinematicCamera.View(camera.getPosition(), camera.getYRot(), camera.getXRot(), 0, mc.options.fov().get());
			p.returnTo = cameraBefore;
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); // the body drawn, no first-person hand
			mc.getSoundManager().stop(null, SoundSource.MUSIC);
			skipHeld = 0;
		}
	}

	/** The cinematic of {@code player} ends here (its length up, the server's word, a skip, a failure). */
	public static void end(UUID player) {
		Playing p = PLAYING.remove(player);
		if (p != null) HeroFx.forget(p);
		if (p != null && p == own) handBack();
	}

	/** The camera, the HUD and the controls back as they were. */
	private static void handBack() {
		own = null;
		skipHeld = 0;
		Minecraft mc = Minecraft.getInstance();
		if (cameraBefore != null) mc.options.setCameraType(cameraBefore);
		cameraBefore = null;
	}

	/** Every cinematic dropped (the world left, a failure): everything back. */
	public static void clear() {
		PLAYING.clear();
		if (own != null) handBack();
	}

	private static void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			if (!PLAYING.isEmpty()) clear();
			return;
		}
		if (!PLAYING.isEmpty()) {
			// Its length up, or seen in a world this client has since left (the server tells only those in the player's world it ended).
			for (Playing p : new ArrayList<>(PLAYING.values())) {
				if (p.elapsed(0) >= p.length || p.world != mc.level.dimension()) end(p.player);
			}
		}
		Playing mine = mc.player == null ? null : PLAYING.get(mc.player.getUUID());
		if (mine != null) { // held where they sat, facing as they did (their own client, so the server never has to pull them back)
			mc.player.setPos(mine.at);
			mc.player.setDeltaMovement(Vec3.ZERO);
			mc.player.setYRot(mine.facing);
			mc.player.yRotO = mine.facing;
			mc.player.setXRot(0);
			mc.player.xRotO = 0;
			mc.player.yBodyRot = mc.player.yBodyRotO = mine.facing;
			mc.player.yHeadRot = mc.player.yHeadRotO = mine.facing;
		}
		if (own == null) return;
		if (mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		// The music kept silent through the sound engine: the music manager's own stop adds to its wait for the next song at every call.
		mc.getSoundManager().stop(null, SoundSource.MUSIC);
		float t = own.elapsed(0);
		float shake = 0;
		for (Sequence.Shake s : own.seq.shakes) {
			if (t >= s.from() && t < s.to()) shake = Math.max(shake, s.start() + (s.end() - s.start()) * (t - s.from()) / Math.max(1, s.to() - s.from()));
		}
		if (shake > 0) com.example.defyingtheheavens.client.ClientSpellFx.shakeAtLeast(shake * CinematicSettings.shake / 100f);
		// Hold Sneak a second to skip: the ring fills (CinematicHud), and at full the server lets the hold go.
		if (mc.options.keyShift.isDown() && mc.screen == null) {
			if (++skipHeld >= 20) {
				sendSkip();
				end(own.player);
			}
		} else {
			skipHeld = 0;
		}
	}

	/** The keys a held player keeps: Sneak (to skip), chat and commands, the player list, screenshots and full screen. */
	private static boolean kept(Options o, KeyMapping k) {
		return k == o.keyShift || k == o.keyChat || k == o.keyCommand || k == o.keyPlayerList || k == o.keyScreenshot || k == o.keyFullscreen;
	}

	/**
	 * While this client's player is held, at each tick's start (before vanilla reads its keys and the mod reads its own): every other
	 * key's presses thrown away and attack and use let go, so the player strikes, uses, picks, drops, swaps and opens nothing, and the
	 * mod's keys (abilities, spells, meditation) do nothing. The mouse's turning and scrolling: CinematicMouseMixin. The movement keys:
	 * SoulInputMixin. (The server refuses a held player's acts too: Cinematics, ModPackets#unlessHeld.)
	 */
	private static void swallowKeys() {
		if (!holdsLocal()) return;
		Options o = Minecraft.getInstance().options;
		for (KeyMapping k : o.keyMappings) {
			if (kept(o, k)) continue;
			while (k.consumeClick()) {
				// thrown away
			}
		}
		o.keyAttack.setDown(false);
		o.keyUse.setDown(false);
	}

	/** The cinematic's own HUD in place of the game's (GuiMixin), guarded as everything of it is: a failure ends it and puts all back. */
	public static void hud(GuiGraphics g, float partial) {
		safely(() -> CinematicHud.render(g, partial));
	}

	private static void sendSkip() {
		if (ClientPlayNetworking.canSend(ModPackets.CINEMATIC_SKIP)) ClientPlayNetworking.send(ModPackets.CINEMATIC_SKIP, PacketByteBufs.create());
	}

	/** Anything of the cinematics that throws ends them all, puts everything back, and is logged: the player is never left trapped. */
	static void safely(Runnable r) {
		try {
			r.run();
		} catch (RuntimeException e) {
			DefyingTheHeavens.LOGGER.error("[DTH-CINEMATIC] failed: every cinematic ended and everything put back", e);
			boolean mine = own != null;
			clear();
			if (mine) sendSkip();
		}
	}

	// --- For the mixins and the renderers ---

	/** This client's own cinematic's camera at this moment, or null when it has none (or it just failed and was ended). */
	public static CinematicCamera.View view(float partial) {
		if (own == null) return null;
		CinematicCamera.View[] out = {null};
		safely(() -> out[0] = CinematicCamera.view(own, own.elapsed(partial)));
		return own == null ? null : out[0];
	}

	/** This client's own cinematic, while it takes the camera, or null. */
	public static Playing own() {
		return own;
	}

	/** The last cinematic begun on this client, ended or not, or null. */
	public static Playing lastBegun() {
		return lastBegun;
	}

	/** All the cinematics this client knows of (drawn in the world, GreyBox). */
	public static Collection<Playing> playing() {
		return PLAYING.values();
	}

	/** Whether {@code player} sits held in a cinematic (drawn in the lotus pose: LotusPose). */
	public static boolean seated(UUID player) {
		return PLAYING.containsKey(player);
	}

	/** Whether this client's own player is held (the movement keys do nothing: SoulInputMixin). */
	public static boolean holdsLocal() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && PLAYING.containsKey(mc.player.getUUID());
	}

	/** How far the skip ring has filled, 0..1. */
	public static float skipProgress() {
		return Math.min(1, skipHeld / 20f);
	}

	private CinematicClient() {}
}
