package com.base.githubproject.adapter;

import java.util.List;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.base.githubproject.R;
import com.base.githubproject.entities.User;

/**
 * A custom ArrayAdapter used by the {@link UserListFragment} to display the
 * users from github.
 */
public class UserListAdapter extends ArrayAdapter<User> implements SectionIndexer, OnItemClickListener {
	private LayoutInflater mInflater;
	private static String sections = "#abcdefghilmnopqrstuvz";
	private Context mContext;

	public UserListAdapter(Context ctx) {
		super(ctx, android.R.layout.simple_list_item_2);
		mContext=ctx;
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
		//((ImageView) view.findViewById(R.id.icon)).setImageDrawable(userPicture);
		((TextView) view.findViewById(R.id.text)).setText(item.getLogin());

		return view;
	}

	public void setData(List<User> data) {
		setNotifyOnChange(false); //// Prevents 'clear()' from clearing/resetting the listview
		clear();
		if (data != null) {
			addAll(data);
		}
		notifyDataSetChanged();
	}

	@Override
	public int getSectionForPosition(int position) {
		return 0;
	}

	@SuppressLint("DefaultLocale")
	@Override
	public int getPositionForSection(int section) {
		for (int i=0; i < this.getCount(); i++) {
			char initialLetter = this.getItem(i).toString().toLowerCase().charAt(0);
			if (section == 0){ //Section is #
				if(!Character.isLetter(initialLetter)){
					return i;
				}
			}
			else if (initialLetter == sections.charAt(section))
				return i;
		}
		return 0;
	}

	@Override
	public Object[] getSections() {
		String[] sectionsArr = new String[sections.length()];
		for (int i=0; i < sections.length(); i++)
			sectionsArr[i] = "" + sections.charAt(i);
		return sectionsArr;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
		User userClicked = (User) getItem(position);
		String url = userClicked.getHtml_url();
			Log.i("CLICK!!!", "User "+userClicked.getLogin()+" clicked\n accessing to url: "+url);
		Intent iWeb = new Intent(Intent.ACTION_VIEW);
		iWeb.setData(Uri.parse(url));
		mContext.startActivity(iWeb);		
	}
}
