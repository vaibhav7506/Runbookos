package com.vaibhav.runbookos.security.crypto;

import com.vaibhav.runbookos.config.RunbookOsProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Authenticated encryption for integration secrets, using AES-256-GCM.
 *
 * <p>GCM is chosen over CBC so that tampering with stored ciphertext is detected on decryption
 * rather than silently yielding garbage. A fresh 96-bit nonce is generated per encryption; nonce
 * reuse under the same key would be catastrophic for GCM, so nonces are never derived from data.
 *
 * <p>The master key is supplied through configuration and never written to the database. {@link
 * #keyId()} is recorded alongside each ciphertext so keys can be rotated without needing to know the
 * plaintext of existing records.
 */
@Component
public class SecretCipher {

  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final int NONCE_LENGTH_BYTES = 12;
  private static final int TAG_LENGTH_BITS = 128;
  private static final int FINGERPRINT_LENGTH = 12;

  private final SecretKey key;
  private final String keyId;
  private final SecureRandom random = new SecureRandom();

  public SecretCipher(RunbookOsProperties properties) {
    byte[] keyBytes = decodeKey(properties.security().encryptionKey());
    this.key = new SecretKeySpec(keyBytes, "AES");
    this.keyId = properties.security().encryptionKeyId();
  }

  private static byte[] decodeKey(String configuredKey) {
    byte[] decoded;
    try {
      decoded = Base64.getDecoder().decode(configuredKey);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(
          "runbookos.security.encryption-key must be Base64-encoded", e);
    }
    if (decoded.length != 32) {
      throw new IllegalStateException(
          "runbookos.security.encryption-key must decode to exactly 32 bytes (AES-256), got "
              + decoded.length);
    }
    return decoded;
  }

  /** Encrypts UTF-8 plaintext. The returned nonce must be stored and supplied back on decryption. */
  public EncryptedValue encrypt(String plaintext) {
    byte[] nonce = new byte[NONCE_LENGTH_BYTES];
    random.nextBytes(nonce);
    try {
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
      byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      return new EncryptedValue(
          Base64.getEncoder().encodeToString(ciphertext),
          Base64.getEncoder().encodeToString(nonce),
          keyId,
          fingerprint(plaintext));
    } catch (GeneralSecurityException e) {
      // Never include the plaintext or key in the message.
      throw new IllegalStateException("Failed to encrypt secret material", e);
    }
  }

  /**
   * Decrypts a stored secret. Throws if the ciphertext or nonce has been altered, because GCM
   * verifies the authentication tag.
   */
  public String decrypt(String ciphertextBase64, String nonceBase64) {
    try {
      byte[] nonce = Base64.getDecoder().decode(nonceBase64);
      byte[] ciphertext = Base64.getDecoder().decode(ciphertextBase64);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
      return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      throw new IllegalStateException("Failed to decrypt secret material", e);
    }
  }

  /**
   * Short, non-reversible digest of a secret so an operator can verify which value is installed
   * without it ever being displayed. Truncated SHA-256; not suitable as a password hash.
   */
  public static String fingerprint(String plaintext) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(plaintext.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash).substring(0, FINGERPRINT_LENGTH);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  public String keyId() {
    return keyId;
  }

  /**
   * Result of an encryption.
   *
   * @param ciphertext Base64 ciphertext including the GCM authentication tag
   * @param nonce Base64 nonce, unique per encryption
   * @param keyId identifier of the master key used
   * @param fingerprint non-reversible digest for operator confirmation
   */
  public record EncryptedValue(String ciphertext, String nonce, String keyId, String fingerprint) {}
}
