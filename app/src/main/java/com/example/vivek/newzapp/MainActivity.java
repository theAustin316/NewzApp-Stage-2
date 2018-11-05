package com.example.vivek.newzapp;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<Newz>> {

    private static final String LOG_TAG = MainActivity.class.getName();

    String apiKey = BuildConfig.GUARDIAN_API_KEY;
    /**
     * URL for news data from the Guardian API
     */
    private static final String GUARDIAN_BASE_URL =
            "https://content.guardianapis.com/search?";
    /**
     * Constant value for the news loader ID.
     **/
    private static final int NEWS_LOADER_ID = 1;

    /**
     * Adapter for the list of news
     */
    private NewzAdapter mAdapter;

    /**
     * TextView that is displayed when the list is empty
     */
    private TextView mEmptyStateTextView;

    /**
     * ImageView that is displayed when the list is empty
     */
    private ImageView mEmptyStateImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView List = findViewById(R.id.list);

        mEmptyStateTextView = findViewById(R.id.empty_TextView);
        List.setEmptyView(mEmptyStateTextView);

        mEmptyStateImageView = findViewById(R.id.empty_ImageView);
        List.setEmptyView((mEmptyStateImageView));

        // Create a new adapter that takes an empty list of news as input
        mAdapter = new NewzAdapter(this, new ArrayList<Newz>());

        // Set the adapter on the {@link ListView}
        // so the list can be populated in the user interface
        List.setAdapter(mAdapter);

        List.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Newz currentNewsArticle = mAdapter.getItem(position);

                // Convert the String URL into a URI object (to pass into the Intent constructor)
                Uri articleUri = Uri.parse(currentNewsArticle.getmWebURL());

                // Create a new intent to view the article URI
                Intent websiteIntent = new Intent(Intent.ACTION_VIEW, articleUri);

                // Send the intent to launch a new activity
                startActivity(websiteIntent);
            }
        });

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);

        // Get details on the currently active default data network
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        // If there is a network connection, fetch data
        if (networkInfo != null && networkInfo.isConnected()) {
            // Get a reference to the LoaderManager, in order to interact with loaders.
            LoaderManager loaderManager = getLoaderManager();

            // Initialize the loader. Pass in the int ID constant defined above and pass in null for
            // the bundle. Pass in this activity for the LoaderCallbacks parameter (which is valid
            // because this activity implements the LoaderCallbacks interface).
            loaderManager.initLoader(NEWS_LOADER_ID, null, this);
        } else {
            // Otherwise, display error
            // First, hide loading indicator so error message will be visible
            View loadingIndicator = findViewById(R.id.loading_indicator);
            loadingIndicator.setVisibility(View.GONE);

            // Update empty state with no connection error message
            mEmptyStateTextView.setText(R.string.no_internet_connection);
            mEmptyStateImageView.setImageResource(R.drawable.no_internet_connection);
        }

    }

    @Override
    public Loader<List<Newz>> onCreateLoader(int id, Bundle args) {

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // getString retrieves a String value from the preferences. The second parameter is the default value for this preference.
        String minMagnitude = sharedPrefs.getString(
                getString(R.string.settings_min_newz_key),
                getString(R.string.settings_min_newz_default));

        // parse breaks apart the URI string that's passed into its parameter
        Uri baseUri = Uri.parse(GUARDIAN_BASE_URL);

        // buildUpon prepares the baseUri that we just parsed so we can add query parameters to it
        Uri.Builder uriBuilder = baseUri.buildUpon();

        String orderBy = sharedPrefs.getString(
                getString(R.string.settings_order_by_key),
                getString(R.string.settings_order_by_default)
        );

        // Append query parameter and its value. For example, the `show-fields=trailText,byline`
        uriBuilder.appendQueryParameter("api-key",apiKey);
        uriBuilder.appendQueryParameter("show-tags","contributor");
        uriBuilder.appendQueryParameter("show-fields","trailText,byline");
        uriBuilder.appendQueryParameter("page-size", minMagnitude);
        uriBuilder.appendQueryParameter("order-by", orderBy);

        // Return the completed uri
        return new NewzLoader(this, uriBuilder.toString());

    }

    @Override
    public void onLoadFinished(Loader<List<Newz>> loader, List<Newz> data) {
        // Hide loading indicator because the data has been loaded
        View loadingIndicator = findViewById(R.id.loading_indicator);
        loadingIndicator.setVisibility(View.GONE);

        // Clear the adapter of previous earthquake data
        mAdapter.clear();

        // If there is a valid list of {@link Newz}s, then add them to the adapter's
        // data set. This will trigger the ListView to update.
        if (data != null && !data.isEmpty()) {
            mAdapter.addAll(data);
        }
        else{
            // Set empty state text to display "No News found."
            mEmptyStateTextView.setText(R.string.no_news_found);
            // Set empty state image to display logo
            mEmptyStateImageView.setImageResource(R.drawable.guard);
        }

    }

    @Override
    public void onLoaderReset(Loader<List<Newz>> loader) {
        // Loader reset, so we can clear out our existing data.
        mAdapter.clear();
    }

    @Override
    // This method initialize the contents of the Activity's options menu
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    // This method is called whenever an item in the options menu is selected.
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}