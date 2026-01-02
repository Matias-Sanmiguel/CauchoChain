package model;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

public class CryptoUtils {

    public KeyPair generateKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            return kpg.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Error generating key pair", e);
        }
    }

    public KeyPair generateKeyPair(String seed) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            java.security.SecureRandom random = java.security.SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(seed.getBytes(StandardCharsets.UTF_8));
            kpg.initialize(2048, random);
            return kpg.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Error generating deterministic key pair", e);
        }
    }

    public String sign(String data, KeyPair keyPair) {
        try {
            PrivateKey privateKey = keyPair.getPrivate();
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(privateKey);
            sig.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = sig.sign();
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error signing data", e);
        }
    }

    public boolean verify(String data, String signature, KeyPair keyPair) {
        try {
            PublicKey publicKey = keyPair.getPublic();
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            return sig.verify(signatureBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error verifying signature", e);
        }
    }

    public String hash(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing data", e);
        }
    }
}
