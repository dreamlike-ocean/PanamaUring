package top.dreamlike.panama.genertor.proxy;

import top.dreamlike.panama.genertor.helper.NativeGeneratorHelper;
import top.dreamlike.panama.genertor.helper.NativeStructEnhanceMark;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class NativeArray<T> implements NativeStructEnhanceMark, List<T> {

    private MemorySegment segment;

    private final MemoryLayout elementLayout;

    private final StructProxyGenerator generator;

    private final long len;

    private final List<T> mappingJavaBean;

    @SafeVarargs
    NativeArray(StructProxyGenerator generator, MemorySegment memorySegment, T... dummy) {
        Class<T> component = (Class<T>) dummy.getClass().getComponentType();
        if (component == Object.class) {
            throw new IllegalArgumentException(STR."please fill generic param");
        }
        this.segment = memorySegment;
        this.elementLayout = generator.extract(component);
        if (this.segment.byteSize() % elementLayout.byteSize() != 0L) {
            throw new IllegalArgumentException(STR. "segment.byteSize() % layout.byteSize() must equals 0!, array size is \{ memorySegment.byteSize() }, single component size is \{ elementLayout.byteSize() }" );
        }
        this.len = segment.byteSize() / elementLayout.byteSize();
        this.generator = generator;
        T[] mappingJavaBeanArray = (T[]) new Object[((int) len)];
        for (int l = 0, offset = 0; l < len; l++, offset += (int) elementLayout.byteSize()) {
            MemorySegment slice = segment.asSlice(offset, elementLayout.byteSize());
            mappingJavaBeanArray[l] = generator.enhance(component, slice);
        }
        this.mappingJavaBean = Arrays.asList(mappingJavaBeanArray);
    }


    @Override
    public StructProxyGenerator fetchStructProxyGenerator() {
        return generator;
    }

    @Override
    public MemorySegment realMemory() {
        return segment;
    }

    @Override
    public long sizeof() {
        return segment.byteSize();
    }

    @Override
    public MemoryLayout layout() {
        return MemoryLayout.sequenceLayout(len, elementLayout);
    }

    @Override
    public void rebind(MemorySegment memorySegment) {
        NativeGeneratorHelper.assertRebindMemory(memorySegment, this.segment);
        this.segment = memorySegment;
    }

    @Override
    public int size() {
        return mappingJavaBean.size();
    }

    @Override
    public boolean isEmpty() {
        return mappingJavaBean.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return mappingJavaBean.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return mappingJavaBean.iterator();
    }

    @Override
    public Object[] toArray() {
        return mappingJavaBean.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return mappingJavaBean.toArray(a);
    }

    @Override
    public boolean add(T t) {
        return mappingJavaBean.add(t);
    }

    @Override
    public boolean remove(Object o) {
        return mappingJavaBean.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return mappingJavaBean.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return mappingJavaBean.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        return mappingJavaBean.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return mappingJavaBean.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return mappingJavaBean.retainAll(c);
    }

    @Override
    public void replaceAll(UnaryOperator<T> operator) {
        mappingJavaBean.replaceAll(operator);
    }

    @Override
    public void sort(Comparator<? super T> c) {
        mappingJavaBean.sort(c);
    }

    @Override
    public void clear() {
        mappingJavaBean.clear();
    }

    @Override
    public boolean equals(Object o) {
        return mappingJavaBean.equals(o);
    }

    @Override
    public int hashCode() {
        return mappingJavaBean.hashCode();
    }

    @Override
    public T get(int index) {
        return mappingJavaBean.get(index);
    }

    @Override
    public T set(int index, T element) {
        return mappingJavaBean.set(index, element);
    }

    @Override
    public void add(int index, T element) {
        mappingJavaBean.add(index, element);
    }

    @Override
    public T remove(int index) {
        return mappingJavaBean.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return mappingJavaBean.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return mappingJavaBean.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return mappingJavaBean.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return mappingJavaBean.listIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return mappingJavaBean.subList(fromIndex, toIndex);
    }

    @Override
    public Spliterator<T> spliterator() {
        return mappingJavaBean.spliterator();
    }

    @Override
    public void addFirst(T t) {
        mappingJavaBean.addFirst(t);
    }

    @Override
    public void addLast(T t) {
        mappingJavaBean.addLast(t);
    }

    @Override
    public T getFirst() {
        return mappingJavaBean.getFirst();
    }

    @Override
    public T getLast() {
        return mappingJavaBean.getLast();
    }

    @Override
    public T removeFirst() {
        return mappingJavaBean.removeFirst();
    }

    @Override
    public T removeLast() {
        return mappingJavaBean.removeLast();
    }

    @Override
    public List<T> reversed() {
        return mappingJavaBean.reversed();
    }

    @Override
    public <T1> T1[] toArray(IntFunction<T1[]> generator) {
        return mappingJavaBean.toArray(generator);
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        return mappingJavaBean.removeIf(filter);
    }

    @Override
    public Stream<T> stream() {
        return mappingJavaBean.stream();
    }

    @Override
    public Stream<T> parallelStream() {
        return mappingJavaBean.parallelStream();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        mappingJavaBean.forEach(action);
    }
}
