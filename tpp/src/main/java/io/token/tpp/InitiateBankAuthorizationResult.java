/**
 * Copyright (c) 2020 Token, Inc.
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

package io.token.tpp;

import com.google.auto.value.AutoValue;
import io.token.proto.common.providerspecific.ProviderSpecific.CredentialField;
import io.token.proto.common.providerspecific.ProviderSpecific.ScaStatus;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@AutoValue
public abstract class InitiateBankAuthorizationResult {
    /**
     * Create an instance of InitiateBankAuthorizationResult.
     *
     * @param redirectUrl redirect url
     * @param status SCA status
     * @param credentialFields additionally requested credentials
     * @return initiation result
     */
    public static InitiateBankAuthorizationResult create(
            Optional<String> redirectUrl,
            Optional<ScaStatus> status,
            List<CredentialField> credentialFields) {
        return new AutoValue_InitiateBankAuthorizationResult(redirectUrl, status, credentialFields);
    }

    public static InitiateBankAuthorizationResult create(String redirectUrl) {
        return create(Optional.of(redirectUrl), Optional.empty(), Collections.emptyList());
    }

    public static InitiateBankAuthorizationResult create(ScaStatus status) {
        return create(Optional.empty(), Optional.of(status), Collections.emptyList());
    }

    public static InitiateBankAuthorizationResult create(List<CredentialField> credentialFields) {
        return create(Optional.empty(), Optional.empty(), credentialFields);
    }

    public abstract Optional<String> getRedirectUrl();

    public abstract Optional<ScaStatus> getStatus();

    public abstract List<CredentialField> getCredentialFields();
}

