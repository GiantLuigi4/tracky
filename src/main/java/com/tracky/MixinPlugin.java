package com.tracky;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
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
	
	UUID trackyUUID = new UUID("tracky".hashCode() * 3427843L, "tracker".hashCode() * 4782347L);
	
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
	private static final String worldClass = "net/minecraft/world/World";
	
	FieldNode targetField;
	
	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		String fieldName = trackyUUID.toString() + " Tracky Forced";
		if (targetClassName.equals(worldClass.replace("/", "."))) {
			// so, the jvm really does not care about the name of a field
			// so I can do stupid stuff like this, and have essentially a 0% chance of anyone ever happening to have the same field name
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
		} else {
			for (MethodNode method : targetClass.methods) {
				if (method.name.equals("getForcedChunks")) {
					String sig = method.signature.substring(method.signature.indexOf(")") + 1);
					System.out.println(sig);
					method.instructions.clear();
					method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
					method.instructions.add(new FieldInsnNode(
							Opcodes.GETFIELD, worldClass,
							fieldName, sig.substring(0, sig.indexOf("<")) + ";"
					));
					method.instructions.add(new InsnNode(Opcodes.ARETURN));
				}
			}
			
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
	}
	
	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
