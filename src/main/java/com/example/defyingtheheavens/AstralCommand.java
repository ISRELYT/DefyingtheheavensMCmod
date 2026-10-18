package com.example.defyingtheheavens;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Locale;

/**
 * The Astral Realm's commands: /astral where (anyone: the planet one is on, or the nearest star and planet in space); op-only
 * helpers: /astral visit <kind> (into space beside the nearest planet of that kind, on its lit side), /astral planet <index>
 * (beside that planet), /astral vestige <rank> (a Vestige of the Void of that cultivation rank, 12 Nascent Soul Early to 19
 * Heavenly Being Grand Perfection, near you in space).
 */
public final class AstralCommand {
	private static final SuggestionProvider<CommandSourceStack> KINDS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(Arrays.stream(AstralPlanets.Kind.values()).map(AstralPlanets.Kind::getId), builder);

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
				dispatcher.register(Commands.literal("astral")
						.then(Commands.literal("where").executes(ctx -> where(ctx.getSource().getPlayerOrException())))
						.then(Commands.literal("visit").requires(src -> src.hasPermission(2))
								.then(Commands.argument("kind", StringArgumentType.word()).suggests(KINDS)
										.executes(ctx -> visit(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "kind")))))
						.then(Commands.literal("planet").requires(src -> src.hasPermission(2))
								.then(Commands.argument("index", IntegerArgumentType.integer(-1, AstralPlanets.PLANETS - 1))
										.executes(ctx -> toPlanet(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "index")))))
						.then(Commands.literal("vestige").requires(src -> src.hasPermission(2))
								.then(Commands.argument("rank", IntegerArgumentType.integer(VoidVestige.MIN_RANK, VoidVestige.MAX_RANK))
										.executes(ctx -> vestige(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "rank")))))));
	}

	private static int where(ServerPlayer p) {
		AstralPlanets planets = AstralPlanets.get();
		if (planets == null) return 0;
		if (ModDimensions.isPlanetSurface(p.level().dimension())) {
			AstralPlanets.Planet planet = planets.planetAt(p.getX(), p.getZ());
			if (planet == null) {
				p.sendSystemMessage(Component.literal("Between the planets' lands."));
				return 1;
			}
			double dx = p.getX() - planet.landX(), dz = p.getZ() - planet.landZ();
			p.sendSystemMessage(Component.literal(String.format(Locale.ROOT, "On %s (#%d), a %s world, qi x%.2f, at %.1f° latitude, %.1f° longitude.",
					planet.name(), planet.index(), planet.kind().getId(), planet.qi(), Math.toDegrees(PlanetTerrain.lat(dz)), Math.toDegrees(PlanetTerrain.lon(dx)))));
			return 1;
		}
		if (!ModDimensions.isSpace(p.level().dimension())) {
			p.sendSystemMessage(Component.literal("Not in the Astral Realm."));
			return 1;
		}
		AstralPlanets.Star star = planets.nearestStar(p.position());
		AstralPlanets.Planet nearest = null;
		double best = Double.MAX_VALUE;
		for (AstralPlanets.Planet planet : planets.planets()) {
			double d = planet.pos().distanceTo(p.position()) - planet.radius();
			if (d < best) {
				best = d;
				nearest = planet;
			}
		}
		double home = planets.overworld().pos().distanceTo(p.position());
		p.sendSystemMessage(Component.literal(String.format(Locale.ROOT, "Nearest star: %s, %.0f blocks. Nearest planet: %s (a %s world), %.0f blocks. The Overworld: %.0f blocks.",
				star.name(), star.pos().distanceTo(p.position()) - star.radius(), nearest == null ? "-" : nearest.name(),
				nearest == null ? "-" : nearest.kind().getId(), best, home)));
		return 1;
	}

	private static int visit(ServerPlayer p, String kindId) {
		AstralPlanets planets = AstralPlanets.get();
		if (planets == null) return 0;
		AstralPlanets.Planet best = null;
		double bestDistance = Double.MAX_VALUE;
		Vec3 from = ModDimensions.isSpace(p.level().dimension()) ? p.position() : AstralPlanets.OVERWORLD_POS;
		for (AstralPlanets.Planet planet : planets.planets()) {
			if (!planet.kind().getId().equals(kindId)) continue;
			double d = planet.pos().distanceTo(from);
			if (d < bestDistance) {
				bestDistance = d;
				best = planet;
			}
		}
		if (best == null) {
			p.sendSystemMessage(Component.literal("No planet of that kind."));
			return 0;
		}
		return toPlanet(p, best.index());
	}

	private static int toPlanet(ServerPlayer p, int index) {
		AstralPlanets planets = AstralPlanets.get();
		ServerLevel space = p.server.getLevel(ModDimensions.ASTRAL_VOID);
		AstralPlanets.Planet planet = planets == null ? null : planets.planet(index);
		if (planet == null || space == null) return 0;
		AstralPlanets.Star star = planets.star(planet.system());
		Vec3 sun = star.pos().subtract(planet.pos()).normalize();
		Vec3 at = planet.pos().add(sun.scale(planet.radius() * 3));
		// A large planet near its star has less room than that on its lit side: just outside its air instead, not in the heat.
		if (planets.starNear(at, AstralRealm.STAR_HEAT) != null) at = planet.pos().add(sun.scale(planet.atmosphere() + 60));
		Vec3 look = planet.pos().subtract(at);
		float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z)), pitch = (float) -Math.toDegrees(Math.atan2(look.y, look.horizontalDistance()));
		p.teleportTo(space, at.x, at.y, at.z, yaw, pitch);
		p.sendSystemMessage(Component.literal(String.format(Locale.ROOT, "Beside %s (#%d), a %s world, qi x%.2f%s.", planet.name(), planet.index(),
				planet.kind().getId(), planet.qi(), planet.parent() >= 0 ? ", a moon" : "")));
		return 1;
	}

	private static int vestige(ServerPlayer p, int rank) {
		if (!ModDimensions.isSpace(p.level().dimension())) {
			p.sendSystemMessage(Component.literal("Only in open space."));
			return 0;
		}
		VoidVestige vestige = ModEntities.VOID_VESTIGE.create(p.serverLevel());
		if (vestige == null) return 0;
		vestige.setRank(rank);
		Vec3 at = p.position().add(p.getLookAngle().scale(60));
		vestige.moveTo(at.x, at.y, at.z, p.getYRot() + 180, 0);
		p.serverLevel().addFreshEntity(vestige);
		return 1;
	}

	private AstralCommand() {}
}
