package com.jarvis.nextgen;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;

/** Small Keystore-backed store for secrets such as provider tokens. */
public final class SecureStore {
    private static final String STORE = "jarvis_secure";
    private static final String KEY_ALIAS = "jarvis_app_key_v1";
    private static final String PREFS = "encrypted_values";
    private final SharedPreferences prefs;

    public SecureStore(Context context) { prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    public void put(String name, String value) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, randomNonce()));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] packed = new byte[cipher.getIV().length + encrypted.length];
            System.arraycopy(cipher.getIV(), 0, packed, 0, cipher.getIV().length);
            System.arraycopy(encrypted, 0, packed, cipher.getIV().length, encrypted.length);
            prefs.edit().putString(name, Base64.encodeToString(packed, Base64.NO_WRAP)).apply();
        } catch (Exception e) { throw new IllegalStateException("Secure storage unavailable", e); }
    }

    public String get(String name) {
        String encoded = prefs.getString(name, null); if (encoded == null) return null;
        try {
            byte[] packed = Base64.decode(encoded, Base64.NO_WRAP); byte[] iv = new byte[12]; byte[] ciphertext = new byte[packed.length - 12];
            System.arraycopy(packed, 0, iv, 0, 12); System.arraycopy(packed, 12, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) { return null; }
    }

    public void remove(String name) { prefs.edit().remove(name).apply(); }
    public void clear() { prefs.edit().clear().apply(); }

    private SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance(STORE); ks.load(null);
        if (!ks.containsAlias(KEY_ALIAS)) {
            KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, STORE);
            kg.init(new KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            kg.generateKey();
        }
        return ((KeyStore.SecretKeyEntry) ks.getEntry(KEY_ALIAS, null)).getSecretKey();
    }
    private byte[] randomNonce() { byte[] nonce = new byte[12]; new SecureRandom().nextBytes(nonce); return nonce; }
}
