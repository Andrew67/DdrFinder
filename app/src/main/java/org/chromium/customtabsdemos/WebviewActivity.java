// Copyright 2015 Google Inc. All Rights Reserved.
// Original: https://github.com/GoogleChrome/android-browser-helper/blob/e164642febefc30444555b8cbb783105591437f8/demos/custom-tabs-example-app/src/main/java/org/chromium/customtabsdemos/WebviewActivity.java
// Customizations: Copyright (c) 2023 Andrés Cordero
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.chromium.customtabsdemos;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.andrew67.ddrfinder.R;

import java.net.URISyntaxException;

/**
 * This Activity is used as a fallback when there is no browser installed that supports
 * Chrome Custom Tabs
 */
public class WebviewActivity extends AppCompatActivity {
    public static final String EXTRA_URL = "extra.url";
    private ActionBar actionBar;
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        final String url = getIntent().getStringExtra(EXTRA_URL);
        webView = findViewById(R.id.webview);
        webView.setWebViewClient(new CustomWebviewClient());
        final WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportMultipleWindows(false);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setTitle(url);
        actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);
        webView.loadUrl(url);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.webview, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_share) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, webView.getUrl());
            shareIntent.setType("text/plain");
            startActivity(Intent.createChooser(shareIntent, getText(R.string.action_share)));
            return true;
        } else if (itemId == R.id.action_reload) {
            webView.reload();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class CustomWebviewClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onLoadResource(view, url);
            if (actionBar != null) {
                final Uri currentUri = Uri.parse(url);
                final String lockIconIfHttps = currentUri.getScheme().equals("https") ? "🔒 " : "";
                actionBar.setSubtitle(lockIconIfHttps + currentUri.getHost());
                actionBar.setTitle(view.getTitle());
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            // TODO: Links within domains we control should stay within the webview
            final Uri requestUri = request.getUrl();
            Intent intent = new Intent(Intent.ACTION_VIEW, requestUri);
            if (requestUri.getScheme().equals("intent")) {
                try {
                    intent = Intent.parseUri(requestUri.toString(), Intent.URI_INTENT_SCHEME);
                } catch (URISyntaxException e) {
                    // TODO: Error for malformed intent: URIs
                }
            }
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                // TODO: Error for links for which an activity cannot be found
            }
            return true;
        }
    }
}
