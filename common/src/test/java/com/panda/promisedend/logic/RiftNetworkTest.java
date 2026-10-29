package com.panda.promisedend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RiftNetworkTest {
    private static RiftNetwork.Gate gate(long pos) {
        return new RiftNetwork.Gate("minecraft:the_end", pos, "Rift Gate", "promisedend:chorus_thicket");
    }

    @Test
    void addsOnceAndUpToTheCap() {
        List<RiftNetwork.Gate> gates = new ArrayList<>();
        assertEquals(RiftNetwork.Added.ADDED, RiftNetwork.add(gates, gate(1)));
        assertEquals(RiftNetwork.Added.KNOWN, RiftNetwork.add(gates, gate(1)));
        // the same place in another level is another gate
        assertEquals(RiftNetwork.Added.ADDED, RiftNetwork.add(gates, new RiftNetwork.Gate("minecraft:overworld", 1, "Rift Gate", "minecraft:plains")));
        for (long p = 2; gates.size() < RiftNetwork.MOST; p++) assertEquals(RiftNetwork.Added.ADDED, RiftNetwork.add(gates, gate(p)));
        assertEquals(RiftNetwork.MOST, gates.size());
        assertEquals(RiftNetwork.Added.FULL, RiftNetwork.add(gates, gate(999)));
        assertEquals(RiftNetwork.Added.KNOWN, RiftNetwork.add(gates, gate(1)));
        assertEquals(RiftNetwork.MOST, gates.size());
    }

    @Test
    void removesByPlace() {
        List<RiftNetwork.Gate> gates = new ArrayList<>(List.of(gate(1), gate(2)));
        assertTrue(RiftNetwork.remove(gates, "minecraft:the_end", 1));
        assertFalse(RiftNetwork.remove(gates, "minecraft:the_end", 1));
        assertFalse(RiftNetwork.remove(gates, "minecraft:overworld", 2));
        assertEquals(1, gates.size());
        assertNull(RiftNetwork.find(gates, "minecraft:the_end", 1));
    }

    @Test
    void renamesTrimmedAndCapped() {
        List<RiftNetwork.Gate> gates = new ArrayList<>(List.of(gate(1)));
        assertTrue(RiftNetwork.rename(gates, "minecraft:the_end", 1, "  Home  "));
        assertEquals("Home", RiftNetwork.find(gates, "minecraft:the_end", 1).name());
        assertFalse(RiftNetwork.rename(gates, "minecraft:the_end", 1, "   "));
        assertEquals("Home", RiftNetwork.find(gates, "minecraft:the_end", 1).name());
        assertFalse(RiftNetwork.rename(gates, "minecraft:the_end", 7, "Nowhere"));
        assertTrue(RiftNetwork.rename(gates, "minecraft:the_end", 1, "x".repeat(50)));
        assertEquals(RiftNetwork.NAME_MOST, RiftNetwork.find(gates, "minecraft:the_end", 1).name().length());
        assertEquals("promisedend:chorus_thicket", RiftNetwork.find(gates, "minecraft:the_end", 1).biome());
    }

    @Test
    void waitsThirtySeconds() {
        assertEquals(0, RiftNetwork.waitLeft(-1, 50));
        assertEquals(600, RiftNetwork.waitLeft(100, 100));
        assertEquals(1, RiftNetwork.waitLeft(100, 699));
        assertEquals(0, RiftNetwork.waitLeft(100, 700));
    }
}
