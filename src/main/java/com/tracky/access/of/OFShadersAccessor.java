package com.tracky.access.of;

import org.joml.Matrix4f;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

public class OFShadersAccessor {
	private static final Object SHADERS_BASE;
	private static final long SHADER_PACK;
	
	// shader uniforms
	private static final long CHUNK_OFFSET;
	private static final MethodHandle FOG_SHAPE;
	private static final MethodHandle FOG_START;
	private static final MethodHandle FOG_END;
	private static final MethodHandle MODEL_VIEW;
	private static final MethodHandle PROJECTION;
	private static final MethodHandle FOG_COLOR;
	private static final MethodHandle FOG_ENABLE;
	private static final MethodHandle FOG_DISABLE;
	// shader uniform class functions
	private static final MethodHandle SET_VALUE_3F;
	
	private static final Unsafe THE_UNSAFE;
	
	static {
		try {
			Field f = Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			THE_UNSAFE = (Unsafe) f.get(null);
			f.setAccessible(false);
			
			Class<?> cls = Class.forName("net.optifine.shaders.Shaders");
			
			MethodHandles.Lookup lookup = MethodHandles.publicLookup();
			
			SHADERS_BASE = THE_UNSAFE.staticFieldBase(cls.getField("currentShaderName"));
			SHADER_PACK = THE_UNSAFE.staticFieldOffset(cls.getField("currentShaderName"));
			
			// fog stuff
			FOG_SHAPE = lookup.unreflect(cls.getMethod("setFogShape", int.class));
			FOG_START = lookup.unreflect(cls.getMethod("setFogStart", float.class));
			FOG_END = lookup.unreflect(cls.getMethod("setFogEnd", float.class));
			FOG_COLOR = lookup.unreflect(cls.getMethod("setFogColor", float.class, float.class, float.class));
			
			FOG_ENABLE = lookup.unreflect(cls.getMethod("enableFog"));
			FOG_DISABLE = lookup.unreflect(cls.getMethod("disableFog"));
			
			// matrices
			MODEL_VIEW = lookup.findStatic(cls, "setModelView", MethodType.methodType(void.class, Matrix4f.class));
			PROJECTION = lookup.findStatic(cls, "setProjection", MethodType.methodType(void.class, Matrix4f.class));
			
			// yipee
			CHUNK_OFFSET = THE_UNSAFE.staticFieldOffset(cls.getField("uniform_chunkOffset"));
			SET_VALUE_3F = lookup.unreflect(Class.forName("net.optifine.shaders.uniform.ShaderUniform3f").getMethod("setValue", float.class, float.class, float.class));
		} catch (Throwable err) {
			err.printStackTrace();
			throw new RuntimeException("HUH");
		}
	}
	
	public static boolean checkShadersActive() {
		try {
			String name = (String) THE_UNSAFE.getObject(SHADERS_BASE, SHADER_PACK);
			// regular switch looks nicer here imo
			//noinspection EnhancedSwitchMigration
			switch (name) {
				case "":
				case "OFF":
				case "(internal)": // internal shaders act the same as no shaders
					return false;
				default:
					return true;
			}
		} catch (Throwable err) {
			err.printStackTrace();
		}
		// unsure if it's best to crash or assume no shaders or assume shaders
		// probably crash tbh
		return false;
	}
	
	public static void setModelViewMatrix(Matrix4f matrix4f) {
		try {
//			System.out.println(matrix4f);
			MODEL_VIEW.invoke(matrix4f);
		} catch (Throwable err) {
			err.printStackTrace();
		}
	}
	
	public static void setProjection(Matrix4f matrix4f) {
		try {
			PROJECTION.invoke(matrix4f);
		} catch (Throwable err) {
			err.printStackTrace();
		}
	}
	
	public static void setFogShape(int shape) {
		try {
			FOG_SHAPE.invoke(shape);
		} catch (Throwable err) {
			err.printStackTrace();
		}
	}
	
	public static void setFogStart(float fogStart) {
		try {
			FOG_START.invoke(fogStart);
		} catch (Throwable err) {
			err.printStackTrace();
		}
	}
	
	public static void setFogEnd(float fogEnd) {
		try {
			FOG_END.invoke(fogEnd);
		} catch (Throwable err) {
			err.printStackTrace();
		}
	}
	
	public static void setFogColor(float red, float green, float blue) {
		try {
			FOG_COLOR.invoke(red, green, blue);
		} catch (Throwable err) {
			err.printStackTrace();
		}
	}
	
	public static void setChunkOffset(float x, float y, float z) {
		try {
			Object o = THE_UNSAFE.getObject(SHADERS_BASE, CHUNK_OFFSET);
			SET_VALUE_3F.invoke(o, x, y, z);
		} catch (Throwable err) {
			err.printStackTrace();
		}
	}
	
	public static void enableFog(boolean value) {
		try {
			if (value) {
				FOG_ENABLE.invoke();
			} else {
				FOG_DISABLE.invoke();
			}
		} catch (Throwable err) {
			err.printStackTrace();
		}
	}
}
