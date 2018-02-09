package com.udacity.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.sync.QuoteSyncJob;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.udacity.stockhawk.ui.MainActivity.TICKER;

/**
 * Created by devbox on 1/15/17.
 */

public class MainFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener,
        StockAdapter.StockAdapterOnClickHandler,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int CHART_HANDLER = 7;
    private static final int STOCK_LOADER = 0;
    private static final String MSG_STRING = "MSG_STRING";
    private StockAdapter mAdapter;
    private String mTickerNotFoundKey;
    private String mStatusPrefKey;
    private static SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    private static SharedPreferences sharedPreferences;
    private Context mContext;
    private boolean twoPane;
    private StockChartFragment chartFragment;
    private int scrollPosition;
    private static Handler handler;


    public static final String SCROLL_POS = "SCROLL_POS";

    @BindView(R.id.recycler_view)
    RecyclerView stockRecyclerView;
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.error)
    TextView error;
    @BindView(R.id.fab)
    FloatingActionButton fab;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mContext = getContext();

        mTickerNotFoundKey = getString(R.string.pref_not_found_key);
        mStatusPrefKey = getString(R.string.pref_status_key);


        chartFragment = new StockChartFragment();
        twoPane = MainActivity.isTwoPane();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);


        if (savedInstanceState != null && savedInstanceState.containsKey(SCROLL_POS)) {
            scrollPosition = savedInstanceState.getInt(SCROLL_POS);
        } else {
            scrollPosition = 0;
        }

        View rootView = (View) inflater.inflate(R.layout.main_fragment, container, false);

        ButterKnife.bind(this, rootView);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                button(view);
            }
        });
        mAdapter = new StockAdapter(mContext, this);
        stockRecyclerView.setAdapter(mAdapter);
        stockRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setRefreshing(true);


        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                String symbol = mAdapter.getSymbolAtPosition(viewHolder.getAdapterPosition());
                PrefUtils.removeStock(mContext, symbol);
                mContext.getContentResolver().delete(Contract.Quote.makeUriForStock(symbol), null, null);
            }


        }).attachToRecyclerView(stockRecyclerView);

        //handler for manipulating charts and toast messages off UI thread
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == CHART_HANDLER) {
                    if (mAdapter.getItemCount() > 0) {
                        loadStockChart(twoPane, mAdapter.getSymbolAtPosition(scrollPosition));
                    }
                    else {
                        chartFragment.clearChart();
                    }
                } else if (msg.getData().containsKey(MSG_STRING)) {
                    Toast.makeText(getContext(), msg.getData().getString(MSG_STRING), Toast.LENGTH_SHORT).show();
                }
            }
        };

        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        //refresh data
        onRefresh();

        getActivity().getSupportLoaderManager().initLoader(STOCK_LOADER, null, this);

        return rootView;

    }

    public void notifyAdapterOfDataChange(){
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SCROLL_POS, scrollPosition);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }


    @Override
    public void onResume() {
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(mContext,
                Contract.Quote.URI,
                Contract.Quote.QUOTE_COLUMNS,
                null, null, Contract.Quote.COLUMN_SYMBOL);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        swipeRefreshLayout.setRefreshing(false);

        if (data.getCount() > 0 && networkUp()) {
            error.setVisibility(View.GONE);
        } else if (data.getCount() > 0 && !networkUp()) {
            error.setVisibility(View.GONE);
            PrefUtils.setPrefStatusCode(getContext(), PrefUtils.STATUS_NO_NET_OLD_DATA);
        }

        mAdapter.setCursor(data);
        stockRecyclerView.smoothScrollToPosition(scrollPosition);

        //load default or last used chart
        if (twoPane) {
            handler.sendEmptyMessage(CHART_HANDLER);
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        swipeRefreshLayout.setRefreshing(false);
        mAdapter.setCursor(null);
    }


    @Override
    public void onClick(String symbol) {
        scrollPosition = mAdapter.getCursorPosition();
        loadStockChart(twoPane, symbol);
    }

    public void setErrorMessageVisible(boolean flag) {
        if (flag) {
            stockRecyclerView.setVisibility(View.GONE);
            error.setVisibility(View.VISIBLE);
        } else {
            stockRecyclerView.setVisibility(View.VISIBLE);
            error.setVisibility(View.GONE);
        }
    }

    private boolean networkUp() {
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onRefresh() {

        //used to facilitate state change tracking
        PrefUtils.setPrefStatusCode(mContext, PrefUtils.STATUS_UNKNOWN);

        if (!sharedPreferences.getBoolean(getString(R.string.pref_stocks_initialized_key), false)) {
            QuoteSyncJob.initialize(mContext);
        }


        if (!networkUp() && mAdapter.getItemCount() == 0) {
            swipeRefreshLayout.setRefreshing(false);
            error.setText(getString(R.string.error_no_network));
            error.setVisibility(View.VISIBLE);
        } else if (!networkUp()) {
            swipeRefreshLayout.setRefreshing(false);
            PrefUtils.setPrefStatusCode(getContext(), PrefUtils.STATUS_NO_NET_OLD_DATA);
            Toast.makeText(mContext, getString(R.string.toast_no_connectivity), Toast.LENGTH_LONG).show();
        } else if (PrefUtils.getStocks(mContext).size() == 0) {
            swipeRefreshLayout.setRefreshing(false);
            error.setText(getString(R.string.error_no_stocks));
            error.setVisibility(View.VISIBLE);
        } else {
            error.setVisibility(View.GONE);
            QuoteSyncJob.syncImmediately(mContext);

        }


    }


    public void button(View view) {
        new AddStockDialog().show(getFragmentManager(), "StockDialogFragment");
    }

    public void addStock(String symbol) {
        if (symbol != null && !symbol.isEmpty()) {

            if (networkUp()) {
                swipeRefreshLayout.setRefreshing(true);
            } else {
                String message = getString(R.string.toast_stock_added_no_connectivity, symbol);
                Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
            }

            PrefUtils.addStock(mContext, symbol);
            QuoteSyncJob.syncImmediately(mContext);
        }
    }

    public void loadStockChart(boolean twoPane, String symbol) {
        if (symbol != null && !symbol.isEmpty()) {

            if (!twoPane) {
                Intent intent = new Intent(mContext, ChartActivity.class);
                intent.putExtra(TICKER, symbol);
                startActivity(intent);
            } else {
                Bundle args = new Bundle();
                args.putString(MainActivity.TICKER, symbol);
                if (!chartFragment.isVisible()) {
                    chartFragment.setArguments(args);
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.stock_chart_fragment, chartFragment).commit();
                } else {
                    chartFragment.changeTicker(symbol);
                }
            }
        }
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        managePrefChange(sharedPreferences, s);
    }

    public void managePrefChange(SharedPreferences sharedPreferences, String s) {

        if (s.equals(mStatusPrefKey)) {
            @PrefUtils.StatusCode int statusCode = sharedPreferences.getInt(s, PrefUtils.STATUS_UNKNOWN);

            switch (statusCode) {
                case PrefUtils.STATUS_SYNC_COMPLETE: {
                    setErrorMessageVisible(false);
                    String message = getString(R.string.toast_refresh_complete);
                    sendToastToUI(message);

                    break;
                }
                case PrefUtils.STATUS_NOT_FOUND: {
                    swipeRefreshLayout.setRefreshing(false);
                    break;
                }
                case PrefUtils.STATUS_NO_NET_NO_DATA: {
                    setErrorMessageVisible(true);
                    break;
                }
                case PrefUtils.STATUS_NO_NET_OLD_DATA: {
                    String message = getString(R.string.error_no_network_old_data);
                    sendToastToUI(message);
                    break;
                }
                case PrefUtils.STATUS_DATA_ERROR: {
                    setErrorMessageVisible(true);
                    error.setText(getString(R.string.error_invalid_data));
                    swipeRefreshLayout.setRefreshing(false);
                    break;
                }
                //used to manage preference change triggers
                case PrefUtils.STATUS_UNKNOWN: {
                    break;
                }

            }
        } else if (s.equals(mTickerNotFoundKey)) {
            String ticker = sharedPreferences.getString(mTickerNotFoundKey, null);

            if (ticker != null && !ticker.isEmpty()) {
                String toastText = String.format(getResources().getString(R.string.toast_ticker_not_found), ticker);
                Toast.makeText(mContext, toastText, Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
                sharedPreferences.edit().clear().apply();

            }
        }

    }

    /**
     * sends a toast message to UI thread
     * @param text
     */
    public void sendToastToUI(String text) {
        Bundle bundle = new Bundle();
        bundle.putString(MSG_STRING, text);
        Message msg = Message.obtain();
        msg.setData(bundle);
        handler.sendMessage(msg);

    }

}
