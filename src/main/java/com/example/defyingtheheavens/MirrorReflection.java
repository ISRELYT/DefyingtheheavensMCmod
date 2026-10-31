package com.example.defyingtheheavens;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * A reflection out of the Jade Lake Mirage (MirageLakeWorks): one who stood before its great mirror, stepped out of it to be bested (its
 * trial), or one risen from the lake beside each visitor when the mirror wakes (its event, at {@link #HALF} their strength, gone after a
 * minute). A copy of them: their rank, their heaven method and so their spells (CultivatorSpells, through {@link #heavenMethod}), their
 * armour and their weapon; drawn in their own skin, glassy (the client's MirrorReflectionRenderer). It fights only them, only their blows
 * touch it, and it never flees, begs or kneels; brought down it shatters, leaving nothing. It lives no life of its own (no rogue's course,
 * no ledger: {@link #livesItsLife}), nor is it ever saved: their leaving the realm, falling or going far, it shatters too.
 */
public class MirrorReflection extends CultivatorNpc {
	private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(MirrorReflection.class, EntityDataSerializers.OPTIONAL_UUID);
	/** An event's reflection: half its owner's health and blows, its rank two stages under theirs. */
	public static final double HALF = 0.5;
	/** Its owner this far off (or out of the realm), it shatters. */
	static final double LEASH = 48;
	private static final UUID HALF_HEALTH = UUID.fromString("3a1e3a1e-0b5e-4d1e-9a1e-4a1f0a1f0001");
	private static final UUID HALF_HARM = UUID.fromString("3a1e3a1e-0b5e-4d1e-9a1e-4a1f0a1f0002");

	private HeavenMethod method;
	private boolean half;
	private long fadesAt = -1;

	public MirrorReflection(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(OWNER, Optional.empty());
	}

	/** Whose reflection it is (its skin on the client). */
	public UUID getOwnerId() { return entityData.get(OWNER).orElse(null); }

	/** Whether it is the mirror's trial's (whole) rather than one the waking mirror raised (half). */
	public boolean isTrial() { return !half; }

	/**
	 * {@code owner}'s reflection raised at {@code at} facing {@code yaw}: their rank as the realm lets them wield it, their practised heaven
	 * method, their armour and weapon (copies, never dropped); {@code half}, the waking mirror's, gone at {@code fadesAt}. Null if it
	 * couldn't be put down.
	 */
	static MirrorReflection raise(ServerLevel level, ServerPlayer owner, Vec3 at, float yaw, boolean half, long fadesAt) {
		MirrorReflection r = ModEntities.MIRROR_REFLECTION.create(level);
		if (r == null) return null;
		PlayerCultivation c = CultivationManager.get(owner);
		int rank = Math.max(0, TwoPaths.higherRank(c, true) - (half ? 2 : 0));
		r.becomeRogue(rank, 0, level.getRandom());
		r.entityData.set(OWNER, Optional.of(owner.getUUID()));
		r.method = HeavenMethods.practised(c.heaven());
		r.half = half;
		r.fadesAt = fadesAt;
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			// Never a legend's copy (its one bearer is kept by Legends: a second would be taken for a duplicate), nor what is in the off hand.
			net.minecraft.world.item.ItemStack own = owner.getItemBySlot(slot);
			boolean copied = slot != EquipmentSlot.OFFHAND && !(own.getItem() instanceof LegendaryWeaponItem);
			r.setItemSlot(slot, copied ? own.copy() : net.minecraft.world.item.ItemStack.EMPTY);
			r.setDropChance(slot, 0.0f);
		}
		r.setCustomName(Component.translatable(ModLang.NAME_REFLECTION, owner.getDisplayName()));
		r.moveTo(at.x, at.y, at.z, yaw, 0);
		r.setYHeadRot(yaw);
		if (half) {
			var health = r.getAttribute(Attributes.MAX_HEALTH);
			if (health != null) health.addPermanentModifier(new AttributeModifier(HALF_HEALTH, "reflection's half", -HALF, AttributeModifier.Operation.MULTIPLY_TOTAL));
			var harm = r.getAttribute(Attributes.ATTACK_DAMAGE);
			if (harm != null) harm.addPermanentModifier(new AttributeModifier(HALF_HARM, "reflection's half", -HALF, AttributeModifier.Operation.MULTIPLY_TOTAL));
		}
		r.setHealth(r.getMaxHealth());
		r.setTarget(owner);
		if (!level.addFreshEntity(r)) return null;
		level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1, at.z, 30, 0.4, 0.9, 0.4, 0.04);
		level.playSound(null, r.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.6f, 0.5f);
		return r;
	}

	/** Its owner's own method (so their spells), else whatever a rogue of its rank would have. */
	@Override
	public HeavenMethod heavenMethod() {
		return method != null ? method : super.heavenMethod();
	}

	@Override
	public boolean livesItsLife() { return false; }

	@Override
	public boolean shouldFlee() { return false; }

	@Override
	public boolean holdsGround() { return true; }

	@Override
	public void beg(Player player, int ticks) {}

	private ServerPlayer owner() {
		UUID id = getOwnerId();
		return id == null || getServer() == null ? null : getServer().getPlayerList().getPlayer(id);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide) {
			if (random.nextInt(4) == 0) level().addParticle(ParticleTypes.END_ROD, getRandomX(0.5), getRandomY(), getRandomZ(0.5), 0, 0.01, 0);
			return;
		}
		ServerPlayer owner = owner();
		if (owner == null || owner.level() != level() || !owner.isAlive() || owner.isSpectator() || distanceTo(owner) > LEASH
				|| fadesAt >= 0 && level().getGameTime() >= fadesAt) {
			shatter();
			return;
		}
		if (getTarget() != owner) setTarget(owner);
		if (isKneeling()) setKneeling(false);
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return target.getUUID().equals(getOwnerId()) && super.canAttack(target);
	}

	/** Only its owner's blows (and spells) touch it. */
	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (source.getEntity() == null || !source.getEntity().getUUID().equals(getOwnerId())) return false;
		return super.hurt(source, amount);
	}

	/** Brought down: it shatters, and the trial's is bested (MirageLakeWorks#reflectionBested). */
	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level) {
			breakUp(level);
			ServerPlayer owner = owner();
			if (owner != null && !half) MirageLakeWorks.reflectionBested(level, owner);
		}
	}

	/** Gone without being bested: into light, nothing left. */
	void shatter() {
		if (level() instanceof ServerLevel level) breakUp(level);
		discard();
	}

	private void breakUp(ServerLevel level) {
		level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 30, 0.4, 0.8, 0.4, 0.06);
		level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 10, 0.3, 0.6, 0.3, 0.02);
		level.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.4f, 0.7f);
	}

	@Override
	protected boolean shouldDropLoot() { return false; }

	@Override
	protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {}

	@Override
	public boolean removeWhenFarAway(double distance) { return false; }

	/** Never saved: unloaded with its land, it is gone. */
	@Override
	public boolean shouldBeSaved() { return false; }
}
