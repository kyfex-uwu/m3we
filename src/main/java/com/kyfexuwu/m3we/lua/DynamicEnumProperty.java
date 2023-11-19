package com.kyfexuwu.m3we.lua;

import com.google.common.collect.ImmutableSet;
import net.minecraft.state.property.Property;

import java.util.*;

public class DynamicEnumProperty extends Property<String> {
    private final ImmutableSet<String> values;

    protected DynamicEnumProperty(String name, String[] values) {
        super(name, String.class);

        this.values=ImmutableSet.copyOf(values);
    }

    @Override
    public Collection<String> getValues() {
        return this.values;
    }

    @Override
    public String name(String value) {
        return value;
    }

    @Override
    public Optional<String> parse(String name) {
        return this.values.contains(name) ? Optional.ofNullable(name) : Optional.empty();
    }

    @Override
    public int computeHashCode() {
        return 31 * super.computeHashCode() + this.values.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof DynamicEnumProperty prop) {
            for(String value : this.values)
                if (!prop.values.contains(value)) return false;
        }else{
            return false;
        }
        return true;
    }

    //--

    public static DynamicEnumProperty of(String name, String[] values){
        return new DynamicEnumProperty(name,values);
    }
}
