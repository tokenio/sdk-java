/**
 * Copyright (c) 2019 Token, Inc.
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
import io.token.rpc.interceptor.SimpleInterceptor;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class FeatureCodeInterceptor<ReqT, ResT> extends SimpleInterceptor<ReqT, ResT> {
    private static final String FEATURE_CODE_KEY = "feature-codes";
    private final List<String> featureCodes;

    FeatureCodeInterceptor(List<String> featureCodes) {
        this.featureCodes = featureCodes;
    }

    @Override
    public void onStart(ReqT reqT, Metadata metadata) {
        if (featureCodes.size() > 0) {
            String value = StringUtils.join(featureCodes, '|');
            metadata.put(Metadata.Key.of(FEATURE_CODE_KEY, ASCII_STRING_MARSHALLER), value);
        }
    }
}
