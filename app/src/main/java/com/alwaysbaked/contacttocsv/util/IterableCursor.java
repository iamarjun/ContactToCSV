package com.alwaysbaked.contacttocsv.util;

import android.database.Cursor;

import java.util.Iterator;

public class IterableCursor implements Iterable<Cursor> {

    private Cursor cursor;

    public IterableCursor(Cursor cursor) {
        this.cursor = cursor;
        this.cursor.moveToPosition(-1);
    }

    @Override
    public Iterator<Cursor> iterator() {
        return new Iterator<Cursor>() {
            @Override
            public boolean hasNext() {
                return !cursor.isClosed() && cursor.moveToNext();
            }

            @Override
            public Cursor next() {
                return cursor;
            }

            @Override
            public void remove() {
            }
        };
    }
}
