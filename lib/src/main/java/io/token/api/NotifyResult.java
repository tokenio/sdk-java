/**
 * Copyright (c) 2018 Token, Inc.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.token.api;

import com.google.auto.value.AutoValue;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;

@AutoValue
public abstract class NotifyResult {
    /**
     * Create token request.
     *
     * @param notificationId notification id
     * @param notifyStatus notify status
     * @return notify result
     */
    public static NotifyResult create(String notificationId, NotifyStatus notifyStatus) {
        return new AutoValue_NotifyResult(notificationId, notifyStatus);
    }

    public abstract String getNotificationId();

    public abstract NotifyStatus getNotifyStatus();
}
