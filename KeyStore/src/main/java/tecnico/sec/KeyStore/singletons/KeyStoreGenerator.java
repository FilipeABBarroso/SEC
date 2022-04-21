package tecnico.sec.KeyStore.singletons;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class KeyStoreGenerator {
    public static void main(String[] args) throws IOException {
        int count = Integer.parseInt(args[0]);
        Path basePath = Path.of("GeneratedKeyStores");
        for(int i = 0 ; i != count ; i++){
            if(Files.notExists(basePath)){
                Files.createDirectories(basePath);
            }
            if(Files.notExists(Path.of("GeneratedKeyStores/server" + i + "keystore.jks"))){
                KeyStore.changeKeyPath("GeneratedKeyStores/server" + i);
                try {
                    KeyStore.generateKeyStore();
                    BufferedWriter output;
                    output = new BufferedWriter(new FileWriter("GeneratedKeyStores/serversInfo.txt" , true));  //clears file every time
                    int port = 8080 + i;
                    output.write(port + "-" + KeyStore.publicKeyToString(KeyStore.getCredentials().getPublic()));
                    output.newLine();
                    output.close();
                }catch (Exception e){
                    System.out.println(e);
                    System.out.println("Something went wrong!");
                }
            }
        }
    }
}
