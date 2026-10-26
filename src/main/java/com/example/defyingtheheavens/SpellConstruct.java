package com.example.defyingtheheavens;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.state.BoneSnapshot;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Map;

/**
 * A great spell's living shape (the realms past Four Axis, 2026-10-24: the user's "use geckolib for animations and also use unique and
 * well-crafted detailed models"): a spirit beast of an element's qi, a tree, a seal, a whirlpool, a vortex of swords, a procession and a
 * gate of the dead, an avatar of heaven and demon and its hands, each its own GeckoLib model, animations and glow (tools/spell-art; drawn
 * translucent and full bright: SpellConstructRenderer). Its spell's lingering
 * effect moves and turns it and calls its moves ({@link #act}); the construct is only the body, as a SpellField is. Never saved.
 * <p>
 * From Void Shattering on, space itself is torn open too (the user's ask, 2026-10-24: "make sure ... the rifts and tears/cracks in space
 * looks cool"): a construct of a {@link Form} other than a model's is a tear in space, drawn by the client's own hand (RiftRenderer: its
 * jagged rim burning, the void and its stars within, cracks running out from it like cracks in glass, shards of space breaking away),
 * opening as it forms and closing as it goes, glowing the colour of what comes through it ({@link #tint}).
 */
public class SpellConstruct extends Entity implements GeoEntity {
	/**
	 * How it is drawn: a GeckoLib model ({@link #SHAPE}); or space torn open, drawn by the client's own hand (RiftRenderer), its size the
	 * tear's length in blocks and its glow its {@link #tint}: a jagged {@link #TEAR} standing in the air (turned to whoever looks at it,
	 * upright, or turned on their screen by its yaw: 90, a slash lying across the sky; or, pitched 90, lying flat in the sky open below), an
	 * {@link #ABYSS} lying open in the ground, a {@link #LINE} cut along its facing from where it stands, or space {@link #SHATTER}ed like
	 * glass round a point; or a {@link #TENDRIL}, a root drawn by hand too (TendrilRenderer), lashing from where it stands to the being it
	 * holds ({@link #target}) and coiled round it, its size its girth. Or {@link #DRAWN} by hand whole (SpellDrawn: an eclipse, a vortex of souls,
	 * the two Impermanences), each its own way (the user's ask, 2026-10-25, of shapes that "look retarded": "try a diff style").
	 */
	public enum Form { SHAPE, TEAR, ABYSS, LINE, SHATTER, TENDRIL, GHOSTS, BLOCKS, DRAWN }

