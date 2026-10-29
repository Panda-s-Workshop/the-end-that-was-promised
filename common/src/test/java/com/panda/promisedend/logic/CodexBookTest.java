package com.panda.promisedend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CodexBookTest {
    @Test
    void tabletsReadInOrder() {
        Set<Integer> read = new HashSet<>();
        for (int n = 1; n <= CodexBook.PAGES; n++) {
            assertEquals(n, CodexBook.nextPage(read::contains));
            read.add(n);
        }
        assertEquals(0, CodexBook.nextPage(read::contains));
    }

    @Test
    void aGapIsFilledFirst() {
        // pages a pack gave out of order, or took out: the first unread comes next
        assertEquals(2, CodexBook.nextPage(n -> n != 2));
        assertEquals(1, CodexBook.nextPage(n -> n > 5));
    }

    @Test
    void everyActHasAtMostSixGoals() {
        for (CodexBook.Act act : CodexBook.ACTS) assertTrue(act.goals().size() <= 6, act.key());
    }
}
