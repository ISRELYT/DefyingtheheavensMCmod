package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * The Command Seal's work (CommandSealItem; the client's CommandSealScreen): the list of a puppeteer's puppets from their ledger
 * (Puppetry.Ledger: each with its order, its health and where it is, wherever it stands), and the orders given through it. A puppet hears
 * its puppeteer within their reach (Puppetry#reach) times its Clarity's share (Puppetry#clarityReach), in the same world, where it is
 * loaded; an inert one hears nothing. Taking one apart wants its puppeteer beside it. A bee is set to work between the two chests the seal
 * marks, or sent with a message to a player (PuppetCouriers).
 * <p>
 * Seizing another's puppet ({@link #seize}): one's Puppetry rank above its puppeteer's, two above for Fine circuits, three for Perfect
 * ones (its seal holds the harder); a seal in hand of a grade above the one it is bound to (phase 2c); a slot free for it; the seal held on
 * it {@link #seizeTicks} (its Clarity draws it out). A cultivator's puppet (an NPC puppeteer's) counts its maker as an Adept.
 * <p>
 * Phase 2c's ranks of command: a Sub-Seal written for a puppet on its page (a sheet of paper; SubSealItem), every one of them voided
 * (Revoke); a humanoid of an Earth frame or better made a General (Puppetry#generalRefusal), its ranks mustered and deployed.
 */
public final class PuppetCommands {
	/** The acts of C2S puppet_command. */
	public static final int CHOOSE = 0, FOLLOW = 1, GUARD = 2, HOLD = 3, WORK = 4, MESSAGE = 5, DISMANTLE = 6, SUB_SEAL = 7, REVOKE = 8,
			MAKE_GENERAL = 9, MUSTER = 10, DEPLOY = 11, RELEASE_SOUL = 12, CONTROL_SOUL = 13, SEVER_SOUL = 14, ENTER_SOUL = 15, OFFER_SOUL = 16,
			SELF_DESTRUCT = 17, LEAVE_SOUL = 18;
	/** A message's most letters. */
	public static final int MESSAGE_LENGTH = 200;
	/** How near its puppeteer must be to take a puppet apart. */
	private static final double DISMANTLE_NEAR = 8;

	/** {@code player}'s list, {@code chosen} (or none) open on its page. */
	public static void open(ServerPlayer player, @Nullable UUID chosen) {
		send(player, chosen, null);
	}

	/** The list as it stands (S2C puppet_list), {@code said} at its foot. */
	static void send(ServerPlayer player, @Nullable UUID chosen, @Nullable Component said) {
		Puppetry.Ledger ledger = Puppetry.Ledger.get(player.server);
		PlayerCultivation own = CultivationManager.own(player);
		// A puppet gone from where its ledger left it, though that place's beings are loaded (wiped out of the world some other way), is
		// let go of: it would hold its slots for ever.
		for (Puppetry.Entry entry : ledger.of(player.getUUID())) {
			ServerLevel level = player.server.getLevel(entry.dimension());
			if (entry.away() || level == null || !level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(entry.pos()))) continue;
			if (level.getEntity(entry.id()) == null) ledger.remove(player.getUUID(), entry.id());
		}
		List<Puppetry.Entry> entries = ledger.of(player.getUUID());
		double reach = Puppetry.reach(own);
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(ledger.used(player.getUUID()));
		buf.writeVarInt(Puppetry.slots(own));
		buf.writeVarInt((int) Math.round(reach));
		buf.writeVarInt(Puppetry.rank(own).ordinal());
		ItemStack seal = held(player);
		BlockPos from = seal.isEmpty() ? null : CommandSealItem.markFrom(seal, player.level());
		BlockPos to = seal.isEmpty() ? null : CommandSealItem.markTo(seal, player.level());
		buf.writeBoolean(from != null && to != null);
		// The seal in hand's grade (-1: none in hand) (phase 2c).
		buf.writeVarInt(seal.isEmpty() ? -1 : CommandSealItem.grade(seal).ordinal());
		CraftMastery.Rank rank = Puppetry.rank(own);
		buf.writeVarInt(entries.size());
		for (Puppetry.Entry entry : entries) {
			buf.writeUUID(entry.id());
			buf.writeUtf(entry.name());
			buf.writeUtf(entry.blueprint().getId());
			buf.writeVarInt(entry.grade().ordinal());
			buf.writeVarInt(entry.rank());
			buf.writeVarInt(entry.order());
			buf.writeFloat(entry.health());
			buf.writeFloat(entry.maxHealth());
			buf.writeResourceLocation(entry.dimension().location());
			buf.writeBlockPos(entry.pos());
			buf.writeBoolean(entry.inert());
			buf.writeBoolean(entry.away());
			// In a General's ranks (phase 2c): its General's name (empty: not).
			String general = ledger.generalOf(entry.id());
			buf.writeUtf(general == null ? "" : general);
			PuppetEntity puppet = loaded(player, entry);
			buf.writeBoolean(puppet != null && inReach(player, puppet));
			if (puppet != null) {
				CircuitBoard.Scores s = puppet.scores();
				buf.writeBoolean(true);
				buf.writeComponent(Component.translatable(ModLang.circuitGrade(s.grade().name().toLowerCase(java.util.Locale.ROOT))));
				buf.writeVarInt(s.flow());
				buf.writeVarInt(s.stability());
				buf.writeVarInt(s.harmony());
				buf.writeVarInt(s.clarity());
				buf.writeVarInt(s.resilience());
				buf.writeVarInt(puppet.cuts().length);
				// Its ranks of command (phase 2c): its seal's grade, whose Sub-Seal it answers, a General's ranks (-1: no General; -2: one
				// can be made of it), its mind's bent (-1: none grown).
				buf.writeVarInt(puppet.sealGrade().ordinal());
				buf.writeUtf(puppet.commander() == null ? "" : puppet.commanderName());
				buf.writeVarInt(puppet.isGeneral() ? puppet.ranksHeld() : Puppetry.generalRefusal(puppet, rank) == null ? -2 : -1);
				PuppetSoul.Trait mind = puppet.mind();
				buf.writeVarInt(mind == null ? -1 : mind.ordinal());
				// A player's soul held in it (phase 2d): -1 none, 0 held under control, 1 released to play it, 2 one's own (one's body: a free
				// soul's); and how far it is copied.
				boolean person = PuppetPlayers.holdsPlayer(puppet);
				buf.writeVarInt(!person ? -1 : player.getUUID().equals(puppet.soul().player()) ? 2 : puppet.released() ? 1 : 0);
				buf.writeVarInt(person ? puppet.soul().copiedPercent() : 0);
				buf.writeBoolean(puppet.soul() == null); // hollow: one to enter, or to offer
				// The linked army (phase 3): its linked fellows near (-1: not linked), held down where it stands, a spring poured into it.
				buf.writeVarInt(puppet.linked() ? puppet.linkFellows() : -1);
				buf.writeBoolean(puppet.suppressed());
				buf.writeBoolean(puppet.springFed());
			} else {
				buf.writeBoolean(false);
			}
		}
		buf.writeBoolean(chosen != null);
		if (chosen != null) buf.writeUUID(chosen);
		buf.writeBoolean(said != null);
		if (said != null) buf.writeComponent(said);
		ServerPlayNetworking.send(player, ModPackets.PUPPET_LIST, buf);
	}

	/** The Command Seal in {@code player}'s hand (the main one first), or nothing. */
	private static ItemStack held(ServerPlayer player) {
		if (player.getMainHandItem().getItem() instanceof CommandSealItem) return player.getMainHandItem();
		if (player.getOffhandItem().getItem() instanceof CommandSealItem) return player.getOffhandItem();
		return ItemStack.EMPTY;
	}

	/** {@code entry}'s puppet, if it is in a loaded place of its world. */
	private static @Nullable PuppetEntity loaded(ServerPlayer player, Puppetry.Entry entry) {
		ServerLevel level = player.server.getLevel(entry.dimension());
		if (level == null) return null;
		return level.getEntity(entry.id()) instanceof PuppetEntity puppet && puppet.isAlive() ? puppet : null;
	}

	/**
	 * Whether {@code puppet} hears {@code player} from where they stand: within its Clarity's share of their reach, farther by its heart's
	 * veins; or, linked, through their other linked puppets (phase 3: {@link #relayed}).
	 */
	static boolean inReach(ServerPlayer player, PuppetEntity puppet) {
		if (puppet.level() != player.level()) return false;
		return hearsDirectly(player, puppet) || puppet.linked() && relayed(player, puppet);
	}

	/** Whether {@code puppet} hears {@code player} itself (its Clarity's share of their reach, farther by its heart's veins). */
	private static boolean hearsDirectly(ServerPlayer player, PuppetEntity puppet) {
		double reach = Puppetry.reach(CultivationManager.own(player)) * Puppetry.clarityReach(puppet.scores().clarity()) * puppet.reachFactor();
		return puppet.level() == player.level() && puppet.distanceTo(player) <= reach;
	}

	/**
	 * Whether {@code player}'s word reaches {@code target} passed along their linked puppets (phase 3: "they also pass your orders along
	 * beyond your Seal's range"): a chain of their linked puppets in its world, each within Puppetry#LINK_REACH of the next, from one that
	 * hears them itself to it. An inert one passes nothing on.
	 */
	public static boolean relayed(ServerPlayer player, PuppetEntity target) {
		if (!(target.level() instanceof ServerLevel level) || player.level() != level || !target.linked()) return false;
		List<PuppetEntity> linked = new java.util.ArrayList<>();
		for (Puppetry.Entry entry : Puppetry.Ledger.get(player.server).of(player.getUUID())) {
			if (entry.away() || entry.dimension() != level.dimension()) continue;
			if (level.getEntity(entry.id()) instanceof PuppetEntity p && p.isAlive() && p.linked() && !p.isInert()) linked.add(p);
		}
		java.util.ArrayDeque<PuppetEntity> reached = new java.util.ArrayDeque<>();
		java.util.Set<PuppetEntity> seen = new java.util.HashSet<>();
		for (PuppetEntity p : linked) {
			if (hearsDirectly(player, p)) {
				reached.add(p);
				seen.add(p);
			}
		}
		while (!reached.isEmpty()) {
			PuppetEntity p = reached.poll();
			if (p == target || p.getUUID().equals(target.getUUID())) return true;
			for (PuppetEntity next : linked) {
				if (!seen.contains(next) && next.distanceTo(p) <= Puppetry.LINK_REACH) {
					seen.add(next);
					reached.add(next);
				}
			}
		}
		return false;
	}

	/** C2S puppet_command: {@code act} for {@code id} (a bee's message {@code text} to the player named {@code to}). */
	public static void act(ServerPlayer player, UUID id, int act, String text, String to) {
		Puppetry.Entry entry = Puppetry.Ledger.get(player.server).get(player.getUUID(), id);
		if (entry == null) {
			send(player, null, Component.translatable(ModLang.COMMAND_GONE).withStyle(ChatFormatting.GRAY));
			return;
		}
		if (act == CHOOSE) {
			send(player, id, null);
			return;
		}
		PuppetEntity puppet = loaded(player, entry);
		if (puppet == null) {
			String general = Puppetry.Ledger.get(player.server).generalOf(id);
			send(player, id, (general != null ? Component.translatable(ModLang.COMMAND_IN_RANKS, general)
					: Component.translatable(entry.away() ? ModLang.COMMAND_AWAY : ModLang.COMMAND_UNLOADED)).withStyle(ChatFormatting.GOLD));
			return;
		}
		// A Sub-Seal written, or every one of them voided (phase 2c): its puppeteer's word, heard or not.
		if (act == SUB_SEAL) {
			if (!player.getAbilities().instabuild && !PuppetBoards.take(player, net.minecraft.world.item.Items.PAPER, 1)) {
				send(player, id, Component.translatable(ModLang.SUB_SEAL_NEEDS).withStyle(ChatFormatting.GOLD));
				return;
			}
			ItemStack sub = SubSealItem.of(puppet, player);
			if (!player.getInventory().add(sub)) player.drop(sub, false);
			player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8f, 1.0f);
			send(player, id, Component.translatable(ModLang.SUB_SEAL_WRITTEN, puppet.getDisplayName()));
			return;
		}
		if (act == REVOKE) {
			puppet.revokeSubSeals();
			player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PUT, SoundSource.PLAYERS, 0.8f, 0.8f);
			send(player, id, Component.translatable(ModLang.SUB_SEAL_REVOKED, puppet.getDisplayName()));
			return;
		}
		// One's own body, one's soul free in it (phase 2d): never released nor severed; left (pressed twice), one's soul going on.
		boolean ownBody = PuppetPlayers.holdsPlayer(puppet) && player.getUUID().equals(puppet.soul().player());
		if (act == LEAVE_SOUL) {
			if (ownBody) PuppetPlayers.act(player, PuppetPlayers.LEAVE);
			else send(player, id, Component.translatable(ModLang.PUPPET_SOUL_NOT_FREE).withStyle(ChatFormatting.GOLD));
			return;
		}
		if (ownBody && (act == RELEASE_SOUL || act == CONTROL_SOUL || act == SEVER_SOUL)) {
			send(player, id, Component.translatable(ModLang.LIST_PERSON_OWN).withStyle(ChatFormatting.GOLD));
			return;
		}
		// A player's soul held in it (phase 2d): released to play it, controlled again, or severed for good, at its puppeteer's will.
		if (act == RELEASE_SOUL || act == CONTROL_SOUL) {
			send(player, id, PuppetPlayers.release(player, puppet, act == RELEASE_SOUL));
			return;
		}
		if (act == SEVER_SOUL) {
			Component said = PuppetPlayers.severByOwner(player, puppet);
			send(player, puppet.isOwnedBy(player) ? id : null, said);
			return;
		}
		// One's own hollow puppet beside one entered (one's body dies: pressed twice), or offered to the player beside one (phase 2d).
		if (act == ENTER_SOUL) {
			Component said = PuppetPlayers.enter(player, puppet);
			if (said != null) send(player, id, said);
			return;
		}
		if (act == OFFER_SOUL) {
			send(player, id, PuppetPlayers.offer(player, puppet, player.server.getPlayerList().getPlayerByName(to.strip())));
			return;
		}
		if (act == DISMANTLE) {
			if (puppet.level() != player.level() || puppet.distanceTo(player) > DISMANTLE_NEAR) {
				send(player, id, Component.translatable(ModLang.COMMAND_DISMANTLE_NEAR, (int) DISMANTLE_NEAR).withStyle(ChatFormatting.GOLD));
				return;
			}
			Component name = puppet.getDisplayName();
			puppet.dismantle();
			send(player, null, Component.translatable(ModLang.COMMAND_DISMANTLED, name));
			return;
		}
		if (!inReach(player, puppet)) {
			send(player, id, Component.translatable(ModLang.COMMAND_OUT_OF_REACH).withStyle(ChatFormatting.GOLD));
			return;
		}
		if (puppet.isInert()) {
			send(player, id, Component.translatable(ModLang.COMMAND_INERT).withStyle(ChatFormatting.RED));
			return;
		}
		// Its puppeteer's own order takes it back from whoever held a Sub-Seal of it, and a better seal in their hand binds it (phase 2c).
		if (act == FOLLOW || act == GUARD || act == HOLD || act == WORK) {
			puppet.reclaim();
			ItemStack seal = held(player);
			if (!seal.isEmpty() && puppet.bindSeal(CommandSealItem.grade(seal))) {
				player.displayClientMessage(Component.translatable(ModLang.SEAL_BOUND, puppet.getDisplayName(), CommandSealItem.grade(seal).getDisplayName())
						.withStyle(ChatFormatting.AQUA), true);
			}
		}
		Component said;
		switch (act) {
			// Self-destruct (phase 3, pressed twice): its fuse lit; it bursts in a moment, its core with it.
			case SELF_DESTRUCT -> {
				puppet.lightFuse();
				said = Component.translatable(ModLang.PUPPET_FUSE_LIT, puppet.getDisplayName()).withStyle(ChatFormatting.GOLD);
			}
			case MAKE_GENERAL -> said = makeGeneral(player, puppet);
			case MUSTER -> said = muster(player, puppet);
			case DEPLOY -> {
				int out = puppet.isGeneral() ? puppet.deploy() : 0;
				said = out > 0 ? Component.translatable(ModLang.GENERAL_DEPLOYED, puppet.getDisplayName(), out)
						: Component.translatable(ModLang.GENERAL_EMPTY).withStyle(ChatFormatting.GOLD);
			}
			case FOLLOW -> {
				puppet.command(PuppetEntity.Order.FOLLOW, null, null);
				said = Component.translatable(ModLang.COMMAND_TOLD, puppet.getDisplayName(), PuppetEntity.Order.FOLLOW.getDisplayName());
			}
			case GUARD -> {
				puppet.command(PuppetEntity.Order.GUARD, player.blockPosition(), null); // guarding where its puppeteer stands
				said = Component.translatable(ModLang.COMMAND_TOLD, puppet.getDisplayName(), PuppetEntity.Order.GUARD.getDisplayName());
			}
			case HOLD -> {
				puppet.command(PuppetEntity.Order.HOLD, null, null);
				said = Component.translatable(ModLang.COMMAND_TOLD, puppet.getDisplayName(), PuppetEntity.Order.HOLD.getDisplayName());
			}
			case WORK -> {
				ItemStack seal = held(player);
				BlockPos from = seal.isEmpty() ? null : CommandSealItem.markFrom(seal, puppet.level());
				BlockPos target = seal.isEmpty() ? null : CommandSealItem.markTo(seal, puppet.level());
				if (!puppet.isBee()) said = Component.translatable(ModLang.COMMAND_BEES_ONLY).withStyle(ChatFormatting.GOLD);
				else if (from == null || target == null) said = Component.translatable(ModLang.COMMAND_MARK_FIRST).withStyle(ChatFormatting.GOLD);
				else {
					puppet.command(PuppetEntity.Order.WORK, from, target);
					said = Component.translatable(ModLang.COMMAND_WORKING, puppet.getDisplayName(), from.getX(), from.getY(), from.getZ(),
							target.getX(), target.getY(), target.getZ());
				}
			}
			case MESSAGE -> {
				String message = text.strip();
				ServerPlayer addressee = player.server.getPlayerList().getPlayerByName(to.strip());
				if (!puppet.isBee()) said = Component.translatable(ModLang.COMMAND_BEES_ONLY).withStyle(ChatFormatting.GOLD);
				else if (PuppetPlayers.holdsPlayer(puppet)) said = Component.translatable(ModLang.PUPPET_SOUL_NO_ERRANDS).withStyle(ChatFormatting.GOLD);
				else if (message.isEmpty()) said = Component.translatable(ModLang.COMMAND_NO_MESSAGE).withStyle(ChatFormatting.GOLD);
				else if (addressee == null) said = Component.translatable(ModLang.COMMAND_NO_ADDRESSEE, to.strip()).withStyle(ChatFormatting.GOLD);
				else said = PuppetCouriers.send(puppet, player, addressee, message.length() > MESSAGE_LENGTH ? message.substring(0, MESSAGE_LENGTH) : message);
			}
			default -> said = null;
		}
		player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5f, 1.4f);
		send(player, id, said);
	}

	// --- Generals (phase 2c) ---

	/** {@code puppet} made a General for its puppeteer {@code player}, if it can be and they have the stones. What to tell them. */
	static Component makeGeneral(ServerPlayer player, PuppetEntity puppet) {
		if (puppet.isGeneral()) return Component.translatable(ModLang.GENERAL_MADE, puppet.getDisplayName(), Puppetry.GENERAL_RANKS);
		String refusal = Puppetry.generalRefusal(puppet, Puppetry.rank(CultivationManager.own(player)));
		if (refusal != null) return Component.translatable(refusal, Puppetry.GENERAL_RANK.getDisplayName()).withStyle(ChatFormatting.GOLD);
		net.minecraft.world.item.Item stone = puppet.frameGrade().stone();
		if (!player.getAbilities().instabuild && !PuppetBoards.take(player, stone, Puppetry.GENERAL_STONES)) {
			return Component.translatable(ModLang.GENERAL_STONES, Puppetry.GENERAL_STONES, new ItemStack(stone).getHoverName()).withStyle(ChatFormatting.GOLD);
		}
		puppet.makeGeneral();
		puppet.remember();
		if (puppet.level() instanceof ServerLevel level) {
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING, puppet.getX(), puppet.getY(0.8), puppet.getZ(), 30, 0.3, 0.6, 0.3, 0.2);
			level.playSound(null, puppet.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 0.5f, 1.4f);
		}
		return Component.translatable(ModLang.GENERAL_MADE, puppet.getDisplayName(), Puppetry.GENERAL_RANKS).withStyle(ChatFormatting.GOLD);
	}

	/** {@code player}'s puppets within Puppetry#MUSTER_REACH of their General {@code general} taken into its ranks, the nearest first. */
	static Component muster(ServerPlayer player, PuppetEntity general) {
		if (!general.isGeneral()) return Component.translatable(ModLang.GENERAL_EMPTY).withStyle(ChatFormatting.GOLD);
		if (general.ranksHeld() >= Puppetry.GENERAL_RANKS) return Component.translatable(ModLang.GENERAL_FULL, Puppetry.GENERAL_RANKS).withStyle(ChatFormatting.GOLD);
		List<PuppetEntity> near = new java.util.ArrayList<>(general.level().getEntitiesOfClass(PuppetEntity.class,
				general.getBoundingBox().inflate(Puppetry.MUSTER_REACH), p -> p != general && p.isAlive() && p.isOwnedBy(player) && !p.isGeneral()
						&& p.distanceTo(general) <= Puppetry.MUSTER_REACH));
		near.sort(java.util.Comparator.comparingDouble(p -> p.distanceToSqr(general)));
		int took = general.muster(near);
		if (took == 0) return Component.translatable(ModLang.GENERAL_NONE_NEAR, (int) Puppetry.MUSTER_REACH).withStyle(ChatFormatting.GOLD);
		return Component.translatable(ModLang.GENERAL_MUSTERED, general.getDisplayName(), took);
	}

	// --- Seizing ---

	/** The rank another's puppet counts its maker at: a player's own Puppetry; a cultivator's (an NPC puppeteer's) Adept. */
	private static int makerRank(ServerPlayer seizer, PuppetEntity puppet) {
		if (!puppet.ownedByPlayer() || puppet.getOwnerUUID() == null) return CraftMastery.Rank.ADEPT.ordinal();
		return Puppetry.rank(CultivationManager.own(seizer.server, puppet.getOwnerUUID())).ordinal(); // online or not
	}

	/**
	 * Why {@code seizer} can't seize {@code puppet}, or null if they may begin: a rank above its maker's, more by its circuits' grade, and one
	 * more for each vein feeding its heart past the first; never one with an Iron Lock on its heart.
	 */
	public static @Nullable Component seizeRefusal(ServerPlayer seizer, PuppetEntity puppet) {
		// An Iron Lock on its heart (the deeper circuits' arrays): no seal takes it, whoever holds it.
		if (puppet.ironLocked()) return Component.translatable(ModLang.SEIZE_LOCKED);
		int needed = makerRank(seizer, puppet) + puppet.heartVeins() - 1 + switch (puppet.scores().grade()) {
			case PERFECT -> 3;
			case FINE -> 2;
			default -> 1;
		};
		// Its circuits known to the seizer (phase 3: a wreck of its design taken apart): a rank less.
		if (Puppetry.knows(seizer.server, seizer.getUUID(), puppet.design())) needed = Math.max(0, needed - 1);
		CraftMastery.Rank own = Puppetry.rank(CultivationManager.own(seizer));
		if (own.ordinal() < needed) {
			return needed >= CraftMastery.Rank.values().length ? Component.translatable(ModLang.SEIZE_NEVER)
					: Component.translatable(ModLang.SEIZE_RANK, CraftMastery.Rank.values()[needed].getDisplayName());
		}
		// Its seal (phase 2c): the seal in hand must be of a grade above the one it is bound to.
		ItemStack seal = held(seizer);
		if (seal.isEmpty() || CommandSealItem.grade(seal).ordinal() <= puppet.sealGrade().ordinal()) {
			return puppet.sealGrade() == ArtifactGrade.IMMORTAL ? Component.translatable(ModLang.SEIZE_NEVER)
					: Component.translatable(ModLang.SEIZE_SEAL, puppet.sealGrade().getDisplayName());
		}
		Puppetry.Ledger ledger = Puppetry.Ledger.get(seizer.server);
		int used = ledger.used(seizer.getUUID()), most = Puppetry.slots(CultivationManager.own(seizer)), cost = Puppetry.cost(puppet.frameGrade());
		if (used + cost > most) return Component.translatable(ModLang.SEAL_NO_SLOTS, cost, used, most);
		return null;
	}

	/** Ticks the seal must be held on {@code puppet}: {@link CommandSealItem#SEIZE_TICKS}, and up to as long again by its Clarity. */
	public static int seizeTicks(PuppetEntity puppet) {
		// A free soul-puppet (a player's own body, phase 2d) is held against the seal by its soul: half a minute.
		PuppetSoul soul = puppet.soul();
		if (soul != null && soul.real() && puppet.getOwnerUUID() != null && puppet.getOwnerUUID().equals(soul.player())) return PuppetPlayers.FREE_SEIZE_TICKS;
		return CommandSealItem.SEIZE_TICKS + puppet.scores().clarity() * CommandSealItem.SEIZE_TICKS / 100;
	}

	/** {@code seizer} takes {@code puppet} (if they still may): its old puppeteer told. */
	static void seize(ServerPlayer seizer, PuppetEntity puppet) {
		Component refusal = seizeRefusal(seizer, puppet);
		if (refusal != null) {
			seizer.displayClientMessage(refusal.copy().withStyle(ChatFormatting.RED), true);
			return;
		}
		ServerPlayer was = puppet.getOwnerUUID() == null ? null : seizer.server.getPlayerList().getPlayer(puppet.getOwnerUUID());
		puppet.seizedBy(seizer, CommandSealItem.grade(held(seizer)));
		seizer.level().playSound(null, puppet.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0f, 0.7f);
		seizer.displayClientMessage(Component.translatable(ModLang.SEIZE_DONE, puppet.getDisplayName()).withStyle(ChatFormatting.GOLD), false);
		if (was != null) was.sendSystemMessage(Component.translatable(ModLang.SEIZE_LOST, puppet.getDisplayName(), seizer.getName()).withStyle(ChatFormatting.RED));
	}

	private PuppetCommands() {}
}
