package com.amannmalik.staticwebsitedeployer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SignatureVerifier {

    private final byte[] secretBytes;

    public SignatureVerifier(String secret) {
        secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    }

    public boolean verify(String content, String hexSignature) {
        byte[] signatureBytes = DatatypeConverter.parseHexBinary(hexSignature);
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        byte[] computedSignatureBytes = createSignature(contentBytes);
        return MessageDigest.isEqual(computedSignatureBytes, signatureBytes);
    }

    private byte[] createSignature(byte[] contentBytes) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretBytes, "HmacSHA1");
            mac.init(secretKeySpec);
            return mac.doFinal(contentBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }
}
