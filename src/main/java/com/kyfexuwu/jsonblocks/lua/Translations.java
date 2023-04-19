package com.kyfexuwu.jsonblocks.lua;

import net.minecraft.client.MinecraftClient;

import java.util.regex.Pattern;

public class Translations {
    public static final boolean OBFUSCATED = !MinecraftClient.class.getSimpleName().equals("MinecraftClient");
    public static ClassToken[] classesTranslations;
    public static FieldToken[] fieldsTranslations;
    public static FieldToken[] compFieldsTranslations;
    public static MethodToken[] methodsTranslations;
    public static final Pattern obfuscatedPattern = Pattern.compile("(class|field|method|comp)_\\d+");

    public static abstract class Token{
        public final String obfuscated;
        public final String deobfuscated;

        public Token(String obfuscated, String deobfuscated) {
            this.obfuscated = obfuscated;
            this.deobfuscated = deobfuscated;
        }
        public String getJavaName(){
            return OBFUSCATED?this.obfuscated:this.deobfuscated;
        }
    }

    public static class ClassToken extends Token{
        public ClassToken(String obfuscated, String deobfuscated) {
            super(obfuscated, deobfuscated);
        }
        public ClassToken(String... args){
            this(args[0],args[1]);
        }
        public static ClassToken fromStr(String str){
            if(str.length()==0) return null;
            return new ClassToken(str.split(" "));
        }
    }
    public static class FieldToken extends Token{
        public final String type;
        public FieldToken(String obfuscated, String deobfuscated, String type) {
            super(obfuscated, deobfuscated);
            this.type=type;
        }
        public FieldToken(String... args){
            this(args[0],args[1],args[2]);
        }
        public static FieldToken fromStr(String str){
            if(str.length()==0) return null;
            return new FieldToken(str.split(" "));
        }
    }
    public static class MethodToken extends Token{
        public final String type;
        public MethodToken(String obfuscated, String deobfuscated, String type) {
            super(obfuscated, deobfuscated);
            this.type=type;
        }
        public MethodToken(String... args){
            this(args[0],args[1],args[2]);
        }
        public static MethodToken fromStr(String str){
            if(str.length()==0) return null;
            return new MethodToken(str.split(" "));
        }
    }
}
