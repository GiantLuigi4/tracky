package com.tracky.mixin;

import com.tracky.Tracky;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Set;

public class TrackyMixinPlugin implements IMixinConfigPlugin {

    private boolean sodium;

    @Override
    public void onLoad(String mixinPackage) {
        this.sodium = FMLLoader.getLoadingModList().getModFileById("rubidium") != null;

        if (this.sodium) {
            Tracky.LOGGER.info("Using Sodium Renderer");
        } else {
            Tracky.LOGGER.info("Using Vanilla Renderer");
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith("com.tracky.mixin.client.impl")) {
            return this.sodium ? mixinClassName.startsWith("com.tracky.mixin.client.impl.sodium") : mixinClassName.startsWith("com.tracky.mixin.client.impl.vanilla");
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (!FMLLoader.isProduction() || !Tracky.ENABLE_TEST) {
            return;
        }

        try {
            File folder = new File("mixin-debug");
            folder.mkdirs();
            FileOutputStream outputStream = new FileOutputStream(new File(folder, targetClass.name.substring(targetClass.name.lastIndexOf("/") + 1) + "-pre.class"));
            ClassWriter writer = new ClassWriter(0);
            targetClass.accept(writer);
            outputStream.write(writer.toByteArray());
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (!FMLLoader.isProduction() || !Tracky.ENABLE_TEST) {
            return;
        }

        try {
            File folder = new File("mixin-debug");
            folder.mkdirs();
            FileOutputStream outputStream = new FileOutputStream(new File(folder, targetClass.name.substring(targetClass.name.lastIndexOf("/") + 1) + "-post.class"));
            ClassWriter writer = new ClassWriter(0);
            targetClass.accept(writer);
            outputStream.write(writer.toByteArray());
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
