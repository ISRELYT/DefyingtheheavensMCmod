package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Ability;
import com.example.defyingtheheavens.ConsciousnessDomainHandler;
import com.example.defyingtheheavens.CultivatorEntity;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.Realm;
import com.example.defyingtheheavens.Stage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * The client half of the Consciousness Domain ({@link ConsciousnessDomainHandler}), while the ability is on:
 * <ul>
 *   <li>the domain itself, a translucent blue sphere around the player, and any other cultivator's domain touching it in
 *   red, drawn on {@link SenseOverlay} so Qi Sense's monochrome doesn't grey them. From inside a sphere its fill is kept
 *   faint (it would tint everything beyond it) and a lattice of latitude and longitude lines marks the boundary instead;</li>
 *   <li>every living thing inside the domain outlined through walls (vanilla's glowing outline, through MinecraftMixin and
 *   LevelRendererMixin; EntityRendererMixin keeps them drawn past vanilla's entity render distance): mortals white,
 *   cultivators red;</li>
 *   <li>each cultivator's realm and stage over their head, drawn on the HUD at a fixed size so it can be read across a
 *   10-chunk domain;</li>
 *   <li>a banner and a chime when another cultivator's domain first touches this one.</li>
 * </ul>
 */
public final class ConsciousnessRenderer {
	/** RGB and alpha of the player's own domain, and of anyone else's. */
	private static final float[] OWN = {0.2f, 0.5f, 1.0f};
	private static final float[] OTHER = {1.0f, 0.2f, 0.2f};
	private static final float FILL_ALPHA = 0.3f;
	/** The fill's alpha seen from inside: every view ray crosses the shell, so more would tint the whole world. */
	private static final float INSIDE_FILL_ALPHA = 0.06f;
	private static final float GRID_ALPHA = 0.3f;
	private static final int MORTAL_OUTLINE = 0xFFFFFF;
	private static final int CULTIVATOR_OUTLINE = 0xFF2B2B;
	private static final int LABEL_COLOR = 0xFF7A7A;
	private static final int LABEL_PRESSED_COLOR = 0xC04848;
	/** Sphere mesh: bands from pole to pole and segments around. */
	private static final int LAT = 24;
	private static final int LON = 48;
	/** Lattice lines every this many degrees, this many radians to either side (about 3 pixels wide from the middle). */
	private static final int GRID_STEP = 15;
	private static final float GRID_HALF_WIDTH = 0.0018f;
	/** Server data older than this is dropped (the ability went off, or the dimension changed). */
	private static final long DATA_EXPIRY_MS = 1500;
	private static final long ALERT_MS = 4500;

	/** A cultivator in the domain, as the server last described them. */
	public record Sensed(int entityId, Realm realm, Stage stage, boolean pressed) {}

	/** Another domain touching this one; the position is used when its owner isn't loaded on this client. */
	public record Domain(int entityId, double x, double y, double z, float radius) {}

	private static final Int2ObjectOpenHashMap<Sensed> SENSED = new Int2ObjectOpenHashMap<>();
	private static List<Domain> domains = List.of();
	private static List<Domain> previousDomains = List.of();
	private static long lastUpdate;
	private static int ticksSinceUpdate;
	/** This tick's outlines: entity id -> colour. */
	private static final Int2IntOpenHashMap OUTLINES = new Int2IntOpenHashMap();
	/** The own domain's drawn radius and strength, eased so a stage-up grows it and switching it fades it. */
	private static float shownRadius = -1;
	private static float strength;
	private static long lastFrame;

	// The last world frame's camera, for placing realm labels on the HUD.
	private static final Matrix4f VIEW_PROJECTION = new Matrix4f();
	private static Vec3 viewCamera;
	private static float viewPartial;

	private static Component alertDetail;
	private static long alertStart;

	public static boolean isActive() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && !mc.player.isSpectator() && ClientCultivationData.get().isAbilityActive(Ability.CONSCIOUSNESS_DOMAIN);
	}

	private static double radius() {
		return ConsciousnessDomainHandler.radius(ClientCultivationData.get());
	}

	/** From the CONSCIOUSNESS_DOMAIN packet. */
	public static void update(List<Sensed> cultivators, List<Domain> touching) {
		SENSED.clear();
		for (Sensed sensed : cultivators) SENSED.put(sensed.entityId(), sensed);
		previousDomains = domains;
		domains = touching;
		lastUpdate = Util.getMillis();
		ticksSinceUpdate = 0;
	}

	/** From the CONSCIOUSNESS_ALERT packet: another domain has just touched this one. */
	public static void alert(int entityId, String name, Realm realm, Stage stage) {
		if (!isActive()) return;
		alertDetail = Component.translatable(ModLang.CONSCIOUSNESS_ALERT_DETAIL, name, PlayerCultivation.rankName(realm, stage));
		alertStart = Util.getMillis();
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6f, 1.0f));
	}

	public static void tick(Minecraft mc) {
		OUTLINES.clear();
		ticksSinceUpdate++;
		if (Util.getMillis() - lastUpdate > DATA_EXPIRY_MS) {
			SENSED.clear();
			domains = previousDomains = List.of();
		}
		if (!isActive() || mc.level == null) {
			if (strength <= 0) shownRadius = -1; // kept while the sphere fades out
			return;
		}
		float radius = (float) radius();
		shownRadius = shownRadius < 0 ? radius : shownRadius + (radius - shownRadius) * 0.15f;

		Player self = mc.player;
		Vec3 center = ConsciousnessDomainHandler.center(self);
		double radiusSqr = radius * radius;
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (entity == self || !(entity instanceof LivingEntity) || entity instanceof ArmorStand || !entity.isAlive() || entity.isSpectator()) continue;
			if (entity.getBoundingBox().getCenter().distanceToSqr(center) > radiusSqr) continue;
			OUTLINES.put(entity.getId(), CultivatorEntity.isCultivator(entity) ? CULTIVATOR_OUTLINE : MORTAL_OUTLINE);
		}
	}

	/** For MinecraftMixin: inside the domain, so it shows through walls. */
	public static boolean isHighlighted(Entity entity) {
		return !OUTLINES.isEmpty() && OUTLINES.containsKey(entity.getId());
	}

	/** For LevelRendererMixin: the outline colour of an entity {@link #isHighlighted}. */
	public static int outlineColor(Entity entity) {
		return OUTLINES.getOrDefault(entity.getId(), MORTAL_OUTLINE);
	}

	// --- Domains ---

	public static boolean hasDomains() {
		updateStrength();
		return strength > 0 && shownRadius > 0;
	}

	/** Eases the domain in and out as the ability is switched; once per frame. */
	private static void updateStrength() {
		long now = Util.getMillis();
		float seconds = Math.min(0.25f, (now - lastFrame) / 1000f);
		lastFrame = now;
		float target = isActive() ? 1 : 0;
		strength = target > strength ? Math.min(1, strength + seconds * 2) : Math.max(0, strength - seconds * 2);
	}

	/** Draws the domains on {@link SenseOverlay}, which sets up blending against the world's depth. */
	static void renderDomains(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null || mc.player == null) return;
		float partial = context.tickDelta();
		Vec3 camera = context.camera().getPosition();
		Matrix4f matrix = context.matrixStack().last().pose();
		float time = level.getGameTime() + partial;
		float pulse = (0.85f + 0.15f * Mth.sin(time * 0.05f)) * strength * strength * (3 - 2 * strength);

		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		Vec3 own = ConsciousnessDomainHandler.center(mc.player).subtract(mc.player.position()).add(mc.player.getPosition(partial));
		sphere(buffer, matrix, own.subtract(camera), shownRadius, OWN, pulse);
		float progress = Math.min(1, (ticksSinceUpdate + partial) / 10f); // packets come every 10 ticks
		for (Domain domain : domains) {
			Vec3 center;
			Entity owner = level.getEntity(domain.entityId());
			if (owner != null) {
				center = ConsciousnessDomainHandler.center(owner).subtract(owner.position()).add(owner.getPosition(partial));
			} else {
				Domain before = previousDomains.stream().filter(d -> d.entityId() == domain.entityId()).findFirst().orElse(domain);
				center = new Vec3(Mth.lerp(progress, before.x(), domain.x()), Mth.lerp(progress, before.y(), domain.y()),
						Mth.lerp(progress, before.z(), domain.z()));
			}
			sphere(buffer, matrix, center.subtract(camera), domain.radius(), OTHER, pulse);
		}
		BufferUploader.drawWithShader(buffer.end());
	}

	/**
	 * One domain: a shell of radius {@code r} around {@code c} (camera-relative) with a lattice on it. Seen from outside only
	 * the near half is drawn (the far half would double the colour), brighter toward the rim; from inside, a faint fill.
	 */
	private static void sphere(BufferBuilder buffer, Matrix4f matrix, Vec3 c, float r, float[] rgb, float strength) {
		if (r <= 0 || strength <= 0) return;
		boolean inside = c.lengthSqr() < (double) r * r;
		float fill = (inside ? INSIDE_FILL_ALPHA : FILL_ALPHA) * strength;
		float grid = GRID_ALPHA * strength;
		float[] gridRgb = {Mth.lerp(0.3f, rgb[0], 1), Mth.lerp(0.3f, rgb[1], 1), Mth.lerp(0.3f, rgb[2], 1)};
		float latStep = Mth.PI / LAT;
		float lonStep = Mth.TWO_PI / LON;
		for (int i = 0; i < LAT; i++) {
			float lat0 = -Mth.HALF_PI + i * latStep;
			for (int j = 0; j < LON; j++) {
				patch(buffer, matrix, c, r, lat0, lat0 + latStep, j * lonStep, (j + 1) * lonStep, rgb, fill, inside);
			}
		}
		float halfWidth = GRID_HALF_WIDTH;
		for (int degrees = -90 + GRID_STEP; degrees < 90; degrees += GRID_STEP) {
			float lat = degrees * Mth.DEG_TO_RAD;
			for (int j = 0; j < LON; j++) {
				patch(buffer, matrix, c, r, lat - halfWidth, lat + halfWidth, j * lonStep, (j + 1) * lonStep, gridRgb, grid, inside);
			}
		}
		for (int degrees = 0; degrees < 360; degrees += GRID_STEP) {
			float lon = degrees * Mth.DEG_TO_RAD;
			for (int i = 0; i < LAT * 2; i++) {
				float lat0 = -Mth.HALF_PI + i * latStep / 2;
				patch(buffer, matrix, c, r, lat0, lat0 + latStep / 2, lon - halfWidth, lon + halfWidth, gridRgb, grid, inside);
			}
		}
	}

	/** One quad of the shell between two latitudes and two longitudes. */
	private static void patch(BufferBuilder buffer, Matrix4f matrix, Vec3 c, float r, float lat0, float lat1, float lon0, float lon1,
							  float[] rgb, float alpha, boolean inside) {
		float midLat = (lat0 + lat1) / 2;
		float midLon = (lon0 + lon1) / 2;
		float nx = Mth.cos(midLat) * Mth.cos(midLon), ny = Mth.sin(midLat), nz = Mth.cos(midLat) * Mth.sin(midLon);
		if (!inside) {
			// Toward the camera, which sits at the origin: -(c + n r) . n > 0 for the near half.
			double facing = -((c.x + nx * r) * nx + (c.y + ny * r) * ny + (c.z + nz * r) * nz);
			if (facing <= 0) return;
			double distance = Math.sqrt((c.x + nx * r) * (c.x + nx * r) + (c.y + ny * r) * (c.y + ny * r) + (c.z + nz * r) * (c.z + nz * r));
			float rim = 1 - (float) (facing / Math.max(1e-6, distance));
			alpha *= Math.min(2, 0.6f + 1.6f * rim * rim);
		}
		vertex(buffer, matrix, c, r, lat0, lon0, rgb, alpha);
		vertex(buffer, matrix, c, r, lat1, lon0, rgb, alpha);
		vertex(buffer, matrix, c, r, lat1, lon1, rgb, alpha);
		vertex(buffer, matrix, c, r, lat0, lon1, rgb, alpha);
	}

	private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 c, float r, float lat, float lon, float[] rgb, float alpha) {
		float cosLat = Mth.cos(lat);
		buffer.vertex(matrix, (float) (c.x + r * cosLat * Mth.cos(lon)), (float) (c.y + r * Mth.sin(lat)), (float) (c.z + r * cosLat * Mth.sin(lon)))
				.color(rgb[0], rgb[1], rgb[2], Math.min(1, alpha)).endVertex();
	}

	// --- HUD: realm labels and the contact alert ---

	/** WorldRenderEvents.END (through {@link SenseOverlay}): remembers the frame's camera for the labels. */
	static void captureView(WorldRenderContext context) {
		if (!isActive()) {
			viewCamera = null;
			return;
		}
		VIEW_PROJECTION.set(context.projectionMatrix()).mul(context.matrixStack().last().pose());
		viewCamera = context.camera().getPosition();
		viewPartial = context.tickDelta();
	}

	public static void renderHud(GuiGraphics graphics) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.options.hideGui) return;
		renderLabels(graphics, mc);
		renderAlert(graphics, mc);
	}

	/** "Realm - Stage" over every cultivator in the domain, through walls and at a size that reads at any distance. */
	private static void renderLabels(GuiGraphics graphics, Minecraft mc) {
		if (viewCamera == null || SENSED.isEmpty() || !isActive()) return;
		Font font = mc.font;
		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
		List<int[]> placed = new ArrayList<>();
		for (Sensed sensed : SENSED.values()) {
			Entity entity = mc.level.getEntity(sensed.entityId());
			if (entity == null || !OUTLINES.containsKey(entity.getId())) continue;
			Vec3 head = entity.getPosition(viewPartial).add(0, entity.getBbHeight() + 0.5, 0).subtract(viewCamera);
			Vector4f clip = new Vector4f((float) head.x, (float) head.y, (float) head.z, 1).mul(VIEW_PROJECTION);
			if (clip.w <= 0.05f) continue; // behind the camera
			float x = (clip.x / clip.w * 0.5f + 0.5f) * width;
			float y = (0.5f - clip.y / clip.w * 0.5f) * height;
			if (x < -50 || x > width + 50 || y < -20 || y > height + 20) continue;
			Component text = PlayerCultivation.rankName(sensed.realm(), sensed.stage());
			int textWidth = font.width(text);
			int left = Math.round(x - textWidth / 2f);
			int top = Math.round(y) - 20; // above the name tag
			// Labels crowding the same spot stack upward instead of overprinting.
			for (int[] other : placed) {
				if (Math.abs(other[0] - left) < textWidth && Math.abs(other[1] - top) < 10) top = other[1] - 11;
			}
			placed.add(new int[] {left, top});
			graphics.fill(left - 2, top - 1, left + textWidth + 2, top + 9, 0x90000000);
			graphics.drawString(font, text, left, top, sensed.pressed() ? LABEL_PRESSED_COLOR : LABEL_COLOR, false);
		}
	}

	/** A lacquer banner trimmed in gold under the boss bars: who touched the domain, and the realm they show. */
	private static void renderAlert(GuiGraphics graphics, Minecraft mc) {
		if (alertDetail == null) return;
		long age = Util.getMillis() - alertStart;
		if (age > ALERT_MS) {
			alertDetail = null;
			return;
		}
		float alpha = Math.min(1, age / 250f) * Math.min(1, (ALERT_MS - age) / 800f);
		if (alpha < 0.05f) return;
		Font font = mc.font;
		Component title = Component.translatable(ModLang.CONSCIOUSNESS_ALERT_TITLE);
		int w = Math.max(font.width(title), font.width(alertDetail)) + 24;
		int x = (graphics.guiWidth() - w) / 2;
		int y = 40;
		int a = (int) (alpha * 255) << 24;
		graphics.fill(x, y, x + w, y + 25, (int) (alpha * 0xD0) << 24 | 0x101420);
		graphics.fill(x, y, x + w, y + 1, a | 0xB8860B);
		graphics.fill(x, y + 24, x + w, y + 25, a | 0x6E5214);
		// Tapered gold tips at both ends, like the Qi bar's.
		for (int row = 0; row < 5; row++) {
			int reach = row < 3 ? row + 1 : 5 - row;
			graphics.fill(x - reach, y + 10 + row, x, y + 11 + row, a | (row < 2 ? 0xB8860B : 0x6E5214));
			graphics.fill(x + w, y + 10 + row, x + w + reach, y + 11 + row, a | (row < 2 ? 0xB8860B : 0x6E5214));
		}
		graphics.drawCenteredString(font, title, x + w / 2, y + 4, a | 0xE6D3A3);
		graphics.drawCenteredString(font, alertDetail, x + w / 2, y + 14, a | 0xFF8A80);
	}

	/** Disconnect. */
	public static void clear() {
		SENSED.clear();
		OUTLINES.clear();
		domains = previousDomains = List.of();
		shownRadius = -1;
		strength = 0;
		alertDetail = null;
		viewCamera = null;
	}

	private ConsciousnessRenderer() {}
}
