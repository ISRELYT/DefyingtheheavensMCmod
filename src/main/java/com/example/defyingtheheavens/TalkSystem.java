package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Talking to cultivators. Right-click one and, if they will talk, the talk wheel opens (client: TalkWheelScreen) with what
 * you can mean by it ({@link TalkIntent}). Greet, Gift and Provoke are answered in one line; Ask opens the dialogue box
 * (DialogueScreen) with topics to ask about, answered from the world as it is: who they are, their sect and its people,
 * the sects nearby.
 * <ul>
 *   <li><b>Standing</b> ({@link Standing}): their realm against yours, as each side shows it (so Realm Suppression and
 *   concealment change it), sets how they speak to you and what they will entertain. Their path sets the tone.</li>
 *   <li><b>Regard</b> ({@link Regard}): what they think of you, changed by what you do here and elsewhere.</li>
 * </ul>
 * They won't talk in a fight, in seclusion, on their way to the sky, or to an enemy of their path. Talking to one who sits
 * in meditation wakes them; elders and the Sect Master on duty don't mind, anyone else thinks a little less of you.
 */
public final class TalkSystem {
	/** How close a player must be to start talking, and to keep talking. */
	public static final double REACH = 6.0;
	public static final double KEEP = 8.0;
	/** Sects further than this aren't known of. */
	private static final double KNOWN_SECTS = 2000;

	/** Their realm against yours. */
	public enum Standing {
		FAR_ABOVE("far_above"), ABOVE("above"), PEER("peer"), BELOW("below"), FAR_BELOW("far_below"),
		/** The player is a mortal: spoken to as one, whatever the cultivator. */
		MORTAL("mortal");

		private final String id;

		Standing(String id) {
			this.id = id;
		}

		public String getId() { return id; }
	}

	/** What one can ask about. */
	public enum Topic {
		SELF("self"), SECT("sect"), PEOPLE("people"), NEARBY("nearby");

		private final String id;

		Topic(String id) {
			this.id = id;
		}

		public String getId() { return id; }

		public static Topic byIndex(int index) {
			return index >= 0 && index < values().length ? values()[index] : null;
		}
	}

	/** One line on the right of the dialogue box: a topic to ask about. */
	public record Reply(int id, Component label, boolean enabled, Component note) {}

	public static Standing standing(ServerPlayer player, CultivatorNpc npc) {
		PlayerCultivation c = CultivationManager.get(player);
		if (c.isMortal()) return npc.isMortal() ? Standing.PEER : Standing.MORTAL;
		int theirs = npc.isMortal() ? -1 : npc.getDisplayedRealm().ordinal();
		int gap = theirs - c.getEffectiveRealm().ordinal();
		if (gap >= 2) return Standing.FAR_ABOVE;
		if (gap == 1) return Standing.ABOVE;
		if (gap == 0) return Standing.PEER;
		return gap == -1 ? Standing.BELOW : Standing.FAR_BELOW;
	}

	/** The intents they will entertain at {@code standing}, as {@link TalkIntent#bit()}s. Far above you, or to a mortal, no insults. */
	public static int intents(Standing standing) {
		int mask = TalkIntent.GREET.bit() | TalkIntent.ASK.bit() | TalkIntent.GIFT.bit();
		if (standing != Standing.FAR_ABOVE && standing != Standing.MORTAL) mask |= TalkIntent.PROVOKE.bit();
		return mask;
	}

	// --- Opening a talk ---

	/** Why they won't talk to {@code player} (the line to say so), or null if they will. */
	private static String refusal(ServerPlayer player, CultivatorNpc npc) {
		if (npc.getLifecycle() == CultivatorNpc.Lifecycle.SECLUDED && npc.isConcealed()) return ModLang.NPC_GREET_SECLUDED;
		if (npc.getTarget() == player || npc.isEnemy(player)) return ModLang.NPC_GREET_HOSTILE;
		if (npc.isInCombat() || npc.shouldFlee() || npc.getLifecycle() == CultivatorNpc.Lifecycle.DEPARTING
				|| npc.getLifecycle() == CultivatorNpc.Lifecycle.ASCENDING) return ModLang.TALK_BUSY;
		return null;
	}

