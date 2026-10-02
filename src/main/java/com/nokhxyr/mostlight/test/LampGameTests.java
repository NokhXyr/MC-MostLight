package com.nokhxyr.mostlight.test;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.HorizontalLampBlock;
import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.block.LampType;
import com.nokhxyr.mostlight.block.TallLampBlock;
import com.nokhxyr.mostlight.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Tests en jeu : ./gradlew runGameTestServer */
@GameTestHolder(MostLight.MOD_ID)
@PrefixGameTestTemplate(false)
public class LampGameTests {
    private static final BlockPos GROUND = new BlockPos(4, 1, 4);
    private static final BlockPos LAMP = GROUND.above();

    private static void place(GameTestHelper helper, Player player, ItemStack stack, BlockPos target, Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = helper.absolutePos(target);
        Vec3 hit = Vec3.atCenterOf(abs).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5));
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, abs, false)));
    }

    @GameTest(template = "empty")
    public static void toggleAndBrightness(GameTestHelper helper) {
        helper.setBlock(GROUND, Blocks.STONE);
        helper.setBlock(LAMP, ModBlocks.lamp(LampType.TABLE_LAMP, DyeColor.WHITE));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.assertTrue(helper.getBlockState(LAMP).getLightEmission() == 15, "lampe posée allumée à 15");
        helper.useBlock(LAMP, player);
        helper.assertTrue(!helper.getBlockState(LAMP).getValue(LampBlock.LIT), "clic droit éteint");
        helper.assertTrue(helper.getBlockState(LAMP).getLightEmission() == 0, "éteinte = aucune lumière");

        player.setShiftKeyDown(true);
        helper.useBlock(LAMP, player);
        BlockState state = helper.getBlockState(LAMP);
        helper.assertTrue(state.getValue(LampBlock.LIT) && state.getValue(LampBlock.BRIGHTNESS) == 1, "accroupi : rallume au niveau suivant");
        helper.assertTrue(state.getLightEmission() == 12, "niveau 1 = 12");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void redstoneControl(GameTestHelper helper) {
        helper.setBlock(GROUND, Blocks.STONE);
        helper.setBlock(LAMP, ModBlocks.lamp(LampType.LAVA_LAMP, DyeColor.RED).defaultBlockState().setValue(LampBlock.LIT, false));
        helper.setBlock(LAMP.east(), Blocks.REDSTONE_BLOCK);
        helper.assertTrue(helper.getBlockState(LAMP).getValue(LampBlock.LIT), "signal redstone allume");
        helper.setBlock(LAMP.east(), Blocks.AIR);
        helper.assertTrue(!helper.getBlockState(LAMP).getValue(LampBlock.LIT), "fin du signal éteint");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dyeRecolors(GameTestHelper helper) {
        helper.setBlock(GROUND, Blocks.STONE);
        helper.setBlock(LAMP, ModBlocks.lamp(LampType.DESK_LAMP, DyeColor.WHITE).defaultBlockState()
                .setValue(HorizontalLampBlock.FACING, Direction.EAST).setValue(LampBlock.BRIGHTNESS, 2));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BLUE_DYE, 2));
        helper.useBlock(LAMP, player);

        BlockState state = helper.getBlockState(LAMP);
        helper.assertTrue(state.getBlock() == ModBlocks.lamp(LampType.DESK_LAMP, DyeColor.BLUE), "devient bleue");
        helper.assertTrue(state.getValue(HorizontalLampBlock.FACING) == Direction.EAST && state.getValue(LampBlock.BRIGHTNESS) == 2,
                "garde orientation et luminosité");
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "consomme un colorant");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tallLampLifecycle(GameTestHelper helper) {
        helper.setBlock(GROUND, Blocks.STONE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        place(helper, player, new ItemStack(ModBlocks.item(LampType.STREET_LAMP, DyeColor.WHITE)), GROUND, Direction.UP);

        BlockState lower = helper.getBlockState(LAMP);
        BlockState upper = helper.getBlockState(LAMP.above());
        helper.assertTrue(lower.getBlock() instanceof TallLampBlock && lower.getValue(TallLampBlock.HALF) == DoubleBlockHalf.LOWER, "moitié basse posée");
        helper.assertTrue(upper.getBlock() instanceof TallLampBlock && upper.getValue(TallLampBlock.HALF) == DoubleBlockHalf.UPPER, "moitié haute posée");
        helper.assertTrue(lower.getLightEmission() == 0 && upper.getLightEmission() == 15, "seul le haut éclaire");

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.useBlock(LAMP, player);
        helper.assertTrue(!helper.getBlockState(LAMP.above()).getValue(LampBlock.LIT), "éteindre le bas éteint le haut");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GREEN_DYE));
        helper.useBlock(LAMP.above(), player);
        helper.assertTrue(helper.getBlockState(LAMP).getBlock() == ModBlocks.lamp(LampType.STREET_LAMP, DyeColor.GREEN)
                && helper.getBlockState(LAMP.above()).getBlock() == ModBlocks.lamp(LampType.STREET_LAMP, DyeColor.GREEN), "les deux moitiés recolorées");

        helper.destroyBlock(LAMP.above());
        helper.assertBlockPresent(Blocks.AIR, LAMP);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void placementNeedsSupport(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos ceiling = new BlockPos(4, 4, 4);
        helper.setBlock(ceiling, Blocks.STONE);
        place(helper, player, new ItemStack(ModBlocks.item(LampType.CHANDELIER, DyeColor.YELLOW)), ceiling, Direction.DOWN);
        helper.assertBlockPresent(ModBlocks.lamp(LampType.CHANDELIER, DyeColor.YELLOW), ceiling.below());

        place(helper, player, new ItemStack(ModBlocks.item(LampType.WALL_SCONCE, DyeColor.CYAN)), ceiling, Direction.EAST);
        helper.assertBlockPresent(ModBlocks.lamp(LampType.WALL_SCONCE, DyeColor.CYAN), ceiling.east());
        helper.assertTrue(helper.getBlockState(ceiling.east()).getValue(HorizontalLampBlock.FACING) == Direction.EAST, "applique tournée vers l'extérieur");

        place(helper, player, new ItemStack(ModBlocks.item(LampType.LAMP_PANEL, DyeColor.WHITE)), ceiling, Direction.WEST);
        helper.assertBlockPresent(ModBlocks.lamp(LampType.LAMP_PANEL, DyeColor.WHITE), ceiling.west());

        helper.setBlock(ceiling, Blocks.AIR);
        helper.assertBlockPresent(Blocks.AIR, ceiling.below());
        helper.assertBlockPresent(Blocks.AIR, ceiling.east());
        helper.assertBlockPresent(Blocks.AIR, ceiling.west());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void allShapesBuild(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(LAMP);
        int states = 0;
        for (DeferredBlock<LampBlock> holder : ModBlocks.all()) {
            for (BlockState state : holder.get().getStateDefinition().getPossibleStates()) {
                helper.assertTrue(!state.getShape(helper.getLevel(), pos).isEmpty(), "hitbox vide : " + state);
                states++;
            }
        }
        helper.assertTrue(states > 0, "aucun état testé");
        helper.succeed();
    }
}
