
package com.alanjeon.doodles.ui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.alanjeon.doodles.R;
import com.alanjeon.doodles.util.DoodleLoader;
import com.alanjeon.doodles.util.ImageFetcher;
import com.alanjeon.doodles.util.UIUtils;

public class ItemListFragment extends ContractListFragment<ItemListFragment.Contract> implements
        LoaderManager.LoaderCallbacks<List<DoodleInfo>>, OnNavigationListener {
    // DoodleInfo

    ArrayList<DoodleInfo> mItem = new ArrayList<DoodleInfo>();

    String mSelectedMonth;

    // Adapters
    private ArrayAdapter<String> mMonthAdapter;
    private DoodleListAdapter mAdapter;
    ImageFetcher mImageFetcher;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mImageFetcher = UIUtils.getImageFetcher(getSherlockActivity());
        mAdapter = new DoodleListAdapter(getSherlockActivity(), mImageFetcher);
        setListAdapter(mAdapter);

        ActionBar actionBar = getSherlockActivity().getSupportActionBar();
        ArrayList<String> strings = new ArrayList<String>();
        Calendar c = Calendar.getInstance();

        mSelectedMonth = c.get(Calendar.YEAR) + "/" + (c.get(Calendar.MONTH) + 1);
        for (int i = 0; i < 30; i++) {
            strings.add(c.get(Calendar.YEAR) + "/" + (c.get(Calendar.MONTH) + 1));
            c.add(Calendar.MONTH, -1);
        }

        getListView().setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (SCROLL_STATE_IDLE != scrollState) {
                    mImageFetcher.setPauseDiskCache(true);
                } else {
                    mImageFetcher.setPauseDiskCache(false);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                    int totalItemCount) {
                // TODO Auto-generated method stub

            }
        });

        mMonthAdapter = new ArrayAdapter<String>(actionBar.getThemedContext(),
                R.layout.sherlock_spinner_item, strings);
        mMonthAdapter.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks(mMonthAdapter, this);

        // Start out with a progress indicator.
        setListShown(false);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        Bundle bundle = new Bundle();
        bundle.putString("ID", mSelectedMonth);
        getLoaderManager().initLoader(0, bundle, this);

    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        getContract().onItemSelectChanged(position, mAdapter.getItem(position));
    }

    public void updateList(String month) {
        Bundle bundle = new Bundle();
        bundle.putString("ID", month);
        getLoaderManager().destroyLoader(0);
        getLoaderManager().initLoader(0, bundle, this);
    }

    public interface Contract {
        public void onItemSelectChanged(int position, DoodleInfo info);
    }

    @Override
    public Loader<List<DoodleInfo>> onCreateLoader(int id, Bundle arg) {
        return new DoodleLoader(getActivity(), arg.getString("ID"));
    }

    @Override
    public void onLoadFinished(Loader<List<DoodleInfo>> loader, List<DoodleInfo> data) {
        // Set the new data in the adapter.
        mAdapter.setData(data);

        // The list should now be shown.
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<DoodleInfo>> loader) {
        // Clear the data in the adapter.
        mAdapter.setData(null);
    }

    public static class DoodleListAdapter extends ArrayAdapter<DoodleInfo> {
        private final LayoutInflater mInflater;
        private ImageFetcher mFetcher;

        public DoodleListAdapter(Context context, ImageFetcher imageFetcher) {
            super(context, android.R.layout.simple_list_item_2);
            mFetcher = imageFetcher;

            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @TargetApi(11)
        public void setData(List<DoodleInfo> data) {
            clear();
            if (data != null) {
                if (UIUtils.isHoneycomb()) {
                    addAll(data);
                } else {
                    for (DoodleInfo info : data) {
                        add(info);
                    }
                }
            }
        }

        class ViewHolder {
            ImageView logo;
            TextView title;
            TextView run_date;
        }

        /**
         * Populate new items in the list.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            ViewHolder holder;
            if (convertView == null) {
                view = mInflater.inflate(R.layout.list_item, parent, false);
                holder = new ViewHolder();
                holder.logo = (ImageView) view.findViewById(R.id.icon);
                holder.run_date = (TextView) view.findViewById(R.id.run_date);
                holder.title = (TextView) view.findViewById(R.id.title);
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (ViewHolder) view.getTag();
            }

            DoodleInfo item = getItem(position);

            mFetcher.loadThumbnailImage(item.url, holder.logo);

            holder.title.setText(item.title);
            holder.run_date.setText(item.run_date);

            return view;
        }
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        updateList(mMonthAdapter.getItem(itemPosition));
        return true;
    }

}
