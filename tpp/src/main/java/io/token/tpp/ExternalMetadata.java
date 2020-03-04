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

import io.token.proto.common.bank.BankProtos.OpenBankingStandard;

import java.util.Optional;
import javax.annotation.Nullable;

public class ExternalMetadata {
    private final OpenBankingStandard openBankingStandard;
    private final Optional<String> consentId;
    private final Optional<String> consent;

    /**
     * Instantiates a new external metadata instance.
     *
     * @param openBankingStandard the open banking standard
     * @param consentId the consent id
     * @param consent the consent
     */
    ExternalMetadata(
            OpenBankingStandard openBankingStandard,
            @Nullable String consentId,
            @Nullable String consent) {
        this.openBankingStandard = openBankingStandard;
        this.consentId = Optional.ofNullable(consentId);
        this.consent = Optional.ofNullable(consent);
    }

    public OpenBankingStandard getOpenBankingStandard() {
        return openBankingStandard;
    }

    public Optional<String> getConsentId() {
        return consentId;
    }

    public Optional<String> getConsent() {
        return consent;
    }
}