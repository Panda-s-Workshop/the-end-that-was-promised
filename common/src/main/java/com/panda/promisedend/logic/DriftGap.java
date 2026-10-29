package com.panda.promisedend.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntBinaryOperator;
import org.jspecify.annotations.Nullable;

/**
 * Where a Drift Islet's route goes: from land, straight out along an axis while the ground keeps level, to where it
 * drops away, across 12-96 blocks of void (or of land far below) to ground at about that level (up or down a block in
 * four of the way across: the route slopes), with a corridor 5 wide clear of land at the islet's height all the way.
 * Heights are the ground's top (the first air above it), {@code floor} or below meaning void. Pure, so it is tested on
 * made-up ground.
 */
public final class DriftGap {
    /**
     * How far from where it starts the search looks for its island's edge: a chunk's width, so of the chunk middles
     * along a line only the one just inside the edge finds it, and a gap gets one route per line.
     */
    public static final int EDGE_REACH = 16;
    /** The ways a route is looked for: east, south, west, north. */
    public static final int[] WAYS = {0, 1, 2, 3};
    /** Half the corridor's width: the islet's 3 and a block each side. */
    public static final int CORRIDOR = 2;
    /** How far the ground may rise or fall on the way out to the edge, and the least rise allowed across. */
    public static final int STEP = 1;
    /** The steepest a route climbs or falls: a block in this many across. */
    public static final int SLOPE = 4;
    /** How far out from either edge an island's tapering rim may lie just under the islet. */
    public static final int RIM = 2;
    /** How far under the islet's top land out over the gap lies: an islet (1) and a rider (1.8) fit under it. */
    public static final int UNDER = 3;
    /** How far apart across two routes' islets keep (see {@link #clash}). */
    public static final int APART = 3;
    /** How far apart in height two routes keep where they pass near each other: a corridor and a mooring's post, each 2 over an islet's top, clear. */
    public static final int APART_HIGH = 4;
    /** How many columns of the way out to an island's edge the search passes at a time. */
    private static final int WALK = 4;
    /** How many columns of void the search for the far side passes at a time: an island it can land on is 3 across. */
    private static final int STRIDE = 3;

    /**
     * A route found: the last land column at each end ({@code e}, {@code f}), the way across ({@code dx}, {@code dz},
     * one of them 0) and the ground's top at each edge ({@code he}, {@code hf}), which the islet's top is flush with at
     * that end.
     */
    public record Gap(int ex, int ez, int fx, int fz, int dx, int dz, int he, int hf) {
        /** The void's width between the two edges. */
        public int width() {
            return Math.abs(fx - ex) + Math.abs(fz - ez) - 1;
        }

        /** The lower edge's top. */
        public int top() {
            return Math.min(he, hf);
        }

        /** The higher edge's top. */
        public int high() {
            return Math.max(he, hf);
        }

        /**
         * The islet's top with its middle {@code j} columns out from the near edge: flush with each edge while it lies
         * at that mooring (its middle 2.5 out), sloping evenly between.
         */
        public double topAt(double j) {
            double from = 2.5, to = width() + 1 - 2.5;
            if (to <= from) return he;
            double t = Math.max(0, Math.min(1, (j - from) / (to - from)));
            return he + (hf - he) * t;
        }

        /** The lowest the islet's top comes over column {@code j} (its middle within 2 of the column's, the islet 3 across). */
        public int lowAt(int j) {
            return (int) Math.floor(Math.min(topAt(j - 1.5), topAt(j + 2.5)));
        }

        /** The highest the islet's top comes over column {@code j}. */
        public int highAt(int j) {
            return (int) Math.ceil(Math.max(topAt(j - 1.5), topAt(j + 2.5)));
        }

        /** The ground the islet covers anywhere on its way: min x, max x, min z, max z. */
        public double[] sweep() {
            DriftRoute r = route();
            return new double[] {Math.min(r.ax(), r.bx()) - 1.5, Math.max(r.ax(), r.bx()) + 1.5, Math.min(r.az(), r.bz()) - 1.5, Math.max(r.az(), r.bz()) + 1.5};
        }

        /** The gap a route was made for (an islet keeps only its route). */
        public static Gap of(DriftRoute r) {
            double ax = r.bx() - r.ax(), az = r.bz() - r.az();
            int dx = Math.abs(ax) >= Math.abs(az) ? (int) Math.signum(ax) : 0, dz = dx == 0 ? (int) Math.signum(az) : 0;
            return new Gap((int) Math.floor(r.ax() - 2 * dx), (int) Math.floor(r.az() - 2 * dz), (int) Math.floor(r.bx() + 2 * dx),
                    (int) Math.floor(r.bz() + 2 * dz), dx, dz, (int) Math.round(r.ay()) + 1, (int) Math.round(r.by()) + 1);
        }

