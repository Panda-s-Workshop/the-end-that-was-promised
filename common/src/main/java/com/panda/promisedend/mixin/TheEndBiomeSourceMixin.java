package com.panda.promisedend.mixin;

import com.panda.promisedend.worldgen.EndBiomes;
import com.panda.promisedend.worldgen.EndRegions;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Our End biomes: vanilla still decides the terrain class of each chunk (main island, highlands, midlands,
 * barrens, small islands); {@link EndRegions} swaps the biome by distance band and region. Vanilla terrain is unchanged.
 *
 * <p>Fabric API injects a cancellable callback at the same RETURN, and a callback that sets the return value returns on
 * the spot, so whichever handler is applied first wins. Priority 900 applies ours first: where we choose a biome,
 * Fabric's picker (which throws on a biome it wasn't told about) never runs; where we keep vanilla's, it runs as usual.
 * The vanilla biome is matched by key, not identity, in case another mod rebuilt the holder.
 */
@Mixin(value = TheEndBiomeSource.class, priority = 900)
abstract class TheEndBiomeSourceMixin {
    @Shadow @Final private Holder<Biome> end;
    @Shadow @Final private Holder<Biome> highlands;
    @Shadow @Final private Holder<Biome> midlands;
    @Shadow @Final private Holder<Biome> islands;
    @Shadow @Final private Holder<Biome> barrens;

    @Unique private EndBiomes promisedend$biomes;

    @Unique
    private EndBiomes promisedend$biomes() {
        if (promisedend$biomes == null) promisedend$biomes = EndBiomes.forSource(end);
        return promisedend$biomes;
    }

    @Inject(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;", at = @At("RETURN"), cancellable = true)
    private void promisedend$pick(int quartX, int quartY, int quartZ, Climate.Sampler sampler, CallbackInfoReturnable<Holder<Biome>> cir) {
        Holder<Biome> vanilla = cir.getReturnValue();
        EndRegions.Terrain terrain;
        if (vanilla.is(end)) terrain = EndRegions.Terrain.MAIN;
        else if (vanilla.is(highlands)) terrain = EndRegions.Terrain.HIGHLANDS;
        else if (vanilla.is(midlands)) terrain = EndRegions.Terrain.MIDLANDS;
        else if (vanilla.is(islands)) terrain = EndRegions.Terrain.ISLANDS;
        else if (vanilla.is(barrens)) terrain = EndRegions.Terrain.BARRENS;
        else return; // another mod already chose a biome of its own here
        Holder<Biome> ours = promisedend$biomes().pick(quartX, quartZ, terrain);
        if (ours != null) cir.setReturnValue(ours);
    }

    @Inject(method = "collectPossibleBiomes", at = @At("RETURN"), cancellable = true)
    private void promisedend$possible(CallbackInfoReturnable<Stream<Holder<Biome>>> cir) {
        cir.setReturnValue(Stream.concat(cir.getReturnValue(), promisedend$biomes().all()));
    }
}
