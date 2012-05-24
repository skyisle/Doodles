/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alanjeon.doodles.util;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.alanjeon.doodles.HomeActivity;
import com.alanjeon.doodles.R;

/**
 * An extension of {@link ActivityHelper} that provides Android 3.0-specific functionality for
 * Honeycomb tablets. It thus requires API level 11.
 */
public class ActivityHelper {
    protected SherlockFragmentActivity mActivity;

    private Menu mOptionsMenu;

    public ActivityHelper(SherlockFragmentActivity activity) {
        mActivity = activity;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        mOptionsMenu = menu;
        mActivity.getSupportMenuInflater().inflate(R.menu.default_menu_items, menu);
        return false;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Handle the HOME / UP affordance. Since the app is only two levels deep
                // hierarchically, UP always just goes home.
                goHome();
                return true;
            case R.id.menu_search:
                goSearch();
                return true;
        }
        return false;
    }
    
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            return true;
        }
        return false;
    }

    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            goHome();
            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    public void setupHomeActivity() {
        // NOTE: there needs to be a content view set before this is called, so this method
        // should be called in onPostCreate.
        if (UIUtils.isTablet(mActivity)) {
            mActivity.getSupportActionBar().setDisplayOptions(
                    ActionBar.DISPLAY_SHOW_HOME,
                    ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        } else {
            mActivity.getSupportActionBar().setDisplayOptions(
                    ActionBar.DISPLAY_USE_LOGO,
                    ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_TITLE);
        }
    }

    /** {@inheritDoc} */
    public void setupSubActivity() {
        // NOTE: there needs to be a content view set before this is called, so this method
        // should be called in onPostCreate.
        if (UIUtils.isTablet(mActivity)) {
            mActivity.getSupportActionBar().setDisplayOptions(
                    ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_USE_LOGO,
                    ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_USE_LOGO);
        } else {
            mActivity.getSupportActionBar().setDisplayOptions(
                    ActionBar.DISPLAY_HOME_AS_UP,
                    ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_USE_LOGO);
        }
    }

    /**
     * Invoke "home" action, returning to
     * {@link com.google.android.apps.iosched.ui.HomeActivity}.
     */
    public void goHome() {
        if (mActivity instanceof HomeActivity) {
            return;
        }

        final Intent intent = new Intent(mActivity, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mActivity.startActivity(intent);

        if (!UIUtils.isHoneycomb()) {
            mActivity.overridePendingTransition(R.anim.home_enter,
                    R.anim.home_exit);
        }
    }

    /**
     * Invoke "search" action, triggering a default search.
     */
    public void goSearch() {
        mActivity.startSearch(null, false, Bundle.EMPTY, false);
    }
    
//    /**
//    -     * No-op on Honeycomb. The action bar color always remains the same.
//    -     */
//    -    @Override
//    -    public void setActionBarColor(int color) {
//    -        if (!UIUtils.isTablet(mActivity)) {
//    -            super.setActionBarColor(color);
//    -        }
//    -    }
    
    /** {@inheritDoc} */
    public void setRefreshActionButtonCompatState(boolean refreshing) {
        // On Honeycomb, we can set the state of the refresh button by giving it a custom
        // action view.
        if (mOptionsMenu == null) {
            return;
        }

        final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
        if (refreshItem != null) {
            if (refreshing) {
                refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
            } else {
                refreshItem.setActionView(null);
            }
        }
    }

    public void setupActionBar(Object object, int i) {
        // TODO Auto-generated method stub
        
    }

    public void setActionBarTitle(CharSequence title) {
        // TODO Auto-generated method stub
        
    }
}
