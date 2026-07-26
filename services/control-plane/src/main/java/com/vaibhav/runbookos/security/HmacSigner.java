package com.vaibhav.runbookos.security;

import com.vaibhav.runbookos.config.RunbookOsProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class HmacSigner {
  private final byte[] key;

  public HmacSigner(RunbookOsProperties properties) {
    this.key = properties.n8n().signingSecret().getBytes(StandardCharsets.UTF_8);
    if (key.length < 32)
      throw new IllegalArgumentException("INTERNAL_HMAC_SECRET must contain at least 32 bytes");
  }

  public String sign(String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key, "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.GeneralSecurityException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public boolean verify(String value, String supplied) {
    if (supplied == null) return false;
    return MessageDigest.isEqual(
        sign(value).getBytes(StandardCharsets.US_ASCII),
        supplied.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
  }
}
