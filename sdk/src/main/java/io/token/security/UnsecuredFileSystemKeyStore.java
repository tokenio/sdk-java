/**
 * Copyright (c) 2017 Token, Inc.
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

package io.token.security;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Base58;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.Key.Level;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

/**
 * A key store that uses the local file system for persistent storage.
 * <br>
 * Keys are stored in a single root directory, with a subdirectory containing each member's keys.
 * No support is provided for security of key files.
 */
public final class UnsecuredFileSystemKeyStore implements KeyStore {
    private final File keyStoreRoot;
    private final KeyCodec codec;

    /**
     * Creates a new key store.
     *
     * @param keyStoreRoot the directory containing keys, or to add keys to.  Must exist.
     */
    public UnsecuredFileSystemKeyStore(File keyStoreRoot) {
        Preconditions.checkArgument(keyStoreRoot.isDirectory(), "keyStoreRoot must be a directory");
        Preconditions.checkArgument(keyStoreRoot.canWrite(), "keyStoreRoot must be writable");
        this.keyStoreRoot = keyStoreRoot;
        this.codec = new JsonKeyCodec();
    }

    @Override
    public void put(String memberId, SecretKey key) {
        File keyFile = getKeyFile(memberId, key.getId());
        File keyDir = keyFile.getParentFile();
        if (!keyDir.exists() && !keyFile.getParentFile().mkdirs()) {
            throw new KeyIOException("Failed to make parent directories for " + keyFile);
        }
        try {
            Files.write(codec.encode(key).getBytes(Charsets.UTF_8), keyFile);
        } catch (IOException e) {
            throw new KeyIOException("Failed to write key to " + keyFile, e);
        }
    }

    @Override
    public SecretKey getByLevel(String memberId, Level keyLevel) {
        for (SecretKey key : listKeys(memberId)) {
            if (key.getLevel() == keyLevel) {
                return key;
            }
        }
        throw new IllegalArgumentException("Key not found for level: " + keyLevel);
    }

    @Override
    public SecretKey getById(String memberId, String keyId) {
        return keyFromFile(getKeyFile(memberId, keyId));
    }

    private File getMemberPath(String memberId) {
        // Assumes memberId does not contain file separators.
        return new File(keyStoreRoot, memberId);
    }

    private File getKeyFile(String memberId, String keyId) {
        // Assumes keyId does not contain file separators.
        return new File(getMemberPath(memberId), keyId);
    }

    private SecretKey keyFromFile(File keyFile) {
        try {
            return codec.decode(keyFile.getName(), Files.toString(keyFile, Charsets.UTF_8));
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Key not found: " + keyFile);
        } catch (IOException e) {
            throw new KeyIOException("Failed to read key: " + keyFile, e);
        }
    }

    private List<SecretKey> listKeys(String memberId) {
        File memberPath = getMemberPath(memberId);
        File[] keyFiles = memberPath.listFiles();
        if (keyFiles == null) {
            throw new IllegalArgumentException("Key not found");
        }

        List<SecretKey> keys = Lists.newArrayList();
        for (File keyFile : keyFiles) {
            keys.add(keyFromFile(keyFile));
        }
        return keys;
    }

    private interface KeyCodec {
        String encode(SecretKey key);

        SecretKey decode(String keyId, String data);
    }

    /**
     * KeyCodec that stores key data in JSON.
     */
    private static final class JsonKeyCodec implements KeyCodec {
        private class JsonKey {
            private final Key.Level level;
            private final String privateKey;
            private final String privateKeyAlgorithm;
            private final String publicKey;
            private final String publicKeyAlgorithm;

            JsonKey(
                    Key.Level level,
                    byte[] privateKey,
                    String privateKeyAlgorithm,
                    byte[] publicKey,
                    String publicKeyAlgorithm) {

                this.level = level;
                this.privateKey = Base58.encode(privateKey);
                this.privateKeyAlgorithm = privateKeyAlgorithm;
                this.publicKey = Base58.encode(publicKey);
                this.publicKeyAlgorithm = publicKeyAlgorithm;
            }
        }

        private final Gson gson = new GsonBuilder().create();

        @Override
        public String encode(SecretKey key) {
            JsonKey jsonKey = new JsonKey(
                    key.getLevel(),
                    key.getPrivateKey().getEncoded(),
                    key.getPrivateKey().getAlgorithm(),
                    key.getPublicKey().getEncoded(),
                    key.getPublicKey().getAlgorithm());

            return gson.toJson(jsonKey);
        }

        @Override
        public SecretKey decode(String keyId, String data) {
            JsonKey jsonKey = gson.fromJson(data, JsonKey.class);

            try {
                KeyFactory privateFactory = KeyFactory.getInstance(jsonKey.privateKeyAlgorithm);
                EncodedKeySpec privateSpec =
                        new PKCS8EncodedKeySpec(base58Decode(jsonKey.privateKey));
                PrivateKey privateKey = privateFactory.generatePrivate(privateSpec);

                KeyFactory publicFactory = KeyFactory.getInstance(jsonKey.publicKeyAlgorithm);
                EncodedKeySpec publicSpec = new X509EncodedKeySpec(base58Decode(jsonKey.publicKey));
                PublicKey publicKey = publicFactory.generatePublic(publicSpec);

                return SecretKey.create(
                        keyId,
                        jsonKey.level,
                        new KeyPair(publicKey, privateKey));
            } catch (GeneralSecurityException e) {
                throw new KeyIOException("Unable to decode key: " + keyId, e);
            }
        }

        private byte[] base58Decode(String data) {
            try {
                return Base58.decode(data);
            } catch (AddressFormatException e) {
                throw new KeyIOException("Failed to decode key", e);
            }
        }
    }
}
