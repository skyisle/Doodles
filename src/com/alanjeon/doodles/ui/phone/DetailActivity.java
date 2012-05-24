
package com.alanjeon.doodles.ui.phone;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import com.alanjeon.doodles.BaseActivity;
import com.alanjeon.doodles.ui.DetailFragment;

public class DetailActivity extends BaseActivity {
    private static final String TAG = "DoodleDetailActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();

        if (savedInstanceState == null) {
            DetailFragment list = new DetailFragment();
            list.setArguments(intentToFragmentArguments(getIntent()));
            fm.beginTransaction().add(android.R.id.content, list).commit();
        }
    }
}
