package dbController;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tecnico.sec.KeyStore.singletons.KeyStore;
import tecnico.sec.grpc.*;
import tecnico.sec.proto.exceptions.BalanceExceptions;
import tecnico.sec.proto.exceptions.KeyExceptions;
import tecnico.sec.server.ServiceImpl;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class EndPointTests {

    public static final byte[] CREATEDUSER = {48, -126, 2, 34, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 3, -126, 2, 15, 0, 48, -126, 2, 10, 2, -126, 2, 1, 0, -75, -102, -29, 1, -20, -99, 103, -94, 65, 88, -99, -41, 7, 76, 107, 121, 116, 20, -19, 96, 44, 106, -104, -91, 4, -20, 55, -76, 67, 110, 60, 48, 63, 26, 21, 19, -115, 38, 60, 32, 110, -4, 21, 96, 72, 73, -47, -118, -45, -61, -53, -50, 11, -29, -23, -83, -67, -62, 100, -81, -54, -56, 85, -99, -89, -84, 56, 17, -118, 26, -35, -35, -77, 110, -103, 29, -20, -39, 71, -52, -90, 23, 0, 60, 64, 75, -50, -70, 20, -71, -110, 33, 15, 96, -77, 66, 109, 53, 83, 48, 2, -45, -67, -119, 17, 72, -38, 32, -72, 2, 15, -76, -35, -41, 62, -3, -4, -100, -80, 83, -74, -58, -72, -31, -84, -106, -44, 104, -71, -6, 82, -1, -105, 5, 127, 42, 107, -56, 9, 121, -34, 63, 56, -77, -36, 83, 105, -91, -69, -54, -46, -80, -34, 48, 36, 10, 66, 50, 29, -42, -13, -91, -42, 64, -85, -46, 77, 0, 78, -35, 52, 68, 41, 59, 100, -21, 126, 51, 26, -51, 60, 75, -83, 121, 121, 120, -29, 82, -112, 60, 2, 3, 37, 45, 35, -39, -113, 39, -18, -32, -40, -76, 61, 69, 92, -62, 98, 19, -35, 66, -14, -30, 74, -126, -53, -21, 125, -13, 15, -123, -122, -32, -88, 21, -50, -46, 21, 11, -115, 95, -44, -72, 9, -50, 93, 42, 73, -54, -31, -12, -84, 27, -53, 68, 114, 20, 67, 103, 108, 29, -113, 23, 4, -49, 111, 82, 59, -55, -86, -102, 106, -10, 49, -72, -120, -106, 78, -24, -114, -43, -8, 106, 121, -23, 116, 115, -17, -122, -31, 16, -6, -121, 105, -70, 33, 88, 73, 80, -108, -69, -110, -96, 111, 111, -5, -85, 120, 15, -65, -49, -114, 0, 7, 120, -47, -116, 117, -128, -2, -59, -69, -75, -5, 14, -64, 118, -16, -68, -18, 13, -70, -95, 70, 17, 46, -34, 59, 99, -75, -127, -77, -5, -57, 65, -87, -118, 96, -56, 123, 56, -27, -49, 49, 100, -117, 64, -56, -17, 46, 121, -71, -119, -86, 25, -49, -122, -80, -47, 121, 9, -63, -85, -7, -44, -1, 89, -110, -80, 53, 101, -80, -57, -49, 3, 30, 14, -99, -75, -71, -45, -77, 25, 94, 89, 8, -31, 21, -117, 71, -91, 73, 8, -91, 126, -45, 74, 126, 86, 123, -80, -5, -64, -97, 89, -75, 78, -22, -108, -82, -22, -6, 84, -45, -39, -86, 116, -61, 35, -16, 39, -53, -52, -39, 76, 16, -43, -43, 36, 11, 30, 58, 44, -58, 110, -90, -104, -28, 90, 79, -37, 55, -33, 108, 67, 39, -79, 88, 115, -117, 4, 70, -84, -35, -54, -9, 14, -88, -23, 94, -7, -80, -93, 15, 54, -51, 12, -59, -1, -105, -10, -65, 57, 83, 45, -39, 24, 43, -58, -13, 39, -17, -68, 20, 79, -124, 74, -47, -3, 40, -103, -101, 116, -125, -6, -22, -51, 66, -118, 38, 99, 69, -82, -9, -118, 86, 72, 87, 119, -37, -115, -116, -27, 2, 3, 1, 0, 1};
    public static final byte[] NOTCREATEDUSER = {48, -126, 2, 34, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 3, -126, 2, 15, 0, 48, -126, 2, 10, 2, -126, 2, 1, 0, -75, -102, -29, 1, -20, -99, 103, -94, 65, 88, -99, -41, 7, 76, 107, 121, 116, 20, -19, 96, 44, 106, -104, -91, 4, -20, 55, -76, 67, 110, 60, 48, 63, 26, 21, 19, -115, 38, 60, 32, 110, -4, 21, 96, 72, 73, -47, -118, -45, -61, -53, -50, 11, -29, -25, -83, -67, -62, 100, -81, -54, -56, 85, -99, -89, -84, 56, 17, -118, 26, -35, -35, -77, 110, -103, 29, -20, -39, 71, -52, -90, 23, 0, 60, 64, 75, -50, -70, 20, -71, -110, 33, 15, 96, -77, 66, 109, 53, 83, 48, 2, -45, -67, -119, 17, 72, -38, 32, -72, 2, 15, -76, -35, -41, 62, -3, -4, -100, -80, 83, -74, -58, -72, -31, -84, -106, -44, 104, -71, -6, 82, -1, -105, 5, 127, 42, 107, -56, 9, 121, -34, 63, 56, -77, -36, 83, 105, -91, -69, -54, -46, -80, -34, 48, 36, 10, 66, 50, 29, -42, -13, -91, -42, 64, -85, -46, 77, 0, 78, -35, 52, 68, 41, 59, 100, -21, 126, 51, 26, -51, 60, 75, -83, 121, 121, 120, -29, 82, -112, 60, 2, 3, 37, 45, 35, -39, -113, 39, -18, -32, -40, -76, 61, 69, 92, -62, 98, 19, -35, 66, -14, -30, 74, -126, -53, -21, 125, -13, 15, -123, -122, -32, -88, 21, -50, -46, 21, 11, -115, 95, -44, -72, 9, -50, 93, 42, 73, -54, -31, -12, -84, 27, -53, 68, 114, 20, 67, 103, 108, 29, -113, 23, 4, -49, 111, 82, 59, -55, -86, -102, 106, -10, 49, -72, -120, -106, 78, -24, -114, -43, -8, 106, 121, -23, 116, 115, -17, -122, -31, 16, -6, -121, 105, -70, 33, 88, 73, 80, -108, -69, -110, -96, 111, 111, -5, -85, 120, 15, -65, -49, -114, 0, 7, 120, -47, -116, 117, -128, -2, -59, -69, -75, -5, 14, -64, 118, -16, -68, -18, 13, -70, -95, 70, 17, 46, -34, 59, 99, -75, -127, -77, -5, -57, 65, -87, -118, 96, -56, 123, 56, -27, -49, 49, 100, -117, 64, -56, -17, 46, 121, -71, -119, -86, 25, -49, -122, -80, -47, 121, 9, -63, -85, -7, -44, -1, 89, -110, -80, 53, 101, -80, -57, -49, 3, 30, 14, -99, -75, -71, -45, -77, 25, 94, 89, 8, -31, 21, -117, 71, -91, 73, 8, -91, 126, -45, 74, 126, 86, 123, -80, -5, -64, -97, 89, -75, 78, -22, -108, -82, -22, -6, 84, -45, -39, -86, 116, -61, 35, -16, 39, -53, -52, -39, 76, 16, -43, -43, 36, 11, 30, 58, 44, -58, 110, -90, -104, -28, 90, 79, -37, 55, -33, 108, 67, 39, -79, 88, 115, -117, 4, 70, -84, -35, -54, -9, 14, -88, -23, 94, -7, -80, -93, 15, 54, -51, 12, -59, -1, -105, -10, -65, 57, 83, 45, -39, 24, 43, -58, -13, 39, -17, -68, 20, 79, -124, 74, -47, -3, 40, -103, -101, 116, -125, -6, -22, -51, 66, -118, 38, 99, 69, -82, -9, -118, 86, 72, 87, 119, -37, -115, -116, -27, 2, 3, 1, 0, 1};

    @BeforeEach
    void initTestDB() throws BalanceExceptions.PublicKeyAlreadyExistException, BalanceExceptions.GeneralMYSQLException, KeyExceptions.GeneralKeyStoreErrorException {
        DBConnection.testDB();
        Balance.openAccount(CREATEDUSER);
        KeyStore.setCredentials("MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAot5+evqqG/Je4aYwokk0XYmNj7WwBsofqnvYTnQZC5RCjxn6/Sl4renY+yXXAMQtxWd4nZlQyH/BQNS4tO5RUFMxCCEnaJl5KymwzczSCYpCntLdxlwqowTToOfvOonQxEBu/yQfDyghsGg5zJhbuDfFBP5XSicaIkxP7/YQ99fRN/OZci39sRyM541d6QnC9o2mQBcVX+id7dZde4dvgEeuFEb3OAFphVzp8ELTH/P78xX/IQRHc7VzQ/bYHamLZJVz8I3iphlwcMQfE4yHXP3COIfbI9ZZeHNNsAgjHSdC+i78kMmRmHzJzP0bgiKFtBEOAr6VCMLyW1yOhe6sMsrRcbxIoncTyE4DJuUieY0vN15sSst9GUkAR1LHx7WbGsw0wyXOq8ves1IbFkrpqi0G/dB/2OTitj9c0dyxjIc/uugi8+kDOBEehQ+y9jELZTAaz4uUf73NfVXITKrQdue5wbNW6OYiVQB69KSsqcfFZCRZ+9IUVqblJI3sKxYemL1JxQFCJ8CBQzy2bGz6ThdeQTfs1CQxDrle1bYZ7VtzrCiNEoDPTMq+UCy5aiY4TgnSypW3vtTwmOXuJVKKBdp4ms6G5IG0hZMJaDiOMmOvoS/2zCkfV8G1t3mDkUTofX+uPJbWOHRtMc00R0OnYNKv2jQlzo2Y58B77IpFAoUCAwEAAQ==" , "MIIJQwIBADANBgkqhkiG9w0BAQEFAASCCS0wggkpAgEAAoICAQCi3n56+qob8l7hpjCiSTRdiY2PtbAGyh+qe9hOdBkLlEKPGfr9KXit6dj7JdcAxC3FZ3idmVDIf8FA1Li07lFQUzEIISdomXkrKbDNzNIJikKe0t3GXCqjBNOg5+86idDEQG7/JB8PKCGwaDnMmFu4N8UE/ldKJxoiTE/v9hD319E385lyLf2xHIznjV3pCcL2jaZAFxVf6J3t1l17h2+AR64URvc4AWmFXOnwQtMf8/vzFf8hBEdztXND9tgdqYtklXPwjeKmGXBwxB8TjIdc/cI4h9sj1ll4c02wCCMdJ0L6LvyQyZGYfMnM/RuCIoW0EQ4CvpUIwvJbXI6F7qwyytFxvEiidxPITgMm5SJ5jS83XmxKy30ZSQBHUsfHtZsazDTDJc6ry96zUhsWSumqLQb90H/Y5OK2P1zR3LGMhz+66CLz6QM4ER6FD7L2MQtlMBrPi5R/vc19VchMqtB257nBs1bo5iJVAHr0pKypx8VkJFn70hRWpuUkjewrFh6YvUnFAUInwIFDPLZsbPpOF15BN+zUJDEOuV7VthntW3OsKI0SgM9Myr5QLLlqJjhOCdLKlbe+1PCY5e4lUooF2niazobkgbSFkwloOI4yY6+hL/bMKR9XwbW3eYORROh9f648ltY4dG0xzTRHQ6dg0q/aNCXOjZjnwHvsikUChQIDAQABAoICAQCZIyS/kzMhRdoKx01ROg2fqXdOWaIiMChoDzxKQAVQit2uWdpR8Y2D0K3xLxoioVL8GpchcAeyDdVylND7Zl1UbnRa1XmNc0in2fJg/yW6TKLpXRGjsgNw9lfaIlogq3Z9IKx8/8SoZq6OhOFbyyHqa523XUJL+cor158N3EsGDoms10I/RpkTz+l4ysYzGLeVRYi9GDdDz5+3Amkxfh3L+xnl/Pf0U/eYqs1X2C1VDP6oCNWzgKeCV2kCzXVVJpZnQ9MYqwAh9ydvECi8I9UfDYZxyuO0pZXImHnCe1Qw1Dq4rDdUGALq4uKH+LDFwpn+vk5zLClLbKmeV7+2lLyZGjV+XVtyzI4eedxynDwoCKmb2mMSsOQ1/bRgiZe5wOC1sadBt+tDxf+LIanB+UFawIXXzEMFE6fiFOFkpoPgP9XOWO9I8qXLL1j9F596stV7LbTgi18MEB1mP7nZtu0BIT79H19fCu5JuJuZ4xoHHZrcmHuJRUEn2uYOl5r5xIfZoJaYVxN0d/BVbnjpJes/jqgpnrNh0SWREIITEictl3GYBkZr3mdsIkutOPef9bNiyaLfZPLk5ZOuTDIV4wF8raYiCOGAX1WNedpjbVmDfe1RhSI/f0jdj5oVmo9VDu5BAE8YtPqC0ow6Y1ZK671s1L3W4WmeAyuqXVXA0w7UQQKCAQEA9/Dqu4QO0yJ3MiJ/qHL7Mdr1AfSmExeE1s/V6LTPD2B+muhqxf5o1nnH4SR30Qat54r+8AhU8aFa2m0N23fz4q4r32NzmFWy+LaJ5jF/ujd4inrFTZSGKKG98RWrGEOeD7LnSkaMEBP4itwbHxUcR2/xdg9CxL87A8kRbTZ+R3kolJyYIHntpAlCql3aN0kdFpW8IgbquWh8zjRy6tH6B5RG20ym+oh5P2b6rzIRlbeUeDFtBfokOW0yUtraWmRWs2v+l99V3wiTzBy00SLwRCGYdcJxWpiF0ttwwziIADb8kXFDeB61rcQx+pGuwFMWhv2Rtngymxfo9iepXdbJNQKCAQEAqCm0iUEK8F4qt8kFW2AQX4jvawZX82X8zB1MZPV7UfiYCWDnaAXSXCpGdsNuAJBEP1VVSuqhCnZMSA2TW1hE9h6pwavloBQMN68j9dFvJIxzmk/6ntmAzONeIA3CxoxU6A/3aTUPplhj7wc2BXt1qAq5wNuDleDf+rXWG+NfR4IphxOcgCanpj0clwW+8gCeMbxu+Nq/JuT14dR6bot9dsPREXNxsL8FH9mrLUlbqH0zLEwES8kUQFZ6uEfkR6DPbo9p5z+rpYBKXliD7aLmMkeU2aVsU/HQVwWxe3u5OVAnOMKIpIext3aKNnaVKhYoi19phqD+m4Yi4C5S0NXOEQKCAQAMrXXjJqp3Y0DDAH6Uh2SOtQpoEjFXspEpRrFCV9Rqjd2LYCzteMe1h76PpYS23GeR+kp0zYhCXqS9nGvj9+sxgQRfRL+JX4BqNTMx4QAN7n4sXoCuKdT+SN7kb94yvGpdlSE8bAUHYW4I854zZanvgSrQhZE9bCXBWMuaUPn9/5TynFsu9e0PwSV2fpwEzak+GWPoPYBNAKm5BXeGyDvHnW5OEkpLbyT79/EDekTp6dwelTBk6NhnU00KbYRSsOS6AeCftVy3rcGY4zLfqrCsWydnhgyzvC7DjFLkuIRDtowNh8QSZUeW3fxMOpy72sPBHUARG25C/eL8iySe1retAoIBAGivPcJMCWFFXdZ3f6w4izjN6C16D3poPQ1cF9ipsRGAbjU4bBYnuaLCdocsdehH5xR+LNCsa4A10JrySI+Oza9hu0/jkNksYcawcvRuYoAgJz8jFuOYh0QFLu1JY71yUtD8T4rhC1MEp4F/vbN//eiUdLqIbMv657QNSrKlwwSjFPMP1Fc6uRcV6Bxj6VCB7fkMUxD8LA8sbZ0eRvFWSM6MbDQu59GduLTxLkzM5BBpkFe9IteC3+aFb3QU3RlyFG/BO65HxrUKvkEpXUesJx2ZziIHhv5jBM2Hr8stZEs3iJMfFy1Fg+6wDq6E96rxjF/nD6xNwPJUeJWSgN+cmeECggEBAN+n1tD6Pusjmm/5TvZsfhXwLhVqb5qt9QMbQNr8dmpw8DzuNzgjoJv5Gbt3ZiLKN+a8wOzkyynETdQnyVFFfYfjaCjTxmPel805twN6OTW3Jla2INsP/tamL4VI54EDlPkQNGgVVdu8dR1jXKKFouH9ZiZY827jKEmPfCCqouvO5taRvuIbjcPJUpXtOn5/86jKQYcRo86DYWtPg1S9Xuf97ZkFAHT4fAYgsSGFAOe5tBf0PkrwK89al9Mq7fgpJxQnid9LjsKmtd8cxyWdhFFBdKud6iN/0k0MDqPUrI77LPtfuRf9fOUIi5K9CtQ0agYHew5/5z14+1nJcNe7vqY=");
    }

    @AfterEach
    void cleanTestDB() throws SQLException {
        DBConnection.testDB();
        Connection conn = DBConnection.getConnection();
        String transactionsQuery = "DELETE from TRANSACTIONS";
        Statement transactionsST = conn.createStatement();
        transactionsST.executeUpdate(transactionsQuery);
        String nonceQuery = "DELETE from NONCE";
        Statement nonceST = conn.createStatement();
        nonceST.executeUpdate(nonceQuery);
        String balanceQuery = "DELETE from BALANCE";
        Statement balanceST = conn.createStatement();
        balanceST.executeUpdate(balanceQuery);
    }

    @Test
    void openAccount(){
        //PARAMS
        byte[] signature = {12, -95, 9, 4, -61, 61, -92, -66, -100, -87, 89, -95, 6, 3, -30, -102, -1, 106, 27, -1, -11, 36, -103, 41, -82, -75, 66, 83, 12, -29, 22, -36, 58, 83, -8, -68, -82, 118, -112, 46, 14, 68, -61, -88, 47, -54, -72, 46, -57, 38, -29, 9, -20, -104, 120, 54, -80, 113, 56, -41, 29, 74, -109, -95, 114, -71, 44, -95, -92, 77, 8, -1, -15, 23, -99, -69, -18, -25, 5, 78, 63, 14, 104, 116, 63, -51, -49, -41, -94, 121, -76, 120, 2, -58, -124, -113, -102, -14, -118, -71, 89, 80, -90, 98, 11, -46, -106, -108, -23, 4, 80, -72, -40, 68, -45, 26, -96, 9, -57, 126, 117, -55, 34, -47, 18, -29, -36, 79, -74, -4, 24, -3, 75, -85, 18, -93, -42, -126, 106, 123, -25, -66, 42, -106, 17, -69, 124, -58, -115, -32, -10, -70, 110, -72, 49, -33, 96, 108, 125, -31, 16, 94, -37, -16, -110, 36, 111, 106, -51, 76, 56, -62, -23, 0, 76, 84, -125, -50, -40, -93, -118, 35, -80, 32, -85, -58, 41, 70, -113, 22, -58, 103, 0, 88, -37, 67, -85, -72, -5, -41, -127, -64, -86, 89, 20, 26, -100, -98, -70, 51, 109, 61, 62, 81, -51, 77, -102, -115, 44, -13, -89, 108, 47, -3, 8, -114, 118, -76, -5, 81, 97, 111, 36, -51, -15, 55, 17, -118, 81, 48, 99, -2, 12, -59, -57, 79, -63, -28, -108, 45, -36, 59, -16, 55, -105, 4, 21, 21, -121, -12, -80, -65, -96, 115, -16, -88, -116, 1, 85, -70, -36, -98, -89, 44, 58, -20, -5, -94, -92, -110, -103, -94, 56, -98, 103, 118, -88, 62, 124, -68, 23, -70, -93, 39, 65, -26, -77, 43, -86, 44, -63, -45, -124, 105, -45, 30, -92, -93, -117, -90, 18, 75, -53, 32, 61, 84, -33, 83, 50, -25, -75, -116, 42, 41, -107, 96, -14, 78, 104, -119, 116, -48, -37, -9, 54, -68, 44, -77, -72, 106, 29, -4, 69, -23, 125, -95, 105, 6, 10, 39, 25, -96, -102, -107, 125, -22, 79, -16, 20, 91, 123, 81, -4, -108, -115, 121, 76, -35, 114, 27, -110, 22, -36, 72, 68, -41, 20, -74, 91, -2, 47, 58, -74, -83, -19, -58, 0, -49, -1, -20, 88, -116, 31, 10, -115, 67, -72, -2, 63, -101, 8, -56, -68, -52, 111, -42, 127, -94, -111, 118, -86, 126, 79, 59, -1, 4, -34, -105, 62, -54, -106, -1, -29, 107, 33, 84, 46, -127, 89, -54, -63, -72, -110, 35, -42, 12, -71, -39, 10, 60, -120, 99, -99, -27, 15, -22, -119, 89, -12, -6, 32, -100, -93, -107, 42, 104, -87, -128, 80, -88, -93, 96, 60, -53, -89, -49, -63, 87, -92, 35, 93, -68, 46, 2, 39, -86, -46, -125, -117, 104, 13, 93, 118, -45, -113, 116, 51, 39, -7, 86, -103, 108, 35, -9, 28, 70, -71, 22, -128, -41, 49, 110, -10, -64, -96, 3, 19, -88, -29, 84, -14, 124};

        ServiceImpl serviceImpl = new ServiceImpl();
        OpenAccountRequest oar = OpenAccountRequest.newBuilder().setSignature(ByteString.copyFrom(signature)).setPublicKey(ByteString.copyFrom(NOTCREATEDUSER)).build();
        StreamObserver<OpenAccountResponse> o = mock(StreamObserver.class);
        serviceImpl.openAccount(oar , o);
        verify(o,times(1)).onCompleted();
        ArgumentCaptor<OpenAccountResponse> c = ArgumentCaptor.forClass(OpenAccountResponse.class);
        verify(o,times(1)).onNext(c.capture());
        OpenAccountResponse response = c.getValue();
        assertNotNull(response);
    }

    @Test
    void openAccountUserAlreadyExists(){
        //PARAMS
        byte[] signature = {12, -95, 9, 4, -61, 61, -92, -66, -100, -87, 89, -95, 6, 3, -30, -102, -1, 106, 27, -1, -11, 36, -103, 41, -82, -75, 66, 83, 12, -29, 22, -36, 58, 83, -8, -68, -82, 118, -112, 46, 14, 68, -61, -88, 47, -54, -72, 46, -57, 38, -29, 9, -20, -104, 120, 54, -80, 113, 56, -41, 29, 74, -109, -95, 114, -71, 44, -95, -92, 77, 8, -1, -15, 23, -99, -69, -18, -25, 5, 78, 63, 14, 104, 116, 63, -51, -49, -41, -94, 121, -76, 120, 2, -58, -124, -113, -102, -14, -118, -71, 89, 80, -90, 98, 11, -46, -106, -108, -23, 4, 80, -72, -40, 68, -45, 26, -96, 9, -57, 126, 117, -55, 34, -47, 18, -29, -36, 79, -74, -4, 24, -3, 75, -85, 18, -93, -42, -126, 106, 123, -25, -66, 42, -106, 17, -69, 124, -58, -115, -32, -10, -70, 110, -72, 49, -33, 96, 108, 125, -31, 16, 94, -37, -16, -110, 36, 111, 106, -51, 76, 56, -62, -23, 0, 76, 84, -125, -50, -40, -93, -118, 35, -80, 32, -85, -58, 41, 70, -113, 22, -58, 103, 0, 88, -37, 67, -85, -72, -5, -41, -127, -64, -86, 89, 20, 26, -100, -98, -70, 51, 109, 61, 62, 81, -51, 77, -102, -115, 44, -13, -89, 108, 47, -3, 8, -114, 118, -76, -5, 81, 97, 111, 36, -51, -15, 55, 17, -118, 81, 48, 99, -2, 12, -59, -57, 79, -63, -28, -108, 45, -36, 59, -16, 55, -105, 4, 21, 21, -121, -12, -80, -65, -96, 115, -16, -88, -116, 1, 85, -70, -36, -98, -89, 44, 58, -20, -5, -94, -92, -110, -103, -94, 56, -98, 103, 118, -88, 62, 124, -68, 23, -70, -93, 39, 65, -26, -77, 43, -86, 44, -63, -45, -124, 105, -45, 30, -92, -93, -117, -90, 18, 75, -53, 32, 61, 84, -33, 83, 50, -25, -75, -116, 42, 41, -107, 96, -14, 78, 104, -119, 116, -48, -37, -9, 54, -68, 44, -77, -72, 106, 29, -4, 69, -23, 125, -95, 105, 6, 10, 39, 25, -96, -102, -107, 125, -22, 79, -16, 20, 91, 123, 81, -4, -108, -115, 121, 76, -35, 114, 27, -110, 22, -36, 72, 68, -41, 20, -74, 91, -2, 47, 58, -74, -83, -19, -58, 0, -49, -1, -20, 88, -116, 31, 10, -115, 67, -72, -2, 63, -101, 8, -56, -68, -52, 111, -42, 127, -94, -111, 118, -86, 126, 79, 59, -1, 4, -34, -105, 62, -54, -106, -1, -29, 107, 33, 84, 46, -127, 89, -54, -63, -72, -110, 35, -42, 12, -71, -39, 10, 60, -120, 99, -99, -27, 15, -22, -119, 89, -12, -6, 32, -100, -93, -107, 42, 104, -87, -128, 80, -88, -93, 96, 60, -53, -89, -49, -63, 87, -92, 35, 93, -68, 46, 2, 39, -86, -46, -125, -117, 104, 13, 93, 118, -45, -113, 116, 51, 39, -7, 86, -103, 108, 35, -9, 28, 70, -71, 22, -128, -41, 49, 110, -10, -64, -96, 3, 19, -88, -29, 84, -14, 124};

        ServiceImpl serviceImpl = new ServiceImpl();
        OpenAccountRequest oar = OpenAccountRequest.newBuilder().setSignature(ByteString.copyFrom(signature)).setPublicKey(ByteString.copyFrom(CREATEDUSER)).build();
        StreamObserver<OpenAccountResponse> o = mock(StreamObserver.class);
        serviceImpl.openAccount(oar , o);
        ArgumentCaptor<Throwable> c = ArgumentCaptor.forClass(Throwable.class);
        verify(o,times(1)).onError(c.capture());
        Throwable response = c.getValue();
        assertInstanceOf(Throwable.class , response);
    }

    @Test
    void openAccountNoParams() {
        //NO PARAMS

        ServiceImpl serviceImpl = new ServiceImpl();
        OpenAccountRequest oar = OpenAccountRequest.newBuilder().build();
        StreamObserver<OpenAccountResponse> o = mock(StreamObserver.class);
        serviceImpl.openAccount(oar , o);
        ArgumentCaptor<Throwable> c = ArgumentCaptor.forClass(Throwable.class);
        verify(o,times(1)).onError(c.capture());
        Throwable response = c.getValue();
        assertInstanceOf(Throwable.class , response);
    }

    @Test
    void sendAmount(){
        //PARAMS
        byte[] signature = {12, -95, 9, 4, -61, 61, -92, -66, -100, -87, 89, -95, 6, 3, -30, -102, -1, 106, 27, -1, -11, 36, -103, 41, -82, -75, 66, 83, 12, -29, 22, -36, 58, 83, -8, -68, -82, 118, -112, 46, 14, 68, -61, -88, 47, -54, -72, 46, -57, 38, -29, 9, -20, -104, 120, 54, -80, 113, 56, -41, 29, 74, -109, -95, 114, -71, 44, -95, -92, 77, 8, -1, -15, 23, -99, -69, -18, -25, 5, 78, 63, 14, 104, 116, 63, -51, -49, -41, -94, 121, -76, 120, 2, -58, -124, -113, -102, -14, -118, -71, 89, 80, -90, 98, 11, -46, -106, -108, -23, 4, 80, -72, -40, 68, -45, 26, -96, 9, -57, 126, 117, -55, 34, -47, 18, -29, -36, 79, -74, -4, 24, -3, 75, -85, 18, -93, -42, -126, 106, 123, -25, -66, 42, -106, 17, -69, 124, -58, -115, -32, -10, -70, 110, -72, 49, -33, 96, 108, 125, -31, 16, 94, -37, -16, -110, 36, 111, 106, -51, 76, 56, -62, -23, 0, 76, 84, -125, -50, -40, -93, -118, 35, -80, 32, -85, -58, 41, 70, -113, 22, -58, 103, 0, 88, -37, 67, -85, -72, -5, -41, -127, -64, -86, 89, 20, 26, -100, -98, -70, 51, 109, 61, 62, 81, -51, 77, -102, -115, 44, -13, -89, 108, 47, -3, 8, -114, 118, -76, -5, 81, 97, 111, 36, -51, -15, 55, 17, -118, 81, 48, 99, -2, 12, -59, -57, 79, -63, -28, -108, 45, -36, 59, -16, 55, -105, 4, 21, 21, -121, -12, -80, -65, -96, 115, -16, -88, -116, 1, 85, -70, -36, -98, -89, 44, 58, -20, -5, -94, -92, -110, -103, -94, 56, -98, 103, 118, -88, 62, 124, -68, 23, -70, -93, 39, 65, -26, -77, 43, -86, 44, -63, -45, -124, 105, -45, 30, -92, -93, -117, -90, 18, 75, -53, 32, 61, 84, -33, 83, 50, -25, -75, -116, 42, 41, -107, 96, -14, 78, 104, -119, 116, -48, -37, -9, 54, -68, 44, -77, -72, 106, 29, -4, 69, -23, 125, -95, 105, 6, 10, 39, 25, -96, -102, -107, 125, -22, 79, -16, 20, 91, 123, 81, -4, -108, -115, 121, 76, -35, 114, 27, -110, 22, -36, 72, 68, -41, 20, -74, 91, -2, 47, 58, -74, -83, -19, -58, 0, -49, -1, -20, 88, -116, 31, 10, -115, 67, -72, -2, 63, -101, 8, -56, -68, -52, 111, -42, 127, -94, -111, 118, -86, 126, 79, 59, -1, 4, -34, -105, 62, -54, -106, -1, -29, 107, 33, 84, 46, -127, 89, -54, -63, -72, -110, 35, -42, 12, -71, -39, 10, 60, -120, 99, -99, -27, 15, -22, -119, 89, -12, -6, 32, -100, -93, -107, 42, 104, -87, -128, 80, -88, -93, 96, 60, -53, -89, -49, -63, 87, -92, 35, 93, -68, 46, 2, 39, -86, -46, -125, -117, 104, 13, 93, 118, -45, -113, 116, 51, 39, -7, 86, -103, 108, 35, -9, 28, 70, -71, 22, -128, -41, 49, 110, -10, -64, -96, 3, 19, -88, -29, 84, -14, 124};

        ServiceImpl serviceImpl = new ServiceImpl();
        SendAmountRequest oar = SendAmountRequest.newBuilder().setSignature(ByteString.copyFrom(signature)).build();
        StreamObserver<SendAmountResponse> o = mock(StreamObserver.class);
        serviceImpl.sendAmount(oar , o);
        verify(o,times(1)).onCompleted();
        ArgumentCaptor<SendAmountResponse> c = ArgumentCaptor.forClass(SendAmountResponse.class);
        verify(o,times(1)).onNext(c.capture());
        SendAmountResponse response = c.getValue();
        assertNotNull(response);
    }

    @Test
    void receiveAmount(){
        //PARAMS
        byte[] signature = {12, -95, 9, 4, -61, 61, -92, -66, -100, -87, 89, -95, 6, 3, -30, -102, -1, 106, 27, -1, -11, 36, -103, 41, -82, -75, 66, 83, 12, -29, 22, -36, 58, 83, -8, -68, -82, 118, -112, 46, 14, 68, -61, -88, 47, -54, -72, 46, -57, 38, -29, 9, -20, -104, 120, 54, -80, 113, 56, -41, 29, 74, -109, -95, 114, -71, 44, -95, -92, 77, 8, -1, -15, 23, -99, -69, -18, -25, 5, 78, 63, 14, 104, 116, 63, -51, -49, -41, -94, 121, -76, 120, 2, -58, -124, -113, -102, -14, -118, -71, 89, 80, -90, 98, 11, -46, -106, -108, -23, 4, 80, -72, -40, 68, -45, 26, -96, 9, -57, 126, 117, -55, 34, -47, 18, -29, -36, 79, -74, -4, 24, -3, 75, -85, 18, -93, -42, -126, 106, 123, -25, -66, 42, -106, 17, -69, 124, -58, -115, -32, -10, -70, 110, -72, 49, -33, 96, 108, 125, -31, 16, 94, -37, -16, -110, 36, 111, 106, -51, 76, 56, -62, -23, 0, 76, 84, -125, -50, -40, -93, -118, 35, -80, 32, -85, -58, 41, 70, -113, 22, -58, 103, 0, 88, -37, 67, -85, -72, -5, -41, -127, -64, -86, 89, 20, 26, -100, -98, -70, 51, 109, 61, 62, 81, -51, 77, -102, -115, 44, -13, -89, 108, 47, -3, 8, -114, 118, -76, -5, 81, 97, 111, 36, -51, -15, 55, 17, -118, 81, 48, 99, -2, 12, -59, -57, 79, -63, -28, -108, 45, -36, 59, -16, 55, -105, 4, 21, 21, -121, -12, -80, -65, -96, 115, -16, -88, -116, 1, 85, -70, -36, -98, -89, 44, 58, -20, -5, -94, -92, -110, -103, -94, 56, -98, 103, 118, -88, 62, 124, -68, 23, -70, -93, 39, 65, -26, -77, 43, -86, 44, -63, -45, -124, 105, -45, 30, -92, -93, -117, -90, 18, 75, -53, 32, 61, 84, -33, 83, 50, -25, -75, -116, 42, 41, -107, 96, -14, 78, 104, -119, 116, -48, -37, -9, 54, -68, 44, -77, -72, 106, 29, -4, 69, -23, 125, -95, 105, 6, 10, 39, 25, -96, -102, -107, 125, -22, 79, -16, 20, 91, 123, 81, -4, -108, -115, 121, 76, -35, 114, 27, -110, 22, -36, 72, 68, -41, 20, -74, 91, -2, 47, 58, -74, -83, -19, -58, 0, -49, -1, -20, 88, -116, 31, 10, -115, 67, -72, -2, 63, -101, 8, -56, -68, -52, 111, -42, 127, -94, -111, 118, -86, 126, 79, 59, -1, 4, -34, -105, 62, -54, -106, -1, -29, 107, 33, 84, 46, -127, 89, -54, -63, -72, -110, 35, -42, 12, -71, -39, 10, 60, -120, 99, -99, -27, 15, -22, -119, 89, -12, -6, 32, -100, -93, -107, 42, 104, -87, -128, 80, -88, -93, 96, 60, -53, -89, -49, -63, 87, -92, 35, 93, -68, 46, 2, 39, -86, -46, -125, -117, 104, 13, 93, 118, -45, -113, 116, 51, 39, -7, 86, -103, 108, 35, -9, 28, 70, -71, 22, -128, -41, 49, 110, -10, -64, -96, 3, 19, -88, -29, 84, -14, 124};

        ServiceImpl serviceImpl = new ServiceImpl();
        ReceiveAmountRequest oar = ReceiveAmountRequest.newBuilder().setSignature(ByteString.copyFrom(signature)).build();
        StreamObserver<ReceiveAmountResponse> o = mock(StreamObserver.class);
        serviceImpl.receiveAmount(oar , o);
        verify(o,times(1)).onCompleted();
        ArgumentCaptor<ReceiveAmountResponse> c = ArgumentCaptor.forClass(ReceiveAmountResponse.class);
        verify(o,times(1)).onNext(c.capture());
        ReceiveAmountResponse response = c.getValue();
        assertNotNull(response);
    }

    @Test
    void checkAccount(){
        //PARAMS
        byte[] signature = {12, -95, 9, 4, -61, 61, -92, -66, -100, -87, 89, -95, 6, 3, -30, -102, -1, 106, 27, -1, -11, 36, -103, 41, -82, -75, 66, 83, 12, -29, 22, -36, 58, 83, -8, -68, -82, 118, -112, 46, 14, 68, -61, -88, 47, -54, -72, 46, -57, 38, -29, 9, -20, -104, 120, 54, -80, 113, 56, -41, 29, 74, -109, -95, 114, -71, 44, -95, -92, 77, 8, -1, -15, 23, -99, -69, -18, -25, 5, 78, 63, 14, 104, 116, 63, -51, -49, -41, -94, 121, -76, 120, 2, -58, -124, -113, -102, -14, -118, -71, 89, 80, -90, 98, 11, -46, -106, -108, -23, 4, 80, -72, -40, 68, -45, 26, -96, 9, -57, 126, 117, -55, 34, -47, 18, -29, -36, 79, -74, -4, 24, -3, 75, -85, 18, -93, -42, -126, 106, 123, -25, -66, 42, -106, 17, -69, 124, -58, -115, -32, -10, -70, 110, -72, 49, -33, 96, 108, 125, -31, 16, 94, -37, -16, -110, 36, 111, 106, -51, 76, 56, -62, -23, 0, 76, 84, -125, -50, -40, -93, -118, 35, -80, 32, -85, -58, 41, 70, -113, 22, -58, 103, 0, 88, -37, 67, -85, -72, -5, -41, -127, -64, -86, 89, 20, 26, -100, -98, -70, 51, 109, 61, 62, 81, -51, 77, -102, -115, 44, -13, -89, 108, 47, -3, 8, -114, 118, -76, -5, 81, 97, 111, 36, -51, -15, 55, 17, -118, 81, 48, 99, -2, 12, -59, -57, 79, -63, -28, -108, 45, -36, 59, -16, 55, -105, 4, 21, 21, -121, -12, -80, -65, -96, 115, -16, -88, -116, 1, 85, -70, -36, -98, -89, 44, 58, -20, -5, -94, -92, -110, -103, -94, 56, -98, 103, 118, -88, 62, 124, -68, 23, -70, -93, 39, 65, -26, -77, 43, -86, 44, -63, -45, -124, 105, -45, 30, -92, -93, -117, -90, 18, 75, -53, 32, 61, 84, -33, 83, 50, -25, -75, -116, 42, 41, -107, 96, -14, 78, 104, -119, 116, -48, -37, -9, 54, -68, 44, -77, -72, 106, 29, -4, 69, -23, 125, -95, 105, 6, 10, 39, 25, -96, -102, -107, 125, -22, 79, -16, 20, 91, 123, 81, -4, -108, -115, 121, 76, -35, 114, 27, -110, 22, -36, 72, 68, -41, 20, -74, 91, -2, 47, 58, -74, -83, -19, -58, 0, -49, -1, -20, 88, -116, 31, 10, -115, 67, -72, -2, 63, -101, 8, -56, -68, -52, 111, -42, 127, -94, -111, 118, -86, 126, 79, 59, -1, 4, -34, -105, 62, -54, -106, -1, -29, 107, 33, 84, 46, -127, 89, -54, -63, -72, -110, 35, -42, 12, -71, -39, 10, 60, -120, 99, -99, -27, 15, -22, -119, 89, -12, -6, 32, -100, -93, -107, 42, 104, -87, -128, 80, -88, -93, 96, 60, -53, -89, -49, -63, 87, -92, 35, 93, -68, 46, 2, 39, -86, -46, -125, -117, 104, 13, 93, 118, -45, -113, 116, 51, 39, -7, 86, -103, 108, 35, -9, 28, 70, -71, 22, -128, -41, 49, 110, -10, -64, -96, 3, 19, -88, -29, 84, -14, 124};

        ServiceImpl serviceImpl = new ServiceImpl();
        CheckAccountRequest oar = CheckAccountRequest.newBuilder().build();
        StreamObserver<CheckAccountResponse> o = mock(StreamObserver.class);
        serviceImpl.checkAccount(oar , o);
        verify(o,times(1)).onCompleted();
        ArgumentCaptor<CheckAccountResponse> c = ArgumentCaptor.forClass(CheckAccountResponse.class);
        verify(o,times(1)).onNext(c.capture());
        CheckAccountResponse response = c.getValue();
        assertNotNull(response);
    }

    @Test
    void audit(){
        //PARAMS
        byte[] signature = {12, -95, 9, 4, -61, 61, -92, -66, -100, -87, 89, -95, 6, 3, -30, -102, -1, 106, 27, -1, -11, 36, -103, 41, -82, -75, 66, 83, 12, -29, 22, -36, 58, 83, -8, -68, -82, 118, -112, 46, 14, 68, -61, -88, 47, -54, -72, 46, -57, 38, -29, 9, -20, -104, 120, 54, -80, 113, 56, -41, 29, 74, -109, -95, 114, -71, 44, -95, -92, 77, 8, -1, -15, 23, -99, -69, -18, -25, 5, 78, 63, 14, 104, 116, 63, -51, -49, -41, -94, 121, -76, 120, 2, -58, -124, -113, -102, -14, -118, -71, 89, 80, -90, 98, 11, -46, -106, -108, -23, 4, 80, -72, -40, 68, -45, 26, -96, 9, -57, 126, 117, -55, 34, -47, 18, -29, -36, 79, -74, -4, 24, -3, 75, -85, 18, -93, -42, -126, 106, 123, -25, -66, 42, -106, 17, -69, 124, -58, -115, -32, -10, -70, 110, -72, 49, -33, 96, 108, 125, -31, 16, 94, -37, -16, -110, 36, 111, 106, -51, 76, 56, -62, -23, 0, 76, 84, -125, -50, -40, -93, -118, 35, -80, 32, -85, -58, 41, 70, -113, 22, -58, 103, 0, 88, -37, 67, -85, -72, -5, -41, -127, -64, -86, 89, 20, 26, -100, -98, -70, 51, 109, 61, 62, 81, -51, 77, -102, -115, 44, -13, -89, 108, 47, -3, 8, -114, 118, -76, -5, 81, 97, 111, 36, -51, -15, 55, 17, -118, 81, 48, 99, -2, 12, -59, -57, 79, -63, -28, -108, 45, -36, 59, -16, 55, -105, 4, 21, 21, -121, -12, -80, -65, -96, 115, -16, -88, -116, 1, 85, -70, -36, -98, -89, 44, 58, -20, -5, -94, -92, -110, -103, -94, 56, -98, 103, 118, -88, 62, 124, -68, 23, -70, -93, 39, 65, -26, -77, 43, -86, 44, -63, -45, -124, 105, -45, 30, -92, -93, -117, -90, 18, 75, -53, 32, 61, 84, -33, 83, 50, -25, -75, -116, 42, 41, -107, 96, -14, 78, 104, -119, 116, -48, -37, -9, 54, -68, 44, -77, -72, 106, 29, -4, 69, -23, 125, -95, 105, 6, 10, 39, 25, -96, -102, -107, 125, -22, 79, -16, 20, 91, 123, 81, -4, -108, -115, 121, 76, -35, 114, 27, -110, 22, -36, 72, 68, -41, 20, -74, 91, -2, 47, 58, -74, -83, -19, -58, 0, -49, -1, -20, 88, -116, 31, 10, -115, 67, -72, -2, 63, -101, 8, -56, -68, -52, 111, -42, 127, -94, -111, 118, -86, 126, 79, 59, -1, 4, -34, -105, 62, -54, -106, -1, -29, 107, 33, 84, 46, -127, 89, -54, -63, -72, -110, 35, -42, 12, -71, -39, 10, 60, -120, 99, -99, -27, 15, -22, -119, 89, -12, -6, 32, -100, -93, -107, 42, 104, -87, -128, 80, -88, -93, 96, 60, -53, -89, -49, -63, 87, -92, 35, 93, -68, 46, 2, 39, -86, -46, -125, -117, 104, 13, 93, 118, -45, -113, 116, 51, 39, -7, 86, -103, 108, 35, -9, 28, 70, -71, 22, -128, -41, 49, 110, -10, -64, -96, 3, 19, -88, -29, 84, -14, 124};

        ServiceImpl serviceImpl = new ServiceImpl();
        AuditRequest oar = AuditRequest.newBuilder().build();
        StreamObserver<AuditResponse> o = mock(StreamObserver.class);
        serviceImpl.audit(oar , o);
        verify(o,times(1)).onCompleted();
        ArgumentCaptor<AuditResponse> c = ArgumentCaptor.forClass(AuditResponse.class);
        verify(o,times(1)).onNext(c.capture());
        AuditResponse response = c.getValue();
        assertNotNull(response);
    }

}
