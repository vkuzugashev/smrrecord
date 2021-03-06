package ru.itsps.smrecord.smrecord;

/**
 * Created by penart on 04.10.2016.
 */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import static com.google.android.gms.internal.zzs.TAG;

public final class RecordStoreContract {
    //public InventoryStoreContract() {}

    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INTEGER";
    private static final String REAL_TYPE = " REAL";
    private static final String COMMA_SEP = ",";

    //Таблица записей
    public static abstract class Record implements BaseColumns {
        public static final String TABLE_NAME="RECORDS";
        public static final String COLUMN_NAME_STARTDT ="STARTDT";  //Начало записи
        public static final String COLUMN_NAME_STOPDT ="STOPDT";  //Окончание записи
        public static final String COLUMN_NAME_DURATION ="DURATION";  //Длительность
        public static final String COLUMN_NAME_UNANSWERED ="UNANSWERED";  //Не отвтвеченный
        public static final String COLUMN_NAME_DIRECTION ="DIRECTION";  //Направление
        public static final String COLUMN_NAME_REMOTE_PHONE ="REMOTE_PHONE";  //Номер абонента
        public static final String COLUMN_NAME_RECORD_FILE ="RECORD_FILE";  //Файл записи
    }

    //Определим SQL создания таблицы
    private static final String SQL_CREATE_RECORD =
            "CREATE TABLE " + Record.TABLE_NAME + " ("
                    + Record._ID + " INTEGER PRIMARY KEY " + COMMA_SEP
                    + Record.COLUMN_NAME_STARTDT + TEXT_TYPE + COMMA_SEP
                    + Record.COLUMN_NAME_STOPDT + TEXT_TYPE + COMMA_SEP
                    + Record.COLUMN_NAME_DURATION + INT_TYPE + COMMA_SEP
                    + Record.COLUMN_NAME_UNANSWERED + INT_TYPE + COMMA_SEP
                    + Record.COLUMN_NAME_DIRECTION + INT_TYPE + COMMA_SEP
                    + Record.COLUMN_NAME_REMOTE_PHONE + TEXT_TYPE + COMMA_SEP
                    + Record.COLUMN_NAME_RECORD_FILE + TEXT_TYPE
                    + " )";

    //Определим SQL удаления таблицы
    private static final String SQL_DELETE_RECORD =
            "DROP TABLE IF EXISTS " + Record.TABLE_NAME;


    public static abstract class LocRecord implements BaseColumns {
        public static final String TABLE_NAME="LOCATIONS";
        public static final String COLUMN_NAME_DT ="DT";  //Начало записи
        public static final String COLUMN_NAME_LAT ="LAT";  //LAT
        public static final String COLUMN_NAME_LON ="LON";  //LON
    }

    //Определим SQL создания таблицы
    private static final String SQL_CREATE_LOCATION =
            "CREATE TABLE " + LocRecord.TABLE_NAME + " ("
                    + LocRecord._ID + " INTEGER PRIMARY KEY " + COMMA_SEP
                    + LocRecord.COLUMN_NAME_DT + TEXT_TYPE + COMMA_SEP
                    + LocRecord.COLUMN_NAME_LAT + REAL_TYPE + COMMA_SEP
                    + LocRecord.COLUMN_NAME_LON + REAL_TYPE
                    + " )";

    //Определим SQL удаления таблицы
    private static final String SQL_DELETE_LOCATION =
            "DROP TABLE IF EXISTS " + LocRecord.TABLE_NAME;

    public RecordStoreDbHelper getRecordStoreDbHelper(Context context){
        return new RecordStoreDbHelper(context);
    }

    //Определим Helper класс для работы с БД
    public class RecordStoreDbHelper extends SQLiteOpenHelper {
        // If you change the database schema, you must increment the database version.
        public static final int DATABASE_VERSION = 4;
        public static final String DATABASE_NAME = "SmRecordStore.db";

        public RecordStoreDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db)
        {
            Log.i(TAG, "DB create tables");
            try {
                db.execSQL(SQL_CREATE_RECORD);
            }
            catch(Exception e) {
                Log.w(TAG, e.getMessage());
            }
            try {
                db.execSQL(SQL_CREATE_LOCATION);
            }
            catch(Exception e) {
                Log.w(TAG, e.getMessage());
            }
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // This database is only a cache for online data, so its upgrade policy is
            // to simply to discard the data and start over
            if(oldVersion!=newVersion) {

                Log.i(TAG, "DB upgrade from "+oldVersion + " to "+ newVersion);

                try {
                    db.execSQL(SQL_DELETE_RECORD);
                } catch (Exception e) {
                    Log.w(TAG, e.getMessage());
                }
                try {
                    db.execSQL(SQL_DELETE_LOCATION);
                } catch (Exception e) {
                    Log.w(TAG, e.getMessage());
                }
                onCreate(db);
            }

        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }



    }

    //Очистка БД
/*    public void ClearDb(SQLiteDatabase db) {
        //Удалить все таблицы
        db.execSQL(SQL_DELETE_RECORD);
        db.execSQL(SQL_DELETE_LOCATION);
        //Создать все таблицы
        db.execSQL(SQL_CREATE_RECORD);
        db.execSQL(SQL_CREATE_LOCATION);
    }*/
}

