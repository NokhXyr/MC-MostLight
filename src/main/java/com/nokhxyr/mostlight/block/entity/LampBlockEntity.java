package com.nokhxyr.mostlight.block.entity;

import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.block.LampFinish;
import com.nokhxyr.mostlight.block.LightTone;
import com.nokhxyr.mostlight.component.ModComponents;
import com.nokhxyr.mostlight.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

/** Stocke la finition du cadre et la teinte de lumière ; le rendu les lit via les teintes de bloc. */
public class LampBlockEntity extends BlockEntity {
    private LampFinish finish;
    private LightTone tone = LightTone.AUTO;
    /** Finition, teinte ou réglage des bandes LED modifiés depuis le dernier envoi aux clients. */
    private boolean lookChanged;
    /**
     * Bandes LED : position (basse / milieu / haute) et sens de chaque bande, 3 bits par face (ordre de Direction),
     * plusieurs bandes pouvant partager le même bloc (angles). Lu par le modèle via ModelData et par la hitbox.
     */
    public static final int DEFAULT_STRIP_LAYOUT = 0b001_001_001_001_001_001;
    public static final ModelProperty<Integer> STRIP_LAYOUT = new ModelProperty<>();
    private int stripLayout = DEFAULT_STRIP_LAYOUT;
    /** Bandes LED : côtés reliés aux bandes voisines pour la redstone (un bit par Direction). */
    private int connections;
    /**
     * Brightness step the lamp comes back on at (0 = brightest) and the last redstone signal it saw. Kept here rather
     * than in the block state: a state property multiplies the number of block states of all 1200 lamp blocks.
     * Tall lamps keep them on their lower half.
     */
    private int brightness;
    private boolean powered;
    /** Incrémenté à chaque changement d'aspect connu du client : invalide le cache de teintes du rendu. */
    public static volatile int lookVersion;

