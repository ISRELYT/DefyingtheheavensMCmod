package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Messenger bees (Puppetry; the design's messenger: "speaks the message to another player"). Sent with a message through the Command Seal
 * (PuppetCommands), a bee rises and is gone: out of the world while it flies (kept in the puppets' ledger, Puppetry.Ledger#flights, so a
 * flight outlasts the server's rest), for as long as the way takes it at its grade's pace ({@link #PACE}; a Heaven or Immortal bee steps
 * through space at once). It comes down beside its addressee, speaks the message in its puppeteer's words, lingers {@link #LINGER} ticks and
 * is gone again, back to its puppeteer the same way; an addressee gone from the world turns it back with its message undelivered. A bee
 * never flies between worlds; one coming back to a puppeteer who has left waits until they are back.
 */
public final class PuppetCouriers {
	/** Ticks a bee lingers by its addressee, speaking. */
	public static final int LINGER = 60;
	/** Blocks a tick a bee flies on an errand, by its frame's grade (Heaven and Immortal: none, it steps there). */
	private static final double[] PACE = {0.6, 0.9, 1.2, 0, 0};
	/** The longest an errand's way takes (ten minutes), and the least (two seconds). */
	private static final long LONGEST = 20 * 600, SHORTEST = 40;

	/** Sends {@code bee} from {@code from} to {@code to} with {@code text}: what to tell its sender. */
	static Component send(PuppetEntity bee, ServerPlayer from, ServerPlayer to, String text) {
		if (to.level() != bee.level()) return Component.translatable(ModLang.COURIER_OTHER_WORLD, to.getName()).withStyle(ChatFormatting.GOLD);
		long travel = travel(bee, bee.distanceTo(to));
		CompoundTag flight = new CompoundTag();
		flight.putUUID("Owner", from.getUUID());
		flight.putUUID("Id", bee.getUUID());
		flight.putUUID("To", to.getUUID());
		flight.putString("ToName", to.getGameProfile().getName());
		flight.putString("From", from.getGameProfile().getName());
		flight.putString("Text", text);
		flight.putLong("Travel", travel);
		depart(bee, flight, 0, travel);
		return Component.translatable(ModLang.COURIER_SENT, bee.getDisplayName(), to.getName(), Math.max(1, travel / 20));
	}

	/** The ticks {@code bee}'s way of {@code distance} blocks takes. */
	private static long travel(PuppetEntity bee, double distance) {
		double pace = PACE[bee.frameGrade().ordinal()];
		if (pace <= 0) return 1;
		return Math.max(SHORTEST, Math.min(LONGEST, (long) Math.ceil(distance / pace)));
	}

	/** {@code bee} rises and is gone on leg {@code leg} of its errand (0 there, 1 back), due in {@code ticks}. */
	static void depart(PuppetEntity bee, CompoundTag flight, int leg, long ticks) {
		if (!(bee.level() instanceof ServerLevel level)) return;
		CompoundTag body = new CompoundTag();
		bee.clearCourier();
		bee.saveAsPassenger(body);
		flight.put("Bee", body);
		flight.putInt("Leg", leg);
		flight.putLong("Due", level.getServer().overworld().getGameTime() + ticks);
		Puppetry.Ledger ledger = Puppetry.Ledger.get(level.getServer());
		ledger.flights.add(flight);
		ledger.setDirty();
		if (bee.getOwnerUUID() != null) ledger.markAway(bee.getOwnerUUID(), bee.getUUID(), true);
		level.sendParticles(ParticleTypes.CLOUD, bee.getX(), bee.getY(0.5), bee.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
		level.playSound(null, bee.blockPosition(), SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.NEUTRAL, 0.6f, 1.6f);
		bee.discard();
	}

	/** Every second: the bees whose way is done come down where they were going. */
	static void tick(MinecraftServer server) {
		Puppetry.Ledger ledger = Puppetry.Ledger.get(server);
		if (ledger.flights.isEmpty()) return;
		long now = server.overworld().getGameTime();
		for (CompoundTag flight : new ArrayList<>(ledger.flights)) {
			if (flight.getLong("Due") > now) continue;
			ledger.flights.remove(flight);
			ledger.setDirty();
			if (flight.getInt("Leg") == 0) arrive(server, flight);
			else home(server, flight);
		}
	}

	/** Come to its addressee: it speaks its message, lingers, and goes back; or, with them gone, turns back at once. */
	private static void arrive(MinecraftServer server, CompoundTag flight) {
		ServerPlayer to = server.getPlayerList().getPlayer(flight.getUUID("To"));
		long travel = flight.getLong("Travel");
		if (to == null) {
			flight.putBoolean("Undelivered", true);
			flight.putInt("Leg", 1);
			flight.putLong("Due", server.overworld().getGameTime() + travel);
			Puppetry.Ledger.get(server).flights.add(flight);
			return;
		}
		PuppetEntity bee = land(to, flight);
		if (bee == null) return;
		to.sendSystemMessage(Component.translatable(ModLang.COURIER_MESSAGE, flight.getString("From"), flight.getString("Text"))
				.withStyle(ChatFormatting.LIGHT_PURPLE));
		to.serverLevel().playSound(null, to.blockPosition(), SoundEvents.BEE_POLLINATE, SoundSource.NEUTRAL, 1.0f, 1.2f);
		ServerPlayer owner = server.getPlayerList().getPlayer(flight.getUUID("Owner"));
		if (owner != null) {
			owner.displayClientMessage(Component.translatable(ModLang.COURIER_DELIVERED, bee.getDisplayName(), to.getName())
					.withStyle(ChatFormatting.LIGHT_PURPLE), false);
		}
		flight.remove("Bee");
		bee.lingerAsCourier(flight);
	}

	/** Back with its puppeteer (or, with them gone, waiting a while longer). */
	private static void home(MinecraftServer server, CompoundTag flight) {
		ServerPlayer owner = server.getPlayerList().getPlayer(flight.getUUID("Owner"));
		if (owner == null) {
			flight.putLong("Due", server.overworld().getGameTime() + 200);
			Puppetry.Ledger.get(server).flights.add(flight);
			return;
		}
		PuppetEntity bee = land(owner, flight);
		if (bee == null) return;
		owner.displayClientMessage(Component.translatable(flight.getBoolean("Undelivered") ? ModLang.COURIER_BACK_UNDELIVERED : ModLang.COURIER_BACK,
				bee.getDisplayName(), flight.getString("ToName")).withStyle(ChatFormatting.LIGHT_PURPLE), false);
	}

	/** The bee come down beside {@code beside}, as it was when it rose. */
	private static PuppetEntity land(ServerPlayer beside, CompoundTag flight) {
		ServerLevel level = beside.serverLevel();
		Vec3 look = beside.getLookAngle().multiply(1, 0, 1);
		Vec3 at = beside.position().add(look.lengthSqr() < 1.0e-4 ? new Vec3(1.5, 1.2, 0) : look.normalize().scale(1.8).add(0, 1.2, 0));
		Entity entity = EntityType.loadEntityRecursive(flight.getCompound("Bee"), level, e -> {
			e.moveTo(at.x, at.y, at.z, beside.getYRot() + 180, 0);
			return e;
		});
		if (!(entity instanceof PuppetEntity bee)) return null;
		if (level.getEntity(bee.getUUID()) != null) {
			UUID old = bee.getUUID();
			bee.setUUID(UUID.randomUUID());
			if (bee.getOwnerUUID() != null) Puppetry.Ledger.get(level.getServer()).remove(bee.getOwnerUUID(), old);
		}
		level.addFreshEntity(bee);
		level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 8, 0.2, 0.2, 0.2, 0.05);
		bee.remember();
		return bee;
	}

	private PuppetCouriers() {}
}
