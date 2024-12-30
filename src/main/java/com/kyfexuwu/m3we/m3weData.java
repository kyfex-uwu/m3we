package com.kyfexuwu.m3we;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;

public class m3weData {
    public static final JsonObject packMetadata = JsonHelper.deserialize("{" +
            "\"pack\":{" +
            "\"pack_format\":9," +
            "\"description\":\""+m3we.MOD_ID+" resources\"" +
            "}" +
        "}");
    public static Set<String> packNamespaces = new HashSet<>();
    public static HashMap<String,String> packResources = new HashMap<>();
    public static HashMap<String,String> packData = new HashMap<>();
    public static void crawlResources(File folder, String prefix, String namespace, String origFilePath, boolean isResource){
        for (File modFile : folder.listFiles()) {
            if(modFile.isDirectory()) {
                crawlResources(modFile, prefix+modFile.getName()+"/", namespace, origFilePath, isResource);
            }else{
                var toPut = isResource?packResources:packData;
                toPut.put(namespace+":"+prefix+modFile.getName(),
                        origFilePath+prefix+modFile.getName());
            }
        }
    }
    public static final ResourcePack resourcePack = new ResourcePack() {
        private HashMap<String, String> getPack(ResourceType type){
            return type==ResourceType.CLIENT_RESOURCES?packResources:packData;
        }

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
            for(var folder : m3we.resourceFolders) {
                var maybeFile = new File(folder.getRight().getAbsolutePath()+getPack(type).get(id.toString()));
                if(maybeFile.exists())
                    return new FileInputStream(maybeFile);
            }

            throw new IOException("file not found :( "+type+" "+id);
        }

        @Override//done
        public Collection<Identifier> findResources(ResourceType type, String namespace, String prefix, Predicate<Identifier> allowedPathPredicate) {
            Collection<Identifier> toReturn = Lists.newArrayList();
            getPack(type).forEach((key,path)->{
                if(key.startsWith(namespace+":"+prefix)){
                    toReturn.add(new Identifier(key));
                }
            });
            return toReturn;
        }

        @Override//done
        public boolean contains(ResourceType type, Identifier id){
            return getPack(type).containsKey(id.toString());
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
            return m3we.MOD_ID+" Pack";
        }

        @Override//done
        public void close() {}
    };
}
