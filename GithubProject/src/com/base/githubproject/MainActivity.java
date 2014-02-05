package com.base.githubproject;

import java.util.List;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;

import com.base.githubproject.adapter.UserListAdapter;
import com.base.githubproject.database.DBHelper;
import com.base.githubproject.database.UserDataSource;
import com.base.githubproject.entities.User;
import com.base.githubproject.loader.UserListLoader;
import com.base.githubproject.service.GithubService;

/**
 * The main activity holds an {@link UserListFragment} which displays the list of
 * users download from github API on the device.
 */
public class MainActivity extends FragmentActivity {
	private static final boolean DEBUG = true;
	private static final String TAG = "MainActivity";
	private int mInterval = 3;//minutes

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Create the ListFragment and add it as our sole content.
		FragmentManager fm = getSupportFragmentManager();
		if (fm.findFragmentById(android.R.id.content) == null) {
			UserListFragment list = new UserListFragment();
			fm.beginTransaction().add(android.R.id.content, list).commit();
		}	
		//Service called to download all users from github
		callService();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private void callService() {
		DBHelper helper = new DBHelper(this);
		SQLiteDatabase database = helper.getWritableDatabase();
		UserDataSource dataSource = new UserDataSource(database);
		List<User> users = dataSource.queryForAll();
		long position=0;
		if(users != null && !users.isEmpty()){
			User lastUser = users.get(users.size()-1);
			position = lastUser.getId();
		}
		database.close();
		Intent iService = new Intent(this, GithubService.class);
		iService.putExtra(UserDataSource.COLUMN_ID, position);
        // check the global background data setting
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (!cm.getActiveNetworkInfo().isConnected()) {     
        	if(DEBUG)
        		Log.i(TAG, "it is not possible to connect");
        	//If it isn't connected we'll try after interval
            startServiceAfterInterval(iService, mInterval);
        }
        else{
        	//It is connected, so we start the service
        	startService(iService);		
        }
	}

	private void startServiceAfterInterval(Intent iService, int minutes) {
		AlarmManager am = (AlarmManager) this.getSystemService(ALARM_SERVICE);
	    PendingIntent pi = PendingIntent.getService(this, 0, iService, PendingIntent.FLAG_UPDATE_CURRENT);
		am.cancel(pi);
		am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime() + minutes*60*1000,
				minutes*60*1000, pi);
	}

	/**
	 * This ListFragment displays a list of all github users on the
	 * device as its sole content. It uses an {@link UserListLoader} to load
	 * its data and the LoaderManager to manage the loader across the activity
	 * and fragment life cycles.
	 */
	public static class UserListFragment extends ListFragment implements 
			LoaderManager.LoaderCallbacks<List<User>> {
		private static final String TAG = "UserListFragment";
		private static final boolean DEBUG = true;
	    private UserListAdapter mAdapter;
	    private static final int LOADER_ID = 1;

	    @Override
	    public void onActivityCreated(Bundle savedInstanceState) {
	    	super.onActivityCreated(savedInstanceState);
	    	mAdapter = new UserListAdapter(getActivity());
	    	setEmptyText("No users");
	    	setListAdapter(mAdapter);
	    	setListShown(false);

	    	if (DEBUG) {
	    		Log.i(TAG, "Calling initLoader()!");
		    	if (getLoaderManager().getLoader(LOADER_ID) == null) {
		    		Log.i(TAG, "Initializing the new Loader...");
		    	} else {
		    		Log.i(TAG, "Reconnecting with existing Loader (id '1')");
		    	}
	    	}

		    // Initialize a Loader with id '1'. If the Loader with this id already
		    // exists, then the LoaderManager will reuse the existing Loader.
		    getLoaderManager().initLoader(LOADER_ID, null, this);
	    }

	    @Override
	    public Loader<List<User>> onCreateLoader(int id, Bundle args) {
	    	if (DEBUG) 
	    		Log.i(TAG, "onCreateLoader() called!");
	    	return new UserListLoader(getActivity());
	    }
	
	    @Override
	    public void onLoadFinished(Loader<List<User>> loader, List<User> data) {
	    	if (DEBUG) 
	    		Log.i(TAG, "onLoadFinished() called!\n"+
	    				"isResumed()="+isResumed());
	    	mAdapter.setData(data);	
	    	if (isResumed()) {
	    		setListShown(true);
	    	} else {
	    		setListShownNoAnimation(true);
	    	}
	    }
	
	    @Override
	    public void onLoaderReset(Loader<List<User>> loader) {
	    	if (DEBUG) 
	    		Log.i(TAG, "onLoadReset() called!");
	    	mAdapter.setData(null);
	    }
	}
}
