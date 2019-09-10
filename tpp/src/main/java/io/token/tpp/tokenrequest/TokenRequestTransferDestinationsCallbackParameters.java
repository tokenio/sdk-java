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

package io.token.tpp.tokenrequest;

import static io.token.tpp.util.Util.parseQueryListTypeParam;
import static io.token.tpp.util.Util.urlDecode;

import com.google.auto.value.AutoValue;
import io.token.tpp.exceptions.InvalidTokenRequestQuery;

import java.util.List;
import java.util.Map;

@AutoValue
public abstract class TokenRequestTransferDestinationsCallbackParameters {
    private static final String COUNTRY_FIELD = "country";
    private static final String BANK_NAME_FIELD = "bankName";
    private static final String SUPPORTED_PAYMENT_TYPES_FIELD = "supportedPaymentTypes";

    /**
     *  Parses url parameters such as country, bank and state for the use case
     *  to allow TPP to set transfer destinations for cross border payment.
     *
     * @param parameters url callback parameters
     * @return TokenRequestTransferDestinationsCallbackParameters instance
     */
    public static TokenRequestTransferDestinationsCallbackParameters create(
            Map<String, String> parameters) {
        if (!parameters.containsKey(COUNTRY_FIELD)
                || !parameters.containsKey(BANK_NAME_FIELD)) {
            throw new InvalidTokenRequestQuery();
        }

        List<String> supportedPaymentTypes = parseQueryListTypeParam(
                parameters.get(SUPPORTED_PAYMENT_TYPES_FIELD));

        return new AutoValue_TokenRequestTransferDestinationsCallbackParameters(
                urlDecode(parameters.get(COUNTRY_FIELD)),
                urlDecode(parameters.get(BANK_NAME_FIELD)),
                supportedPaymentTypes);
    }

    public abstract String getCountry();

    public abstract String getBankName();

    public abstract List<String> getSupportedPaymentTypes();
}
