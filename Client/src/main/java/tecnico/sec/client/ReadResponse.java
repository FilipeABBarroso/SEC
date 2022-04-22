package tecnico.sec.client;

import tecnico.sec.grpc.Transaction;

import java.util.*;

public class ReadResponse {

    private final Object response;
    private final boolean isError;
    private final String message;
    private final List<Transaction> transactions;


    public ReadResponse(Object response, boolean isError, String message, List<Transaction> transactions) {
        this.response = response;
        this.isError = isError;
        this.message = message;
        this.transactions = transactions;
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
        return Objects.hash(response, isError, message, transactions);
    }

    public static ReadResponse getResult(List<ReadResponse> responseList) {
        Map<ReadResponse, Integer> map = new HashMap<>();

        for(ReadResponse r : responseList) {
            Integer val = map.get(r);
            map.put(r, val == null ? 1 : val + 1);
        }

        System.out.println(map);

        Map.Entry<ReadResponse, Integer> max = Collections.max(map.entrySet(), Comparator.comparing(Map.Entry::getValue));
        return max.getKey();
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
}
