package utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;


public class FingerDBOperation {
	Context context;

	public FingerDBOperation(Context context) {
		// TODO Auto-generated constructor stub
		this.context = context;
	}

	/**
	 * Save
	 * @param template
	 */
	public boolean saveTemplate(FBIFingerModel template) {
		try {
			ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(arrayOutputStream);
			objectOutputStream.writeObject(template);
			objectOutputStream.flush();
			byte data[] = arrayOutputStream.toByteArray();
			objectOutputStream.close();
			arrayOutputStream.close();
			Dbhelper dbhelper = Dbhelper.getInstens(context);
			SQLiteDatabase database = dbhelper.getWritableDatabase();
			database.execSQL("insert into finger (template) values(?)", new Object[] { data });
			database.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	/**
	 * Get
	 * @return
	 */
	public List<FBIFingerModel> getTemplates() {
		List<FBIFingerModel> templates = new ArrayList<FBIFingerModel>();
		Dbhelper dbhelper = Dbhelper.getInstens(context);
		SQLiteDatabase database = dbhelper.getReadableDatabase();
		Cursor cursor = database.rawQuery("select * from finger", null);
		if (cursor != null) {
			while (cursor.moveToNext()) {
				FBIFingerModel template = null;
				byte data[] = cursor.getBlob(cursor.getColumnIndex("template"));
				ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(data);
				try {
					ObjectInputStream inputStream = new ObjectInputStream(arrayInputStream);
					template = (FBIFingerModel) inputStream.readObject();
					inputStream.close();
					arrayInputStream.close();
					templates.add(template);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return templates;
	}
	public boolean deleteAllTemplate(){
		Dbhelper dbhelper = Dbhelper.getInstens(context);
		SQLiteDatabase database = dbhelper.getReadableDatabase();
		int c = database.delete("finger", null, null);
//		database.execSQL("delete from finger");
		return c>=0 ? true : false;
	}
}
