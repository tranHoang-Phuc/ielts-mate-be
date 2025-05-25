package com.fptu.sep490.commonlibrary.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
@Component
public class AesSecretKeyUtils {
    private final String secretKey;
    private final String algorithm;
    private final String cipherMode;
    private final int ivLength;

    public AesSecretKeyUtils(
            @Value("${aes.secret.key}") String secretKey,
            @Value("${aes.algorithm:AES}") String algorithm,
            @Value("${aes.cipher:AES/CBC/PKCS5Padding}") String cipherMode,
            @Value("${aes.iv-length:16}") int ivLength
    ) {
        this.secretKey  = secretKey;
        this.algorithm  = algorithm;
        this.cipherMode = cipherMode;
        this.ivLength   = ivLength;
    }

    private  SecretKeySpec getKeyFromString() throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(secretKey.getBytes("UTF-8"));
        return new SecretKeySpec(keyBytes, algorithm);
    }

    public  String encrypt(String plaintext) throws Exception {
        SecretKeySpec keySpec = getKeyFromString();
        byte[] iv = new byte[ivLength];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance(cipherMode);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] cipherBytes = cipher.doFinal(plaintext.getBytes("UTF-8"));
        byte[] ivAndCipher = new byte[ivLength + cipherBytes.length];
        System.arraycopy(iv,           0, ivAndCipher, 0,        ivLength);
        System.arraycopy(cipherBytes,  0, ivAndCipher, ivLength, cipherBytes.length);
        return Base64.getEncoder().encodeToString(ivAndCipher);
    }

    public  String decrypt(String b64IvAndCipher) throws Exception {
        byte[] ivAndCipher = Base64.getDecoder().decode(b64IvAndCipher);
        byte[] iv          = Arrays.copyOfRange(ivAndCipher, 0, ivLength);
        byte[] cipherBytes = Arrays.copyOfRange(ivAndCipher, ivLength, ivAndCipher.length);
        SecretKeySpec keySpec = getKeyFromString();
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance(cipherMode);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] plainBytes = cipher.doFinal(cipherBytes);
        return new String(plainBytes, "UTF-8");
    }

}
