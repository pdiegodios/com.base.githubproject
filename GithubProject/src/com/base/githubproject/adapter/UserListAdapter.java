package com.base.githubproject.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.base.githubproject.R;
import com.base.githubproject.MainActivity.UserListFragment;
import com.base.githubproject.entities.User;

/**
 * A custom ArrayAdapter used by the {@link UserListFragment} to display the
 * users from github.
 */
public class UserListAdapter extends ArrayAdapter<User> {
	private LayoutInflater mInflater;

	public UserListAdapter(Context ctx) {
		super(ctx, android.R.layout.simple_list_item_2);
		mInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;

		if (convertView == null) {
			view = mInflater.inflate(R.layout.list_item_icon_text, parent, false);
		} else {
			view = convertView;
		}

		User item = getItem(position);
		//TODO: Load picture from User
		((ImageView) view.findViewById(R.id.icon)).setImageDrawable(null);
		((TextView) view.findViewById(R.id.text)).setText(item.getLogin());

		return view;
	}

	public void setData(List<User> data) {
		clear();
		if (data != null) {
			for (int i = 0; i < data.size(); i++) {
				add(data.get(i));
			}
		}
	}
}
