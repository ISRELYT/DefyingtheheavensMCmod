package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/**
 * The aura a Cultivation Fruit gives off while it still hangs on the tree. Each tier has its own colour and its own
 * way of moving, so they read as different treasures rather than one effect scaled up:
 * <ul>
 * <li>100+, jade: breathes slowly in and out.</li>
 * <li>500+, aqua: still, like water, with a soft ripple spreading now and then and glyphs swirling in.</li>
 * <li>1,000+, white with a blue tinge: a steady, crisp glow that glints like ice catching the light; a frost-white
 * ring of qi and a glyph swirl.</li>
 * <li>5,000+, white-gold: radiant. Short beams of light burst out like the Ender Dragon's death rays, each beam
 * breathing on its own; counter-rotating golden rings and a two-armed glyph swirl.</li>
 * <li>10,000 (the cap): a heartbeat. Everything (halo, beams, pillar, ripple, chime) answers a slow double beat.</li>
 * </ul>
 * This class does the client-side particles and sounds (from the block's client ticker) and the timing curves the
 * renderer shares (client/CultivationFruitAuraRenderer draws the glow). Every fruit runs on its own clock offset, so
 * fruit hanging side by side never pulse in unison.
 */
public final class FruitAura {
    public enum Tier { NONE, HUNDRED_YEAR, FIVE_HUNDRED_YEAR, THOUSAND_YEAR, HEAVENLY, TEN_THOUSAND_YEAR }

    /** Height of the fruit's centre within its block (it hangs from the leaves above; body spans y 6.5..14.5 px). */
    public static final float CENTER_Y = 10.5f / 16;
    /** Height of a fruit's centre above a Spirit Pedestal's block: floating a little over the pedestal's top (20 px, nubs 21). */
    public static final float PEDESTAL_FRUIT_Y = 1.675f;
    /**
     * How far the aura is pushed out to clear the fruit's skin. The aura was tuned on a fruit of about 1.25 px radius;
     * the peach is 4 px, so halos, ripples and motes start this much further out to stay visible around it.
     */
    public static final float SKIN = (4.0f - 1.25f) / 16;
    /** Jade's slow breath. */
    public static final int BREATH_TICKS = 70;
    /** How often a ripple spreads from an aqua fruit. */
    public static final int RIPPLE_TICKS = 70;
    /** Window in which a frost fruit may glint once. */
    public static final int GLINT_TICKS = 50;
    /** One "lub-dub" of the 10,000-year heartbeat; the second beat lands {@link #SECOND_BEAT} ticks after the first. */
    public static final int HEARTBEAT_TICKS = 60;
    public static final int SECOND_BEAT = 9;

    /** Aura colours, shared with the renderer so particles and glow match. */
    public static final float[] JADE = {0.45f, 1.0f, 0.7f};
    public static final float[] AQUA = {0.35f, 0.95f, 1.0f};
    public static final float[] FROST = {0.82f, 0.92f, 1.0f};
    public static final float[] WHITE_GOLD = {1.0f, 0.92f, 0.65f};
    public static final float[] GOLD = {1.0f, 0.8f, 0.35f};

    private static final DustParticleOptions JADE_MOTE = dust(JADE, 0.6f);
    private static final DustParticleOptions AQUA_MOTE = dust(AQUA, 0.6f);
    private static final DustParticleOptions FROST_MOTE = dust(FROST, 0.7f);
    private static final DustParticleOptions WHITE_GOLD_MOTE = dust(WHITE_GOLD, 0.9f);
    private static final DustParticleOptions GOLD_MOTE = dust(GOLD, 0.8f);

    public static Tier of(int years) {
        if (years >= FruitAge.MAX_YEARS) return Tier.TEN_THOUSAND_YEAR;
        if (years >= FruitAge.HEAVENLY_TIER) return Tier.HEAVENLY;
        if (years >= FruitAge.THOUSAND_YEAR_TIER) return Tier.THOUSAND_YEAR;
        if (years >= FruitAge.FIVE_HUNDRED_YEAR_TIER) return Tier.FIVE_HUNDRED_YEAR;
        if (years >= FruitAge.HUNDRED_YEAR_TIER) return Tier.HUNDRED_YEAR;
        return Tier.NONE;
    }

    /** How far a heavenly treasure has grown toward the cap: 0 at 5,000 years, 1 at 10,000. */
    public static float heavenlyStrength(int years) {
        return Mth.clamp((years - FruitAge.HEAVENLY_TIER) / (float) (FruitAge.MAX_YEARS - FruitAge.HEAVENLY_TIER), 0.0f, 1.0f);
    }

    // ---- Timing, shared with the renderer. "clock" is the fruit's own clock (see clock()). ----

    /**
     * The fruit's own clock: game time plus a fixed 0..1023-tick offset per position, so neighbouring fruit keep
     * different time. Wrapped (at a multiple of every cycle length) to keep it small enough that float animation stays
     * smooth in old worlds.
     */
    public static long clock(long gameTime, BlockPos pos) {
        long h = pos.asLong() * 0x9E3779B97F4A7C15L;
        return (gameTime + ((h >>> 40) & 1023L)) % CLOCK_WRAP;
    }

