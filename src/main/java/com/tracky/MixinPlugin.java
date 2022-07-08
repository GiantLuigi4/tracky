package com.tracky;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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
	private static final String playerClass = "net/minecraft/entity/Player";
	
	FieldNode targetField;
	
	private static final String type = "java/util/Function<L" + playerClass + ";Ljava/lang/Iterable<L"+ chunkPosClass+";>;";
	
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
			
			targetClass.fields.add(targetField = new FieldNode(
					// synthetic hides it from the decompiler
					Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
					fieldName,
					"Ljava/util/Map;",
					"Ljava/util/Map<Ljava/util/UUID;L" + type + ";>;",
					null
			));
			isMainTracky = true;
			for (MethodNode method : targetClass.methods) {
				if (method.name.equals("<init>")) {
					ArrayList<AbstractInsnNode> targets = new ArrayList<>();
					for (AbstractInsnNode instruction : method.instructions) {
						if (instruction.getOpcode() == Opcodes.RETURN) {
							targets.add(instruction);
						}
					}
					for (AbstractInsnNode target : targets) {
						InsnList list = new InsnList();
						String des = targetField.desc.substring(1, targetField.desc.length() - 1);
						list.add(new VarInsnNode(Opcodes.ALOAD, 0));
						list.add(new TypeInsnNode(Opcodes.NEW, des.replace("Map", "HashMap")));
						list.add(new InsnNode(Opcodes.DUP));
						list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, des.replace("Map", "HashMap"), "<init>", "()V"));
						AbstractInsnNode insn = new FieldInsnNode(Opcodes.PUTFIELD, targetClassName.replace(".", "/"), targetField.name, targetField.desc);
						list.add(insn);
						method.instructions.insertBefore(target, list);
					}
				}
			}
			
			dump(targetClass);
		} else {
			for (MethodNode method : targetClass.methods) {
				if (method.name.equals("getForcedChunks")) {
					String sig = method.signature.substring(method.signature.indexOf(")") + 1);
					method.instructions.clear();
					method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
					String desc = sig.substring(0, sig.indexOf("<")) + ";";
					method.instructions.add(new FieldInsnNode(
							Opcodes.GETFIELD, worldClass,
							fieldName, desc
					));
					method.instructions.add(new InsnNode(Opcodes.ARETURN));
					method.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;
				} else if (method.name.equals("<init>")) {
					method.access = method.access | Opcodes.ACC_SYNTHETIC;
				}
			}
			
			dump(targetClass);
		}
	}
	
	public void dump(ClassNode clazz) {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		clazz.accept(writer);
		try {
			File f = new File("dmp/" + clazz.name + ".class");
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
