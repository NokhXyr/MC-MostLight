package com.nokhxyr.mostlight.link;

import com.nokhxyr.mostlight.component.ModComponents;
import com.nokhxyr.mostlight.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Mémorise les lampes liées à un interrupteur posé. */
public class SwitchBlockEntity extends BlockEntity {
    private List<BlockPos> links = List.of();

    public SwitchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SWITCH.get(), pos, state);
    }

    public List<BlockPos> links() {
        return links;
    }

    public void setLinks(List<BlockPos> links) {
        this.links = List.copyOf(links);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (BlockPos pos : links) {
            list.add(NbtUtils.writeBlockPos(pos));
        }
        tag.put("links", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        List<BlockPos> loaded = new ArrayList<>();
        ListTag list = tag.getList("links", Tag.TAG_INT_ARRAY);
        for (Tag entry : list) {
            if (entry instanceof net.minecraft.nbt.IntArrayTag array && array.size() == 3) {
                loaded.add(new BlockPos(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt()));
            }
        }
        links = List.copyOf(loaded);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        links = List.copyOf(input.getOrDefault(ModComponents.LINKS.get(), List.of()));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (!links.isEmpty()) {
            builder.set(ModComponents.LINKS.get(), links);
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("links");
    }
}
