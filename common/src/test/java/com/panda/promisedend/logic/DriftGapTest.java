package com.panda.promisedend.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.IntBinaryOperator;
import org.junit.jupiter.api.Test;

class DriftGapTest {
    private static final int FLOOR = 0;
    private static final int[] EAST_FIRST = {0, 1, 2, 3};

    /** Two islands along x: land from x = 6 to 20 and from x = 20 + gap + 1 on, both at height 60, wide in z. */
    private static IntBinaryOperator twoIslands(int gap, int farHeight) {
        return (x, z) -> Math.abs(z) > 30 ? FLOOR : x <= 20 && x >= 6 ? 60 : x > 20 + gap && x <= 60 + gap ? farHeight : FLOOR;
    }

    @Test
    void findsTheGapAndTheEdges() {
        var gap = DriftGap.find(twoIslands(30, 60), FLOOR, 8, 0, EAST_FIRST);
        assertNotNull(gap);
        assertEquals(20, gap.ex());
        assertEquals(51, gap.fx());
        assertEquals(30, gap.width());
        assertEquals(60, gap.top());
        var route = gap.route();
        // the islet's box lies against each edge: its middle 2.5 past the near edge's block, 1.5 short of the far one's
        assertEquals(22.5, route.ax(), 1e-9);
        assertEquals(49.5, route.bx(), 1e-9);
        assertEquals(59, route.ay(), 1e-9);
        assertEquals(27, route.length(), 1e-9);
    }

    @Test
    void onlyTwelveToNinetySix() {
        assertNull(DriftGap.find(twoIslands(11, 60), FLOOR, 8, 0, EAST_FIRST));
        assertNotNull(DriftGap.find(twoIslands(12, 60), FLOOR, 8, 0, EAST_FIRST));
        assertNotNull(DriftGap.find(twoIslands(96, 60), FLOOR, 8, 0, EAST_FIRST));
        assertNull(DriftGap.find(twoIslands(97, 60), FLOOR, 8, 0, EAST_FIRST));
    }

    @Test
    void edgesWithinTheSlope() {
        assertNotNull(DriftGap.find(twoIslands(30, 61), FLOOR, 8, 0, EAST_FIRST));
        assertEquals(59, DriftGap.find(twoIslands(30, 59), FLOOR, 8, 0, EAST_FIRST).top());
        // a block in four across: 30 across, the far side up to 7 higher (the route climbs, flush with each edge at its
        // end); 8 higher is a wall
        var up = DriftGap.find(twoIslands(30, 67), FLOOR, 8, 0, EAST_FIRST);
        assertNotNull(up);
        assertEquals(59, up.route().ay(), 1e-9);
        assertEquals(66, up.route().by(), 1e-9);
        assertNull(DriftGap.find(twoIslands(30, 68), FLOOR, 8, 0, EAST_FIRST));
        // 12 across, a step of 3 at most; down as well as up
        assertNotNull(DriftGap.find(twoIslands(12, 63), FLOOR, 8, 0, EAST_FIRST));
        assertNull(DriftGap.find(twoIslands(12, 64), FLOOR, 8, 0, EAST_FIRST));
        assertNotNull(DriftGap.find(twoIslands(30, 53), FLOOR, 8, 0, EAST_FIRST));
        assertNull(DriftGap.find(twoIslands(30, 52), FLOOR, 8, 0, EAST_FIRST));
    }

    @Test
    void anIsletsRouteGivesBackItsGap() {
        for (var gap : new DriftGap.Gap[] {new DriftGap.Gap(20, 0, 51, 0, 1, 0, 60, 66), new DriftGap.Gap(-20, 4, -51, 4, -1, 0, 57, 57),
                new DriftGap.Gap(8, 8, 8, 40, 0, 1, 60, 58), new DriftGap.Gap(-3, -8, -3, -60, 0, -1, 44, 50)}) {
            assertEquals(gap, DriftGap.Gap.of(gap.route()));
        }
    }

    @Test
    void aSlopedRouteKeepsItsCorridor() {
        // 30 across rising 60 to 66: land 3 under the islet where it passes is fine, 2 under isn't, and the islet's top
        // over a column out there is the sloped one, not either edge's
        IntBinaryOperator base = twoIslands(30, 66);
        var gap = DriftGap.find(base, FLOOR, 8, 0, new int[] {0});
        assertNotNull(gap);
        int j = 15, low = gap.lowAt(j);
        assertTrue(low > 60 && low < 66, "the top over the middle " + low);
        IntBinaryOperator under = (x, z) -> x == 20 + j && z == 0 ? low - 3 : base.applyAsInt(x, z);
        IntBinaryOperator near = (x, z) -> x == 20 + j && z == 0 ? low - 2 : base.applyAsInt(x, z);
        assertNotNull(DriftGap.find(under, FLOOR, 8, 0, new int[] {0}));
        assertNull(DriftGap.find(near, FLOOR, 8, 0, new int[] {0}));
    }

