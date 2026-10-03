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

    /** Ventilateur lumineux : 1er clic lumière, 2e clic ventilateur, 3e clic tout éteint. */
    @GameTest(template = "empty")
    public static void fanLightClickCycle(GameTestHelper helper) {
        BlockPos ceiling = new BlockPos(4, 4, 4);
        helper.setBlock(ceiling, Blocks.STONE);
        BlockPos fan = ceiling.below();
        helper.setBlock(fan, ModBlocks.lamp(LampType.FAN_LIGHT, DyeColor.WHITE).defaultBlockState()
                .setValue(com.nokhxyr.mostlight.block.LampBlock.LIT, false));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.useBlock(fan, player);
        helper.assertBlockProperty(fan, com.nokhxyr.mostlight.block.LampBlock.LIT, true);
        helper.assertBlockProperty(fan, com.nokhxyr.mostlight.block.FanLampBlock.FAN, false);
        helper.useBlock(fan, player);
        helper.assertBlockProperty(fan, com.nokhxyr.mostlight.block.FanLampBlock.FAN, true);
        helper.useBlock(fan, player);
        helper.assertBlockProperty(fan, com.nokhxyr.mostlight.block.LampBlock.LIT, false);
        helper.assertBlockProperty(fan, com.nokhxyr.mostlight.block.FanLampBlock.FAN, false);
        helper.succeed();
    }

    /** Interrupteur double : bascule gauche = lumière des lampes liées, bascule droite = leurs ventilateurs. */
    @GameTest(template = "empty")
    public static void doubleSwitchLightAndFan(GameTestHelper helper) {
        BlockPos ceiling = new BlockPos(2, 4, 4);
        helper.setBlock(ceiling, Blocks.STONE);
        BlockPos fan = ceiling.below();
        helper.setBlock(fan, ModBlocks.lamp(LampType.FAN_LIGHT, DyeColor.WHITE).defaultBlockState()
                .setValue(com.nokhxyr.mostlight.block.LampBlock.LIT, false));
        BlockPos wall = new BlockPos(6, 2, 4);
        helper.setBlock(wall, Blocks.STONE);
        BlockPos switchPos = wall.north();
        helper.setBlock(switchPos, ModBlocks.switchBlock(ModBlocks.SwitchKind.DOUBLE, DyeColor.RED).defaultBlockState()
                .setValue(com.nokhxyr.mostlight.link.LightSwitchBlock.FACE, net.minecraft.world.level.block.state.properties.AttachFace.WALL)
                .setValue(com.nokhxyr.mostlight.link.LightSwitchBlock.FACING, Direction.NORTH));
        ((com.nokhxyr.mostlight.link.SwitchBlockEntity) helper.getBlockEntity(switchPos)).setLinks(java.util.List.of(helper.absolutePos(fan)));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos abs = helper.absolutePos(switchPos);
        // interrupteur tourné vers le nord : bascule de la lumière côté ouest, du ventilateur côté est (retour en jeu)
        Vec3 left = Vec3.atCenterOf(abs).add(-0.25, 0, -0.4);
        Vec3 right = Vec3.atCenterOf(abs).add(0.25, 0, -0.4);
        BlockState sw = helper.getLevel().getBlockState(abs);
        sw.useWithoutItem(helper.getLevel(), player, new BlockHitResult(left, Direction.NORTH, abs, false));
        helper.assertBlockProperty(fan, com.nokhxyr.mostlight.block.LampBlock.LIT, true);
        sw = helper.getLevel().getBlockState(abs);
        sw.useWithoutItem(helper.getLevel(), player, new BlockHitResult(right, Direction.NORTH, abs, false));
        helper.assertBlockProperty(fan, com.nokhxyr.mostlight.block.FanLampBlock.FAN, true);
        helper.assertBlockProperty(switchPos, com.nokhxyr.mostlight.link.DoubleSwitchBlock.FAN, true);
        helper.succeed();
    }

    /** Clé de décorateur : la finition choisie (molette) est celle appliquée. */
    @GameTest(template = "empty")
    public static void wrenchAppliesSelectedFinish(GameTestHelper helper) {
        helper.setBlock(LAMP, ModBlocks.lamp(LampType.LAMP_BLOCK, DyeColor.WHITE));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack wrench = new ItemStack(ModBlocks.DESIGNER_WRENCH.get());
        com.nokhxyr.mostlight.item.DesignerWrenchItem.scroll(player, wrench, 1);
        com.nokhxyr.mostlight.item.DesignerWrenchItem.scroll(player, wrench, 1);
        com.nokhxyr.mostlight.item.DesignerWrenchItem.scroll(player, wrench, -1);
        LampFinish chosen = wrench.get(com.nokhxyr.mostlight.component.ModComponents.FINISH.get());
        helper.assertTrue(chosen == LampFinish.values()[0], "molette : 2 crans puis 1 en arrière -> 1re finition, obtenu " + chosen);
        com.nokhxyr.mostlight.item.DesignerWrenchItem.scroll(player, wrench, 1);
        place(helper, player, wrench, LAMP, Direction.UP);
        helper.assertTrue(((LampBlockEntity) helper.getBlockEntity(LAMP)).finish() == LampFinish.values()[1], "finition choisie appliquée");
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

    private static int layout(GameTestHelper helper, BlockPos pos) {
        return ((LampBlockEntity) helper.getBlockEntity(pos)).stripLayout();
    }

    @GameTest(template = "empty")
    public static void ledStripSlotsAndRotation(GameTestHelper helper) {
        BlockPos wall = new BlockPos(4, 2, 4);
        helper.setBlock(wall, Blocks.STONE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        // face nord du mur, visée près du bas : bande horizontale en position basse
        placeAt(helper, player, new ItemStack(ModBlocks.item(LampType.LIGHT_STRIP, DyeColor.CYAN)), wall, Direction.NORTH, new Vec3(0.5, 0.1, 0));
        int low = layout(helper, wall.north());
        helper.assertTrue(LightStripBlock.has(helper.getBlockState(wall.north()), Direction.SOUTH), "bande fixée au mur");
        helper.assertTrue(LampBlockEntity.slot(low, Direction.SOUTH) == 0 && !LampBlockEntity.rotated(low, Direction.SOUTH), "bande basse horizontale");

        // face sud, accroupi, visée en haut à droite : verticale
        player.setShiftKeyDown(true);
        placeAt(helper, player, new ItemStack(ModBlocks.item(LampType.LIGHT_STRIP, DyeColor.CYAN)), wall, Direction.SOUTH, new Vec3(0.9, 0.5, 1));
        player.setShiftKeyDown(false);
        int vertical = layout(helper, wall.south());
        helper.assertTrue(LampBlockEntity.rotated(vertical, Direction.NORTH), "accroupi : bande verticale");
        helper.assertTrue(LampBlockEntity.slot(vertical, Direction.NORTH) != 1, "position latérale selon la visée");
        helper.succeed();
    }

    /** Angle de pièce : deux bandes sur deux murs dans le même bloc, chacune avec sa propre position. */
    @GameTest(template = "empty")
    public static void ledStripCornerInOneBlock(GameTestHelper helper) {
        BlockPos corner = new BlockPos(4, 2, 4);
        helper.setBlock(corner.north(), Blocks.STONE);
        helper.setBlock(corner.east(), Blocks.STONE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack strip = new ItemStack(ModBlocks.item(LampType.LIGHT_STRIP, DyeColor.ORANGE), 2);
        // mur nord, visée en haut ; puis mur est, visée en bas
        placeAt(helper, player, strip, corner.north(), Direction.SOUTH, new Vec3(0.5, 0.9, 1));
        placeAt(helper, player, strip, corner.east(), Direction.WEST, new Vec3(0, 0.1, 0.5));
        BlockState state = helper.getBlockState(corner);
        helper.assertTrue(LightStripBlock.has(state, Direction.NORTH) && LightStripBlock.has(state, Direction.EAST),
                "deux bandes dans le même bloc : " + state);
        int layout = layout(helper, corner);
        helper.assertTrue(LampBlockEntity.slot(layout, Direction.NORTH) == 2 && LampBlockEntity.slot(layout, Direction.EAST) == 0,
                "chaque bande garde sa position");
        // le mur est disparaît : seule sa bande tombe, l'autre reste
        helper.setBlock(corner.east(), Blocks.AIR);
        BlockState after = helper.getBlockState(corner);
        helper.assertTrue(LightStripBlock.has(after, Direction.NORTH) && !LightStripBlock.has(after, Direction.EAST), "seule la bande sans support tombe");
        helper.succeed();
    }

    /** Chaîne redstone : 5 bandes reliées au connecteur ; alimenter la première allume tout, couper éteint tout. */
    @GameTest(template = "empty")
    public static void ledChainFollowsRedstone(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (int x = 2; x <= 6; x++) {
            helper.setBlock(new BlockPos(x, 1, 4), Blocks.STONE);
            placeAt(helper, player, new ItemStack(ModBlocks.item(LampType.LIGHT_STRIP, DyeColor.LIME)), new BlockPos(x, 1, 4), Direction.UP,
                    new Vec3(0.5, 1, 0.5));
        }
        // connecteur : clic près du bord est de chaque bande
        for (int x = 2; x <= 5; x++) {
            placeAt(helper, player, new ItemStack(ModBlocks.LED_CONNECTOR.get()), new BlockPos(x, 2, 4), Direction.UP, new Vec3(0.95, 0.06, 0.5));
        }
        BlockPos first = new BlockPos(2, 2, 4);
        helper.assertTrue(LightStripBlock.chain(helper.getLevel(), helper.absolutePos(first)).size() == 5, "5 bandes dans la chaîne");
        helper.setBlock(first.west(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(2, () -> {
            for (int x = 2; x <= 6; x++) {
                helper.assertBlockProperty(new BlockPos(x, 2, 4), LampBlock.LIT, true);
            }
            helper.setBlock(first.west(), Blocks.AIR);
            helper.runAfterDelay(2, () -> {
                for (int x = 2; x <= 6; x++) {
                    helper.assertBlockProperty(new BlockPos(x, 2, 4), LampBlock.LIT, false);
                }
                helper.succeed();
            });
        });
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
                // une bande LED sans aucune face n'existe jamais dans le monde (le bloc disparaît)
                if (state.getBlock() instanceof LightStripBlock && java.util.Arrays.stream(Direction.values()).noneMatch(d -> LightStripBlock.has(state, d))) {
                    continue;
                }
                helper.assertTrue(!state.getShape(helper.getLevel(), pos).isEmpty(), "hitbox vide : " + state);
                states++;
            }
        }
        helper.assertTrue(states > 0, "aucun état testé");
        helper.succeed();
    }
    /** Établi de luminaire : les recettes du mod s'y trouvent, plus à l'établi vanilla. */
    @GameTest(template = "empty")
    public static void recipesOnlyAtLampWorkbench(GameTestHelper helper) {
        net.minecraft.world.item.crafting.CraftingInput rod = net.minecraft.world.item.crafting.CraftingInput.of(1, 3, java.util.List.of(
                new ItemStack(Items.GLOWSTONE_DUST), new ItemStack(Items.GLASS), new ItemStack(Items.IRON_NUGGET)));
        var recipes = helper.getLevel().getRecipeManager();
        var lamp = recipes.getRecipeFor(com.nokhxyr.mostlight.crafting.ModRecipes.WORKBENCH.get(), rod, helper.getLevel());
        helper.assertTrue(lamp.isPresent() && lamp.get().value().getResultItem(helper.getLevel().registryAccess())
                .is(ModBlocks.item(LampType.LIGHT_ROD, DyeColor.WHITE)), "tige lumineuse à l'établi de luminaire");
        helper.assertTrue(recipes.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, rod, helper.getLevel()).isEmpty(),
                "plus de recette de lampe à l'établi vanilla");
        // teinture (sans forme) : lampe + colorant
        net.minecraft.world.item.crafting.CraftingInput dye = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, java.util.List.of(
                new ItemStack(ModBlocks.item(LampType.LIGHT_ROD, DyeColor.WHITE)), new ItemStack(Items.RED_DYE)));
        var dyed = recipes.getRecipeFor(com.nokhxyr.mostlight.crafting.ModRecipes.WORKBENCH.get(), dye, helper.getLevel());
        helper.assertTrue(dyed.isPresent() && dyed.get().value().getResultItem(helper.getLevel().registryAccess())
                .is(ModBlocks.item(LampType.LIGHT_ROD, DyeColor.RED)), "teinture à l'établi de luminaire");
        helper.succeed();
    }

    /** Blocs lumineux reliés au connecteur : alimenter le premier allume toute la rangée. */
    @GameTest(template = "empty")
    public static void lampBlocksChainRedstone(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (int x = 2; x <= 5; x++) {
            helper.setBlock(new BlockPos(x, 2, 4), ModBlocks.lamp(LampType.LAMP_BLOCK, DyeColor.WHITE).defaultBlockState().setValue(LampBlock.LIT, false));
        }
        for (int x = 2; x <= 4; x++) {
            placeAt(helper, player, new ItemStack(ModBlocks.LED_CONNECTOR.get()), new BlockPos(x, 2, 4), Direction.UP, new Vec3(0.95, 1, 0.5));
        }
        BlockPos first = new BlockPos(2, 2, 4);
        helper.assertTrue(LightStripBlock.chain(helper.getLevel(), helper.absolutePos(first)).size() == 4, "4 blocs dans la chaîne");
        helper.setBlock(first.west(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(2, () -> {
            for (int x = 2; x <= 5; x++) {
                helper.assertBlockProperty(new BlockPos(x, 2, 4), LampBlock.LIT, true);
            }
            helper.succeed();
        });
    }

    /** Bandes : angle intérieur (une s'arrête contre l'autre), T sur un mur, angle extérieur autour d'un pilier. */
    @GameTest(template = "empty")
    public static void stripsJoinCornersAndTees(GameTestHelper helper) {
        BlockPos corner = new BlockPos(4, 2, 4);
        helper.setBlock(corner.south(), Blocks.STONE);
        helper.setBlock(corner.east(), Blocks.STONE);
        helper.setBlock(corner.south().below(), Blocks.STONE);
        helper.setBlock(corner.west().south(), Blocks.STONE);
        LampBlock strip = ModBlocks.lamp(LampType.LIGHT_STRIP, DyeColor.ORANGE);
        setStrip(helper, strip, corner, Direction.SOUTH, 2, false);
        setStrip(helper, strip, corner, Direction.EAST, 2, false);
        setStrip(helper, strip, corner.west(), Direction.SOUTH, 2, false);
        setStrip(helper, strip, corner.below(), Direction.SOUTH, 1, true);
        BlockPos abs = helper.absolutePos(corner);
        int[] segs = LightStripBlock.segments(helper.getLevel(), abs, helper.getLevel().getBlockState(abs), layout(helper, corner));
        boolean southToEast = false, eastFull = false, tee = false;
        for (int seg : segs) {
            Direction side = LightStripBlock.segmentSide(seg);
            if (side == Direction.SOUTH && LightStripBlock.segmentAxis(seg) == Direction.Axis.X && LightStripBlock.segmentTo(seg) == 7) {
                southToEast = true; // la bande sud va jusqu'au mur est (est = indice plus grand : elle passe)
            }
            if (side == Direction.EAST && LightStripBlock.segmentAxis(seg) == Direction.Axis.Z && LightStripBlock.segmentTo(seg) == 6) {
                eastFull = true; // la bande est s'arrête contre la bande sud
            }
            if (side == Direction.SOUTH && LightStripBlock.segmentAxis(seg) == Direction.Axis.Y) {
                tee = true; // bras vers la bande verticale du dessous
            }
        }
        helper.assertTrue(southToEast && eastFull, "angle intérieur emboîté : " + java.util.Arrays.toString(segs));
        helper.assertTrue(tee, "T vers la bande verticale");
        helper.succeed();
    }

    private static void setStrip(GameTestHelper helper, LampBlock block, BlockPos pos, Direction side, int slot, boolean rotated) {
        BlockState current = helper.getBlockState(pos);
        BlockState state = current.is(block) ? current : block.defaultBlockState().setValue(LightStripBlock.SIDES.get(Direction.DOWN), false);
        helper.setBlock(pos, state.setValue(LightStripBlock.SIDES.get(side), true));
        ((LampBlockEntity) helper.getBlockEntity(pos)).setStrip(side, slot, rotated);
    }

    /** Tige lumineuse : couchée à l'horizontale au mur, à la verticale en étant accroupi, debout au sol. */
    @GameTest(template = "empty")
    public static void rodLiesAlongWalls(GameTestHelper helper) {
        BlockPos wall = new BlockPos(4, 2, 5);
        BlockPos floor = new BlockPos(1, 1, 2);
        helper.setBlock(wall, Blocks.STONE);
        helper.setBlock(floor, Blocks.STONE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack rod = new ItemStack(ModBlocks.item(LampType.LIGHT_ROD, DyeColor.WHITE), 3);
        place(helper, player, rod, wall, Direction.NORTH);
        BlockState lying = helper.getBlockState(wall.north());
        helper.assertTrue(lying.getValue(com.nokhxyr.mostlight.block.RodLampBlock.LYING)
                && lying.getValue(com.nokhxyr.mostlight.block.RodLampBlock.AXIS) == Direction.Axis.X, "au mur : couchée à l'horizontale " + lying);
        place(helper, player, rod, floor, Direction.UP);
        BlockState standing = helper.getBlockState(floor.above());
        helper.assertTrue(!standing.getValue(com.nokhxyr.mostlight.block.RodLampBlock.LYING), "au sol : debout " + standing);
        helper.assertTrue(!lying.getShape(helper.getLevel(), helper.absolutePos(wall.north())).isEmpty(), "hitbox couchée");
        helper.succeed();
    }
}
