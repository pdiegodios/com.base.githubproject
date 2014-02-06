package com.base.githubproject.adapter;

import java.util.List;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.base.githubproject.R;
import com.base.githubproject.entities.User;

/**
 * A custom ArrayAdapter used by the {@link AlphabeticalListView} to display the
 * users from github.
 */
public class UserListAdapter extends ArrayAdapter<User> implements SectionIndexer{
	public static String sections = "#abcdefghijklmnopqrstuvwxyz";
	private LayoutInflater mInflater;
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

		ImageView imageIcon = (ImageView) view.findViewById(R.id.icon);
		TextView textLogin = (TextView) view.findViewById(R.id.text);
		User item = getItem(position);
		final String login = item.getLogin();
		final String url = item.getHtml_url();

		//TODO: Load picture 
		//imageIcon.setImageDrawable(userPicture)
		if(login.length()==1){
			textLogin.setText(login.toUpperCase());
			textLogin.setTextColor(Color.BLUE);
			imageIcon.setImageResource(R.drawable.ic_header);
			textLogin.setOnClickListener(null);
			imageIcon.setOnClickListener(null);
		}
		else{
			textLogin.setText(login);
			textLogin.setTextColor(Color.BLACK);
			imageIcon.setImageResource(R.drawable.ic_launcher);
			launchUrlOnClick(textLogin, url);
			launchUrlOnClick(imageIcon, url);	
		}

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
	
	private void launchUrlOnClick(View v, String gitUrl){
		final String url = gitUrl;
		v.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				Intent iWeb = new Intent(Intent.ACTION_VIEW);
				iWeb.setData(Uri.parse(url));
				mContext.startActivity(iWeb);					
			}
		});
	}
}
