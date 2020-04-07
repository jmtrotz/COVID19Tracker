package com.jefftrotz.covid19tracker;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.net.ssl.HttpsURLConnection;

/**
 * Main activity for COVID-19 Tracker
 * @author Jeffrey Trotz
 * @date 3/30/2020
 * @version 0.0.1
 */
public class MainActivity extends AppCompatActivity
{
    private Spinner mStateSpinner; // Drop down list where the user can manually pick a US state
    private TextView mNumTestedTV; // Text view to display the number of people tested in that state
    private TextView mNumPositiveTestsTV; // Text view to display the number of positive tests in that state
    private TextView mNumDeathsTV; // Text view to display the number of deaths in that state
    private TextView mLastUpdatedTV; // Text view to display the last time the data was updated
    private GraphView mTestsGraph; // Graph to display the number of tests vs number of positive tests
    private GraphView mTimeGraph; // Graph to display the number of positive tests over time
    private GraphView mDeathsGraph; // Graph to display the number of deaths over time
    private ProgressDialog progressDialog; // Progress dialog shown when updating data
    private static final Character SPLIT_CHARACTER = ','; // Character used to "split" the CSV string returned by the API
    private static final String SPLIT_REGEX = ","; // Used by a loop to count the number of commas in the CSV string
    private static final String DATE_TIME_FORMAT = "MM/dd/yyyy hh:mm:ss aa"; // Date/time format used by SimpleDateFormat
    private static final String PROGRESS_DIALOG_TITLE = "Please Wait"; // Title for the progress dialog
    private static final String PROGRESS_DIALOG_TEXT = "Getting Data..."; // Message body for the progress dialog
    private static final String MALFORMED_URL_EXCEPTION_TEXT = "Malformed URL Exception: "; // Text fed into the Log.e() method when logging a Malformed URL exception
    private static final String RESPONSE_CODE_ERROR_TEXT = "Error: response code "; // Text fed into the Log.e() method when logging a response code error
    private static final String ILLEGAL_ARG_EXCEPTION_TEXT = "Illegal Argument Exception: "; // Text fed into the Log.e() method when logging an illegal argument exception
    private static final String API_URL = "https://coronavirusapi.com/getTimeSeries/"; // URL for the API used to get per-state COVID-19 data
    private static final String TAG = "COVID-19 Tracker";   // Tag used when logging errors
    private static final String IO_EXCEPTION_TEXT = "IO Exception: ";   // Text fed into the Log.e() method when an IO exception is logged
    private static final String TESTS_GRAPH_TITLE = "Total Tests vs. Positive Tests";  // Title for the tests graph
    private static final String TIME_GRAPH_TITLE = "Time vs. Positive Tests";    // Title for the time graph
    private static final String DEATHS_GRAPH_TITLE = "Time vs. Deaths";  // Title for the deaths graph

