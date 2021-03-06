package com.base.githubproject;

import java.util.List;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.base.githubproject.adapter.UserListAdapter;
import com.base.githubproject.entities.User;
import com.base.githubproject.loader.UserListLoader;
import com.base.githubproject.service.GithubService;
import com.base.githubproject.widget.AlphabeticalListView;

/**
 * The main activity holds an {@link UserListFragment} which displays the list of
 * users download from github API on the device.
 */
public class MainActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<List<User>>{
	private static final boolean DEBUG = true;
	private static final String TAG = "Main";
    private static final int LOADER_ID = 1;
    private UserListAdapter mAdapter;
    private AlphabeticalListView mAlphaList;
    private TextView mEmptyText;
	private int mInterval = 10;//minutes to retry if there is no connexion

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);		
    	mAdapter = new UserListAdapter(this);
    	mEmptyText = (TextView) findViewById(android.R.id.empty);
    	mAlphaList = (AlphabeticalListView) findViewById(android.R.id.list);
    	//mAlphaList.setFastScrollEnabled(true);
    	setEmptyText(R.string.loading);
        setListShown(false);    	
    	setListAdapter(mAdapter);
	    // Initialize a Loader with id '1'. If the Loader with this id already
	    // exists, then the LoaderManager will reuse the existing Loader.
        getSupportLoaderManager().initLoader(LOADER_ID, null, this);
        
		//Service called to download all users from github
		callService();
	}
	
	private void setEmptyText(int idString) {
		String text = getResources().getString(idString);
		mEmptyText.setText(text);
	}

	private void setListShown(boolean visible) {
		if(visible){
			mAlphaList.setVisibility(View.VISIBLE);
			mEmptyText.setVisibility(View.INVISIBLE);
		}	
		else{
			mAlphaList.setVisibility(View.INVISIBLE);
			mEmptyText.setVisibility(View.VISIBLE);
		}	
	}

	private void setListAdapter(UserListAdapter userAdapter) {
		mAlphaList.setAdapter(userAdapter);
	}

	@Override
	protected void onDestroy() {
		//Prevent service to die
		callService();
		super.onDestroy();
	}
	
    @Override
    public Loader<List<User>> onCreateLoader(int id, Bundle args) {
    	if (DEBUG) 
    		Log.i(TAG, "onCreateLoader() called!");
    	return new UserListLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<List<User>> loader, List<User> data) {
    	if (DEBUG) Log.i(TAG, "onLoadFinished() called!");
    	mAdapter.setData(data);	
    	if(data==null || data.size()==0){
    		//It is necessary to change text message if there is no users
    		setListShown(false);
    		setEmptyText(R.string.empty);
    	}
    	else {
    		setListShown(true);
    	}
    }

    @Override
    public void onLoaderReset(Loader<List<User>> loader) {
    	if (DEBUG) 
    		Log.i(TAG, "onLoaderReset() called!");
    	mAdapter.setData(null);    	
    }

	private void callService() {
        // check the global background data setting
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (!cm.getActiveNetworkInfo().isConnected()) {
        	//If it isn't connected we'll try after interval
            startServiceAfterInterval(mInterval);
        }
        else{
        	//It is connected, so we start the service
    		startServiceAfterInterval(0);
        }
	}

	private void startServiceAfterInterval(int minutes) {
		Intent iService = new Intent(this, GithubService.class);
		if (minutes==0){
			this.startService(iService);
		}
		else{
			AlarmManager am = (AlarmManager) this.getSystemService(ALARM_SERVICE);
		    PendingIntent pi = PendingIntent.getService(this, 0, iService, PendingIntent.FLAG_UPDATE_CURRENT);
			am.cancel(pi);
			am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + minutes*60*1000, pi);
		}
	}
}
