/**
 * Copyright (c) 2017 Token, Inc.
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
import static io.token.rpc.Constants.TOKEN_CUSTOM_ERROR_HEADER_NAME;
import static io.token.rpc.Constants.TOKEN_ERROR_DETAILS_HEADER_NAME;
import static io.token.rpc.Constants.TOKEN_HTTP_HEADER_ENCODING;
import static java.lang.String.format;

import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.Status;
import io.token.exceptions.VersionMismatchException;
import io.token.rpc.interceptor.SimpleInterceptor;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import javax.annotation.Nullable;

/**
 * gRPC interceptor that facilitates handling cross-cutting API errors. Converts generic
 * StatusRuntimeException instances into specific Exception types to be handled by the callers.
 */
final class ErrorHandler<ReqT, ResT> extends SimpleInterceptor<ReqT, ResT> {
    private static final String ERROR_UNSUPPORTED_CLIENT_VERSION = "unsupported-client-version";

    @Override
    public void onStart(ReqT req, Metadata headers) {
        // Ignore
    }

    @Override
    public void onHalfClose(ReqT req, Metadata headers) {

    }

    @Override
    public Status onComplete(
            Status status,
            ReqT req,
            @Nullable ResT res,
            @Nullable Metadata trailers) {
        if (!status.isOk() && trailers != null) {
            Key<String> errorDetailsKey = Key.of(
                    TOKEN_ERROR_DETAILS_HEADER_NAME,
                    ASCII_STRING_MARSHALLER);
            Key<String> customErrorKey = Key.of(
                    TOKEN_CUSTOM_ERROR_HEADER_NAME,
                    ASCII_STRING_MARSHALLER);

            String errorDetails = trailers.get(errorDetailsKey);
            if (errorDetails != null) {
                try {
                    errorDetails = URLDecoder.decode(errorDetails, TOKEN_HTTP_HEADER_ENCODING);
                } catch (UnsupportedEncodingException ex) {
                    ex.printStackTrace();
                    errorDetails = null;
                }
            }
            String description = formatMessage(status.getDescription(), errorDetails);
            String customError = trailers.get(customErrorKey);
            if (customError != null) {
                if (ERROR_UNSUPPORTED_CLIENT_VERSION.equals(customError)) {
                    RuntimeException exception = new VersionMismatchException(description);
                    return status
                            .withCause(exception)
                            .withDescription(description);
                }
            }

            return status.withDescription(description);
        }

        return status;
    }

    private static String formatMessage(String message, @Nullable String details) {
        if (details != null) {
            return format(
                    "%s \nToken error details: \n%s",
                    message,
                    details.replaceAll("; ", "\n"));
        }
        return message;
    }
}
