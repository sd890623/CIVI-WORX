package au.edu.unimelb.comp90018.civiworx.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Database metadata
    private static final String DATABASE_FILE = "civiworx.db";
    private static final int DATABASE_VERSION = 1;

    // Table names
    public static final String TABLE_REPORTS = "reports";
    public static final String TABLE_MESSAGES = "message";

    // SQL for creating reports table
    private static final String CREATE_REPORTS =
            "CREATE TABLE " + TABLE_REPORTS + " ( " +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "_cwx_id INTEGER, " +
                    "_sync INTEGER, " +
                    "reported_id TEXT, " +
                    "reported_by TEXT, " +
                    "reported_on TEXT, " + // technically date time
                    "title TEXT, " +
                    "lng REAL, " +
                    "lat REAL" +
            " );";

    // SQL for creating messages table
    private static final String CREATE_MESSAGES =
            "CREATE TABLE " + TABLE_MESSAGES + " ( " +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "_cwx_id INTEGER, " +
                    "_sync INTEGER, " +
                    "report_id INTEGER, " +
                    "is_read INTEGER, " +
                    "written_id TEXT, " +
                    "written_by TEXT, " +
                    "written_on TEXT, " + // technically date time
                    "reply_to INTEGER NULL, " +
                    "msg_txt TEXT, " +
                    "msg_img BLOB " +
            " );";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_FILE, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(CREATE_REPORTS);
        database.execSQL(CREATE_MESSAGES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(DatabaseHelper.class.getName(),
                "Database from " + oldVersion + " to " + newVersion);
        if (newVersion == 1) { // special case for creation of database
            onCreate(db);
        }

        // TODO: Any upgrades that get made after submission
    }

}
