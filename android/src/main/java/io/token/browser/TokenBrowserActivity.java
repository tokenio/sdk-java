package io.token.browser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.Toolbar;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;

import static io.token.browser.TokenBrowserService.MSG_CLOSE;
import static io.token.browser.TokenBrowserService.MSG_GO_TO;
import static io.token.browser.TokenBrowserService.MSG_KEY_SID;
import static io.token.browser.TokenBrowserService.MSG_KEY_URL;
import static io.token.browser.TokenBrowserService.MSG_ON_URL;

public class TokenBrowserActivity extends Activity {
    public static final String CONTENT_URL_PARAM = "content_webview";
    private static final String START_URL_PARAM = "start_url";
    private static final String TITLE_PARAM = "title";
    private static final String REGEX_PARAM = "regex";

    private String sessionId;
    private MessengerClient messenger;
//    private WebView webview;
    @BindView(R.id.webview_toolbar) Toolbar toolbar;
    @BindView(R.id.toolbar_title) TextView toolbarTitle;
    @BindView(R.id.webview) WebView webView;

    /**
     * Creates a new {@link Intent} for {@link TokenBrowserActivity}.
     */
    public static Intent newIntent(Context context, String title, String linkingUrl, String regex) {
        return new Intent(context, TokenBrowserActivity.class)
                .putExtra(TITLE_PARAM, title)
                .putExtra(START_URL_PARAM, linkingUrl)
                .putExtra(REGEX_PARAM, regex);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        ButterKnife.bind(this);
//        setSupportActionBar(toolbar);
//        getSupportActionBar().setDisplayShowTitleEnabled(false);

        String linkingUrl = getIntent().getStringExtra(START_URL_PARAM);
        String title = getIntent().getStringExtra(TITLE_PARAM);
        final String regex = getIntent().getStringExtra(REGEX_PARAM);

        toolbarTitle.setText(title);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.matches(regex)) {
                    onDone(url);
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }
        });
        webView.loadUrl(linkingUrl);

//        System.out.println("<<<<< TokenBrowserActivity/onCreate");
//        super.onCreate(savedInstanceState);
////        if (savedInstanceState != null) {
////            this.sessionId = savedInstanceState.getString(MSG_KEY_SID);
////        } else {
////            this.sessionId = getIntent().getExtras().getString(MSG_KEY_SID);
////        }
//
//        ButterKnife.bind(this);
//        setContentView(R.layout.activity_webview);
//
////        webview = new WebView(this);
//
//        WebSettings webSettings = webView.getSettings();
//        webSettings.setJavaScriptEnabled(true);
////        webSettings.setDomStorageEnabled(true);
//
////        webview.addJavascriptInterface(new JavascriptInterface(), "HTMLOUT");
////        webview.setWebChromeClient(new WebChromeClient());
//
//        webView.setWebViewClient(new WebViewClient() {
//            @Override
//            public boolean shouldOverrideUrlLoading(WebView view, String url) {
//                return super.shouldOverrideUrlLoading(view, url);
//            }
//
////            @Override
////            public void onPageStarted(WebView view, String url, Bitmap favicon) {
////                Bundle data = new Bundle(2);
////                data.putString(MSG_KEY_SID, sessionId);
////                data.putString(MSG_KEY_URL, url);
////                messenger.send(MSG_ON_URL, data);
////            }
//        });
////        setContentView(webview);
////        webview.loadUrl("https://bank-demo.sandbox.token.io/auth/login?username=tien");
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
//                        WebSettings webSettings = webView.getSettings();
//                        webSettings.setJavaScriptEnabled(true);
//                        webView.loadUrl(url);
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void onDone(String url) {
        Intent intent = new Intent();
        intent.putExtra(CONTENT_URL_PARAM, url);
        setResult(RESULT_OK, intent);
        finish();
    }
}