	/** A right-click: opens the talk wheel, or says in a line why they won't talk. */
	public static void open(ServerPlayer player, CultivatorNpc npc) {
		if (player.isSpectator() || !npc.isAlive()) return;
		String refusal = refusal(player, npc);
		if (refusal != null) {
			player.displayClientMessage(Component.translatable(refusal, nameOf(npc)), true);
			return;
		}
		Player partner = npc.talkPartner();
		if (partner != null && partner != player) {
			player.displayClientMessage(Component.translatable(ModLang.TALK_OCCUPIED, nameOf(npc)), true);
			return;
		}
		if (npc.isMeditating()) {
			npc.setMeditating(false);
			boolean onDuty = npc.getLifecycle() == CultivatorNpc.Lifecycle.SECT && npc.getTitle().isElderOrAbove();
			if (!onDuty) npc.changeRegard(player, Regard.DISTURBED);
		}
		npc.beginTalk(player);
		Standing standing = standing(player, npc);
		ModPackets.sendTalkOpen(player, npc, npc.regard(player), intents(standing));
	}

	/**
	 * The cultivator {@code player} is talking with, if it is the entity they name, still in reach and still willing. A talk
	 * that lapsed while the player read (nobody else having taken it up) picks up again.
	 */
	private static CultivatorNpc partner(ServerPlayer player, int entityId) {
		Entity entity = player.level().getEntity(entityId);
		if (!(entity instanceof CultivatorNpc npc) || !npc.isAlive() || player.isSpectator()) return null;
		if (npc.distanceToSqr(player) > KEEP * KEEP || refusal(player, npc) != null) {
			npc.endTalk(player);
			return null;
		}
		Player other = npc.talkPartner();
		if (other != null && other != player) return null;
		return npc;
	}

	/** The player closed the talk. */
	public static void end(ServerPlayer player, int entityId) {
		if (player.level().getEntity(entityId) instanceof CultivatorNpc npc) npc.endTalk(player);
	}

	// --- Intents ---

	/** The player chose an intent on the wheel (or a tab of the dialogue box). */
	public static void intent(ServerPlayer player, int entityId, int index) {
		CultivatorNpc npc = partner(player, entityId);
		TalkIntent intent = TalkIntent.byIndex(index);
		if (npc == null || intent == null) return;
		Standing standing = standing(player, npc);
		if ((intents(standing) & intent.bit()) == 0) return;
		npc.beginTalk(player);
		switch (intent) {
			case GREET -> greet(player, npc, standing);
			case ASK -> ModPackets.sendTalkDialogue(player, npc, npc.regard(player), intent, speech(line("ask", npc, standing)),
					replies(npc, standing));
			case GIFT -> gift(player, npc);
			case PROVOKE -> provoke(player, npc);
		}
		if (intent.isQuick()) npc.endTalk(player);
	}

	private static void greet(ServerPlayer player, CultivatorNpc npc, Standing standing) {
		Regard.Entry entry = npc.regardEntry(player);
		long today = Regard.day(npc.level().getGameTime());
		int delta = 0;
		if (entry.greetDay != today) {
			entry.greetDay = today;
			delta = Regard.GREET;
			npc.changeRegard(player, delta);
		}
		say(player, npc, line("greet", npc, standing), delta);
	}

	/** What a gift is worth to a cultivator of the first realm; a stronger one makes less of it. Zero: no use to them. */
	public static double worth(ItemStack stack) {
		if (stack.getItem() instanceof GinsengItem) {
			double w = Math.max(1, GinsengItem.age(stack) / 40.0);
			if (stack.is(ModItems.SPIRIT_GINSENG)) w *= 2;
			else if (stack.is(ModItems.PURPLE_LINGZHI) || stack.is(ModItems.OCHRE_HUANGJING)) w *= 1.5;
			return w;
		}
		if (stack.getItem() instanceof PillItem) {
			double w = PillItem.grade(stack).getMultiplier() * 4;
			if (stack.is(ModItems.FOUNDATION_PILL)) w += 6;
			else if (stack.is(ModItems.CORE_PILL)) w += 10;
			return w;
		}
		if (stack.getItem() instanceof CultivationFruitItem) return Math.max(2, CultivationFruitItem.age(stack) / 30.0);
		return 0;
	}

