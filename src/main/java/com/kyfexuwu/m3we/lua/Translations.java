package com.kyfexuwu.m3we.lua;

import com.kyfexuwu.m3we.Utils;
import com.kyfexuwu.m3we.m3we;
import net.minecraft.resource.DefaultResourcePack;
import net.minecraft.resource.Resource;
import net.minecraft.util.math.BlockPos;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

public class Translations {
    public static final boolean OBFUSCATED = !BlockPos.class.getSimpleName().equals("BlockPos");
    public static ClassToken[] classesTranslations;
    public static FieldToken[] fieldsTranslations;
    public static FieldToken[] compFieldsTranslations;
    public static MethodToken[] methodsTranslations;
    public static final Pattern obfuscatedPattern = Pattern.compile("(class|field|method|comp)_\\d+");

    public static MethodToken getToken(Method method){
        String methodName = method.getName();
        if(!OBFUSCATED){
            var maybeMethod = Arrays.stream(methodsTranslations).filter(methodToken ->{
                if(methodToken==null||!methodToken.deobfuscated.equals(method.getName())) return false;

                var cleanedParams = methodToken.paramClasses;
                var actualParams = method.getParameterTypes();

                boolean isCorrect=true;
                if(actualParams.length==cleanedParams.length)
                    for(int i=0;i<cleanedParams.length;i++) {
                        if (!(cleanedParams[i].equals(actualParams[i].getSimpleName()))) {
                            isCorrect = false;
                            break;
                        }
                    }
                else
                    return false;

                return isCorrect;
            }).findFirst();
            if(maybeMethod.isPresent())
                methodName = maybeMethod.get().obfuscated;
        }

        try {
            if(!methodName.startsWith("method_")) throw new Exception("not mapped");

            return Translations.methodsTranslations[Integer.decode(methodName.substring(7))];
        }catch(Exception ignored) {
            var argClasses=method.getParameterTypes();
            String[] args=new String[argClasses.length];
            for(int i=0;i<argClasses.length;i++){
                args[i]="arg"+(i+1)+":"+Utils.deobfuscate(argClasses[i].getSimpleName());
            }

            return new MethodToken("", "", Utils.deobfuscate(method.getReturnType().getSimpleName()),args);
        }
    }

    public static abstract class Token{
        public final String obfuscated;
        public final String deobfuscated;

        public Token(String obfuscated, String deobfuscated) {
            this.obfuscated = obfuscated;
            this.deobfuscated = deobfuscated;
        }
        /*
        public String getJavaName(){
            return OBFUSCATED?this.obfuscated:this.deobfuscated;
        }
         */
    }

    public static class ClassToken extends Token{
        public final String longObfuscated;
        public final String longDeobfuscated;
        public ClassToken(String obfuscated, String deobfuscated,
                          String longObfuscated, String longDeobfuscated) {
            super(obfuscated, deobfuscated);
            this.longObfuscated=longObfuscated;
            this.longDeobfuscated=longDeobfuscated;
        }
        public ClassToken(String... args){
            this(args[0],args[1],args[2],args[3]);
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
    }
    public static class MethodToken extends Token{
        public final String type;
        public final String[] paramNames;
        public final String[] paramClasses;
        public MethodToken(String obfuscated, String deobfuscated, String type, String... params) {
            super(obfuscated, deobfuscated);
            this.type=type;

            this.paramNames=new String[params.length];
            this.paramClasses=new String[params.length];
            for(int i=0;i<params.length;i++){
                String[] paramBroken = params[i].split(":");
                if(paramBroken.length==1){
                    this.paramNames[i] = "arg"+(i+1);
                    this.paramClasses[i] = paramBroken[0];
                }else {
                    this.paramNames[i] = paramBroken[0];
                    this.paramClasses[i] = paramBroken[1];
                }
            }
        }
        public MethodToken(String... args){
            this(args[0],args[1],args[2], Arrays.copyOfRange(args,3,args.length));
        }
    }

    private static boolean inited=false;

    private static <T> List<T> load(String name, Function<String, T> lineParser){
        var resources = new Resource("m3we_mappings", () -> {
            try {
                return DefaultResourcePack.class.getResourceAsStream("/m3we_mappings/"+name+".txt");
            } catch (Exception e) {
                m3we.LOGGER.error("loading error");
                e.printStackTrace();
                throw new FileNotFoundException();
            }
        });

        try (InputStream inputStream = resources.getInputStream()) {
            return Arrays.stream(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).split("\n"))
                    .map(lineParser).toList();
        } catch (Exception e) {
            e.printStackTrace();
            m3we.LOGGER.error("Translations not loaded!! There was an error with the "+name);
            return List.of();
        }
    }
    public static void init(){
        if(inited) return;

        Translations.classesTranslations = load("classes", (str)->{
            var args=str.split(" ");
            return args.length>=2 ? new ClassToken(args) : null;
        }).toArray(new ClassToken[]{});
        Translations.fieldsTranslations = load("fields", (str)->{
            var args=str.split(" ");
            return args.length>=3 ? new FieldToken(args) : null;
        }).toArray(new FieldToken[]{});
        Translations.compFieldsTranslations = load("compfields", (str)->{
            var args=str.split(" ");
            return args.length>=3 ? new FieldToken(args) : null;
        }).toArray(new FieldToken[]{});
        Translations.methodsTranslations = load("methods", (str)->{
            var args=str.split(" ");
            return args.length>=3 ? new MethodToken(args) : null;
        }).toArray(new MethodToken[]{});

        inited=true;
        m3we.LOGGER.info("Translations loaded successfully");
    }
}
