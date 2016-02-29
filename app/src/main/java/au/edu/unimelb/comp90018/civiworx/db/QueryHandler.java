package au.edu.unimelb.comp90018.civiworx.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class QueryHandler {

    // Reference to the actual SQLite database
    private SQLiteDatabase database;
    private DatabaseHelper dbHelper;

    // Parser for date formats
    private static SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public QueryHandler(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    /**
     * Write the given report to the database.
     */
    public Report saveReport(Report report) {
        // Setup the query content values
        ContentValues cvData = new ContentValues();
        cvData.put("reported_id", report.userId);
        cvData.put("reported_by", report.author);
        cvData.put("reported_on", dateToString(report.reportedDate));
        cvData.put("title", report.title);
        cvData.put("lat", report.lat);
        cvData.put("lng", report.lng);
        // Either way the report should now be flagged for background sync
        cvData.put("_sync", 1);

        // If id is null then this is a new report
        if (report.id == null) {
            // Insert
            report.id = database.insert(DatabaseHelper.TABLE_REPORTS, null, cvData);
        } else {
            // Update
            database.update(DatabaseHelper.TABLE_REPORTS, cvData, "_id=?",
                    new String[]{ Long.toString(report.id) });
        }

        return report;
    }

    /**
     * Write the given message to the database
     */
    public Message saveMessage(Message message) {
        // Setup the query content values
        ContentValues cvData = new ContentValues();
        cvData.put("report_id", message.report);
        cvData.put("is_read", 1); // just written so mark it as read
        cvData.put("written_id", message.userId);
        cvData.put("written_by", message.author);
        cvData.put("written_on", dateToString(message.postedDate));
        cvData.put("msg_txt", message.messageText);
        cvData.put("msg_img", message.b64Image);
        // Either way the message should now be flagged for background sync
        cvData.put("_sync", 1);

        // If id is null then this is a new message
        if (message.id == null) {
            // Insert
            message.id = database.insert(DatabaseHelper.TABLE_MESSAGES, null, cvData);
        } else {
            // Update
            database.update(DatabaseHelper.TABLE_MESSAGES, cvData, "_id=?",
                    new String[]{ Long.toString(message.id) });
        }

        return message;
    }

    /**
     * Gets the report detail and primary message from database
     */
    public Report getReport(long id) {
        Cursor cursor = database.rawQuery(
                "SELECT _id, reported_id, reported_by, DATETIME(reported_on) AS 'rep_dte', title, lat, lng " +
                        " FROM " + DatabaseHelper.TABLE_REPORTS +
                        " WHERE _id=?;",
                new String[] { Long.toString(id) }
        );

        cursor.moveToFirst();
        Report o = cursorToReport(cursor);
        cursor.close();
        return o;
    }

    /**
     * Return the set of messages for the given report
     */
    public List<Message> getMessages(Report r) {
        List<Message> messages = new ArrayList<Message>();

        Cursor cursor = database.rawQuery(
                "SELECT _id, report_id, written_id, written_by, DATETIME(written_on) AS dte, " +
                        " reply_to, msg_txt, msg_img " +
                " FROM " + DatabaseHelper.TABLE_MESSAGES +
                " WHERE report_id=? ORDER BY dte;",
                new String[] { Long.toString(r.id) }
        );

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            messages.add(cursorToMessage(cursor));
            cursor.moveToNext();
        }

        cursor.close();
        return messages;
    }

    /**
     * Returns the "inbox" content"
     */
    public List<InboxItem> getInbox() {
        List<InboxItem> messages = new ArrayList<InboxItem>();

        Cursor cursor = database.rawQuery(
                "SELECT r._id, m._id, r.title, m.msg_txt, m.msg_img " +
                " FROM " + DatabaseHelper.TABLE_REPORTS + " r " +
                " JOIN " + DatabaseHelper.TABLE_MESSAGES + " m ON m.report_id=r._id;",
                null
        );

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            messages.add(new InboxItem(cursor.getLong(0), cursor.getLong(1),
                    cursor.getString(2), cursor.getString(3), cursor.getString(4)));
            cursor.moveToNext();
        }

        cursor.close();
        return messages;
    }

    /**
     * Return the number of unread messages.
     */
    public int countUnreadMessages() {
        Cursor cursor = database.rawQuery(
                "SELECT COUNT(_id) " +
                " FROM " + DatabaseHelper.TABLE_MESSAGES +
                " WHERE is_read=0",
                null
        );

        cursor.moveToFirst();
        int res = cursor.getInt(0);
        cursor.close();
        return res;
    }

    /**
     * Return a List<Report> of all non-sync'd reports.
     *
     * NOTE: This only is used for new reports - they're READ ONLY once sync'd.
     */
    public List<Report> reportsToSync() {
        List<Report> reports = new ArrayList<Report>();

        Cursor cursor = database.rawQuery(
                "SELECT _id, reported_id, reported_by, DATETIME(reported_on) AS 'rep_dte', title, lat, lng " +
                        " FROM " + DatabaseHelper.TABLE_REPORTS +
                        " WHERE _sync=1 AND _cwx_id IS NULL",
                null
        );

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            reports.add(cursorToReport(cursor));
            cursor.moveToNext();
        }

        cursor.close();
        return reports;
    }

    /**
     * Return a List<Message> of all non-sync'd messages.
     *
     * NOTE: This is only used for new messages - they're READ ONLY once sync'd.
     */
    public List<Message> messagesToSync() {
        List<Message> messages = new ArrayList<Message>();

        Cursor cursor = database.rawQuery(
                "SELECT m._id, r._cwx_id, m.reply_to, m.msg_txt, m.msg_img " +
                " FROM " + DatabaseHelper.TABLE_MESSAGES + " m " +
                " JOIN " + DatabaseHelper.TABLE_REPORTS + " r ON r._id=m.report_id " +
                " WHERE m._sync=1 AND m._cwx_id IS NULL",
                null
        );

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Long id = cursor.getLong(0),
                    report = cursor.getLong(1),
                    reply = cursor.getLong(2);
            String mt = cursor.getString(3);
            String ib64 = cursor.getString(4);
            messages.add(new Message(id, report, null, null, null, reply, mt, ib64));
            cursor.moveToNext();
        }

        cursor.close();
        return messages;
    }

    /**
     * Set the _cwx_id for the given report
     */
    public void setCiviWorxReport(long localId, long cwxId) {
        // Setup the query content values
        if (cwxId < 0)
            return; // no that's not valid
        ContentValues cvData = new ContentValues();
        cvData.put("_cwx_id", cwxId);
        cvData.put("_sync", 0);
        database.update(DatabaseHelper.TABLE_REPORTS, cvData, "_id=?",
                new String[] { Long.toString(localId) });
    }

    /**
     * Set the _cwx_id for the given message
     */
    public void setCiviWorxMessage(long localId, long cwxId) {
        // Setup the query content values
        if (cwxId < 0)
            return; // no that's not valid
        ContentValues cvData = new ContentValues();
        cvData.put("_cwx_id", cwxId);
        cvData.put("_sync", 0);
        database.update(DatabaseHelper.TABLE_MESSAGES, cvData, "_id=?",
                new String[] { Long.toString(localId) });
    }

    /**
     * Return a List<Report> of all Reports in the given bounding box.
     *
     * NOTE: This will fail on the date line - but SQLite doesn't really
     * provide a good way to actually do this without writing a massive
     * query. If there is time this should be done.
     */
    public List<Report> reportsByBoundingBox(BoundingBox mbr) {
        List<Report> reports = new ArrayList<Report>();

        Cursor cursor = database.rawQuery(
                "SELECT _id, reported_id, reported_by, DATETIME(reported_on) AS 'rep_dte', title, lat, lng " +
                " FROM " + DatabaseHelper.TABLE_REPORTS +
                " WHERE lat<=? AND ?<=lat AND lng<=? AND ?<=lng;",
                new String[] {
                        Double.toString(mbr.latNorthEast), Double.toString(mbr.latSouthWest),
                        Double.toString(mbr.lngNorthEast), Double.toString(mbr.lngSouthWest)
                }
        );

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            reports.add(cursorToReport(cursor));
            cursor.moveToNext();
        }

        cursor.close();
        return reports;
    }

    private Date stringToDate(String s) {
        // Extract the date as an actual date field
        Date fDate = null;
        try {
            fDate = dateFormat.parse(s);
        } catch (ParseException pe) {
            Log.w("CWX_DB", "Could not parse date: " + s);
        }
        return fDate;
    }

    private String dateToString(Date d) {
        String sDate = "";
        try {
            sDate = dateFormat.format(d);
        } catch (Exception e) {
            if (d != null) {
                Log.w("CWX_DB", "Could not format date: " + d.toString());
            } else {
                Log.w("CWX_DB", "Could not format date: is null");
            }
        }
        return sDate;
    }


    private Report cursorToReport(Cursor c) {
        return new Report(c.getLong(0), c.getString(1), c.getString(2), stringToDate(c.getString(3)),
                c.getString(4), c.getDouble(5), c.getDouble(6));
    }

    private Message cursorToMessage(Cursor c) {
        return new Message(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3),
                stringToDate(c.getString(4)), c.getLong(5), c.getString(6), c.getString(7));
    }


}
