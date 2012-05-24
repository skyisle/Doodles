
package com.alanjeon.doodles;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import com.alanjeon.doodles.ui.DetailFragment;
import com.alanjeon.doodles.ui.DoodleInfo;
import com.alanjeon.doodles.ui.ItemListFragment;
import com.alanjeon.doodles.ui.phone.DetailActivity;

public class HomeActivity extends BaseActivity implements ItemListFragment.Contract {

    private ItemListFragment mListFragment;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        FragmentManager fm = getSupportFragmentManager();
        mListFragment = (ItemListFragment) fm.findFragmentById(R.id.fragment_list);
    }

    public void onItemSelectChanged(int position, DoodleInfo info) {
        DetailFragment detailFrag = (DetailFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_detail);
        if (detailFrag == null) {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra("position", position);
            intent.putExtra("doodle", info);
            startActivity(intent);
        } else {
            detailFrag.updateContent(position, info);
        }
    }
}
