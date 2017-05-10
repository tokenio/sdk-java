package io.token;

import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import io.token.common.TokenRule;
import io.token.proto.ProtoHasher;
import io.token.proto.common.blob.BlobProtos.Attachment;
import io.token.proto.common.blob.BlobProtos.Blob;
import io.token.proto.common.blob.BlobProtos.Blob.Payload;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.util.codec.ByteEncoding;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.assertj.core.api.ThrowableAssert;
import org.junit.Rule;
import org.junit.Test;

public class BlobsTest {
    @Rule public TokenRule rule = new TokenRule();
    private final Account payerAccount = rule.account();
    private final Member payer = payerAccount.member();
    private final Member payee = rule.member();
    private final Member otherMember = rule.member();
    private static String FILETYPE = "application/json";
    private static String FILENAME = "file.json";

    @Test
    public void checkHash() {
        byte[] randomData = new byte[100];
    new Random().nextBytes(randomData);

        Attachment attachment = payer
                .createBlob(payer.memberId(), FILETYPE, FILENAME, randomData);

        Payload blobPayload = Payload.newBuilder()
                .setData(ByteString.copyFrom(randomData))
                .setName(FILENAME)
                .setType(FILETYPE)
                .setOwnerId(payer.memberId())
                .build();
        String hash = ByteEncoding.serializeHumanReadable(ProtoHasher.hash(blobPayload));
        assertThat(attachment.getBlobId()).contains(hash);
    }

    @Test
    public void create() {
        byte[] randomData = new byte[100];
        new Random().nextBytes(randomData);

        Attachment attachment = payer
                .createBlob(payer.memberId(), FILETYPE, FILENAME, randomData);
        assertThat(attachment.getName()).isEqualTo(FILENAME);
        assertThat(attachment.getType()).isEqualTo(FILETYPE);
        assertThat(attachment.getBlobId().length()).isGreaterThan(5);
    }

    @Test
    public void createIdempotent() {
        byte[] randomData = new byte[100];
        new Random().nextBytes(randomData);

        Attachment attachment = payer.createBlob(
                payer.memberId(), FILETYPE, FILENAME, randomData);

        Attachment attachment2 = payer
                .createBlob(payer.memberId(), FILETYPE, FILENAME, randomData);
        assertThat(attachment).isEqualTo(attachment2);
    }

    @Test
    public void get() {
        byte[] randomData = new byte[100];
        new Random().nextBytes(randomData);

        Attachment attachment = payer
                .createBlob(payer.memberId(), FILETYPE, FILENAME, randomData);

        Blob blob = payer.getBlob(attachment.getBlobId());

        assertThat(blob.getId()).isEqualTo(attachment.getBlobId());
        assertThat(blob.getPayload().getData().toByteArray()).isEqualTo(randomData);
        assertThat(blob.getPayload().getOwnerId()).isEqualTo(payer.memberId());
    }

    @Test
    public void empty() {
        byte[] randomData = new byte[0];

        Attachment attachment = payer
                .createBlob(payer.memberId(), FILETYPE, FILENAME, randomData);

        Blob blob = payer.getBlob(attachment.getBlobId());

        assertThat(blob.getId()).isEqualTo(attachment.getBlobId());
        assertThat(blob.getPayload().getData().toByteArray()).isEqualTo(randomData);
        assertThat(blob.getPayload().getOwnerId()).isEqualTo(payer.memberId());
    }

    @Test
    public void mediumSize() {
        byte[] randomData = new byte[50000];
        new Random().nextBytes(randomData);

        Attachment attachment = payer
                .createBlob(payer.memberId(), FILETYPE, FILENAME, randomData);

        Blob blob = payer.getBlob(attachment.getBlobId());

        assertThat(blob.getId()).isEqualTo(attachment.getBlobId());
        assertThat(blob.getPayload().getData().toByteArray()).isEqualTo(randomData);
        assertThat(blob.getPayload().getOwnerId()).isEqualTo(payer.memberId());
    }

    @Test
    public void filename() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("local.conf").getFile());

        String filename = file.getAbsolutePath();

        Attachment attachment = payer.createBlob(filename);

        assertThat(attachment.getBlobId().length()).isGreaterThan(5);
        assertThat(attachment.getName()).isEqualTo("local.conf");
        assertThat(attachment.getType()).isEqualTo("content/unknown");
        assertThat(attachment.getBlobId().length()).isGreaterThan(5);

        Blob blob = payer.getBlob(attachment.getBlobId());

        assertThat(blob.getId()).isEqualTo(attachment.getBlobId());
        assertThat(blob.getPayload().getOwnerId()).isEqualTo(payer.memberId());
    }

    @Test
    public void tokenBlob() {
        byte[] randomData = new byte[50];
        byte[] randomData2 = new byte[150];

        new Random().nextBytes(randomData);
        new Random().nextBytes(randomData2);

        Attachment attachment = payer
                .createBlob(payer.memberId(), FILETYPE, FILENAME, randomData);

        Attachment attachment2 = payer
                .createBlob(payer.memberId(), FILETYPE, FILENAME, randomData);

        Token token = payer.createTransferToken(100, "EUR")
                .setAccountId(payerAccount.id())
                .setRedeemerUsername(payee.firstUsername())
                .addAttachment(attachment)
                .addAttachment(attachment2)
                .execute();
        payer.endorseToken(token, PRIVILEGED);

        Blob blob = payer.getBlob(attachment.getBlobId());
        Blob blob2 = payer.getTokenBlob(token.getId(), attachment.getBlobId());
        Blob blob3 = payee.getTokenBlob(token.getId(), attachment.getBlobId());

        Blob blob4 = payer.getBlob(attachment2.getBlobId());
        Blob blob5 = payer.getTokenBlob(token.getId(), attachment2.getBlobId());
        Blob blob6 = payee.getTokenBlob(token.getId(), attachment2.getBlobId());

        assertThat(blob).isEqualTo(blob2).isEqualTo(blob3);
        assertThat(blob4).isEqualTo(blob5).isEqualTo(blob6);
    }

    @Test
    public void noAccess() {
        byte[] randomData = new byte[50];

        new Random().nextBytes(randomData);

        final Attachment attachment = payer
                .createBlob(payer.memberId(), FILETYPE, FILENAME, randomData);

        final Token token = payer.createTransferToken(100, "EUR")
                .setAccountId(payerAccount.id())
                .setRedeemerUsername(payee.firstUsername())
                .addAttachment(attachment)
                .execute();
        payer.endorseToken(token, PRIVILEGED);
        assertThatThrownBy(
                new ThrowableAssert.ThrowingCallable() {
                    public void call() throws Throwable {
                        otherMember.getBlob(attachment.getBlobId());
                    }
                })
                .isInstanceOf(StatusRuntimeException.class);

        assertThatThrownBy(
                new ThrowableAssert.ThrowingCallable() {
                    public void call() throws Throwable {
                        otherMember.getTokenBlob(token.getId(), attachment.getBlobId());
                    }
                })
                .isInstanceOf(StatusRuntimeException.class);
    }
}
