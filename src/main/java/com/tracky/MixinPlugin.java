package com.tracky;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MixinPlugin implements IMixinConfigPlugin {
	boolean isMainTracky = false;
	
	UUID trackyUUID = new UUID("tracky".hashCode() * 3427843L, "unique".hashCode() * 4782347L);
	
	@Override
	public void onLoad(String mixinPackage) {
	}
	
	@Override
	public String getRefMapperConfig() {
		return null;
	}
	
	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return true;
	}
	
	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}
	
	@Override
	public List<String> getMixins() {
		return null;
	}
	
	private static final String chunkPosClass = "net/minecraft/util/math/ChunkPos";
	
	FieldNode targetField;
	
	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		String fieldName = trackyUUID.toString().replace("-", "!") + "TrackyForced";
		for (FieldNode field : targetClass.fields) {
			if (field.name.equals(fieldName)) {
				isMainTracky = false;
				targetField = field;
				return;
			}
		}
		
		targetClass.fields.add(new FieldNode(
				Opcodes.ACC_PUBLIC,
				fieldName,
				"Ljava/util/ArrayList;",
				"Ljava/util/ArrayList<L" + chunkPosClass + ";>;",
				null
		));
		
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		targetClass.accept(writer);
		try {
			File f = new File("dmp/" + targetClass.name + ".class");
			if (!f.exists()) f.getParentFile().mkdirs();
			FileOutputStream outputStream = new FileOutputStream(f);
			byte[] bytes = writer.toByteArray();
			outputStream.write(bytes);
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