    /** A multiple of every cycle above (70, 50 and 60 ticks, and 4 heartbeats for the chime): about 84 minutes. */
    private static final long CLOCK_WRAP = 100_800L;

    /** 0..1 position within a repeating cycle of {@code period} ticks. */
    public static float phase(float clock, int period) {
        return (clock % period) / period;
    }

    /** A smooth 0..1..0 swell over {@code period} ticks: 0 at the start of each cycle, 1 halfway. */
    public static float swell(float clock, int period) {
        return 0.5f - 0.5f * Mth.cos(phase(clock, period) * (float) (Math.PI * 2));
    }

    /**
     * The 10,000-year heartbeat, 0..1: a quick rise and slow fall on the first beat, then a softer second beat
     * {@link #SECOND_BEAT} ticks later, then stillness until the next.
     */
    public static float heartbeat(float clock) {
        float t = clock % HEARTBEAT_TICKS;
        return Math.max(beat(t), 0.6f * beat(t - SECOND_BEAT));
    }

    private static float beat(float sinceBeat) {
        if (sinceBeat < 0) return 0.0f;
        if (sinceBeat < 2) return sinceBeat / 2.0f;
        return (float) Math.exp(-(sinceBeat - 2) / 5.0);
    }

    /**
     * Frost's glint, 0..1: in most {@link #GLINT_TICKS} windows the fruit flashes once, briefly, at a moment that
     * differs from window to window (and fruit to fruit), like ice catching the light.
     */
    public static float glint(float clock) {
        long window = (long) Math.floor(clock / GLINT_TICKS);
        long h = (window + 1) * 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        if ((h & 3) == 0) return 0.0f; // a quarter of the windows stay quiet
        float at = 6 + ((h >>> 8) & 0xFFFF) % (GLINT_TICKS - 12);
        float distance = Math.abs(clock - window * GLINT_TICKS - at);
        float flash = Math.max(0.0f, 1.0f - distance / 3.0f);
        return flash * flash;
    }

    // ---- Particles and sounds ----

    /** The colour that stands for a tier: its aura's colour, and the qi it gives a meditator (pale jade below 100 years). */
    public static float[] colour(Tier tier) {
        return switch (tier) {
            case NONE -> PALE_JADE;
            case HUNDRED_YEAR -> JADE;
            case FIVE_HUNDRED_YEAR -> AQUA;
            case THOUSAND_YEAR -> FROST;
            case HEAVENLY -> WHITE_GOLD;
            case TEN_THOUSAND_YEAR -> GOLD;
        };
    }

    /** Young fruit's qi: no aura of its own, but a meditator still draws a faint, pale stream from it. */
    public static final float[] PALE_JADE = {0.7f, 0.95f, 0.8f};

    /** Client ticker for attached fruit. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, CultivationFruitBlockEntity fruit) {
        emit(level, pos, fruit.age(), pos.getX() + 0.5, pos.getY() + CENTER_Y, pos.getZ() + 0.5);
    }

    /** Client ticker for a Spirit Pedestal: the fruit on display gives off the same aura as on the tree. */
    public static void pedestalClientTick(Level level, BlockPos pos, BlockState state, SpiritPedestalBlockEntity pedestal) {
        if (!pedestal.hasFruit()) return;
        emit(level, pos, pedestal.fruitAge(), pos.getX() + 0.5, pos.getY() + PEDESTAL_FRUIT_Y, pos.getZ() + 0.5);
    }

