package gov.nih.nci.bento.utility;

import java.util.List;
import java.util.Map;

public class TypeChecker {
    /**
     * A generalized type checking method that can verify various collection types and their nested elements.
     *
     * @param obj The object to check
     * @param containerType The expected container type (e.g., List.class, Map.class)
     * @param elementTypes The expected element types in order:
     *                     - For List<E>: pass the element type
     *                     - For Map<K,V>: pass key type then value type, or Object.class for any type
     * @return true if the object matches the expected types, false otherwise
     */
    public static boolean isOfType(Object obj, Class<?> containerType, Class<?>... elementTypes) {
        if (obj == null || !containerType.isInstance(obj)) {
            return false;
        }

        if (List.class.isAssignableFrom(containerType)) {
            if (elementTypes.length != 1) {
                throw new IllegalArgumentException("List requires exactly one element type");
            }
            List<?> list = (List<?>) obj;
            for (Object item : list) {
                if (Map.class.isAssignableFrom(elementTypes[0])) {
                    // Handle nested Map type
                    if (!Map.class.isInstance(item)) {
                        return false;
                    }
                    Map<?, ?> mapItem = (Map<?, ?>) item;
                    for (Map.Entry<?, ?> entry : mapItem.entrySet()) {
                        if (!(entry.getKey() instanceof String)) {
                            return false;
                        }
                    }
                } else if (!elementTypes[0].isInstance(item)) {
                    return false;
                }
            }
            return true;
        }

        if (Map.class.isAssignableFrom(containerType)) {
            if (elementTypes.length != 2) {
                throw new IllegalArgumentException("Map requires exactly two element types (key and value)");
            }
            Map<?, ?> map = (Map<?, ?>) obj;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!elementTypes[0].isInstance(entry.getKey())) {
                    return false;
                }
                
                Object value = entry.getValue();
                if (List.class.isAssignableFrom(elementTypes[1])) {
                    // Handle Map with List values
                    if (!(value instanceof List<?>)) {
                        return false;
                    }
                    // For Map<String, List<T>>, we assume the third type parameter is the list element type
                    if (elementTypes.length > 2) {
                        for (Object listItem : (List<?>) value) {
                            if (!elementTypes[2].isInstance(listItem)) {
                                return false;
                            }
                        }
                    }
                } else if (!elementTypes[1].isInstance(value)) {
                    return false;
                }
            }
            return true;
        }

        throw new IllegalArgumentException("Unsupported container type: " + containerType.getName());
    }
}
