/*
 * Copyright (c) 2013 Andrés Cordero 
 * Web: https://github.com/Andrew67/DdrFinder
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.andrew67.ddrfinder.adapters;

import java.util.ArrayList;
import java.util.List;

import com.andrew67.ddrfinder.R;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * Creates the list of actions for an arcade location.
 */
public class ActionListAdapter extends BaseAdapter {
	
	public static final int ACTION_NAVIGATE = 0;
	public static final int ACTION_MOREINFO = 1;
	public static final int ACTION_COPYGPS = 2;
	public static final int ACTION_SYGIC_DRIVE = 3;
	public static final int ACTION_SYGIC_WALK = 4;
	
	private final Context context;
	private final String[] actionNames;
	private final List<Integer> actionList;
	public ActionListAdapter(Context context) {
		this.context = context;
		this.actionNames =
				context.getResources().getStringArray(R.array.location_actions);
		this.actionList = new ArrayList<Integer>();
		
		// Detect the presence of the Sygic navigation app
		boolean sygicInstalled = false;
		try {
			context.getPackageManager().getPackageInfo("com.sygic.aura", 0);
			sygicInstalled = true;
		} catch(NameNotFoundException e) {
			// Do nothing; already false
		}
		
		// Detect API version (for clipboard support)
		boolean clipboardSupported = false;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			clipboardSupported = true;

		// Add options to list, depending on availability
		for (int id = 0; id < actionNames.length; ++id) {
			if ((id == ACTION_SYGIC_DRIVE || id == ACTION_SYGIC_WALK)
				&& !sygicInstalled) {
				continue;
			}
			else if (id == ACTION_COPYGPS && !clipboardSupported) {
				continue;
			}
			actionList.add(id);
		}
	}

	@Override
	public int getCount() {
		return actionList.size();
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return actionList.get(position);
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		TextView action;
		if (view == null) {
			action = new TextView(context);
		}
		else {
			action = (TextView) view;
		}
		action.setText(actionNames[actionList.get(position)]);
		action.setTextSize(context.getResources().getDimension(
				R.dimen.location_action_text));
		return action;
	}

}
