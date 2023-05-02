package com.kyfexuwu.m3we.mixins;

import com.kyfexuwu.m3we.lua.api.DatastoreAPI;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public class DatastoreLoadMixin {
    @Shadow @Final protected LevelStorage.Session session;

    @Inject(method="save", at=@At(value = "RETURN"))
    private void saveDatastoreMixin(boolean suppressLogs, boolean flush, boolean force, CallbackInfoReturnable<Boolean> cir) {
        try {
            NbtIo.write((NbtCompound) DatastoreAPI.table.toNBT().get(), this.session.getDirectory(WorldSavePath.ROOT)
                    .resolve("m3we_datastore.dat").toFile());
        }catch(Exception e){ e.printStackTrace(); }
    }

    @Inject(method="loadWorld", at=@At(value = "RETURN"))
    private void loadDatastoreMixin(CallbackInfo ci) {
        try {
            var file=this.session.getDirectory(WorldSavePath.ROOT)
                    .resolve("m3we_datastore.dat").toFile();
            DatastoreAPI.table = new DatastoreAPI.DatastoreTable();
            if(file.exists())
                DatastoreAPI.DatastoreTable.fromNBTVal(NbtIo.read(this.session.getDirectory(WorldSavePath.ROOT)
                        .resolve("m3we_datastore.dat").toFile()), DatastoreAPI.table);

        }catch(Exception e){ e.printStackTrace(); }
    }
}