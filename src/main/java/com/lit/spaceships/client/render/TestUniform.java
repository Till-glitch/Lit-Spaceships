package com.peaceman.alpha.client.render;

import com.mojang.blaze3d.shaders.Uniform;

public class TestUniform {
    public void test(Uniform uniform) {
        float[] arr = new float[64];
        uniform.set(arr);
    }
}
