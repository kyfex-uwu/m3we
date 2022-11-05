package com.kyfexuwu.jsonblocks;

import com.kyfexuwu.jsonblocks.lua.CustomScript;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.util.Identifier;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public class JsonBlocks implements ModInitializer {

    public static String MOD_ID = "m3we";

    public static HashMap<String, Block> jsonBlocks= new HashMap<>();
    public static HashMap<String, Item> jsonItems= new HashMap<>();

    public static File JBFolder =new File(FabricLoader.getInstance().getConfigDir()//just inside the .minecraft folder
            .toString()+"\\m3we");
    public static final ResourcePack m3weResourcePack = new ResourcePack() {
        @Nullable
        @Override
        public InputStream openRoot(String fileName) throws IOException {
            return null;
        }

        @Override
        public InputStream open(ResourceType type, Identifier id) throws IOException {
            return null;
        }

        @Override
        public Collection<Identifier> findResources(ResourceType type, String namespace, String prefix, Predicate<Identifier> allowedPathPredicate) {
            return null;
        }

        @Override
        public boolean contains(ResourceType type, Identifier id) {
            return false;
        }

        @Override
        public Set<String> getNamespaces(ResourceType type) {
            return null;
        }

        @Nullable
        @Override
        public <T> T parseMetadata(ResourceMetadataReader<T> metaReader) throws IOException {
            return null;
        }

        @Override
        public String getName() {
            return MOD_ID;
        }

        @Override
        public void close() {}
    };

    public static final ItemGroup m3weItems = FabricItemGroupBuilder.build(
            new Identifier(MOD_ID,"item_group"),
            ()->new ItemStack(Blocks.BEACON)
    );

    @Override
    public void onInitialize() {
        JBFolder.mkdir();

        File blocksFolder = new File(JBFolder.getAbsolutePath()+"\\blocks");
        blocksFolder.mkdir();
        File itemsFolder = new File(JBFolder.getAbsolutePath()+"\\items");
        itemsFolder.mkdir();
        File scriptsFolder = new File(JBFolder.getAbsolutePath()+"\\scripts");
        scriptsFolder.mkdir();

        initObjects(blocksFolder, JBBlockIniter::blockFromFile,"");
        initObjects(itemsFolder, JBItemIniter::itemFromFile,"");

        //--

        try {
            WatchService watcher = FileSystems.getDefault().newWatchService();
            new File(JsonBlocks.JBFolder + "\\scripts").toPath().register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
            Thread watcherThread = new Thread(null, () -> {
                try {
                    while (true) {
                        WatchKey key = watcher.take();//blocking
                        for (WatchEvent<?> event : key.pollEvents()) {
                            if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                                CustomScript.reloadScript(((Path)event.context()).toString());
                            }
                        }
                        if (!key.reset()) {
                            //ono this is invalid
                            System.out.println("unable to can (this is a bad message)");
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    System.out.println("oof ouch interrupted");
                }
            });
            watcherThread.start();
        } catch (IOException e) {
            System.out.println("something happened to the watcher");
            e.printStackTrace();
        }

        //--

        /*
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("m3wereload")
                .executes(context -> {
                    context.getSource().sendMessage(Text.literal("does nothing rn, might need it later"));

                    return 1;
                })));

         */
    }

    private static void initObjects(File folder,Function<File, Utils.SuccessAndIdentifier> func, String prefix){
        prefix+=folder.getName()+"/";

        for (File modFile : folder.listFiles()) {
            if(modFile.isDirectory())
                initObjects(modFile,func,prefix);

            if(FilenameUtils.isExtension(modFile.getName(),"json")) {
                Utils.SuccessAndIdentifier modObject = func.apply(modFile);
                switch (modObject.successRate) {
                    case CANT_READ -> System.out.println("Can't read file "+prefix+modFile.getName());
                    case BAD_JSON -> System.out.println("Bad JSON in file "+prefix+modFile.getName());
                    case IDK -> System.out.println("Message me on discord KYFEX#3614 and tell me to fix my mod");
                    case YOU_DID_IT -> {
                        System.out.println("Registered file "+prefix+modFile.getName());
                    }
                }
            }
        }
    }
}
