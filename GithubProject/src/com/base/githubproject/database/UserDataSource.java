package com.base.githubproject.database;

import java.util.ArrayList;
import java.util.List;

import com.base.githubproject.entities.User;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class UserDataSource extends DataSource<User>{
	public static final String TABLE = "user";
	public static final String COLUMN_ROWID = "rowid";
	public static final String COLUMN_LOGIN = "login";
	public static final String COLUMN_ID = "id";
	public static final String COLUMN_AVATAR_URL = "avatar_url";
	public static final String COLUMN_GRAVATAR_ID = "gravatar_id";
	public static final String COLUMN_URL = "url";
	public static final String COLUMN_HTML_URL = "html_url";
	public static final String COLUMN_FOLLOWERS_URL = "followers_url";
	public static final String COLUMN_FOLLOWING_URL = "following_url";
	public static final String COLUMN_GISTS_URL = "gists_url";
	public static final String COLUMN_STARRED_URL = "starred_url";
	public static final String COLUMN_SUBSCRIPTIONS_URL = "subscriptions_url";
	public static final String COLUMN_ORGANIZATIONS_URL = "organizations_url";
	public static final String COLUMN_REPOS_URL = "repos_url";
	public static final String COLUMN_EVENTS_URL = "events_url";
	public static final String COLUMN_RECEIVED_EVENTS_URL = "received_events_url";
	public static final String COLUMN_TYPE = "type";
	public static final String COLUMN_SITE_ADMIN = "site_admin";
	private String[] allColumns = {COLUMN_ROWID, COLUMN_LOGIN, COLUMN_ID, COLUMN_AVATAR_URL, COLUMN_GRAVATAR_ID,
			COLUMN_URL, COLUMN_HTML_URL, COLUMN_FOLLOWERS_URL, COLUMN_FOLLOWING_URL, COLUMN_GISTS_URL,
			COLUMN_STARRED_URL, COLUMN_SUBSCRIPTIONS_URL, COLUMN_ORGANIZATIONS_URL, COLUMN_REPOS_URL,
			COLUMN_EVENTS_URL, COLUMN_RECEIVED_EVENTS_URL, COLUMN_TYPE, COLUMN_SITE_ADMIN};
	// Database creation sql statement
	public static final String DATABASE_CREATE = "create table " + TABLE
			+ "(" + COLUMN_ROWID + " integer primary key, "+ COLUMN_LOGIN + " text not null,"
			+ COLUMN_ID+" integer,"+COLUMN_AVATAR_URL+" text,"
			+ COLUMN_GRAVATAR_ID + " text,"+ COLUMN_URL+" text,"
			+ COLUMN_HTML_URL + " text,"+ COLUMN_FOLLOWERS_URL+" text,"
			+ COLUMN_FOLLOWING_URL + " text,"+ COLUMN_GISTS_URL+" text,"
			+ COLUMN_STARRED_URL + " text,"+ COLUMN_SUBSCRIPTIONS_URL+" text,"
			+ COLUMN_ORGANIZATIONS_URL + " text,"+ COLUMN_REPOS_URL+" text,"
			+ COLUMN_EVENTS_URL + " text,"+ COLUMN_RECEIVED_EVENTS_URL+" text,"
			+ COLUMN_TYPE + " integer,"+ COLUMN_SITE_ADMIN+" text"
			+");";

	public UserDataSource(SQLiteDatabase database) {
		super(database);
	}

	@Override
	public boolean insert(User user) {
		if (user == null) {
			return false;
		}
		long result = mDatabase.insert(TABLE, null,getContentValues(user));
		return result != -1;
	}

	@Override
	public boolean delete(User user) {
		if (user == null) {
			return false;
		}
		int result = mDatabase.delete(TABLE, COLUMN_ID + " = " + user.getId(), null);
		return result != 0;
	}

	@Override
	public boolean update(User user) {
		if (user == null) {
			return false;
		}
		int result = mDatabase.update(TABLE, getContentValues(user), 
				COLUMN_ID + " = "+ user.getId(), null);
		return result != 0;
	}

	@Override
	public List<User> queryForAll() {
		Cursor cursor = mDatabase.query(TABLE, allColumns, null, null, null, null, null);
		List<User> users = new ArrayList<User>();
		if (cursor != null && cursor.moveToFirst()) {
			while (!cursor.isAfterLast()) {
				users.add(getObjectFromCursor(cursor));
				cursor.moveToNext();
			}
			cursor.close();
		}
		return users;
	}

	@Override
	public List<User> query(String selection, String[] selectionArgs, 
			String groupBy, String having, String orderBy) {
		Cursor cursor = mDatabase.query(TABLE, allColumns, selection, 
				selectionArgs, groupBy, having, orderBy);
		List<User> users = new ArrayList<User>();
		if (cursor != null && cursor.moveToFirst()) {
			while (!cursor.isAfterLast()) {
				users.add(getObjectFromCursor(cursor));
				cursor.moveToNext();
			}
			cursor.close();
		}
		return users;
	}

	private User getObjectFromCursor(Cursor cursor) {
		if (cursor == null) {
			return null;
		}
		User user = new User();
		user.setRowid(cursor.getLong(cursor.getColumnIndex(COLUMN_ROWID)));
		user.setLogin(cursor.getString(cursor.getColumnIndex(COLUMN_LOGIN)));
		user.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
		user.setAvatar_url(cursor.getString(cursor.getColumnIndex(COLUMN_AVATAR_URL)));
		user.setGravatar_id(cursor.getString(cursor.getColumnIndex(COLUMN_GRAVATAR_ID)));
		user.setUrl(cursor.getString(cursor.getColumnIndex(COLUMN_URL)));
		user.setHtml_url(cursor.getString(cursor.getColumnIndex(COLUMN_HTML_URL)));
		user.setFollowers_url(cursor.getString(cursor.getColumnIndex(COLUMN_FOLLOWERS_URL)));
		user.setFollowing_url(cursor.getString(cursor.getColumnIndex(COLUMN_FOLLOWING_URL)));
		user.setGists_url(cursor.getString(cursor.getColumnIndex(COLUMN_GISTS_URL)));
		user.setStarred_url(cursor.getString(cursor.getColumnIndex(COLUMN_STARRED_URL)));
		user.setSubscriptions_url(cursor.getString(cursor.getColumnIndex(COLUMN_SUBSCRIPTIONS_URL)));
		user.setOrganizations_url(cursor.getString(cursor.getColumnIndex(COLUMN_ORGANIZATIONS_URL)));
		user.setRepos_url(cursor.getString(cursor.getColumnIndex(COLUMN_REPOS_URL)));
		user.setEvents_url(cursor.getString(cursor.getColumnIndex(COLUMN_EVENTS_URL)));
		user.setReceived_events_url(cursor.getString(cursor.getColumnIndex(COLUMN_RECEIVED_EVENTS_URL)));
		user.setType(cursor.getString(cursor.getColumnIndex(COLUMN_TYPE)));
		user.setSite_admin(cursor.getInt(cursor.getColumnIndex(COLUMN_SITE_ADMIN))>0);
		return user;
	}

	public ContentValues getContentValues(User user) {
		if (user == null) {
			return null;
		}
		ContentValues values = new ContentValues();
		values.put(COLUMN_LOGIN, user.getLogin());
		values.put(COLUMN_ID, user.getId());
		values.put(COLUMN_AVATAR_URL, user.getAvatar_url());
		values.put(COLUMN_GRAVATAR_ID, user.getGravatar_id());
		values.put(COLUMN_URL, user.getUrl());
		values.put(COLUMN_HTML_URL, user.getHtml_url());
		values.put(COLUMN_FOLLOWERS_URL, user.getFollowers_url());
		values.put(COLUMN_FOLLOWING_URL, user.getFollowing_url());
		values.put(COLUMN_GISTS_URL, user.getGists_url());
		values.put(COLUMN_STARRED_URL, user.getStarred_url());
		values.put(COLUMN_SUBSCRIPTIONS_URL, user.getSubscriptions_url());
		values.put(COLUMN_ORGANIZATIONS_URL, user.getOrganizations_url());
		values.put(COLUMN_REPOS_URL, user.getRepos_url());
		values.put(COLUMN_EVENTS_URL, user.getEvents_url());
		values.put(COLUMN_RECEIVED_EVENTS_URL, user.getReceived_events_url());
		values.put(COLUMN_TYPE, user.getType());
		values.put(COLUMN_SITE_ADMIN, user.getSite_admin());
		return values;
	}

}
