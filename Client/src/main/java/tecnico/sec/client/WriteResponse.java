package tecnico.sec.client;

import java.util.*;

public class WriteResponse {

    private final Object response;
    private final boolean isError;
    private final String message;

    public WriteResponse(Object response, boolean isError, String message) {
        this.response = response;
        this.isError = isError;
        this.message = message;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WriteResponse response1 = (WriteResponse) o;
        return isError == response1.isError && Objects.equals(message, response1.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(response, isError, message);
    }

    public static WriteResponse getResult(List<WriteResponse> responseList) {
        Map<WriteResponse, Integer> map = new HashMap<>();

        for(WriteResponse r : responseList) {
            Integer val = map.get(r);
            map.put(r, val == null ? 1 : val + 1);
        }

        System.out.println(map);

        Map.Entry<WriteResponse, Integer> max = Collections.max(map.entrySet(), Comparator.comparing(Map.Entry::getValue));
        return max.getKey();
    }
}