	/**
	 * What it is: its model's id (geo/spell/&lt;id&gt;.geo.json, its texture and animations alike), the move it enters with (or null),
	 * the one it loops, the moves its spell may call, and how many times its model's size it is drawn; or a tear in space ({@link Form}),
	 * which has none of these. Synced by ordinal: add at the end.
	 */
	public enum Kind {
		AZURE_DRAGON("azure_dragon", null, "swim", 4.0f, "roar"),
		DAO_TREE("dao_tree", "grow", "sway", 4.0f, "wither"),
		VERMILION_BIRD("vermilion_bird", null, "glide", 4.0f, "dive"),
		ONE_FLAME_SEED("one_flame_seed", null, "spin", 2.0f, "bloom"),
		ONE_FLAME_PILLAR("one_flame_pillar", "erupt", "burn", 4.0f),
		QILIN("qilin", null, "gallop", 4.0f, "rear"),
		MOUNTAIN_SEAL("mountain_seal", null, "fall", 4.0f, "stamp"),
		BLACK_TORTOISE("black_tortoise", "rise", "idle", 4.0f, "strike"),
		HUNDRED_RIVERS("hundred_rivers", null, "spin", 4.0f, "burst"),
		WHITE_TIGER("white_tiger", null, "prowl", 4.0f, "pounce", "claw"),
		MYRIAD_SWORDS("myriad_swords", null, "orbit", 4.0f, "merge"),
		COLOSSAL_SWORD("colossal_sword", null, "idle", 4.0f),
		// Dao Integration's ghost foundation's (GhostSpells) and its spell of both paths (HeavenDemonSpells).
		PROCESSION("procession", "pour", "march", 4.0f),
		GHOST_GATE("ghost_gate", "open", "draw", 4.0f, "slam"),
		HEAVEN_DEMON_AVATAR("heaven_demon_avatar", "rise", "loom", 4.0f, "strike_palm", "strike_claw"),
		AVATAR_PALM("avatar_palm", "fall", "press", 4.0f),
		AVATAR_CLAW("avatar_claw", "fall", "press", 4.0f),
		// Void Shattering's (2026-10-24): the elements' (their kits), the ghost foundation's (GhostSpells) and both paths' (HeavenDemonSpells)...
		AZURE_THUNDER_CLOUD("azure_thunder_cloud", "gather", "churn", 4.0f, "flash"),
		VOID_ROOTS("void_roots", "descend", "drink", 4.0f, "withdraw"),
		GOLDEN_CROW("golden_crow", "emerge", "dive", 4.0f, "strike"),
		VOID_SUN("void_sun", "emerge", "blaze", 4.0f, "collapse"),
		BUZHOU_PILLAR("buzhou_pillar", null, "fall", 4.0f, "impact"),
		EARTH_VEIN_LOCK("earth_vein_lock", "rise", "hold", 4.0f, "release"),
		WEAK_WATER_FALL("weak_water_fall", "pour", "fall", 4.0f, "dry"),
		KUN("kun", null, "swim", 4.0f),
		SEVERING_LIGHT("severing_light", null, "thrust", 4.0f),
		SWORD_PRISON("sword_prison", "descend", "hold", 4.0f, "converge"),
		NINE_NETHERS("nine_nethers", "open", "turn", 4.0f, "seal"),
		NAIHE_BRIDGE("naihe_bridge", "form", "stand", 4.0f),
		HEAVEN_DEMON_FIST("heaven_demon_fist", null, "fly", 4.0f, "impact"),
		// ...and the tears in space they come through (Form).
		VOID_RIFT("void_rift", Form.TEAR),
		ABYSS_RIFT("abyss_rift", Form.ABYSS),
		RIFT_LINE("rift_line", Form.LINE),
		SHATTERED_SPACE("shattered_space", Form.SHATTER),
		// The void tree's grasping roots (Void Withering, 2026-10-24: the user's ask, roots that reach out and seize).
		VOID_TENDRIL("void_tendril", Form.TENDRIL),
		// Sacred Vessel's (2026-10-24): the sacred treasures, the elements' (their kits), the ghost foundation's (GhostSpells) and both paths'
		// (HeavenDemonSpells); and the golden ropes, drawn by hand (TendrilRenderer).
		SWALLOWING_GOURD("swallowing_gourd", "descend", "draw", 4.0f, "spit"),
		BODHI_BRANCH("bodhi_branch", "appear", "hold", 4.0f, "sweep"),
		WIND_FIRE_WHEEL("wind_fire_wheel", "ignite", "roll", 4.0f, "burst"),
		TRIGRAM_FURNACE("trigram_furnace", null, "burn", 4.0f, "jet", "burst"),
		XIRANG("xirang", null, "pulse", 4.0f, "swell", "settle"),
		NINE_CAULDRON("nine_cauldron", null, "stand", 4.0f, "land", "rise"),
		SEA_PEARL("sea_pearl", null, "spin", 4.0f),
		OCEAN_VASE("ocean_vase", "come", "drink", 4.0f, "seal", "slam"),
		BINDING_RING("binding_ring", "form", "turn", 4.0f, "cinch"),
		GOLDEN_ROPE("golden_rope", Form.TENDRIL),
		DRAGON_SCISSORS("dragon_scissors", "open", "hover", 4.0f, "snap"),
		KARMA_MIRROR("karma_mirror", "rise", "shine", 4.0f, "flash"),
		SOUL_PAGODA("soul_pagoda", null, "glow", 4.0f, "land", "seal"),
		HEAVEN_DEMON_BELL("heaven_demon_bell", null, "hang", 4.0f, "toll"),
		// Entering Nirvana's (2026-10-24): death and rebirth; the elements' (their kits), the ghost foundation's (GhostSpells) and both paths'
		// (HeavenDemonSpells).
		REBIRTH_SEED("rebirth_seed", null, "fly", 4.0f, "root", "bloom", "wither"),
		SEASON_WHEEL("season_wheel", "grow", "turn", 4.0f, "flip"),
		UNDYING_EMBER("undying_ember", null, "fly", 4.0f, "flare", "dim"),
		HEAVEN_PYRE("heaven_pyre", "rise", "burn", 4.0f, "collapse"),
		GRAVESTONE("gravestone", null, "fall", 4.0f, "land", "sink"),
		STANDING_STONE("standing_stone", "rise", "stand", 4.0f, "topple"),
		FROZEN_MOON("frozen_moon", "rise", "shine", 4.0f, "shatter"),
		ICE_MOON("ice_moon", null, "fall", 4.0f, "shatter"),
		SWORD_TOMB("sword_tomb", "erupt", "stand", 4.0f, "rise", "rain"),
		REBIRTH_SWORD("rebirth_sword", "reborn", "flare", 4.0f, "reborn"),
		WHITE_IMPERMANENCE("white_impermanence", "come", "idle", 4.0f, "strike"),
		BLACK_IMPERMANENCE("black_impermanence", "come", "idle", 4.0f, "strike"),
		LIFE_DEATH_BOOK("life_death_book", "open", "write", 4.0f, "strike"),
		HEAVEN_DEMON_ECLIPSE("heaven_demon_eclipse", "converge", "blaze", 4.0f, "pulse"),
		// True Immortal's (2026-10-24): the elements as they were before heaven and earth were parted; the elements' (their kits), the ghost
		// foundation's (GhostSpells) and both paths' (HeavenDemonSpells).
		PRIMORDIAL_CYCLONE("primordial_cyclone", "form", "spin", 4.0f, "fade"),
		CREATION_SEED("creation_seed", "grow", "pulse", 4.0f, "bloom"),
		SAMADHI_PILLARS("samadhi_pillars", "erupt", "burn", 4.0f, "converge"),
		CHAOS_FLOWER("chaos_flower", "open", "burn", 4.0f, "implode"),
		HOUTU_HAND("houtu_hand", "rise", "open", 4.0f, "grasp", "slam"),
		FALLING_CONTINENT("falling_continent", null, "fall", 4.0f, "impact"),
		PRIMAL_GLACIER("primal_glacier", "rise", "advance", 4.0f, "calve"),
		PRIMORDIAL_SEA("primordial_sea", "rise", "churn", 4.0f, "close"),
		HEAVEN_SLAYING_SWORD("heaven_slaying_sword", "rise", "stand", 4.0f, "flare"),
		PANGU_AXE("pangu_axe", "raise", "hold", 4.0f, "chop"),
		FENGDU_WALL("fengdu_wall", "rise", "stand", 4.0f, "fall"),
		SOUL_HELIX("soul_helix", "rise", "spiral", 4.0f, "descend"),
		CHAOS_SPHERE("chaos_sphere", "swell", "churn", 4.0f, "burst"),
		// Four Axis's and below, made over as the great spells' are (2026-10-25: the user's "they were done before geckolib and some look not
		// great"): fire's (FireSpells), carried by the missiles they fly as (SpellProjectile#body)...
		FALLING_SUN("falling_sun", "kindle", "burn", 4.0f, "impact"),
		// ...and the phoenix of nirvana, risen out of its caster as it is cast, and again (its other enter) bursting out of them as it saves them.
		NIRVANA_PHOENIX("nirvana_phoenix", "emerge", "hover", 4.0f, "enfold"),
		NIRVANA_REBIRTH("nirvana_phoenix", "reborn", "hover", 4.0f),
		// Wood's (WoodSpells): the vines that bind a being, drawn at that being's size (two blocks tall at size 1).
		BINDING_VINES("binding_vines", "seize", "squeeze", 1.0f, "crush"),
		// Earth's (EarthSpells): the talisman that seals the mountain it raises. Water's (WaterSpells): the coffin of ice a frozen being stands in,
		// drawn at that being's size.
		MOUNTAIN_TALISMAN("mountain_talisman", "descend", "hold", 4.0f, "break"),
		ICE_COFFIN("ice_coffin", "freeze", "hold", 1.0f, "shatter"),
		// Metal's (MetalSpells): the Heaven-Severing Sword's sword of light, there at once (its stroke is over in half a second).
		SEVERING_SWORD("severing_sword", 1, "stroke", "gone", 4.0f),
		// Heavenly Being's and Nascent Soul's, made over: fire's stooping phoenix and meteor, earth's falling mountain (missiles' bodies).
		DESCENT_PHOENIX("descent_phoenix", "form", "fly", 2.0f, "strike"),
		BLAZING_METEOR("blazing_meteor", null, "fall", 4.0f, "impact"),
		FALLING_PEAK("falling_peak", null, "fall", 4.0f, "impact"),
		// ...metal's domain of swords; wood's piercing bamboo, lotus and thorn spears.
		SWORD_DOMAIN("sword_domain", "unfold", "wheel", 4.0f, "strike"),
		PIERCING_BAMBOO("piercing_bamboo", "burst", "sway", 4.0f),
		REBIRTH_LOTUS("rebirth_lotus", "bloom", "glow", 4.0f),
		THORN_FOREST("thorn_forest", "erupt", "stand", 4.0f),
		// Core Formation's, made over: metal's ring of swords and each sword it rains; earth's dragon surging through the land.
		SWORD_RING("sword_ring", "gather", "turn", 2.0f),
		RAIN_SWORD("rain_sword", null, "fall", 2.0f),
		EARTH_DRAGON("earth_dragon", "breach", "surge", 4.0f, "dive"),
		// Both paths' below Dao Integration (HeavenDemonSpells): the palm, the mantle, the seal and its maw (drawn at their beings' sizes), the
		// claw's rake (there at once: it comes down in a moment), the two made one.
		HD_PALM("hd_palm", "thrust", "glow", 2.0f, "strike"),
		HD_MANTLE("hd_mantle", "wrap", "turn", 1.0f),
		DEVOURING_SEAL("devouring_seal", null, "hold", 1.0f, "bite"),
		HD_CLAW("hd_claw", 1, "rake", "linger", 4.0f),
		HD_AS_ONE("hd_as_one", "converge", "spin", 4.0f),
		// The ghost path's below Dao Integration (Spells; tools/spell-art Underworld, 2026-10-25): the cold fire kindled in the hands, the bolt,
		// the wraith a dash leaves (there at once), the wail, the Yellow Springs' tide, the marrow ward, the core's burst, the king's decree, the
		// ways through the dark, Yama's tablet, the way to hell the chains come out of and the chains themselves (drawn by hand, as a tendril),
		// the wheel of samsara, the six pillars. Those that come in a moment fade in as quickly.
		GHOST_KINDLE("ghost_kindle", 2, "kindle", "burn", 1.0f),
		GHOST_BOLT("ghost_bolt", 2, null, "fly", 2.0f, "burst"),
		SOUL_WRAITH("soul_wraith", 1, "left", "drift", 1.0f),
		GHOST_WAIL("ghost_wail", 3, "wail", "echo", 4.0f),
		SPRINGS_TIDE("springs_tide", 3, "surge", "ebb", 4.0f),
		MARROW_WARD("marrow_ward", "form", "hold", 1.0f, "crumble"),
		CORE_BURST("core_burst", 2, "burst", "smoulder", 4.0f),
		KINGS_DECREE("kings_decree", "unroll", "hold", 2.0f),
		NETHER_POOL("nether_pool", 3, "open", "swirl", 1.0f, "close"),
		YAMA_TABLET("yama_tablet", 2, "slam", "stand", 2.0f, "sink"),
		HELL_GATE("hell_gate", "open", "burn", 2.0f),
		HELL_CHAIN("hell_chain", Form.TENDRIL),
		SAMSARA_WHEEL("samsara_wheel", "appear", "turn", 2.0f),
		SIX_PATHS("six_paths", 3, "erupt", "burn", 4.0f, "collapse"),
		// The ghost path's dead as the mod's own ghosts are drawn (Form GHOSTS: the soul shader's cyan light in a body's own shape, SoulRenderer;
		// the user's ask, 2026-10-25: "make ghost dash my own players ghost ... why tf would u make these shitty ghost models? we already have
		// ghosts in the mod"), drawn by hand (SpellGhosts): the caster's own ghost a Soul Dash leaves along its way, and the giant one that
		// wails over them; the dead pouring out in the Procession; the dead dragged down into the Nine Nethers; the helix of ten thousand souls;
		// the White and the Black Impermanence.
		SOUL_AFTERIMAGE("soul_afterimage", Form.GHOSTS),
		WAIL_SPECTRE("wail_spectre", Form.GHOSTS, "wail"),
		GHOST_PROCESSION("ghost_procession", Form.GHOSTS),
		NETHER_DESCENT("nether_descent", Form.GHOSTS, "seal"),
		SOUL_ASCENT("soul_ascent", Form.GHOSTS, "descend"),
		WHITE_WUCHANG("white_wuchang", Form.GHOSTS, "strike", "take"),
		BLACK_WUCHANG("black_wuchang", Form.GHOSTS, "strike", "take"),
		// Made of the world's own blocks (Form BLOCKS: laid out block by block, the same for its seed, its tint, on every screen, and drawn as the
		// game draws its land: BlockMasses; the user's ask, 2026-10-25): Falling Continent's land, its round its size; Glacier of the First Age's
		// wall of ice, the half of its width its size and how far back it runs its value.
		CONTINENT_MASS("continent_mass", Form.BLOCKS, "impact"),
		GLACIER_MASS("glacier_mass", Form.BLOCKS, "calve"),
		// A living root reaching up out of the ground to the being it binds and drinking from it (drawn by hand: TendrilRenderer), its size its
		// girth: the Seed of Withering Rebirth's, planted (WoodSpells), and the woods' that drain the land round them.
		LIVING_ROOT("living_root", Form.TENDRIL),
		// Seven-Treasure Bodhi Branch's tree (WoodSpells#Branch, the user's ask, 2026-10-25: "redo it completely"): the branch planted grown into a
		// towering bodhi tree of the world's own wood and leaves, its height its size, the seven treasures hanging from its boughs (how many have
		// fallen, its value); it sheds its leaves as it goes ({@code shed}).
		BODHI_TREE("bodhi_tree", Form.BLOCKS, "shed"),
		// Drawn by hand whole (Form DRAWN: SpellDrawn). Heaven-Demon Eclipse (HeavenDemonSpells#Eclipse; the user's report, 2026-10-25: "the beam
		// coming out of the moons as its larger and doesn't line up"): heaven's sun and the demon's moon meeting, its corona, and its beam of darkness
		// poured down from the moon's face to the ground, its size the beam's round (the reach of its harm), its value how far down it reaches.
		ECLIPSE("eclipse", Form.DRAWN, "pulse"),
		// Ten Thousand Souls Ascend (GhostSpells#Souls; "try a diff style"): a vortex of souls, the dead's own faces (the Nether's souls) by the
		// thousand wound up a whirlwind of ghost fire out of the dark, its size its round at its foot, its value its height.
		SOUL_VORTEX("soul_vortex", Form.DRAWN, "descend"),
		// Black and White Impermanence (GhostSpells#Impermanence; "try a diff style"): the White and the Black, tall figures in their robes and
		// hats, drawn as beings are (models of their own, painted: SpellDrawn), their size their height in men.
		WHITE_IMPERMANENCE_FIGURE("white_impermanence_figure", Form.DRAWN, "strike", "take"),
		BLACK_IMPERMANENCE_FIGURE("black_impermanence_figure", Form.DRAWN, "strike", "take"),
		// The earth's great spells made of the world's own blocks (Form BLOCKS: EarthMasses; the user's ask, 2026-10-25: "rework falling mountain,
		// to look more like falling continent and do that for most earth skill the GeckoLib stuff still just looks a bit weird"), each its size
		// in blocks: Falling Mountain's peak (its round at its foot); Buzhou's broken pillar (its length); the Seal of Ten Thousand Mountains (half
		// its width); Gravestone Falls' stele on its tortoise (its height); a cauldron of the Nine (its height); a standing stone of the Buried
		// World (its height); Xirang's clod (its round); the dome of the Earth Veins (its round); the Hand of Houtu (its fist's reach from its
		// wrist's foot).
		PEAK_MASS("peak_mass", Form.BLOCKS, "impact"),
		PILLAR_MASS("pillar_mass", Form.BLOCKS, "impact"),
		SEAL_MASS("seal_mass", Form.BLOCKS, "stamp"),
		GRAVESTONE_MASS("gravestone_mass", Form.BLOCKS, "land", "sink"),
		CAULDRON_MASS("cauldron_mass", Form.BLOCKS, "land", "rise"),
		MENHIR_MASS("menhir_mass", Form.BLOCKS, "topple"),
		CLOD_MASS("clod_mass", Form.BLOCKS, "swell", "settle"),
		VEIN_DOME_MASS("vein_dome_mass", Form.BLOCKS, "release"),
		HAND_MASS("hand_mass", Form.BLOCKS, "grasp", "slam"),
		// Heaven-Demon Avatar (HeavenDemonSpells#Avatar; the user's ask, 2026-10-25, to redo it): a colossal projection of its caster's own body
		// in heaven's gold and the demon's crimson, drawn as a spirit is (Form GHOSTS: SpellGhosts), its size its height in men, its target its
		// caster (whose body it is); and its hand come down out of the sky, its caster's own arm as great, its value 1 heaven's palm, 2 the claw.
		AVATAR_PROJECTION("avatar_projection", Form.GHOSTS, "strike_palm", "strike_claw"),
		AVATAR_HAND("avatar_hand", Form.GHOSTS, "press"),
		// Twenty-Four Sea-Fixing Pearls (WaterSpells#Pearls; the user's ask, 2026-10-25: "looks stupid"): a pearl drawn by hand (SpellDrawn), its
		// size its round: lustrous, the sea turning in it, its water streaming after it as it flies. Ocean-Sealing Vase's drinking (WaterSpells#Vase;
		// "needs polish"): the water drawn up out of the land into the vase's mouth in twisting streams over a whirlpool, its size the whirlpool's
		// round, its value how high the mouth stands over it.
		PEARL("pearl", Form.DRAWN),
		VASE_VORTEX("vase_vortex", Form.DRAWN),
		// More made of the world's own blocks (Form BLOCKS: SpellMasses; the user's ask, 2026-10-25, "polish all the skills again"), each its size
		// in blocks: the ice moon that falls (its round), the crescent of ice over the well (its round), Blazing Meteor's rock (its round), the
		// Soul-Locking Pagoda (its round at its foot; its value its height), a length of Fengdu's walls (its length; its value its height), a span
		// of the Naihe Bridge (its length; its value the half of its width). And a sun drawn by hand, blazing, its fire streaming after it as it
		// falls (Form DRAWN: Nine Suns Descend's suns), its size its round.
		MOON_MASS("moon_mass", Form.BLOCKS, "shatter"),
		CRESCENT_MASS("crescent_mass", Form.BLOCKS, "shatter"),
		METEOR_MASS("meteor_mass", Form.BLOCKS, "impact"),
		PAGODA_MASS("pagoda_mass", Form.BLOCKS, "land", "seal"),
		WALL_MASS("wall_mass", Form.BLOCKS, "fall"),
		BRIDGE_MASS("bridge_mass", Form.BLOCKS),
		SUN("sun", Form.DRAWN, "impact");

