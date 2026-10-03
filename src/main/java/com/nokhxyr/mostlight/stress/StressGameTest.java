package com.nokhxyr.mostlight.stress;

import com.nokhxyr.mostlight.MostLight;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Hôte du test de charge côté serveur (./gradlew runStressServer) : le serveur de GameTest est un serveur
 * sans interface, sans CLUF à accepter. Sans -Dmostlight.stress=true ce test réussit immédiatement.
 */
@GameTestHolder(MostLight.MOD_ID)
@PrefixGameTestTemplate(false)
public class StressGameTest {
    @GameTest(template = "empty", timeoutTicks = 72_000)
    public static void stress(GameTestHelper helper) {
        if (!StressConfig.SERVER) {
            helper.succeed();
            return;
        }
        // loin de la zone des tests pour ne pas les gêner
        StressDriver.start(helper.getLevel().getServer(), helper.getLevel(), new BlockPos(1000, 0, 1000),
                StressConfig.fromProperties(false), FMLPaths.GAMEDIR.get().resolve("stress"));
        helper.succeedWhen(() -> helper.assertTrue(StressDriver.isFinished(), "test de charge en cours : " + StressDriver.phase()));
    }
}
