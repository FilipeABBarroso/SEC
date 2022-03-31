package tecnico.sec.KeyStore.singletons;

import tecnico.sec.proto.exceptions.KeyExceptions;

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

    private static final String KEYSTOREPATH = "";

    public static KeyPair getCredentials() throws KeyExceptions.NoSuchAlgorithmException{
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

    public static PublicKey toPublicKey(byte[] publicKey) throws KeyExceptions.InvalidPublicKeyException, KeyExceptions.NoSuchAlgorithmException{
        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKey);
            KeyFactory keyFactory = KeyTools.getKeyFactory();
            return keyFactory.generatePublic(keySpec);
        } catch (InvalidKeySpecException e) {
            throw new KeyExceptions.InvalidPublicKeyException();
        }
    }

    public static PublicKey stringToPublicKey(String publicKeyString) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] publicBytes = Base64.getDecoder().decode(publicKeyString);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    private static void saveToFile(String path , byte[] toSave) throws IOException {
        FileOutputStream fos = new FileOutputStream(path);
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

    private static KeyPair loadKeyPair() throws InvalidKeySpecException, IOException, KeyExceptions.NoSuchAlgorithmException {

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

    public static KeyFactory getKeyFactory() throws KeyExceptions.NoSuchAlgorithmException {
        if (keyFactory == null) {
            try {
                keyFactory = KeyFactory.getInstance(ALGORITHM);
            } catch (NoSuchAlgorithmException e) {
                System.out.println("Key factory algorithm " + ALGORITHM + "not found.");
                throw new KeyExceptions.NoSuchAlgorithmException();
            }
        }
        return keyFactory;
    }

    public static KeyPairGenerator getKeyPairGenerator() throws KeyExceptions.NoSuchAlgorithmException {
        if (keyGen == null) {
            try {
                keyGen = KeyPairGenerator.getInstance(ALGORITHM);
                keyGen.initialize(4096);
            } catch (NoSuchAlgorithmException e) {
                System.out.println("Key generator algorithm " + ALGORITHM + "not found.");
                throw new KeyExceptions.NoSuchAlgorithmException();
            }
        }
        return keyGen;
    }
}

