/**
 * Copyright (c) 2021 Token, Inc.
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

package io.token.rpc;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

import io.grpc.Metadata;
import io.grpc.Status;
import io.token.rpc.interceptor.SimpleInterceptor;
import io.token.rpc.util.Tracing;

import javax.annotation.Nullable;

/**
 * Interceptor to fetch token-trace-id from grpc call metadata.
 *
 * @param <ReqT> Request message type
 * @param <ResT> Response message type
 */
public final class TracingInterceptor<ReqT, ResT> extends SimpleInterceptor<ReqT, ResT> {
    private static ThreadLocal<String> ttidThreadLocal = new ThreadLocal<>();
    private final Metadata.Key<String> traceIdMetadataKey = Metadata.Key.of(
            Tracing.TRACE_ID_KEY,
            ASCII_STRING_MARSHALLER);

    @Override
    public Status onComplete(
            Status status, ReqT req, @Nullable ResT res, @Nullable Metadata trailers) {
        if (trailers != null && trailers.containsKey(traceIdMetadataKey)) {
            setTraceId(trailers.get(traceIdMetadataKey));
        }
        return super.onComplete(status, req, res, trailers);
    }

    private static void setTraceId(String ttid) {
        ttidThreadLocal.set(ttid);
    }

    public static String getTraceId() {
        return ttidThreadLocal.get();
    }

    public static void resetTraceId() {
        setTraceId(null);
    }

}
