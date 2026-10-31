package com.cerbon.cerbons_api.neoforge.attachment.saved_data;

import com.cerbon.cerbons_api.api.general.event.EventScheduler;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class LevelEventScheduler extends SavedData {
    private static final Codec<LevelEventScheduler> CODEC = CompoundTag.CODEC.xmap(
            tag -> new LevelEventScheduler(),
            scheduler -> new CompoundTag()
    );
    public static final SavedDataType<LevelEventScheduler> TYPE = new SavedDataType<>(
            Identifier.withDefaultNamespace("event_scheduler"), LevelEventScheduler::new, CODEC
    );

    public static LevelEventScheduler INSTANCE;
    private EventScheduler eventScheduler;

    public LevelEventScheduler() {}

    public static EventScheduler get(Level level) {
        LevelEventScheduler levelEventScheduler = getSavedData(level);

        if (levelEventScheduler.eventScheduler == null)
            levelEventScheduler.eventScheduler = new EventScheduler();

        return levelEventScheduler.eventScheduler;
    }

    private static LevelEventScheduler getSavedData(Level level) {
        return level.isClientSide() ? INSTANCE : ((ServerLevel) level).getDataStorage().computeIfAbsent(TYPE);
    }
}
