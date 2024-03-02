package com.tracky.mixin.client.impl.sodium;

import me.jellysquid.mods.sodium.client.gl.shader.ShaderConstants;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderParser;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.regex.Pattern;

@Mixin(value = ShaderParser.class, remap = false)
public class ShaderParserMixin {

    @Unique
    private static final Pattern METHOD_PATTERN = Pattern.compile("v_FragDistance = getFragDistance\\(u_FogShape, position\\);");

    @Inject(method = "parseShader(Ljava/lang/String;Lme/jellysquid/mods/sodium/client/gl/shader/ShaderConstants;)Ljava/lang/String;", at = @At("RETURN"), cancellable = true)
    private static void modifySource(String src, ShaderConstants constants, CallbackInfoReturnable<String> cir) {
        // We have to include the modelview matrix in the fog calculation because of shells
        cir.setReturnValue(METHOD_PATTERN.matcher(cir.getReturnValue()).replaceAll("v_FragDistance = getFragDistance(u_FogShape, u_FogShape != FOG_SHAPE_CYLINDRICAL ? (u_ModelViewMatrix * vec4(position, 1.0)).xyz : position);"));
    }
}
