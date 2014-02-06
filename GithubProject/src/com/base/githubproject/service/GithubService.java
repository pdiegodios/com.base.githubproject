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

import com.base.githubproject.R;
import com.base.githubproject.database.DBHelper;
import com.base.githubproject.database.UserDataSource;
import com.base.githubproject.entities.User;
import com.google.gson.Gson;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Service to make calls to github API {@link http://developer.github.com/v3/}.
 * For unauthenticated requests, the rate limit allows you to make up to 60 requests per hour
 * So, it is necessary to manage limit raised (403 forbidden status) to stop service and call 
 * it again an hour later. 
 * The user can stop service manually using a notification displayed in notification bar.
 */
public class GithubService extends IntentService {
	public static final String NOTIFICATION = "com.base.githubproject.service.receiver";
	public static final String STOP = "com.base.githubproject.service.stop";
	private static final String URL = "https://api.github.com/users?since=";
	private static final String TAG = "GithubService";
	private static final boolean DEBUG = true;
	private static final int ID = 1;
	private BroadcastReceiver mReceiver;
	private DBHelper mDBHelper;
	//flag to stop manually by user
	private boolean mStop = false;
	//minutes to check again if there are new users from Github API
	private int mInterval = 60; 
    private long mPosition;

	public GithubService() {
		super(GithubService.class.toString());
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	};
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		if(DEBUG) Log.i(TAG, "Destroying Service");
		super.onDestroy();
        if (mDBHelper != null) { 
        	mDBHelper.close();
            mDBHelper = null;
        }
		NotificationManager nm = (NotificationManager) this.getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(ID);
	};
    
    @Override
	protected void onHandleIntent(Intent intent) {
    	mPosition = getLastUserId();
        // Start to download existing Github users calling Github API
        // start from the last position saved in database
    	boolean keepAlive=true;
    	registerReceiver();
        while(keepAlive && !mStop){      
        	//will be downloading users until finish while there are more users in github
        	String read_users = readUserFeed(URL+mPosition);        		
        	callNotification();
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
	    				//to warn the broadcast receiver: There are new users to load
	    				publish();
	    			}
	    			else{
	    				//if user==null=>numItems==0=>There is no more users to download
	    				keepAlive=false;
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
        //If it wasn't manually stopped it will reset after mInterval minutes
        if(!mStop) {
        	startServiceAfterInterval(mInterval);  	
        }	
		unregisterReceiver(mReceiver);
        stopSelf();
    }
        

    
	//AUXILIAR METHODS	
    
    /**
     * Method to make a GET call to WebService returning String depending Status Response.
     * @param url: url to get JSON
     * @return 
     * status = 200 OK: It will obtain a String to convert in JSONArray with all elements returned from WebService. 
     * status = 403 Forbidden: Empty String. It will reset service after specific interval.
     * another status: Empty String.
     */
	private String readUserFeed(String url){
		if(DEBUG) Log.i(TAG, "GET "+url);
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
						"you've probably exceeded limit of requests per hour. The service " +
						"will be restart after 1 hour");
			}
			else {
				Log.e(this.toString(), "Failed to download file "+statusCode);	
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
		if(mDBHelper==null){
			mDBHelper = new DBHelper(this.getBaseContext());
		}
		SQLiteDatabase database = mDBHelper.getWritableDatabase();
		UserDataSource dataSource = new UserDataSource(database);
		for(User user:users){
			dataSource.insert(user);
		}
		database.close();
	}
	
	/**
	 * Query to local database asking for last user ID added.
	 * @return last github user ID added to sqlite DB or 0 if there are no users
	 */
	private long getLastUserId(){
		long lastId = 0;
		if(mDBHelper==null){
			mDBHelper = new DBHelper(this.getBaseContext());
		}
		SQLiteDatabase database = mDBHelper.getReadableDatabase();
		UserDataSource dataSource = new UserDataSource(database);
		List<User> users = dataSource.queryForAll();
		if(users!=null & !users.isEmpty()){
			lastId = users.get(users.size()-1).getId();
		}
		database.close();
		return lastId;
	}
	
	/**
	 * Send Broadcast to warn loader that there are new changes
	 */
	private void publish() {
		if(DEBUG) Log.i(TAG,"Publishing changes");
	    Intent intent = new Intent();
	    intent.setAction(NOTIFICATION);
	    sendBroadcast(intent);
	}

	/**
	 * Method to reset service after a specific time
	 * @param minutes: time to start the service again
	 */
	private void startServiceAfterInterval(int minutes) {
		if(DEBUG) Log.i(TAG, "Restarting service in "+minutes+" minutes");		
		NotificationManager nm = (NotificationManager) this.getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);
		AlarmManager am = (AlarmManager) this.getSystemService(ALARM_SERVICE);
		Intent iService = new Intent(this.getBaseContext(), GithubService.class);
	    PendingIntent pi = PendingIntent.getService(this, 0, iService, PendingIntent.FLAG_UPDATE_CURRENT);
		//cancel current service & notification
	    am.cancel(pi);
		nm.cancel(ID);
		//set service to restart after {@param minutes}
		am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime() + minutes*60*1000, pi);
	}
	
	/**
	 * Auxiliar method to stop loop manually and consequently...the service.
	 */
	private void stop(){
		if(DEBUG) Log.i(TAG,"stopping service");
		mStop=true;
	}
	
	/**
	 * New receiver to hear if STOP action is called.
	 * It will be called when user push the notification.
	 * If it happens stop() method is called.
	 */
	private void registerReceiver(){
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
	}
	
	/**
	 * Cancel current notification if it exists and send new notification
	 * to notification bar to warn users than Service is working on background
	 */
	private void callNotification(){    	
		//Send Stop Action when Notification is pushed
		Intent stopIntent = new Intent(STOP);
		PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, stopIntent, 0);
		// Build the notification
		String text = "getting users since ID "+mPosition;
		NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(this.getBaseContext())
		        .setContentTitle("Downloading... PUSH TO STOP")
		        .setContentText(text)
		        .setSmallIcon(R.drawable.ic_notification)
		        .setContentIntent(stopPendingIntent);
		Notification notification = notBuilder.build();            		  
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		// Flag to hide notification after selection
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notificationManager.notify(ID, notification); 
	}
	
}