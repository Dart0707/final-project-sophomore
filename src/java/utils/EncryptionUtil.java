package utils;

import java.nio.ByteBuffer;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.spec.IvParameterSpec;

public class EncryptionUtil {

    public static String encrypt(String plainText, String key, String algorithm) throws Exception {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        byte[] combined = ByteBuffer.allocate(iv.length + encryptedBytes.length)
                .put(iv)
                .put(encryptedBytes)
                .array();

        return Base64.getEncoder().encodeToString(combined);
    }

    public static String decrypt(String combinedBase64, String key, String algorithm) throws Exception {
        byte[] combined = Base64.getDecoder().decode(combinedBase64);

        ByteBuffer bb = ByteBuffer.wrap(combined);
        byte[] iv = new byte[16];
        bb.get(iv);
        byte[] encryptedBytes = new byte[bb.remaining()];
        bb.get(encryptedBytes);

        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}