        /** The islet's route: its bottom middle beside each edge, its top flush with that edge. */
        public DriftRoute route() {
            return new DriftRoute(ex + 0.5 + 2 * dx, he - 1, ez + 0.5 + 2 * dz, fx + 0.5 - 2 * dx, hf - 1, fz + 0.5 - 2 * dz);
        }
    }

    private DriftGap() {}

    /** How much higher or lower than the near edge the far one may be, {@code width} across. */
    public static int rise(int width) {
        return Math.max(STEP, width / SLOPE);
    }

    /**
     * Every route from ({@code x}, {@code z}) that is built, one each way at most: of two routes whose islets would
     * meet, one gives way ({@link #givesWay}).
     */
    public static List<Gap> all(IntBinaryOperator height, int floor, int x, int z) {
        IntBinaryOperator h = remembered(height);
        List<Gap> gaps = new ArrayList<>();
        for (int way : WAYS) {
            Gap gap = find(h, floor, x, z, new int[] {way});
            if (gap != null) gaps.add(gap);
        }
        return gaps;
    }

    /** The first route built from ({@code x}, {@code z}) along the four ways in {@code order} (0 east, 1 south, 2 west, 3 north), or null. */
    public static @Nullable Gap find(IntBinaryOperator height, int floor, int x, int z, int[] order) {
        for (int way : order) {
            Gap gap = probe(height, floor, x, z, way == 0 ? 1 : way == 2 ? -1 : 0, way == 1 ? 1 : way == 3 ? -1 : 0);
            if (gap != null && !givesWay(height, floor, gap)) return gap;
        }
        return null;
    }

    /**
     * Whether two routes come too near each other: the ground their islets cover within {@link #APART} of each other, at
     * heights less than {@link #APART_HIGH} apart anywhere along them. Near enough, the islets would meet, or one route's
     * corridor (cleared a block past where its islet goes) would take the other's mooring: its dock, or a post standing
     * a block past where that islet goes, whichever chunk was made first.
     */
    public static boolean clash(Gap a, Gap b) {
        if (a.top() - b.high() >= APART_HIGH || b.top() - a.high() >= APART_HIGH) return false;
        double[] p = a.sweep(), q = b.sweep();
        return p[0] < q[1] + APART && q[0] < p[1] + APART && p[2] < q[3] + APART && q[2] < p[3] + APART;
    }

    /**
     * Whether this route gives way to another its islet would meet, found from a chunk middle near it. Every route runs
     * on a line through chunk middles, so each search can look for the others itself and both chunks come to the same
     * answer: one north or south beats one east or west; on the same line one east or south beats one west or north
     * (the same gap seen from its far side, an edge a column off), and of two going the same way (one from an island
     * lower down, under the other's way), the one whose edge is farther back. The other is looked for without its own
     * giving way, so a route sometimes gives way to one that isn't built: fewer routes, never two that meet.
     */
    private static boolean givesWay(IntBinaryOperator h, int floor, Gap gap) {
        int reach = EDGE_REACH + DriftRoute.LONGEST + 8;
        boolean alongX = gap.dx() != 0;
        int line = alongX ? gap.ez() : gap.ex();
        int from = (alongX ? Math.min(gap.ex(), gap.fx()) : Math.min(gap.ez(), gap.fz())) - reach;
        int to = (alongX ? Math.max(gap.ex(), gap.fx()) : Math.max(gap.ez(), gap.fz())) + reach;
        for (int m = middleFrom(from); m <= to; m += 16) {
            for (int s = -1; s <= 1; s += 2) {
                Gap other = alongX ? probe(h, floor, m, line, s, 0) : probe(h, floor, line, m, 0, s);
                if (other == null || other.equals(gap) || !clash(gap, other)) continue;
                boolean same = other.dx() == gap.dx() && other.dz() == gap.dz();
                if (!same && other.dx() + other.dz() > 0) return true;
                if (same && (other.ex() - gap.ex()) * gap.dx() + (other.ez() - gap.ez()) * gap.dz() < 0) return true;
            }
        }
        if (!alongX) return false;
        double[] sweep = gap.sweep();
        // a south route's line meets this one's sweep within APART of its own (its line, a half column, and its islet's
        // half): 2 more west of the sweep and 1 more east
        for (int lx = middleFrom((int) Math.floor(sweep[0]) - APART - 2); lx <= sweep[1] + APART + 1; lx += 16) {
            for (int mz = middleFrom(gap.ez() - reach); mz <= gap.ez() + reach; mz += 16) {
                for (int s = -1; s <= 1; s += 2) {
                    Gap other = probe(h, floor, lx, mz, 0, s);
                    if (other != null && clash(gap, other)) return true;
                }
            }
        }
        return false;
    }

    /** The first chunk middle (8 into a chunk, every 16) at or past {@code at}. */
    private static int middleFrom(int at) {
        return Math.floorDiv(at - 8 + 15, 16) * 16 + 8;
    }

