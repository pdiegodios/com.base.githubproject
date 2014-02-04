package com.base.githubproject.loader;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.base.githubproject.database.DBHelper;
import com.base.githubproject.database.DataSource;
import com.base.githubproject.database.UserDataSource;
import com.base.githubproject.entities.User;
import com.base.githubproject.observer.UpdatedUsersObserver;

/**
 * An implementation of AsyncTaskLoader which loads a {@code List<User>}
 * containing all downloaded github users on the device.
 */
public class UserListLoader extends AsyncTaskLoader<List<User>> {
	private static final String TAG = "UserListLoader";
  	private static final boolean DEBUG = true;
  	// An observer to notify the Loader when new users are downloaded.
  	private UpdatedUsersObserver mUsersObserver;
  	private List<User> mUsers;
  	private Context mContext;

  	public UserListLoader(Context ctx) {
  		super(ctx);
  		mContext=ctx;
  	}

  	/**
  	 * This method is called on a background thread and generates a List of
  	 * {@link User} objects. Each entry corresponds to a single github user
  	 */
  	@Override
  	public List<User> loadInBackground() {
  		if (DEBUG) Log.i(TAG, "loadInBackground() called!");

  		// Retrieve all users from sqliteDb.
  		DBHelper helper = new DBHelper(mContext);
  		SQLiteDatabase database = helper.getWritableDatabase();
  		DataSource<User> dataSource = new UserDataSource(database);
  		List<User> users = dataSource.queryForAll();
  		database.close();
  		if (users == null) {
  			users = new ArrayList<User>();
  		}

	    // Sort the list.
	    Collections.sort(users, ALPHA_COMPARATOR);

	    return users;
  	}


  	/**
  	 * Called when there is new data to deliver to the client. The superclass will
  	 * deliver it to the registered listener (i.e. the LoaderManager), which will
  	 * forward the results to the client through a call to onLoadFinished.
  	 */
  	@Override
  	public void deliverResult(List<User> users) {
  		if (isReset()) {
  			if (DEBUG) Log.w(TAG, "Warning! An async query came in while the Loader was reset!");
  			if (users != null) {
  				releaseResources(users);
  				return;
  			}
  		}

  		// Hold a reference to the old data so it doesn't get garbage collected.
  		// Must protect it until the new data has been delivered.
  		List<User> oldUsers = mUsers;
  		mUsers = users;

  		if (isStarted()) {
  			if (DEBUG) Log.i(TAG, "Delivering results to the LoaderManager for the ListFragment to display!");
  			super.deliverResult(users);
  		}

  		// Invalidate the old data as we don't need it any more.
  		if (oldUsers != null && oldUsers != users) {
  			if (DEBUG) Log.i(TAG, "Releasing any old data associated with this Loader.");
  			releaseResources(oldUsers);
  		}
  	}



  	@Override
  	protected void onStartLoading() {
  		if (DEBUG) 
  			Log.i(TAG, "onStartLoading() called!");
  		if (mUsers != null) {
  			// Deliver any previously loaded data immediately.
  			if (DEBUG) 
  				Log.i(TAG, "Delivering previously loaded data to the client...");
  			deliverResult(mUsers);
  		}

  		// Register the observers that will notify the Loader when changes are made.
  		if (mUsersObserver == null) {
  			if(DEBUG)
  				Log.i(TAG, "Initializing broadcastReceiver mUsersObserver");
  			mUsersObserver = new UpdatedUsersObserver(this);
  		}

  		if (takeContentChanged()) {
  			// When the observer detects new downloaded users, it will call
  			// onContentChanged() on the Loader, which will cause the next call to
  			// takeContentChanged() to return true. If this is ever the case (or if
  			// the current data is null), we force a new load.
  			if (DEBUG) 
  				Log.i(TAG, "A content change has been detected... so force load!");
  			forceLoad();
  		} else if (mUsers == null) {
  			if (DEBUG) 
  				Log.i(TAG, "The current data is data is null... so force load!");
  			forceLoad();
  		}
  	}

  	@Override
  	protected void onStopLoading() {
  		if (DEBUG) Log.i(TAG, "onStopLoading() called!");
  		cancelLoad();
  		// Loaders in a stopped state should still monitor the data source 
  		// for changes so that the Loader will know to force a new load 
  		// if it is ever started again.
  	}

  	@Override
  	protected void onReset() {
  		if (DEBUG) Log.i(TAG, "onReset() called!");
  		// Ensure the loader is stopped.
  		onStopLoading();
  		// At this point we can release the resources associated with 'users'.
  		if (mUsers != null) {
  			releaseResources(mUsers);
  			mUsers = null;
  		}
  		// The Loader is being reset, so we should stop monitoring for changes.
  		if (mUsersObserver != null) {
  			getContext().unregisterReceiver(mUsersObserver);
  			mUsersObserver = null;
  		}
  	}

  	@Override
  	public void onCanceled(List<User> users) {
  		if (DEBUG) 
  			Log.i(TAG, "onCanceled() called!");
  		// Attempt to cancel the current asynchronous load.
  		super.onCanceled(users);
  		// The load has been canceled, so we should release the resources associated with 'mUsers'.
  		releaseResources(users);
  	}

  	@Override
  	public void forceLoad() {
  		if (DEBUG) 
  			Log.i(TAG, "forceLoad() called!");
  		super.forceLoad();
  	}

  	/**
  	 * Helper method to take care of releasing resources associated with an actively loaded data set.
  	 */
  	private void releaseResources(List<User> users) {
  		// For a simple List, there is nothing to do.
  	}

  
  	/**
  	 * Performs alphabetical comparison of {@link User} objects. This is
  	 * used to sort queried data in {@link loadInBackground}.
  	 */
  	private static final Comparator<User> ALPHA_COMPARATOR = new Comparator<User>() {
  		Collator sCollator = Collator.getInstance();

  		@Override
  		public int compare(User user1, User user2) {
  			return sCollator.compare(user1.getLogin(), user2.getLogin());
  		}
  	};
}