    /** The aura's particles and sounds for a fruit of {@code years} centred on (x, y, z); {@code pos} keys its clock. */
    private static void emit(Level level, BlockPos pos, int years, double x, double y, double z) {
        Tier tier = of(years);
        if (tier == Tier.NONE) return;
        RandomSource random = level.random;
        long clock = clock(level.getGameTime(), pos);
        switch (tier) {
            case HUNDRED_YEAR -> {
                if (random.nextInt(12) == 0) mote(level, random, JADE_MOTE, x, y, z, 0.4);
            }
            case FIVE_HUNDRED_YEAR -> {
                if (random.nextInt(12) == 0) mote(level, random, AQUA_MOTE, x, y, z, 0.4);
                if (clock % 4 == 0) glyphSwirl(level, x, y, z, clock, 1, 0.9);
            }
            case THOUSAND_YEAR -> {
                if (clock % 3 == 0) glyphSwirl(level, x, y, z, clock, 1, 1.1);
                if (clock % 3 == 1) ring(level, FROST_MOTE, x, y, z, 0.6, clock * 0.09, 0.0);
                if (random.nextInt(30) == 0) wisp(level, random, x, y, z, 0.04);
            }
            case HEAVENLY -> {
                if (clock % 2 == 0) {
                    glyphSwirl(level, x, y, z, clock, 2, 1.3);
                    ring(level, WHITE_GOLD_MOTE, x, y, z, 0.8, clock * 0.11, 0.0);
                } else {
                    ring(level, GOLD_MOTE, x, y, z, 1.15, -clock * 0.07, 0.0);
                }
                if (random.nextInt(8) == 0) mote(level, random, WHITE_GOLD_MOTE, x, y, z, 1.0);
                if (random.nextInt(14) == 0) wisp(level, random, x, y, z, 0.06);
            }
            case TEN_THOUSAND_YEAR -> {
                glyphSwirl(level, x, y, z, clock, 3, 1.5);
                // A horizontal ring and one tilted across it, turning opposite ways: an armillary of qi.
                ring(level, WHITE_GOLD_MOTE, x, y, z, 0.85, clock * 0.12, 0.0);
                if (clock % 2 == 0) ring(level, GOLD_MOTE, x, y, z, 1.25, -clock * 0.08, Math.toRadians(35));
                // A slow fall of light around the fruit.
                if (clock % 4 == 0) {
                    double angle = random.nextDouble() * Math.PI * 2, radius = 0.4 + random.nextDouble() * 1.4;
                    level.addParticle(ParticleTypes.END_ROD, x + Math.cos(angle) * radius, y + 1.6 + random.nextDouble() * 0.6,
                            z + Math.sin(angle) * radius, 0, -0.025, 0);
                }
                // On each first beat the fruit breathes out a shell of light.
                long beatTick = clock % HEARTBEAT_TICKS;
                if (beatTick == 0) burst(level, random, WHITE_GOLD_MOTE, x, y, z, 8, 0.5);
                // Every fourth heartbeat, a two-note chime on the "lub" and the "dub".
                long phrase = clock % (HEARTBEAT_TICKS * 4L);
                if (phrase == 0) chime(level, x, y, z, 1.4f, 0.6f);
                else if (phrase == SECOND_BEAT) chime(level, x, y, z, 1.1f, 0.9f);
            }
            default -> {}
        }
    }

    private static DustParticleOptions dust(float[] colour, float scale) {
        return new DustParticleOptions(new Vector3f(colour[0], colour[1], colour[2]), scale);
    }

    /** A mote hovering somewhere near the fruit. */
    private static void mote(Level level, RandomSource random, ParticleOptions particle, double x, double y, double z, double spread) {
        double dx = random.nextGaussian() * spread * 0.5, dy = random.nextGaussian() * spread * 0.4, dz = random.nextGaussian() * spread * 0.5;
        // Keep motes outside the fruit: anything that lands inside is pushed out to just beyond the skin.
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz), min = 0.25 + 0.05;
        if (d < min) {
            double scale = d < 1.0e-3 ? 0 : min / d;
            dx *= scale; dy *= scale; dz *= scale;
            if (scale == 0) dy = min;
        }
        level.addParticle(particle, x + dx, y + dy, z + dz, 0, 0.01, 0);
    }

    /** A handful of motes on a small sphere around the fruit, released together. */
    private static void burst(Level level, RandomSource random, ParticleOptions particle, double x, double y, double z, int count, double radius) {
        for (int i = 0; i < count; i++) {
            double angle = (i + random.nextDouble() * 0.5) * Math.PI * 2 / count;
            level.addParticle(particle, x + Math.cos(angle) * radius, y + (random.nextDouble() - 0.5) * radius,
                    z + Math.sin(angle) * radius, 0, 0, 0);
        }
    }

    /**
     * One point of a ring around the fruit; spawning along a moving angle traces a circling band. {@code tilt} leans
     * the ring's plane about the X axis (0 is horizontal).
     */
    private static void ring(Level level, ParticleOptions particle, double x, double y, double z, double radius, double angle, double tilt) {
        double across = Math.sin(angle) * radius;
        level.addParticle(particle, x + Math.cos(angle) * radius, y + across * Math.sin(tilt) + Math.sin(angle * 0.5) * 0.08,
                z + across * Math.cos(tilt), 0, 0, 0);
    }

    /**
     * Enchanting glyphs spiralling in: each arm releases a glyph from a point circling the fruit, and every glyph arcs
     * inward to it, so the arms sweep round like a whirlpool. Enchant particles fly from (origin + offset) to the
     * origin and drop 1.2 blocks at the end of their flight, so the origin sits 1.2 above the fruit.
     */
    private static void glyphSwirl(Level level, double x, double y, double z, long clock, int arms, double radius) {
        for (int arm = 0; arm < arms; arm++) {
            double angle = clock * 0.15 + arm * Math.PI * 2 / arms;
            double rise = 0.25 * Math.sin(clock * 0.05 + arm * 2.1);
            level.addParticle(ParticleTypes.ENCHANT, x, y + 1.2, z, Math.cos(angle) * radius, rise - 1.2, Math.sin(angle) * radius);
        }
    }

    /** A wisp of light lifting off the fruit. */
    private static void wisp(Level level, RandomSource random, double x, double y, double z, double speed) {
        level.addParticle(ParticleTypes.END_ROD, x + random.nextGaussian() * 0.15, y + 0.3, z + random.nextGaussian() * 0.15, 0, speed, 0);
    }

    private static void chime(Level level, double x, double y, double z, float volume, float pitch) {
        level.playLocalSound(x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, volume, pitch, false);
    }

    private FruitAura() {}
}
