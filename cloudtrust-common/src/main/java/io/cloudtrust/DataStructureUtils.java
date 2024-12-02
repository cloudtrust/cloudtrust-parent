package io.cloudtrust;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DataStructureUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private DataStructureUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Converts a Map to a List of key-value pairs represented as Maps.
     *
     * @param inputMap the input Map
     * @return a List of Maps with "key" and "value" entries
     */
    public static List<Map<String, String>> mapToKeyValueList(Map<String, String> inputMap) {
        List<Map<String, String>> keyValueList = new ArrayList<>();

        for (Map.Entry<String, String> entry : inputMap.entrySet()) {
            Map<String, String> keyValueMap = new HashMap<>();
            keyValueMap.put("key", entry.getKey());
            keyValueMap.put("value", entry.getValue());
            keyValueList.add(keyValueMap);
        }

        return keyValueList;
    }

    /**
     * Converts a List of key-value pair Maps back to a Map.
     *
     * @param keyValueList the List of key-value pair Maps
     * @return the resulting Map
     */
    public static Map<String, String> keyValueListToMap(List<Map<String, String>> keyValueList) {
        Map<String, String> resultMap = new HashMap<>();

        for (Map<String, String> keyValueMap : keyValueList) {
            String key = keyValueMap.get("key");
            String value = keyValueMap.get("value");
            if (key != null && value != null) {
                resultMap.put(key, value);
            }
        }

        return resultMap;
    }

    /**
     * Converts a List of key-value pair Maps to a JSON String.
     *
     * @param listOfMaps the List of key-value pair Maps
     * @return the JSON String
     */
    public static String keyValueListToString(List<Map<String, String>> listOfMaps) {
        try {
            return objectMapper.writeValueAsString(listOfMaps);
        } catch (Exception e) {
            throw new RuntimeException("Error converting list of maps to string", e);
        }
    }

    /**
     * Converts a JSON String to a List of key-value pair Maps.
     *
     * @param jsonString the JSON String
     * @return the List of key-value pair Maps
     */
    public static List<Map<String, String>> stringToKeyValueList(String jsonString) {
        try {
            return objectMapper.readValue(jsonString, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Error converting string to list of maps", e);
        }
    }

    /**
     * Converts a JSON String to a map.
     *
     * @param jsonString the JSON String
     * @return the map
     */
    public static Map<String, String> stringToMap(String jsonString) {
        try {
            return objectMapper.readValue(jsonString, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Error converting string to a map", e);
        }
    }

    /**
     * Converts a map to a JSON String.
     *
     * @param map the map
     * @return the JSON String
     */
    public static String mapToString(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new RuntimeException("Error converting a map to a JSON string", e);
        }
    }

    /**
     * Validates that a given configuration map contains exactly the specified set of keys.
     *
     * <p>This method checks that:
     * <ul>
     *   <li>All keys in the {@code validKeys} set are present in the {@code setting} map.</li>
     *   <li>There are no extra keys in the {@code setting} map that are not part of {@code validKeys}.</li>
     * </ul>
     *
     * <p>If either condition is violated, an {@link IllegalArgumentException} is thrown with a detailed
     * error message indicating the missing or unexpected keys.
     *
     * @param setting   the configuration map to validate
     * @param validKeys the set of valid keys that the configuration map should contain
     * @throws IllegalArgumentException if the configuration map does not match the expected keys
     */

    public static void validateSettingKeys(Map<String, String> setting, Set<String> validKeys) {
        if (setting.size() != validKeys.size()) {
            throw new IllegalArgumentException("The configuration must only contain the following keys: %s.".formatted(validKeys));
        }

        if (!setting.keySet().containsAll(validKeys)) {
            StringBuilder errorMessage = new StringBuilder();

            validKeys.stream()
                    .filter(key -> !setting.containsKey(key))
                    .forEach(missingKey -> errorMessage.append("The configuration misses key: '").append(missingKey).append("'.\n"));

            setting.keySet().stream()
                    .filter(key -> !validKeys.contains(key))
                    .forEach(extraKey -> errorMessage.append("The configuration contains an unexpected key: '").append(extraKey).append("'.\n"));

            throw new IllegalArgumentException(errorMessage.toString());
        }
    }
}
