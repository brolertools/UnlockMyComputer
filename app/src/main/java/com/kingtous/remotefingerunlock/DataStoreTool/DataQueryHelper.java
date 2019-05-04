package com.kingtous.remotefingerunlock.DataStoreTool;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DataQueryHelper extends SQLiteOpenHelper {

    private final String CreateBook = "create table data(" +
            "Type text," +
            "Name text," +
            "Mac text," +
            "User text," +
            "Ip Integer," +
            "Passwd text," +
            "isDefault Integer," +
            "PRIMARY KEY(Mac,User))";


    public DataQueryHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CreateBook);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
