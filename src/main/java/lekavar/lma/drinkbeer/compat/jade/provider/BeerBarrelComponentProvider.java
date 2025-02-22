package lekavar.lma.drinkbeer.compat.jade.provider;

import lekavar.lma.drinkbeer.blockentities.BeerBarrelBlockEntity;
import lekavar.lma.drinkbeer.blocks.BeerBarrelBlock;
import lekavar.lma.drinkbeer.recipes.BrewingRecipe;
import lekavar.lma.drinkbeer.registries.RecipeRegistry;
import lekavar.lma.drinkbeer.utils.Convert;
import lekavar.lma.drinkbeer.utils.ItemStackHelper;
import mcp.mobius.waila.api.BlockAccessor;
import mcp.mobius.waila.api.IComponentProvider;
import mcp.mobius.waila.api.IServerDataProvider;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.config.IPluginConfig;
import mcp.mobius.waila.api.ui.IElement;
import mcp.mobius.waila.api.ui.IElementHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec2;
import org.lwjgl.system.CallbackI;
import snownee.jade.VanillaPlugin;

import java.util.ArrayList;
import java.util.List;

public class BeerBarrelComponentProvider implements IComponentProvider, IServerDataProvider<BlockEntity> {
    public static BeerBarrelComponentProvider INSTANCE = new BeerBarrelComponentProvider();
    public static final String KEY_TIME_REMAINING = "timeRemaining";

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig pluginConfig) {
        if(!(accessor.getBlock() instanceof BeerBarrelBlock &&
                accessor.getBlockEntity() instanceof BeerBarrelBlockEntity barrel))
            return;

        // item at INPUT_SIZE is beer mug, and INPUT_SIZE + 1 is the product
        ItemStack output = barrel.getItem(BrewingRecipe.INPUT_SIZE + 1);
        int timeRemaining = accessor.getServerData().getInt(KEY_TIME_REMAINING);

        if (!output.isEmpty()) {
            tooltip.remove(VanillaPlugin.INVENTORY); // remove Jade's default inventory tooltip

            Level level = accessor.getLevel();
            List<BrewingRecipe> recipes = level.getRecipeManager().getAllRecipesFor(RecipeRegistry.RECIPE_TYPE_BREWING.get());
            BrewingRecipe recipe = null;
            for (BrewingRecipe candidate : recipes) {
                if (ItemStackHelper.isSameItem(output, candidate.getResultItem()) &&
                        timeRemaining < candidate.getBrewingTime())
                {
                    recipe = candidate;
                    break;
                }
            }
            if (recipe == null) {
                return;
            }

            IElementHelper helper = tooltip.getElementHelper();

            List<IElement> elements = new ArrayList<>(5);

            NonNullList<Ingredient> ingredients = recipe.getIngredients();
            for (Ingredient ingredient : ingredients) {
                elements.add(helper.item(ingredient.getItems()[0]));
            }

            IElement[][] inputs = JadeHelper.layoutItems(elements, ingredients.size());

            List<List<IElement>> grids = JadeHelper.createRows(inputs.length);

            JadeHelper.placeItems(grids, inputs, true);
            JadeHelper.placeArrowProgress(grids, 1 - (float) timeRemaining / recipe.getBrewingTime());
            JadeHelper.placeScaledItem(helper, grids, output, 0.8F).translateDelta(-6, 0);

            if (timeRemaining > 0) {
                int inputGridsWidth = inputs[0].length;
                IElement sample = JadeHelper.createSpacerItem();

                tooltip.add(
                        List.of(
                                helper.text(new TextComponent(Convert.tickToTime(timeRemaining)))
                                        .size(new Vec2(0, 0))
                                        .translate(new Vec2(sample.getSize().x * inputGridsWidth, 0))
                        )
                );
            }

            JadeHelper.addGridsToTooltip(tooltip, grids);
            tooltip.add(List.of(helper.text(output.getHoverName()).align(IElement.Align.RIGHT)));
        }
    }

    @Override
    public void appendServerData(CompoundTag data, ServerPlayer serverPlayer, Level level, BlockEntity blockEntity, boolean b) {
        BeerBarrelBlockEntity be = (BeerBarrelBlockEntity) blockEntity;
        data.putInt(KEY_TIME_REMAINING, be.syncData.get(0));
    }
}