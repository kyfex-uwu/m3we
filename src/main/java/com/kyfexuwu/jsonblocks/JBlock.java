package com.kyfexuwu.jsonblocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.*;
import net.minecraft.util.math.Direction;

public class JBlock extends Block {

    public enum PropType{
        BOOL,
        INT,
        ENUM,
        DIRECTION
    }
    public static class PropData{
        public String name;
        public PropType type;

        public int min=0;
        public int max=1;

        PropData(String name, PropType type){
            this.name=name;
            this.type=type;
        }

        public static PropData bool(String name){
            return new PropData(name,PropType.BOOL);
        }
        public static PropData direction(String name){
            //need to change so it only goes with certain values
            return new PropData(name,PropType.DIRECTION);
        }
        public static PropData num(String name, int min, int max){
            var toReturn = new PropData(name,PropType.INT);
            toReturn.min=min;
            toReturn.max=max;
            return toReturn;
        }
    }

    public PropData[] propData;
    public Property[] properties;
    public JBlock(Settings settings, PropData[] propData) {
        super(settings);
        this.propData=propData;
        this.properties = new Property[this.propData.length];
        var defaultState = getStateManager().getDefaultState();
        for(int i=0;i<this.propData.length;i++){
            var property = this.propData[i];

            switch (property.type) {
                case INT -> {
                    this.properties[i] = IntProperty.of(property.name, property.min, property.max);
                    defaultState.with((IntProperty)this.properties[i], property.min);
                }
                case BOOL -> {
                    this.properties[i] = BooleanProperty.of(property.name);
                    defaultState.with((BooleanProperty)this.properties[i], false);
                }
                case DIRECTION -> {
                    this.properties[i] = DirectionProperty.of(property.name);
                    defaultState.with((DirectionProperty)this.properties[i], Direction.UP);
                }
            }
        }
        setDefaultState(defaultState);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        for(Property property : this.properties){
            builder.add(property);
        }
    }
}
