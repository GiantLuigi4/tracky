package com.tracky.util;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class ReadOnlySet<T> implements Set<T> {
    Collection<T> list;

    public ReadOnlySet(Collection<T> list) {
        this.list = list;
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @NotNull
    @Override
    public <T1> T1[] toArray(@NotNull T1[] a) {
        return list.toArray(a);
    }

    @Override
    public boolean add(T t) {
        throw new RuntimeException("add is unsupported on a read-only list");
    }

    @Override
    public boolean remove(Object o) {
        throw new RuntimeException("remove is unsupported on a read-only list");
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return list.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends T> c) {
        throw new RuntimeException("add is unsupported on a read-only list");
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        throw new RuntimeException("remove is unsupported on a read-only list");
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new RuntimeException("retain is unsupported on a read-only list");
    }

    @Override
    public void clear() {
        throw new RuntimeException("clear is unsupported on a read-only list");
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        list.forEach(action);
    }
}
