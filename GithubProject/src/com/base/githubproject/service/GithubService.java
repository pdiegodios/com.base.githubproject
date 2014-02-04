package com.base.githubproject.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import com.base.githubproject.database.DBHelper;
import com.base.githubproject.database.UserDataSource;
import com.base.githubproject.entities.User;
import com.google.gson.Gson;

import android.app.IntentService;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;


public class GithubService extends IntentService {
	public static final String NOTIFICATION = "com.base.githubproject.service.receiver";
	private static final String TAG = "GithubService";
	private static final boolean DEBUG = true;
	private String mPath = "users";
    private long mPosition;
    
    public GithubService() {
		super(GithubService.class.toString());
	}    
    
    @Override
	protected void onHandleIntent(Intent intent) {
    	long result = -1;
        Bundle myBundle=intent.getExtras();
        mPosition = myBundle.getLong(UserDataSource.COLUMN_ID);
        // check the global background data setting
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (!cm.getActiveNetworkInfo().isConnected()) {
            stopSelf();
            return;
        }
        // Start to download existing Github users calling Github API
        // We start from the last position saved in database
        String read_users = readUserFeed(WebServiceTask.URL+mPath+"?since="+mPosition);
		try{
			JSONArray jsonContent = new JSONArray(read_users);
			int numItems = jsonContent.length();
			ArrayList<User> users = new ArrayList<User>();
			Gson gson = new Gson();
			User user = null;
			for(int i=0; i<numItems;i++){
				JSONObject itemjson = jsonContent.getJSONObject(i);
				user = gson.fromJson(itemjson.toString(), User.class);
				users.add(user);
			}
			if(user!=null){
				//We obtain last position to make the next call to service
				result = user.getId(); 
				//We add all new users to SQLiteDatabase
				addUsers(users);
			}
			if(result>0){
				publish(result);
			}
		} catch (Exception e) {
			e.printStackTrace();
			result = -1;
		}  
    }
        

	//AUXILIAR METHODS	
	private String readUserFeed(String url){
	    StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(url);
		if(DEBUG)
			Log.i(TAG, "GET "+url);
		try {
			HttpResponse response = client.execute(httpGet);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode == 200) {
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(content));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
			} else {
				Log.e(this.toString(), "Failed to download file");
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return builder.toString();
	}
	
	/**
	 * Add new users to SQLite Database
	 * @param users: new users fetched from Github API
	 */
	private void addUsers(ArrayList<User> users){
		try{
			DBHelper helper = new DBHelper(this.getBaseContext());
			SQLiteDatabase database = helper.getWritableDatabase();
			UserDataSource dataSource = new UserDataSource(database);
			if(DEBUG)
				Log.i(TAG, "Added "+users.size()+" new users from "+mPosition+"position");
    		for(User user:users){
    			dataSource.insert(user);
    		}
    		database.close();
		}catch(Exception e){
			e.printStackTrace();
			Log.e(this.toString(), "Error adding Users to Database");	    			
		}
	}
	
	private void publish(long result) {
		if(DEBUG)
			Log.i(TAG,"Publishing changes");
	    Intent intent = new Intent();
	    intent.setAction(NOTIFICATION);
	    intent.putExtra(UserDataSource.COLUMN_ID, result);
	    sendBroadcast(intent);
	}
	
}