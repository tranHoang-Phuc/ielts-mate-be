package com.fptu.sep490.commonlibrary.utils;

import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class AesSecretKeyUtils {
    @Value("${aes.secret.key}")
    private static String secretKey;

    @Value("${aes.algorithm}")
    private static final String ALGORITHM = "AES";
    @Value("${aes.cipher}")
    private static final String CIPHER_MODE = "AES/CBC/PKCS5Padding";
    @Value("${aes.iv-length}")
    private static final int IV_LENGTH   = 16;

    private static SecretKeySpec getKeyFromString() throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(secretKey.getBytes("UTF-8"));
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    public static String encrypt(String plaintext) throws Exception {
        SecretKeySpec keySpec = getKeyFromString();
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance(CIPHER_MODE);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] cipherBytes = cipher.doFinal(plaintext.getBytes("UTF-8"));
        byte[] ivAndCipher = new byte[IV_LENGTH + cipherBytes.length];
        System.arraycopy(iv,           0, ivAndCipher, 0,        IV_LENGTH);
        System.arraycopy(cipherBytes,  0, ivAndCipher, IV_LENGTH, cipherBytes.length);
        return Base64.getEncoder().encodeToString(ivAndCipher);
    }

    public static String decrypt(String b64IvAndCipher) throws Exception {
        byte[] ivAndCipher = Base64.getDecoder().decode(b64IvAndCipher);
        byte[] iv          = Arrays.copyOfRange(ivAndCipher, 0, IV_LENGTH);
        byte[] cipherBytes = Arrays.copyOfRange(ivAndCipher, IV_LENGTH, ivAndCipher.length);
        SecretKeySpec keySpec = getKeyFromString();
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance(CIPHER_MODE);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] plainBytes = cipher.doFinal(cipherBytes);
        return new String(plainBytes, "UTF-8");
    }

}
