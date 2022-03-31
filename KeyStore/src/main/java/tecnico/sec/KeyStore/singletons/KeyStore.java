package tecnico.sec.KeyStore.singletons;

import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.operator.OperatorCreationException;
import tecnico.sec.proto.exceptions.KeyExceptions;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Scanner;

public class KeyStore {

    private static KeyPair credentials;

    private static final String KEYSTOREPATH = "";
    private static final String KEYSTOREALIASPRIVATE = "PRIVATEKEY";
    private static final String KEYSTOREALIASCERTIFICATE = "CERTIFICATE";

    public static KeyPair getCredentials() throws KeyExceptions.NoSuchAlgorithmException{
        if (credentials == null) {
            try {
                credentials = loadKeyPair();
                System.out.println("");
            } catch (CertificateException | UnrecoverableEntryException | IOException e){
                System.out.println("Wrong password!");
                System.exit(0);
            } catch (Exception e) {
                e.printStackTrace();
                credentials = KeyTools.getKeyPairGenerator().generateKeyPair();
                try {
                    X509Certificate crt = CertificateGenerator.generate(credentials ,  "SHA256withRSA"  , "SELF" , 0);
                    saveKeyPair(credentials , crt);
                } catch (OperatorCreationException ex) {
                    ex.printStackTrace();
                } catch (CertificateException ex) {
                    ex.printStackTrace();
                } catch (CertIOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return credentials;
    }

    public static PublicKey getPublicKey() throws KeyExceptions.NoSuchAlgorithmException {
        return getCredentials().getPublic();
    }

    public static PrivateKey getPrivateKey() throws KeyExceptions.NoSuchAlgorithmException {
        return getCredentials().getPrivate();
    }

    public static PublicKey toPublicKey(byte[] publicKey) throws KeyExceptions.InvalidPublicKeyException, KeyExceptions.NoSuchAlgorithmException{
        try {
            return KeyTools.getKeyFactory().generatePublic(new X509EncodedKeySpec(publicKey));
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

    private static char[] requestPassword(){
        Console console = System.console();
        if(console != null){
            return console.readPassword("Enter your secret password: ");
        }
        System.out.print("Enter your secret password: ");
        Scanner s = new Scanner(System.in);
        return s.nextLine().toCharArray();
    }

    private static void saveKeyPair(KeyPair keyPair , X509Certificate cert){

        X509Certificate[] certificateChain = new X509Certificate[1];
        certificateChain[0] = cert;
        try {
            char[] password = requestPassword();
            java.security.KeyStore keyStore = KeyTools.getKeyStore();
            keyStore.setKeyEntry(KEYSTOREALIASPRIVATE , keyPair.getPrivate() , password , certificateChain);
            keyStore.setCertificateEntry(KEYSTOREALIASCERTIFICATE , cert);
            java.io.FileOutputStream fos = new java.io.FileOutputStream(KEYSTOREPATH + "keystore.jks");
            keyStore.store(fos, password);
            System.out.println(keyStore.getCertificate(KEYSTOREALIASCERTIFICATE).getPublicKey());
            fos.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyExceptions.KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private static KeyPair loadKeyPair() throws IOException, KeyExceptions.KeyStoreException, CertificateException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableEntryException{
        char[] password = requestPassword();
        File initialFile = new File(KEYSTOREPATH + "keystore.jks");
        InputStream ins = new FileInputStream(initialFile);

        java.security.KeyStore keyStore = KeyTools.getKeyStore();
        keyStore.load(ins, password);
        java.security.KeyStore.PasswordProtection keyPassword = new java.security.KeyStore.PasswordProtection(password);

        PublicKey publicKey = keyStore.getCertificate(KEYSTOREALIASCERTIFICATE).getPublicKey();

        java.security.KeyStore.PrivateKeyEntry privateKeyEntry = (java.security.KeyStore.PrivateKeyEntry) keyStore.getEntry(KEYSTOREALIASPRIVATE, keyPassword);
        PrivateKey privateKey = privateKeyEntry.getPrivateKey();

        return new KeyPair(publicKey, privateKey);

    }

}

class KeyTools{

    private static final String ALGORITHM = "RSA";
    private static final String KEYSTOREALGORITHM = "JCEKS";

    private static KeyFactory keyFactory;
    private static KeyPairGenerator keyGen;
    private static java.security.KeyStore keyStore;

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

    public static java.security.KeyStore getKeyStore() throws KeyExceptions.KeyStoreException {
        if (keyStore == null) {
            try {
                keyStore = java.security.KeyStore.getInstance(KEYSTOREALGORITHM);
                keyStore.load(null , null);
            } catch (KeyStoreException e) {
                throw new KeyExceptions.KeyStoreException();
            } catch (CertificateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        return keyStore;
    }
}

