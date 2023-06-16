package com.tracky.util;

import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class MapWrapper<T, V> implements Map<T, V> {
	Map<T, V> wrapped;
	
	public MapWrapper(Map<T, V> wrapped) {
		this.wrapped = wrapped;
	}
	
	@Override
	public int size() {
		return wrapped.size();
	}
	
	@Override
	public boolean isEmpty() {
		return wrapped.isEmpty();
	}
	
	@Override
	public boolean containsKey(Object key) {
		return wrapped.containsKey(key);
	}
	
	@Override
	public boolean containsValue(Object value) {
		return wrapped.containsValue(value);
	}
	
	@Override
	public V get(Object key) {
		return wrapped.get(key);
	}
	
	@Nullable
	@Override
	public V put(T key, V value) {
		return wrapped.put(key, value);
	}
	
	@Override
	public V remove(Object key) {
		return wrapped.remove(key);
	}
	
	@Override
	public void putAll(@NotNull Map<? extends T, ? extends V> m) {
		wrapped.putAll(m);
	}
	
	@Override
	public void clear() {
		if (FMLEnvironment.production) {
			// TODO: warn
		} else {
			// TODO: throw exception
//			throw new RuntimeException("Cannot clear tracky maps");
		}
	}
	
	@NotNull
	@Override
	public Set<T> keySet() {
		return wrapped.keySet();
	}
	
	@NotNull
	@Override
	public Collection<V> values() {
		return wrapped.values();
	}
	
	@NotNull
	@Override
	public Set<Entry<T, V>> entrySet() {
		return wrapped.entrySet();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof MapWrapper mapWrapper) return wrapped.equals(mapWrapper.wrapped);
		return wrapped.equals(o);
	}
	
	@Override
	public int hashCode() {
		return wrapped.hashCode();
	}
	
	@Override
	public V getOrDefault(Object key, V defaultValue) {
		return wrapped.getOrDefault(key, defaultValue);
	}
	
	@Override
	public void forEach(BiConsumer<? super T, ? super V> action) {
		wrapped.forEach(action);
	}
	
	@Override
	public void replaceAll(BiFunction<? super T, ? super V, ? extends V> function) {
		wrapped.replaceAll(function);
	}
	
	@Nullable
	@Override
	public V putIfAbsent(T key, V value) {
		return wrapped.putIfAbsent(key, value);
	}
	
	@Override
	public boolean remove(Object key, Object value) {
		return wrapped.remove(key, value);
	}
	
	@Override
	public boolean replace(T key, V oldValue, V newValue) {
		return wrapped.replace(key, oldValue, newValue);
	}
	
	@Nullable
	@Override
	public V replace(T key, V value) {
		return wrapped.replace(key, value);
	}
	
	@Override
	public V computeIfAbsent(T key, @NotNull Function<? super T, ? extends V> mappingFunction) {
		return wrapped.computeIfAbsent(key, mappingFunction);
	}
	
	@Override
	public V computeIfPresent(T key, @NotNull BiFunction<? super T, ? super V, ? extends V> remappingFunction) {
		return wrapped.computeIfPresent(key, remappingFunction);
	}
	
	@Override
	public V compute(T key, @NotNull BiFunction<? super T, ? super V, ? extends V> remappingFunction) {
		return wrapped.compute(key, remappingFunction);
	}
	
	@Override
	public V merge(T key, @NotNull V value, @NotNull BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		return wrapped.merge(key, value, remappingFunction);
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new MapWrapper<>(wrapped); // TODO: clone wrapped
	}
	
	@Override
	public String toString() {
		return wrapped.toString();
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}
}
