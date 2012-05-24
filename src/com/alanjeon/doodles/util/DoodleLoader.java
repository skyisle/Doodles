
package com.alanjeon.doodles.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.alanjeon.doodles.ui.DoodleInfo;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;

public class DoodleLoader extends AsyncTaskLoader<List<DoodleInfo>> {
    List<DoodleInfo> mDoodles;
    String mDate;
    String URL_BASE = "http://www.google.com/doodles/json/";

    public DoodleLoader(Context context, String date) {
        super(context);
        mDate = date;
    }

    @Override
    public List<DoodleInfo> loadInBackground() {
        List<DoodleInfo> entries = new ArrayList<DoodleInfo>();

        try {

            AQuery aq = new AQuery(getContext());
            long expire = 24 * 60 * 60 * 1000;

            AjaxCallback<JSONArray> cb = new AjaxCallback<JSONArray>();
            cb.url(URL_BASE + mDate).type(JSONArray.class);
            cb.expire(expire);

            aq.sync(cb);

            JSONArray itemAray = cb.getResult();
            for (int i = 0; i < itemAray.length(); i++) {
                JSONObject item = itemAray.getJSONObject(i);
                DoodleInfo doodle = new DoodleInfo();

                doodle.title = item.getString("title");
                doodle.name = item.getString("name");
                doodle.url = "http:" + item.getString("url");
                doodle.blog_text = item.getString("blog_text");
                SimpleDateFormat format = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
                doodle.run_date = format.format(new Date(item.getLong("run_date") * 1000))
                        .toString();

                entries.add(doodle);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            entries = null;
        } catch (NullPointerException e) {
            e.printStackTrace();
            entries = null;
        }

        return entries;
    }

    @Override
    public void deliverResult(List<DoodleInfo> doodles) {
        if (isReset()) {
            // An async query came in while the loader is stopped. We
            // don't need the result.
            if (doodles != null) {
                onReleaseResources(doodles);
            }
        }
        List<DoodleInfo> oldApps = doodles;
        mDoodles = doodles;

        if (isStarted()) {
            // If the Loader is currently started, we can immediately
            // deliver its results.
            super.deliverResult(doodles);
        }

        // At this point we can release the resources associated with
        // 'oldApps' if needed; now that the new result is delivered we
        // know that it is no longer in use.
        if (oldApps != null) {
            onReleaseResources(oldApps);
        }
    }

    /**
     * Handles a request to start the Loader.
     */
    @Override
    protected void onStartLoading() {
        if (mDoodles != null) {
            // If we currently have a result available, deliver it
            // immediately.
            deliverResult(mDoodles);
        }

        if (takeContentChanged() || mDoodles == null) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }
    }

    /**
     * Handles a request to stop the Loader.
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    /**
     * Handles a request to cancel a load.
     */
    @Override
    public void onCanceled(List<DoodleInfo> doodles) {
        super.onCanceled(doodles);

        // At this point we can release the resources associated with
        // 'doodles'
        // if needed.
        onReleaseResources(doodles);
    }

    /**
     * Handles a request to completely reset the Loader.
     */
    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        // At this point we can release the resources associated with
        // 'doodles'
        // if needed.
        if (mDoodles != null) {
            onReleaseResources(mDoodles);
            mDoodles = null;
        }
    }

    /**
     * Helper function to take care of releasing resources associated with an
     * actively loaded data set.
     */
    protected void onReleaseResources(List<DoodleInfo> doodles) {
        // For a simple List<> there is nothing to do. For something
        // like a Cursor, we would close it here.
    }
}