		public final String id, enter, loop;
		public final float scale;
		public final String[] moves;
		public final Form form;
		/** Ticks it takes to fade in as it forms (SpellConstructRenderer): {@link #FADE}, or fewer for a shape that comes and goes in a moment. */
		public final int fadeIn;

		Kind(String id, String enter, String loop, float scale, String... moves) {
			this(id, FADE, enter, loop, scale, moves);
		}

		Kind(String id, int fadeIn, String enter, String loop, float scale, String... moves) {
			this.id = id;
			this.fadeIn = fadeIn;
			this.enter = enter;
			this.loop = loop;
			this.scale = scale;
			this.moves = moves;
			this.form = Form.SHAPE;
		}

		/**
		 * A tear in space, a tendril, or ghosts drawn by hand: no model; its size a tear's length in blocks, a tendril's girth, the ghosts' own
		 * measure; its {@code moves} (the ghosts') called as phases its watchers draw it by ({@link #act}, {@link #phase}).
		 */
		Kind(String id, Form form, String... moves) {
			this.id = id;
			this.fadeIn = FADE;
			this.enter = null;
			this.loop = null;
			this.scale = 1.0f;
			this.moves = moves;
			this.form = form;
		}

		/** One of GeckoLib's models (not a tear in space, which the client draws by its own hand). */
		public boolean modelled() { return form == Form.SHAPE; }

