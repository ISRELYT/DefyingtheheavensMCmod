package com.example.defyingtheheavens;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * The body each player is born with (Physique), server side: rolled the first time they join ({@link #onJoin}), told in chat
 * and written in their journal; a Jade Poison Body's waiting pill qi refined as soon as it fits ({@link #tick}); the gifts that
 * live in no other system (a Nine Yang Body's fire, a Jade Poison Body's poison, a Blood Demon Body's healing, a Soul-Devouring
 * Body's kills, a Primordial Chaos Body's spells, the righteous's unease at a Heaven-Defying or Soul-Devouring Body); and an op's
 * /physique get|set|reroll [player].
 */
public final class Physiques {
	/** A Blood Demon Body heals by this share of the health its blows take. */
	public static final float BLOOD_SHARE = 0.15f;
	/** A Jade Poison Body's blow poisons for this long (ticks): Poison I, II from Core Formation, III from Heavenly Being. */
	public static final int POISON_TICKS = 5 * 20;
	/** A Soul-Devouring Body takes this share of what the stage of a cultivator it slays takes. */
	public static final double DEVOUR_SHARE = 0.1;
	/** Each righteous or neutral cultivator a Soul-Devouring Body slays drags its path this far toward the devilish. */
	public static final int DEVOUR_SHIFT = 10;
	/** What the righteous think the less of a Heaven-Defying Body, and of a Soul-Devouring Body. */
	public static final int HEAVEN_DEFYING_REGARD = -15, SOUL_DEVOURING_REGARD = -20;
	/** A Primordial Chaos Body's spells strike this many times as hard. */
	public static final float CHAOS_SPELLS = 1.25f;

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
				dispatcher.register(Commands.literal("physique")
						.requires(src -> src.hasPermission(2))
						.then(Commands.literal("get")
								.executes(ctx -> get(ctx.getSource(), ctx.getSource().getPlayerOrException()))
								.then(Commands.argument("player", EntityArgument.player())
										.executes(ctx -> get(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
						.then(Commands.literal("set")
								.then(Commands.argument("id", StringArgumentType.word()).suggests(IDS)
										.executes(ctx -> set(ctx.getSource(), ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "id")))
										.then(Commands.argument("player", EntityArgument.player())
												.executes(ctx -> set(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
														StringArgumentType.getString(ctx, "id"))))))
						.then(Commands.literal("reroll")
								.executes(ctx -> reroll(ctx.getSource(), ctx.getSource().getPlayerOrException()))
								.then(Commands.argument("player", EntityArgument.player())
										.executes(ctx -> reroll(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))));
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> allowDamage(entity, source));
		ServerLivingEntityEvents.AFTER_DEATH.register(Physiques::onDeath);
	}

	private static final SuggestionProvider<CommandSourceStack> IDS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(Arrays.stream(Physique.values()).map(Physique::getId), builder);

	/** Whether joining rolls a physique: the game tests turn it off, so their mock players are born plain (Five Element Spiritual Roots, no limit). */
	public static boolean rollOnJoin = true;

	/** On joining: one born before physiques were (or never yet joined) is given one now, and told. */
	public static void onJoin(ServerPlayer player) {
		if (Possession.isHeld(player.getUUID())) return; // back while their body is held: their own record already has one
		PlayerCultivation c = CultivationManager.get(player);
		if (c.getPhysique() != null || !rollOnJoin) return;
		Physique physique = Physique.roll(player.getRandom());
		// One already cultivating (a world from before physiques) can't have been born with meridians that never held qi.
		while (physique == Physique.SHATTERED_MERIDIANS && !c.isMortal()) physique = Physique.roll(player.getRandom());
		give(player, physique);
	}

	/**
	 * {@code physique} becomes {@code player}'s (a Myriad Form Body's forms rolled with it), its roots drawn (the elements of methods
	 * known first), and they are told.
	 */
	public static void give(ServerPlayer player, Physique physique) {
		give(player, physique, physique == Physique.MYRIAD_FORM_BODY ? Physique.rollForms(player.getRandom()) : EnumSet.noneOf(Physique.class));
	}

	/** {@code physique} with these {@code forms} (a Myriad Form Body's; none for any other) becomes {@code player}'s, and they are told. */
	public static void give(ServerPlayer player, Physique physique, Collection<Physique> forms) {
		PlayerCultivation c = CultivationManager.get(player);
		c.setPhysique(physique);
		c.setForms(physique == Physique.MYRIAD_FORM_BODY ? forms : EnumSet.noneOf(Physique.class));
		Set<QiElement> known = EnumSet.noneOf(QiElement.class);
		for (HeavenMethod method : c.heaven().known()) known.add(method.element());
		c.setRootElements(Physique.rollRoots(c.rootCount(), known, player.getRandom()));
		if (!c.hasShatteredMeridians()) c.setBodyRefining(0);
		CultivationManager.refresh(player); // the qi pool, the body (and the client told)
		tell(player, c);
	}

	private static void tell(ServerPlayer player, PlayerCultivation c) {
		Physique physique = c.physique();
		// The physique's lore is in the journal (below), not the chat.
		player.sendSystemMessage(Component.translatable(ModLang.MSG_PHYSIQUE_BORN, physique.getDisplayName(), physique.grade().getDisplayName())
				.withStyle(ChatFormatting.GOLD));
		if (!c.getForms().isEmpty()) {
			player.sendSystemMessage(Component.translatable(ModLang.MSG_PHYSIQUE_FORMS, formsLine(c.getForms())).withStyle(ChatFormatting.GOLD));
		}
		if (!c.getRootElements().isEmpty()) {
			player.sendSystemMessage(Component.translatable(ModLang.MSG_PHYSIQUE_ROOTS, rootsLine(c.getRootElements())).withStyle(ChatFormatting.GOLD));
		}
		if (physique.grade().ordinal() >= Physique.Grade.SUPREME.ordinal()) {
			player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.7f, 1.0f);
		}
		MutableComponent text = Component.translatable(ModLang.JOURNAL_PHYSIQUE_TEXT, physique.getDisplayName(), physique.grade().getDisplayName(),
				physique.getDescription());
		for (Physique form : c.getForms()) {
			text.append(" ").append(Component.translatable(ModLang.JOURNAL_PHYSIQUE_FORM, form.getDisplayName(), form.getDescription()));
		}
		if (!c.getRootElements().isEmpty()) text.append(" ").append(Component.translatable(ModLang.MSG_PHYSIQUE_ROOTS, rootsLine(c.getRootElements())));
		Journal.note(player, "physique", ModLang.JOURNAL_PHYSIQUE, text);
	}

	/** "Fire, Water, Wood": the roots' elements, each in its colour. */
	public static Component rootsLine(Collection<QiElement> roots) {
		MutableComponent line = Component.empty();
		boolean first = true;
		for (QiElement element : roots) {
			if (!first) line.append(", ");
			line.append(element.getDisplayName());
			first = false;
		}
		return line;
	}

	/** "Nine Yang Body, Blood Demon Body": a Myriad Form Body's forms, each in its grade's colour. */
	public static Component formsLine(Collection<Physique> forms) {
		MutableComponent line = Component.empty();
		boolean first = true;
		for (Physique form : forms) {
			if (!first) line.append(", ");
			line.append(form.getDisplayName());
			first = false;
		}
		return line;
	}

	/** Once a second: a Jade Poison Body's pill qi waiting behind a bottleneck refined the moment it fits; a Nine Yang Body's flames out. */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) return;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			PlayerCultivation c = CultivationManager.get(player);
			if (c.hasPhysique(Physique.JADE_POISON_BODY) && c.getMedicinalQi() > 0 && c.refineAtOnce()) CultivationManager.refresh(player);
			if (c.hasPhysique(Physique.NINE_YANG_BODY) && player.isOnFire()) player.clearFire();
		}
	}

	// --- The gifts that live here ---

	/** ALLOW_DAMAGE: fire and lava never burn a Nine Yang Body; what would have, goes out. */
	static boolean allowDamage(LivingEntity entity, DamageSource source) {
		if (!(entity instanceof ServerPlayer player) || !source.is(DamageTypeTags.IS_FIRE)) return true;
		if (!CultivationManager.get(player).hasPhysique(Physique.NINE_YANG_BODY)) return true;
		player.clearFire();
		return false;
	}

	/** Whether {@code effect} may take hold of {@code entity} (LivingEntity#canBeAffected): no poison nor wither of a Jade Poison Body. */
	public static boolean allowEffect(LivingEntity entity, MobEffectInstance effect) {
		if (!(entity instanceof ServerPlayer player)) return true;
		MobEffect kind = effect.getEffect();
		if (kind != MobEffects.POISON && kind != MobEffects.WITHER) return true;
		return !CultivationManager.get(player).hasPhysique(Physique.JADE_POISON_BODY);
	}

	/** {@code attacker}'s own blow lands on {@code target}: a Jade Poison Body's poisons it, the more strongly the higher its realm. */
	public static void onMeleeHit(ServerPlayer attacker, LivingEntity target) {
		RaceGifts.onMeleeHit(attacker, target); // a lizard's venom; a fox's Illusion broken
		PlayerCultivation c = CultivationManager.get(attacker);
		if (!c.hasPhysique(Physique.JADE_POISON_BODY)) return;
		int realm = Math.max(0, TwoPaths.higherRank(c, true)) / Stage.values().length;
		target.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS, Math.min(2, realm / 2)), attacker);
	}

	/** {@code victim} has lost {@code lost} health to a blow from {@code source}: a Blood Demon Body that dealt it heals by its share. */
	public static void onHurt(LivingEntity victim, DamageSource source, float lost) {
		if (lost > 0) RaceGifts.onHurt(victim); // a lizard's Shed Skin
		if (lost <= 0 || !(source.getEntity() instanceof ServerPlayer attacker) || attacker == victim || !attacker.isAlive()) return;
		if (CultivationManager.get(attacker).hasPhysique(Physique.BLOOD_DEMON_BODY)) attacker.heal(lost * BLOOD_SHARE);
	}

	/**
	 * What a blow from {@code source} comes to before anything else makes of it: a Primordial Chaos Body's spells (the heaven methods'
	 * qi, the ghost path's harm, though not one of the dead's own blows: GhostSystem#isBlow) strike {@link #CHAOS_SPELLS} times as hard;
	 * and any blow or spell of one whose lifespan burns (BurnLifespan#outgoing), half again as hard.
	 */
	public static float outgoing(DamageSource source, float amount) {
		if (!(source.getEntity() instanceof ServerPlayer caster)) return amount;
		amount = BurnLifespan.outgoing(caster, amount);
		amount = RaceGifts.outgoing(caster, source, amount); // a tiger's blows the harder
		boolean spell = HeavenSpells.isQi(source) || source.is(ModDamageTypes.GHOSTLY) && !GhostSystem.isBlow();
		return spell && CultivationManager.get(caster).hasPhysique(Physique.PRIMORDIAL_CHAOS_BODY) ? amount * CHAOS_SPELLS : amount;
	}

	/** What a cultivator of {@code faction} thinks the less of one born as {@code c}: the righteous, of a Heaven-Defying or Soul-Devouring Body. */
	public static int regardShift(PlayerCultivation c, Alignment.Faction faction) {
		if (faction != Alignment.Faction.RIGHTEOUS) return 0;
		int shift = 0;
		if (c.hasPhysique(Physique.HEAVEN_DEFYING_BODY)) shift += HEAVEN_DEFYING_REGARD;
		if (c.hasPhysique(Physique.SOUL_DEVOURING_BODY)) shift += SOUL_DEVOURING_REGARD;
		return shift;
	}

	/**
	 * AFTER_DEATH: a cultivator slain (an NPC's, or a player's on either path) by a Soul-Devouring Body feeds the path it draws on,
	 * a tenth of what the victim's stage takes ({@link #devour}); a righteous or neutral one drags its path toward the devilish too.
	 */
	private static void onDeath(LivingEntity victim, DamageSource source) {
		ServerPlayer killer = source.getEntity() instanceof ServerPlayer p ? p : victim.getKillCredit() instanceof ServerPlayer p ? p : null;
		if (killer == null || killer == victim || !killer.isAlive()) return;
		PlayerCultivation c = CultivationManager.get(killer);
		if (!c.hasPhysique(Physique.SOUL_DEVOURING_BODY)) return;
		int rank, alignment;
		if (victim instanceof CultivatorNpc npc) {
			if (npc.isMortal()) return;
			rank = npc.getRank();
			alignment = npc.getAlignment();
		} else if (victim instanceof ServerPlayer slain) {
			PlayerCultivation v = CultivationManager.get(slain);
			rank = TwoPaths.higherRank(v, false);
			if (rank < 0) return; // a mortal: no cultivation to devour
			alignment = v.getAlignment();
			devoured(slain, v, killer);
		} else {
			return;
		}
		double gained = devour(killer, c, rank);
		if (gained > 0) {
			killer.sendSystemMessage(Component.translatable(ModLang.MSG_DEVOURED, victim.getDisplayName(), Math.round(gained))
					.withStyle(ChatFormatting.DARK_RED));
		}
		if (Alignment.factionOf(alignment) != Alignment.Faction.DEVILISH) {
			c.setAlignment(c.getAlignment() - DEVOUR_SHIFT);
			CultivationManager.markDirty(killer.server);
			CultivationManager.sync(killer);
			killer.sendSystemMessage(Component.translatable(ModLang.MSG_DEVOURED_DRAG, DEVOUR_SHIFT, Alignment.describe(c.getAlignment()))
					.withStyle(ChatFormatting.DARK_RED));
		}
	}

	/**
	 * A player slain by a Soul-Devouring Body loses a stage of its cultivation (the user's ask, 2026-10-20, so a friend killed again and
	 * again to feed one pays for each): on the higher of its paths, the one devoured (the ghost path's if it stands higher, else the
	 * heaven path's), its cultivation there lost with the methods' layers past it, as a failed tribulation's fall. Told so.
	 */
	static void devoured(ServerPlayer slain, PlayerCultivation v, ServerPlayer killer) {
		if (v.isMortal()) return;
		Component fallen;
		if (TwoPaths.ghostRank(v, false) > TwoPaths.heavenRank(v, false)) {
			GhostMethods.fallOneStage(v.netherworld());
			fallen = GhostMethods.rankName(TwoPaths.ghostRank(v, false));
		} else {
			v.fallOneStage();
			fallen = PlayerCultivation.rankName(v.getRealm(), v.getStage());
		}
		CultivationManager.markDirty(slain.server);
		slain.sendSystemMessage(Component.translatable(ModLang.MSG_DEVOURED_FALL, killer.getDisplayName(), fallen).withStyle(ChatFormatting.DARK_RED));
	}

	/**
	 * {@code killer} devours a cultivator of {@code victimRank}: {@link #DEVOUR_SHARE} of what that stage takes on the path the killer
	 * draws on (TwoPaths#cultivatesGhost: the ghost path's bar, or the heaven path's), as far as it fits before a bottleneck. A mortal's
	 * sealed meridians, or a ghost path not begun, take nothing. @return how much it was
	 */
	static double devour(ServerPlayer killer, PlayerCultivation c, int victimRank) {
		if (c.isMortal() || victimRank < 0) return 0;
		int stages = Stage.values().length;
		Realm realm = Realm.byIndex(Math.min(Realm.values().length - 1, victimRank / stages));
		Stage stage = Stage.byIndex(victimRank % stages);
		if (TwoPaths.cultivatesDemon(c)) { // the Demon Path drawn on (DemonPath): its bar
			double gained = DEVOUR_SHARE * DemonPath.required(victimRank);
			int before = DemonPath.rank(c);
			if (DemonPath.addCultivation(c, gained) > 0) DemonPath.afterRank(killer, before);
			CultivationManager.markDirty(killer.server);
			CultivationManager.sync(killer);
			return gained;
		}
		if (TwoPaths.cultivatesGhost(c)) {
			NetherworldState s = c.netherworld();
			if (GhostMethods.ghostRank(s) < 0) return 0;
			double gained = DEVOUR_SHARE * GhostMethods.required(victimRank);
			int before = GhostMethods.ghostRank(s);
			GhostMethods.addCultivation(s, gained);
			GhostMethods.afterRank(killer, before);
			CultivationManager.markDirty(killer.server);
			Spells.sync(killer); // the Cultivation tab's ghost bar
			return gained;
		}
		double gained = DEVOUR_SHARE * PlayerCultivation.required(realm, stage);
		if (c.addCultivation(gained)) {
			killer.displayClientMessage(Component.translatable(ModLang.MSG_STAGE_UP, c.getRealm().getDisplayName(), c.getStage().getDisplayName()), true);
		}
		CultivationManager.refresh(killer);
		return gained;
	}

	private static int get(CommandSourceStack source, ServerPlayer player) {
		PlayerCultivation c = CultivationManager.get(player);
		MutableComponent body = c.physique().getDisplayName().copy();
		if (!c.getForms().isEmpty()) body.append(" (").append(formsLine(c.getForms())).append(")");
		source.sendSuccess(() -> Component.translatable(ModLang.CMD_PHYSIQUE_GET, player.getDisplayName(), body), false);
		return 1;
	}

	private static int set(CommandSourceStack source, ServerPlayer player, String id) {
		Physique physique = Physique.byId(id);
		if (physique == null) {
			source.sendFailure(Component.translatable(ModLang.CMD_PHYSIQUE_UNKNOWN, id));
			return 0;
		}
		give(player, physique);
		source.sendSuccess(() -> Component.translatable(ModLang.CMD_PHYSIQUE_SET, player.getDisplayName(), physique.getDisplayName()), true);
		return 1;
	}

	private static int reroll(CommandSourceStack source, ServerPlayer player) {
		give(player, Physique.roll(player.getRandom()));
		Physique physique = CultivationManager.get(player).physique();
		source.sendSuccess(() -> Component.translatable(ModLang.CMD_PHYSIQUE_SET, player.getDisplayName(), physique.getDisplayName()), true);
		return 1;
	}

	private Physiques() {}
}
