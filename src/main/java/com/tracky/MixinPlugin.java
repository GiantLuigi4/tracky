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
	protected static boolean isMainTracky = false;
	
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
	private static final String clientWorldClass = "net/minecraft/client/world/ClientWorld";
	private static final String playerClass = "net/minecraft/entity/Player";
	
	FieldNode targetField;
	FieldNode renderField;
	
	private static final String type = "java/util/Function<L" + playerClass + ";L" + chunkPosClass + ";>";
	private static final String typeClient = "java/util/Supplier<L" + chunkPosClass + ";>";
	private static final String typeServer = "java/util/Map<Ljava/util/UUID;L" + type + ";>";
	
	public FieldNode injectField(ClassNode targetClass, String fieldName, boolean isMainCheck, boolean isMap) {
		for (FieldNode field : targetClass.fields) {
			if (field.name.equals(fieldName)) {
				if (isMainCheck)
					isMainTracky = false;
				targetField = field;
				return field;
			}
		}
		
		FieldNode nd;
		targetClass.fields.add(nd = new FieldNode(
				// synthetic hides it from the decompiler
				Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
				fieldName,
				"Ljava/util/Map;",
				isMap ? "L" + typeServer + ";" : "L" + typeClient + ";",
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
					String des = nd.desc.substring(1, nd.desc.length() - 1);
					list.add(new VarInsnNode(Opcodes.ALOAD, 0));
					list.add(new TypeInsnNode(Opcodes.NEW, des.replace("Map", "HashMap")));
					list.add(new InsnNode(Opcodes.DUP));
					list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, des.replace("Map", "HashMap"), "<init>", "()V"));
					AbstractInsnNode insn = new FieldInsnNode(Opcodes.PUTFIELD, targetClass.name.replace(".", "/"), nd.name, nd.desc);
					list.add(insn);
					method.instructions.insertBefore(target, list);
				}
			}
		}
		
		dump(targetClass);
		
		return nd;
	}
	
	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		// so, the jvm really does not care about the name of a field
		// so I can do stupid stuff like this, and have essentially a 0% chance of anyone ever happening to have the same field name
		// however, this also guarantees that there is a 100% certainty that all trackies use the same central field
		// this makes it possible to make it so that only the main tracky does stuff
		String fieldName = trackyUUID.toString() + " Tracky Forced";
		// idk why intelliJ complained about the "toString" on the one below but not the one above, but ok
		String renderFieldName = trackyUUID + " Tracky Rendered";
		if (targetClassName.equals(clientWorldClass.replace("/", "."))) {
			renderField = injectField(targetClass, renderFieldName, false, false);
		} else if (targetClassName.equals(worldClass.replace("/", "."))) {
			targetField = injectField(targetClass, fieldName, true, true);
		} else {
			for (MethodNode method : targetClass.methods) {
				FieldNode targ = null;
				String owner = null;
				switch (method.name) {
					case "getForcedChunks": {
						targ = targetField;
						owner = worldClass;
						break;
					}
					case "getRenderedChunks": {
						targ = renderField;
						owner = clientWorldClass;
						break;
					}
					case "<init>":
						method.access = method.access | Opcodes.ACC_SYNTHETIC;
						break;
				}
				if (targ != null) {
					String sig = method.signature.substring(method.signature.indexOf(")") + 1);
					method.instructions.clear();
					method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
					String desc = sig.substring(0, sig.indexOf("<")) + ";";
					method.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, owner));
					method.instructions.add(new FieldInsnNode(
							Opcodes.GETFIELD, owner,
							targ.name, desc
					));
					method.instructions.add(new InsnNode(Opcodes.ARETURN));
					method.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
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
			if (!f.exists()) f.getParentFile().mkdirs(); // how do I suppress ignored result?
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
