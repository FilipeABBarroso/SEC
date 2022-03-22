package tecnico.sec.KeyStore.singletons;

public class Nonce {
    private static int nonce;

    public static int getNonce(){
        if( nonce > 0 ){
            return nonce;
        }
        else{
            //todo read from file assign to static variable and return it
            return 1;
        }
    }
}
