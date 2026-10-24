package com.votesystem.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class CryptoService {

    private static final Logger log = LoggerFactory.getLogger(CryptoService.class);
    private static final String SECRET_KEY = "ONLINE_VOTING_AES_SECRET_2026"; // 30 bytes, padded to 32 bytes
    private static final String AES_ALGO = "AES/CBC/PKCS5Padding";
    private static final String RSA_ALGO = "SHA256withRSA";

    private KeyPair rsaKeyPair;

    public CryptoService() {
        initRsaKeys();
    }

    private void initRsaKeys() {
        try {
            File privFile = findKeyFile("keys/private.pem");
            File pubFile = findKeyFile("keys/public.pem");

            if (privFile != null && pubFile != null && privFile.exists() && pubFile.exists()) {
                PrivateKey privateKey = loadPrivateKey(privFile);
                PublicKey publicKey = loadPublicKey(pubFile);
                this.rsaKeyPair = new KeyPair(publicKey, privateKey);
                log.info("RSA Key Pair loaded successfully from filesystem.");
            } else {
                log.warn("RSA key files not found in keys/ directory. Generating fresh 2048-bit RSA KeyPair...");
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                keyGen.initialize(2048);
                this.rsaKeyPair = keyGen.generateKeyPair();
                saveGeneratedKeys();
            }
        } catch (Exception e) {
            log.error("Failed to initialize RSA keys, generating memory fallback: {}", e.getMessage());
            try {
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                keyGen.initialize(2048);
                this.rsaKeyPair = keyGen.generateKeyPair();
            } catch (Exception ex) {
                throw new RuntimeException("Could not initialize RSA generator", ex);
            }
        }
    }

    private File findKeyFile(String relativePath) {
        File f = new File(relativePath);
        if (f.exists()) return f;
        f = new File("../" + relativePath);
        if (f.exists()) return f;
        return null;
    }

    private void saveGeneratedKeys() {
        try {
            File dir = new File("keys");
            if (!dir.exists()) dir.mkdirs();

            File privFile = new File(dir, "private.pem");
            File pubFile = new File(dir, "public.pem");

            if (!privFile.exists()) {
                String privPem = "-----BEGIN PRIVATE KEY-----\n" +
                        Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(rsaKeyPair.getPrivate().getEncoded()) +
                        "\n-----END PRIVATE KEY-----\n";
                try (FileOutputStream fos = new FileOutputStream(privFile)) {
                    fos.write(privPem.getBytes(StandardCharsets.UTF_8));
                }
            }

            if (!pubFile.exists()) {
                String pubPem = "-----BEGIN PUBLIC KEY-----\n" +
                        Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(rsaKeyPair.getPublic().getEncoded()) +
                        "\n-----END PUBLIC KEY-----\n";
                try (FileOutputStream fos = new FileOutputStream(pubFile)) {
                    fos.write(pubPem.getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (Exception e) {
            log.warn("Could not save generated keys to disk: {}", e.getMessage());
        }
    }

    private PrivateKey loadPrivateKey(File file) throws Exception {
        String key = Files.readString(file.toPath());
        key = key.replaceAll("-----BEGIN (.*)-----", "")
                 .replaceAll("-----END (.*)-----", "")
                 .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    private PublicKey loadPublicKey(File file) throws Exception {
        String key = Files.readString(file.toPath());
        key = key.replaceAll("-----BEGIN (.*)-----", "")
                 .replaceAll("-----END (.*)-----", "")
                 .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    private byte[] getAesKeyBytes() {
        byte[] keyBytes = new byte[32]; // AES-256
        byte[] secretBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(secretBytes, 0, keyBytes, 0, Math.min(secretBytes.length, 32));
        return keyBytes;
    }

    private byte[] getAesIvBytes() {
        byte[] iv = new byte[16];
        byte[] secretBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(secretBytes, 0, iv, 0, Math.min(secretBytes.length, 16));
        return iv;
    }

    /**
     * Encrypts candidateId using AES-256-CBC matching the voting system spec.
     */
    public String encryptVote(Long candidateId) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(getAesKeyBytes(), "AES");
            IvParameterSpec iv = new IvParameterSpec(getAesIvBytes());

            Cipher cipher = Cipher.getInstance(AES_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);

            byte[] encrypted = cipher.doFinal(String.valueOf(candidateId).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("Error encrypting vote: {}", e.getMessage());
            throw new RuntimeException("Vote encryption failed", e);
        }
    }

    /**
     * Decrypts encrypted vote payload to recover candidate ID.
     */
    public Long decryptVote(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isBlank()) return null;
        try {
            SecretKeySpec secretKey = new SecretKeySpec(getAesKeyBytes(), "AES");
            IvParameterSpec iv = new IvParameterSpec(getAesIvBytes());

            Cipher cipher = Cipher.getInstance(AES_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);

            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedBase64));
            String str = new String(decrypted, StandardCharsets.UTF_8).trim();
            return Long.parseLong(str);
        } catch (Exception e) {
            log.warn("Vote decryption failed for payload: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Signs the encrypted vote using RSA private key and SHA-256.
     */
    public String signVote(String encryptedVote) {
        try {
            Signature signature = Signature.getInstance(RSA_ALGO);
            signature.initSign(rsaKeyPair.getPrivate());
            signature.update(encryptedVote.getBytes(StandardCharsets.UTF_8));
            byte[] digitalSignature = signature.sign();
            return Base64.getEncoder().encodeToString(digitalSignature);
        } catch (Exception e) {
            log.error("Error signing vote: {}", e.getMessage());
            throw new RuntimeException("Vote signing failed", e);
        }
    }

    /**
     * Verifies digital signature with RSA public key.
     */
    public boolean verifySignature(String encryptedVote, String base64Signature) {
        if (encryptedVote == null || base64Signature == null) return false;
        try {
            Signature signature = Signature.getInstance(RSA_ALGO);
            signature.initVerify(rsaKeyPair.getPublic());
            signature.update(encryptedVote.getBytes(StandardCharsets.UTF_8));
            byte[] sigBytes = Base64.getDecoder().decode(base64Signature);
            return signature.verify(sigBytes);
        } catch (Exception e) {
            log.warn("RSA signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Computes SHA-256 vote hash: voterId + candidateId + positionId + encryptedVote + digitalSignature
     */
    public String calculateVoteHash(Long voterId, Long candidateId, Long positionId, String encryptedVote, String digitalSignature) {
        String data = "" + voterId + candidateId + positionId + encryptedVote + digitalSignature;
        return sha256(data);
    }

    /**
     * General SHA-256 digest helper.
     */
    public String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 hashing failed", e);
        }
    }

    public PublicKey getPublicKey() {
        return rsaKeyPair.getPublic();
    }
}
