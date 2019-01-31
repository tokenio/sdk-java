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

package io.token.partner;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.token.Account;
import io.token.TokenClient.TokenCluster;
import io.token.partner.rpc.Client;
import io.token.proto.common.member.MemberProtos;

/**
 * Represents a Member in the Token system. Each member has an active secret
 * and public key pair that is used to perform authentication.
 */
public class Member extends io.token.Member {
    private final Client client;

    /**
     * Creates an instance of {@link Member}.
     *
     * @param member internal member representation, fetched from server
     * @param client RPC client used to perform operations against the server
     * @param cluster Token cluster, e.g. sandbox, production
     */
    Member(
            MemberProtos.Member member,
            Client client,
            TokenCluster cluster) {
        super(member, client, cluster);
        this.client = client;
    }

    Member(io.token.Member member, Client client) {
        super(member);
        this.client = client;
    }

    /**
     * Verifies an affiliated TPP.
     *
     * @param memberId member ID of the TPP to verify
     * @return completable
     */
    public Completable verifyAffiliate(String memberId) {
        return client.verifyAffiliate(memberId);
    }
}
