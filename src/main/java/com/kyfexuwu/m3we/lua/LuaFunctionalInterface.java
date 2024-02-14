package com.kyfexuwu.m3we.lua;

import com.kyfexuwu.m3we.Utils;
import org.luaj.vm2.LuaFunction;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;

public class LuaFunctionalInterface {
    public static boolean isFunctionalInterface(Class<?> clazz){
        return clazz.isInterface() && Arrays.stream(clazz.getDeclaredMethods())
                .filter(m->Modifier.isAbstract(m.getModifiers())).toList().size()==1;
    }
    public static <T> T createFunctionalInterface(LuaFunction func, Class<T> clazz){
        return (T)Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz},
                new FunctionInterfaceInvocationHandler(func));
    }

    private record FunctionInterfaceInvocationHandler(LuaFunction func) implements InvocationHandler {
        public Object invoke(Object proxy, Method m, Object[] args) {
            return Utils.tryAndExecute(null, this.func, args, Utils::toObject);
        }
    }
}
