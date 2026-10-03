package com.nokhxyr.mostlight.test;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.HorizontalLampBlock;
import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.block.LampType;
import com.nokhxyr.mostlight.block.LampFinish;
import com.nokhxyr.mostlight.block.LightStripBlock;
import com.nokhxyr.mostlight.block.LightTone;
import com.nokhxyr.mostlight.block.TallLampBlock;
import com.nokhxyr.mostlight.block.entity.LampBlockEntity;
import com.nokhxyr.mostlight.link.LampLinks;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
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
    public static void wrenchAndDyeKeepLook(GameTestHelper helper) {
        helper.setBlock(GROUND, Blocks.STONE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        place(helper, player, new ItemStack(ModBlocks.item(LampType.FLOOR_LAMP, DyeColor.WHITE)), GROUND, Direction.UP);
        BlockPos upper = LAMP.above();

        place(helper, player, new ItemStack(ModBlocks.DESIGNER_WRENCH.get()), LAMP, Direction.NORTH);
        LampBlockEntity lower = (LampBlockEntity) helper.getBlockEntity(LAMP);
        LampBlockEntity top = (LampBlockEntity) helper.getBlockEntity(upper);
        helper.assertTrue(lower.finish() == LampType.FLOOR_LAMP.defaultFinish().next() && top.finish() == lower.finish(),
                "la clé change la finition des deux moitiés");

        player.setShiftKeyDown(true);
        place(helper, player, new ItemStack(ModBlocks.DESIGNER_WRENCH.get()), upper, Direction.NORTH);
        helper.assertTrue(lower.tone() == LightTone.COLORED && top.tone() == LightTone.COLORED, "accroupi : teinte suivante");
        player.setShiftKeyDown(false);

        LampFinish finish = lower.finish();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PURPLE_DYE));
        helper.useBlock(LAMP, player);
        LampBlockEntity recolored = (LampBlockEntity) helper.getBlockEntity(upper);
        helper.assertTrue(helper.getBlockState(upper).getBlock() == ModBlocks.lamp(LampType.FLOOR_LAMP, DyeColor.PURPLE)
                && recolored.finish() == finish && recolored.tone() == LightTone.COLORED, "le colorant garde finition et teinte");
        helper.succeed();
    }

    /** Poser une lampe haute (par un joueur ou bloc par bloc) ne doit faire tomber aucun objet. */
    @GameTest(template = "empty")
    public static void tallLampPlacementDropsNothing(GameTestHelper helper) {
        helper.setBlock(GROUND, Blocks.STONE);
        helper.setBlock(GROUND.east(2), Blocks.STONE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        place(helper, player, new ItemStack(ModBlocks.item(LampType.STREET_LAMP, DyeColor.WHITE)), GROUND, Direction.UP);
        BlockState lower = ModBlocks.lamp(LampType.STREET_LAMP, DyeColor.RED).defaultBlockState();
        helper.setBlock(LAMP.east(2), lower);
        helper.setBlock(LAMP.east(2).above(), lower.setValue(TallLampBlock.HALF, DoubleBlockHalf.UPPER));
        helper.runAfterDelay(2, () -> {
            var items = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(helper.absolutePos(BlockPos.ZERO)).inflate(8));
            helper.assertTrue(items.isEmpty(), "objets tombés : " + items.stream().map(i -> i.getItem().toString()).toList());
            helper.assertTrue(helper.getBlockState(LAMP.above()).getBlock() instanceof TallLampBlock
                    && helper.getBlockState(LAMP.east(2).above()).getBlock() instanceof TallLampBlock, "les deux lampes sont entières");
            helper.succeed();
        });
    }

    /** Réseau : une bascule n'envoie pas les données de la lampe, un changement de finition si, et le client suit. */
    @GameTest(template = "empty")
    public static void lookSyncsOnlyWhenChanged(GameTestHelper helper) {
        helper.setBlock(LAMP, ModBlocks.lamp(LampType.LAMP_BLOCK, DyeColor.RED));
        LampBlockEntity lamp = (LampBlockEntity) helper.getBlockEntity(LAMP);
        var registries = helper.getLevel().registryAccess();
        helper.assertTrue(lamp.getUpdateTag(registries).isEmpty(), "aspect par défaut : rien à envoyer avec le chunk");

        helper.useBlock(LAMP, helper.makeMockPlayer(GameType.SURVIVAL));
        helper.assertTrue(lamp.getUpdatePacket() == null, "allumer/éteindre n'envoie pas les données de la lampe");

        lamp.setLook(LampFinish.GOLD, LightTone.COOL);
        helper.assertTrue(lamp.getUpdatePacket() != null, "changer la finition envoie les données");
        helper.assertTrue(lamp.getUpdatePacket() == null, "une seule fois");

        // côté client : une copie qui reçoit les paquets successifs
        LampBlockEntity client = new LampBlockEntity(helper.absolutePos(LAMP), helper.getBlockState(LAMP));
        client.loadWithComponents(lamp.getUpdateTag(registries), registries);
        helper.assertTrue(client.finish() == LampFinish.GOLD && client.tone() == LightTone.COOL, "le client reçoit la finition");
        lamp.setLook(LampType.LAMP_BLOCK.defaultFinish(), LightTone.AUTO);
        client.loadWithComponents(lamp.getUpdateTag(registries), registries);
        helper.assertTrue(client.finish() == LampType.LAMP_BLOCK.defaultFinish() && client.tone() == LightTone.AUTO,
                "retour aux valeurs par défaut transmis au client");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void switchControlsLinkedLamps(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        ItemStack switchItem = new ItemStack(ModBlocks.LIGHT_SWITCH_ITEM.get());
        for (int x = 1; x <= 3; x++) {
            helper.setBlock(new BlockPos(x, 1, 1), Blocks.STONE);
            helper.setBlock(new BlockPos(x, 2, 1), ModBlocks.lamp(LampType.LAVA_LAMP, DyeColor.ORANGE));
            place(helper, player, switchItem, new BlockPos(x, 2, 1), Direction.NORTH);
        }
        helper.assertTrue(LampLinks.get(switchItem).size() == 3, "3 lampes liées");
        player.setShiftKeyDown(false);

        helper.setBlock(new BlockPos(6, 1, 6), Blocks.STONE);
        place(helper, player, switchItem, new BlockPos(6, 1, 6), Direction.UP);
        BlockPos switchPos = new BlockPos(6, 2, 6);
        helper.assertBlockPresent(ModBlocks.LIGHT_SWITCH.get(), switchPos);

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.useBlock(switchPos, player);
        for (int x = 1; x <= 3; x++) {
            helper.assertTrue(!helper.getBlockState(new BlockPos(x, 2, 1)).getValue(LampBlock.LIT), "interrupteur : tout éteint");
        }
        helper.useBlock(switchPos, player);
        for (int x = 1; x <= 3; x++) {
            helper.assertTrue(helper.getBlockState(new BlockPos(x, 2, 1)).getValue(LampBlock.LIT), "interrupteur : tout rallumé");
        }

        ItemStack remote = new ItemStack(ModBlocks.LAMP_REMOTE.get());
        player.setShiftKeyDown(true);
        place(helper, player, remote, new BlockPos(2, 2, 1), Direction.NORTH);
        helper.assertTrue(LampLinks.get(remote).size() == 1, "télécommande liée");
        helper.succeed();
    }

    /** Pose en visant un point précis de la face (coordonnées 0..1 dans le bloc support). */
    private static void placeAt(GameTestHelper helper, Player player, ItemStack stack, BlockPos target, Direction face, Vec3 local) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = helper.absolutePos(target);
        Vec3 hit = Vec3.atLowerCornerOf(abs).add(local);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, abs, false)));
    }

    @GameTest(template = "empty")
    public static void ledStripSlotsAndRotation(GameTestHelper helper) {
        BlockPos wall = new BlockPos(4, 2, 4);
        helper.setBlock(wall, Blocks.STONE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        // face nord du mur, visée près du bas : bande horizontale en position basse
        placeAt(helper, player, new ItemStack(ModBlocks.item(LampType.LIGHT_STRIP, DyeColor.CYAN)), wall, Direction.NORTH, new Vec3(0.5, 0.1, 0));
        BlockState low = helper.getBlockState(wall.north());
        helper.assertTrue(low.getValue(LightStripBlock.SLOT) == LightStripBlock.Slot.LOW && !low.getValue(LightStripBlock.ROTATED),
                "bande basse horizontale");

        // face sud, accroupi, visée en haut à droite : verticale
        player.setShiftKeyDown(true);
        placeAt(helper, player, new ItemStack(ModBlocks.item(LampType.LIGHT_STRIP, DyeColor.CYAN)), wall, Direction.SOUTH, new Vec3(0.9, 0.5, 1));
        player.setShiftKeyDown(false);
        BlockState vertical = helper.getBlockState(wall.south());
        helper.assertTrue(vertical.getValue(LightStripBlock.ROTATED), "accroupi : bande verticale");
        helper.assertTrue(vertical.getValue(LightStripBlock.SLOT) != LightStripBlock.Slot.MIDDLE, "position latérale selon la visée");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void placeOnPartialBlocks(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        // escalier à l'envers : LED collée dessous
        BlockPos stairs = new BlockPos(2, 3, 2);
        helper.setBlock(stairs, Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HALF, Half.TOP));
        placeAt(helper, player, new ItemStack(ModBlocks.item(LampType.LIGHT_STRIP, DyeColor.WHITE)), stairs, Direction.DOWN, new Vec3(0.5, 0, 0.5));
        helper.assertBlockPresent(ModBlocks.lamp(LampType.LIGHT_STRIP, DyeColor.WHITE), stairs.below());

        // tête de joueur : spot encastré sur le côté, lampe de table dessus
        BlockPos head = new BlockPos(6, 1, 6);
        helper.setBlock(head, Blocks.PLAYER_HEAD);
        placeAt(helper, player, new ItemStack(ModBlocks.item(LampType.FLUSH_LIGHT, DyeColor.RED)), head, Direction.EAST, new Vec3(1, 0.3, 0.5));
        helper.assertBlockPresent(ModBlocks.lamp(LampType.FLUSH_LIGHT, DyeColor.RED), head.east());
        placeAt(helper, player, new ItemStack(ModBlocks.item(LampType.TABLE_LAMP, DyeColor.RED)), head, Direction.UP, new Vec3(0.5, 0.5, 0.5));
        helper.assertBlockPresent(ModBlocks.lamp(LampType.TABLE_LAMP, DyeColor.RED), head.above());

        // l'air ne porte rien
        helper.setBlock(stairs, Blocks.AIR);
        helper.assertBlockPresent(Blocks.AIR, stairs.below());
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
