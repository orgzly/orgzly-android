package com.orgzly.android.util;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;

public class CircularArrayList<T> extends AbstractList {
    private List<T> list;

    public CircularArrayList(T[] array) {
        this.list = Arrays.asList(array);
    }

    @Override
    public T get(int index) {
        if (list.size() == 0) {
            throw new IndexOutOfBoundsException("Empty array");
        }

        index = convert(index);

        return list.get(index);
    }

    private int convert(int index) {
        /* If too small. */
        while (index < 0) {
            index = index + list.size();
        }

        /* If too big. */
        index = index % list.size();

        return index;
    }

    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    @Override
    public int size() {
        return list.size();
    }
}