    public LampBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.LAMP.get(), pos, state);
    }

    protected LampBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        finish = state.getBlock() instanceof LampBlock lamp ? lamp.type().defaultFinish() : LampFinish.STEEL;
    }

    public LampFinish finish() {
        return finish;
    }

    public LightTone tone() {
        return tone;
    }

    public void setLook(LampFinish finish, LightTone tone) {
        this.finish = finish;
        this.tone = tone;
        sync();
    }

    public int brightness() {
        return brightness;
    }

    public void setBrightness(int brightness) {
        if (this.brightness != brightness) {
            this.brightness = brightness;
            setChanged();
        }
    }

    public boolean powered() {
        return powered;
    }

    /** Server-side memory only: clients work out whether a chain is powered from the redstone around it. */
    public void setPowered(boolean powered) {
        if (this.powered != powered) {
            this.powered = powered;
            setChanged();
        }
    }

    public int stripLayout() {
        return stripLayout;
    }

    /** Position (0 basse, 1 milieu, 2 haute) et sens de la bande fixée côté {@code side}. */
    public void setStrip(Direction side, int slot, boolean rotated) {
        int shift = side.ordinal() * 3;
        stripLayout = (stripLayout & ~(0b111 << shift)) | (((slot & 0b11) | (rotated ? 0b100 : 0)) << shift);
        sync();
    }

    public static int slot(int layout, Direction side) {
        return Math.min(2, (layout >> (side.ordinal() * 3)) & 0b11);
    }

    public static boolean rotated(int layout, Direction side) {
        return ((layout >> (side.ordinal() * 3)) & 0b100) != 0;
    }

    public int connections() {
        return connections;
    }

    public boolean connected(Direction side) {
        return (connections & (1 << side.ordinal())) != 0;
    }

    public void setConnected(Direction side, boolean on) {
        connections = on ? connections | (1 << side.ordinal()) : connections & ~(1 << side.ordinal());
        sync();
    }

    /** Arrivée dans le monde : reprend les données d'une lampe déplacée par un piston (PistonCarry). */
    @Override
    public void onLoad() {
        super.onLoad();
        com.nokhxyr.mostlight.block.PistonCarry.restore(this);
    }

    /** Après un déplacement par piston : renvoie l'aspect aux clients et met à jour le rendu. */
    public void afterMove() {
        sync();
    }

    private void sync() {
        lookChanged = true;
        lookVersion++;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public ModelData getModelData() {
        if (!(getBlockState().getBlock() instanceof com.nokhxyr.mostlight.block.LightStripBlock)) {
            return ModelData.EMPTY;
        }
        return ModelData.builder().with(STRIP_LAYOUT, stripLayout).build();
    }

    /**
     * Sauvegarde : seulement ce qui diffère des valeurs par défaut (comme les paquets réseau). La plupart des lampes
     * gardent la finition et la teinte de leur modèle : leur block entity ne contient alors que sa position, ce qui
     * allège la sauvegarde des chunks qui en portent des milliers. loadAdditional remet les valeurs par défaut quand
     * une clé manque.
     */
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeLook(tag);
        writeStrip(tag);
        if (brightness != 0) {
            tag.putByte("brightness", (byte) brightness);
        }
        writePowered(tag);
    }

    private void writePowered(CompoundTag tag) {
        if (powered) {
            tag.putBoolean("powered", true);
        }
    }

    private void writeLook(CompoundTag tag) {
        LampFinish defaultFinish = getBlockState().getBlock() instanceof LampBlock lamp ? lamp.type().defaultFinish() : null;
        if (finish != defaultFinish) {
            tag.putString("finish", finish.getSerializedName());
        }
        if (tone != LightTone.AUTO) {
            tag.putString("tone", tone.getSerializedName());
        }
    }

    private void writeStrip(CompoundTag tag) {
        if (stripLayout != DEFAULT_STRIP_LAYOUT) {
            tag.putInt("strip", stripLayout);
        }
        if (connections != 0) {
            tag.putByte("connections", (byte) connections);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // une clé absente veut dire « valeur par défaut » (les paquets n'envoient que les différences)
        finish = getBlockState().getBlock() instanceof LampBlock lamp ? lamp.type().defaultFinish() : LampFinish.STEEL;
        tone = LightTone.AUTO;
        for (LampFinish f : LampFinish.values()) {
            if (f.getSerializedName().equals(tag.getString("finish"))) {
                finish = f;
            }
        }
        for (LightTone t : LightTone.values()) {
            if (t.getSerializedName().equals(tag.getString("tone"))) {
                tone = t;
            }
        }
        stripLayout = tag.contains("strip") ? tag.getInt("strip") : DEFAULT_STRIP_LAYOUT;
        connections = tag.getByte("connections") & 0b111111;
        brightness = Math.floorMod(tag.getByte("brightness"), 4);
        powered = tag.getBoolean("powered");
        if (level != null && level.isClientSide) {
            lookVersion++;
            requestModelDataUpdate();
        } else {
            // placed by /setblock, a structure or a piston: send a custom look once (a new lamp sends nothing otherwise)
            lookChanged = customLook();
        }
    }

    /** True when clients need data beyond the defaults of the lamp model. */
    private boolean customLook() {
        LampFinish defaultFinish = getBlockState().getBlock() instanceof LampBlock lamp ? lamp.type().defaultFinish() : finish;
        return finish != defaultFinish || tone != LightTone.AUTO || stripLayout != DEFAULT_STRIP_LAYOUT || connections != 0;
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        finish = input.getOrDefault(ModComponents.FINISH.get(), finish);
        tone = input.getOrDefault(ModComponents.LIGHT_TONE.get(), tone);
        // placed from an item with a finish or a tone: clients only learn it from this first packet
        lookChanged = customLook();
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(ModComponents.FINISH.get(), finish);
        builder.set(ModComponents.LIGHT_TONE.get(), tone);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("finish");
        tag.remove("tone");
    }

    /** Données envoyées avec le chunk : seulement ce qui diffère des valeurs par défaut du modèle. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeLook(tag);
        writeStrip(tag);
        return tag;
    }

    /**
     * Appelé par le serveur à chaque changement d'état du bloc (allumage, luminosité, redstone). Le client garde sa
     * block entity dans ces cas-là : on n'envoie les données que si la finition ou la teinte ont changé
     * (clé, recoloration), au lieu d'un paquet par bascule et par joueur.
     */
    @Override
    public @Nullable ClientboundBlockEntityDataPacket getUpdatePacket() {
        if (!lookChanged) {
            return null;
        }
        lookChanged = false;
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        super.onDataPacket(connection, packet, registries);
        // les teintes sont figées dans le maillage du chunk : on force sa reconstruction
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
        }
    }
}
