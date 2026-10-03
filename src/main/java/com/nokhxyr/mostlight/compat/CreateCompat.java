package com.nokhxyr.mostlight.compat;

import com.mojang.logging.LogUtils;
import com.nokhxyr.mostlight.block.HorizontalLampBlock;
import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.block.LightStripBlock;
import com.nokhxyr.mostlight.block.OmniLampBlock;
import com.nokhxyr.mostlight.block.TallLampBlock;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.slf4j.Logger;

/**
 * Create (facultatif) : dit aux contraptions à quoi chaque lampe est attachée (plafond, mur, sol, faces des bandes,
 * moitié du dessous des lampes hautes), pour qu'une lampe parte avec le bloc qui la porte. Les tags
 * create:movable_empty_collider, create:brittle et create:safe_nbt font le reste (données générées).
 * Appelé par réflexion : Create n'est pas une dépendance du mod.
 */
public final class CreateCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String[] CHECK_CLASSES = {
            "com.simibubi.create.api.contraption.BlockMovementChecks",
            "com.simibubi.create.content.contraptions.BlockMovementChecks",
    };

    private CreateCompat() {}

    /** Côté vers lequel la lampe est attachée à son support ; vrai si elle tient au bloc situé de ce côté. */
    public static boolean attachedTowards(BlockState state, Direction direction) {
        if (!(state.getBlock() instanceof LampBlock lamp)) {
            return false;
        }
        if (lamp instanceof LightStripBlock) {
            return LightStripBlock.has(state, direction);
        }
        if (lamp instanceof TallLampBlock) {
            // le bas tient au sol et au haut, le haut tient au bas
            boolean lower = state.getValue(TallLampBlock.HALF) == DoubleBlockHalf.LOWER;
            return lower ? direction == Direction.DOWN || direction == Direction.UP : direction == Direction.DOWN;
        }
        if (lamp instanceof OmniLampBlock) {
            return direction == state.getValue(OmniLampBlock.FACING).getOpposite();
        }
        if (lamp instanceof HorizontalLampBlock) {
            return switch (lamp.type().placement()) {
                case HANGING -> direction == Direction.UP;
                case WALL -> direction == state.getValue(HorizontalLampBlock.FACING).getOpposite();
                default -> direction == Direction.DOWN;
            };
        }
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register() {
        for (String name : CHECK_CLASSES) {
            try {
                Class<?> checks = Class.forName(name);
                Class<?> attached = Class.forName(name + "$AttachedCheck");
                Class<Enum> result = (Class<Enum>) Class.forName(name + "$CheckResult");
                Object success = Enum.valueOf(result, "SUCCESS");
                Object pass = Enum.valueOf(result, "PASS");
                Object check = Proxy.newProxyInstance(CreateCompat.class.getClassLoader(), new Class<?>[] {attached}, (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> "MostLight attached check";
                        };
                    }
                    BlockState state = (BlockState) args[0];
                    Direction direction = (Direction) args[args.length - 1];
                    return attachedTowards(state, direction) ? success : pass;
                });
                Method register = checks.getMethod("registerAttachedCheck", attached);
                register.invoke(null, check);
                LOGGER.info("[MostLight] Create : lampes attachées à leur support sur les contraptions ({})", name);
                return;
            } catch (ClassNotFoundException ignored) {
                // autre version de Create : on essaie le nom suivant
            } catch (ReflectiveOperationException | RuntimeException e) {
                LOGGER.warn("[MostLight] Create : enregistrement des lampes attachées impossible", e);
                return;
            }
        }
        LOGGER.warn("[MostLight] Create : API BlockMovementChecks introuvable, les lampes se déplacent seulement avec de la colle");
    }
}
