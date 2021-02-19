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

package io.token.tpp.tokenrequest;

import static io.token.tpp.util.Util.urlDecode;

import com.google.auto.value.AutoValue;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferDestination.DestinationCase;
import io.token.tpp.exceptions.InvalidTokenRequestQuery;
import io.token.tpp.util.Util;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AutoValue
public abstract class TokenRequestTransferDestinationsCallbackParameters {
    private static final String COUNTRY_FIELD = "country";
    private static final String BANK_NAME_FIELD = "bankName";
    private static final String SUPPORTED_TRANSFER_DESTINATION_TYPES_FIELD
            = "supportedTransferDestinationType";

    /**
     * Parses url parameters such as country, bank and state for the use case
     * to allow TPP to set transfer destinations for cross border payment.
     *
     * @param parameters url callback parameters
     * @return TokenRequestTransferDestinationsCallbackParameters instance
     */
    public static TokenRequestTransferDestinationsCallbackParameters create(
            Map<String, List<String>> parameters) {
        if (!parameters.containsKey(COUNTRY_FIELD)
                || !parameters.containsKey(BANK_NAME_FIELD)
                || !parameters.containsKey(SUPPORTED_TRANSFER_DESTINATION_TYPES_FIELD)) {
            throw new InvalidTokenRequestQuery();
        }

        List<DestinationCase> destinationCases = parameters
                .get(SUPPORTED_TRANSFER_DESTINATION_TYPES_FIELD)
                .stream()
                .map(Util::urlDecode)
                .map(DestinationCase::valueOf)
                .collect(Collectors.toList());

        return new AutoValue_TokenRequestTransferDestinationsCallbackParameters(
                urlDecode(parameters.get(COUNTRY_FIELD).get(0)),
                urlDecode(parameters.get(BANK_NAME_FIELD).get(0)),
                destinationCases);
    }

    public abstract String getCountry();

    public abstract String getBankName();

    public abstract List<DestinationCase> getSupportedTransferDestinationTypes();
}
