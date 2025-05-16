package gov.nih.nci.bento.utility;

import java.util.List;
import java.util.Map;

public class TypeChecker {
    /**
     * Checks whether the provided object is a List of Maps with String keys and Object values.
     *
     * @param obj the object to check
     * @return true if obj is a List<Map<String, Object>>, false otherwise
     */
    public static boolean isListOfMapStringObject(Object obj) {
        if (!(obj instanceof List<?> list)) {
            return false;
        }

        for (Object item : list) {
            if (!isMapStringObject(item)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks whether the provided object is a List of Strings.
     *
     * @param obj the object to check
     * @param type the class of the type to check against
     * @param <T> the type of the list elements
     * @return true if obj is a List<T>, false otherwise
     */
    public static <T> boolean isListOfType(Object obj, Class<T> type) {
        if (!(obj instanceof List<?> list)) {
            return false;
        }

        for (Object item : list) {
            if (!type.isInstance(item)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether the provided object is a Map with String keys and Lists of Strings as values.
     *
     * @param obj the object to check
     * @param type the class of the type to check against
     * @param <T> the type of the list elements
     * @return true if obj is a Map<String, List<T>>, false otherwise
     */
    public static <T> boolean isMapStringListOfType(Object obj, Class<T> type) {
        if (!(obj instanceof Map<?, ?> map)) return false;

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String)) {
                return false;
            }

            Object value = entry.getValue();
            if (!(value instanceof List<?> list)) {
                return false;
            }

            for (Object item : list) {
                if (!type.isInstance(item)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Checks whether the provided object is a Map with String keys and Object values.
     *
     * @param obj the object to check
     * @return true if obj is a Map<String, Object>, false otherwise
     */
    public static boolean isMapStringObject(Object obj) {
        if (!(obj instanceof Map<?, ?> map)) {
            return false;
        }

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String)) {
                return false;
            }
            // We accept any value since it's Map<String, Object>
        }

        return true;
    }
}
