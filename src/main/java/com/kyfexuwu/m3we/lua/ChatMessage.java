package com.kyfexuwu.m3we.lua;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class ChatMessage {
    private static final SuperMessage messageSender = new Message();
    public static void message(Text text){
        messageSender.message(text);
    }
    private static class SuperMessage{
        public void message(Text text){}
    }
    private static class Message extends SuperMessage{
        @Override @Environment(EnvType.CLIENT)
        public void message(Text text){
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
        }
    }
}
