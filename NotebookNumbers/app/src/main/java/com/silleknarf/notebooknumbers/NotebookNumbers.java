package com.silleknarf.notebooknumbers;

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class NotebookNumbers extends AppCompatActivity {

    WebSettings wSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_notebook_numbers);

        WebView webView = (WebView) findViewById(R.id.webView);
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

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
}
