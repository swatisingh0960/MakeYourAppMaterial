package com.example.xyzreader.ui;

import android.animation.ObjectAnimator;
//import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
//import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
//import android.util.TypedValue;
//import android.view.MenuItem;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Adapter;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.adapter.ArticleListAdapter;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static android.content.ContentValues.TAG;
import static com.example.xyzreader.utilities.Utils.CURRENT_ARTICLE_POSITION;
import static com.example.xyzreader.utilities.Utils.STARTING_ARTICLE_POSITION;
import static java.util.Arrays.asList;

import android.support.v4.content.Loader;
import android.support.v4.app.LoaderManager;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = ArticleListActivity.class.toString();
    public List<String> colorsHolder = asList("#d27456","#9f8d87","#e7467c","#b354c3","#b9c6cc");

    @BindView(R.id.appBarLayout)
    AppBarLayout appBarLayout;
    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private Bundle mTmpReenterState;
    private boolean mIsRefreshing;
    public static boolean mIsDetailsActivityStarted;

    private final SharedElementCallback mCallback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (mTmpReenterState != null){
                int startingPosition = mTmpReenterState.getInt(STARTING_ARTICLE_POSITION);
                int currentPosition = mTmpReenterState.getInt(CURRENT_ARTICLE_POSITION);
                if (startingPosition != currentPosition){
                    String newTransitionName = getString(R.string.book_image) + currentPosition;
                    View newSharedElement = mRecyclerView.findViewWithTag(newTransitionName);
                    if (newSharedElement != null){
                        names.clear();
                        names.add(newTransitionName);
                        sharedElements.clear();
                        sharedElements.put(newTransitionName, newSharedElement);
                    }
                }
                mTmpReenterState = null;
            } else {
                //if mTmpReenterState is null, then the activity is exiting.
                View navigationBar = findViewById(android.R.id.navigationBarBackground);
                View statusBar = findViewById(android.R.id.statusBarBackground);
                if (navigationBar != null){
                    names.add(navigationBar.getTransitionName());
                    sharedElements.put(navigationBar.getTransitionName(),navigationBar);
                }
                if (statusBar != null){
                    names.add(statusBar.getTransitionName());
                    sharedElements.put(statusBar.getTransitionName(), statusBar);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);
        ButterKnife.bind(this);
//        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setExitSharedElementCallback(mCallback);
        }

        mSwipeRefreshLayout.setOnRefreshListener(this);

        getSupportLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            startService(new Intent(this, UpdaterService.class));
        }

        IntentFilter filter = new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE);
        filter.addAction(UpdaterService.EXTRA_REFRESHING);
        LocalBroadcastManager.getInstance(this).registerReceiver(mRefreshingReceiver, filter);
    }

//    private void refresh() {
//        startService(new Intent(this, UpdaterService.class));
//    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
        }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsDetailsActivityStarted = false;
    }

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
            } else if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())){
                mSwipeRefreshLayout.setRefreshing(false);
                Toast.makeText(ArticleListActivity.this, getString(R.string.empty_recycle_view_no_network),Toast.LENGTH_LONG).show();
            }
        }
    };

//    private void updateRefreshingUI() {
//        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
//    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

//
//    @Override
//    public void onLoaderReset(@NonNull android.support.v4.content.Loader<Cursor> loader) {
//
//    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        ArticleListAdapter adapter = new ArticleListAdapter(cursor, this, R.layout.list_item_article);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    @Override
    public void onRefresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home){
            ActivityCompat.finishAfterTransition(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);

        mTmpReenterState = new Bundle(data.getExtras());
        final int startingPosition = mTmpReenterState.getInt(STARTING_ARTICLE_POSITION);
        int currentPosition = mTmpReenterState.getInt(CURRENT_ARTICLE_POSITION);

        if (startingPosition != currentPosition){
            mRecyclerView.scrollToPosition(currentPosition);
        }
        if (Build.VERSION.SDK_INT >=Build.VERSION_CODES.LOLLIPOP){
            postponeEnterTransition();
            mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                        mRecyclerView.requestLayout();
                        startPostponedEnterTransition();
                        return true;
                    }
                    return false;
                }
            });
        }
    }
    public static boolean getmIsdetailsActivityStarted(){
        return mIsDetailsActivityStarted;
    }
}
