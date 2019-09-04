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

import com.google.auto.value.AutoValue;

/**
 * Represents a url in Token Flow that makes TPPs aware if user selection of country and bank.
 */
@AutoValue
public abstract class TokenRequestSetTransferDestinationUrl {

    /**
     * Creates a TokenRequestSetTransferDestinationUrl instance.
     *
     * @param country country selected
     * @param bank bank chosen
     * @return TokenRequestSetTransferDestinationUrl instance
     */
    public static TokenRequestSetTransferDestinationUrl create(
            String country,
            String bank) {
        return new AutoValue_TokenRequestSetTransferDestinationUrl(
                country,
                bank);
    }

    /**
     * Get the Country code selected by the user.
     *
     * @return country
     */
    public abstract String getCountry();

    /**
     * Get the bank selected by the user.
     *
     * @return bank
     */
    public abstract String getBank();
}
