package com.kyfexuwu.jsonblocks;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.kyfexuwu.jsonblocks.lua.CustomScript;
import com.kyfexuwu.jsonblocks.luablock.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.registry.Registry;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class JsonBlocks implements ModInitializer {

    public static String MOD_ID = "m3we";

    public static Block luaBlock = new LuaBlock(FabricBlockSettings.copyOf(Blocks.COMMAND_BLOCK));
    public static final BlockEntityType<LuaBlockEntity> luaBlockEntity = Registry.register(
            Registry.BLOCK_ENTITY_TYPE,
            new Identifier(MOD_ID, "lua_block"),
            FabricBlockEntityTypeBuilder.create(LuaBlockEntity::new,luaBlock).build()
    );
    static{
        Registry.register(Registry.BLOCK, new Identifier(MOD_ID, "lua_block"),luaBlock);
        Registry.register(Registry.ITEM, new Identifier(MOD_ID, "lua_block"),
                new BlockItem(luaBlock, new FabricItemSettings()));
        //.group(ItemGroup.OPERATOR)
    }
    public static final ScreenHandlerType<LuaBlockEntity.LuaBlockScreenHandler> luaBlockScreenHandler =
            Registry.register(Registry.SCREEN_HANDLER, new Identifier(MOD_ID, "lua_block"),
            LuaBlockEntity.LuaBlockScreenHandlerType);

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
        /* //for enums
        for(Field field : .class.getDeclaredFields())
            if(field.getName().toUpperCase().equals(field.getName()))
                System.out.print("\""+field.getName()+"\", ");
         */

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
                    case YOU_DID_IT -> packNamespaces.add(modObject.identifier.getNamespace());
                }
            }
        }
    }

    public static File m3weResources = new File(JBFolder.getAbsolutePath()+"\\resources");
    static{ m3weResources.mkdir(); }
    public static final JsonObject packMetadata = JsonHelper.deserialize("{" +
        "\"pack\":{" +
            "\"pack_format\":9," +
            "\"description\":\""+MOD_ID+" resources\"" +
        "}" +
    "}");
    public static Set<String> packNamespaces = new HashSet<>();
    public static HashMap<String,String> packFiles = new HashMap<>();
    private static void crawlResources(File folder, String prefix, String namespace, String origFilePath){
        for (File modFile : folder.listFiles()) {
            if(modFile.isDirectory()) {
                crawlResources(modFile, prefix+modFile.getName()+"/", namespace, origFilePath);
            }else{
                packFiles.put(namespace+":"+prefix+modFile.getName(),origFilePath+prefix+modFile.getName());
            }
        }
    }
    static{
        m3weResources.mkdir();
        for (File packDir : m3weResources.listFiles()) {
            if(packDir.isDirectory()&&
                Arrays.asList(packDir.list()).contains("assets")) {
                for(File resourceDir : new File(packDir.getAbsolutePath()+"\\assets").listFiles()){
                    crawlResources(resourceDir,"", resourceDir.getName(),
                            packDir.getName()+"/assets/"+resourceDir.getName()+"/");
                }
            }
        }
    }
    public static final ResourcePack m3weResourcePack = new ResourcePack() {
        @Nullable
        @Override//done
        public InputStream openRoot(String fileName){
            if (!fileName.contains("/") && !fileName.contains("\\")) {
                return null;
                //literally no clue what this is
            } else {
                throw new IllegalArgumentException("Root resources can only be filenames, not paths (no / allowed!)");
            }
        }

        @Override//done
        public InputStream open(ResourceType type, Identifier id) throws IOException {
            return new FileInputStream(m3weResources.getAbsolutePath()+"/"+packFiles.get(id.toString()));
        }

        @Override//done
        public Collection<Identifier> findResources(ResourceType type, String namespace, String prefix, Predicate<Identifier> allowedPathPredicate) {
            //System.out.println(namespace+"\n"+prefix+"\n"+allowedPathPredicate.toString());
            //todo: this asks for a font and a realms main screen, maybe i add?
            return Lists.newArrayList();
        }

        @Override//done
        public boolean contains(ResourceType type, Identifier id){
            if(type==ResourceType.SERVER_DATA) return false;

            return packFiles.containsKey(id.toString());
        }

        @Override//done
        public Set<String> getNamespaces(ResourceType type) {
            return packNamespaces;
        }

        @Nullable
        @Override//done
        public <T> T parseMetadata(ResourceMetadataReader<T> metaReader) throws IOException {
            if(!packMetadata.has(metaReader.getKey())) return null;
            return metaReader.fromJson(JsonHelper.getObject(packMetadata, metaReader.getKey()));
        }

        @Override//done
        public String getName() {
            return MOD_ID;
        }

        @Override//done
        public void close() {}
    };
}