    @Test
    void theCorridorIsClear() {
        // a spur of land into the gap 2 to the side, at the islet's height: no route; 3 to the side, or far below: fine
        IntBinaryOperator base = twoIslands(30, 60);
        IntBinaryOperator spur = (x, z) -> x == 35 && z == 2 ? 60 : base.applyAsInt(x, z);
        IntBinaryOperator wide = (x, z) -> x == 35 && z == 3 ? 60 : base.applyAsInt(x, z);
        IntBinaryOperator low = (x, z) -> x == 35 && z == 0 ? 40 : base.applyAsInt(x, z);
        assertNull(DriftGap.find(spur, FLOOR, 8, 0, new int[] {0}));
        assertNotNull(DriftGap.find(wide, FLOOR, 8, 0, new int[] {0}));
        assertNotNull(DriftGap.find(low, FLOOR, 8, 0, new int[] {0}));
        // land out in the gap only 2 under the islet's top: a route on it would run into this one; 3 under is fine
        IntBinaryOperator near = (x, z) -> x == 35 && z == 0 ? 58 : base.applyAsInt(x, z);
        IntBinaryOperator under = (x, z) -> x == 35 && z == 0 ? 57 : base.applyAsInt(x, z);
        assertNull(DriftGap.find(near, FLOOR, 8, 0, new int[] {0}));
        assertNotNull(DriftGap.find(under, FLOOR, 8, 0, new int[] {0}));
    }

    @Test
    void notFromTheVoidNorToAPillar() {
        assertNull(DriftGap.find(twoIslands(30, 60), FLOOR, 100, 0, EAST_FIRST));
        IntBinaryOperator base = twoIslands(30, 60);
        // the far side one column wide
        IntBinaryOperator pillar = (x, z) -> x == 51 ? 60 : x > 51 ? FLOOR : base.applyAsInt(x, z);
        assertNull(DriftGap.find(pillar, FLOOR, 8, 0, new int[] {0}));
    }

    @Test
    void docksWhereTheGroundKeepsLevel() {
        // the near island's rim tapers: 60 to x = 16, then 58, 55, 51, 46, then void; the edge is x = 16, the last at 60
        IntBinaryOperator base = twoIslands(30, 60);
        IntBinaryOperator taper = (x, z) -> x >= 17 && x <= 20 && Math.abs(z) <= 30 ? 60 - (x - 16) * (x - 15) / 2 - 1 : base.applyAsInt(x, z);
        var gap = DriftGap.find(taper, FLOOR, 8, 0, new int[] {0});
        assertNotNull(gap);
        assertEquals(16, gap.ex());
        assertEquals(51, gap.fx());
        // a rise on the way out, a stride (4) or wider: no route that way; a bump narrower is passed
        IntBinaryOperator rise = (x, z) -> x >= 10 && x <= 13 && Math.abs(z) <= 30 ? 63 : base.applyAsInt(x, z);
        assertNull(DriftGap.find(rise, FLOOR, 8, 0, new int[] {0}));
        IntBinaryOperator bump = (x, z) -> x == 10 && Math.abs(z) <= 30 ? 63 : base.applyAsInt(x, z);
        assertNotNull(DriftGap.find(bump, FLOOR, 8, 0, new int[] {0}));
    }

    @Test
    void everyWayAndOnlyNearTheEdge() {
        // the same islands turned: west from x = -8 when the gap is on the west (the far island is a cliff 10 higher
        // behind a strip 3 wide at its edge, so the search east from its side finds no route there to take it first)
        IntBinaryOperator west = (x, z) -> x <= -54 && x >= -90 && Math.abs(z) <= 30 ? 70 : twoIslands(30, 60).applyAsInt(-x, z);
        var gap = DriftGap.find(west, FLOOR, -8, 0, new int[] {2});
        assertNotNull(gap);
        assertEquals(-1, gap.dx());
        assertEquals(-21.5, gap.route().ax(), 1e-9);
        assertEquals(27, gap.route().length(), 1e-9);
        // a chunk middle more than 16 from the edge leaves it to the one nearer, so a line has one route
        IntBinaryOperator wide = (x, z) -> Math.abs(z) > 30 ? FLOOR : x <= 20 && x >= -40 ? 60 : x > 50 && x <= 90 ? 60 : FLOOR;
        assertNotNull(DriftGap.find(wide, FLOOR, 8, 0, new int[] {0}));
        assertNull(DriftGap.find(wide, FLOOR, -8, 0, new int[] {0}));
    }

