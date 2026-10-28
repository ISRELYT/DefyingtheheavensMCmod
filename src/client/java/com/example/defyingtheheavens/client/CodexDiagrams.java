package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.BeastKind;
import com.example.defyingtheheavens.Bloodline;
import com.example.defyingtheheavens.Codex;
import com.example.defyingtheheavens.CodexMap;
import com.example.defyingtheheavens.CultivatorNpc;
import com.example.defyingtheheavens.DaoFragmentItem;
import com.example.defyingtheheavens.DemonBeast;
import com.example.defyingtheheavens.DemonBlood;
import com.example.defyingtheheavens.DemonBloodItem;
import com.example.defyingtheheavens.DemonCoreItem;
import com.example.defyingtheheavens.HeartDemonEntity;
import com.example.defyingtheheavens.HeavenMethod;
import com.example.defyingtheheavens.JadeSlipItem;
import com.example.defyingtheheavens.Law;
import com.example.defyingtheheavens.ModItems;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.QiElement;
import com.example.defyingtheheavens.Realm;
import com.example.defyingtheheavens.VoidVestige;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Dao Codex's diagrams (Codex, "@diagram name"; the user's pick, 2026-10-28), drawn in its lacquer and gold (TalkUi) into a page's
 * width, each giving back the tooltip of what the mouse points at:
 * <ul>
 *   <li>"worlds": the map of the worlds (CodexMap; the user's ask, 2026-10-28). Every world as a plate, the ways between them as lines;
 *   a world not yet visited (CodexState#visited) shows ??? and, pointed at, only its way in (the user's follow-up the same day: the way
 *   always shows, or a world is hard to find); one visited its name, and pointed at, its qi, the realm to reach before going and its way
 *   in. A gold ring round the world one is in.</li>
 *   <li>"realms": the ladder of the realms, Qi Refining at its foot. A realm is named once its Codex page is open; brackets show where
 *   each realm's qi is rich enough to cultivate it (PlayerCultivation#qiNeed); one's own realm in gold.</li>
 *   <li>"elements": the cycle of the five (QiElement#overcomes), each element pointing at the one it overcomes.</li>
 * </ul>
 */
final class CodexDiagrams {
	private static final int TIP_WIDTH = 170;
	/** The map: a world's plate, a row of them, and how tall the whole map stands (short of the page's view, so it shows whole). */
	private static final int PLATE_W = 84;
	private static final int PLATE_H = 14;
	private static final int ROW = 22;
	private static final int MAP_HEIGHT = 162;
	/** The ladder: a rung to a realm, and the brackets' column. */
	private static final int RUNG = 10;
	private static final int BRACKET_X = 136;
	/** The cycle of the five: an element's square, and how far each sits from the middle. */
	private static final int NODE = 20;
	private static final int RADIUS = 40;
	private static final int CYCLE_HEIGHT = 106;

	private static final int HERE_RING = 0xFFF2C94C;
	private static final int PLATE = 0xFF1E1610;
	private static final int PLATE_SEALED = 0xFF120E0B;
	private static final int WAY = TalkUi.GOLD_DIM;
	private static final int WAY_KNOWN = 0xFF8A6A2E;

	static int height(String name) {
		return switch (name) {
			case "worlds" -> MAP_HEIGHT;
			case "realms" -> RUNG * Realm.values().length + 2;
			case "elements" -> CYCLE_HEIGHT;
			default -> 0;
		};
	}

	/** Draws {@code name} with its top left at (x, y), {@code width} wide; the tooltip of what (mouseX, mouseY) points at, or null. */
	static List<FormattedCharSequence> draw(GuiGraphics g, Font font, String name, int x, int y, int width, int mouseX, int mouseY) {
		if (name.startsWith(BEAST)) {
			beast(g, font, name.substring(BEAST.length()), x, y, width);
			return null;
		}
		return switch (name) {
			case "worlds" -> worlds(g, font, x, y, width, mouseX, mouseY);
			case "realms" -> realms(g, font, x, y, width, mouseX, mouseY);
			case "elements" -> elements(g, font, x, y, width, mouseX, mouseY);
			default -> null;
		};
	}

	// --- A Bestiary page's creature ---

	/** A creature's picture is the diagram "beast:" and its entity type's id; its frame. */
	static final String BEAST = "beast:";
	private static final int FRAME = 64;
	private static final int FRAME_INSIDE = 0xFF2A2018;
	/** The likenesses drawn, made once for the world they were made in (never added to it); the Hungry Ghost's body, made once. */
	private static final Map<String, LivingEntity> LIKENESSES = new HashMap<>();
	private static Level likenessesOf;
	private static PlayerModel<LivingEntity> ghostBody;

	/** How far down a creature's picture "Drops:" stands (the user's word, 2026-10-28): under the frame, or under what it is, whichever is lower. */
	private static int dropsAt(Font font, String id, int width) {
		int facts = 3;
		for (Component fact : facts(id)) facts += font.split(fact, width - FRAME - 8).size() * 9 + 2;
		return Math.max(FRAME, facts) + 4;
	}

	/** A creature's picture's height, its cells of what it drops (CodexScreen) laid out from there. */
	static int beastHeight(Font font, String id, int width) {
		return dropsAt(font, id, width) + 12;
	}

	/**
	 * The creature turning slowly in its frame (clipped to it), what it is beside the frame (its realm, its element, how it goes about,
	 * its temper), and "Drops:" under it, the cells of what it drops laid out below by CodexScreen ({@link #drops}).
	 */
	private static void beast(GuiGraphics g, Font font, String id, int x, int y, int width) {
		g.fill(x, y, x + FRAME, y + FRAME, TalkUi.GOLD_DIM);
		g.fill(x + 1, y + 1, x + FRAME - 1, y + FRAME - 1, FRAME_INSIDE);
		float angle = (Util.getMillis() % 18000L) / 50.0f;
		Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI).rotateX((float) Math.toRadians(-12)).rotateY((float) Math.toRadians(angle));
		g.enableScissor(x + 1, y + 1, x + FRAME - 1, y + FRAME - 1);
		if (id.endsWith(":hungry_ghost")) {
			// Drawn by the ghost shader in the world, by no renderer of its own: here its body in its own skin, faded.
			float scale = (FRAME - 14) / 2.0f;
			renderGhost(g, x + FRAME / 2f, y + FRAME / 2f + 0.9f * scale, scale, pose);
		} else {
			LivingEntity likeness = likeness(id);
			if (likeness != null) {
				// Fitted to the frame by how large it is drawn: its body, most models longer than it, the vast ones far larger.
				double extent = Math.max(likeness.getBbWidth(), likeness.getBbHeight()) * drawnBeyondBody(likeness);
				float scale = (float) ((FRAME - 14) / Math.max(0.5, extent));
				renderLikeness(g, x + FRAME / 2f, y + FRAME / 2f + (float) (likeness.getBbHeight() * scale / 2), scale, pose, likeness);
			}
		}
		g.disableScissor();
		// What it is, beside the frame.
		int fy = y + 3;
		for (Component fact : facts(id)) {
			for (FormattedCharSequence line : font.split(fact, width - FRAME - 8)) {
				g.drawString(font, line, x + FRAME + 6, fy, TalkUi.CREAM, false);
				fy += 9;
			}
			fy += 2;
		}
		Component drops = Component.translatable(drops(id).isEmpty() ? ModLang.CODEX_BEAST_DROPS_NOTHING : ModLang.CODEX_BEAST_DROPS);
		g.drawString(font, drops, x, y + dropsAt(font, id, width), TalkUi.MUTED, false);
	}

	/**
	 * How much larger than its body a creature is drawn: most models longer than their bodies are wide, an elder's a little, the primordial
	 * beasts and the Vestige far beyond their hitboxes.
	 */
	private static double drawnBeyondBody(LivingEntity likeness) {
		if (likeness instanceof VoidVestige) return 7.0;
		if (!(likeness instanceof DemonBeast beast)) return 1;
		return switch (beast.kind()) {
			case KUN_PENG -> 2.2;
			case TRUE_PHOENIX -> 3.5;
			case TRUE_DRAGON -> 4.5;
			case PRIMORDIAL_CHAOS -> 2.0;
			case CHAOS_FIEND -> 1.2;
			default -> beast.kind().isElder() ? 1.5 : 1.6;
		};
	}

	/** A Hungry Ghost's body: a man's, in the Hungry Ghost's skin, faded as the dead are to the living. */
	private static void renderGhost(GuiGraphics g, float x, float y, float scale, Quaternionf pose) {
		Minecraft mc = Minecraft.getInstance();
		if (ghostBody == null) ghostBody = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
		g.pose().pushPose();
		g.pose().translate(x, y, 50.0);
		g.pose().mulPoseMatrix(new Matrix4f().scaling(scale, scale, -scale));
		g.pose().mulPose(pose);
		g.pose().mulPose(Axis.YP.rotationDegrees(180));
		g.pose().scale(-1, -1, 1);
		g.pose().translate(0, -1.501, 0);
		Lighting.setupForEntityInInventory();
		ghostBody.renderToBuffer(g.pose(), g.bufferSource().getBuffer(RenderType.entityTranslucent(GhostlyRenderers.HUNGRY_GHOST_SKIN)), 0xF000F0,
				OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 0.75f);
		g.flush();
		g.pose().popPose();
		Lighting.setupFor3DItems();
	}

	/** Its likeness, made once (a beast at its kind's least, a heart demon in the reader's own skin), or null if it can't be made here. */
	private static LivingEntity likeness(String id) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return null;
		if (likenessesOf != mc.level) {
			LIKENESSES.clear();
			likenessesOf = mc.level;
		}
		LivingEntity made = LIKENESSES.get(id);
		if (made != null) return made;
		ResourceLocation key = ResourceLocation.tryParse(id);
		EntityType<?> type = key == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(key).orElse(null);
		if (type == null || !(type.create(mc.level) instanceof LivingEntity living)) return null;
		if (living instanceof DemonBeast beast) beast.setRank(beast.kind().minRank());
		if (living instanceof HeartDemonEntity demon && mc.player != null) demon.setOwnerId(mc.player.getUUID());
		living.setYRot(0);
		living.setYBodyRot(0);
		living.setYHeadRot(0);
		living.yRotO = 0;
		living.yBodyRotO = 0;
		living.yHeadRotO = 0;
		LIKENESSES.put(id, living);
		return living;
	}

	/** Drawn as the inventory draws its player, at a scale of its own (fractions too: the vast ones are small here). */
	private static void renderLikeness(GuiGraphics g, float x, float y, float scale, Quaternionf pose, LivingEntity likeness) {
		g.pose().pushPose();
		g.pose().translate(x, y, 50.0);
		g.pose().mulPoseMatrix(new Matrix4f().scaling(scale, scale, -scale));
		g.pose().mulPose(pose);
		Lighting.setupForEntityInInventory();
		EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
		dispatcher.setRenderShadow(false);
		RenderSystem.runAsFancy(() -> dispatcher.render(likeness, 0, 0, 0, 0, 1.0f, g.pose(), g.bufferSource(), 0xF000F0));
		g.flush();
		dispatcher.setRenderShadow(true);
		g.pose().popPose();
		Lighting.setupFor3DItems();
	}

	/** What a creature is: its realm (from least to greatest), its element, how it goes about, and whether it seeks a fight. */
	private static List<Component> facts(String id) {
		List<Component> facts = new ArrayList<>();
		String path = id.substring(id.indexOf(':') + 1);
		BeastKind kind = BeastKind.byId(path);
		if (kind != null) {
			facts.add(Component.translatable(ModLang.CODEX_BEAST_REALM, ranks(kind.minRank(), kind.maxRank())));
			facts.add(Component.translatable(ModLang.CODEX_BEAST_ELEMENT, kind.element.getDisplayName().copy()
					.withStyle(s -> s.withColor(kind.element.getCoreColor()))));
			facts.add(Component.translatable(ModLang.codexBeastMoves(kind.moves.name().toLowerCase(Locale.ROOT))));
			facts.add(Component.translatable(kind.territorial ? ModLang.CODEX_BEAST_HUNTS : ModLang.CODEX_BEAST_FIGHTS_BACK));
			return facts;
		}
		switch (path) {
			case "void_vestige" -> {
				facts.add(Component.translatable(ModLang.CODEX_BEAST_REALM, ranks(VoidVestige.MIN_RANK, VoidVestige.MAX_RANK)));
				facts.add(Component.translatable(ModLang.codexBeastMoves("flying")));
				facts.add(Component.translatable(ModLang.CODEX_BEAST_HUNTS));
			}
			case "heart_demon" -> {
				facts.add(Component.translatable(ModLang.CODEX_BEAST_REALM, Component.translatable(ModLang.CODEX_BEAST_YOUR_REALM)));
				facts.add(Component.translatable(ModLang.CODEX_BEAST_HUNTS));
			}
			case "hungry_ghost" -> {
				facts.add(Component.translatable(ModLang.CODEX_BEAST_REALM, Realm.QI_REFINING.getDisplayName()));
				facts.add(Component.translatable(ModLang.CODEX_BEAST_ELEMENT, QiElement.YIN.getDisplayName().copy()
						.withStyle(s -> s.withColor(QiElement.YIN.getCoreColor()))));
				facts.add(Component.translatable(ModLang.CODEX_BEAST_HUNTS));
			}
			default -> {}
		}
		return facts;
	}

	/**
	 * "Foundation Building Late to Core Formation Grand Perfection"; within one realm "Qi Refining, Early to Grand Perfection"; one realm and
	 * stage alone where both ends are one.
	 */
	private static Component ranks(int low, int high) {
		if (low == high) return stageName(low);
		Realm from = CultivatorNpc.realmOf(low), to = CultivatorNpc.realmOf(high);
		if (from == to) {
			return Component.translatable(ModLang.CODEX_BEAST_WITHIN, from.getDisplayName(), CultivatorNpc.stageOf(low).getDisplayName(),
					CultivatorNpc.stageOf(high).getDisplayName());
		}
		return Component.translatable(ModLang.CODEX_RANGE, stageName(low), stageName(high));
	}

	private static Component stageName(int rank) {
		return Component.translatable(ModLang.CODEX_BEAST_STAGE, CultivatorNpc.realmOf(rank).getDisplayName(), CultivatorNpc.stageOf(rank).getDisplayName());
	}

	/**
	 * What a creature drops when slain, a cell to each (several in turn where it drops one of many: a Dao Fragment of any Law): a beast its
	 * core and its kin's blood, both of its least realm, and its remains; a primordial beast its core, its kin's True Immortal blood and
	 * Immortal Jade (the Kun-Peng a jade slip too); chaos what chaos sets hard; a Vestige spirit stones; the rest nothing.
	 */
	static List<List<ItemStack>> drops(String id) {
		List<List<ItemStack>> drops = new ArrayList<>();
		String path = id.substring(id.indexOf(':') + 1);
		BeastKind kind = BeastKind.byId(path);
		List<ItemStack> fragments = new ArrayList<>();
		for (Law law : Law.values()) fragments.add(DaoFragmentItem.of(law));
		if (kind == BeastKind.PRIMORDIAL_CHAOS) {
			drops.add(fragments);
			drops.add(List.of(new ItemStack(ModItems.CHAOS_ORIGIN_STONE)));
			drops.add(List.of(new ItemStack(ModItems.IMMORTAL_JADE)));
		} else if (kind == BeastKind.CHAOS_FIEND) {
			drops.add(List.of(new ItemStack(ModItems.CHAOS_ORIGIN_STONE)));
			drops.add(fragments);
		} else if (kind != null) {
			drops.add(List.of(DemonCoreItem.of(kind.minRank(), kind.element, kind.getDisplayName())));
			Bloodline kin = DemonBlood.kin(kind);
			Realm realm = kind.isPrimordial() ? Realm.TRUE_IMMORTAL : CultivatorNpc.realmOf(kind.minRank());
			if (kin != null) drops.add(List.of(DemonBloodItem.of(kin, realm)));
			if (kind.isPrimordial()) drops.add(List.of(new ItemStack(ModItems.IMMORTAL_JADE)));
			if (kind == BeastKind.KUN_PENG) drops.add(List.of(JadeSlipItem.of(HeavenMethod.of(kind.element, Realm.TRUE_IMMORTAL))));
			for (Item remains : kind.remains()) drops.add(List.of(new ItemStack(remains)));
		} else if (path.equals("void_vestige")) {
			drops.add(List.of(new ItemStack(ModItems.LOW_SPIRIT_STONE), new ItemStack(ModItems.MID_SPIRIT_STONE), new ItemStack(ModItems.HIGH_SPIRIT_STONE)));
		}
		return drops;
	}

	// --- The map of the worlds ---

	/**
	 * Where a world's plate sits: its column (0 the main one, the ladder up from the Overworld and down to the dead; 1 the side one) and
	 * its top. The Astral Realm between the two worlds whose skies lead to it; the worlds reached from anywhere framed apart below.
	 */
	private static int[] place(CodexMap.World world) {
		return switch (world) {
			case IMMORTAL_REALM -> new int[] {0, 0};
			case CHAOS_FRONTIER -> new int[] {1, 0};
			case GREAT_VOID -> new int[] {0, ROW};
			case JADE_FIRMAMENT -> new int[] {0, ROW * 2};
			case ASTRAL -> new int[] {1, ROW * 5 / 2};
			case SPATIAL_GAP -> new int[] {0, ROW * 3};
			case OVERWORLD -> new int[] {0, ROW * 4};
			case THE_END -> new int[] {1, ROW * 4};
			case NETHER -> new int[] {0, ROW * 5};
			case NETHERWORLD -> new int[] {0, ROW * 6};
			case INNER_REALM -> new int[] {1, FRAME_TOP + 15};
			case SMALL_WORLD -> new int[] {1, FRAME_TOP + 33};
		};
	}

	/** The frame of the worlds reached from anywhere: its top, under The End. */
	private static final int FRAME_TOP = ROW * 5 + 1;

	private static int column(int column, int x, int width) {
		return x + (column == 0 ? width / 4 : width * 3 / 4 + 2);
	}

	private static List<FormattedCharSequence> worlds(GuiGraphics g, Font font, int x, int y, int width, int mouseX, int mouseY) {
		Minecraft mc = Minecraft.getInstance();
		CodexMap.World here = mc.level == null || mc.player == null ? null : CodexMap.World.at(mc.level, mc.player.getX(), mc.player.getZ());

		// The ways first, under the plates: from the side of one plate that faces the other.
		for (CodexMap.Way way : CodexMap.WAYS) {
			int[] a = place(way.from());
			int[] b = place(way.to());
			int ax = column(a[0], x, width), bx = column(b[0], x, width);
			int color = known(way.from(), here) && known(way.to(), here) ? WAY_KNOWN : WAY;
			if (ax == bx) {
				int top = Math.min(a[1], b[1]) + PLATE_H, bottom = Math.max(a[1], b[1]);
				g.fill(ax, y + top, ax + 1, y + bottom, color);
			} else {
				int[] left = ax < bx ? a : b, right = ax < bx ? b : a;
				line(g, column(left[0], x, width) + PLATE_W / 2, y + left[1] + PLATE_H / 2, column(right[0], x, width) - PLATE_W / 2 - 1,
						y + right[1] + PLATE_H / 2, color);
			}
		}

		// The worlds reached from anywhere, in a frame of their own.
		int frameX = column(1, x, width) - PLATE_W / 2 - 4;
		dashedFrame(g, frameX, y + FRAME_TOP, frameX + PLATE_W + 8, y + MAP_HEIGHT, WAY);
		Component anywhere = Component.translatable(ModLang.CODEX_ANYWHERE);
		g.drawString(font, anywhere, column(1, x, width) - font.width(anywhere) / 2, y + FRAME_TOP + 3, TalkUi.MUTED, false);

		List<FormattedCharSequence> tip = null;
		for (CodexMap.World world : CodexMap.World.values()) {
			int[] at = place(world);
			int cx = column(at[0], x, width);
			int x0 = cx - PLATE_W / 2, y0 = y + at[1];
			boolean known = known(world, here);
			boolean hover = mouseX >= x0 && mouseX < x0 + PLATE_W && mouseY >= y0 && mouseY < y0 + PLATE_H;
			if (world == here) g.fill(x0 - 2, y0 - 2, x0 + PLATE_W + 2, y0 + PLATE_H + 2, HERE_RING);
			g.fill(x0, y0, x0 + PLATE_W, y0 + PLATE_H, world == here ? TalkUi.INNER : known ? TalkUi.GOLD : TalkUi.GOLD_DIM);
			g.fill(x0 + 1, y0 + 1, x0 + PLATE_W - 1, y0 + PLATE_H - 1, hover ? TalkUi.PANEL_LIT : known ? PLATE : PLATE_SEALED);
			Component name = Component.translatable(known ? world.nameKey() : ModLang.CODEX_LOCKED);
			name = trimmed(font, name, PLATE_W - 4);
			g.drawString(font, name, cx - font.width(name) / 2, y0 + 3, known ? TalkUi.CREAM : TalkUi.DISABLED, false);
			if (hover) tip = known ? worldTip(font, world, world == here) : sealedTip(font, world);
		}
		return tip;
	}

	/** A world not yet visited: ??? for all it is, but its way in, so it can be found. */
	private static List<FormattedCharSequence> sealedTip(Font font, CodexMap.World world) {
		List<FormattedCharSequence> tip = new ArrayList<>(split(font, Component.translatable(ModLang.CODEX_LOCKED)));
		tip.addAll(font.split(Component.translatable(world.wayKey()).withStyle(s -> s.withColor(TalkUi.MUTED)), TIP_WIDTH));
		return tip;
	}

	/** Visited, or stood in now (the client's own note of it comes at its next look). */
	private static boolean known(CodexMap.World world, CodexMap.World here) {
		return world == here || CodexState.visited(world);
	}

	/** A visited world: its name, its qi and ghostly qi, the realm to reach before going, its way in, and whether one is there. */
	private static List<FormattedCharSequence> worldTip(Font font, CodexMap.World world, boolean here) {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable(world.nameKey()).withStyle(s -> s.withColor(TalkUi.GOLD_TEXT)));
		lines.add(Component.translatable(world.qiOfTheBody() ? ModLang.CODEX_QI_BODY : ModLang.CODEX_QI, amount(world.qi(), world.qiTop())));
		if (world.ghostQiTop() > 0) {
			lines.add(Component.translatable(world.ghostQiInPlaces() ? ModLang.CODEX_GHOST_QI_IN_PLACES : ModLang.CODEX_GHOST_QI,
					amount(world.ghostQi(), world.ghostQiTop())));
		}
		Component realm = world.recommended() == null ? Component.translatable(ModLang.CODEX_ANY_REALM)
				: world.recommendedStage() == null ? world.recommended().getDisplayName()
				: Component.translatable(ModLang.CODEX_REALM_STAGE, world.recommended().getDisplayName(), world.recommendedStage().getDisplayName());
		lines.add(Component.translatable(ModLang.CODEX_RECOMMENDED, realm));
		lines.add(Component.translatable(world.wayKey()).withStyle(s -> s.withColor(TalkUi.MUTED)));
		if (here) lines.add(Component.translatable(ModLang.CODEX_HERE).withStyle(s -> s.withColor(TalkUi.GOLD_TEXT)));
		List<FormattedCharSequence> tip = new ArrayList<>();
		for (Component line : lines) tip.addAll(font.split(line, TIP_WIDTH));
		return tip;
	}

	/** "×10", or "×0.25 to ×3" where it differs from place to place. */
	private static Component amount(double low, double high) {
		Component from = Component.translatable(ModLang.CODEX_TIMES, number(low));
		return low == high ? from : Component.translatable(ModLang.CODEX_RANGE, from, Component.translatable(ModLang.CODEX_TIMES, number(high)));
	}

	/** 1000, 12, 1.25, 0.25: two places at most, and no trailing zeros. */
	private static String number(double value) {
		return BigDecimal.valueOf(Math.round(value * 100) / 100.0).stripTrailingZeros().toPlainString();
	}

	// --- The ladder of the realms ---

	private static List<FormattedCharSequence> realms(GuiGraphics g, Font font, int x, int y, int width, int mouseX, int mouseY) {
		Realm[] realms = Realm.values();
		PlayerCultivation c = ClientCultivationData.get();
		Realm mine = c == null || c.isMortal() ? null : c.getRealm();
		int bottom = y + RUNG * realms.length;
		// The ladder's two rails, the whole height.
		g.fill(x + 3, y + 1, x + 4, bottom + 1, TalkUi.GOLD_DIM);
		g.fill(x + 12, y + 1, x + 13, bottom + 1, TalkUi.GOLD_DIM);

		List<FormattedCharSequence> tip = null;
		for (Realm realm : realms) {
			int ry = bottom - RUNG * (realm.ordinal() + 1);
			boolean known = realmKnown(realm) || mine != null && realm.ordinal() <= mine.ordinal();
			boolean own = realm == mine;
			g.fill(x + 3, ry + 5, x + 13, ry + 6, own ? TalkUi.GOLD : TalkUi.GOLD_DIM);
			if (own) {
				g.fill(x + 6, ry + 3, x + 10, ry + 8, HERE_RING);
				g.fill(x + 7, ry + 4, x + 9, ry + 7, 0xFFFFF4C9);
			}
			Component name = trimmed(font, known ? realm.getDisplayName() : Component.translatable(ModLang.CODEX_LOCKED), BRACKET_X - 22);
			g.drawString(font, name, x + 18, ry + 2, own ? TalkUi.GOLD_TEXT : known ? TalkUi.CREAM : TalkUi.DISABLED, false);
			if (mouseX >= x && mouseX < x + BRACKET_X - 2 && mouseY >= ry && mouseY < ry + RUNG) {
				if (!known) tip = split(font, Component.translatable(ModLang.CODEX_LOCKED));
				else {
					List<Component> lines = new ArrayList<>();
					lines.add(realm.getDisplayName().copy().withStyle(s -> s.withColor(TalkUi.GOLD_TEXT)));
					lines.add(Component.translatable(ModLang.CODEX_CULTIVATED_IN, homeName(realm)));
					if (own) lines.add(Component.translatable(ModLang.CODEX_YOUR_REALM).withStyle(s -> s.withColor(TalkUi.GOLD_TEXT)));
					tip = new ArrayList<>();
					for (Component line : lines) tip.addAll(font.split(line, TIP_WIDTH));
				}
			}
		}

		// The brackets: each run of realms cultivated on the same qi, and the world it is found in.
		int from = 0;
		for (int i = 1; i <= realms.length; i++) {
			if (i < realms.length && PlayerCultivation.qiNeed(realms[i]) == PlayerCultivation.qiNeed(realms[from])) continue;
			int top = bottom - RUNG * i + 2, low = bottom - RUNG * from - 2;
			int bx = x + BRACKET_X;
			g.fill(bx, top, bx + 1, low, TalkUi.GOLD_DIM);
			g.fill(bx - 3, top, bx, top + 1, TalkUi.GOLD_DIM);
			g.fill(bx - 3, low - 1, bx, low, TalkUi.GOLD_DIM);
			CodexMap.World home = home(realms[from]);
			boolean known = home == null || CodexState.visited(home);
			List<FormattedCharSequence> label = font.split(known ? homeName(realms[from]) : Component.translatable(ModLang.CODEX_LOCKED),
					width - BRACKET_X - 5);
			int ly = (top + low) / 2 - label.size() * 9 / 2;
			for (FormattedCharSequence part : label) {
				g.drawString(font, part, bx + 4, ly, known ? TalkUi.MUTED : TalkUi.DISABLED, false);
				ly += 9;
			}
			from = i;
		}
		return tip;
	}

	/** Whether a realm's Codex page is open (its own, or for the realms past True Immortal, the page on them). */
	private static boolean realmKnown(Realm realm) {
		Codex.Entry page = Codex.entry(realm.isPastTrueImmortal() ? "beyond" : realm.name().toLowerCase(Locale.ROOT));
		return page != null && CodexState.isOpen(page);
	}

	/** The world whose qi a realm is cultivated on (null: the lower realms', anywhere). */
	private static CodexMap.World home(Realm realm) {
		double need = PlayerCultivation.qiNeed(realm);
		if (need >= PlayerCultivation.IMMORTAL_QI_MULTIPLIER) return CodexMap.World.IMMORTAL_REALM;
		if (need >= PlayerCultivation.GREAT_VOID_QI_MULTIPLIER) return CodexMap.World.GREAT_VOID;
		if (need >= PlayerCultivation.UPPER_REALM_QI_MULTIPLIER) return CodexMap.World.JADE_FIRMAMENT;
		return null;
	}

	private static Component homeName(Realm realm) {
		CodexMap.World home = home(realm);
		if (home == null) return Component.translatable(ModLang.CODEX_LOWER_REALMS);
		return CodexState.visited(home) ? Component.translatable(home.nameKey()) : Component.translatable(ModLang.CODEX_LOCKED);
	}

	// --- The cycle of the five ---

	private static List<FormattedCharSequence> elements(GuiGraphics g, Font font, int x, int y, int width, int mouseX, int mouseY) {
		// Round the circle from fire, each element followed by the one it overcomes.
		List<QiElement> ring = new ArrayList<>();
		QiElement at = QiElement.FIRE;
		for (int i = 0; i < 5; i++) {
			ring.add(at);
			for (QiElement next : QiElement.five()) if (at.overcomes(next)) {
				at = next;
				break;
			}
		}
		int cx = x + width / 2, cy = y + NODE / 2 + 1 + RADIUS;
		int[][] centre = new int[5][];
		for (int i = 0; i < 5; i++) {
			double angle = Math.toRadians(-90 + 72 * i);
			centre[i] = new int[] {cx + (int) Math.round(RADIUS * Math.cos(angle)), cy + (int) Math.round(RADIUS * Math.sin(angle))};
		}
		// The arrows, each in the colour of the element that overcomes.
		for (int i = 0; i < 5; i++) {
			int[] a = centre[i], b = centre[(i + 1) % 5];
			double dx = b[0] - a[0], dy = b[1] - a[1], length = Math.sqrt(dx * dx + dy * dy);
			double ux = dx / length, uy = dy / length;
			int color = 0xFF000000 | ring.get(i).getCoreColor();
			int sx = (int) Math.round(a[0] + ux * 14), sy = (int) Math.round(a[1] + uy * 14);
			int ex = (int) Math.round(b[0] - ux * 14), ey = (int) Math.round(b[1] - uy * 14);
			line(g, sx, sy, ex, ey, color);
			for (int side : new int[] {-1, 1}) {
				int wx = (int) Math.round(ex - ux * 5 - uy * 3 * side), wy = (int) Math.round(ey - uy * 5 + ux * 3 * side);
				line(g, ex, ey, wx, wy, color);
			}
		}
		List<FormattedCharSequence> tip = null;
		for (int i = 0; i < 5; i++) {
			QiElement element = ring.get(i);
			int x0 = centre[i][0] - NODE / 2, y0 = centre[i][1] - NODE / 2;
			boolean hover = mouseX >= x0 && mouseX < x0 + NODE && mouseY >= y0 && mouseY < y0 + NODE;
			g.fill(x0, y0, x0 + NODE, y0 + NODE, 0xFF000000 | (hover ? element.getCoreColor() : element.getGlowColor()));
			g.fill(x0 + 1, y0 + 1, x0 + NODE - 1, y0 + NODE - 1, hover ? TalkUi.PANEL_LIT : PLATE);
			g.renderItem(new ItemStack(icon(element)), x0 + 2, y0 + 2);
			// Its name outward of the circle: beside the top one and the sides, under the two below.
			Component name = element.getDisplayName();
			int w = font.width(name);
			int nx, ny;
			if (i == 0) {
				nx = x0 + NODE + 3;
				ny = y0 + 6;
			} else if (centre[i][1] > cy) {
				nx = centre[i][0] - w / 2;
				ny = y0 + NODE + 2;
			} else {
				nx = centre[i][0] > cx ? x0 + NODE + 3 : x0 - 3 - w;
				ny = y0 + 6;
			}
			g.drawString(font, name, nx, ny, element.getCoreColor(), false);
			if (hover) {
				QiElement overcomes = ring.get((i + 1) % 5), overcomeBy = ring.get((i + 4) % 5);
				List<Component> lines = List.of(name.copy().withStyle(s -> s.withColor(element.getCoreColor())),
						Component.translatable(ModLang.CODEX_OVERCOMES, overcomes.getDisplayName()),
						Component.translatable(ModLang.CODEX_OVERCOME_BY, overcomeBy.getDisplayName()));
				tip = new ArrayList<>();
				for (Component line : lines) tip.addAll(font.split(line, TIP_WIDTH));
			}
		}
		return tip;
	}

	private static ItemLike icon(QiElement element) {
		return switch (element) {
			case WOOD -> Items.OAK_SAPLING;
			case WATER -> Items.WATER_BUCKET;
			case FIRE -> Items.BLAZE_POWDER;
			case EARTH -> Items.DIRT;
			case METAL, YIN -> Items.IRON_INGOT;
		};
	}

	// --- Drawing ---

	/** A line a pixel wide from (x0, y0) to (x1, y1), both ends drawn. */
	private static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
		if (y0 == y1) {
			g.fill(Math.min(x0, x1), y0, Math.max(x0, x1) + 1, y0 + 1, color);
			return;
		}
		if (x0 == x1) {
			g.fill(x0, Math.min(y0, y1), x0 + 1, Math.max(y0, y1) + 1, color);
			return;
		}
		int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx + dy;
		while (true) {
			g.fill(x0, y0, x0 + 1, y0 + 1, color);
			if (x0 == x1 && y0 == y1) return;
			int e2 = 2 * err;
			if (e2 >= dy) {
				err += dy;
				x0 += sx;
			}
			if (e2 <= dx) {
				err += dx;
				y0 += sy;
			}
		}
	}

	/** A frame of dashes, two pixels on and two off. */
	private static void dashedFrame(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
		for (int x = x0; x < x1; x += 4) {
			g.fill(x, y0, Math.min(x + 2, x1), y0 + 1, color);
			g.fill(x, y1 - 1, Math.min(x + 2, x1), y1, color);
		}
		for (int y = y0; y < y1; y += 4) {
			g.fill(x0, y, x0 + 1, Math.min(y + 2, y1), color);
			g.fill(x1 - 1, y, x1, Math.min(y + 2, y1), color);
		}
	}

	private static List<FormattedCharSequence> split(Font font, Component text) {
		return font.split(text, TIP_WIDTH);
	}

	private static Component trimmed(Font font, Component text, int width) {
		if (font.width(text) <= width) return text;
		MutableComponent cut = Component.literal(font.plainSubstrByWidth(text.getString(), width - font.width("...")) + "...");
		return cut.withStyle(text.getStyle());
	}

	private CodexDiagrams() {}
}
