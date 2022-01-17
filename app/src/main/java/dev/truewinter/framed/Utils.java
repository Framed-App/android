package dev.truewinter.framed;

import android.util.Base64;

import org.json.JSONObject;

import java.nio.Buffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.CharacterIterator;
import java.text.DecimalFormat;
import java.text.StringCharacterIterator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.ExemptionMechanismException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class Utils {
    static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static SecureRandom rnd = new SecureRandom();

    public static String decrypt(byte[] encrypted, String key, String iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));

        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        byte[] decrypted = cipher.doFinal(encrypted);

        return new String(decrypted);
    }

    public static String encrypt(JSONObject data, String key, String iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        byte[] decrypted = cipher.doFinal(data.toString().getBytes(StandardCharsets.UTF_8));

        return Base64.encodeToString(decrypted, Base64.DEFAULT);
    }

    public static String getRandomString(int len) {
        StringBuilder sb = new StringBuilder(len);
        for(int i = 0; i < len; i++)
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        return sb.toString();
    }

    // https://www.devglan.com/java8/rsa-encryption-decryption-java
    public static String encryptRSA(String data, String publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, getPublicKey(publicKey));
        return Base64.encodeToString(cipher.doFinal(data.getBytes()), Base64.DEFAULT);
    }

    // https://stackoverflow.com/questions/24223275/when-to-use-x509encodedkeyspec-vs-rsapublickeyspec
    private static PublicKey getPublicKey(String base64PublicKey) {
        PublicKey publicKey = null;
        try {
            base64PublicKey = base64PublicKey.replace("-----BEGIN PUBLIC KEY-----\\n", "");
            base64PublicKey = base64PublicKey.replace("-----END PUBLIC KEY-----\\n", "");
            base64PublicKey = base64PublicKey.replaceAll("\\\\n", "\n");

            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.decode(base64PublicKey, Base64.DEFAULT));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKey = keyFactory.generatePublic(keySpec);
            return publicKey;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return publicKey;
    }

    public static boolean verifySignature(String signature, String message, String publicKey) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(getPublicKey(publicKey));
        sig.update(message.getBytes());
        return sig.verify(Base64.decode(signature, Base64.DEFAULT));
    }

    public static String getKeyFingerprint(String publicKey) throws Exception {
        publicKey = publicKey.replaceAll("\\\\n", "\n");

        String hash = sha256HashAsBase64(publicKey).toUpperCase()
                .replaceAll(Pattern.quote("+"), "X")
                .replaceAll(Pattern.quote("/"), "Z")
                .substring(0, 6);

        return rotateVowels(hash);
    }

    // This function is to replace the vowels to prevent
    // any bad words being produced by the fingerprint function
    private static String rotateVowels(String string) {
        final int ROTATE = 1;
        final String[] chars = new String[] {"A", "E", "I", "O", "U", "Y"};
        final Set<String> CHARS = new HashSet(Arrays.asList(chars));
        String[] stringArr = string.split("");
        String out = "";

        for (int i = 0; i < stringArr.length; i++) {
            if (CHARS.contains(stringArr[i])) {
                out += Character.toString((char) ((int) stringArr[i].charAt(0) + ROTATE));
            } else {
                out += stringArr[i];
            }
        }

        return out;
    }

    public static String sha256HashAsBase64(String text) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(hash, Base64.DEFAULT);
    }

    // https://stackoverflow.com/a/3758880
    public static String humanReadableByteCount(long size) {
        if(size <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static String humanReadableBitCount(long size) {
        if(size <= 0) return "0 b";
        final String[] units = new String[] { "b", "Kb", "Mb", "Gb", "Tb" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
