package tecnico.sec.KeyStore.singletons;

import tecnico.sec.KeyStore.Credentials;

public class KeyStore {

    private static Credentials credentials;

    public static Credentials getCredentials(){
        if(credentials != null){
            return credentials;
        }
        else{
            //todo read file create credentials assign to static variable and return it
            return null;
        }

    }

    public static String getPublicKey(){
        Credentials credentials = getCredentials();
        return credentials.getPublicKey();
    }

    public static String getPrivateKey(){
        Credentials credentials = getCredentials();
        return credentials.getPrivateKey();
    }
}
