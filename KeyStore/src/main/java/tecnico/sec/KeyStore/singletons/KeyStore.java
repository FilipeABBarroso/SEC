package tecnico.sec.KeyStore.singletons;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class KeyStore {

    private static KeyPair credentials;

    private static final String KEYSTOREPATH = "/keys/";

    public static KeyPair getCredentials() {
        if (credentials == null) {
            try {
                credentials = loadKeyPair();
            } catch (IOException | InvalidKeySpecException e) {
                credentials = KeyTools.getKeyPairGenerator().generateKeyPair();
                saveKeyPair(credentials);
            }
        }
        return credentials;
    }

    public static PublicKey getPublicKey(){
        return credentials.getPublic();
    }

    public static PrivateKey getPrivateKey(){
        return credentials.getPrivate();
    }

    public static PublicKey stringToPubKey(String pubKeyString) throws InvalidKeySpecException {
        byte[] publicBytes = Base64.getDecoder().decode(pubKeyString);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
        KeyFactory keyFactory = KeyTools.getKeyFactory();
        return keyFactory.generatePublic(keySpec);
    }

    public static String pubKeyToString(PublicKey pubKey) {
        return Base64.getEncoder().encodeToString(pubKey.getEncoded());
    }

    private static void saveToFile(String path , byte[] toSave) throws IOException {
        FileOutputStream fos = new FileOutputStream( path);
        fos.write(toSave);
        fos.close();
    }

    private static void saveKeyPair(KeyPair keyPair){
        try {
            PublicKey publicKey = keyPair.getPublic();
            saveToFile(KEYSTOREPATH + "public.pub" , publicKey.getEncoded());
            PrivateKey privateKey = keyPair.getPrivate();
            saveToFile(KEYSTOREPATH + "private.key" , privateKey.getEncoded());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] readFile(String path) throws IOException {
        Path keyPath = Paths.get(path);
        return Files.readAllBytes(keyPath);
    }

    private static KeyPair loadKeyPair() throws InvalidKeySpecException, IOException {

        KeyFactory keyFactory = KeyTools.getKeyFactory();

        byte[] encodedPublicKey = readFile(KEYSTOREPATH + "public.pub");
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedPublicKey);
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        byte[] encodedPrivateKey = readFile(KEYSTOREPATH + "private.key");
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

        return new KeyPair(publicKey, privateKey);
    }

}

class KeyTools{

    private static final String ALGORITHM = "RSA";

    private static KeyFactory keyFactory;
    private static KeyPairGenerator keyGen;

    public static KeyFactory getKeyFactory(){
        if (keyFactory == null) {
            try {
                keyFactory = KeyFactory.getInstance(ALGORITHM);
            } catch (NoSuchAlgorithmException e) {
                System.out.println("Key factory algorithm " + ALGORITHM + "not found.");
            }
        }
        return keyFactory;
    }

    public static KeyPairGenerator getKeyPairGenerator(){
        if (keyGen == null) {
            try {
                keyGen = KeyPairGenerator.getInstance(ALGORITHM);
                keyGen.initialize(4096);
            } catch (NoSuchAlgorithmException e) {
                System.out.println("Key generator algorithm " + ALGORITHM + "not found.");
            }
        }
        return keyGen;
    }
}

