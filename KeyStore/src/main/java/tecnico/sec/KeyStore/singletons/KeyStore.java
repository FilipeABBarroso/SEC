package tecnico.sec.KeyStore.singletons;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class KeyStore {

    private static KeyPair credentials;

    private static final String KEYSTOREPATH = "PUT PATH HERE";

    public static KeyPair getCredentials() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        if (credentials == null) {
            credentials = loadKeyPair(KEYSTOREPATH);
        }
        return credentials;
    }

    public static PublicKey getPublicKey(){
        return credentials.getPublic();
    }

    public static PrivateKey getPrivateKey(){
        return credentials.getPrivate();
    }

    public static PublicKey stringToPubKey(String pubKeyString) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] publicBytes = Base64.getDecoder().decode(pubKeyString);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    public static String pubKeyToString(PublicKey pubKey) {
        return Base64.getEncoder().encodeToString(pubKey.getEncoded());
    }

    public static void saveKeyPair(KeyPair clientKeyPair, String path){
        try {
            PrivateKey privateKey = clientKeyPair.getPrivate();
            PublicKey publicKey = clientKeyPair.getPublic();

            // Store Public Key.
            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(
                    publicKey.getEncoded());
            FileOutputStream fos = new FileOutputStream( path + "public.key");
            fos.write(x509EncodedKeySpec.getEncoded());
            fos.close();

            // Store Private Key.
            PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(
                    privateKey.getEncoded());
            fos = new FileOutputStream(path + "private.key");
            fos.write(pkcs8EncodedKeySpec.getEncoded());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static KeyPair loadKeyPair(String path) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Read Public Key.
        File filePublicKey = new File(path + "public.key");
        FileInputStream fis = new FileInputStream(path + "public.key");
        byte[] encodedPublicKey = new byte[(int) filePublicKey.length()];
        fis.read(encodedPublicKey);
        fis.close();

        // Read Private Key.
        File filePrivateKey = new File(path + "private.key");
        fis = new FileInputStream(path + "private.key");
        byte[] encodedPrivateKey = new byte[(int) filePrivateKey.length()];
        fis.read(encodedPrivateKey);
        fis.close();

        // Generate KeyPair.
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(
                encodedPublicKey);
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(
                encodedPrivateKey);
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

        return new KeyPair(publicKey, privateKey);
    }
}
