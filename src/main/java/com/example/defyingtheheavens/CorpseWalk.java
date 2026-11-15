package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Walking in the dead (the user's ask, 2026-11-13: "can you posses dead bodies? If so you should be able to, and they become a walking skeleton
 * as they rot and you roam with them"; and their picks of the questions put to them). A body of the dead (CorpseBlock) is possessed as a
 * person's is (Possession, the corpse's kind), by a projected soul (SoulProjection#tryPossessCorpse: Nascent Soul and up, the living's or the
 * dead's; tethered to its own body as in a living one, pulled out past the tether's end: the user's word) or by one of the dead as it is
 * (GhostPossession, the dead's own ability: a ghost needs no soul sent out, it is one, and nothing tethers it: the user's word). Any body of a player or a
 * cultivator, fresh, rotting or bare bones, the ancient dead of the secret realms too; at least a realm below the walker's own highest path,
 * as possession weighs a body ({@link #refusal}); one's own whatever its realm, one's ghost walking in it, dead still.
 * <ul>
 *   <li><b>What it is</b>: what the dead was ({@link #recordOf}: their record as they died), at the whole strength of their realm whatever
 *   its rot; their spells and techniques, wielded on the qi they died with, that the yin about it refills ({@link #feed}: the ghostly qi's
 *   density against a Soul Sand Valley's, no more than twice). It flies by no means (the dead walk: QiFlight), eats nothing, mends of
 *   itself only by the yin about it ({@link #MEND_SHARE} of a living body's pace at most), and is undead to the world (Smite, a healing
 *   potion's harm, a harming one's healing, no Regeneration or poison: CorpseUndeadMixin). The sun is nothing to it.</li>
 *   <li><b>Its rot</b> goes on in its walker's keeping, the slower the higher the walker stands ({@link #rotSlowed}): fresh, then rotting,
 *   then a walking skeleton in its shape, its armour on it, for as long as it lasts. What it holds is the walker's to use.</li>
 *   <li><b>Leaving it</b> (Soul Projection, qi run dry, the tether, logging out, its health spent) lays it down again where it stands,
 *   as rotten as it came to be, holding what it carries ({@link #layBack}).</li>
 *   <li><b>The world</b>: righteous cultivators who see it fall on it, mortals run, the devilish let it be ({@link #people}); its own
 *   sect, seeing its dead walk, turns on its walker for good. It gives off its yin as it walks, as it did lying (Corpses#yinAt). Taking a
 *   body draws the walker toward the devilish path as bleeding one does (Corpses#BLEED_SHIFT).</li>
 *   <li><b>Meditating in it</b> refines Corpse Yin Refining for the walker's own ghost path ({@link #meditate}), sent home as it leaves.</li>
 * </ul>
 */
public final class CorpseWalk {
	/** Its rot goes this much slower for each realm of its walker's highest path: a Qi Refining walker's as lying, a Nascent Soul's 2.5 times as slow. */
	public static final double ROT_SLOWED_PER_REALM = 0.5;
	/** It mends of itself this share of a living body's pace (CultivationStats#mendPerSecond) where the yin is a Soul Sand Valley's or thicker. */
	public static final double MEND_SHARE = 0.5;
	/** Its qi gathers at its realm's pace times the yin's density against a Soul Sand Valley's, no more than this. */
	public static final double YIN_GATHER_CAP = 2.0;
	/** Righteous cultivators within this who see it fall on it; mortals within {@link #MORTALS_FLEE} run. */
	public static final double NPC_SEES = 24, MORTALS_FLEE = 16;
	/** Meditating in a body refines its whole yin's share (CorpseYin#share) every this many seconds. */
	public static final double MEDITATE_SECONDS = 120;
	/** Words of what meditation owes, at most this often (ticks). */
	private static final int OWED_TOLD_EVERY = 100;

	/** The ghostly qi's density about each walker, as last measured (each second): its qi and its mending go by it. */
	private static final Map<UUID, Double> DENSITY = new HashMap<>();
	/** Walkers told already that meditating in the dead refines nothing for them. */
	private static final Set<UUID> TOLD_NO_YIN = new HashSet<>();
	/** Client hook (set by the client): whether a player walks in a body of the dead, as this client is shown it. */
	public static Predicate<Player> clientWalking = player -> false;

	// --- What the dead was ---

	/** What of a record belongs to the living's own affairs, not to what they were: left out of a body's keeping. */
	private static final String[] TRANSIENT = {"Possession", "Clone", "CloneReadyAt", "InnerReturn", "SoulReturn", "SoulWounds", "Journal",
			"SpatialTrial", "GapFromUpper", "InTribulation", "LifespanTrial", "LifeEnded", "FortuneOwed", "FortuneOwedDay", "HeartDemonAt",
			"FallProtected"};

	/** {@code c} as a body keeps it (CorpseBlockEntity#record): its record's tag, the living's own affairs left out. */
	static CompoundTag snapshot(PlayerCultivation c) {
		CompoundTag tag = c.save();
		for (String key : TRANSIENT) tag.remove(key);
		return tag;
	}

	/** A cultivator's, as a soul possessing it would have it (Possession#enter): its record, its path, its own method, a body cultivator's body. */
	static CompoundTag snapshot(CultivatorNpc npc) {
		PlayerCultivation copy = PlayerCultivation.load(npc.cultivation().save());
		copy.setAlignment(npc.getAlignment());
		Possession.knowOwnMethod(npc, copy);
		if (npc.isBodyCultivator()) BodyCultivators.asBody(npc, copy);
		return snapshot(copy);
	}

	/**
	 * The record a soul walking in {@code corpse} plays: what the dead was as they died, or, for the ancient dead and bodies laid before
	 * the dead were kept so, a cultivator of its rank with no arts of its own. Of the living's rules always (a dead player's own record
	 * went on as a ghost's: this one is the body's).
	 */
	static PlayerCultivation recordOf(CorpseBlockEntity corpse) {
		PlayerCultivation c;
		if (corpse.record() != null) {
			c = PlayerCultivation.load(corpse.record().copy());
		} else {
			c = new PlayerCultivation();
			int rank = corpse.rank();
			if (rank < 0) {
				c.setMortal(true);
			} else {
				int stages = Stage.values().length;
				c.awaken(0);
				c.setState(Realm.byIndex(Math.min(rank / stages, Realm.values().length - 1)), Stage.byIndex(rank % stages), 0);
			}
		}
		c.netherworld().setGhost(false);
		return c;
	}

	// --- Rising in it ---

	/**
	 * Why {@code walker} can't rise in {@code corpse} (a lang key), or null: at least {@link SoulProjection#POSSESS_REALM_GAP} realms below
	 * the highest of the walker's own paths (the dead's realm as it died; a mortal's below Qi Refining), as possession weighs a body. One's own
	 * body whatever its realm (the user's pick: one's ghost walks in it, dead still).
	 */
	static String refusal(ServerPlayer walker, CorpseBlockEntity corpse) {
		if (Possession.inhabits(walker.getUUID())) return ModLang.MSG_POSSESS_CANNOT;
		if (corpse.dead() != null && corpse.dead().equals(walker.getUUID())) return null;
		int stages = Stage.values().length;
		int own = Math.max(0, TwoPaths.higherRank(CultivationManager.own(walker), true)) / stages;
		int dead = corpse.rank() < 0 ? -1 : corpse.rank() / stages;
		return dead > own - SoulProjection.POSSESS_REALM_GAP ? ModLang.MSG_POSSESS_TOO_STRONG : null;
	}

	/** A soul has risen in a body (either way): drawn toward the devilish path as bleeding the dead draws one (Corpses#BLEED_SHIFT); heard. */
	static void risen(ServerPlayer walker) {
		PlayerCultivation own = CultivationManager.own(walker);
		own.setAlignment(own.getAlignment() - Corpses.BLEED_SHIFT);
		CultivationManager.markDirty(walker.server);
		CultivationManager.sync(walker);
		TOLD_NO_YIN.remove(walker.getUUID());
		ServerLevel level = walker.serverLevel();
		level.playSound(null, walker.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.6f, 0.5f);
		level.sendParticles(ParticleTypes.SOUL, walker.getX(), walker.getY() + 1.0, walker.getZ(), 24, 0.3, 0.6, 0.3, 0.02);
	}

	/** A walker out of its body (GhostPossession#leave), or logging out: what was noted of it forgotten (the body lain down already: GhostPossession, SoulProjection). */
	public static void forget(ServerPlayer player) {
		DENSITY.remove(player.getUUID());
		TOLD_NO_YIN.remove(player.getUUID());
	}

	/** A walker logging out: forgotten ({@link #forget}). */
	public static void onDisconnect(ServerPlayer player) {
		forget(player);
	}

	/** Server stop: nothing carries over. */
	public static void clear() {
		DENSITY.clear();
		TOLD_NO_YIN.clear();
	}

	// --- In it ---

	/** Whether {@code player} walks in a body of the dead now (server). */
	public static boolean walking(ServerPlayer player) {
		return Possession.inCorpse(player.getUUID());
	}

	/** {@link #walking}, both sides (the client's as it is shown: ClientBodies). */
	public static boolean walking(Player player) {
		if (player == null) return false;
		if (player.level().isClientSide) return clientWalking.test(player);
		return player instanceof ServerPlayer sp && walking(sp);
	}

	/** How far gone a body dead {@code age} ticks is (CorpseBlock#tick's own measure). */
	public static CorpseBlock.Rot rot(double age) {
		return age < Corpses.FRESH_TICKS ? CorpseBlock.Rot.FRESH : age < Corpses.BONES_AT ? CorpseBlock.Rot.ROTTING : CorpseBlock.Rot.BONES;
	}

	/**
	 * How many times as slowly a body rots in the keeping of a walker whose highest path stands in {@code realm} (the user's pick: the higher
	 * the walker, the slower): a Qi Refining walker's as it would lying (bones in five minutes), a Nascent Soul's 2.5 times as slowly, a Heavenly
	 * Being's 3, a True Immortal's 6.
	 */
	public static double rotSlowed(int realm) {
		return 1 + ROT_SLOWED_PER_REALM * Math.max(0, realm);
	}

	/** The realm of {@code own}'s highest path, as possession weighs a soul. */
	private static int realmOf(PlayerCultivation own) {
		return Math.max(0, TwoPaths.higherRank(own, false)) / Stage.values().length;
	}

	/** Each server tick: every walker's body rots on, hungers for nothing; each second, the yin about it, its mending and the world's people. */
	public static void tick(MinecraftServer server) {
		List<ServerPlayer> walkers = Possession.corpseWalkers();
		if (walkers.isEmpty()) return;
		int tick = server.getTickCount();
		for (ServerPlayer p : walkers) {
			if (!p.isAlive()) continue;
			try {
				tickWalker(p, tick);
			} catch (Exception e) {
				DefyingTheHeavens.LOGGER.error("Corpse walk tick failed for {}", p.getScoreboardName(), e);
			}
		}
	}

	private static void tickWalker(ServerPlayer p, int tick) {
		PlayerCultivation own = CultivationManager.own(p);
		double age = Possession.corpseAge(p);
		CorpseBlock.Rot was = rot(age);
		age += 1 / rotSlowed(realmOf(own));
		Possession.setCorpseAge(p, age);
		CorpseBlock.Rot now = rot(age);
		CompoundTag corpse = Possession.corpseOf(p);
		if (now != was) {
			Possession.lookNow(p); // everyone shown the body as it is now
			String name = corpse == null ? "" : corpse.getString("Name");
			p.displayClientMessage(Component.translatable(now == CorpseBlock.Rot.BONES ? ModLang.MSG_CORPSE_BARE : ModLang.MSG_CORPSE_ROTS, name)
					.withStyle(ChatFormatting.DARK_GREEN), true);
		}
		// The dead hunger for nothing, and starve of nothing: a full belly that mends nothing (FoodDataMixin).
		FoodData food = p.getFoodData();
		food.setFoodLevel(20);
		food.setSaturation(0);
		food.setExhaustion(0);
		if (now == CorpseBlock.Rot.ROTTING && tick % 10 == 0) { // flies about it, as about a body lying
			p.serverLevel().sendParticles(ParticleTypes.MYCELIUM, p.getX(), p.getY() + 1.2, p.getZ(), 2, 0.5, 0.6, 0.5, 0.0);
		}
		if (tick % 20 == 0) second(p, own);
	}

	/** A walker's second: the yin about it (its qi and mending go by it), its mending, the world's people (a ghost's upkeep: GhostPossession). */
	static void second(ServerPlayer p, PlayerCultivation own) {
		ServerLevel level = p.serverLevel();
		double density = densityAt(level, p.blockPosition(), p.getUUID());
		DENSITY.put(p.getUUID(), density);
		double share = Math.min(1, density / GhostQi.SOUL_SAND_VALLEY);
		if (p.getHealth() < p.getMaxHealth() && share > 0) p.heal((float) (CultivationStats.mendPerSecond(p.getMaxHealth()) * MEND_SHARE * share));
		if (p.isAlive() && walking(p)) people(p, level);
	}

	/** The ghostly qi's density at {@code pos} (GhostQi#densityAt), the body {@code self} walks in left out (its own yin feeds it nothing). */
	static double densityAt(ServerLevel level, BlockPos pos, UUID self) {
		double place = GhostQi.placeDensity(level, pos), yin = Corpses.yinAt(level, pos, self);
		return yin <= 0 ? place : Math.max(place, Math.min(GhostQi.NETHERWORLD, place + yin));
	}

	/**
	 * A walker's qi this tick (QiManager, in place of the pool's own gathering): its realm's pace times the yin about it against a Soul Sand
	 * Valley's ({@link #YIN_GATHER_CAP} at most), less what it spends. @return whether the pool changed
	 */
	public static boolean feed(ServerPlayer p, PlayerCultivation c, double spendPerSecond) {
		double place = Math.min(YIN_GATHER_CAP, DENSITY.getOrDefault(p.getUUID(), GhostQi.OVERWORLD) / GhostQi.SOUL_SAND_VALLEY);
		double next = Math.max(0, Math.min(c.maxQi(), c.getQi() + (c.qiGatherAt(place) - spendPerSecond) / 20.0));
		if (next == c.getQi()) return false;
		c.setQi(next);
		return true;
	}

	/**
	 * The world's people about a walker, each second: its own sect's, seeing their dead walk, turn on its walker for good (their sect's standing
	 * with the walker's own self: the user's pick); righteous cultivators who see it fall on it (their hatred the body's: Possession#identity);
	 * mortals run from it; the devilish and the rest let it be.
	 */
	private static void people(ServerPlayer p, ServerLevel level) {
		CompoundTag corpse = Possession.corpseOf(p);
		String sectName = corpse == null ? "" : corpse.getString("Sect");
		double reach = Math.max(NPC_SEES, CorpseYin.SECT_SEES);
		for (CultivatorNpc npc : level.getEntitiesOfClass(CultivatorNpc.class, p.getBoundingBox().inflate(reach),
				n -> n.isAlive() && !n.isVacant() && !n.isGhost())) {
			double d = npc.distanceTo(p);
			if (!npc.hasLineOfSight(p)) continue;
			Sect sect = npc.isSectMember() ? npc.sect() : null;
			if (!sectName.isEmpty() && sect != null && sectName.equals(sect.name)) {
				if (d <= CorpseYin.SECT_SEES) turnSect(p, sect, npc);
				continue;
			}
			if (d > NPC_SEES) continue;
			if (npc.isMortal()) {
				if (d <= MORTALS_FLEE) npc.startFleeing(p);
				continue;
			}
			if (npc.getFaction() == Alignment.Faction.RIGHTEOUS && npc.getTarget() == null) {
				Regard.Entry entry = npc.regardEntry(p);
				entry.value = Math.min(entry.value, Regard.HOSTILE);
				npc.setTarget(p);
			}
		}
	}

	/** {@code sect}, its {@code npc} seeing its dead walk: its standing with the walker's own self hostile for good, and the one who saw it at them. */
	private static void turnSect(ServerPlayer p, Sect sect, CultivatorNpc npc) {
		UUID own = p.getUUID();
		if (sect.standing(own) > Regard.HOSTILE) {
			sect.changeStanding(own, Regard.HOSTILE - sect.standing(own));
			SectData.get(p.server).setDirty();
		}
		Regard.Entry entry = npc.regardEntry(p);
		entry.value = Math.min(entry.value, Regard.HOSTILE);
		if (npc.getTarget() == null) npc.setTarget(p);
		if (!Possession.sectTurned(p)) {
			p.sendSystemMessage(Component.translatable(ModLang.MSG_CORPSE_SECT_TURNS, sect.name).withStyle(ChatFormatting.RED));
		}
	}

	// --- Meditating in it ---

	/**
	 * {@code seconds} of meditation in a body (MeditationManager, in place of its own: the user's pick, "you have to meditate in the corpse and
	 * it sends the cultivation to your main body"): Corpse Yin Refining, the walker's own ghost path's, refines the body's yin, its whole share
	 * of the path's stage (CorpseYin#share, by its rot) every {@link #MEDITATE_SECONDS}; owed, and paid into the walker's own record as it leaves
	 * the body (Possession#leave). Nothing for a walker whose ghost path doesn't practise it (told once).
	 */
	public static void meditate(ServerPlayer p, double seconds) {
		PlayerCultivation own = CultivationManager.own(p);
		NetherworldState s = own.netherworld();
		if (GhostMethods.ghostRank(s) < 0 || CultivationMethods.practised(own, TwoPaths.Path.GHOST) != CultivationMethod.CORPSE_YIN) {
			if (TOLD_NO_YIN.add(p.getUUID())) p.displayClientMessage(Component.translatable(ModLang.MSG_CORPSE_YIN_NONE).withStyle(ChatFormatting.GRAY), true);
			return;
		}
		CompoundTag corpse = Possession.corpseOf(p);
		Realm realm = GhostMethods.ghostRealm(s);
		int deadRank = corpse == null || !corpse.contains("Rank") ? -1 : corpse.getInt("Rank");
		double share = CorpseYin.share(deadRank, realm == null ? 0 : realm.ordinal(), rot(Possession.corpseAge(p))) * seconds / MEDITATE_SECONDS;
		Possession.oweCorpseYin(p, share);
		ServerLevel level = p.serverLevel();
		level.sendParticles(ParticleTypes.SOUL, p.getX(), p.getY() + 1.0, p.getZ(), 1, 0.4, 0.5, 0.4, 0.01);
		if (p.tickCount % OWED_TOLD_EVERY < 10) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_CORPSE_YIN_OWED,
					String.format(java.util.Locale.ROOT, "%.1f", Possession.corpseYin(p) * 100)).withStyle(ChatFormatting.DARK_AQUA), true);
		}
	}

	/** Corpse Yin Refining owed to {@code walker} for meditating in the dead: into its own record (the body given back already). */
	static void pay(ServerPlayer walker, double share) {
		if (share > 0) CultivationMethods.grant(walker, TwoPaths.Path.GHOST, CultivationMethod.CORPSE_YIN, share);
	}

	// --- Its yin as it walks (Corpses#yinAt) ---

	/** The yin the bodies walking in {@code level} give off at {@code pos}, as they did lying (Corpses#yin, #yinRadius); {@code except}'s left out. */
	static double yinAt(ServerLevel level, BlockPos pos, UUID except) {
		double yin = 0;
		for (ServerPlayer walker : Possession.corpseWalkers()) {
			if (walker.level() != level || walker.getUUID().equals(except)) continue;
			CompoundTag corpse = Possession.corpseOf(walker);
			int rank = corpse == null || !corpse.contains("Rank") ? -1 : corpse.getInt("Rank");
			int radius = Corpses.yinRadius(rank);
			if (walker.blockPosition().distSqr(pos) > (double) radius * radius) continue;
			yin += Corpses.yin(rank, rot(Possession.corpseAge(walker)) == CorpseBlock.Rot.BONES);
		}
		return yin;
	}

	// --- Its things, going in and coming out ---

	/**
	 * What a body holds as a soul rising in it carries it (Possession#giveTo): what it wore on it again (armour, in its slot), the rest in its
	 * pack, what won't fit at its feet; its health whole, fed (it hungers for nothing), nothing on it.
	 */
	static CompoundTag bodyOf(List<ItemStack> items, ServerPlayer soul) {
		ListTag list = new ListTag();
		boolean[] used = new boolean[256];
		int next = 0;
		for (ItemStack stack : items) {
			if (stack.isEmpty()) continue;
			int slot = -1;
			Equipable worn = Equipable.get(stack);
			if (worn != null && worn.getEquipmentSlot().getType() == EquipmentSlot.Type.ARMOR) {
				int armour = 100 + worn.getEquipmentSlot().getIndex();
				if (!used[armour]) slot = armour;
			}
			if (slot < 0) {
				while (next < 36 && used[next]) next++;
				if (next < 36) slot = next++;
			}
			if (slot < 0) {
				soul.drop(stack, true, false);
				continue;
			}
			used[slot] = true;
			CompoundTag one = stack.save(new CompoundTag());
			one.putByte("Slot", (byte) slot);
			list.add(one);
		}
		CompoundTag tag = new CompoundTag();
		tag.put("Inventory", list);
		tag.putInt("Selected", 0);
		tag.putFloat("Health", 1);
		tag.putFloat("MaxHealth", 1);
		tag.putBoolean("Scaled", true); // its whole health, whatever its realm's measure
		CompoundTag food = new CompoundTag();
		new FoodData().addAdditionalSaveData(food);
		tag.put("Food", food);
		tag.put("Effects", new ListTag());
		tag.putInt("Air", soul.getMaxAirSupply());
		return tag;
	}

	/** Everything a body carried out of a soul's keeping (Possession#takeFrom's tag): its pack, its armour, its rings. */
	private static List<ItemStack> itemsOf(CompoundTag body, ServerPlayer at) {
		List<ItemStack> items = new ArrayList<>();
		Inventory loaded = new Inventory(at);
		loaded.load(body.getList("Inventory", Tag.TAG_COMPOUND));
		for (int i = 0; i < loaded.getContainerSize(); i++) {
			ItemStack stack = loaded.getItem(i);
			if (!stack.isEmpty()) items.add(stack);
		}
		for (Tag entry : body.getList("Rings", Tag.TAG_COMPOUND)) {
			ItemStack stack = ItemStack.of((CompoundTag) entry);
			if (!stack.isEmpty()) items.add(stack);
		}
		return items;
	}

	/**
	 * The body a soul walked in (its block entity's tag as it rose, {@code saved}; dead {@code age} ticks now) lies down again where {@code at}
	 * stands (or on the ground under it: Corpses#placeFor), as it faces, holding what it carried ({@code body}: Possession#takeFrom's tag), its
	 * rot going on from there. With nowhere to lie (the void, lava), what it carried falls where it is, and the body is gone.
	 */
	static void layBack(ServerPlayer at, CompoundTag saved, double age, CompoundTag body) {
		ServerLevel level = at.serverLevel();
		List<ItemStack> items = itemsOf(body, at);
		BlockPos pos = Corpses.placeFor(level, at.blockPosition());
		CorpseBlock.Rot rot = rot(age);
		if (pos != null) {
			boolean water = level.getFluidState(pos).is(Fluids.WATER) && level.getFluidState(pos).isSource();
			level.setBlock(pos, ModBlocks.CORPSE.defaultBlockState().setValue(CorpseBlock.WATERLOGGED, water).setValue(CorpseBlock.STAGE, rot), Block.UPDATE_ALL);
			if (level.getBlockEntity(pos) instanceof CorpseBlockEntity corpse) {
				corpse.restore(saved, Math.round(age), level.getGameTime(), at.getYRot(), items);
				if (rot != CorpseBlock.Rot.BONES) {
					long next = (rot == CorpseBlock.Rot.FRESH ? Corpses.FRESH_TICKS : Corpses.BONES_AT) - Math.round(age);
					level.scheduleTick(pos, ModBlocks.CORPSE, (int) Math.max(1, next));
				}
				return;
			}
			level.removeBlock(pos, false);
		}
		for (ItemStack stack : items) Containers.dropItemStack(level, at.getX(), at.getY() + 0.3, at.getZ(), stack);
	}

	/**
	 * Joining with a corpse walked in still on record (a crash: a logout or a stop lays it down first): it lies down where they stood, holding
	 * what they carried in it ({@code body}), and the Corpse Yin Refining they were owed is paid (Possession#onJoin).
	 */
	static void crashed(ServerPlayer player, CompoundTag saved, double age, CompoundTag body, double owed) {
		layBack(player, saved, age, body);
		pay(player, owed);
	}

	private CorpseWalk() {}
}
