package com.nokhxyr.mostlight.block.entity;

import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.block.LampFinish;
import com.nokhxyr.mostlight.block.LightTone;
import com.nokhxyr.mostlight.component.ModComponents;
import com.nokhxyr.mostlight.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Stocke la finition du cadre et la teinte de lumière ; le rendu les lit via les teintes de bloc. */
public class LampBlockEntity extends BlockEntity {
    private LampFinish finish;
    private LightTone tone = LightTone.AUTO;

    public LampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAMP.get(), pos, state);
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
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("finish", finish.getSerializedName());
        tag.putString("tone", tone.getSerializedName());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
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
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        finish = input.getOrDefault(ModComponents.FINISH.get(), finish);
        tone = input.getOrDefault(ModComponents.LIGHT_TONE.get(), tone);
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

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        super.onDataPacket(connection, packet, registries);
        // les teintes sont figées dans le maillage du chunk : on force sa reconstruction
        if (level != null && level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
        }
    }
}
