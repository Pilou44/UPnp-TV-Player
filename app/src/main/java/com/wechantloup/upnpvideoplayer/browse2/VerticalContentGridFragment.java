/**
 * This file was modified by Amazon:
 * Copyright 2015-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.wechantloup.upnpvideoplayer.browse2;

import android.os.Bundle;
import android.util.Log;

import androidx.leanback.app.VerticalGridFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.VerticalGridPresenter;

/**
 * A fragment that displays content in a vertical grid.
 */
public class VerticalContentGridFragment extends VerticalGridFragment {

    private static final String TAG = VerticalContentGridFragment.class.getSimpleName();
    private static final int NUM_COLUMNS = 10;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
//        ContentContainer mContentContainer = ContentBrowser.getInstance(getActivity())
//                                                           .getLastSelectedContentContainer();
//        setTitle(mContentContainer.getName());
        setTitle("Toto");
        setupFragment();
    }

    private void setupFragment() {

        VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
        gridPresenter.setNumberOfColumns(NUM_COLUMNS);
        setGridPresenter(gridPresenter);

        ArrayObjectAdapter mAdapter = new ArrayObjectAdapter(new CardPresenter());

//        ContentContainer contentContainer = ContentBrowser.getInstance(getActivity())
//                                                          .getLastSelectedContentContainer();
//        for (Content content : contentContainer) {
//            mAdapter.add(content);
//        }
        setAdapter(mAdapter);

//        setOnItemViewClickedListener((itemViewHolder, item, rowViewHolder, row) -> {
//            Log.i(TAG, "item clicked: " + ((Content) item).getTitle());
//            if (item instanceof Content) {
//                Content content = (Content) item;
//                Log.d(TAG, "Content with title " + content.getTitle() + " was clicked");
//
//                ContentBrowser.getInstance(getActivity())
//                              .setLastSelectedContent(content)
//                              .switchToScreen(ContentBrowser.CONTENT_DETAILS_SCREEN, content);
//            }
//        });
//
//        setOnItemViewSelectedListener((itemViewHolder, item, rowViewHolder, row) ->
//                                              Log.i(TAG, "item selected: " +
//                                                      ((Content) item).getTitle())
//        );
//
//        setOnSearchClickedListener(view -> ContentBrowser.getInstance(
//                                           getActivity()).switchToScreen(
//                                           ContentBrowser.CONTENT_SEARCH_SCREEN)
//        );
    }
}