	/** How much regard {@code worth} buys from a cultivator of {@code realm} (mortals: as the first realm). */
	public static int giftRegard(double worth, int realm, int repeats) {
		double scaled = worth * 6.0 / (6.0 + 3.0 * Math.max(0, realm)) / (1 + repeats);
		return Mth.clamp((int) Math.round(scaled), 1, Regard.GIFT_MAX);
	}

	private static void gift(ServerPlayer player, CultivatorNpc npc) {
		ItemStack stack = player.getMainHandItem();
		if (stack.isEmpty()) {
			player.displayClientMessage(Component.translatable(ModLang.TALK_GIFT_EMPTY), true);
			return;
		}
		double worth = worth(stack);
		if (worth <= 0) {
			say(player, npc, line("gift.refuse", npc), 0);
			return;
		}
		Regard.Entry entry = npc.regardEntry(player);
		long today = Regard.day(npc.level().getGameTime());
		String item = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		if (entry.giftDay == today && entry.giftItem.equals(item)) {
			entry.giftCount++;
		} else {
			entry.giftDay = today;
			entry.giftItem = item;
			entry.giftCount = 0;
		}
		int realm = npc.isMortal() ? 0 : CultivatorNpc.realmOf(npc.getRank()).ordinal();
		int delta = giftRegard(worth, realm, entry.giftCount);
		if (!player.getAbilities().instabuild) stack.shrink(1);
		npc.changeRegard(player, delta);
		say(player, npc, line(delta <= 3 ? "gift.small" : delta <= 10 ? "gift.good" : "gift.great", npc), delta);
	}

	/** The chance an insult starts a fight: likelier on the demonic path, and from someone stronger than you; never from the weaker. */
	public static float provokeChance(Alignment.Faction faction, int theirRank, int yourRank) {
		if (theirRank < yourRank - Stage.values().length) return 0; // a realm or more below you: they swallow it
		float chance = switch (faction) {
			case RIGHTEOUS -> 0.15f;
			case NEUTRAL -> 0.35f;
			case DEMONIC -> 0.6f;
		};
		if (theirRank > yourRank) chance += 0.3f;
		return Mth.clamp(chance, 0, 1);
	}

	private static void provoke(ServerPlayer player, CultivatorNpc npc) {
		npc.changeRegard(player, Regard.PROVOKE);
		boolean attack = npc.getRandom().nextFloat() < provokeChance(npc.getFaction(), CultivatorNpc.rankOf(npc), CultivatorNpc.rankOf(player));
		say(player, npc, line(attack ? "provoke.attack" : "provoke.scorn", npc), Regard.PROVOKE);
		if (attack) {
			npc.endTalk(player);
			npc.provokedBy(player); // the player started it: killing them is still murder
			npc.setTarget(player);
		}
	}

	// --- Asking ---

	/** The topics on offer: who they are, their sect and its people (members only; its people not to someone far beneath), the sects nearby. */
	private static List<Reply> replies(CultivatorNpc npc, Standing standing) {
		List<Reply> replies = new ArrayList<>();
		replies.add(topic(Topic.SELF));
		if (npc.isSectMember() && npc.sect() != null) {
			replies.add(topic(Topic.SECT));
			if (standing != Standing.FAR_ABOVE && standing != Standing.MORTAL) replies.add(topic(Topic.PEOPLE));
		}
		replies.add(topic(Topic.NEARBY));
		return replies;
	}

	private static Reply topic(Topic topic) {
		return new Reply(topic.ordinal(), Component.translatable(ModLang.talkTopic(topic.getId())), true, null);
	}

