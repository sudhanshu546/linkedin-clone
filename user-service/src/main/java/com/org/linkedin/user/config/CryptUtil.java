package com.org.linkedin.user.config;

import static java.nio.charset.StandardCharsets.UTF_8;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CryptUtil {

  private static String KEY;

  private static String INIT_VECTOR;
  private static final String ALOGRITHM = "AES";
  private static final String TRANSAFORMATION = "AES/CBC/PKCS5PADDING";

  @Value("${encryption.secureSharingEncryptKey}")
  public void setKey(String key) {
    KEY = key;
  }

  @Value("${encryption.secureSharingEncryptInitVector}")
  public void setInitVector(String initVector) {
    INIT_VECTOR = initVector;
  }

  /**
   * Encrypts a given string using AES/CBC/PKCS5PADDING encryption algorithm.
   *
   * @param value The string to be encrypted.
   * @return The encrypted string in Base64 encoding.
   * @throws Exception If any error occurs during the encryption process.
   */
  public static String encrypt(final String value) throws Exception {
    Cipher cipher = getCipher(Cipher.ENCRYPT_MODE);
    byte[] encrypted = cipher.doFinal(value.getBytes());

    return Base64.encodeBase64String(encrypted);
  }

  public static String encrypt(final String value, final String secretKey, final String initVector)
      throws Exception {
    Cipher cipher = getCipher(Cipher.ENCRYPT_MODE, secretKey, initVector);
    byte[] encrypted = cipher.doFinal(value.getBytes());

    return Base64.encodeBase64String(encrypted);
  }

  /**
   * Decrypts a Base64 encoded string that was encrypted using the AES/CBC/PKCS5PADDING encryption
   * algorithm.
   *
   * @param encrypted The Base64 encoded string to be decrypted.
   * @return The decrypted string.
   * @throws Exception If any error occurs during the decryption process.
   */
  public static String decrypt(final String encrypted) throws Exception {
    Cipher cipher = getCipher(Cipher.DECRYPT_MODE);
    byte[] original = cipher.doFinal(Base64.decodeBase64(encrypted));
    return new String(original);
  }

  /**
   * Initializes and returns a Cipher instance for encryption or decryption.
   *
   * @param mode the operation mode of the cipher (e.g., Cipher.ENCRYPT_MODE or
   *     Cipher.DECRYPT_MODE).
   * @return the initialized Cipher instance.
   * @throws Exception if an error occurs during the cipher initialization.
   */
  private static Cipher getCipher(final int mode) throws Exception {
    IvParameterSpec iv = new IvParameterSpec(INIT_VECTOR.getBytes(UTF_8));
    SecretKeySpec skeySpec = new SecretKeySpec(KEY.getBytes(UTF_8), ALOGRITHM);

    Cipher cipher = Cipher.getInstance(TRANSAFORMATION);
    cipher.init(mode, skeySpec, iv);

    return cipher;
  }

  /**
   * Initializes and returns a Cipher instance for encryption or decryption.
   *
   * @param mode the operation mode of the cipher (e.g., Cipher.ENCRYPT_MODE or
   *     Cipher.DECRYPT_MODE).
   * @param secretKey the secret key used for encryption/decryption.
   * @param initVector the initialization vector used for encryption/decryption.
   * @return the initialized Cipher instance.
   * @throws Exception if an error occurs during the cipher initialization.
   */
  private static Cipher getCipher(final int mode, final String secretKey, final String initVector)
      throws Exception {
    IvParameterSpec iv = new IvParameterSpec(initVector.getBytes(UTF_8));
    SecretKeySpec skeySpec = new SecretKeySpec(secretKey.getBytes(UTF_8), ALOGRITHM);

    Cipher cipher = Cipher.getInstance(TRANSAFORMATION);
    cipher.init(mode, skeySpec, iv);

    return cipher;
  }
}
