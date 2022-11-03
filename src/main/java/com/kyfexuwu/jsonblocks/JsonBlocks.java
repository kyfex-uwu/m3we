package com.kyfexuwu.jsonblocks;

import com.kyfexuwu.jsonblocks.lua.CustomScript;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.function.Function;

public class JsonBlocks implements ModInitializer {

    public static String MOD_ID = "m3we";

    public static HashMap<String, Block> jsonBlocks= new HashMap<>();
    public static HashMap<String, Item> jsonItems= new HashMap<>();

    public static File JBFolder =new File(FabricLoader.getInstance().getConfigDir()//just inside the .minecraft folder
            .toString()+"\\m3we");

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

        initObjects(blocksFolder, JBBlockIniter::blockFromFile);
        initObjects(itemsFolder, JBItemIniter::itemFromFile);

        //--

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
            new SimpleSynchronousResourceReloadListener() {
                @Override
                public void reload(ResourceManager manager) {
                    //todo
                }

                @Override
                public Identifier getFabricId() {
                    return new Identifier(MOD_ID,"m3we_resources");
                }
            });

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

    private static void initObjects(File folder, Function<File, Utils.SuccessRate> func){
        initObjects(folder,func,"");
    }
    private static void initObjects(File folder, Function<File, Utils.SuccessRate> func, String prefix){
        prefix+=folder.getName()+"/";

        for (File modFile : folder.listFiles()) {
            if(modFile.isDirectory())
                initObjects(modFile,func,prefix);

            if(FilenameUtils.isExtension("json")) {
                switch (func.apply(modFile)) {
                    case CANT_READ -> System.out.println("Can't read file "+prefix+modFile.getName());
                    case BAD_JSON -> System.out.println("Bad JSON in file "+prefix+modFile.getName());
                    case IDK -> System.out.println("Message me on discord KYFEX#3614 and tell me to fix my mod");
                    case YOU_DID_IT -> System.out.println("Registered file "+prefix+modFile.getName());
                }
            }
        }
    }
}
