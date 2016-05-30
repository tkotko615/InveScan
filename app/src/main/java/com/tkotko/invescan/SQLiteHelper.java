package com.tkotko.invescan;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class SQLiteHelper extends SQLiteOpenHelper {

    SQLiteHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    /*
    // 需要資料庫的元件呼叫這個方法，這個方法在一般的應用都不需要修改
    public static SQLiteDatabase getDatabase(Context context) {
        if (database == null || !database.isOpen()) {
            database = new SQLiteHelper(context, DATABASE_NAME,
                    null, VERSION).getWritableDatabase();
        }

        return database;
    }
    */

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String create ="CREATE  TABLE IF NOT EXISTS inve " +
            "(_id INTEGER PRIMARY KEY  NOT NULL , " +
			"barcode VARCHAR, " +
		    "barcode_format VARCHAR, " +
			"input_type VARCHAR, " +
			"inve_no VARCHAR, " +
			"annx_no VARCHAR, " +
			"inve_name VARCHAR, " +
			"dept VARCHAR, " +
			"ins_user VARCHAR, " +
			"ins_date DATETIME, " +
			"remark VARCHAR, " +
			"info VARCHAR, " +
			"cust_1 VARCHAR, " +
			"cust_2 INTEGER)";
			
        db.execSQL(create);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        final String drop ="DROP TABLE IF EXISTS inve";
        db.execSQL(drop);
        onCreate(db);
    }
}
