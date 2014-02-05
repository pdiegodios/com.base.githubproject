package com.base.githubproject.observer;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

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
		// Tell the loader about the change.
		if(mLoader!=null){
			if(DEBUG)
				Log.i(TAG,"communicating new changes to loader");
			mLoader.onContentChanged();
		}	
	}
}