		public static Kind byIndex(int index) {
			return index >= 0 && index < values().length ? values()[index] : AZURE_DRAGON;
		}
	}

	/** Ticks it takes to fade in as it forms and out as it goes (SpellConstructRenderer). */
	public static final int FADE = 10;
	/** Ticks its animations ease from one into the next. */
	private static final int EASE = 3;

	/**
	 * Its kind, by its place among them: an int (it was a byte, and past the 128th kind, the 2026-10-25 spell overhaul's, a byte went
	 * negative and every later kind was read as the first, the Azure Dragon, on the server and every screen alike: "every spell uses the
	 * azure dragon model").
	 */
	private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(SpellConstruct.class, EntityDataSerializers.INT);
	/** Drawn this much larger (or smaller) than its kind's own size. */
	private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(SpellConstruct.class, EntityDataSerializers.FLOAT);
	/** How long it lasts in all, in ticks (it fades in over its first ticks and out over its last: SpellConstructRenderer). */
	private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(SpellConstruct.class, EntityDataSerializers.INT);
	/** Its caster's entity id (0: none). */
	private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(SpellConstruct.class, EntityDataSerializers.INT);
	/** A tear in space's glow (0xRRGGBB: what comes through it; 0, the void's own violet). */
	private static final EntityDataAccessor<Integer> TINT = SynchedEntityData.defineId(SpellConstruct.class, EntityDataSerializers.INT);
	/** The entity id of the being a tendril holds (0: none). */
	private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(SpellConstruct.class, EntityDataSerializers.INT);
	/** The being it is held on (SpellAnchor: 0, none), how, and where on it (x to its right, y up, z ahead). */
	private static final EntityDataAccessor<Integer> ANCHOR = SynchedEntityData.defineId(SpellConstruct.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Byte> ANCHOR_HOW = SynchedEntityData.defineId(SpellConstruct.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<org.joml.Vector3f> ANCHOR_OFFSET = SynchedEntityData.defineId(SpellConstruct.class, EntityDataSerializers.VECTOR3);
	/**
	 * A shape drawn by hand's phase (one of its kind's moves called: {@link #act}; 0, none yet) and the tick it was called at, so its
	 * watchers draw it from the moment it began; and a number of its kind's own (how far a procession pours out, how high a helix rises).
	 */
	private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(SpellConstruct.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> PHASE_AT = SynchedEntityData.defineId(SpellConstruct.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> VALUE = SynchedEntityData.defineId(SpellConstruct.class, EntityDataSerializers.FLOAT);
	/** The game time it was made (the server's): a watcher who came late still knows how far through its course it is ({@link #flown}). */
	private static final EntityDataAccessor<Integer> BORN = SynchedEntityData.defineId(SpellConstruct.class, EntityDataSerializers.INT);

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	/** Whether it is fading away (its spell done with it): it goes when its life is out. */
	private boolean dissolving;

	public SpellConstruct(EntityType<? extends SpellConstruct> type, Level level) {
		super(type, level);
		noPhysics = true;
		noCulling = true;
	}

	/**
	 * A construct of {@code kind} at {@code at}, facing {@code yaw} and {@code pitch} (degrees), lasting {@code life} ticks: its spell's
	 * whole time, so it has faded out by the time its spell {@link #dissolve dissolves} it.
	 */
	public static SpellConstruct create(Level level, Kind kind, Vec3 at, float yaw, float pitch, int life, Entity owner) {
		SpellConstruct construct = new SpellConstruct(ModEntities.SPELL_CONSTRUCT, level);
		construct.entityData.set(KIND, kind.ordinal());
		construct.entityData.set(LIFE, life);
		construct.entityData.set(OWNER, owner == null ? 0 : owner.getId());
		construct.entityData.set(BORN, (int) level.getGameTime());
		construct.moveTo(at.x, at.y, at.z, yaw, pitch);
		return construct;
	}

	/** Adds it to its level. */
	public SpellConstruct spawn() {
		level().addFreshEntity(this);
		return this;
	}

	public SpellConstruct size(float size) { entityData.set(SIZE, size); return this; }

	/**
	 * Drawn as great as {@code spell}'s realm (Spell#realm: an element's, a ghost foundation's, both paths') makes a spell beside one of Four
	 * Axis (HeavenSpells#greatness), as its reach and areas are.
	 */
	public SpellConstruct sizedFor(Spell spell) { return size((float) HeavenSpells.greatness(spell.realm())); }
	public SpellConstruct life(int life) { entityData.set(LIFE, life); return this; }
	/** A tear in space glowing {@code rgb} (0xRRGGBB): the colour of what comes through it. */
	public SpellConstruct tint(int rgb) { entityData.set(TINT, rgb & 0xFFFFFF); return this; }

	/** A tendril holding {@code held} (its watchers draw it reaching to it, wherever it goes). */
	public SpellConstruct target(Entity held) { entityData.set(TARGET, held == null ? 0 : held.getId()); return this; }

	/**
	 * Held on {@code on} ({@link SpellAnchor}: the user's ask, 2026-10-25, a shape worn on its caster lagged a step behind them): drawn where
	 * {@code on} is drawn each frame, {@code offset} on it, turned {@code how}; its spell's {@link #place} keeps it near for the server's sake.
	 */
	public SpellConstruct anchor(Entity on, SpellAnchor how, Vec3 offset) {
		entityData.set(ANCHOR, on == null ? 0 : on.getId());
		entityData.set(ANCHOR_HOW, (byte) how.ordinal());
		entityData.set(ANCHOR_OFFSET, offset.toVector3f());
		if (on != null) moveTo(how.at(on, offset, 1, getYRot()));
		return this;
	}

	/** The being it is held on, or null. */
	public Entity anchored() {
		int id = entityData.get(ANCHOR);
		return id <= 0 || SpellAnchor.byIndex(entityData.get(ANCHOR_HOW)) == SpellAnchor.NONE ? null : level().getEntity(id);
	}

	public SpellAnchor anchoring() { return SpellAnchor.byIndex(entityData.get(ANCHOR_HOW)); }

	private Vec3 anchorOffset() { return new Vec3(entityData.get(ANCHOR_OFFSET)); }

	/**
	 * How far from where it would be drawn this frame (its own place, eased between ticks) its watchers draw it: nothing, unless it is held
	 * on a being ({@link #anchor}), then the way to where that being is drawn this very frame.
	 */
	public Vec3 anchorShift(float partial) {
		Entity on = anchored();
		if (on == null) return Vec3.ZERO;
		Vec3 own = new Vec3(net.minecraft.util.Mth.lerp(partial, xo, getX()), net.minecraft.util.Mth.lerp(partial, yo, getY()), net.minecraft.util.Mth.lerp(partial, zo, getZ()));
		return anchoring().at(on, anchorOffset(), partial, drawnYaw(partial)).subtract(own);
	}

	/** The yaw it is drawn facing this frame: its own, eased between ticks, or (held on a being) as that being turns. */
	public float drawnYaw(float partial) {
		float own = net.minecraft.util.Mth.rotLerp(partial, yRotO, getYRot());
		Entity on = anchored();
		return on == null ? own : anchoring().yaw(on, partial, own);
	}

	/** The pitch it is drawn facing this frame, likewise. */
	public float drawnPitch(float partial) {
		float own = net.minecraft.util.Mth.lerp(partial, xRotO, getXRot());
		Entity on = anchored();
		return on == null ? own : anchoring().pitch(on, partial, own);
	}

	public Kind kind() { return Kind.byIndex(entityData.get(KIND)); }
	public float size() { return entityData.get(SIZE); }
	public int life() { return entityData.get(LIFE); }
	public int tint() { return entityData.get(TINT); }

	/** The being a tendril holds, or null. */
	public Entity target() {
		int id = entityData.get(TARGET);
		return id <= 0 ? null : level().getEntity(id);
	}

	public Entity owner() {
		int id = entityData.get(OWNER);
		return id <= 0 ? null : level().getEntity(id);
	}

	/** Moves it to {@code at}, facing {@code yaw} and {@code pitch} (its spell does, tick by tick; its watchers' copies follow). */
	public void place(Vec3 at, float yaw, float pitch) {
		setPos(at.x, at.y, at.z);
		setYRot(yaw);
		setXRot(pitch);
	}

	/**
	 * Plays one of its kind's moves once ({@link Kind#moves}: a roar, a dive, a strike), on every watcher's screen; for a shape drawn by hand,
	 * begins it as its phase ({@link #phase}).
	 */
	public void act(String move) {
		if (!kind().modelled()) {
			String[] moves = kind().moves;
			for (int i = 0; i < moves.length; i++) {
				if (moves[i].equals(move)) {
					entityData.set(PHASE, i + 1);
					entityData.set(PHASE_AT, tickCount);
				}
			}
			return;
		}
		triggerAnim("main", move);
	}

	/** Its phase now: the move last called ({@link #act}), or null before any. */
	public String phase() {
		int i = entityData.get(PHASE);
		String[] moves = kind().moves;
		return i <= 0 || i > moves.length ? null : moves[i - 1];
	}

	/** How long its phase has run, in ticks ({@code partial} of this one in): 0 before any. */
	public float phaseAge(float partial) {
		return entityData.get(PHASE) <= 0 ? 0 : Math.max(0, tickCount + partial - entityData.get(PHASE_AT));
	}

	/** Its kind's own number (a procession's reach, a helix's height). */
	public SpellConstruct value(float value) { entityData.set(VALUE, value); return this; }
	public float value() { return entityData.get(VALUE); }

	/**
	 * Lets it go as its spell ends with it: it fades out over what is left of its last {@link #FADE} ticks and is gone (taken away at once,
	 * a shape still bright blinked out; one whose life its spell's own time spent is already faded, and goes now).
	 */
	public void dissolve() {
		if (isRemoved()) return;
		// Where the world no longer moves what is in it (flown on out past it, as the Azure Dragon does into the sky), it would never fade nor go
		// (only its own tick takes it away), hanging there for good: taken away at once (found checking toast's push in game, 2026-10-26).
		if (level() instanceof net.minecraft.server.level.ServerLevel server && !server.isPositionEntityTicking(blockPosition())) {
			discard();
			return;
		}
		dissolving = true;
		entityData.set(LIFE, Math.min(life(), tickCount + FADE));
	}

	@Override
	protected void defineSynchedData() {
		entityData.define(KIND, 0);
		entityData.define(SIZE, 1.0f);
		entityData.define(LIFE, 20);
		entityData.define(OWNER, 0);
		entityData.define(TINT, 0);
		entityData.define(TARGET, 0);
		entityData.define(ANCHOR, 0);
		entityData.define(ANCHOR_HOW, (byte) 0);
		entityData.define(ANCHOR_OFFSET, new org.joml.Vector3f());
		entityData.define(PHASE, 0);
		entityData.define(PHASE_AT, 0);
		entityData.define(VALUE, 0.0f);
		entityData.define(BORN, 0);
	}

	/**
	 * How many ticks it has been since it was made ({@code partial} of this one in), by the world's clock: the same on every watcher's screen
	 * however late it came into sight (a flight laid out along a path worked out alike on both sides: DragonFlight).
	 */
	public double flown(float partial) {
		return (int) level().getGameTime() - entityData.get(BORN) + partial;
	}

	/** Where its spell last put it, as a watcher heard it ({@link #lerpTo}), and whether that is still to be taken. */
	private double lerpX, lerpY, lerpZ;
	private float lerpYRot, lerpXRot;
	private boolean lerping;

	/**
	 * Where its spell has moved it, heard by a watcher: taken at their next tick ({@link #tick}), not at once as an entity's place is by default,
	 * so it is drawn gliding there over that tick. Taken at once, a construct its spell moves every tick is drawn standing still through a tick
	 * and leaping the next, as the word falls between the watcher's ticks (a sun falling two blocks a tick, 2026-10-25).
	 */
	@Override
	public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
		lerpX = x;
		lerpY = y;
		lerpZ = z;
		lerpYRot = yRot;
		lerpXRot = xRot;
		lerping = true;
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide) {
			if (lerping) {
				lerping = false;
				setPos(lerpX, lerpY, lerpZ);
				setRot(lerpYRot, lerpXRot);
			}
			// Held on a being: kept where it is (it is drawn where that being is drawn each frame: #anchorShift).
			Entity on = anchored();
			if (on != null) setPos(anchoring().at(on, anchorOffset(), 1, getYRot()));
			if (!kind().modelled()) motes();
			return;
		}
		// Dissolved, it goes once faded (a tick or two after, its watchers' clocks a little behind); else its spell's end dissolves it, and
		// this is only lest one be left behind (its spell gone with the server or its caster).
		if (tickCount > life() + (dissolving ? 2 : 100)) discard();
	}

	/** The void's own violet: a tear's glow where its spell gives it none ({@link #tint}). */
	public static final int VOID = 0xB070FF;

	/**
	 * A tear in space's motes, on its watchers' screens while it stands open: sparks drawn in toward its heart (along its cut, out of its
	 * abyss, off its shattering) and wisps of its glow; more the greater it is.
	 */
	private void motes() {
		// A tendril's light is its own (TendrilRenderer), and so are the ghosts' (SpellGhosts).
		if (kind().form == Form.TENDRIL || kind().form == Form.GHOSTS || kind().form == Form.BLOCKS || kind().form == Form.DRAWN || tickCount < 6
				|| tickCount > life() - FADE) return;
		float length = size() * kind().scale;
		int rgb = tint() != 0 ? tint() : VOID;
		double r = (rgb >> 16 & 255) / 255.0, g = (rgb >> 8 & 255) / 255.0, b = (rgb & 255) / 255.0;
		net.minecraft.util.RandomSource random = level().random;
		Vec3 at = position();
		int count = Math.min(6, 1 + (int) (length / 14));
		for (int i = 0; i < count; i++) {
			Vec3 from, way;
			switch (kind().form) {
				case LINE -> {
					Vec3 along = Vec3.directionFromRotation(getXRot(), getYRot());
					Vec3 on = at.add(along.scale(random.nextDouble() * length));
					from = on.add(random.nextGaussian() * 1.2, random.nextGaussian() * 1.2, random.nextGaussian() * 1.2);
					way = on.subtract(from).scale(0.06);
				}
				case ABYSS -> {
					double angle = random.nextDouble() * Math.PI * 2, out = Math.sqrt(random.nextDouble()) * length * 0.4;
					from = at.add(Math.cos(angle) * out, 0.2, Math.sin(angle) * out);
					way = new Vec3(-Math.cos(angle) * 0.04, 0.12 + random.nextDouble() * 0.1, -Math.sin(angle) * 0.04);
				}
				case SHATTER -> {
					Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
					from = at.add(dir.scale(length * 0.1 + random.nextDouble() * length * 0.35));
					way = dir.scale(0.05 + random.nextDouble() * 0.08);
				}
				default -> {
					// A tear stands upright, its heart at its middle: sparks drawn into it from round about.
					Vec3 dir = new Vec3(random.nextGaussian() * 0.6, random.nextGaussian(), random.nextGaussian() * 0.6).normalize();
					from = at.add(dir.scale(length * (0.35 + random.nextDouble() * 0.3)));
					way = at.subtract(from).scale(0.045);
				}
			}
			level().addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD, from.x, from.y, from.z, way.x, way.y, way.z);
			if (random.nextInt(2) == 0) level().addParticle(net.minecraft.core.particles.ParticleTypes.ENTITY_EFFECT, from.x, from.y, from.z, r, g, b);
		}
	}

	/** Whether a move of its has played (on its watchers' screens; after one, only its loop is asked for). */
	private boolean entered;

	/**
	 * Its one controller: its enter move then its loop, and its moves when its spell calls them. It eases {@link #EASE} ticks into a move
	 * called (from wherever its loop had got to), and goes straight into all else: its first pose (easing into it from its model's rest
	 * showed a tree shrinking into its seedling and a seal's imprint into nothing as they formed), and its loop after its enter or a move
	 * (each ends where its loop begins: tools/spell-art, SpellArt#carryTheLoop; easing there only held it still a moment, its flames
	 * stopped). Once a move has played only the loop is asked for: GeckoLib reloads what is asked for whenever a called move ends, so
	 * asking for the enter and the loop still played the enter again after every move (a tortoise rising afresh after each strike of its
	 * serpent; all seen checking them in game, 2026-10-24).
	 */
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		Kind kind = kind();
		if (!kind.modelled()) return; // a tear in space: drawn by the client's own hand, nothing of GeckoLib's to move
		RawAnimation loop = RawAnimation.begin().thenLoop(kind.loop), going = kind.enter == null ? loop : RawAnimation.begin().thenPlay(kind.enter).thenLoop(kind.loop);
		AnimationController<SpellConstruct> main = new AnimationController<>(this, "main", 0, state -> {
			AnimationProcessor.QueuedAnimation playing = state.getController().getCurrentAnimation();
			if (!entered && playing != null && !playing.animation().name().equals(kind.enter) && !playing.animation().name().equals(kind.loop)) entered = true;
			return state.setAndContinue(entered ? loop : going);
		}) {
			private boolean posed;

			@Override
			public void process(CoreGeoModel<SpellConstruct> model, AnimationState<SpellConstruct> state, Map<String, CoreGeoBone> bones, Map<String, BoneSnapshot> snapshots,
					double seekTime, boolean crashWhenCantFindBone) {
				// Chosen between transitions (one under way keeps its own: changed midway, GeckoLib eases from where it began).
				if (getAnimationState() != State.TRANSITIONING) transitionLength = posed && triggeredAnimation != null && !hasAnimationFinished() ? EASE : 0;
				super.process(model, state, bones, snapshots, seekTime, crashWhenCantFindBone);
				if (getCurrentAnimation() != null) posed = true;
			}
		};
		for (String move : kind.moves) main.triggerableAnim(move, RawAnimation.begin().thenPlay(move));
		controllers.add(main);
		// Its spins on a controller of their own, from its first moment, turning on through its enter and every move (tools/spell-art
		// SpellArt#splitSpins: carried into a move, a wheel was eased back to its loop's start, whole turns backwards in a moment).
		RawAnimation spins = RawAnimation.begin().thenLoop(kind.loop + "_spin");
		controllers.add(new AnimationController<>(this, "spin", 0, state -> state.setAndContinue(spins)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) { return distance < 384 * 384; }

	@Override
	public boolean isPickable() { return false; }

	@Override
	public boolean isPushable() { return false; }

	@Override
	public boolean canBeCollidedWith() { return false; }

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}
}
