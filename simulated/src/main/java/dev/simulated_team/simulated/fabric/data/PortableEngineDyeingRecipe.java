package dev.simulated_team.simulated.fabric.data;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.codec.StreamCodec;
import com.zurrtum.create.AllItemTags;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlock;
import dev.simulated_team.simulated.fabric.FabricSimRecipeTypes;
import dev.simulated_team.simulated.index.SimBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public final class PortableEngineDyeingRecipe extends CustomRecipe {
    // PORT-NOTE(mc26.1): CustomRecipe lost its category constructor and CustomRecipe.Serializer; a stateless recipe
    // now serializes as a unit codec over a single instance.
    public static final PortableEngineDyeingRecipe INSTANCE = new PortableEngineDyeingRecipe();
    public static final RecipeSerializer<PortableEngineDyeingRecipe> SERIALIZER =
            new RecipeSerializer<>(MapCodec.unit(INSTANCE), StreamCodec.unit(INSTANCE));

    private PortableEngineDyeingRecipe() {
    }

    @Override
    public boolean matches(final CraftingInput input, final Level level) {
        int engines = 0;
        int dyes = 0;

        for (int slot = 0; slot < input.size(); slot++) {
            final ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (Block.byItem(stack.getItem()) instanceof PortableEngineBlock) {
                engines++;
            } else if (stack.is(AllItemTags.DYES)) {
                dyes++;
            } else {
                return false;
            }
            if (engines > 1 || dyes > 1) {
                return false;
            }
        }

        return engines == 1 && dyes == 1;
    }

    @Override
    public ItemStack assemble(final CraftingInput input) {
        ItemStack engine = ItemStack.EMPTY;
        DyeColor color = DyeColor.RED;

        for (int slot = 0; slot < input.size(); slot++) {
            final ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (Block.byItem(stack.getItem()) instanceof PortableEngineBlock) {
                engine = stack;
            } else {
                final DyeColor stackColor = AllItemTags.getDyeColor(stack);
                if (stackColor != null) {
                    color = stackColor;
                }
            }
        }

        final ItemStack dyedEngine = SimBlocks.PORTABLE_ENGINES.get(color).asItem().getDefaultInstance();
        if (!engine.getComponentsPatch().isEmpty()) {
            dyedEngine.applyComponents(engine.getComponentsPatch());
        }
        return dyedEngine;
    }

    @Override
    public RecipeSerializer<PortableEngineDyeingRecipe> getSerializer() {
        return FabricSimRecipeTypes.PORTABLE_ENGINE_DYEING;
    }
}
