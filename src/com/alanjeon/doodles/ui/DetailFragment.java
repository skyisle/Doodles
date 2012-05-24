
package com.alanjeon.doodles.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.alanjeon.doodles.BaseActivity;
import com.alanjeon.doodles.R;
import com.androidquery.AQuery;

public class DetailFragment extends SherlockFragment {
    public static final String TAG = "DetailFragment";

    DoodleInfo mDoodle;

    private TextView mTitle;
    private WebView mBlogText;
    private ImageView mLogo;

    AQuery aq;

    private TextView mRunDate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reloadFromArguments(getArguments());
        aq = new AQuery(getActivity());
    }

    public void reloadFromArguments(Bundle arguments) {
        // Load new arguments
        final Intent intent = BaseActivity.fragmentArgumentsToIntent(arguments);
        DoodleInfo item = (DoodleInfo) intent.getSerializableExtra("doodle");
        if (item != null) {
            mDoodle = item;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_detail, null);
        mTitle = (TextView) root.findViewById(R.id.title);
        mLogo = (ImageView) root.findViewById(R.id.logo);
        mRunDate = (TextView) root.findViewById(R.id.run_date);
        mBlogText = (WebView) root.findViewById(R.id.blog_text);

        if (mDoodle != null) {
            updateContent(0, mDoodle);
        }

        return root;
    }

    public void updateContent(int position, DoodleInfo info) {

        mTitle.setText(info.title);
        mRunDate.setText(info.run_date);
        aq.id(mLogo).image(info.url);
        if (info.blog_text != null) {
            String template = readAsset("blog_text_template.html",
                    getResources().getAssets());

            final String blog_text = template.replace("BLOG_TEXT_TEMPLATE", info.blog_text);
            mBlogText.post(new Runnable() {
                public void run() {
                    mBlogText.loadData(blog_text, "text/html", "utf-8");
                }
            });
        }
    }

    public static String readAsset(String name, AssetManager asset)
    {
        BufferedReader in = null;

        try {
            in = new BufferedReader(new InputStreamReader(asset.open(name)));

            String line;
            StringBuilder buffer = new StringBuilder();

            while ((line = in.readLine()) != null)
            {
                buffer.append(line).append('\n');
            }

            in.close();
            return buffer.toString();
        } catch (IOException e) {
            return "";
        }
    }

}
