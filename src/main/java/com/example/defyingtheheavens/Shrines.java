package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The shrine system (the methods overhaul's phase 3: the user's "worship... for the heaven path too, not only ghosts"): a shrine (ShrineBlock)
 * of whoever set it down, in a village, a sect or a city, prayed at by the people about it. Three methods gather by it, each its own
 * look ({@link Kind}): the heaven path's Incense and Faith (a statue), the ghost path's Ancestral Incense (an ancestral tablet), and the
 * Demon Path's Beast Deity Worship (a beast idol, prayed to by mortals only, as their mountain god). No talent weight on any.
 * <ul>
 *   <li><b>Prayer</b> ({@link #PRAYER_TICKS}): every villager within {@link #RADIUS} lays up one faith, every cultivator 2 to the power
 *   of its realm (Qi Refining 1), times the worshippers' devotion; at most {@link #MAX_FAITH} laid up.</li>
 *   <li><b>Incense</b>: another (never its owner) burns a stick of it there for {@link #INCENSE_FAITH}, 2 to the power of its own
 *   realm times as much.</li>
 *   <li><b>Faith flows</b> to its owner ({@link #FLOW_TICKS}), online and practising the shrine's method with its volume: each faith is
 *   {@link #PER_FAITH} of the path's stage, halved for each realm the path stands above Qi Refining, at most {@link #MOST_SHARE} a
 *   flow (CultivationMethods#grant); what isn't drawn waits.</li>
 *   <li><b>Requests</b>: once a game day, worshipped, its people ask something of its owner ({@link #REQUESTS}); brought (the owner's
 *   right-click with it) their devotion rises by {@link #DEVOTION_STEP} (to {@link #DEVOTION_MOST}) and {@link #ANSWER_FAITH} faith is
 *   laid up; left unanswered till the next, it falls as much (to {@link #DEVOTION_LEAST}).</li>
 *   <li><b>Desecration</b>: broken by another's hand, or blown apart, it wounds its owner ({@link #WOUND} of its health), told who.</li>
 * </ul>
 */
public final class Shrines {
	public static final int PRAYER_TICKS = 600, FLOW_TICKS = 1200, REQUEST_TICKS = 24_000;
	public static final double RADIUS = 12, MAX_FAITH = 4000, PER_FAITH = 0.004, MOST_SHARE = 0.25, INCENSE_FAITH = 4, ANSWER_FAITH = 40;
	public static final double DEVOTION_STEP = 0.25, DEVOTION_LEAST = 0.25, DEVOTION_MOST = 1.5;
	public static final float WOUND = 0.25f;

	/** What a shrine is (its look, ShrineBlock#KIND): whose method gathers by it, on which path. */
	public enum Kind implements StringRepresentable {
		HEAVEN("heaven", CultivationMethod.INCENSE_FAITH), GHOST("ghost", CultivationMethod.ANCESTRAL_INCENSE), DEMON("demon", CultivationMethod.BEAST_DEITY);

		private final String id;
		private final CultivationMethod method;

		Kind(String id, CultivationMethod method) {
			this.id = id;
			this.method = method;
		}

		@Override
		public String getSerializedName() { return id; }

		public CultivationMethod method() { return method; }
		public TwoPaths.Path path() { return method.path(); }
	}

	/** What its people ask of a shrine's owner, by its kind: an item and how many (its owner brings them; Kind order). */
	static final Object[][][] REQUESTS = {
			{{Items.BREAD, 8}, {Items.WHEAT, 16}, {Items.HONEY_BOTTLE, 2}, {Items.IRON_INGOT, 4}, {Items.CANDLE, 4}, {Items.GOLD_INGOT, 1}},
			{{Items.CANDLE, 6}, {Items.PAPER, 8}, {Items.GOLD_NUGGET, 9}, {Items.BONE_MEAL, 8}, {Items.SOUL_TORCH, 4}},
			{{Items.BEEF, 6}, {Items.PORKCHOP, 6}, {Items.MUTTON, 6}, {Items.BONE, 8}, {Items.RABBIT_HIDE, 4}}};

	/** The shrine one is worshipped by ({@code c}'s): the kind whose method it practises, else one it knows, else the heaven path's. */
	public static Kind kindFor(PlayerCultivation c) {
		for (Kind kind : Kind.values()) if (CultivationMethods.chosen(c, kind.path()) == kind.method()) return kind;
		for (Kind kind : Kind.values()) if (CultivationMethods.knows(c, kind.method())) return kind;
		return Kind.HEAVEN;
	}

	/** A shrine's tick (ShrineBlock's ticker): its prayers, its faith's flow and its requests, each in its own rhythm. */
	static void tick(Level level, BlockPos pos, BlockState state, ShrineBlockEntity shrine) {
		if (!(level instanceof ServerLevel server) || shrine.owner == null) return;
		shrine.ticks++;
		long now = level.getGameTime();
		Kind kind = state.getValue(ShrineBlock.KIND);
		if (shrine.ticks % PRAYER_TICKS == 0) pray(server, pos, kind, shrine, now);
		if (shrine.ticks % FLOW_TICKS == 0) flow(server, kind, shrine);
		if (shrine.lastWorship > 0 && now - shrine.requestAt >= REQUEST_TICKS) request(server, pos, kind, shrine, now);
	}

	/** What the people within {@link #RADIUS} of {@code pos} lay up in a prayer to a shrine of {@code kind} (before devotion). */
	public static double worship(ServerLevel level, BlockPos pos, Kind kind) {
		AABB around = new AABB(pos).inflate(RADIUS);
		Vec3 centre = Vec3.atCenterOf(pos);
		double worth = 0;
		for (Villager villager : level.getEntitiesOfClass(Villager.class, around, LivingEntity::isAlive)) {
			worth += 1;
			villager.getLookControl().setLookAt(centre);
		}
		for (CultivatorNpc npc : level.getEntitiesOfClass(CultivatorNpc.class, around, n -> n.isAlive() && !n.isGhost() && !n.isVacant())) {
			if (kind == Kind.DEMON && !npc.isMortal()) continue; // a mountain god's worshippers are mortals
			worth += npc.isMortal() ? 1 : Math.pow(2, npc.getCultivationRealm().ordinal());
			npc.getLookControl().setLookAt(centre);
		}
		return worth;
	}

	static void pray(ServerLevel level, BlockPos pos, Kind kind, ShrineBlockEntity shrine, long now) {
		double worth = worship(level, pos, kind);
		if (worth <= 0) return;
		shrine.faith = Math.min(MAX_FAITH, shrine.faith + worth * shrine.devotion);
		shrine.lastWorship = now;
		if (shrine.requestAt <= 0) shrine.requestAt = now; // its first day of worship: its first request comes a day on
		shrine.setChanged();
		level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 3, 0.1, 0.05, 0.1, 0.01);
	}

	/** Its faith flows to its owner, online and practising its method with its volume: as much as one flow carries. @return the share won */
	static double flow(ServerLevel level, Kind kind, ShrineBlockEntity shrine) {
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(shrine.owner);
		if (owner == null || shrine.faith <= 0) return 0;
		PlayerCultivation c = CultivationManager.get(owner);
		if (CultivationMethods.practised(c, kind.path()) != kind.method() || !CultivationMethods.holdsVolume(c, kind.path())) return 0;
		Realm realm = CultivationMethods.realmOf(c, kind.path());
		double perFaith = perFaith(realm == null ? 0 : realm.ordinal());
		double share = Math.min(MOST_SHARE, shrine.faith * perFaith);
		if (CultivationMethods.grant(owner, kind.path(), kind.method(), share) <= 0) return 0; // its bottleneck: the faith waits
		shrine.faith = Math.max(0, shrine.faith - share / perFaith);
		shrine.setChanged();
		return share;
	}

	/** The share of its path's stage one faith is to an owner whose path stands in {@code realm} (ordinal). */
	public static double perFaith(int realm) {
		return PER_FAITH / Math.pow(2, realm);
	}

	/** A new request (a day since the last): the last one unanswered lowers their devotion; the owner told, if online. */
	static void request(ServerLevel level, BlockPos pos, Kind kind, ShrineBlockEntity shrine, long now) {
		if (shrine.hasRequest()) shrine.devotion = Math.max(DEVOTION_LEAST, shrine.devotion - DEVOTION_STEP);
		Object[][] asks = REQUESTS[kind.ordinal()];
		Object[] ask = asks[level.random.nextInt(asks.length)];
		Item item = (Item) ask[0];
		shrine.request = BuiltInRegistries.ITEM.getKey(item).toString();
		shrine.requestCount = (Integer) ask[1];
		shrine.requestAt = now;
		shrine.setChanged();
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(shrine.owner);
		if (owner != null) {
			owner.sendSystemMessage(Component.translatable(ModLang.MSG_SHRINE_REQUEST, shrine.requestCount, item.getDescription(),
					pos.getX() + ", " + pos.getY() + ", " + pos.getZ()).withStyle(ChatFormatting.GOLD));
		}
	}

	/** Right-clicked (ShrineBlock#use): a request answered, incense burned, or what it holds told. */
	static InteractionResult use(BlockState state, ServerPlayer player, ShrineBlockEntity shrine, InteractionHand hand) {
		ServerLevel level = player.serverLevel();
		BlockPos pos = shrine.getBlockPos();
		ItemStack held = player.getItemInHand(hand);
		boolean owner = player.getUUID().equals(shrine.owner);
		Item asked = shrine.hasRequest() ? BuiltInRegistries.ITEM.get(new ResourceLocation(shrine.request)) : null;
		if (owner && asked != null && held.is(asked) && held.getCount() >= shrine.requestCount) {
			if (!player.getAbilities().instabuild) held.shrink(shrine.requestCount);
			shrine.request = "";
			shrine.devotion = Math.min(DEVOTION_MOST, shrine.devotion + DEVOTION_STEP);
			shrine.faith = Math.min(MAX_FAITH, shrine.faith + ANSWER_FAITH);
			shrine.setChanged();
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 12, 0.4, 0.3, 0.4, 0.02);
			level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.2f);
			player.displayClientMessage(Component.translatable(ModLang.MSG_SHRINE_ANSWERED, devotion(shrine)).withStyle(ChatFormatting.GOLD), true);
			return InteractionResult.SUCCESS;
		}
		if (held.is(ModItems.INCENSE)) {
			if (owner) {
				player.displayClientMessage(Component.translatable(ModLang.MSG_SHRINE_OWN_INCENSE).withStyle(ChatFormatting.GRAY), true);
				return InteractionResult.SUCCESS;
			}
			if (!player.getAbilities().instabuild) held.shrink(1);
			int realm = TwoPaths.higherRank(CultivationManager.get(player), false) / Stage.values().length;
			shrine.faith = Math.min(MAX_FAITH, shrine.faith + INCENSE_FAITH * Math.pow(2, Math.max(0, realm)) * shrine.devotion);
			shrine.lastWorship = level.getGameTime();
			shrine.setChanged();
			level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 10, 0.1, 0.1, 0.1, 0.02);
			level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.4f, 1.6f);
			player.displayClientMessage(Component.translatable(ModLang.MSG_SHRINE_INCENSE, shrine.ownerName).withStyle(ChatFormatting.GOLD), true);
			return InteractionResult.SUCCESS;
		}
		Kind kind = state.getValue(ShrineBlock.KIND);
		if (owner) {
			Component asks = asked == null ? Component.translatable(ModLang.SHRINE_NO_REQUEST)
					: Component.translatable(ModLang.SHRINE_ASKS, shrine.requestCount, asked.getDescription());
			player.displayClientMessage(Component.translatable(ModLang.MSG_SHRINE_STATUS, kind.method().getDisplayName(), Math.round(shrine.faith),
					devotion(shrine), asks).withStyle(ChatFormatting.AQUA), true);
		} else {
			player.displayClientMessage(Component.translatable(ModLang.MSG_SHRINE_OF, shrine.ownerName, kind.method().getDisplayName())
					.withStyle(ChatFormatting.GRAY), true);
		}
		return InteractionResult.SUCCESS;
	}

	private static String devotion(ShrineBlockEntity shrine) {
		return String.format(java.util.Locale.ROOT, "%.2f", shrine.devotion);
	}

	/** Gone (ShrineBlock#onRemove): taken up by its owner, nothing; else desecrated: its owner wounded and told, its faith lost. */
	static void removed(Level level, BlockPos pos, ShrineBlockEntity shrine) {
		if (shrine.owner == null || shrine.owner.equals(shrine.breaker) || !(level instanceof ServerLevel server)) return;
		ServerPlayer owner = server.getServer().getPlayerList().getPlayer(shrine.owner);
		if (owner == null) return;
		ServerPlayer culprit = shrine.breaker == null ? null : server.getServer().getPlayerList().getPlayer(shrine.breaker);
		if (!owner.isCreative()) owner.setHealth(Math.max(1, owner.getHealth() - owner.getMaxHealth() * WOUND));
		owner.sendSystemMessage(Component.translatable(culprit != null ? ModLang.MSG_SHRINE_DESECRATED_BY : ModLang.MSG_SHRINE_DESECRATED,
				pos.getX() + ", " + pos.getY() + ", " + pos.getZ(), culprit == null ? "" : culprit.getGameProfile().getName()).withStyle(ChatFormatting.RED));
		owner.playNotifySound(SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.6f, 0.6f);
	}

	private Shrines() {}
}
