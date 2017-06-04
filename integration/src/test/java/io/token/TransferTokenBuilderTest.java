package io.token;


import static io.token.testing.sample.Sample.string;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.protobuf.ByteString;
import com.typesafe.config.ConfigException;
import io.token.common.LinkedAccount;
import io.token.common.TokenRule;
import io.token.proto.common.blob.BlobProtos.Attachment;
import io.token.proto.common.blob.BlobProtos.Blob;
import io.token.proto.common.security.SecurityProtos;
import io.token.proto.common.token.TokenProtos.Token;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TransferTokenBuilderTest {
    @Rule public TokenRule rule = new TokenRule();

    private LinkedAccount payerAccount;
    private Member payer;
    private Member payee;

    @Before
    public void before() {
        this.payerAccount = rule.linkedAccount();
        this.payer = payerAccount.getMember();

        LinkedAccount payeeAccount = rule.linkedAccount();
        this.payee = payeeAccount.getMember();
    }

    @Test
    public void basicToken() {
        payerAccount.createTransferToken(100.0)
                .setRedeemerUsername(payee.firstUsername())
                .setDescription("book purchase")
                .execute();
    }

    @Test
    public void noSource() {
        assertThatThrownBy(() -> payer.createTransferToken(100.0, payerAccount.getCurrency())
                .setRedeemerUsername(payee.firstUsername())
                .setDescription("book purchase")
                .execute());
    }

    @Test
    public void noRedeemer() {
        assertThatThrownBy(() -> payerAccount.createTransferToken(100.0)
                .setDescription("book purchase")
                .execute());
    }

    @Test
    public void blobs() {
        byte[] randomData1 = new byte[100];
        new Random().nextBytes(randomData1);

        Attachment attachment = payer.createBlob(payer.memberId(), string(), string(), randomData1);

        Token token = payerAccount.createTransferToken(100.0)
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
    public void blobs_direct() throws IOException {
        byte[] randomData1 = new byte[100];
        byte[] randomData2 = new byte[300];
        byte[] randomData3 = new byte[500];
        byte[] randomData4 = new byte[100];

        new Random().nextBytes(randomData1);
        new Random().nextBytes(randomData2);
        new Random().nextBytes(randomData3);
        new Random().nextBytes(randomData4);

        Token token = payerAccount.createTransferToken(100.0)
                .setRedeemerUsername(payee.firstUsername())
                .setDescription("book purchase")
                .addAttachment(payer.memberId(), string(), string(), randomData1)
                .addAttachment(payer.memberId(), string(), string(), randomData2)
                .addAttachment(payer.memberId(), string(), string(), randomData3)
                .addAttachment(payer.memberId(), string(), string(), randomData4)
                .execute();

        payer.endorseToken(token, SecurityProtos.Key.Level.STANDARD);

        Blob blob = payer.getTokenBlob(
                token.getId(),
                token.getPayload().getTransfer().getAttachments(3).getBlobId());
        boolean eq1 = blob.getPayload().getData().equals(ByteString.copyFrom(randomData1));
        boolean eq2 = blob.getPayload().getData().equals(ByteString.copyFrom(randomData2));
        boolean eq3 = blob.getPayload().getData().equals(ByteString.copyFrom(randomData3));
        boolean eq4 = blob.getPayload().getData().equals(ByteString.copyFrom(randomData4));
        assertThat(eq1 || eq2 || eq3 || eq4);
    }

    @Test
    public void full() {
        payerAccount.createTransferToken(100.0)
                .setRedeemerUsername(payee.firstUsername())
                .setEffectiveAtMs(System.currentTimeMillis() + 10000)
                .setExpiresAtMs(System.currentTimeMillis() + 1000000)
                .setChargeAmount(40)
                .setDescription("book purchase")
                .execute();
    }
}
