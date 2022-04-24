package tecnico.sec.client;

import tecnico.sec.grpc.Transaction;

import java.util.*;

public class ReadResponse {

    private final ServerInfo server;
    private final Object response;
    private final boolean isError;
    private final String message;
    private final List<Transaction> transactions;
    private final int balance;


    public ReadResponse(ServerInfo server, Object response, boolean isError, String message, List<Transaction> transactions, int balance) {
        this.server = server;
        this.response = response;
        this.isError = isError;
        this.message = message;
        this.transactions = transactions;
        this.balance = balance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReadResponse response1 = (ReadResponse) o;
        return isError == response1.isError && Objects.equals(message, response1.message) && transactions.equals(response1.transactions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(response.getClass(), isError, message, transactions);
    }

    public static ReadResponse getResult(List<ReadResponse> responseList) {
        Map.Entry<ReadResponse, Integer> max = getMostCommonResponse(responseList);
        return max.getKey();
    }

    public static boolean quorumExists(List<ReadResponse> responseList, int quorum) {
        Map.Entry<ReadResponse, Integer> max = getMostCommonResponse(responseList);
        return max.getValue() >= quorum;
    }

    public static Map.Entry<ReadResponse, Integer> getMostCommonResponse(List<ReadResponse> responseList) {
        Map<ReadResponse, Integer> map = new HashMap<>();

        for(ReadResponse r : responseList) {
            Integer val = map.get(r);
            map.put(r, val == null ? 1 : val + 1);
        }

        System.out.println(map);

        return Collections.max(map.entrySet(), Comparator.comparing(Map.Entry::getValue));
    }

    public static List<ReadResponse> getResponseQuorum(List<ReadResponse> responseList, ReadResponse result) {
        List<ReadResponse> responsesQuorum = new ArrayList<>();
        for(ReadResponse r : responseList) {
            if(r.equals(result)) {
                responsesQuorum.add(r);
            }
        }
        return responsesQuorum;
    }

    public String getMessage() {
        return message;
    }

    public boolean isError() {
        return isError;
    }

    public Object getResponse() {
        return response;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public int getBalance() {
        return balance;
    }

    public ServerInfo getServer() {
        return server;
    }
}
