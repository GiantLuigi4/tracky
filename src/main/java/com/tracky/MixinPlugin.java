package com.tracky;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.*;

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
	
	private static final String chunkPosClass = "net/minecraft/world/level/ChunkPos";
	private static final String worldClass = "net/minecraft/world/level/Level";
	private static final String clientWorldClass = "net/minecraft/client/multiplayer/ClientLevel";
	private static final String playerClass = "net/minecraft/world/entity/Player";
	private static final String sharedConstantsClass = "net/minecraft/SharedConstants";
	
	FieldNode targetField;
	FieldNode renderField;
	FieldNode versionsField;
	
	private static final String type = "java/util/Function<L" + playerClass + ";Ljava/lang/Iterable<L" + chunkPosClass + ";>;";
	private static final String typeClient = "java/util/Map<Ljava/util/UUID;Ljava/util/Supplier<L" + chunkPosClass + ";>;>";
	private static final String typeServer = "java/util/Map<Ljava/util/UUID;L" + type + ";>";
	private static final String typeVersion = "java/util/Map<Ljava/lang/String;Ljava/lang/String;>";
	
	public FieldNode injectField(ClassNode targetClass, String fieldName, boolean isMap) {
		for (FieldNode field : targetClass.fields) {
			if (field.name.equals(fieldName)) {
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
	
	protected static final String VERSION = "0.0.1";
	
	private void injectVersionInjection(ClassNode clazz) {
		for (MethodNode method : clazz.methods) {
			if (method.name.equals("<clinit>")) {
				ArrayList<AbstractInsnNode> targets = new ArrayList<>();
				for (AbstractInsnNode instruction : method.instructions) {
					if (instruction.getOpcode() == Opcodes.RETURN) {
						targets.add(instruction);
					}
				}
				for (AbstractInsnNode target : targets) {
					InsnList list = new InsnList();
					list.add(new FieldInsnNode(Opcodes.GETSTATIC, clazz.name, versionsField.name, versionsField.desc));
					list.add(new LdcInsnNode(MixinPlugin.class.toString()));
					list.add(new LdcInsnNode(VERSION));
					list.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"));
					method.instructions.insertBefore(target, list);
				}
			}
		}
	}
	
	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		if (!mixinClassName.contains("dummy")) {
			// no reason to process it if it is not a dummy mixin
			return;
		}
		// so, the jvm really does not care about the name of a field
		// so I can do stupid stuff like this, and have essentially a 0% chance of anyone ever happening to have the same field name
		// however, this also guarantees that there is a 100% certainty that all trackies use the same central field
		// this makes it possible to make it so that only the main tracky does stuff
		String fieldName = trackyUUID.toString() + " Tracky Forced";
		// idk why intelliJ complained about the "toString" on the one below but not the one above, but ok
		String renderFieldName = trackyUUID + " Tracky Rendered";
		String versionFieldName = trackyUUID + " Tracky Versions";
		if (targetClassName.equals(clientWorldClass.replace("/", "."))) {
			renderField = injectField(targetClass, renderFieldName, false);
		} else if (targetClassName.equals(worldClass.replace("/", "."))) {
			targetField = injectField(targetClass, fieldName, true);
		} else {
			if (targetClassName.equals(sharedConstantsClass.replace("/", "."))) {
				for (FieldNode field : targetClass.fields) {
					if (field.name.equals(versionFieldName)) {
						versionsField = field;
						injectVersionInjection(targetClass);
						break;
					}
				}
				FieldNode nd;
				targetClass.fields.add(nd = new FieldNode(
						// synthetic hides it from the decompiler
						Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_STATIC,
						versionFieldName,
						"Ljava/util/Map;",
						"L" + typeVersion + ";",
						null
				));
				versionsField = nd;
				
				for (MethodNode method : targetClass.methods) {
					if (method.name.equals("<clinit>")) {
						ArrayList<AbstractInsnNode> targets = new ArrayList<>();
						for (AbstractInsnNode instruction : method.instructions) {
							if (instruction.getOpcode() == Opcodes.RETURN) {
								targets.add(instruction);
							}
						}
						for (AbstractInsnNode target : targets) {
							InsnList list = new InsnList();
							String des = nd.desc.substring(1, nd.desc.length() - 1);
							list.add(new TypeInsnNode(Opcodes.NEW, des.replace("Map", "HashMap")));
							list.add(new InsnNode(Opcodes.DUP));
							list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, des.replace("Map", "HashMap"), "<init>", "()V"));
							AbstractInsnNode insn = new FieldInsnNode(Opcodes.PUTSTATIC, targetClass.name.replace(".", "/"), nd.name, nd.desc);
							list.add(insn);
							method.instructions.insertBefore(target, list);
						}
					}
				}
				injectVersionInjection(targetClass);
				
				dump(targetClass);
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
						case "getTrackyVersions": {
							targ = versionsField;
							owner = sharedConstantsClass;
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
						if (Modifier.isStatic(targ.access)) {
							String sig = method.signature.substring(method.signature.indexOf(")") + 1);
							method.instructions.clear();
							String desc = sig.substring(0, sig.indexOf("<")) + ";";
							method.instructions.add(new FieldInsnNode(
									Opcodes.GETSTATIC, owner,
									targ.name, desc
							));
							method.instructions.add(new InsnNode(Opcodes.ARETURN));
							method.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
						} else {
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
				}
				
				dump(targetClass);
			}
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
