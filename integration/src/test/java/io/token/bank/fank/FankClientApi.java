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
    @PUT("/banks/{bic}/clients")
    Call<String> addClient(
            @Path("bic") String bic,
            @Body String request);

    @PUT("/banks/{bic}/clients/{client_id}/accounts")
    Call<String> addAccount(
            @Path("bic") String bic,
            @Path("client_id") String clientId,
            @Body String request);

    @PUT("/banks/{bic}/clients/{client_id}/link-accounts")
    Call<String> authorizeLinkAccounts(
            @Path("bic") String bic,
            @Path("client_id") String clientId,
            @Body String request);
}
