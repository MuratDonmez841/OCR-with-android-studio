package com.birkanyosma.orc2.LocalDB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;

/**Local db id şifreyi tutuyor değişiklik yapmayın*/
public class RemindingMail extends SQLiteOpenHelper {
    private static final String DataBase_Name = "DBReminding";
    private static final String Table_Name = "T_hatirlatma";
    private static final String EMAİL = "email";
    private static final String PASSWORD = "password";
    private static final String CHECKBOXDURUM = "checkboxdurum";
    private static final String OTURUMDURUM = "oturumdurum";

    public RemindingMail(Context context) {
        super(context, DataBase_Name, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + Table_Name + "("
                + EMAİL + " TEXT,"
                + PASSWORD + " TEXT,"
                + OTURUMDURUM + " TEXT,"
                + CHECKBOXDURUM + " TEXT" + ")";

        db.execSQL(CREATE_TABLE);
    }

    public void reminding(String email, String password, String checkboxdurum, String oturumdurum) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(EMAİL, email);
        values.put(PASSWORD, password);
        values.put(CHECKBOXDURUM, checkboxdurum);
        values.put(OTURUMDURUM, oturumdurum);
        db.insert(Table_Name, null, values);
        db.close();
    }

    public ArrayList<HashMap<String, String>> remindingList() {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + Table_Name;
        Cursor cursor = db.rawQuery(selectQuery, null);
        ArrayList<HashMap<String, String>> hatirlatmaList = new ArrayList<HashMap<String, String>>();
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<String, String>();
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    map.put(cursor.getColumnName(i), cursor.getString(i));
                }
                hatirlatmaList.add(map);
            } while (cursor.moveToNext());
        }
        return hatirlatmaList;
    }

    public void resetReminding() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(Table_Name, null, null);
        db.close();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
