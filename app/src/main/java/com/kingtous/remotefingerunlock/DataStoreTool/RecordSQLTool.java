package com.kingtous.remotefingerunlock.DataStoreTool;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class RecordSQLTool {

    public static RecordData toRecordData(Cursor cursor) {
        //返回cursor下的数据，需要配合cursor.movetoNext使用
        RecordData recordData = new RecordData();
        int cnt = cursor.getColumnCount();
        for (int colomn = 0; colomn < cnt; ++colomn) {
            String colomnName = cursor.getColumnName(colomn);
            switch (colomnName) {
                case "Type":
                    recordData.setType(cursor.getString(cursor.getColumnIndex(colomnName)));
                    break;
                case "Name":
                    recordData.setName(cursor.getString(cursor.getColumnIndex(colomnName)));
                    break;
                case "Mac":
                    recordData.setMac(cursor.getString(cursor.getColumnIndex(colomnName)));
                    break;
                case "Ip":
                    recordData.setIp(cursor.getString(cursor.getColumnIndex(colomnName)));
                    break;
                case "User":
                    recordData.setUser(cursor.getString(cursor.getColumnIndex(colomnName)));
                    break;
                case "Passwd":
                    recordData.setPasswd(cursor.getString(cursor.getColumnIndex(colomnName)));
                    break;
                case "isDefault":
                    recordData.setIsDefault(cursor.getInt(cursor.getColumnIndex(colomnName)));
                    break;
                default:
                    break;
            }
        }
        return recordData;
    }

    public static boolean updatetoSQL(SQLiteDatabase writableDatabase, RecordData old_record, RecordData new_record) {
        if (writableDatabase != null && old_record != null && new_record != null) {

            ContentValues values = loadValues(new_record);
            boolean result = false;
            //先在数据库中查找是否有Mac,User相同的值
            String[] cond = new String[]{old_record.getMac(), old_record.getUser()};

            int cnt = writableDatabase.update("data", values, "Mac=? and User=?", cond);
            if (cnt > 0) {
                result = true;
            }
            return result;

        } else return false;
    }

    private static ContentValues loadValues(RecordData record) {
        ContentValues values = new ContentValues();
        values.put("Type", record.getType());
        values.put("Name", record.getName());
        values.put("Mac", record.getMac());
        values.put("User", record.getUser());
        values.put("Ip", record.getIp());
        values.put("Passwd", record.getPasswd());
        values.put("isDefault", record.getIsDefault());
        return values;
    }


    public static boolean deleteRecordFromSQL(SQLiteDatabase writableDatabase, RecordData data) {
        if (writableDatabase != null && data != null) {

            boolean result = false;
            //先在数据库中查找是否有Mac相同的值
            String[] cond = new String[]{data.getMac(), data.getUser()};
            int cnt = writableDatabase.delete("data", "Mac=? and User=?", cond);
            if (cnt > 0) {
                result = true;
            }
            return result;

        } else return false;
    }

    public static boolean addtoSQL(SQLiteOpenHelper helper, RecordData data) {

        SQLiteDatabase writableDatabase = helper.getWritableDatabase();

        if (writableDatabase != null && data != null) {

            ContentValues values = loadValues(data);

            boolean result = false;
            //先在数据库中查找是否有Mac,User相同的值
            String[] cond = new String[]{data.getMac(), data.getUser()};

            Cursor cursor = writableDatabase.rawQuery
                    ("select Mac,User from data where Mac='" + data.getMac() + "' and User='" + data.getUser() + "'", new String[]{});
            if (cursor.moveToNext()) {
                //有值，放弃插入
                return result;
            }
            try {
                writableDatabase.insert("data", null, values);
                if (data.getIsDefault() == RecordData.TRUE) {
                    //去除default
                    updateDefaultRecord(helper, data.getMac(), data.getUser());
                }
                result = true;
            } catch (Exception e) {
                e.printStackTrace();
                result = false;
            } finally {
                cursor.close();
                return result;
            }
        } else return false;
    }


    public static RecordData getDefaultRecordData(SQLiteDatabase readableDatabase) {
        if (readableDatabase == null) {
            return null;
        }
        String[] cond = new String[]{String.valueOf(RecordData.TRUE)};
        Cursor cursor = readableDatabase.query("data", null, "isDefault=?", cond, null, null, null);
        if (cursor.moveToNext()) {
            return toRecordData(cursor);
        }
        return null;
    }

    public static boolean updateDefaultRecord(SQLiteOpenHelper helper, String macAddress, String user) {
        SQLiteDatabase readSQL = helper.getReadableDatabase();
        SQLiteDatabase writeSQL = helper.getWritableDatabase();
        if (readSQL != null && writeSQL != null) {
            //寻找原来的defaultRecord
            boolean result = false;
            try {
                String[] cond = new String[]{String.valueOf(RecordData.TRUE)};
                Cursor cursor = readSQL.query("data", null, "isDefault=?", cond, null, null, null);
                if (cursor.moveToNext()) {
                    //有默认的就修改掉
                    ContentValues values = new ContentValues();
                    values.put("isDefault", RecordData.FALSE);
                    writeSQL.update("data", values, "isDefault=?", new String[]{String.valueOf(RecordData.TRUE)});
                }
                //设置为newlyRecordData
                ContentValues values = new ContentValues();
                values.put("isDefault", RecordData.TRUE);
                writeSQL.update("data", values, "Mac=? and User=?", new String[]{macAddress, user});
                cursor.close();
                result = true;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                return result;
            }
        } else return false;

    }


}
