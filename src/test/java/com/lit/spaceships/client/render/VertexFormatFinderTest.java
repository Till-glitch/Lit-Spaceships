package com.lit.spaceships.client.render;

import org.junit.jupiter.api.Test;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.lang.reflect.Field;

public class VertexFormatFinderTest {
    @Test
    public void printFormats() {
        for (Field f : DefaultVertexFormat.class.getDeclaredFields()) {
            System.out.println("FIELD: " + f.getName());
        }
    }
}