    /**
     * Called when the activity is launched
     * @param savedInstanceState Saved instance of the app
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        // Call super class and set the layout view for the activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize view elements
        mStateSpinner = findViewById(R.id.stateSpinner);
        mNumTestedTV = findViewById(R.id.numTestedTextView);
        mNumPositiveTestsTV = findViewById(R.id.numPositiveTestsTextView);
        mNumDeathsTV = findViewById(R.id.numDeathsTextView);
        mLastUpdatedTV = findViewById(R.id.lastUpdatedTextView);
        mTestsGraph = findViewById(R.id.testsGraph);
        mTimeGraph = findViewById(R.id.timeGraph);
        mDeathsGraph = findViewById(R.id.deathsGraph);

        // Make graphs visible
        mTestsGraph.setVisibility(View.VISIBLE);
        mTimeGraph.setVisibility(View.VISIBLE);
        mDeathsGraph.setVisibility(View.VISIBLE);
        mTestsGraph.setTitle(TESTS_GRAPH_TITLE);
        mTimeGraph.setTitle(TIME_GRAPH_TITLE);
        mDeathsGraph.setTitle(DEATHS_GRAPH_TITLE);

        // Create an ArrayAdapter using the string array in strings.xml and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.states_array, android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears and apply the adapter to the spinner
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mStateSpinner.setAdapter(adapter);

        // Add a listener to the spinner
        mStateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            // Called when an item is selected, or after the value was set programmatically
            // (see getState() method below) using location data from the user's device
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                // Remove old data from the graphs and execute a background thread to get data
                // for the selected state
                new FetchDataTask().execute(new StateNameConverter()
                    .getStateAbbreviation(mStateSpinner.getSelectedItem().toString()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
                // Not used
            }
        });

        // Check if we have permission to access location data
        checkLocationPermission();
    }

    /**
     * Called after location permissions have been granted/denied
     *
     * @param requestCode  Identifier for the request
     * @param permissions  Permissions being requested
     * @param grantResults Permissions that were granted
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        // Show an alert dialog if the request code is not 0 (0 = approved)
        if (requestCode != 0)
        {
            showNoLocationPermissionAlert();
        }

        // If the request was approved, get the user's location
        else
        {
            getLocation();
        }
    }

    /**
     * Method to check that the app has permission to access location data
     */
    private void checkLocationPermission()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED)
        {
            // If we don't, show dialog to request access to location data
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        // If we already have permission, call getLocation() to get location data
        else
        {
            getLocation();
        }
    }

    /**
     * Gets the user's current location (data provided by Google Play Services)
     */
    private void getLocation()
    {
        // Create Fused Location Provider Client object to access location data via Google Play Services
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>()
        {
            // Called if a location object was successfully returned by Google Play Services
            @Override
            public void onSuccess(Location location)
            {
                // If the location object is not null, try to get the name of the state
                if (location != null)
                {
                    extractState(location);
                }

                // If the location object is null, let the user know so they can manually
                // pick their state from the spinner
                else
                {
                    showNoLocationDataAlert();
                }
            }
        });
    }

    /**
     * Method to extract the state name from the location data returned by Google Play services
     * @param location Location object provided by Google Play Services
     */
    private void extractState(Location location)
    {
        // Geocoder object for converting latitude/longitude data into a state name
        Geocoder geocoder = new Geocoder(getBaseContext(), Locale.getDefault());

        try
        {
            // Try to convert lat/long numbers into a state name
            List<Address> address = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

            // If the address is not blank, set the value selected in the spinner so the
            // onItemSelected() will be called and run a background thread
            if (address.size() > 0)
            {
                mStateSpinner.setSelection(new StateNameConverter()
                    .getStateSpinnerIndex(address.get(0).getAdminArea()), true);
            }
        }

        // Catch and log any IO exceptions
        catch (IOException ioException)
        {
            Log.e(TAG, IO_EXCEPTION_TEXT + ioException.getMessage());
            ioException.printStackTrace();
        }
    }

    /**
     * Method to show an alert dialog to the user in the event that location
     * permissions are denied
     */
    private void showNoLocationPermissionAlert()
    {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setMessage(R.string.no_permissions_dialog_text)
            .setPositiveButton(R.string.dialog_positive_button_text, new DialogInterface.OnClickListener()
            {
                // If the positive button is clicked, launch settings so the user can grant location permissions
                public void onClick(DialogInterface dialog, int id)
                {
                    startActivityForResult(new Intent(Settings.ACTION_APPLICATION_SETTINGS), 0);
                }
            })
            .setNegativeButton(R.string.dialog_negative_button_text, new DialogInterface.OnClickListener()
            {
                // If the negative button is clicked, close the alert dialog
                public void onClick(DialogInterface dialog, int id)
                {
                    dialog.dismiss();
                }
            });

        alertDialog.create();
        alertDialog.show();
    }

    /**
     * Method to show an alert dialog to the user in the event that the
     * location object is null (no recent data stored on device)
     */
    private void showNoLocationDataAlert()
    {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setMessage(R.string.no_location_data_dialog_message)
            .setPositiveButton(R.string.dialog_positive_button_text, new DialogInterface.OnClickListener()
            {
                // If the positive button is clicked, launch settings so the user can grant location permissions
                public void onClick(DialogInterface dialog, int id)
                {
                    startActivityForResult(new Intent(Settings.ACTION_APPLICATION_SETTINGS), 0);
                }
            })
            .setNegativeButton(R.string.dialog_negative_button_text, new DialogInterface.OnClickListener()
            {
                // If the negative button is clicked, close the alert dialog
                public void onClick(DialogInterface dialog, int id)
                {
                    dialog.dismiss();
                }
            });

        alertDialog.create();
        alertDialog.show();
    }

    /**
     * Nested class to run a background thread to fetch the latest COVID-19 data for the selected state
     */
    private class FetchDataTask extends AsyncTask<String, String, String>
    {
        /**
         * Called before the thread has executed
         */
        @Override
        protected void onPreExecute()
        {
            // Show a progress dialog and remove any previously graphed data
            progressDialog = ProgressDialog.show(MainActivity.this, PROGRESS_DIALOG_TITLE, PROGRESS_DIALOG_TEXT);
            mTestsGraph.removeAllSeries();
            mTimeGraph.removeAllSeries();
            mDeathsGraph.removeAllSeries();
        }

        /**
         * Background task to contact the API and download the latest COVID-19 data
         * @param params URL for the API
         * @return Returns the data received from the API as a String
         */
        @Override
        protected String doInBackground(String... params)
        {
            // Declare HTTPS Connection and Buffered Reader objects
            HttpsURLConnection connection = null;
            BufferedReader reader = null;

            try
            {
                // Create URL object and initialize HTTPS Connection object
                URL url = new URL(API_URL + params[0]);
                connection = (HttpsURLConnection) url.openConnection();

                // Log the error if the HTTPS response code is not 200
                if (connection.getResponseCode() != 200)
                {
                    Log.e(TAG, RESPONSE_CODE_ERROR_TEXT + connection.getResponseCode());
                }

                // Attempt to read data if the HTTPS response code is 200
                else
                {
                    // Create InputStream and StringBuffer objects. Initialize BufferedReader object
                    InputStream inputStream = connection.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder builder = new StringBuilder();
                    String line;

                    // Read data from the API into the "line" String initialized above
                    while ((line = reader.readLine()) != null)
                    {
                        builder.append(line).append(SPLIT_CHARACTER);
                    }

                    // Return the data read from the API
                    return builder.toString();
                }
            }

            // Catch and log Malformed URL Exceptions
            catch (MalformedURLException malUrlException)
            {
                Log.e(TAG, MALFORMED_URL_EXCEPTION_TEXT + malUrlException.getMessage());
                malUrlException.printStackTrace();
            }

            // Catch and log IO Exceptions
            catch (IOException ioException)
            {
                Log.e(TAG, IO_EXCEPTION_TEXT + ioException.getMessage());
                ioException.printStackTrace();
            }

            // Clean up after the try block executes or after an exception is caught
            finally
            {
                // Close the connection (if it exists)
                if (connection != null)
                {
                    connection.disconnect();
                }

                // Try to close the BufferedReader (if it exists)
                try
                {
                    if (reader != null)
                    {
                        reader.close();
                    }
                }

                // Catch and log IO Exceptions
                catch (IOException ioException)
                {
                    Log.e(TAG, IO_EXCEPTION_TEXT + ioException.getMessage());
                    ioException.printStackTrace();
                }
            }

            // Default return value (if the string builder object was not returned above)
            return null;
        }

        /**
         * Called after the thread has finished
         * @param apiResult Result of doInBackground(). The result string contains 4 pieces of data
         * in this order: time the stats were updated, number of people tested, number of positive
         * tests, and # number of deaths. The most recent numbers are always at the end of the
         * string.
         */
        @Override
        protected void onPostExecute(String apiResult)
        {
            super.onPostExecute(apiResult);

            int numOfCommas = 0; // Stores the number of commas in the CSV string

            // Loop through the result string and count the number of commas for use in the array declared below
            for (int i = 0; i < apiResult.length(); i ++)
            {
                if (apiResult.charAt(i) == SPLIT_CHARACTER)
                {
                    numOfCommas++;
                }
            }

            // Array to store the data nested between the commas in the result string
            String[] covidData = new String[numOfCommas + 1];

            // Loop through the result string and put each piece of data into the array declared above
            for (int i = 0; i <= apiResult.length(); i++)
            {
                covidData = apiResult.split(SPLIT_REGEX);
            }

            int arrayIndex = 0; // Used in the loop below to load data into an array
            Date[] dateOfStats = new Date[10]; // Used in the loop below to store 10 dates for graphing

            // Loop through the data string and extract the last 5 dates
            for (int i = (numOfCommas - 4); i > (numOfCommas - 41) ; i -= 4)
            {
                dateOfStats[arrayIndex] =  new Date(Long.parseLong(covidData[i]) * 1000);
                arrayIndex++;
            }

            System.out.println("Time: " + covidData[numOfCommas - 4]);
            System.out.println("Tests: " + covidData[numOfCommas - 3]);
            System.out.println("Positive: " + covidData[numOfCommas - 2]);
            System.out.println("Deaths: " + covidData[numOfCommas - 1]);

            // Call methods to load data into the graphs and update the app's UI
            // (created separate methods to save space in this method)
            this.loadTestsGraph(numOfCommas, covidData);
            this.loadTimeGraph(numOfCommas, covidData, dateOfStats);
            this.loadDeathsGraph(numOfCommas, covidData, dateOfStats);
            this.updateUI(numOfCommas, covidData, dateOfStats);


            // Close the progress dialog if it's still showing
            if (progressDialog.isShowing())
            {
                progressDialog.dismiss();
            }
        }

        /**
         * Feeds data points into the graph to show the number of positive tests vs total number of tests
         * @param data Array of strings containing COVID-19 data for the selected state
         * @param commas Number of commas in the CSV String returned by the API
         */
        private void loadTestsGraph(int commas, String[] data)
        {
            try
            {
                // Create a series of data points to be plotted
                LineGraphSeries<DataPoint> testedSeries = new LineGraphSeries<>(new DataPoint[]
                {
                    new DataPoint(Long.parseLong(data[commas - 39]), Long.parseLong(data[commas - 38])),
                    new DataPoint(Long.parseLong(data[commas - 35]), Long.parseLong(data[commas - 34])),
                    new DataPoint(Long.parseLong(data[commas - 31]), Long.parseLong(data[commas - 30])),
                    new DataPoint(Long.parseLong(data[commas - 27]), Long.parseLong(data[commas - 26])),
                    new DataPoint(Long.parseLong(data[commas - 23]), Long.parseLong(data[commas - 22])),
                    new DataPoint(Long.parseLong(data[commas - 19]), Long.parseLong(data[commas - 18])),
                    new DataPoint(Long.parseLong(data[commas - 15]), Long.parseLong(data[commas - 14])),
                    new DataPoint(Long.parseLong(data[commas - 11]), Long.parseLong(data[commas - 10])),
                    new DataPoint(Long.parseLong(data[commas - 7]), Long.parseLong(data[commas - 6])),
                    new DataPoint(Long.parseLong(data[commas - 3]), Long.parseLong(data[commas - 2]))
                });

                // Set the number of labels for the X/Y axis (only 4 due to space)
                mTestsGraph.getGridLabelRenderer().setNumVerticalLabels(4);
                mTestsGraph.getGridLabelRenderer().setNumHorizontalLabels(4);

                // Set manual X & Y bounds to have nice steps
                mTestsGraph.getViewport().setMinX(Long.parseLong(data[commas - 39]));
                mTestsGraph.getViewport().setMaxX(Long.parseLong(data[commas - 3]));
                mTestsGraph.getViewport().setMinY(Long.parseLong(data[commas - 38]));
                mTimeGraph.getViewport().setMaxY(Long.parseLong(data[commas - 2]));

                // Add the series of data points to the graph
                mTestsGraph.addSeries(testedSeries);
            }

            // Catch and log any illegal argument exceptions thrown above
            catch (IllegalArgumentException illegalArgException)
            {
                Log.e(TAG, ILLEGAL_ARG_EXCEPTION_TEXT + illegalArgException.getMessage());
                illegalArgException.printStackTrace();
            }
        }

        /**
         * Feeds data points into the graph showing the number of positive tests over time
         * @param commas Number of commas in the CSV String returned by the API
         * @param data Array of strings containing COVID-19 data for the selected state
         * @param dates Array of dates extracted from the data array
         */
        private void loadTimeGraph(int commas, String[] data, Date[] dates)
        {
            try
            {
                // Create a series of data points to be plotted
                LineGraphSeries <DataPoint> timeSeries = new LineGraphSeries<>(new DataPoint[]
                {
                    new DataPoint(dates[9], Long.parseLong(data[commas - 38])),
                    new DataPoint(dates[8], Long.parseLong(data[commas - 34])),
                    new DataPoint(dates[7], Long.parseLong(data[commas - 30])),
                    new DataPoint(dates[5], Long.parseLong(data[commas - 26])),
                    new DataPoint(dates[5], Long.parseLong(data[commas - 22])),
                    new DataPoint(dates[4], Long.parseLong(data[commas - 18])),
                    new DataPoint(dates[3], Long.parseLong(data[commas - 14])),
                    new DataPoint(dates[2], Long.parseLong(data[commas - 10])),
                    new DataPoint(dates[1], Long.parseLong(data[commas - 6])),
                    new DataPoint(dates[0], Long.parseLong(data[commas - 2]))
                });

                // Set date label formatter and number of X/Y axis labels (only 4 due to space)
                mTimeGraph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(MainActivity.this));
                mTimeGraph.getGridLabelRenderer().setNumVerticalLabels(4);
                mTimeGraph.getGridLabelRenderer().setNumHorizontalLabels(4);

                // Set manual X & Y bounds to have nice steps
                mTimeGraph.getViewport().setMinX(dates[9].getTime());
                mTimeGraph.getViewport().setMaxX(dates[0].getTime());
                mTimeGraph.getViewport().setMinY(Long.parseLong(data[commas - 38]));
                mTimeGraph.getViewport().setMaxY(Long.parseLong(data[commas - 2]));

                // Add the series of data points to the graph
                mTimeGraph.addSeries(timeSeries);
            }

            // Catch and log any illegal argument exceptions thrown above
            catch (IllegalArgumentException illegalArgException)
            {
                Log.e(TAG, ILLEGAL_ARG_EXCEPTION_TEXT + illegalArgException.getMessage());
                illegalArgException.printStackTrace();
            }
        }

        /**
         * Feeds data points into the graph showing the number of deaths tests over time
         * @param commas Number of commas in the CSV String returned by the API
         * @param data Array of Strings containing COVID-19 data for the selected state
         * @param dates Array of dates extracted from the data array
         */
        private void loadDeathsGraph(int commas, String[] data, Date[] dates)
        {
            try
            {
                // Create a series of data points to be plotted
                LineGraphSeries <DataPoint> deathsSeries = new LineGraphSeries<>(new DataPoint[]
                {
                    new DataPoint(dates[9], Long.parseLong(data[commas - 37])),
                    new DataPoint(dates[8], Long.parseLong(data[commas - 33])),
                    new DataPoint(dates[7], Long.parseLong(data[commas - 29])),
                    new DataPoint(dates[6], Long.parseLong(data[commas - 25])),
                    new DataPoint(dates[5], Long.parseLong(data[commas - 21])),
                    new DataPoint(dates[4], Long.parseLong(data[commas - 17])),
                    new DataPoint(dates[3], Long.parseLong(data[commas - 13])),
                    new DataPoint(dates[2], Long.parseLong(data[commas - 9])),
                    new DataPoint(dates[1], Long.parseLong(data[commas - 5])),
                    new DataPoint(dates[0], Long.parseLong(data[commas - 1]))
                });

                // Set date label formatter and number of X/Y axis labels (only 4 due to space)
                mDeathsGraph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(MainActivity.this));
                mDeathsGraph.getGridLabelRenderer().setNumVerticalLabels(4);
                mDeathsGraph.getGridLabelRenderer().setNumHorizontalLabels(4);

                // Set manual X & Y bounds to have nice steps
                mDeathsGraph.getViewport().setMinX(dates[9].getTime());
                mDeathsGraph.getViewport().setMaxX(dates[0].getTime());
                mDeathsGraph.getViewport().setMinY(Long.parseLong(data[commas - 37]));
                mDeathsGraph.getViewport().setMaxY(Long.parseLong(data[commas - 1]));

                // Add the series of data points to the graph
                mDeathsGraph.addSeries(deathsSeries);
            }

            // Catch and log any illegal argument exceptions thrown above
            catch (IllegalArgumentException illegalArgException)
            {
                Log.e(TAG, ILLEGAL_ARG_EXCEPTION_TEXT + illegalArgException.getMessage());
                illegalArgException.printStackTrace();
            }
        }

        /**
         * Updates the UI for the app with the data obtained from the API
         * @param commas Number of commas in the CSV String returned by the API
         * @param data Array of Strings containing COVID-19 data for the selected state
         * @param dates Array of dates extracted from the data array
         */
        private void updateUI(int commas, String[] data, Date[] dates)
        {
            // Used below to format the latest date from the CSV string from epoch time to
            // something a little more "eye friendly"
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);

            // Get the latest stats from the data string and format the numbers so they're nicer
            // to look at, then display them in the UI
            mNumTestedTV.setText(NumberFormat.getNumberInstance(Locale.US).format(Long.parseLong(data[commas - 3])));
            mNumPositiveTestsTV.setText(NumberFormat.getNumberInstance(Locale.US).format(Long.parseLong(data[commas - 2])));
            mNumDeathsTV.setText(NumberFormat.getNumberInstance(Locale.US).format(Long.parseLong(data[commas - 1])));
            mLastUpdatedTV.setText(dateFormat.format(dates[0]));
        }
    }
}