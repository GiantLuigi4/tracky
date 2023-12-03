package com.tracky.mixin.client.impl.vanilla;

import com.mojang.blaze3d.shaders.Uniform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.nio.FloatBuffer;

@Mixin(Uniform.class)
public interface UniformAccessor {

	@Accessor
	FloatBuffer getFloatValues();

	@Invoker
	void invokeMarkDirty();
}
