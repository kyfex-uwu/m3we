package com.kyfexuwu.m3we;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.Translations;
import com.kyfexuwu.m3we.luablock.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.metadata.ResourceMetadataReader;
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

public class m3we implements ModInitializer {
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

    public static HashMap<String, Block> jsonBlocks= new HashMap<>();
    public static HashMap<String, Item> jsonItems= new HashMap<>();

    public static File JBFolder = new File(FabricLoader.getInstance().getConfigDir()//just inside the .minecraft folder
            .toString()+"\\m3we");
    public static File blocksFolder = new File(JBFolder.getAbsolutePath()+"\\blocks");
    public static File itemsFolder = new File(JBFolder.getAbsolutePath()+"\\items");
    public static File scriptsFolder = new File(JBFolder.getAbsolutePath()+"\\scripts");

    public static final ItemGroup m3weItems = FabricItemGroupBuilder.build(
            new Identifier(MOD_ID,"item_group"),
            ()->new ItemStack(Blocks.BEACON)
    );

    public static final Identifier updateLuaBlockPacket = new Identifier("m3we","update_lua_block");

    @Override
    public void onInitialize() {
        /* //for enums
        for(Field field : .class.getDeclaredFields())
            if(field.getName().toUpperCase().equals(field.getName()))
                System.out.print("\""+field.getName()+"\", ");
         */

        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener(){

            @Override
            public void reload(ResourceManager manager) {
                for(var ooga : manager.findResources("m3we_mappings",(id)->true).entrySet()){
                    try {
                        var toAssign=
                                new String(ooga.getValue().getInputStream().readAllBytes()).split("\n");
                        switch (ooga.getKey().getPath().substring("m3we_mappings/".length())) {
                            case "classes.txt" -> Translations.classesTranslations = Arrays.stream(toAssign)
                                    .map(Translations.ClassToken::fromStr)
                                    .toList().toArray(Translations.ClassToken[]::new);
                            case "fields.txt" -> Translations.fieldsTranslations = Arrays.stream(toAssign)
                                    .map(Translations.FieldToken::fromStr)
                                    .toList().toArray(Translations.FieldToken[]::new);
                            case "compfields.txt" -> Translations.compFieldsTranslations = Arrays.stream(toAssign)
                                    .map(Translations.FieldToken::fromStr)
                                    .toList().toArray(Translations.FieldToken[]::new);
                            case "methods.txt" -> Translations.methodsTranslations = Arrays.stream(toAssign)
                                    .map(Translations.MethodToken::fromStr)
                                    .toList().toArray(Translations.MethodToken[]::new);
                            default -> System.out.println("what do i do with this mapping file "
                                    +ooga.getKey().getPath());
                        }
                    } catch (IOException ignored) { }
                }
                System.out.println("Loaded m3we mappings");
            }

            @Override
            public Identifier getFabricId() {
                return new Identifier(MOD_ID,"m3we_mappings");
            }
        });

        JBFolder.mkdir();
        blocksFolder.mkdir();
        itemsFolder.mkdir();
        scriptsFolder.mkdir();

        initObjects(blocksFolder, BlockIniter::blockFromFile,"");
        initObjects(itemsFolder, ItemIniter::itemFromFile,"");

        //--

        try {
            WatchService watcher = FileSystems.getDefault().newWatchService();
            scriptsFolder.toPath().register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
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
        public <T> T parseMetadata(ResourceMetadataReader<T> metaReader) {
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
