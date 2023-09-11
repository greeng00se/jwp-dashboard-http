package org.apache.coyote.http11.request;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;
import static org.apache.coyote.http11.common.Constants.EMPTY;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RequestBody {

    private static final String SEPARATOR = "&";
    private static final String DELIMITER = "=";
    private static final int KEY_INDEX = 0;
    private static final int VALUE_INDEX = 1;
    private static final int FIELD_COUNT = 2;

    private final Map<String, String> items = new HashMap<>();

    public RequestBody() {
        this(Collections.emptyMap());
    }

    private RequestBody(final Map<String, String> items) {
        this.items.putAll(items);
    }

    public static RequestBody from(final String body) {
        return Arrays.stream(body.split(SEPARATOR))
                .map(field -> field.split(DELIMITER, FIELD_COUNT))
                .collect(collectingAndThen(
                        toMap(field -> field[KEY_INDEX].strip(), field -> field[VALUE_INDEX].strip()),
                        RequestBody::new
                ));
    }

    public String get(final String key) {
        return items.getOrDefault(key, EMPTY);
    }

    public Map<String, String> getItems() {
        return items;
    }
}
