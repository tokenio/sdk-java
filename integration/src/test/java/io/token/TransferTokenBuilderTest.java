package io.token;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.token.common.TokenRule;
import io.token.proto.common.blob.BlobProtos.Attachment;
import io.token.proto.common.blob.BlobProtos.Blob;
import io.token.proto.common.security.SecurityProtos;
import io.token.proto.common.token.TokenProtos.Token;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TransferTokenBuilderTest {
    @Rule public TokenRule rule = new TokenRule();

    private Account payerAccount;
    private Member payer;
    private Member payee;

    @Before
    public void before() {
        this.payerAccount = rule.account();
        this.payer = payerAccount.member();

        Account payeeAccount = rule.account();
        this.payee = payeeAccount.member();
    }

    @Test
    public void basicToken() {
        payer.createTransferToken(100.0, "USD")
                .setAccountId(payerAccount.id())
                .setRedeemerUsername(payee.firstUsername())
                .setDescription("book purchase")
                .execute();
    }

    @Test
    public void noSource() {
        assertThatThrownBy(() -> payer.createTransferToken(100.0, "USD")
                .setRedeemerUsername(payee.firstUsername())
                .setDescription("book purchase")
                .execute());
    }

    @Test
    public void noRedeemer() {
        assertThatThrownBy(() -> payer.createTransferToken(100.0, "USD")
                .setAccountId(payerAccount.id())
                .setDescription("book purchase")
                .execute());
    }

    @Test
    public void blobs() {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("local.conf").getFile());
        Attachment attachment = null;
        String filename = file.getAbsolutePath();

        try {
            attachment = payer.createBlob(filename);
        } catch (IOException exception) {
            // Fail test
            assert false;
        }

        Token token = payer.createTransferToken(100.0, "USD")
                .setAccountId(payerAccount.id())
                .setRedeemerUsername(payee.firstUsername())
                .setDescription("book purchase")
                .addAttachment(attachment)
                .execute();

        payer.endorseToken(token, SecurityProtos.Key.Level.STANDARD);

        Blob blob = payer.getTokenBlob(token.getId(), attachment.getBlobId());
        assertThat(blob.getId()).isEqualTo(attachment.getBlobId());
        assertThat(blob.getPayload().getName()).isEqualTo(attachment.getName());
    }

    @Test
    public void full() {
        payer.createTransferToken(100.0, "USD")
                .setAccountId(payerAccount.id())
                .setRedeemerUsername(payee.firstUsername())
                .setEffectiveAtMs(System.currentTimeMillis() + 10000)
                .setExpiresAtMs(System.currentTimeMillis() + 1000000)
                .setChargeAmount(40)
                .setDescription("book purchase")
                .execute();
    }
}
