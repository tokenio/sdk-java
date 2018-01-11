package io.token.browser;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TokenBrowserService extends Service {
    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_ON_URL = 3;
    static final int MSG_CLOSE = 4;
    static final int MSG_GO_TO = 5;
    static final String MSG_KEY_SID = "SID";
    static final String MSG_KEY_URL = "URL";

    private final Collection<Messenger> clients = new ConcurrentLinkedQueue<>();
    private final Messenger messenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    /**
     * Handler of incoming messages from clients.
     */
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            broadcast(msg);
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    clients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    clients.remove(msg.replyTo);
                    break;
                case MSG_ON_URL:
                    break;
                case MSG_GO_TO:
                    break;
                case MSG_CLOSE:
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void broadcast(Message msg) {
        Iterator<Messenger> iterator = clients.iterator();
        while(iterator.hasNext()) {
            try {
                iterator.next().send(msg);
            } catch (RemoteException e) {
                iterator.remove();
            }
        }
    }
}
