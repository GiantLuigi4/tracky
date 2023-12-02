package com.tracky.util.list;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import it.unimi.dsi.fastutil.objects.ObjectSpliterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ObjectUnionList<T> extends ObjectArrayList<T> {
	ArrayList<List<T>> lists;
	
	public ObjectUnionList(List<T>... lists) {
		this.lists = new ArrayList<>(Arrays.asList(lists));
	}
	
	public void addList(List<T> list) {
		lists.add(list);
	}
	
	public List<T> getList(int index) {
		return lists.get(index);
	}
	
	@Override
	public T get(int index) {
		int idx = 0;
		for (List<T> list : lists) {
			int sz = list.size();
			if (index >= idx && index < sz + idx)
				return list.get(index - idx);
			
			idx += sz;
		}
		return super.get(index - idx);
	}
	
	@Override
	public int size() {
		int sz = 0;
		for (List<T> list : lists)
			sz += list.size();
		return sz + super.size();
	}
	
	@Override
	public void size(int size) {
		super.size(size);
	}

	@Override
	public ObjectListIterator<T> listIterator(int index) {
		return new ObjectListIterator<T>() {
			int index = 0;
			int endIndex = lists.get(0).size();
			int lindex = 0;
			Iterator<T> current = lists.get(0).iterator();
			final int sz = size();
			
			@Override
			public T previous() {
				throw new RuntimeException("NYI");
			}
			
			@Override
			public boolean hasPrevious() {
				return false;
			}
			
			@Override
			public int nextIndex() {
				return index + 1;
			}
			
			@Override
			public int previousIndex() {
				return index - 1;
			}
			
			@Override
			public boolean hasNext() {
				if (sz == 0) return false;
				if (index >= sz) return false;
				if (lindex < lists.size()) return true;
				return index < endIndex;
			}
			
			@Override
			public T next() {
				index++;
				while (index > endIndex) {
					lindex++;
					if (lindex >= lists.size()) {
						current = ObjectUnionList.super.listIterator(0);
						endIndex = sz;
					} else {
						current = lists.get(lindex).iterator();
						endIndex = lists.get(lindex).size() + index - 1;
					}
				}
				return current.next();
			}
		};
	}
	
	@Override
	public ObjectSpliterator<T> spliterator() {
		throw new RuntimeException("NYI");
	}
}
