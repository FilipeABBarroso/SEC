package tecnico.sec.KeyStore.singletons;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.*;

public class Sign {

    public byte[] signMessage(Object ... args) throws SignatureException, InvalidKeyException {
        Signature signature = SignTools.getSignature();
        signature.initSign(KeyStore.getPrivateKey());
        try {
            byte[] toSign = SignTools.argsToBytes(args);
            signature.update(toSign);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return signature.sign();
    }

    public boolean checkSignature(PublicKey publicKey , byte[] toVerifySignature, Object ... args) throws SignatureException, InvalidKeyException {
        Signature signature = SignTools.getSignature();
        signature.initVerify(publicKey);
        try {
            byte[] toValidate = SignTools.argsToBytes(args);
            signature.update(toValidate);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return signature.verify(toVerifySignature);
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
