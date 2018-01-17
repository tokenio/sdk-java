package io.token.browser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import butterknife.BindView;
import butterknife.ButterKnife;

import static io.token.browser.TokenBrowserService.MSG_CLOSE;
import static io.token.browser.TokenBrowserService.MSG_GO_TO;
import static io.token.browser.TokenBrowserService.MSG_KEY_SID;
import static io.token.browser.TokenBrowserService.MSG_KEY_URL;
import static io.token.browser.TokenBrowserService.MSG_ON_URL;

public class TokenBrowserActivity extends Activity {
    private String sessionId;
    private MessengerClient messenger;
    private WebView webview;
    @BindView(R.id.webview) WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("<<<<< TokenBrowserActivity/onCreate");
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            this.sessionId = savedInstanceState.getString(MSG_KEY_SID);
        } else {
            this.sessionId = getIntent().getExtras().getString(MSG_KEY_SID);
        }

        setContentView(R.layout.activity_webview);
        ButterKnife.bind(this);

        webview = new WebView(this);

        WebSettings webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        webview.addJavascriptInterface(new JavascriptInterface(), "HTMLOUT");
        webView.setWebChromeClient(new WebChromeClient());



        webview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Bundle data = new Bundle(2);
                data.putString(MSG_KEY_SID, sessionId);
                data.putString(MSG_KEY_URL, url);
                messenger.send(MSG_ON_URL, data);
            }
        });
//        setContentView(webview);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Bundle data = new Bundle(1);
        data.putString(MSG_KEY_SID, sessionId);
        this.messenger = new MessengerClient(this, new IncomingHandler(), data);
    }

    @Override
    protected void onStop() {
        super.onStop();
        messenger.stop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(MSG_KEY_SID, sessionId);
        super.onSaveInstanceState(outState);
    }

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            String sid = data != null ? data.getString(MSG_KEY_SID) : null;

            switch (msg.what) {
                case MSG_CLOSE:
                    if (sessionId.equals(sid)) {
                        finish();
                    }
                    break;
                case MSG_GO_TO:
                    final String url = data.getString(MSG_KEY_URL);
                    if (sessionId.equals(sid)) {
                        webview.loadUrl(url);
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
