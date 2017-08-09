package com.silleknarf.notebooknumbers;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.Player;
import com.google.example.games.basegameutils.BaseGameUtils;

import java.text.NumberFormat;

/**
 * The main activity which hosts the Notebook Numbers webview and interacts with the leaderboard
 */
public class NotebookNumbers extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    WebSettings wSettings;
    WebView webView;

    // Client used to interact with Google APIs
    private GoogleApiClient mGoogleApiClient;

    // Are we currently resolving a connection failure?
    private boolean mResolvingConnectionFailure = false;

    // request codes we use when invoking an external activity
    private static final int RC_SIGN_IN = 9001;
    private static final int RC_LEADERBOARD = 8001;


    final String TAG = "NNMainActivity";

    // Has the user clicked the sign-in button?
    private boolean mSignInClicked = false;

    // Automatically start the sign-in flow when the Activity starts
    private boolean mAutoStartSignInFlow = true;

    private static final String LOGGED_IN_EVENT = "SYSTEM:LEADERBOARDS:LOGGED_IN";
    private static final String LOGGED_OUT_EVENT = "SYSTEM:LEADERBOARDS:LOGGED_OUT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_notebook_numbers);

        webView = (WebView) findViewById(R.id.webView);
        webView.setClickable(true);
        wSettings = webView.getSettings();
        wSettings.setAllowFileAccessFromFileURLs(true);
        wSettings.setJavaScriptEnabled(true);
        wSettings.setDomStorageEnabled(true);
        wSettings.setUserAgentString(
                wSettings.getUserAgentString() +
                " notebook-numbers-android"
        );
        webView.loadUrl("file:///android_asset/notebook-numbers/index.html");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        webView.addJavascriptInterface(this, "AppInterface");

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Create the Google API Client with access to Games
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();
    }

    private void executeJavaScriptEvent(String event)
    {
        Log.d(TAG, "Executing JS event: " + event);
        webView.loadUrl("javascript:eventManager.vent.trigger('" + event + "')");
    }

    @android.webkit.JavascriptInterface
    public boolean isSignedIn() {
        return (mGoogleApiClient != null && mGoogleApiClient.isConnected());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart(): connecting");
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop(): disconnecting");
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @android.webkit.JavascriptInterface
    public void updateLeaderboards(int finalScore) {
        NumberFormat nf = NumberFormat.getInstance();
        String formattedScore = nf.format(finalScore);
        String scoreText = "Submitting score of: " + formattedScore + " to the leaderboards";

        Log.d(TAG, scoreText);

        // Notify the user
        Toast.makeText(this.getApplicationContext(), scoreText, Toast.LENGTH_SHORT).show();

        Games.Leaderboards.submitScore(
            mGoogleApiClient,
            getString(R.string.leaderboard_high_scores),
            finalScore);
    }

    @android.webkit.JavascriptInterface
    public void openLeaderboards() {
        Log.d(TAG, "Opening leaderboards");
        startActivityForResult(Games.Leaderboards.getLeaderboardIntent(
                mGoogleApiClient,
                getString(R.string.leaderboard_high_scores)),
                RC_LEADERBOARD);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == RC_SIGN_IN) {
            mSignInClicked = false;
            mResolvingConnectionFailure = false;
            if (resultCode == RESULT_OK) {
                mGoogleApiClient.connect();
            } else {
                BaseGameUtils.showActivityResultError(this, requestCode, resultCode, R.string.signin_other_error);
            }
        }
        else if (requestCode == RC_LEADERBOARD &&
                resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) {
                logOut();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected(): connected to Google APIs");
        executeJavaScriptEvent(LOGGED_IN_EVENT);

        // Set the greeting appropriately on main menu
        Player p = Games.Players.getCurrentPlayer(mGoogleApiClient);
        String displayName;
        if (p == null) {
            Log.w(TAG, "mGamesClient.getCurrentPlayer() is NULL!");
            displayName = "???";
        } else {
            displayName = p.getDisplayName();
        }
        // TODO: Greet the signed in user
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended(): attempting to connect");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed(): attempting to resolve");
        if (mResolvingConnectionFailure) {
            Log.d(TAG, "onConnectionFailed(): already resolving");
            return;
        }

        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = true;
            if (!BaseGameUtils.resolveConnectionFailure(this, mGoogleApiClient, connectionResult,
                    RC_SIGN_IN, R.string.signin_other_error)) {
                mResolvingConnectionFailure = false;
            }
        }

        // Sign-in failed, so show sign-in button on main menu
        executeJavaScriptEvent(LOGGED_OUT_EVENT);
    }

    @android.webkit.JavascriptInterface
    public void logIn() {
        Log.d(TAG, "Starting the sign in flow");
        mSignInClicked = true;
        mGoogleApiClient.connect();
        executeJavaScriptEvent(LOGGED_IN_EVENT);
    }

    @android.webkit.JavascriptInterface
    public void logOut() {
        Log.d(TAG, "Starting the sign out flow");
        mSignInClicked = false;
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        executeJavaScriptEvent(LOGGED_OUT_EVENT);
    }
}
