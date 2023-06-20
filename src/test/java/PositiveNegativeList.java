import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PositiveNegativeList<T> implements List<T> {
	List<Entry<T>> internal;
	
	Collected positive = new Collected(true), negative = new Collected(false);
	
	public PositiveNegativeList(List<Entry<T>> internal) {
		this.internal = internal;
	}
	
	public PositiveNegativeList(PositiveNegativeList<T> src) {
		internal = new ArrayList<>(src.internal);
	}
	
	public PositiveNegativeList() {
		internal = new ArrayList<>();
	}
	
	public void setValue(int index, boolean v) {
		internal.get(index).v = v;
	}
	
	public Collection<T> getPositive() {
		return positive.update();
	}
	
	public Collection<T> getNegative() {
		return negative.update();
	}
	
	public class Collected implements java.util.Collection<T> {
		boolean v;
		int end;
		
		public Collected(boolean v) {
			this.v = v;
		}
		
		public Collected update() {
			end = -1;
			for (int i = internal.size() - 1; i >= 0; i--) {
				Entry<T> entry = internal.get(i);
				if (entry.v == v) {
					end = i;
					break;
				}
			}
			return this;
		}
		
		@Override
		public int size() {
			throw new RuntimeException("Unsupported");
		}
		
		@Override
		public boolean isEmpty() {
			throw new RuntimeException("Unsupported");
		}
		
		@Override
		public boolean contains(Object o) {
			throw new RuntimeException("Unsupported");
		}
		
		@NotNull
		@Override
		public Iterator<T> iterator() {
			return new Iterator<>() {
				final Iterator<Entry<T>> back = internal.iterator();
				int indx = 0;
				
				@Override
				public boolean hasNext() {
					return indx < end;
				}
				
				@Override
				public T next() {
					indx++;
					Entry<T> entry = back.next();
					while (entry.v != v) {
						indx++;
						entry = back.next();
					}
					return entry.t;
				}
			};
		}
		
		@NotNull
		@Override
		public Object[] toArray() {
			throw new RuntimeException("Unsupported");
		}
		
		@NotNull
		@Override
		public <T1> T1[] toArray(@NotNull T1[] a) {
			throw new RuntimeException("Unsupported");
		}
		
		@Override
		public boolean add(T t) {
			throw new RuntimeException("Unsupported");
		}
		
		@Override
		public boolean remove(Object o) {
			throw new RuntimeException("Unsupported");
		}
		
		@Override
		public boolean containsAll(@NotNull java.util.Collection<?> c) {
			throw new RuntimeException("Unsupported");
		}
		
		@Override
		public boolean addAll(@NotNull java.util.Collection<? extends T> c) {
			throw new RuntimeException("Unsupported");
		}
		
		@Override
		public boolean removeAll(@NotNull java.util.Collection<?> c) {
			throw new RuntimeException("Unsupported");
		}
		
		@Override
		public boolean retainAll(@NotNull java.util.Collection<?> c) {
			throw new RuntimeException("Unsupported");
		}
		
		@Override
		public void clear() {
			throw new RuntimeException("Unsupported");
		}
	}
	
	@Override
	public int size() {
		return internal.size();
	}
	
	@Override
	public boolean isEmpty() {
		return internal.isEmpty();
	}
	
	@Override
	public boolean contains(Object o) {
		return internal.contains(new Entry<>(o));
	}
	
	@NotNull
	@Override
	public Iterator<T> iterator() {
		return new Iterator<>() {
			Iterator<Entry<T>> back = internal.iterator();
			
			@Override
			public boolean hasNext() {
				return back.hasNext();
			}
			
			@Override
			public T next() {
				return back.next().t;
			}
		};
	}
	
	@NotNull
	@Override
	public Object[] toArray() {
		throw new RuntimeException("NYI");
	}
	
	@NotNull
	@Override
	public <T1> T1[] toArray(@NotNull T1[] a) {
		throw new RuntimeException("NYI");
	}
	
	@Override
	public boolean add(T t) {
		return internal.add(new Entry<>(t));
	}
	
	@Override
	public boolean remove(Object o) {
		return internal.remove(new Entry<>(o));
	}
	
	@Override
	public boolean containsAll(@NotNull Collection<?> c) {
		throw new RuntimeException("NYI");
	}
	
	@Override
	public boolean addAll(@NotNull Collection<? extends T> c) {
		throw new RuntimeException("NYI");
	}
	
	@Override
	public boolean addAll(int index, @NotNull Collection<? extends T> c) {
		throw new RuntimeException("NYI");
	}
	
	@Override
	public boolean removeAll(@NotNull Collection<?> c) {
		throw new RuntimeException("NYI");
	}
	
	@Override
	public boolean retainAll(@NotNull Collection<?> c) {
		throw new RuntimeException("NYI");
	}
	
	@Override
	public void clear() {
		internal.clear();
	}
	
	@Override
	public T get(int index) {
		return internal.get(index).t;
	}
	
	@Override
	public T set(int index, T element) {
		return internal.set(index, new Entry<>(element)).t;
	}
	
	@Override
	public void add(int index, T element) {
		internal.add(index, new Entry<>(element));
	}
	
	@Override
	public T remove(int index) {
		return internal.remove(index).t;
	}
	
	@Override
	public int indexOf(Object o) {
		return 0;
	}
	
	@Override
	public int lastIndexOf(Object o) {
		return 0;
	}
	
	@NotNull
	@Override
	public ListIterator<T> listIterator() {
		throw new RuntimeException("NYI");
	}
	
	@NotNull
	@Override
	public ListIterator<T> listIterator(int index) {
		throw new RuntimeException("NYI");
	}
	
	@NotNull
	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		throw new RuntimeException("NYI");
	}
	
	class Entry<T> {
		boolean v;
		T t;
		
		public Entry(T t) {
			this.t = t;
		}
		
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Entry<?> entry = (Entry<?>) o;
			if (this.t == entry.t) return true;
			return Objects.equals(t, entry.t);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(t);
		}
	}
}
