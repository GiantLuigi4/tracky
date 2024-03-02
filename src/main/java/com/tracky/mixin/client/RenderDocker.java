package com.tracky.mixin.client;

import net.minecraft.client.main.Main;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Locale;

@Mixin(Main.class)
public class RenderDocker {

    @Unique
    private static final Logger tracky$LOGGER = LoggerFactory.getLogger("Tracky::DEBUG");
    @Unique
    private static final int ENABLE_TIME = 4000;

    @Inject(method = "main", at = @At("HEAD"), remap = false)
    private static void preMain(String[] pArgs, CallbackInfo ci) {
        if (FMLEnvironment.production) return;

        String pth = System.getProperty("java.library.path");
        String name = System.mapLibraryName("renderdoc");
        boolean rdDetected = false;
        for (String s : pth.split(";")) {
            if (new File(s + "/" + name).exists()) {
                rdDetected = true;
                break;
            }
        }

        if (!rdDetected) {
            return;
        }

        boolean[] doEnable = new boolean[]{false};

        Thread td = new Thread(() -> {
            tracky$LOGGER.warn("Renderdoc detected, would you like to load it? y/N");

            long start = System.currentTimeMillis();
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            while (System.currentTimeMillis() - start <= ENABLE_TIME) {
                try {
                    if (reader.ready()) {
                        String ln = reader.readLine().trim().toLowerCase(Locale.ROOT);
                        if (ln.startsWith("y")) {
                            doEnable[0] = true;
                            return;
                        } else if (ln.startsWith("n")) {
                            return;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }, "Tracky-RenderDocker");

        td.setDaemon(true);
        td.start();

        try {
            try {
                // We just need to wait for the thread to stop
                td.join();
            } catch (Throwable ignored) {
            }

            if (doEnable[0]) {
                System.loadLibrary("renderdoc");
            }
        } catch (Throwable ignored) {
        }
    }
}