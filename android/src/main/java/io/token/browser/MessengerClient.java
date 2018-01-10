package io.token.browser;

import static io.token.browser.TokenBrowserService.MSG_REGISTER_CLIENT;
import static io.token.browser.TokenBrowserService.MSG_UNREGISTER_CLIENT;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import io.reactivex.annotations.Nullable;

class MessengerClient {
    private final Context context;
    private final ServiceConnection connection;

    private Messenger service;
    private Messenger messenger;
    private boolean isBound;

    MessengerClient(
            Context context,
            Handler handler,
            @Nullable Bundle data) {
        this.context = context;
        this.connection = new NotifyingServiceConnection(handler, data);
        this.context.bindService(
                new Intent(context, TokenBrowserService.class),
                connection,
                Context.BIND_AUTO_CREATE);
    }

    public void send(int what, @Nullable Bundle data) {
        if (!isBound) return;
        Message msg = Message.obtain(null, what);
        if (data != null) {
            msg.setData(data);
        }
        msg.replyTo = messenger;
        try {
            service.send(msg);
        } catch (RemoteException ex) {
            // the service has crashed
            throw new RuntimeException(ex);
        }
    }

    public void stop() {
        if (isBound) {
            send(MSG_UNREGISTER_CLIENT, null);
            context.unbindService(connection);
        }
    }

    private class NotifyingServiceConnection implements ServiceConnection {
        private final Handler handler;
        private final Bundle data;

        public NotifyingServiceConnection(Handler handler, @Nullable Bundle data) {
            this.handler = handler;
            this.data = data;
        }

        @Override
        public void onServiceConnected(ComponentName className, IBinder serviceBinder) {
            service = new Messenger(serviceBinder);
            messenger = new Messenger(handler);
            isBound = true;
            send(MSG_REGISTER_CLIENT, data);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            service = null;
            messenger = null;
            isBound = false;
        }
    }
}
