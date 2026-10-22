package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The wishes of the lower realms' people (the user's ask, 2026-10-19): a settler of the Astral Realm's worlds (Settlements) has a
 * wish only a Four Axis cultivator can grant, of one of the four kinds, and granting it is how the axis of its kind is filled
 * (FourAxis), each in its own measure (the user's ask, 2026-10-20), if that is the axis the granter stands at. Asked under Ask in a
 * talk ("Their wish": TalkSystem); granted by the dialogue's own replies ({@link #ACTIONS} and on). The dead of Four Axis on the
 * ghost path grant them too ({@link #refusal}), a Ghost King walking among the living in its human form (CultivatorNpc#senses).
 * <ul>
 *   <li><b>Longevity</b>: one of theirs lies sick and fading. Pour qi into them (a quarter of one's pool), then take what years the
 *   grateful can spare, none of them, a few, a share, or all ({@link #takeYears}): the years taken fill the axis, and go onto one's
 *   own lifespan besides (PlayerCultivation#extraYears). The more taken, the less they think of you.</li>
 *   <li><b>Fortune</b>: their stores are bare. Bring what they lack (an item and a count), and take a share of their fortune in turn:
 *   spirit stones marked as theirs shared with you ({@link #give}), which fill the axis by their worth as one absorbs them
 *   (SpiritStoneItem#fillsFortune).</li>
 *   <li><b>Merit</b>: something preys on them ({@link Foe}: a beast, raiders, a sorcerer, the restless dead, a demonic cultivator...).
 *   Take it on, and it comes out of the land nearby, glowing; each of it slain by one's own hand fills the axis by its hearts
 *   ({@link #hearts}), and slain, all of it, with one's own hand among the slayers, the wish is granted ({@link #onThreatDeath}).</li>
 *   <li><b>Enlightenment</b>: a lesser cultivator is stuck at its wall. Sit in meditation within {@link #GUIDE_REACH} of it for
 *   {@link #GUIDE_SECONDS}: it breaks through a stage, the wish is granted, and the stage fills the axis. Or take its body
 *   (Possession) and carry it through a stage oneself, meditating in it ({@link #guideWithin}; the user's ask, 2026-10-19).</li>
 * </ul>
 * A granted wish is done; a new one comes to the settler {@link #RENEW_TICKS} later. The wish is saved on the settler (CultivatorNpc#wish).
 */
public final class Wishes {
	/** A Fortune wish's share of the settlement's fortune: this many Mid Spirit Stones and up to {@link #SHARE_MORE} more. */
	public static final int SHARE_STONES = 3, SHARE_MORE = 2;
	/** The dialogue's reply ids for a wish's own choices: grant it ({@code +0}), take no years ({@code +1}) ... all they can spare ({@code +4}). */
	public static final int ACTIONS = 200;
	/** How long after a wish is granted the settler has another. */
	public static final long RENEW_TICKS = 3L * 24000;
	/** A wish taken on and not seen through in this long lapses (a healed one's years, a threat that wandered off). */
	public static final long ACCEPT_TICKS = 24000;
	/** Healing the sick costs this share of one's pool. */
	public static final double HEAL_COST = 0.25;
	/** Guidance: this close to the one guided, in meditation, this many seconds. */
	public static final double GUIDE_REACH = 6;
	public static final int GUIDE_SECONDS = 60;
	/** A demonic cultivator preying on settlers is of these ranks: Nascent Soul Early to Heavenly Being Late. */
	public static final int DEMONIC_LOW = PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY),
			DEMONIC_HIGH = PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.LATE);
	/**
	 * Tags on a wish's threat: it is one; whose wish and whose taking-on it is; and the game time it lapses with the wish (an old
	 * threat come back with its land, its wish long lapsed, slips away).
	 */
	private static final String THREAT_TAG = "dth_wish_threat", THREAT_NPC = "dth_wish_npc:", THREAT_PLAYER = "dth_wish_player:",
			THREAT_UNTIL = "dth_wish_until:";
	/** How many variants each kind of wish but Merit has to say it (a Merit wish says what preys on them: its Foe's own line). */
	private static final int VARIANTS = 3;

	/** What a Fortune wish may ask for: an item and how many. */
	private record Want(Item item, int count) {}

	private static final List<Want> WANTS = List.of(new Want(Items.IRON_INGOT, 16), new Want(Items.BREAD, 24), new Want(Items.WHEAT, 32),
			new Want(Items.COAL, 24), new Want(Items.LEATHER, 12), new Want(Items.GOLD_INGOT, 8), new Want(Items.EMERALD, 6),
			new Want(Items.OAK_LOG, 32), new Want(Items.WHITE_WOOL, 16), new Want(Items.COOKED_BEEF, 16));

	/**
	 * What preys on a settlement, for a Merit wish to be rid of (its line: "wish.say.merit.&lt;id&gt;"): what comes out of the land
	 * when the wish is taken on ({@link #summon}). Saved by id.
	 */
	public enum Foe {
		/** A ravager. */
		BEAST("beast", true),
		/** A ravager, a pillager riding it. */
		RIDER("rider", true),
		/** An evoker. */
		SORCERER("sorcerer", true),
		/** An illusioner. */
		ILLUSIONIST("illusionist", true),
		/** Two pillagers and two vindicators. */
		RAIDERS("raiders", true),
		/** Three witches. */
		COVEN("coven", true),
		/** Four zombies (husks in dry lands) and two skeletons (strays in snow), helmed so the sun doesn't burn them. */
		UNDEAD("undead", true),
		/** Three skeletons on skeleton horses (the riders are the threat; the horses, mounts). */
		HORSEMEN("horsemen", true),
		/** Three spiders and two cave spiders. */
		BROOD("brood", true),
		/** Four wolves, angry at whoever takes them on. */
		PACK("pack", false),
		/** A cultivator of the demonic path ({@link #DEMONIC_LOW} to {@link #DEMONIC_HIGH}), out for whoever takes it on. */
		DEMONIC("demonic", false);

		private final String id;
		/** Made of monsters, which Peaceful takes from the world the moment they come: there, a demonic cultivator comes instead. */
		private final boolean monsters;

		Foe(String id, boolean monsters) {
			this.id = id;
			this.monsters = monsters;
		}

		public String getId() { return id; }

		/** What comes for this threat on {@code difficulty}: itself, or on Peaceful (no monster lives there) a demonic cultivator. */
		public Foe on(Difficulty difficulty) {
			return monsters && difficulty == Difficulty.PEACEFUL ? DEMONIC : this;
		}

		public static Foe byId(String id) {
			for (Foe foe : values()) if (foe.id.equals(id)) return foe;
			return null;
		}
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register(Wishes::onThreatDeath);
		ServerTickEvents.END_SERVER_TICK.register(Wishes::tick);
		// Coming back with their land: a settler whose threat died far from it learns of it; a threat whose wish has lapsed slips away
		// (a tick on: not while it is being taken in).
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof CultivatorNpc npc && PENDING.containsKey(npc.getUUID())) {
				level.getServer().tell(new TickTask(level.getServer().getTickCount(), () -> open(npc)));
			} else if (isThreat(entity) && until(entity) < level.getGameTime()) {
				level.getServer().tell(new TickTask(level.getServer().getTickCount(), entity::discard));
			}
		});
		// A threat taken from the world without dying (Peaceful's clearing of monsters, a command): gone, by no hand of the taker's.
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
			if (entity.getRemovalReason() == Entity.RemovalReason.DISCARDED && isThreat(entity)) gone(level, entity, false);
		});
	}

	/** Whether {@code entity} is a Merit wish's threat (it hunts the one who took it on, and only them: CultivatorNpc, CultivatorGoals). */
	public static boolean isThreat(Entity entity) {
		return entity.getTags().contains(THREAT_TAG);
	}

	/** Who took on the wish {@code threat} came for, or null. */
	public static UUID takerOf(Entity threat) {
		for (String tag : threat.getTags()) if (tag.startsWith(THREAT_PLAYER)) return parse(tag.substring(THREAT_PLAYER.length()));
		return null;
	}

	// --- A wish's making ---

	/** Gives {@code npc} a new wish: any kind for a cultivator, any but Enlightenment for a mortal (there is no wall to see past). */
	public static void assign(CultivatorNpc npc, RandomSource random) {
		FourAxis.Axis[] kinds = FourAxis.Axis.values();
		FourAxis.Axis axis = npc.isMortal() ? kinds[random.nextInt(kinds.length - 1)] : kinds[random.nextInt(kinds.length)];
		CompoundTag wish = new CompoundTag();
		wish.putInt("Axis", axis.ordinal());
		wish.putInt("Variant", random.nextInt(VARIANTS));
		switch (axis) {
			case LONGEVITY -> wish.putInt("Spare", 20 + random.nextInt(81) + (npc.isMortal() ? 0 : 40 * (1 + CultivatorNpc.realmOf(npc.getRank()).ordinal())));
			case FORTUNE -> {
				Want want = WANTS.get(random.nextInt(WANTS.size()));
				wish.putString("Item", BuiltInRegistries.ITEM.getKey(want.item()).toString());
				wish.putInt("Count", want.count());
			}
			case MERIT -> wish.putString("Foe", Foe.values()[random.nextInt(Foe.values().length)].getId());
			default -> { }
		}
		npc.setWish(wish);
	}

	/** {@code npc}'s wish if it has one waiting (a granted one renewed in its time; one taken on and left lapses, see {@link #lapsed}). */
	public static CompoundTag open(CultivatorNpc npc) {
		// Its threat's deaths while its land lay unloaded, learned now.
		List<Death> pending = PENDING.remove(npc.getUUID());
		if (pending != null && npc.level() instanceof ServerLevel level) apply(level.getServer(), npc, pending);
		CompoundTag wish = npc.wish();
		if (wish == null) return null;
		if (wish.contains("Done")) {
			if (npc.level().getGameTime() - wish.getLong("Done") < RENEW_TICKS) return null;
			assign(npc, npc.getRandom());
			wish = npc.wish();
		}
		// A threat taken on before threats came many (its one creature as "Threat"): the list it is kept in now.
		if (wish.hasUUID("Threat") && !wish.contains("Foes")) {
			ListTag one = new ListTag();
			one.add(NbtUtils.createUUID(wish.getUUID("Threat")));
			wish.put("Foes", one);
			wish.remove("Threat");
		}
		if (wish.hasUUID("Accepted") && lapsed(npc, wish)) {
			// Cleared first, so its creatures leaving (below) find no wish waiting on them.
			ListTag foes = wish.getList("Foes", Tag.TAG_INT_ARRAY).copy();
			clearTaken(wish);
			discardFoes(npc, foes);
			Settlements.endAlarm(npc);
		}
		return wish;
	}

	/**
	 * Whether a wish taken on has been left: a guidance no longer under way (its guide gone, or a restart forgot it), or a healing or
	 * a threat a day old (the healer never came back for the years, the threat wandered off or unloaded). It can be taken on afresh.
	 */
	private static boolean lapsed(CultivatorNpc npc, CompoundTag wish) {
		if (axisOf(wish) == FourAxis.Axis.ENLIGHTENMENT) return !npc.getUUID().equals(GUIDING.get(wish.getUUID("Accepted")));
		return npc.level().getGameTime() - wish.getLong("AcceptedAt") > ACCEPT_TICKS;
	}

	/** Everything of a wish's taking-on, gone: open to be taken on again. */
	private static void clearTaken(CompoundTag wish) {
		for (String key : new String[] {"Healed", "Accepted", "AcceptedAt", "Threat", "Foes", "Slain", "Guided"}) wish.remove(key);
	}

	public static FourAxis.Axis axisOf(CompoundTag wish) {
		return FourAxis.Axis.byIndex(wish.getInt("Axis"));
	}

	/** A Merit wish's threat (saves from before the foes were many: a ravager or an evoker). */
	public static Foe foeOf(CompoundTag wish) {
		Foe foe = Foe.byId(wish.getString("Foe"));
		return foe != null ? foe : wish.getBoolean("Beast") ? Foe.BEAST : Foe.SORCERER;
	}

	private static Item item(CompoundTag wish) {
		Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(wish.getString("Item")));
		return item == null || item == Items.AIR ? Items.BREAD : item;
	}

	// --- In a talk (TalkSystem) ---

	/** The talk's "Their wish" line, if {@code npc} has one: open to one who could grant it, said why not to anyone else. */
	public static TalkSystem.Reply reply(ServerPlayer player, CultivatorNpc npc, int topicId) {
		CompoundTag wish = open(npc);
		if (wish == null) return null;
		Component label = Component.translatable(ModLang.talkTopic("wish"));
		String why = refusal(player, wish);
		return new TalkSystem.Reply(topicId, label, true, why == null ? null : Component.translatable(why, axisOf(wish).getDisplayName()));
	}

	/**
	 * Why {@code player} can't grant {@code wish} (a lang key taking the wish's axis), or null if they can. One's own record counts,
	 * whatever body one's soul is in; the dead grant by their ghost path, the one they cultivate (the user's ask, 2026-10-19: the dead
	 * of Four Axis on the ghost path fill their axes too).
	 */
	static String refusal(ServerPlayer player, CompoundTag wish) {
		PlayerCultivation c = CultivationManager.own(player);
		boolean dead = c.netherworld().ghost();
		int rank = dead ? TwoPaths.ghostRank(c, false) : TwoPaths.higherRank(c, false);
		if (rank < PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.EARLY)) return dead ? ModLang.WISH_NOT_FOUR_AXIS_DEAD : ModLang.WISH_NOT_FOUR_AXIS;
		FourAxis.Axis current = FourAxis.current(c);
		if (current == null) return FourAxis.allComplete(c) ? ModLang.WISH_AXES_DONE : ModLang.WISH_AXIS_DONE;
		return current == axisOf(wish) ? null : ModLang.WISH_OTHER_AXIS;
	}

	/** What {@code npc} says of its wish, and how it stands with {@code player}'s taking it on. */
	public static Component tell(ServerPlayer player, CultivatorNpc npc) {
		CompoundTag wish = open(npc);
		if (wish == null) return Component.translatable(ModLang.WISH_NONE);
		FourAxis.Axis axis = axisOf(wish);
		String key = axis == FourAxis.Axis.MERIT ? ModLang.WISH_SAY + "merit." + foeOf(wish).getId()
				: ModLang.WISH_SAY + axis.getId() + "." + (1 + wish.getInt("Variant") % VARIANTS);
		Component said = axis == FourAxis.Axis.FORTUNE ? Component.translatable(key, wish.getInt("Count"), item(wish).getDescription())
				: Component.translatable(key);
		if (wish.getBoolean("Healed")) return Component.translatable(ModLang.WISH_HEALED, wish.getInt("Spare"));
		if (wish.hasUUID("Accepted")) {
			if (axis != FourAxis.Axis.MERIT) return Component.translatable(ModLang.WISH_GUIDE_SIT, GUIDE_SECONDS - wish.getInt("Guided"));
			int left = wish.getList("Foes", Tag.TAG_INT_ARRAY).size();
			return left > 1 ? Component.translatable(ModLang.WISH_THREAT_OUT_MANY, left) : Component.translatable(ModLang.WISH_THREAT_OUT);
		}
		String why = refusal(player, wish);
		if (why != null) return Component.empty().append(said).append(" ").append(Component.translatable(why, axis.getDisplayName()));
		return said;
	}

	/** The wish's own choices in the dialogue: grant it; once a sick one is healed, how many years to take. */
	public static List<TalkSystem.Reply> actions(ServerPlayer player, CultivatorNpc npc) {
		List<TalkSystem.Reply> replies = new ArrayList<>();
		CompoundTag wish = open(npc);
		if (wish == null || refusal(player, wish) != null) return replies;
		if (wish.getBoolean("Healed")) {
			if (!healer(player, wish)) return replies;
			int spare = wish.getInt("Spare");
			replies.add(new TalkSystem.Reply(ACTIONS + 1, Component.translatable(ModLang.WISH_TAKE_NONE), true, null));
			replies.add(new TalkSystem.Reply(ACTIONS + 2, Component.translatable(ModLang.WISH_TAKE_FEW, spare / 4), true, null));
			replies.add(new TalkSystem.Reply(ACTIONS + 3, Component.translatable(ModLang.WISH_TAKE_SHARE, spare / 2), true, null));
			replies.add(new TalkSystem.Reply(ACTIONS + 4, Component.translatable(ModLang.WISH_TAKE_ALL, spare), true, null));
		} else if (!wish.hasUUID("Accepted")) {
			String grant = switch (axisOf(wish)) {
				case LONGEVITY -> ModLang.WISH_GRANT_HEAL;
				case FORTUNE -> ModLang.WISH_GRANT_GIVE;
				case MERIT -> ModLang.WISH_GRANT_THREAT;
				case ENLIGHTENMENT -> ModLang.WISH_GRANT_GUIDE;
			};
			Component label = axisOf(wish) == FourAxis.Axis.FORTUNE ? Component.translatable(grant, wish.getInt("Count"), item(wish).getDescription())
					: Component.translatable(grant);
			replies.add(new TalkSystem.Reply(ACTIONS, label, true, null));
		}
		return replies;
	}

	/** {@code player} chose one of the wish's own replies ({@code action}: 0 grant, 1-4 the years taken). @return what the settler says */
	public static Component act(ServerPlayer player, CultivatorNpc npc, int action) {
		CompoundTag wish = open(npc);
		if (wish == null) return Component.translatable(ModLang.WISH_NONE);
		String why = refusal(player, wish);
		if (why != null) return Component.translatable(why, axisOf(wish).getDisplayName());
		FourAxis.Axis axis = axisOf(wish);
		if (action >= 1 && action <= 4) {
			return wish.getBoolean("Healed") && healer(player, wish) ? takeYears(player, npc, wish, action) : tell(player, npc);
		}
		if (action != 0 || wish.getBoolean("Healed") || wish.hasUUID("Accepted")) return tell(player, npc);
		return switch (axis) {
			case LONGEVITY -> heal(player, npc, wish);
			case FORTUNE -> give(player, npc, wish);
			case MERIT -> takeOnThreat(player, npc, wish);
			case ENLIGHTENMENT -> {
				wish.putUUID("Accepted", player.getUUID());
				wish.putInt("Guided", 0);
				GUIDING.put(player.getUUID(), npc.getUUID());
				yield Component.translatable(ModLang.WISH_GUIDE_SIT, GUIDE_SECONDS);
			}
		};
	}

	// --- Longevity ---

	private static Component heal(ServerPlayer player, CultivatorNpc npc, CompoundTag wish) {
		PlayerCultivation c = CultivationManager.own(player);
		// The dead's pool is the ghost path's; the living heal from the higher path's.
		TwoPaths.Path path = c.netherworld().ghost() || TwoPaths.ghostRank(c, false) > TwoPaths.heavenRank(c, false) ? TwoPaths.Path.GHOST : TwoPaths.Path.HEAVEN;
		double cost = TwoPaths.poolMax(c, path) * HEAL_COST;
		if (!TwoPaths.spend(c, path, cost)) return Component.translatable(ModLang.WISH_HEAL_QI, Math.round(HEAL_COST * 100));
		CultivationManager.markDirty(player.server);
		CultivationManager.sync(player);
		wish.putBoolean("Healed", true);
		wish.putUUID("Accepted", player.getUUID());
		wish.putLong("AcceptedAt", npc.level().getGameTime());
		ServerLevel level = player.serverLevel();
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, npc.getX(), npc.getY() + 1, npc.getZ(), 30, 0.6, 0.8, 0.6, 0.02);
		level.playSound(null, npc.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0f, 1.2f);
		return Component.translatable(ModLang.WISH_HEALED, wish.getInt("Spare"));
	}

	/** Whether {@code player} healed the sick of {@code wish} (the years are theirs alone to take). */
	private static boolean healer(ServerPlayer player, CompoundTag wish) {
		return wish.hasUUID("Accepted") && wish.getUUID("Accepted").equals(player.getUUID());
	}

	/**
	 * The years taken from the healed: none ({@code 1}), a quarter ({@code 2}), half ({@code 3}) or all they could spare ({@code 4}).
	 * They fill the Axis of Longevity, and go onto one's own lifespan too: only while one stands at that axis, the one such a wish
	 * can be granted at ({@link #refusal}).
	 */
	private static Component takeYears(ServerPlayer player, CultivatorNpc npc, CompoundTag wish, int choice) {
		int spare = wish.getInt("Spare");
		int taken = switch (choice) {
			case 2 -> spare / 4;
			case 3 -> spare / 2;
			case 4 -> spare;
			default -> 0;
		};
		PlayerCultivation c = CultivationManager.own(player);
		c.setExtraYears(c.extraYears() + taken);
		// The more taken, the less they think of one; nothing taken, the more.
		npc.changeRegard(player, taken == 0 ? 30 : taken < spare / 2 ? 15 : taken < spare ? 5 : -10);
		done(npc, wish);
		FourAxis.add(player, FourAxis.Axis.LONGEVITY, taken);
		return Component.translatable(taken == 0 ? ModLang.WISH_TOOK_NONE : ModLang.WISH_TOOK, taken);
	}

	// --- Fortune ---

	private static Component give(ServerPlayer player, CultivatorNpc npc, CompoundTag wish) {
		Item item = item(wish);
		int count = wish.getInt("Count");
		if (player.getInventory().countItem(item) < count && !player.getAbilities().instabuild) {
			return Component.translatable(ModLang.WISH_GIVE_LACK, count, item.getDescription());
		}
		if (!player.getAbilities().instabuild) {
			player.getInventory().clearOrCountMatchingItems(stack -> stack.is(item), count, player.inventoryMenu.getCraftSlots());
		}
		// A share of their fortune in turn: spirit stones that fill the giver's Axis of Fortune as they absorb them.
		int stones = SHARE_STONES + npc.getRandom().nextInt(SHARE_MORE + 1);
		ItemStack share = SpiritStoneItem.fortune(new ItemStack(ModItems.MID_SPIRIT_STONE, stones), player);
		if (!player.getInventory().add(share)) player.drop(share, false);
		npc.changeRegard(player, 20);
		done(npc, wish);
		return Component.translatable(ModLang.WISH_GIVEN, stones);
	}

	// --- Merit ---

	private static Component takeOnThreat(ServerPlayer player, CultivatorNpc npc, CompoundTag wish) {
		ServerLevel level = player.serverLevel();
		RandomSource random = npc.getRandom();
		List<LivingEntity> foes = summon(level, foeOf(wish).on(level.getDifficulty()), threatGround(level, npc, random), random);
		if (foes.isEmpty()) return tell(player, npc);
		long until = level.getGameTime() + ACCEPT_TICKS;
		ListTag ids = new ListTag();
		for (LivingEntity foe : foes) {
			foe.addTag(THREAT_TAG);
			foe.addTag(THREAT_NPC + npc.getUUID());
			foe.addTag(THREAT_PLAYER + player.getUUID());
			foe.addTag(THREAT_UNTIL + until);
			foe.setGlowingTag(true);
			if (foe instanceof Mob mob) {
				mob.setPersistenceRequired();
				mob.setTarget(player);
			}
			if (foe instanceof NeutralMob neutral) {
				neutral.setPersistentAngerTarget(player.getUUID());
				neutral.startPersistentAngerTimer();
			}
			if (foe instanceof CultivatorNpc cultivator) cultivator.markAggressed(player); // it turned on them: slaying it is no murder
			ids.add(NbtUtils.createUUID(foe.getUUID()));
		}
		wish.putUUID("Accepted", player.getUUID());
		wish.putLong("AcceptedAt", level.getGameTime());
		wish.put("Foes", ids);
		wish.remove("Slain");
		Settlements.raiseAlarm(npc, until); // its people go home until it is gone (CultivatorGoals.ShelterGoal)
		return foes.size() > 1 ? Component.translatable(ModLang.WISH_THREAT_OUT_MANY, foes.size()) : Component.translatable(ModLang.WISH_THREAT_OUT);
	}

	/** The game time {@code npc}'s threat lapses while one is out for it (a Merit wish taken on, not all of it fallen yet); else -1. */
	public static long threatUntil(CultivatorNpc npc) {
		CompoundTag wish = npc.wish();
		if (wish == null || wish.contains("Done") || !wish.hasUUID("Accepted") || axisOf(wish) != FourAxis.Axis.MERIT
				|| wish.getList("Foes", Tag.TAG_INT_ARRAY).isEmpty()) {
			return -1;
		}
		return wish.getLong("AcceptedAt") + ACCEPT_TICKS;
	}

	/**
	 * What comes out of the land for a Merit wish ({@code foe}), round {@code at}, put in the world: the creatures to slay (a mount
	 * ridden by one of them comes too, but is no threat of its own).
	 */
	static List<LivingEntity> summon(ServerLevel level, Foe foe, BlockPos at, RandomSource random) {
		List<LivingEntity> foes = new ArrayList<>();
		List<Entity> roots = new ArrayList<>();
		switch (foe) {
			case BEAST -> add(foes, roots, mob(level, EntityType.RAVAGER, at, random));
			case RIDER -> {
				Ravager beast = mob(level, EntityType.RAVAGER, at, random);
				Pillager rider = mob(level, EntityType.PILLAGER, at, random);
				if (beast != null && rider != null) {
					rider.startRiding(beast, true);
					foes.add(beast);
					foes.add(rider);
					roots.add(beast);
				}
			}
			case SORCERER -> add(foes, roots, mob(level, EntityType.EVOKER, at, random));
			case ILLUSIONIST -> add(foes, roots, mob(level, EntityType.ILLUSIONER, at, random));
			case RAIDERS -> {
				for (int i = 0; i < 2; i++) {
					add(foes, roots, mob(level, EntityType.PILLAGER, at, random));
					add(foes, roots, mob(level, EntityType.VINDICATOR, at, random));
				}
			}
			case COVEN -> {
				for (int i = 0; i < 3; i++) add(foes, roots, mob(level, EntityType.WITCH, at, random));
			}
			case UNDEAD -> {
				Holder<Biome> biome = level.getBiome(at);
				EntityType<? extends Mob> walker = biome.is(Biomes.DESERT) || biome.is(BiomeTags.IS_BADLANDS) ? EntityType.HUSK : EntityType.ZOMBIE;
				EntityType<? extends Mob> archer = biome.value().coldEnoughToSnow(at) ? EntityType.STRAY : EntityType.SKELETON;
				for (int i = 0; i < 4; i++) add(foes, roots, helmed(mob(level, walker, at, random)));
				for (int i = 0; i < 2; i++) add(foes, roots, helmed(mob(level, archer, at, random)));
			}
			case HORSEMEN -> {
				for (int i = 0; i < 3; i++) {
					SkeletonHorse horse = mob(level, EntityType.SKELETON_HORSE, at, random);
					Skeleton rider = helmed(mob(level, EntityType.SKELETON, at, random));
					if (horse == null || rider == null) continue;
					horse.setTamed(true);
					horse.setPersistenceRequired();
					rider.startRiding(horse, true);
					foes.add(rider);
					roots.add(horse);
				}
			}
			case BROOD -> {
				for (int i = 0; i < 3; i++) add(foes, roots, mob(level, EntityType.SPIDER, at, random));
				for (int i = 0; i < 2; i++) add(foes, roots, mob(level, EntityType.CAVE_SPIDER, at, random));
			}
			case PACK -> {
				for (int i = 0; i < 4; i++) add(foes, roots, mob(level, EntityType.WOLF, at, random));
			}
			case DEMONIC -> {
				CultivatorNpc demon = CultivatorNpc.createFor(level);
				if (demon != null) {
					// A threat before it is anything else: becoming a rogue decides its course at once, and a threat's is to hunt
					// (a Heavenly Being of the lower realms would otherwise go off into seclusion: CultivatorNpc#decideRogueCourse).
					demon.addTag(THREAT_TAG);
					int alignment = Alignment.DEMONIC_BELOW - 10 - random.nextInt(Alignment.DEMONIC_BELOW - 10 - Alignment.MIN + 1);
					demon.becomeRogue(DEMONIC_LOW + random.nextInt(DEMONIC_HIGH - DEMONIC_LOW + 1), alignment, random);
					BlockPos spot = spot(level, at, random);
					demon.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, random.nextFloat() * 360, 0);
					add(foes, roots, demon);
				}
			}
		}
		for (Entity root : roots) level.addFreshEntityWithPassengers(root);
		return foes;
	}

	private static void add(List<LivingEntity> foes, List<Entity> roots, LivingEntity foe) {
		if (foe == null) return;
		foes.add(foe);
		roots.add(foe);
	}

	/** One of {@code type}, made ready (its gear, by vanilla's way) on open ground near {@code at}; not yet in the world. */
	private static <T extends Mob> T mob(ServerLevel level, EntityType<T> type, BlockPos at, RandomSource random) {
		T mob = type.create(level);
		if (mob == null) return null;
		BlockPos spot = spot(level, at, random);
		mob.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, random.nextFloat() * 360, 0);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.EVENT, null, null);
		return mob;
	}

	/** An unbreakable helm the sun can't get past (the dead come by day too), never dropped. */
	private static <T extends Mob> T helmed(T mob) {
		if (mob == null) return null;
		ItemStack helm = new ItemStack(Items.IRON_HELMET);
		helm.getOrCreateTag().putBoolean("Unbreakable", true);
		mob.setItemSlot(EquipmentSlot.HEAD, helm);
		mob.setDropChance(EquipmentSlot.HEAD, 0);
		return mob;
	}

	/**
	 * Where a threat comes out of the land: 24-36 blocks off the settler, on ground the world runs; failing that, nearer (10-17);
	 * failing that, by the settler. One put down where the world stands still would be hidden there, never coming, never found.
	 */
	private static BlockPos threatGround(ServerLevel level, CultivatorNpc npc, RandomSource random) {
		for (int tries = 0; tries < 12; tries++) {
			double angle = random.nextDouble() * Math.PI * 2;
			int distance = tries < 8 ? 24 + random.nextInt(13) : 10 + random.nextInt(8);
			BlockPos column = BlockPos.containing(npc.getX() + Math.cos(angle) * distance, npc.getY(), npc.getZ() + Math.sin(angle) * distance);
			if (level.isPositionEntityTicking(column)) return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
		}
		return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, npc.blockPosition());
	}

	/** Open ground a few blocks round {@code at} the world runs, out of the water where there is any. */
	private static BlockPos spot(ServerLevel level, BlockPos at, RandomSource random) {
		BlockPos first = null;
		for (int tries = 0; tries < 6; tries++) {
			BlockPos column = at.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
			if (!level.isPositionEntityTicking(column)) continue;
			BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
			if (first == null) first = pos;
			if (level.getBlockState(pos.below()).getFluidState().isEmpty()) return pos;
		}
		return first != null ? first : level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at);
	}

	/** A death of a wish's threat, kept for its settler while its land lies unloaded ({@link #PENDING}). */
	private record Death(UUID foe, boolean byTaker) {}

	/** Deaths of threats whose settler wasn't there to learn of them, by settler: applied when it is next about ({@link #open}). */
	private static final Map<UUID, List<Death>> PENDING = new HashMap<>();

	/**
	 * A wish's threat has died: one fewer of them, and whether it fell by the hand of the one who took it on. By their hand, its
	 * hearts fill their Axis of Merit (they needn't be online).
	 */
	static void onThreatDeath(LivingEntity entity, DamageSource source) {
		if (!(entity.level() instanceof ServerLevel level) || !isThreat(entity)) return;
		UUID taker = takerOf(entity);
		boolean byTaker = taker != null && byTaker(entity, source, taker);
		if (byTaker) FourAxis.add(level.getServer(), taker, FourAxis.Axis.MERIT, hearts(entity));
		gone(level, entity, byTaker);
	}

	/**
	 * What a threat slain is worth to the Axis of Merit: its hearts (its max health, halved), as it stood unpressed (Realm Suppress
	 * takes a share of it while it lasts; a cultivator's, by its own rank).
	 */
	static int hearts(LivingEntity threat) {
		double health = threat instanceof CultivatorNpc npc && !npc.isMortal()
				? npc.getAttributeBaseValue(Attributes.MAX_HEALTH)
						+ CultivationStats.maxHealth(CultivatorNpc.realmOf(npc.getRank()), CultivatorNpc.stageOf(npc.getRank()))
				: RealmSuppressSystem.unpressedMaxHealth(threat);
		return Math.max(1, (int) Math.round(health / 2));
	}

	/** One of a wish's threat gone (dead, or taken from the world): struck from its settler's list, now or when it is next about. */
	private static void gone(ServerLevel level, Entity entity, boolean byTaker) {
		UUID npcId = null;
		for (String tag : entity.getTags()) if (tag.startsWith(THREAT_NPC)) npcId = parse(tag.substring(THREAT_NPC.length()));
		if (npcId == null) return;
		Death death = new Death(entity.getUUID(), byTaker);
		if (level.getEntity(npcId) instanceof CultivatorNpc npc) apply(level.getServer(), npc, List.of(death));
		else PENDING.computeIfAbsent(npcId, id -> new ArrayList<>()).add(death);
	}

	/**
	 * Whether {@code dead} leaves its spoils (its loot, its gear): anything but a wish's threat does; a threat only when the one who
	 * took it on slew it. Slain by anything else (a trap, the settlers, a fall) its wish stands open to be taken on again, and its
	 * spoils with it would come without end (an evoker's Totem of Undying: found in the 2026-10-20 sweep). ThreatLootMixin asks it.
	 */
	public static boolean spoilsDrop(LivingEntity dead, DamageSource source) {
		if (!isThreat(dead)) return true;
		UUID taker = takerOf(dead);
		return taker != null && byTaker(dead, source, taker);
	}

	/** Whether {@code taker} slew it: their blow (or arrow, or spell), or the last of theirs it took. */
	private static boolean byTaker(LivingEntity dead, DamageSource source, UUID taker) {
		Entity killer = source == null ? null : source.getEntity();
		if (!(killer instanceof Player)) killer = dead.getKillCredit();
		return killer instanceof Player player && player.getUUID().equals(taker);
	}

	/**
	 * Deaths of {@code npc}'s threat: each struck from its list. With none left, the wish is granted to the one who took it on, if
	 * any fell by their hand (they needn't be online: their axis has it either way); else the threat is gone by other hands (the
	 * settlers', a fall) and the wish is open to be taken on again.
	 */
	private static void apply(MinecraftServer server, CultivatorNpc npc, List<Death> deaths) {
		CompoundTag wish = npc.wish();
		if (wish == null || wish.contains("Done") || !wish.hasUUID("Accepted") || axisOf(wish) != FourAxis.Axis.MERIT || !wish.contains("Foes")) return;
		ListTag foes = wish.getList("Foes", Tag.TAG_INT_ARRAY);
		boolean struck = false;
		for (Death death : deaths) {
			for (int i = 0; i < foes.size(); i++) {
				if (!NbtUtils.loadUUID(foes.get(i)).equals(death.foe())) continue;
				foes.remove(i);
				struck = true;
				if (death.byTaker()) wish.putBoolean("Slain", true);
				break;
			}
		}
		if (!struck || !foes.isEmpty()) return;
		UUID taker = wish.getUUID("Accepted");
		ServerPlayer player = server.getPlayerList().getPlayer(taker);
		Component name = Component.literal(npc.getPersonalName());
		if (!wish.getBoolean("Slain")) {
			clearTaken(wish);
			Settlements.endAlarm(npc);
			if (player != null) player.sendSystemMessage(Component.translatable(ModLang.WISH_THREAT_LOST, name).withStyle(ChatFormatting.GRAY));
			return;
		}
		if (player != null) npc.changeRegard(player, 25);
		done(npc, wish);
		if (player != null) player.sendSystemMessage(Component.translatable(ModLang.WISH_THREAT_SLAIN, name).withStyle(ChatFormatting.GOLD));
	}

	/** A lapsed threat's creatures ({@code foes}) still about, gone with it (any out of reach go when they come back: {@link #register}). */
	private static void discardFoes(CultivatorNpc npc, ListTag foes) {
		if (!(npc.level() instanceof ServerLevel level)) return;
		for (Tag id : foes) {
			Entity foe = level.getEntity(NbtUtils.loadUUID(id));
			if (foe != null) foe.discard();
		}
	}

	/** The game time a threat lapses with its wish (its tag), or forever for one from before threats lapsed. */
	private static long until(Entity entity) {
		for (String tag : entity.getTags()) {
			if (!tag.startsWith(THREAT_UNTIL)) continue;
			try {
				return Long.parseLong(tag.substring(THREAT_UNTIL.length()));
			} catch (NumberFormatException e) {
				return Long.MAX_VALUE;
			}
		}
		return Long.MAX_VALUE;
	}

	private static UUID parse(String id) {
		try {
			return UUID.fromString(id);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	// --- Enlightenment ---

	/** Who is guiding whom now (player to settler): kept while they're both about; a restart forgets it (they take it on again). */
	private static final Map<UUID, UUID> GUIDING = new HashMap<>();

	/**
	 * Once a second: each guidance under way counts a second while the guide meditates near the one guided; and each soul in a
	 * settler's body that has carried it through its wall grants its wish ({@link #guideWithin}).
	 */
	static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) return;
		for (Map.Entry<UUID, UUID> entry : new ArrayList<>(GUIDING.entrySet())) {
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				GUIDING.remove(entry.getKey());
				continue;
			}
			if (!(player.serverLevel().getEntity(entry.getValue()) instanceof CultivatorNpc npc)) continue;
			guide(player, npc);
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!Possession.inhabits(player.getUUID())) continue;
			CultivatorNpc body = Possession.npcBody(player);
			if (body != null) guideWithin(player, body);
		}
	}

	/** A second of guidance, if {@code player} meditates near {@code npc}. @return whether it counted */
	static boolean guide(ServerPlayer player, CultivatorNpc npc) {
		CompoundTag wish = npc.wish();
		if (wish == null || wish.contains("Done") || axisOf(wish) != FourAxis.Axis.ENLIGHTENMENT || !wish.hasUUID("Accepted")
				|| !wish.getUUID("Accepted").equals(player.getUUID())) {
			GUIDING.remove(player.getUUID());
			return false;
		}
		if (!MeditationManager.isMeditating(player.getUUID()) || player.distanceTo(npc) > GUIDE_REACH) return false;
		npc.setMeditating(true);
		int seconds = wish.getInt("Guided") + 1;
		wish.putInt("Guided", seconds);
		if (seconds < GUIDE_SECONDS) return true;
		GUIDING.remove(player.getUUID());
		npc.setMeditating(false);
		npc.setRank(npc.getRank() + 1);
		ServerLevel level = player.serverLevel();
		level.sendParticles(ParticleTypes.END_ROD, npc.getX(), npc.getY() + 1, npc.getZ(), 40, 0.5, 0.8, 0.5, 0.06);
		level.playSound(null, npc.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.8f, 1.2f);
		npc.changeRegard(player, 30);
		done(npc, wish);
		player.sendSystemMessage(Component.translatable(ModLang.WISH_GUIDED, Component.literal(npc.getPersonalName()),
				PlayerCultivation.rankName(CultivatorNpc.realmOf(npc.getRank()), CultivatorNpc.stageOf(npc.getRank()))).withStyle(ChatFormatting.GOLD));
		FourAxis.add(player, FourAxis.Axis.ENLIGHTENMENT, 1);
		return true;
	}

	/**
	 * Guidance from within (the user's ask, 2026-10-19): {@code soul} is in the body of {@code npc} (Possession), which wishes to be
	 * guided through its wall, and has cultivated it through a stage (meditating in it: its record has come past the rank it had,
	 * which it takes up when given back). The wish is granted, as if the soul had sat beside it, if it is one the soul could grant
	 * (its own Four Axis at the Axis of Enlightenment); else it waits for one who can. The settler knows only that it broke through:
	 * it thinks nothing more of anyone (whoever it would think of is itself, to it, while it is borrowed).
	 * @return whether the wish was granted
	 */
	static boolean guideWithin(ServerPlayer soul, CultivatorNpc npc) {
		CompoundTag wish = open(npc);
		if (wish == null || axisOf(wish) != FourAxis.Axis.ENLIGHTENMENT || refusal(soul, wish) != null) return false;
		// The record the soul cultivates (Possession's: carried into another world, the NPC is made anew from its save, and its own
		// copy of the record stands still until it is given back), against the rank the NPC had when it was taken.
		PlayerCultivation body = Possession.borrowed(soul);
		if (body == null || body.isMortal() || PlayerCultivation.rank(body.getRealm(), body.getStage()) <= npc.getRank()) return false;
		done(npc, wish);
		ServerLevel level = soul.serverLevel();
		level.sendParticles(ParticleTypes.END_ROD, soul.getX(), soul.getY() + 1, soul.getZ(), 40, 0.5, 0.8, 0.5, 0.06);
		level.playSound(null, soul.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.8f, 1.2f);
		soul.sendSystemMessage(Component.translatable(ModLang.WISH_GUIDED_WITHIN, Component.literal(npc.getPersonalName()),
				PlayerCultivation.rankName(body.getRealm(), body.getStage())).withStyle(ChatFormatting.GOLD));
		FourAxis.add(soul, FourAxis.Axis.ENLIGHTENMENT, 1);
		return true;
	}

	// --- Granted ---

	/**
	 * The wish is granted: done (a new one in its time). What it fills the granter's axis with is its own to give: the years taken,
	 * the stones' worth as they are absorbed, each threat's hearts as it falls, the stage guided.
	 */
	private static void done(CultivatorNpc npc, CompoundTag wish) {
		wish.putLong("Done", npc.level().getGameTime());
		clearTaken(wish);
		if (axisOf(wish) == FourAxis.Axis.MERIT) Settlements.endAlarm(npc); // its threat slain: its people come out again
	}

	public static void forget(UUID player) {
		GUIDING.remove(player);
	}

	public static void clear() {
		GUIDING.clear();
		PENDING.clear();
	}

	private Wishes() {}
}
