package com.vaibhav.runbookos.security;

import com.vaibhav.runbookos.exception.DomainException;
import java.net.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Resolves and validates every custom HTTP target and every redirect target before use. */
@Component
public class SafeHttpTargetValidator {
  public URI validate(String rawTarget, Set<String> explicitlyAllowlistedPrivateHosts) {
    final URI target;
    try {
      target = URI.create(rawTarget).normalize();
    } catch (RuntimeException ex) {
      throw blocked("Target is not a valid URI");
    }
    if (!"https".equalsIgnoreCase(target.getScheme())
        || target.getHost() == null
        || target.getUserInfo() != null) {
      throw blocked("Only credential-free HTTPS targets are allowed");
    }
    String host = target.getHost().toLowerCase(Locale.ROOT);
    if ("localhost".equals(host) || host.endsWith(".localhost")) {
      throw blocked("Localhost targets are denied");
    }
    boolean allowPrivate = explicitlyAllowlistedPrivateHosts.contains(host);
    final InetAddress[] addresses;
    try {
      addresses = InetAddress.getAllByName(host);
    } catch (UnknownHostException ex) {
      throw blocked("Target DNS resolution failed");
    }
    if (addresses.length == 0) throw blocked("Target has no DNS addresses");
    for (InetAddress address : addresses) {
      if (!allowPrivate && isDangerous(address)) {
        throw blocked("Target resolves to a private, loopback, or link-local address");
      }
    }
    return target;
  }

  /** Call this for every Location header before following it. */
  public URI validateRedirect(
      URI original, String location, Set<String> explicitlyAllowlistedPrivateHosts) {
    URI resolved = original.resolve(location);
    return validate(resolved.toString(), explicitlyAllowlistedPrivateHosts);
  }

  private static boolean isDangerous(InetAddress address) {
    if (address.isAnyLocalAddress()
        || address.isLoopbackAddress()
        || address.isLinkLocalAddress()
        || address.isSiteLocalAddress()
        || address.isMulticastAddress()) return true;
    byte[] bytes = address.getAddress();
    if (bytes.length == 4) {
      int first = Byte.toUnsignedInt(bytes[0]);
      int second = Byte.toUnsignedInt(bytes[1]);
      return first == 0
          || first == 10
          || first == 127
          || (first == 169 && second == 254)
          || (first == 172 && second >= 16 && second <= 31)
          || (first == 192 && second == 168)
          || first >= 224;
    }
    return address.isLoopbackAddress()
        || address.isLinkLocalAddress()
        || address.isSiteLocalAddress();
  }

  private static UnsafeTargetException blocked(String message) {
    return new UnsafeTargetException(message);
  }

  public static final class UnsafeTargetException extends DomainException {
    UnsafeTargetException(String message) {
      super("HTTP_TARGET_BLOCKED", message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
  }
}
