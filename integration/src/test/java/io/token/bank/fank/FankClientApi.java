/**
 * Copyright (C) 2017 Token, Inc.
 * <p>
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package io.token.bank.fank;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Wraps around HTTP REST API for the fake bank client.
 */
interface FankClientApi {
    @PUT("/clients")
    Call<String> addClient(@Body String request);

    @PUT("/clients/{client_id}/accounts")
    Call<String> addAccount(@Body String request, @Path("client_id") String clientId);

    @PUT("/clients/{client_id}/link-accounts")
    Call<String> authorizeLinkAccounts(@Body String request, @Path("client_id") String clientId);
}
