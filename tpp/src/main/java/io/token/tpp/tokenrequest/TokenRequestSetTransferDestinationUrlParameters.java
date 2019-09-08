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

import static io.token.tpp.util.Util.urlDecode;

import com.google.auto.value.AutoValue;
import io.token.tpp.exceptions.InvalidTokenRequestQuery;

import java.util.Map;

@AutoValue
public abstract class TokenRequestSetTransferDestinationUrlParameters {
    private static final String COUNTRY_FIELD = "country";
    private static final String BANK_FIELD = "bank";

    /**
     *  Parses url parameters such as country, bank and state for the use case
     *  to allow TPP to set transfer destinations for cross border payment.
     *
     * @param parameters url callback parameters
     * @return TokenRequestSetTransferDestinationUrlParameters instance
     */
    public static TokenRequestSetTransferDestinationUrlParameters create(
            Map<String, String> parameters) {
        if (!parameters.containsKey(COUNTRY_FIELD)
                || !parameters.containsKey(BANK_FIELD)) {
            throw new InvalidTokenRequestQuery();
        }

        return new AutoValue_TokenRequestSetTransferDestinationUrlParameters(
                urlDecode(parameters.get(COUNTRY_FIELD)),
                urlDecode(parameters.get(BANK_FIELD)));
    }

    public abstract String getCountry();

    public abstract String getBank();
}
