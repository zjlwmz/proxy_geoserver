package cn.geofound.geoservergateway;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** 实现现有 CND_PARAMS 的 AES/CBC/PKCS5Padding 传输格式。 */
public final class LegacyAesCipher {

    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private final byte[] key;

    public LegacyAesCipher(String key) {
        this.key = key.getBytes(StandardCharsets.UTF_8);
    }

    public String decrypt(String encrypted) throws Exception {
        byte[] input = Base64.getDecoder().decode(encrypted);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(key));
        byte[] plainBytes = cipher.doFinal(input);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(plainBytes))
                    .toString();
        } catch (CharacterCodingException ex) {
            throw new IllegalArgumentException("Decrypted CND_PARAMS is not UTF-8 text", ex);
        }
    }

    // 保持包内可见，供兼容性回归测试及运维诊断使用。
    String encrypt(String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(key));
        return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
    }
}
