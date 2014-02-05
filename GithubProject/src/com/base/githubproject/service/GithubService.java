package com.base.githubproject.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

/**
 * Service to make calls to github API {@link http://developer.github.com/v3/}.
 * For unauthenticated requests, the rate limit allows you to make up to 60 requests per hour
 * So, it is necessary to manage limit raised to stop the service and call it again an hour later.
 */
public class GithubService extends IntentService {
	public static final String NOTIFICATION = "com.base.githubproject.service.receiver";
	private static final String URL = "https://api.github.com/users?since=";
	private static final String TAG = "GithubService";
	private static final boolean DEBUG = true;
    private long mPosition;

	public GithubService() {
		super(GithubService.class.toString());
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	};
	
	@Override
	public void onDestroy() {
		if(DEBUG) Log.i(TAG, "Destroying Service");
		super.onDestroy();
	};
    
    @Override
	protected void onHandleIntent(Intent intent) {
    	if(intent!=null){
	        Bundle myBundle=intent.getExtras();
	        mPosition = myBundle.getLong(UserDataSource.COLUMN_ID);
    	}
    	else{
    		//It was killed due low resources or other reason, but it didn't finish
    		//get last position saved
    		mPosition = getLastUserId();
    	}
    	
        // Start to download existing Github users calling Github API
        // start from the last position saved in database
    	boolean keepAlive=true;
        while(keepAlive){      	
        	//will be downloading users until finish while there are more users in github
        	String read_users = readUserFeed(URL+mPosition);
        	if(read_users!=null && !read_users.isEmpty()){
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
	    				//obtain last position to make the next call to service
	    				mPosition = user.getId(); 
	    				//add all new users to SQLiteDatabase
	    				addUsers(users);
	    			}
	    			else{
	    				//send -1 to break the looper
	    				keepAlive=false;
	    			}
	    			if(mPosition>0){
	    				//to warn the broadcast receiver
	    				publish();
	    			}
	    		} catch (Exception e) {
	    			if(DEBUG) Log.e(TAG, e.getMessage());
	    			e.printStackTrace();
	    			keepAlive=false;
	    		}  
        	}
        	else{
        		keepAlive = false;
        	}
        }      
        //If service finished to download all new users or some error happened. The service
        //will be restarted 2 hours forward to check if there are new users
        if(DEBUG)
        	Log.i(TAG, "loop finished");
        startServiceAfterInterval(120);
        stopSelf();
    }
        

	//AUXILIAR METHODS	
	private String readUserFeed(String url){
		if(DEBUG)
			Log.i(TAG, "GET "+url);
	    StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(url);
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
			} else if (statusCode == 403){
				Log.e(this.toString(), "Failed to download file "+statusCode+"\n"+
						"you probably exceed limit of requests per hour. The service " +
						"will be reset 1 hour later");
				startServiceAfterInterval(61); //Call again in 61 minutes
				stopSelf();
			}
			else {
				Log.e(this.toString(), "Failed to download file "+statusCode);		
				stopSelf();		
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
		DBHelper helper = new DBHelper(this.getBaseContext());
		SQLiteDatabase database = helper.getWritableDatabase();
		UserDataSource dataSource = new UserDataSource(database);
		if(DEBUG)
			Log.i(TAG, "Added "+users.size()+" new users from "+mPosition+"position");
		for(User user:users){
			dataSource.insert(user);
		}
		database.close();
	}
	
	private long getLastUserId(){
		long lastId = 0;
		DBHelper helper = new DBHelper(this.getBaseContext());
		SQLiteDatabase database = helper.getWritableDatabase();
		UserDataSource dataSource = new UserDataSource(database);
		List<User> users = dataSource.queryForAll();
		if(users!=null & !users.isEmpty()){
			lastId = users.get(users.size()-1).getId();
		}
		database.close();
		return lastId;
	}
	
	/**
	 * Call to BroadcastReceiver to warn loader that there are new changes
	 */
	private void publish() {
		if(DEBUG)
			Log.i(TAG,"Publishing changes");
	    Intent intent = new Intent();
	    intent.setAction(NOTIFICATION);
	    sendBroadcast(intent);
	}

	/**
	 * This method program service to run after a specific time
	 * @param minutes: time to start the service again
	 */
	private void startServiceAfterInterval(int minutes) {
		Intent iService = new Intent(this.getBaseContext(), GithubService.class);
		iService.putExtra(UserDataSource.COLUMN_ID, mPosition);	
		AlarmManager am = (AlarmManager) this.getSystemService(ALARM_SERVICE);
	    PendingIntent pi = PendingIntent.getService(this, 0, iService, PendingIntent.FLAG_UPDATE_CURRENT);
		am.cancel(pi);
		am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime() + minutes*60*1000,
				minutes*60*1000, pi);
	}
	
	/**
	 * Auxiliar method to stop both Notification and Service.
	 */
	/*private void stop(){
		if(DEBUG)
			Log.i(TAG,"stopping service");
		NotificationManager nm = (NotificationManager) this.getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(ID);
		unregisterReceiver(mReceiver);
		stopSelf();
	}*/
	
	/**
	 * New receiver to heard if STOP action is called (from notification)
	 * if it happens stop() method is called.
	 */
	/*private void registerReceiver(){
    	IntentFilter filter = new IntentFilter();
    	filter.addAction(STOP);
    	// Add other actions as needed
    	mReceiver = new BroadcastReceiver() {
    	    @Override
    	    public void onReceive(Context context, Intent intent) {
    	        if (intent.getAction().equals(STOP)) {
    	            stop();
    	        } 
    	    }
    	};
    	registerReceiver(mReceiver, filter);
	}*/
	
	/**
	 * Cancel current notification if it exists and send new notification
	 * to notification bar to warn users than Service is working background
	 */
	/*private void callNotification(){    	
		NotificationManager nm = (NotificationManager) this.getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(ID);		
		//Send Stop Action when Notification is pushed
		Intent stopIntent = new Intent(STOP);
		PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, stopIntent, 0);
		// Build the notification
		String text = "getting users since ID "+mPosition;
		NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(this.getBaseContext())
		        .setContentTitle("Downloading... PUSH TO STOP")
		        .setContentText(text)
		        .setLights(0xff0000ff, 100, 100)
		        .setSmallIcon(R.drawable.ic_launcher)
		        .setContentIntent(stopPendingIntent);
		Notification notification = notBuilder.build();            		  
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		// Flag to hide notification after selection
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notificationManager.notify(ID, notification); 
	}*/
	
}