    /** The heights, each column read once (the searches cross the same columns many times). */
    private static IntBinaryOperator remembered(IntBinaryOperator height) {
        Map<Long, Integer> seen = new HashMap<>();
        return (x, z) -> seen.computeIfAbsent(((long) x << 32) ^ (z & 0xFFFFFFFFL), k -> height.applyAsInt(x, z));
    }

    /** The route one way from a chunk middle on land, whether or not it gives way to another, or null. */
    private static @Nullable Gap probe(IntBinaryOperator h, int floor, int x, int z, int dx, int dz) {
        return h.applyAsInt(x, z) <= floor ? null : along(h, floor, x, z, dx, dz);
    }

    /** Whether the column at ({@code x}, {@code z}) is an island's edge: two more columns of land behind it, near its height. */
    private static boolean island(IntBinaryOperator h, int floor, int x, int z, int dx, int dz, int height) {
        for (int b = 1; b <= 2; b++) {
            int hb = h.applyAsInt(x + dx * b, z + dz * b);
            if (hb <= floor || Math.abs(hb - height) > 2) return false;
        }
        return true;
    }

    private static @Nullable Gap along(IntBinaryOperator h, int floor, int x, int z, int dx, int dz) {
        // the level the islet docks at: the ground where the search starts. Out along the way the ground keeps near it
        // (a rise above it ends the search); where it drops below it, the island's edge is behind (its rim tapers, so
        // the last column of land is lower than the island and a poor dock)
        // The way out is read a stride at a time, and column by column only in the stride where it drops: a notch or a
        // bump narrower than a stride on the way to the edge isn't an edge, and it is most of the search's reads
        int level = h.applyAsInt(x, z);
        int edge = -1;
        for (int k = WALK; k <= EDGE_REACH && edge < 0; k += WALK) {
            int hk = h.applyAsInt(x + dx * k, z + dz * k);
            if (hk > level + STEP) return null;
            if (hk >= level - STEP) continue;
            for (int i = k - WALK + 1; i <= k; i++) {
                int hi = i == k ? hk : h.applyAsInt(x + dx * i, z + dz * i);
                if (hi > level + STEP) return null;
                if (hi < level - STEP) {
                    edge = i - 1;
                    break;
                }
            }
        }
        if (edge < 0) return null;
        int ex = x + dx * edge, ez = z + dz * edge, he = h.applyAsInt(ex, ez);
        // the far side: an island (not a pillar's top) up near the edge's height, within the slope allowed that far
        // across; lower land (an island well under the route), and a pillar, are passed over (the corridor's rule
        // below says whether the islet clears them). Void is passed a stride at a time, and at the first land seen
        // the search steps back to the stride's start, so no column of an island 3 across is missed
        int far = -1, last = 0, end = DriftRoute.LONGEST + 1;
        // every column up to last is passed: void, or land too low to land on
        while (far < 0 && last < end) {
            int j = Math.min(last + STRIDE, end);
            if (h.applyAsInt(ex + dx * j, ez + dz * j) <= floor) {
                last = j;
                continue;
            }
            for (int i = last + 1; ; i++) {
                if (i > end) {
                    last = end;
                    break;
                }
                int hi = h.applyAsInt(ex + dx * i, ez + dz * i);
                if (hi > floor && hi >= he - rise(i - 1) && island(h, floor, ex + dx * i, ez + dz * i, dx, dz, hi)) {
                    far = i;
                    break;
                }
                if (i >= j && hi <= floor) {
                    last = i;
                    break;
                }
            }
        }
        if (far < 0 || far - 1 < DriftRoute.SHORTEST || far - 1 > DriftRoute.LONGEST) return null;
        int fx = ex + dx * far, fz = ez + dz * far, hf = h.applyAsInt(fx, fz);
        if (Math.abs(he - hf) > rise(far - 1)) return null;
        Gap gap = new Gap(ex, ez, fx, fz, dx, dz, he, hf);
        // the corridor, 2 to each side: within 2 of an edge, where an island's rim tapers away under the dock, no ground
        // up into the islet's block (a height is the first air, so ground of height t fills t - 1); out over the gap,
        // land passed over lies 3 or more below the islet's lowest top there, so a route on it (they are common) runs
        // with its rider's head under this islet
        int lx = dz != 0 ? 1 : 0, lz = dx != 0 ? 1 : 0;
        for (int j = 1; j < far; j++) {
            int highest = j <= RIM || far - j <= RIM ? gap.lowAt(j) - 1 : gap.lowAt(j) - UNDER;
            for (int l = -CORRIDOR; l <= CORRIDOR; l++) {
                int hc = h.applyAsInt(ex + dx * j + lx * l, ez + dz * j + lz * l);
                if (hc > floor && hc > highest) return null;
            }
        }
        return gap;
    }
}
