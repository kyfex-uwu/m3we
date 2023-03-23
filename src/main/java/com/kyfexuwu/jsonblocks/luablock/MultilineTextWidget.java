package com.kyfexuwu.jsonblocks.luablock;

import net.minecraft.SharedConstants;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public class MultilineTextWidget extends TextFieldWidget {
    public MultilineTextWidget(TextRenderer textRenderer, int x, int y, int width, int height, Text text) {
        super(textRenderer, x, y, width, height, text);
    }

    public MultilineTextWidget(TextRenderer textRenderer, int x, int y, int width, int height, @Nullable TextFieldWidget copyFrom, Text text) {
        super(textRenderer, x, y, width, height, copyFrom, text);
    }

    static Method widenedIsEditable;
    boolean widenedIsEditable(){
        try{
            return (boolean) widenedIsEditable.invoke(this);
        }catch(Exception e){
            return false;
        }
    }
    static{
        try {
            widenedIsEditable=TextFieldWidget.class.getDeclaredMethod("isEditable");
            widenedIsEditable.setAccessible(true);
        }catch(Exception ignored){}
    }
    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!this.isActive()) {
            return false;
        }
        if (SharedConstants.isValidChar(chr)||chr == '\n') {
            if (this.widenedIsEditable()) {
                this.write(Character.toString(chr));
            }
            return true;
        }
        return false;
    }
}
