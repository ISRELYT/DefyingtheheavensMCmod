package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Natal treasures (the user's "Build as designed", 2026-11-13). From Core Formation (the higher of the heaven path and the Demon Path:
 * TwoPaths#higherRank), one artifact blade one forged oneself (the forge stamps its forger on every blade since: {@link #FORGER}) is bound to
 * one's core at the forge ({@link #atForge}: sneaking, the blade in hand), one at a time (PlayerCultivation#natalTreasure, the id written on
 * it: {@link #ID}). Bound:
 * <ul>
 *   <li>in its owner's hand its might grows with their realm: {@link #PER_REALM} of all their blows for each realm from Core Formation (its
 *   owner's rank kept on it while they hold it, {@link #RANK}: ArtifactWeaponItem#getAttributeModifiers);</li>
 *   <li>at the forge with the next grade's materials of its kind it rises a grade in place (ArtifactRecipes#refining, ArtifactForgeBlockEntity:
 *   only its owner works it), its work and its bond kept ({@link #raised});</li>
 *   <li>Natal Recall (Ability#NATAL_RECALL, {@link #recall}) calls it to its owner's hand from anywhere in the world they are in: off the
 *   ground, out of anyone's pack, from a chest in loaded land;</li>
 *   <li>broken, worn out ({@link #broken}), it is gone and its owner's soul wounded ({@link #wound}: {@link #WOUND_TICKS}, the qi pool
 *   {@link #WOUNDED_POOL} of itself and their blows the weaker, ModEffects#SOUL_WOUND); unbinding it at the forge wounds them the same, as does
 *   binding another while one is bound (the old bond severed). Lying on the ground it waits for them (NatalItemEntityMixin).</li>
 * </ul>
 */
public final class NatalTreasures {
	/** On a forged blade: who forged it (UUID, name). */
	public static final String FORGER = "ArtifactForger", FORGER_NAME = "ArtifactForgerName";
	/** On a natal treasure: its id, its owner (UUID, name), and its owner's rank while they hold it (removed in another's hand). */
	public static final String ID = "NatalId", OWNER = "NatalOwner", OWNER_NAME = "NatalOwnerName", RANK = "NatalRank";
	/** Its might for each realm its owner stands at from Core Formation (Core Formation the first): a share of all their blows. */
	public static final double PER_REALM = 0.1;
	/** A wounded soul's time: twenty minutes. */
	public static final int WOUND_TICKS = 20 * 60 * 20;
	/** What of its qi pool a wounded soul keeps. */
	public static final double WOUNDED_POOL = 0.5;
	/** Natal Recall's price (a share of the qi pool) and its wait (ticks). */
	public static final double RECALL_QI = 0.1;
	public static final int RECALL_WAIT = 200;
	/** A severing asked for at the forge (an unbinding, another bound instead) goes ahead if asked again within this long. */
	public static final int ASK_AGAIN = 100;

	private record Asked(String what, @Nullable UUID id, long until) {}

	private static final Map<UUID, Asked> ASKED = new HashMap<>();
	private static final Map<UUID, Long> RECALLED = new HashMap<>();

	// --- What a blade is ---

	/** {@code stack}, forged now, remembers who forged it. */
	public static void stampForger(ItemStack stack, ServerPlayer forger) {
		CompoundTag tag = stack.getOrCreateTag();
		tag.putUUID(FORGER, forger.getUUID());
		tag.putString(FORGER_NAME, forger.getGameProfile().getName());
	}

	/** Who forged {@code stack} (blades forged since 2026-11-13), or null. */
	public static @Nullable UUID forgerOf(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.hasUUID(FORGER) ? tag.getUUID(FORGER) : null;
	}

	/** The natal treasure's id written on {@code stack}, or null. */
	public static @Nullable UUID idOf(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.hasUUID(ID) ? tag.getUUID(ID) : null;
	}

	/** Whose natal treasure {@code stack} is, or null. */
	public static @Nullable UUID ownerOf(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.hasUUID(OWNER) ? tag.getUUID(OWNER) : null;
	}

	/** Whether {@code stack} is a natal treasure (bound to someone's core). */
	public static boolean isNatal(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem() instanceof ArtifactWeaponItem && idOf(stack) != null;
	}

	/** Its might in its owner's hand: {@link #PER_REALM} a realm from Core Formation, by their rank kept on it ({@link #RANK}); 0 in anyone else's. */
	public static double might(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		if (tag == null || !tag.contains(RANK) || !tag.hasUUID(ID)) return 0;
		return mightAt(tag.getInt(RANK));
	}

	/** The might of a natal treasure in the hand of an owner of {@code rank}. */
	public static double mightAt(int rank) {
		int realms = CultivatorNpc.realmOf(Math.max(0, rank)).ordinal() - Realm.CORE_FORMATION.ordinal() + 1;
		return realms <= 0 ? 0 : PER_REALM * realms;
	}

	/** Whether {@code c} may have a natal treasure: Core Formation or above on the higher of its two paths. */
	static boolean reaches(PlayerCultivation c) {
		return TwoPaths.higherRank(c, false) >= PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY);
	}

	/**
	 * Each second {@code stack} sits in {@code holder}'s pack (ArtifactWeaponItem#inventoryTick): its owner's rank kept on it while they hold
	 * it (for its might), none in another's; and a blade whose bond is no more (unbound, another bound instead while it was lost) a blade again.
	 */
	static void held(ItemStack stack, ServerPlayer holder) {
		UUID id = idOf(stack);
		if (id == null) return;
		CompoundTag tag = stack.getOrCreateTag();
		if (!holder.getUUID().equals(ownerOf(stack))) {
			if (tag.contains(RANK)) tag.remove(RANK);
			return;
		}
		PlayerCultivation own = CultivationManager.own(holder);
		if (!id.equals(own.natalTreasure())) {
			loosen(stack);
			return;
		}
		int rank = TwoPaths.higherRank(own, false);
		if (!tag.contains(RANK) || tag.getInt(RANK) != rank) tag.putInt(RANK, rank);
	}

	/** {@code stack}'s bond undone: a blade again, its forger still remembered. */
	static void loosen(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		if (tag == null) return;
		for (String key : new String[] {ID, OWNER, OWNER_NAME, RANK}) tag.remove(key);
	}

	// --- At the forge ---

	/**
	 * {@code player}, sneaking, uses the forge with {@code held}, an artifact blade (ArtifactForgeBlockEntity#interact): bound as their natal
	 * treasure; theirs already, unbound (asked twice: a soul wound); another bound already, this one bound instead, that bond severed (asked
	 * twice, the same wound).
	 */
	static InteractionResult atForge(ServerPlayer player, ItemStack held) {
		PlayerCultivation own = CultivationManager.own(player);
		UUID bound = own.natalTreasure(), id = idOf(held);
		if (id != null && id.equals(bound) && player.getUUID().equals(ownerOf(held))) {
			if (!askedAgain(player, "unbind", id)) {
				tell(player, Component.translatable(ModLang.MSG_NATAL_UNBIND_ASK, held.getHoverName()), ChatFormatting.GOLD);
				return InteractionResult.CONSUME;
			}
			loosen(held);
			own.setNatalTreasure(null);
			wound(player);
			tell(player, Component.translatable(ModLang.MSG_NATAL_UNBOUND, held.getHoverName()), ChatFormatting.RED);
			CultivationManager.markDirty(player.server);
			CultivationManager.sync(player);
			return InteractionResult.SUCCESS;
		}
		Component why = refusal(player, own, held);
		if (why != null) {
			tell(player, why, ChatFormatting.GRAY);
			return InteractionResult.CONSUME;
		}
		if (bound != null) {
			if (!askedAgain(player, "sever", null)) {
				tell(player, Component.translatable(ModLang.MSG_NATAL_SEVER_ASK, held.getHoverName()), ChatFormatting.GOLD);
				return InteractionResult.CONSUME;
			}
			wound(player);
		}
		bind(player, own, held);
		return InteractionResult.SUCCESS;
	}

	/** Why {@code player} can't bind {@code held} to their core, or null. */
	static @Nullable Component refusal(ServerPlayer player, PlayerCultivation own, ItemStack held) {
		if (!reaches(own)) return Component.translatable(ModLang.MSG_NATAL_TOO_LOW);
		UUID owner = ownerOf(held);
		if (owner != null && !owner.equals(player.getUUID())) return Component.translatable(ModLang.MSG_NATAL_OTHERS, held.getHoverName());
		if (!player.getUUID().equals(forgerOf(held))) return Component.translatable(ModLang.MSG_NATAL_NOT_FORGED);
		return null;
	}

	/** {@code held} bound to {@code player}'s core: a new id written on it and on their record. */
	static void bind(ServerPlayer player, PlayerCultivation own, ItemStack held) {
		UUID id = UUID.randomUUID();
		CompoundTag tag = held.getOrCreateTag();
		tag.putUUID(ID, id);
		tag.putUUID(OWNER, player.getUUID());
		tag.putString(OWNER_NAME, player.getGameProfile().getName());
		tag.putInt(RANK, TwoPaths.higherRank(own, false));
		own.setNatalTreasure(id);
		CultivationManager.markDirty(player.server);
		CultivationManager.sync(player);
		ServerLevel level = player.serverLevel();
		Vec3 at = player.position().add(0, 1.0, 0);
		level.sendParticles(ParticleTypes.ENCHANT, at.x, at.y, at.z, 60, 0.6, 0.8, 0.6, 0.6);
		level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 20, 0.4, 0.6, 0.4, 0.05);
		level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.4f);
		tell(player, Component.translatable(ModLang.MSG_NATAL_BOUND, held.getHoverName()), ChatFormatting.GOLD);
	}

	/** Whether {@code player} asked for {@code what} a moment ago, and so goes ahead; else this is the asking. */
	private static boolean askedAgain(ServerPlayer player, String what, @Nullable UUID id) {
		long now = player.level().getGameTime();
		Asked asked = ASKED.get(player.getUUID());
		if (asked != null && asked.what().equals(what) && Objects.equals(asked.id(), id) && now <= asked.until()) {
			ASKED.remove(player.getUUID());
			return true;
		}
		ASKED.put(player.getUUID(), new Asked(what, id, now + ASK_AGAIN));
		return false;
	}

	/** The blade a forge's {@code contents} raise a grade (a natal treasure among them: ArtifactRecipes#refining), or null. */
	static @Nullable ItemStack bladeIn(java.util.List<ItemStack> contents) {
		for (ItemStack stack : contents) if (isNatal(stack)) return stack;
		return null;
	}

	/** {@code blade} risen to {@code recipe}'s grade: its next grade's item, its work, its forger and its bond kept, its wear made good. */
	static ItemStack raised(ItemStack blade, ArtifactRecipes.Recipe recipe) {
		ItemStack up = new ItemStack(recipe.output());
		CompoundTag tag = blade.getTag() == null ? new CompoundTag() : blade.getTag().copy();
		tag.remove("Damage");
		up.setTag(tag);
		return up;
	}

	// --- Natal Recall ---

	/**
	 * Natal Recall (Ability#NATAL_RECALL): {@code player}'s natal treasure to their hand, wherever it is in the world they are in (#find): for
	 * {@link #RECALL_QI} of their qi pool, once in {@link #RECALL_WAIT}. Not found (another world, land not loaded, gone), nothing spent.
	 */
	public static void recall(ServerPlayer player) {
		PlayerCultivation own = CultivationManager.own(player);
		UUID id = own.natalTreasure();
		if (id == null) return;
		if (id.equals(idOf(player.getMainHandItem()))) {
			tell(player, Component.translatable(ModLang.MSG_NATAL_IN_HAND), ChatFormatting.GRAY);
			return;
		}
		long now = player.level().getGameTime();
		Long ready = RECALLED.get(player.getUUID());
		if (ready != null && now < ready) {
			tell(player, Component.translatable(ModLang.MSG_NATAL_RECALL_WAIT, (ready - now + 19) / 20), ChatFormatting.GRAY);
			return;
		}
		PlayerCultivation c = CultivationManager.get(player);
		double cost = c.maxQi() * RECALL_QI;
		boolean free = player.isCreative();
		if (!free && c.getQi() < cost) {
			tell(player, Component.translatable(ModLang.MSG_NATAL_RECALL_QI, Math.round(cost)), ChatFormatting.GRAY);
			return;
		}
		ServerLevel level = player.serverLevel();
		Found found = find(level, id, player);
		if (found == null) {
			tell(player, Component.translatable(ModLang.MSG_NATAL_NOT_FOUND), ChatFormatting.GRAY);
			return;
		}
		ItemStack stack = found.take();
		if (!free) c.setQi(c.getQi() - cost);
		RECALLED.put(player.getUUID(), now + RECALL_WAIT);
		trail(level, found.at(), player);
		toHand(player, stack);
		tell(player, Component.translatable(ModLang.MSG_NATAL_RECALLED, stack.getHoverName()), ChatFormatting.GOLD);
		CultivationManager.sync(player);
	}

	/** Where a natal treasure was found, and how to take it from there. */
	record Found(Vec3 at, java.util.function.Supplier<ItemStack> taking) {
		ItemStack take() { return taking.get(); }
	}

	/**
	 * Where in {@code level} the natal treasure {@code id} is: its owner's own pack first, then lying on the ground, in anyone's pack, in a
	 * beast's or anyone's hands, in an item frame or a cart's hold, in a chest (any block's container) in loaded land. Null if nowhere.
	 */
	static @Nullable Found find(ServerLevel level, UUID id, ServerPlayer owner) {
		Found found = inPack(owner, id);
		if (found != null) return found;
		for (ItemEntity item : level.getEntities(EntityType.ITEM, e -> e.isAlive() && id.equals(idOf(e.getItem())))) {
			return new Found(item.position(), () -> {
				ItemStack stack = item.getItem().copy();
				item.discard();
				return stack;
			});
		}
		for (ServerPlayer other : level.players()) {
			if (other == owner) continue;
			found = inPack(other, id);
			if (found != null) return found;
		}
		for (Entity entity : level.getAllEntities()) {
			if (entity instanceof ServerPlayer || !entity.isAlive()) continue;
			if (entity instanceof LivingEntity living) {
				for (EquipmentSlot slot : EquipmentSlot.values()) {
					if (!id.equals(idOf(living.getItemBySlot(slot)))) continue;
					return new Found(living.position(), () -> {
						ItemStack stack = living.getItemBySlot(slot).copy();
						living.setItemSlot(slot, ItemStack.EMPTY);
						return stack;
					});
				}
			} else if (entity instanceof ItemFrame frame && id.equals(idOf(frame.getItem()))) {
				return new Found(frame.position(), () -> {
					ItemStack stack = frame.getItem().copy();
					frame.setItem(ItemStack.EMPTY);
					return stack;
				});
			} else if (entity instanceof Container hold) {
				found = inContainer(hold, entity.position(), id);
				if (found != null) return found;
			}
		}
		for (ChunkHolder holder : ((com.example.defyingtheheavens.mixin.ChunkMapAccessor) level.getChunkSource().chunkMap).dth$chunks()) {
			LevelChunk chunk = level.getChunkSource().getChunkNow(holder.getPos().x, holder.getPos().z);
			if (chunk == null) continue;
			for (BlockEntity block : chunk.getBlockEntities().values()) {
				if (!(block instanceof Container container)) continue;
				found = inContainer(container, Vec3.atCenterOf(block.getBlockPos()), id);
				if (found != null) return found;
			}
		}
		return null;
	}

	private static @Nullable Found inPack(ServerPlayer holder, UUID id) {
		Inventory pack = holder.getInventory();
		for (int slot = 0; slot < pack.getContainerSize(); slot++) {
			if (!id.equals(idOf(pack.getItem(slot)))) continue;
			int at = slot;
			return new Found(holder.position().add(0, 1, 0), () -> {
				ItemStack stack = pack.getItem(at).copy();
				pack.setItem(at, ItemStack.EMPTY);
				holder.inventoryMenu.broadcastChanges();
				return stack;
			});
		}
		return null;
	}

	private static @Nullable Found inContainer(Container container, Vec3 at, UUID id) {
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			if (!id.equals(idOf(container.getItem(slot)))) continue;
			int which = slot;
			return new Found(at, () -> {
				ItemStack stack = container.getItem(which).copy();
				container.setItem(which, ItemStack.EMPTY);
				container.setChanged();
				return stack;
			});
		}
		return null;
	}

	/** {@code stack} into {@code player}'s main hand: what they held moved into their pack (no room: their treasure into the pack, or at their feet). */
	static void toHand(ServerPlayer player, ItemStack stack) {
		Inventory pack = player.getInventory();
		ItemStack held = player.getMainHandItem();
		if (held.isEmpty()) {
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		} else {
			int free = pack.getFreeSlot();
			if (free >= 0 && free != pack.selected) {
				pack.setItem(free, held.copy());
				player.setItemInHand(InteractionHand.MAIN_HAND, stack);
			} else if (!pack.add(stack)) {
				player.drop(stack, false);
			}
		}
		player.inventoryMenu.broadcastChanges();
	}

	/** Its flight: a streak of light from where it was to its owner, a sound at each end. */
	private static void trail(ServerLevel level, Vec3 from, ServerPlayer to) {
		Vec3 end = to.position().add(0, 1.0, 0);
		double length = from.distanceTo(end);
		if (to.level() == level && length < 96) {
			int points = (int) Math.min(48, Math.max(4, length * 2));
			for (int i = 0; i <= points; i++) {
				Vec3 at = from.lerp(end, i / (double) points);
				level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0);
			}
		}
		level.playSound(null, from.x, from.y, from.z, SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.8f, 1.2f);
		level.playSound(null, to.blockPosition(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 1.0f, 1.0f);
		level.sendParticles(ParticleTypes.END_ROD, end.x, end.y, end.z, 16, 0.3, 0.4, 0.3, 0.05);
	}

	// --- Broken, severed ---

	/** {@code stack} wears out in {@code holder}'s hands (ArtifactWeaponItem's blows and blocks): a natal treasure gone, its owner's soul wounded. */
	static void broken(ItemStack stack, LivingEntity holder) {
		if (!isNatal(stack) || !(holder.level() instanceof ServerLevel level)) return;
		UUID id = idOf(stack), owner = ownerOf(stack);
		ServerPlayer player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
		if (player == null) {
			// Its owner away: the bond ends when they come back (#login), the wound with it.
			if (owner != null) BROKEN_AWAY.put(owner, id);
			return;
		}
		PlayerCultivation own = CultivationManager.own(player);
		if (!Objects.equals(id, own.natalTreasure())) return;
		own.setNatalTreasure(null);
		wound(player);
		tell(player, Component.translatable(ModLang.MSG_NATAL_BROKEN, stack.getHoverName()), ChatFormatting.RED);
		CultivationManager.markDirty(player.server);
		CultivationManager.sync(player);
	}

	/** Natal treasures broken while their owners were away (owner to the treasure's id): told, and wounded, when they come back. */
	private static final Map<UUID, UUID> BROKEN_AWAY = new HashMap<>();

	/** {@code player} back: a natal treasure of theirs that broke while they were away is gone, and their soul wounded. */
	public static void login(ServerPlayer player) {
		UUID id = BROKEN_AWAY.remove(player.getUUID());
		if (id == null) return;
		PlayerCultivation own = CultivationManager.own(player);
		if (!id.equals(own.natalTreasure())) return;
		own.setNatalTreasure(null);
		wound(player);
		tell(player, Component.translatable(ModLang.MSG_NATAL_BROKEN, Component.translatable(ModLang.NATAL_TREASURE)), ChatFormatting.RED);
		CultivationManager.markDirty(player.server);
	}

	/** {@code player}'s soul wounded for {@link #WOUND_TICKS}: half their qi pool, their blows the weaker (ModEffects#SOUL_WOUND). */
	public static void wound(ServerPlayer player) {
		PlayerCultivation own = CultivationManager.own(player);
		own.setSoulWoundUntil(player.level().getGameTime() + WOUND_TICKS);
		player.addEffect(new MobEffectInstance(ModEffects.SOUL_WOUND, WOUND_TICKS, 0, false, true, true));
		settle(player, own, true);
		ServerLevel level = player.serverLevel();
		level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.2, player.getZ(), 24, 0.4, 0.5, 0.4, 0.04);
		level.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, 0.7f);
		level.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8f, 0.6f);
	}

	/** What of {@code c}'s qi pool it keeps (PlayerCultivation#maxQi): a wounded soul's half. */
	public static double poolFactor(PlayerCultivation c) {
		return c.soulWounded() ? WOUNDED_POOL : 1;
	}

	/** {@code player}'s record told whether its soul is wounded now; its qi no more than the pool holds. */
	private static void settle(ServerPlayer player, PlayerCultivation own, boolean wounded) {
		if (own.soulWounded() == wounded) return;
		own.setSoulWounded(wounded);
		PlayerCultivation c = CultivationManager.get(player);
		if (c.getQi() > c.maxQi()) c.setQi(c.maxQi());
		CultivationManager.sync(player);
	}

	/**
	 * Each second: every wounded soul's wound kept (its effect given back if milk or death took it before its time) and its record told,
	 * and a healed one's told too.
	 */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) return;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) second(player);
	}

	/** A second of {@code player}'s: their wound kept to its time (its effect given back, or taken once healed), their record told. */
	static void second(ServerPlayer player) {
		PlayerCultivation own = CultivationManager.own(player);
		long left = own.soulWoundUntil() - player.level().getGameTime();
		boolean wounded = left > 0;
		if (wounded && !player.hasEffect(ModEffects.SOUL_WOUND)) {
			player.addEffect(new MobEffectInstance(ModEffects.SOUL_WOUND, (int) Math.min(left, WOUND_TICKS), 0, false, true, true));
		}
		if (!wounded && own.soulWoundUntil() > 0) {
			own.setSoulWoundUntil(0);
			player.removeEffect(ModEffects.SOUL_WOUND);
		}
		settle(player, own, wounded);
	}

	public static void forget(UUID id) {
		ASKED.remove(id);
		RECALLED.remove(id);
	}

	public static void clear() {
		ASKED.clear();
		RECALLED.clear();
		BROKEN_AWAY.clear();
	}

	private static void tell(ServerPlayer player, Component message, ChatFormatting style) {
		player.displayClientMessage(message.copy().withStyle(style), true);
	}

	private NatalTreasures() {}
}