	/** The player picked a topic in the dialogue box. */
	public static void topic(ServerPlayer player, int entityId, int topicId) {
		CultivatorNpc npc = partner(player, entityId);
		Topic topic = Topic.byIndex(topicId);
		if (npc == null || topic == null) return;
		Standing standing = standing(player, npc);
		List<Reply> replies = replies(npc, standing);
		if (replies.stream().noneMatch(r -> r.id() == topic.ordinal())) return;
		npc.beginTalk(player);
		Component answer = switch (topic) {
			case SELF -> aboutSelf(npc);
			case SECT -> aboutSect(npc);
			case PEOPLE -> aboutPeople(npc);
			case NEARBY -> aboutNearby(npc);
		};
		ModPackets.sendTalkDialogue(player, npc, npc.regard(player), TalkIntent.ASK, speech(answer), replies);
	}

	private static Component aboutSelf(CultivatorNpc npc) {
		Component name = Component.literal(npc.getPersonalName());
		if (npc.isMortal()) return Component.translatable(ModLang.talkLine("self.commoner"), name);
		Component realm = PlayerCultivation.rankName(npc.getDisplayedRealm(), npc.getDisplayedStage());
		Sect sect = npc.sect();
		String path = npc.getFaction().getId();
		if (sect != null) return Component.translatable(ModLang.talkLine("self.sect", path), name, npc.getTitle().getDisplayName(), sect.name, realm);
		return Component.translatable(ModLang.talkLine("self.rogue", path), name, realm);
	}

	private static Component aboutSect(CultivatorNpc npc) {
		Sect sect = npc.sect();
		if (sect == null) return aboutSelf(npc);
		Component path = Component.translatable(ModLang.talkPath(Alignment.factionOf(sect.alignment).getId()));
		Sect.Member master = sect.master();
		int count = sect.members.size();
		if (master == null) return Component.translatable(ModLang.talkLine("sect.no_master"), sect.name, count, path);
		if (master.id.equals(npc.getUUID())) return Component.translatable(ModLang.talkLine("sect.self_master"), sect.name, count, path);
		return Component.translatable(ModLang.talkLine("sect"), sect.name, count, path, NpcNames.of(master.id), rankName(master.rank));
	}

	private static Component aboutPeople(CultivatorNpc npc) {
		Sect sect = npc.sect();
		if (sect == null) return aboutSelf(npc);
		List<Component> elders = new ArrayList<>();
		Sect.Member master = null;
		for (Sect.Member member : sect.members.values()) {
			if (member.title == NpcTitle.SECT_MASTER) master = member;
			if ((member.title == NpcTitle.GRAND_ELDER || member.title == NpcTitle.ELDER) && !member.id.equals(npc.getUUID())) {
				elders.add(Component.translatable(ModLang.TALK_TITLED, member.title.getDisplayName(), NpcNames.of(member.id)));
			}
		}
		boolean iAmMaster = master != null && master.id.equals(npc.getUUID());
		if (iAmMaster) {
			return elders.isEmpty() ? Component.translatable(ModLang.talkLine("people.alone"))
					: Component.translatable(ModLang.talkLine("people.master_self"), list(elders));
		}
		if (master == null) {
			return elders.isEmpty() ? Component.translatable(ModLang.talkLine("people.alone")) : Component.translatable(ModLang.talkLine("people.no_master"), list(elders));
		}
		Component masterName = Component.literal(NpcNames.of(master.id));
		return elders.isEmpty() ? Component.translatable(ModLang.talkLine("people.no_elders"), masterName)
				: Component.translatable(ModLang.talkLine("people"), list(elders), masterName);
	}

