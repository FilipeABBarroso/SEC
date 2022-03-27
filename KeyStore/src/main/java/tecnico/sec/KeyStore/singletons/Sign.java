package tecnico.sec.KeyStore.singletons;

import tecnico.sec.proto.exceptions.IOExceptions;
import tecnico.sec.proto.exceptions.KeyExceptions;
import tecnico.sec.proto.exceptions.SignatureExceptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.*;

public class Sign {

    public static byte[] signMessage(Object ... args) throws KeyExceptions.InvalidPublicKeyException, SignatureExceptions.CanNotSignException, IOExceptions.IOException {
        try {
            Signature signature = SignTools.getSignature();
            signature.initSign(KeyStore.getPrivateKey());
            byte[] toSign = SignTools.argsToBytes(args);
            signature.update(toSign);
            return signature.sign();
        } catch (IOException e) {
            throw new IOExceptions.IOException();
        } catch (SignatureException e) {
            throw new SignatureExceptions.CanNotSignException();
        } catch (InvalidKeyException e) {
            throw new KeyExceptions.InvalidPublicKeyException();
        }
    }

    public static void checkSignature(byte[] publicKeyString , byte[] toVerifySignature, Object ... args) throws IOExceptions.IOException, SignatureExceptions.CanNotSignException, KeyExceptions.InvalidPublicKeyException, SignatureExceptions.SignatureDoNotMatchException, KeyExceptions.NoSuchAlgorithmException {
        try {
            PublicKey publicKey = KeyStore.toPublicKey(publicKeyString);
            Signature signature = SignTools.getSignature();
            signature.initVerify(publicKey);
            byte[] toValidate = SignTools.argsToBytes(args);
            signature.update(toValidate);
            if(!signature.verify(toVerifySignature)) throw new SignatureExceptions.SignatureDoNotMatchException();
        } catch (IOException e) {
            throw new IOExceptions.IOException();
        } catch (SignatureException e) {
            throw new SignatureExceptions.CanNotSignException();
        } catch (InvalidKeyException e) {
            throw new KeyExceptions.InvalidPublicKeyException();
        } catch (KeyExceptions.NoSuchAlgorithmException e) {
            throw new KeyExceptions.NoSuchAlgorithmException();
        }
    }
}

class SignTools {
    private static final String ALGORITHM = "SHA256WithRSA";
    private static Signature signature;

    public static Signature getSignature(){
        if( signature == null){
            try {
                signature = Signature.getInstance("SHA256WithRSA");
            } catch (NoSuchAlgorithmException e) {
                System.out.println("Signature algorithm " + ALGORITHM + "not found.");
            }
        }
        return signature;
    }

    public static byte[] argsToBytes(Object ... args) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(args);
        return bos.toByteArray();
    }
}
