package com.base.githubproject.observer;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;

import com.base.githubproject.database.DBHelper;
import com.base.githubproject.database.UserDataSource;
import com.base.githubproject.entities.User;
import com.base.githubproject.loader.UserListLoader;
import com.base.githubproject.service.GithubService;

/**
 * Used by the {@link UserListLoader}. An observer that listens for
 * new added users (and notifies the loader when these changes are detected).
 */
public class UpdatedUsersObserver extends BroadcastReceiver {
	private static final String TAG = "UpdatedUsersObserver";
	private static final boolean DEBUG = true;

	private UserListLoader mLoader;

	public UpdatedUsersObserver(UserListLoader loader) {
		mLoader = loader;
		// Register for events related to Download from github API
		IntentFilter filter = new IntentFilter(GithubService.NOTIFICATION);
		mLoader.getContext().registerReceiver(this, filter);
	}
	
	public UpdatedUsersObserver(){
		if(mLoader!=null){
			IntentFilter filter = new IntentFilter(GithubService.NOTIFICATION);
			mLoader.getContext().registerReceiver(this, filter);
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (DEBUG) 
			Log.i(TAG, "The observer has detected new users!\n Notifying Loader...");
		Bundle bundle = intent.getExtras();
		if (bundle != null) {
			long resultCode = bundle.getLong(UserDataSource.COLUMN_ID);
			if (resultCode > 0) {
				if(DEBUG) 
					Log.i(TAG, "Downloaded to position "+resultCode);
				if(mLoader!=null){
					callService();
				}	
			} else {
				if(DEBUG) 
					Log.i(TAG, "Download Failed");
			}
		}
		// Tell the loader about the change.
		if(mLoader!=null){
			mLoader.onContentChanged();
		}	
	}

	private void callService() {
		DBHelper helper = new DBHelper(mLoader.getContext());
		SQLiteDatabase database = helper.getWritableDatabase();
		UserDataSource dataSource = new UserDataSource(database);
		List<User> users = dataSource.queryForAll();
		long position=0;
		if(users != null && !users.isEmpty()){
			User lastUser = users.get(users.size()-1);
			position = lastUser.getId();
		}
		database.close();
		Intent iService = new Intent(mLoader.getContext(), GithubService.class);
		iService.putExtra(UserDataSource.COLUMN_ID, position);
		mLoader.getContext().startService(iService);		
	}
}