	/** The nearest other living sect they would know of: its name, path, which way and how far. */
	private static Component aboutNearby(CultivatorNpc npc) {
		if (!(npc.level() instanceof ServerLevel level)) return Component.empty();
		Sect best = null;
		double bestDistance = KNOWN_SECTS * KNOWN_SECTS;
		for (Sect sect : SectData.get(level.getServer()).all()) {
			if (sect.extinct || sect.dimension != level.dimension() || sect.id.equals(npc.getSectId())) continue;
			double d = npc.blockPosition().distSqr(sect.core);
			if (d < bestDistance) {
				best = sect;
				bestDistance = d;
			}
		}
		if (best == null) return Component.translatable(ModLang.talkLine("nearby.none"));
		Alignment.Faction faction = Alignment.factionOf(best.alignment);
		int distance = Math.max(100, (int) Math.round(Math.sqrt(bestDistance) / 100.0) * 100);
		Component direction = Component.translatable(ModLang.talkDirection(direction(npc.blockPosition(), best.core)));
		Component path = Component.translatable(ModLang.talkPath(faction.getId()));
		String kind = npc.getFaction().isEnemyOf(faction) ? "nearby.enemy" : "nearby";
		return Component.translatable(ModLang.talkLine(kind), best.name, path, direction, distance);
	}

	/** The eight-point compass direction from {@code from} to {@code to}. */
	public static String direction(BlockPos from, BlockPos to) {
		double dx = to.getX() - from.getX(), dz = to.getZ() - from.getZ();
		// Minecraft's north is -z; angle 0 = north, turning clockwise through east.
		double angle = Math.toDegrees(Math.atan2(dx, -dz));
		int sector = Math.floorMod((int) Math.round(angle / 45.0), 8);
		return new String[] {"north", "north_east", "east", "south_east", "south", "south_west", "west", "north_west"}[sector];
	}

	private static Component rankName(int rank) {
		return rank < 0 ? Component.translatable(ModLang.TALK_MORTAL) : PlayerCultivation.rankName(CultivatorNpc.realmOf(rank), CultivatorNpc.stageOf(rank));
	}

	/** "A, B and C". */
	private static Component list(List<Component> parts) {
		if (parts.size() == 1) return parts.get(0);
		MutableComponent head = Component.empty();
		for (int i = 0; i < parts.size() - 1; i++) {
			if (i > 0) head.append(Component.translatable(ModLang.TALK_LIST));
			head.append(parts.get(i));
		}
		return Component.translatable(ModLang.TALK_AND, head, parts.get(parts.size() - 1));
	}

	// --- Lines ---

	/** The key of a line of {@code kind} for this cultivator at this standing: a mortal's own, else by path and standing. */
	private static Component line(String kind, CultivatorNpc npc, Standing standing) {
		if (npc.isMortal()) return Component.translatable(ModLang.talkLine(kind + ".commoner" + (standing == Standing.PEER ? ".mortal" : "")));
		return Component.translatable(ModLang.talkLine(kind, npc.getFaction().getId(), standing.getId()));
	}

	/** A line by path alone (gifts, insults); a mortal speaks for itself. */
	private static Component line(String kind, CultivatorNpc npc) {
		if (npc.isMortal()) return Component.translatable(ModLang.talkLine(kind + ".commoner"));
		return Component.translatable(ModLang.talkLine(kind, npc.getFaction().getId()));
	}

	/** Their words, in quotes. */
	private static Component speech(Component words) {
		return Component.literal("“").append(words).append("”");
	}

	/** Says a line above the hotbar: "Mo Lian: "..."", with what it did to their regard. */
	private static void say(ServerPlayer player, CultivatorNpc npc, Component words, int regardDelta) {
		MutableComponent message = Component.translatable(ModLang.TALK_SAYS, nameOf(npc), speech(words));
		if (regardDelta != 0) {
			String signed = (regardDelta > 0 ? "+" : "") + regardDelta;
			message.append(Component.translatable(ModLang.TALK_REGARD_DELTA, signed)
					.withStyle(regardDelta > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
		}
		player.displayClientMessage(message, true);
	}

	/** Their name, in their title's colour. */
	public static MutableComponent nameOf(CultivatorNpc npc) {
		return Component.literal(npc.getPersonalName()).withStyle(style -> style.withColor(npc.getTitle().getColor()));
	}

	private TalkSystem() {}
}
