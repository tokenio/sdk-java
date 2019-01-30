package io.token.browser;

import static io.token.browser.TokenBrowserService.MSG_CLOSE;
import static io.token.browser.TokenBrowserService.MSG_COMPLETE;
import static io.token.browser.TokenBrowserService.MSG_GO_TO;
import static io.token.browser.TokenBrowserService.MSG_KEY_SID;
import static io.token.browser.TokenBrowserService.MSG_KEY_URL;
import static io.token.browser.TokenBrowserService.MSG_ON_URL;
import static io.token.browser.TokenBrowserService.MSG_REGISTER_CLIENT;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import io.token.user.browser.Browser;
import io.token.user.browser.BrowserFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TokenBrowserFactory implements BrowserFactory {
    private Context parent;
    private final Map<String, TokenBrowser> browsers;
    private final MessengerClient messenger;

    public TokenBrowserFactory(Context parent) {
        this.parent = parent;
        this.browsers = new HashMap<>();
        this.messenger = new MessengerClient(parent, new IncomingHandler(), null);
    }

    @Override
    public Browser create() {
        String sessionId = UUID.randomUUID().toString();

        TokenBrowser browser = new TokenBrowser(sessionId);
        browsers.put(sessionId, browser);

        Intent intent = new Intent(parent, TokenBrowserActivity.class);
        Bundle extras = new Bundle(1);
        extras.putString(MSG_KEY_SID, sessionId);
        intent.putExtras(extras);

        parent.startActivity(intent);

        return browser;
    }

    public void setParentContext(Context parent) {
        this.parent = parent;
    }

    private class TokenBrowser implements Browser {
        private final String sessionId;
        private final Subject<URL> subject;

        private boolean registered;
        private URL nextToGoTo;

        private TokenBrowser(String sessionId) {
            this.sessionId = sessionId;
            this.subject = PublishSubject.create();
        }

        @Override
        public void goTo(URL url) {
            if (!registered) {
                nextToGoTo = url;
                return;
            }
            Bundle data = new Bundle(2);
            data.putString(MSG_KEY_SID, sessionId);
            data.putString(MSG_KEY_URL, url.toExternalForm());
            messenger.send(MSG_GO_TO, data);
        }

        @Override
        public void close() {
            Bundle data = new Bundle(1);
            data.putString(MSG_KEY_SID, sessionId);
            messenger.send(MSG_CLOSE, data);
            subject.onComplete();
        }

        @Override
        public Observable<URL> url() {
            return new Observable<URL>() {
                @Override
                protected void subscribeActual(Observer<? super URL> observer) {
                    subject.subscribe(observer);
                }
            };
        }

        private void onRegistered() {
            this.registered = true;

            if (nextToGoTo != null) {
                goTo(nextToGoTo);
                nextToGoTo = null;
            }
        }

        private void onUrl(String url) {
            try {
                subject.onNext(new URL(url));
            } catch (MalformedURLException ex) {
                subject.onError(ex);
            }
        }
    }

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            String sessionId = data != null ? data.getString(MSG_KEY_SID) : null;
            TokenBrowser browser;

            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    browser = browsers.get(sessionId);
                    if (browser != null) {
                        browser.onRegistered();
                    }
                    break;
                case MSG_ON_URL:
                    String url = data.getString(MSG_KEY_URL);
                    browser = browsers.get(sessionId);
                    if (browser != null) {
                        browser.onUrl(url);
                    }
                    break;
                case MSG_COMPLETE:
                    browser = browsers.get(sessionId);
                    if (browser != null) {
                        browser.close();
                    }
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
