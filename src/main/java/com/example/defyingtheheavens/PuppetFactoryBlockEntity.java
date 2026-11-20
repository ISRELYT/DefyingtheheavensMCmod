package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Puppet Factory's works (PuppetFactoryBlock; puppet phase 3): a Circuit Diagram ({@link #DIAGRAM}), matter ({@link #MATTER}, nine
 * slots) and demon cores ({@link #CORES}, four). While its maker is in the world it builds copies of the diagram's puppet, one at a time,
 * {@link Puppetry#factoryTicks} each: the frame's forging matter and the sealing's stones taken ({@link #needs}), a core that fits the frame
 * and seals the drawing, the frame cast so its flaws fall clear of the diagram's lines (else flawless), the drawing laid on it whole, and
 * the puppet stands up on the factory, hollow, holding there: so long as its maker's Puppetry paints every stroke of the drawing and they
 * have a slot free for it. Nothing is learnt by it (no practice). Building or not, every {@link Puppetry#FACTORY_MEND_TICKS} it mends its
 * maker's puppets within {@link Puppetry#FACTORY_REACH} blocks: one cut line (as it was drawn), and a quarter of one's health for a piece of
 * its frame's matter from its slots. Switched to repair only, it builds nothing.
 */
public class PuppetFactoryBlockEntity extends BaseContainerBlockEntity {
	public static final int DIAGRAM = 0, MATTER = 1, MATTER_SLOTS = 9, CORES = MATTER + MATTER_SLOTS, CORE_SLOTS = 4, SLOTS = CORES + CORE_SLOTS;
	/** What its menu is shown (synced): its mode, how far its copy has come (per mille), why it waits, how many it has built. */
	public static final int MODE = 0, PROGRESS = 1, STATE = 2, BUILT = 3, DATA = 4;
	public static final int BUILD = 0, REPAIR_ONLY = 1;
	/** Why it waits ({@link #STATE}); {@link #READY}: it builds. */
	public static final int READY = 0, NO_DIAGRAM = 1, NO_RANK = 2, NO_SLOTS = 3, NO_MATTER = 4, NO_CORE = 5, WONT_SEAL = 6, OWNER_AWAY = 7,
			REPAIRING = 8, NO_OWNER = 9;

	private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private @Nullable UUID owner;
	private String ownerName = "";
	private int mode, progress, state = NO_DIAGRAM, built, ticks;
	/** Its core slot that seals its diagram (coreSlot), weighed once till its slots change (a board's weighing isn't cheap); MIN_VALUE: to weigh. */
	private int coreChecked = Integer.MIN_VALUE;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case MODE -> mode;
				case PROGRESS -> progressPerMille();
				case STATE -> state;
				case BUILT -> built;
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {
			if (index == MODE) mode = value;
		}

		@Override
		public int getCount() {
			return DATA;
		}
	};

	public PuppetFactoryBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.PUPPET_FACTORY, pos, state);
	}

	public @Nullable UUID owner() { return owner; }
	public String ownerName() { return ownerName; }

	public void setOwner(ServerPlayer player) {
		owner = player.getUUID();
		ownerName = player.getGameProfile().getName();
		setChanged();
	}

	/** Its maker, or one in creative; anyone, if it has no maker yet. */
	public boolean mayUse(Player player) {
		return owner == null || owner.equals(player.getUUID()) || player.getAbilities().instabuild;
	}

	/** The mode button (its menu): building copies, or repair only. */
	public void toggleMode() {
		mode = mode == BUILD ? REPAIR_ONLY : BUILD;
		progress = 0;
		setChanged();
	}

	private int progressPerMille() {
		ItemStack diagram = items.get(DIAGRAM);
		if (!(diagram.getItem() instanceof CircuitDiagramItem)) return 0;
		return Math.min(1000, progress * 1000 / Math.max(1, Puppetry.factoryTicks(CircuitDiagramItem.grade(diagram))));
	}

	// --- What a copy takes ---

	/**
	 * What a copy of a {@code blueprint} frame of {@code grade} takes from its matter: the frame's forging (ArtifactRecipes: the grade's body,
	 * metal, sinews or wings and stones) and the sealing's Puppetry#SEAL_STONES of the grade's stones. In order, as the forge lists them.
	 */
	public static Map<Item, Integer> needs(PuppetBlueprint blueprint, ArtifactGrade grade) {
		Map<Item, Integer> needs = new LinkedHashMap<>();
		for (ArtifactRecipes.Recipe recipe : ArtifactRecipes.all()) {
			if (recipe.kind() == ArtifactKind.frameOf(blueprint) && recipe.grade() == grade) {
				needs.putAll(recipe.materials());
				break;
			}
		}
		needs.merge(grade.stone(), Puppetry.SEAL_STONES, Integer::sum);
		return needs;
	}

	/** How many of {@code item} lie in its matter's slots. */
	public static int count(List<ItemStack> matter, Item item) {
		int n = 0;
		for (ItemStack stack : matter) if (stack.is(item)) n += stack.getCount();
		return n;
	}

	private List<ItemStack> matter() {
		return items.subList(MATTER, MATTER + MATTER_SLOTS);
	}

	private boolean hasMatter(Map<Item, Integer> needs) {
		for (Map.Entry<Item, Integer> need : needs.entrySet()) if (count(matter(), need.getKey()) < need.getValue()) return false;
		return true;
	}

	private void takeMatter(Map<Item, Integer> needs) {
		for (Map.Entry<Item, Integer> need : needs.entrySet()) {
			int left = need.getValue();
			for (int i = MATTER; i < MATTER + MATTER_SLOTS && left > 0; i++) {
				ItemStack stack = items.get(i);
				if (!stack.is(need.getKey())) continue;
				int taken = Math.min(left, stack.getCount());
				stack.shrink(taken);
				left -= taken;
			}
		}
	}

	/** One of {@code item} taken from its matter: whether there was one. */
	private boolean takeOne(Item item) {
		for (int i = MATTER; i < MATTER + MATTER_SLOTS; i++) {
			if (!items.get(i).is(item)) continue;
			items.get(i).shrink(1);
			return true;
		}
		return false;
	}

	/**
	 * Its core slot whose core fits a {@code blueprint} frame of {@code grade} and seals {@code strokes} (the heart fed, the veins within its
	 * qi); -1 with none that fits the frame, -2 with some that fit but none that seals it.
	 */
	private int coreSlot(PuppetBlueprint blueprint, ArtifactGrade grade, byte[] strokes) {
		boolean fits = false;
		for (int i = CORES; i < CORES + CORE_SLOTS; i++) {
			ItemStack core = items.get(i);
			if (!core.is(ModItems.DEMON_CORE) || Puppetry.puppetRank(grade, DemonCoreItem.rank(core)) < 0) continue;
			fits = true;
			CircuitBoard board = new CircuitBoard(blueprint, grade.ordinal(), 0, 0);
			Puppetry.lay(board, strokes);
			CircuitBoard.Scores scores = board.score(Puppetry.budget(grade, core), Puppetry.phaseOf(core));
			if (scores.sealFed() && !scores.over()) return i;
		}
		return fits ? -2 : -1;
	}

	/** Why it can't build now (READY: it can), for its maker {@code player} (null: away). */
	private int refusal(ServerLevel level, @Nullable ServerPlayer player) {
		if (owner == null) return NO_OWNER;
		ItemStack diagram = items.get(DIAGRAM);
		PuppetBlueprint blueprint = diagram.getItem() instanceof CircuitDiagramItem ? CircuitDiagramItem.blueprint(diagram) : null;
		if (blueprint == null) return NO_DIAGRAM;
		if (player == null) return OWNER_AWAY;
		ArtifactGrade grade = CircuitDiagramItem.grade(diagram);
		PlayerCultivation c = CultivationManager.own(player);
		if (Puppetry.rank(c).ordinal() < CircuitDiagramItem.rankWanted(diagram)) return NO_RANK;
		if (Puppetry.Ledger.get(level.getServer()).used(owner) + Puppetry.cost(grade) > Puppetry.slots(c)) return NO_SLOTS;
		if (!hasMatter(needs(blueprint, grade))) return NO_MATTER;
		if (coreChecked == Integer.MIN_VALUE) coreChecked = coreSlot(blueprint, grade, CircuitDiagramItem.strokes(diagram));
		return coreChecked == -1 ? NO_CORE : coreChecked == -2 ? WONT_SEAL : READY;
	}

	/** Its slots changed: its cores weighed again when next asked. */
	@Override
	public void setChanged() {
		coreChecked = Integer.MIN_VALUE;
		super.setChanged();
	}

	// --- Its work ---

	/** Each tick (server): its mending now and then; each second, its copy carried on, or why it waits. */
	void tick(ServerLevel level) {
		ticks++;
		if (ticks % Puppetry.FACTORY_MEND_TICKS == 0) mend(level);
		if (ticks % 20 != 0) return;
		ServerPlayer player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
		int why = mode == REPAIR_ONLY ? REPAIRING : refusal(level, player);
		if (why != state) {
			state = why;
			saved();
		}
		if (why != READY) return;
		ArtifactGrade grade = CircuitDiagramItem.grade(items.get(DIAGRAM));
		progress += 20;
		if (progress >= Puppetry.factoryTicks(grade)) {
			progress = 0;
			build(level, player);
			setChanged(); // its cores and matter spent: weighed again
		}
		saved();
	}

	/** Its progress kept with the world (not its slots changed: its cores' weighing stands). */
	private void saved() {
		if (level != null) setChanged(level, worldPosition, getBlockState());
	}

	/** A copy built and stood up on it, its maker told. */
	private void build(ServerLevel level, ServerPlayer player) {
		ItemStack diagram = items.get(DIAGRAM);
		PuppetBlueprint blueprint = CircuitDiagramItem.blueprint(diagram);
		ArtifactGrade grade = CircuitDiagramItem.grade(diagram);
		byte[] strokes = CircuitDiagramItem.strokes(diagram);
		int slot = blueprint == null ? -1 : coreSlot(blueprint, grade, strokes);
		if (slot < 0) return;
		ItemStack core = items.get(slot).split(1);
		takeMatter(needs(blueprint, grade));
		// Cast so its flaws fall clear of the drawing's lines: a Normal frame whose flaws miss them, else a flawless one.
		Long seed = castSeed(blueprint, grade, strokes, level.random.nextLong());
		ItemStack frame = PuppetFrameItem.of(blueprint, grade, seed != null ? Artifacts.Quality.NORMAL : Artifacts.Quality.PERFECT,
				seed != null ? seed : level.random.nextLong(), strokes);
		PuppetEntity puppet = PuppetEntity.seal(level, frame, core, player, Vec3.atBottomCenterOf(worldPosition.above()));
		puppet.command(PuppetEntity.Order.HOLD, null, null);
		built++;
		level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8f, 0.7f);
		level.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, 0.8f);
		level.sendParticles(ParticleTypes.END_ROD, worldPosition.getX() + 0.5, worldPosition.getY() + 1.6, worldPosition.getZ() + 0.5, 24, 0.3, 0.5, 0.3, 0.04);
		CircuitBoard.Scores scores = puppet.scores();
		player.sendSystemMessage(Component.translatable(ModLang.FACTORY_BUILT, puppet.getDisplayName(), worldPosition.getX(), worldPosition.getY(),
				worldPosition.getZ(), Component.translatable(ModLang.circuitGrade(scores.grade().name().toLowerCase(java.util.Locale.ROOT))))
				.withStyle(ChatFormatting.AQUA));
	}

	/** A seed (from {@code first} on) placing a Normal frame's flaws clear of every painted cell of {@code strokes}; null if none of 64 does. */
	static @Nullable Long castSeed(PuppetBlueprint blueprint, ArtifactGrade grade, byte[] strokes, long first) {
		java.util.Random random = new java.util.Random(first);
		for (int tries = 0; tries < 64; tries++) {
			long seed = random.nextLong();
			CircuitBoard board = new CircuitBoard(blueprint, grade.ordinal(), CircuitBoard.flawsFor(Artifacts.Quality.NORMAL.ordinal()), seed);
			boolean clear = true;
			for (int i = 0; i < strokes.length && clear; i++) {
				if (strokes[i] != 0 && board.isFlaw(i % board.size, i / board.size)) clear = false;
			}
			if (clear) return seed;
		}
		return null;
	}

	/** Its mending: one cut line of its maker's nearest puppet that has one, and a quarter of the health of the nearest hurt, for its matter. */
	private void mend(ServerLevel level) {
		if (owner == null) return;
		Vec3 centre = Vec3.atCenterOf(worldPosition);
		double reach = Puppetry.FACTORY_REACH;
		List<PuppetEntity> near = new java.util.ArrayList<>(level.getEntitiesOfClass(PuppetEntity.class, new AABB(worldPosition).inflate(reach),
				p -> p.isAlive() && owner.equals(p.getOwnerUUID()) && p.position().distanceTo(centre) <= reach));
		near.sort(Comparator.comparingDouble(p -> p.position().distanceToSqr(centre)));
		boolean mended = false, healed = false;
		for (PuppetEntity p : near) {
			if (!mended && p.cuts().length > 0 && p.mendOne()) mended = true;
			if (!healed && p.getHealth() < p.getMaxHealth() && takeOne(ArtifactRecipes.frameBody(p.frameGrade()))) {
				p.heal(p.getMaxHealth() * 0.25f);
				p.remember();
				healed = true;
				level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY(0.6), p.getZ(), 6, 0.25, 0.3, 0.25, 0.02);
			}
			if (mended && healed) break;
		}
		if (mended || healed) {
			level.playSound(null, worldPosition, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.5f, 1.3f);
			setChanged();
		}
	}

	// --- Its container ---

	@Override
	protected Component getDefaultName() {
		return Component.translatable(ModLang.FACTORY_TITLE);
	}

	@Override
	protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return new PuppetFactoryMenu(id, inventory, this, data);
	}

	@Override
	public int getContainerSize() { return SLOTS; }

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : items) if (!stack.isEmpty()) return false;
		return true;
	}

	@Override
	public ItemStack getItem(int slot) { return items.get(slot); }

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack taken = ContainerHelper.removeItem(items, slot, amount);
		if (!taken.isEmpty()) setChanged();
		return taken;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(slot, stack);
		if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
		// A new diagram starts its copy anew.
		if (slot == DIAGRAM) progress = 0;
		setChanged();
	}

	/** Its diagram's slot takes a Circuit Diagram, its cores' demon cores, its matter anything else. */
	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot == DIAGRAM) return stack.getItem() instanceof CircuitDiagramItem;
		if (slot >= CORES) return stack.is(ModItems.DEMON_CORE);
		return !(stack.getItem() instanceof CircuitDiagramItem) && !stack.is(ModItems.DEMON_CORE);
	}

	@Override
	public boolean stillValid(Player player) { return net.minecraft.world.Container.stillValidBlockEntity(this, player) && mayUse(player); }

	@Override
	public void clearContent() {
		items.clear();
		setChanged();
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		ContainerHelper.saveAllItems(tag, items);
		if (owner != null) tag.putUUID("Owner", owner);
		tag.putString("OwnerName", ownerName);
		tag.putInt("Mode", mode);
		tag.putInt("Progress", progress);
		tag.putInt("Built", built);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, items);
		owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
		ownerName = tag.getString("OwnerName");
		mode = tag.getInt("Mode");
		progress = tag.getInt("Progress");
		built = tag.getInt("Built");
	}
}