    @Test
    void aGapIsFoundOnceFromEitherSide() {
        // islands 8-20 and 51-90 on x, gap 30: east from x 8 finds it; west from x 56 (a chunk middle) would find it
        // too, but east from 8 already does
        IntBinaryOperator both = (x, z) -> Math.abs(z) > 30 ? FLOOR : x >= 6 && x <= 20 || x >= 51 && x <= 90 ? 60 : FLOOR;
        assertNotNull(DriftGap.find(both, FLOOR, 8, 0, DriftGap.WAYS));
        var fromFar = DriftGap.find(both, FLOOR, 56, 0, new int[] {2});
        assertNull(fromFar, "found again from the far side: " + fromFar);
        // the near side rises before its edge, so east from 8 finds nothing: then west from the far side does
        IntBinaryOperator rough = (x, z) -> x == 12 && Math.abs(z) <= 30 ? 64 : both.applyAsInt(x, z);
        assertNull(DriftGap.find(rough, FLOOR, 8, 0, new int[] {0}));
        var west = DriftGap.find(rough, FLOOR, 56, 0, new int[] {2});
        assertNotNull(west);
        assertEquals(51, west.ex());
        assertEquals(20, west.fx());
        // every way from one place: east and south both
        IntBinaryOperator cross = (x, z) -> x >= 0 && x <= 20 && z >= 0 && z <= 20 || x >= 51 && x <= 90 && z >= 0 && z <= 20
                || z >= 51 && z <= 90 && x >= 0 && x <= 20 ? 60 : FLOOR;
        assertEquals(2, DriftGap.all(cross, FLOOR, 8, 8).size());
    }

    @Test
    void aGapSeenAColumnOffFromItsFarSideIsBuiltOnce() {
        // both edges a block lower than their islands: east from 8 (at 58) docks on 20 and lands on 51; west from 56 (at
        // 59) passes over 51 and 20 as too low, docking on 52 and landing on 19. The two would lie on each other, so
        // the one west isn't built
        IntBinaryOperator step = (x, z) -> Math.abs(z) > 30 ? FLOOR : x >= 6 && x <= 19 ? 58 : x == 20 || x == 51 ? 57 : x >= 52 && x <= 90 ? 59 : FLOOR;
        var east = DriftGap.find(step, FLOOR, 8, 0, new int[] {0});
        assertNotNull(east);
        assertEquals(51, east.fx());
        assertNull(DriftGap.find(step, FLOOR, 56, 0, new int[] {2}));
    }

    @Test
    void routesThatWouldMeetBuildOne() {
        // the middle is an island's corner: east and south both leave it, and their islets would lie on each other
        // there; the one south is built
        IntBinaryOperator corner = (x, z) -> x >= -10 && x <= 8 && z >= -10 && z <= 8 || x >= 30 && x <= 60 && z >= -10 && z <= 8
                || z >= 30 && z <= 60 && x >= -10 && x <= 8 ? 60 : FLOOR;
        assertNull(DriftGap.find(corner, FLOOR, 8, 8, new int[] {0}), "the one east is built");
        var built = DriftGap.all(corner, FLOOR, 8, 8);
        assertEquals(1, built.size(), "built " + built);
        assertEquals(1, built.getFirst().dz());
        // two routes are kept 4 apart in height where they pass near (a corridor and a post each reach 2 over a top),
        // and 3 apart across (a corridor is cleared a block past its islet's way, a post stands a block past it)
        var south = new DriftGap.Gap(8, 8, 8, 30, 0, 1, 60, 60);
        assertFalse(DriftGap.clash(new DriftGap.Gap(8, 8, 30, 8, 1, 0, 56, 56), south));
        assertTrue(DriftGap.clash(new DriftGap.Gap(8, 8, 30, 8, 1, 0, 57, 57), south));
        // side by side along x: the south one covers x 7-10; one 6 east of it covers 13-16, 3 apart; 5 east, 2 apart
        assertFalse(DriftGap.clash(new DriftGap.Gap(14, 8, 14, 30, 0, 1, 60, 60), south));
        assertTrue(DriftGap.clash(new DriftGap.Gap(13, 8, 13, 30, 0, 1, 60, 60), south));
    }

    @Test
    void aSouthRouteJustWestOfAnEastOnesSweepStillWins() {
        // the start is 3 short of the island's east edge and 2 short of its south one: the east route's sweep begins 4
        // east of the start, so a search for the south ones only 3 round that sweep missed the south route out of the
        // start itself, and both were built, their islets 2 apart (a Fabric server check, 2026-10-29)
        IntBinaryOperator corner = (x, z) -> x >= -10 && x <= 11 && z >= -10 && z <= 10 || x >= 30 && x <= 60 && z >= -10 && z <= 10
                || z >= 30 && z <= 60 && x >= -10 && x <= 11 ? 60 : FLOOR;
        var built = DriftGap.all(corner, FLOOR, 8, 8);
        assertEquals(1, built.size(), "built " + built);
        assertEquals(1, built.getFirst().dz());
    }
}
