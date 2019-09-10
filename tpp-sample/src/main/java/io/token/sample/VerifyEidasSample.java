package io.token.sample;

import static io.token.proto.AliasHasher.normalize;
import static io.token.proto.common.alias.AliasProtos.Alias.Type.BANK;
import static io.token.proto.common.alias.AliasProtos.Alias.Type.EIDAS;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.eidas.EidasProtos.VerifyEidasPayload;
import io.token.proto.common.security.SecurityProtos.Key.Algorithm;
import io.token.proto.gateway.Gateway.VerifyEidasResponse;
import io.token.security.Signer;
import io.token.security.crypto.Crypto;
import io.token.security.crypto.CryptoRegistry;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;

import java.security.PrivateKey;

public class VerifyEidasSample {

    public static Member verifyEidas(
            TokenClient client,
            String tppAuthNumber,
            String certificate,
            String bankId,
            PrivateKey privateKey) {
        Algorithm signingAlgorithm = Algorithm.RS256;
        Crypto crypto = CryptoRegistry.getInstance().cryptoFor(signingAlgorithm);
        Signer signer = crypto.signer("eidas", privateKey);

        // resolve memberId of the bank TPP is trying to get access to
        String bankMemberId = client
                .resolveAliasBlocking(Alias.newBuilder().setValue(bankId).setType(BANK).build())
                .getId();
        // create an eIDAS alias under realm of the target bank
        Alias eidasAlias = normalize(Alias.newBuilder()
                .setValue(tppAuthNumber)
                .setRealmId(bankMemberId)
                .setType(EIDAS)
                .build());
        // create a member under realm of the bank with eIDAS alias
        Member tpp = client.createMember(eidasAlias, null, bankMemberId).blockingSingle();
        System.out.println("AN: aliases: " + tpp.aliasesBlocking());
        // construct a payload with all the required data
        VerifyEidasPayload payload = VerifyEidasPayload
                .newBuilder()
                .setAlgorithm(signingAlgorithm)
                .setAlias(eidasAlias)
                .setCertificate(certificate)
                .setMemberId(tpp.memberId())
                .build();
        // verify eIDAS
        VerifyEidasResponse response = tpp
                .verifyEidas(payload, signer.sign(payload))
                .blockingSingle();
        System.out.println("AN: response: " + response);
        return tpp;
    }
}
