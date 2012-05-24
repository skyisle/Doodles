
package com.alanjeon.doodles.ui;

import android.app.Activity;

import com.actionbarsherlock.app.SherlockListFragment;

public class ContractListFragment<T> extends SherlockListFragment {
    private T mContract;

    @SuppressWarnings("unchecked")
    @Override
    public void onAttach(Activity activity) {
        try {
            mContract = (T) activity;
        } catch (ClassCastException e) {
            throw new IllegalStateException(activity.getClass().getSimpleName()
                    + " does not implement " + getClass().getSimpleName()
                    + "'s contract interface.", e);
        }
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContract = null;
    }

    public final T getContract() {
        return mContract;
    }

}
