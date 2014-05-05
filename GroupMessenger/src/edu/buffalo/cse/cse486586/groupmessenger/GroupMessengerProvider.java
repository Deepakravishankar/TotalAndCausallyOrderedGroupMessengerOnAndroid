package edu.buffalo.cse.cse486586.groupmessenger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author Deepak Ravishankar Ramkumar
 *
 */
public class GroupMessengerProvider extends ContentProvider {
	private static final String CONTENT_URI="content://edu.buffalo.cse.cse486586.groupmessenger.provider";

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         * 
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that I used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */
    	
    	try {
    		String key=null;                      //Create key array to hold all keys
    		String value=null;                    //Create value array to hold all values
    		Context context=this.getContext();  		  // Get current context
    		key=values.getAsString("key");      //Store value for each key and value and store it in array
    		value=values.getAsString("value");
    		FileOutputStream fos = context.getApplicationContext().openFileOutput(key, Context.MODE_PRIVATE);
    		try {
    				
    			fos.write(value.getBytes());  //Create output stream and set the key value as the filename
    			fos.close();                     //Write the corresponding value to each key file
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
    		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
        Log.v("insert", values.toString());
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         * 
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         * 
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */
    	String FILENAME=selection;                                               // Store the key into FILENAME
    	Context context=this.getContext();                                       //Get Current Context
    	MatrixCursor mCursor=new MatrixCursor(new String [] {"key","value"});    //Create Matrix Cursor object with key and value column names
    	FileInputStream fis;                                                     
		try {
			fis = context.getApplicationContext().openFileInput(FILENAME);       // Create a input stream to open file
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));  
			mCursor.addRow(new String[]{FILENAME,br.readLine()});                //	Read from it and store the (key,value) 
			br.close();                                                          //pair as a cursor object
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
        Log.v("query", selection);
        if(mCursor!=null && mCursor.getCount()>0)
		{
        	mCursor.setNotificationUri(getContext().getContentResolver(), uri); //Notify all listeners that are waiting on the 
        	                                                                    //Cursor object
			return mCursor;
		}
        else
        return null;
    }


	@Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }
}
