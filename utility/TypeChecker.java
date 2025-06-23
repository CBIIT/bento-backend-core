package gov.nih.nci.bento.utility;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import com.google.common.reflect.TypeToken;

public class TypeChecker {
    /**
     * Checks if the given object is of the specified type.
     * This method uses Guava's TypeToken to handle generic types and collections.
     * @param obj The object to check
     * @param typeToken<T> A TypeToken representing the expected type
     * @return
     */
    public static <T> boolean isOfType(Object obj, TypeToken<T> typeToken) {
        if (obj == null) {
            return false;
        }

        return matches(obj, typeToken.getType());
    }

    /**
     * Recursively checks if the object matches the expected type.
     * Helper method for isOfType.
     * @param obj The object to check
     * @param type A Type representing the expected type
     * @return true if the object matches the type, false otherwise
     */
    private static boolean matches(Object obj, Type type) {
        TypeToken<?> expectedType = TypeToken.of(type);

        if (expectedType.getRawType().isAssignableFrom(obj.getClass())) {
            // Check for known container types with generic params
            if (expectedType.isSubtypeOf(new TypeToken<List<?>>() {})) {
                List<?> list = (List<?>) obj;
                TypeToken<?> itemType = expectedType.resolveType(List.class.getTypeParameters()[0]);

                for (Object item : list) {
                    if (!matches(item, itemType.getType())) {
                        return false;
                    }
                }

                return true;
            } else if (expectedType.isSubtypeOf(new TypeToken<Map<?, ?>>() {})) {
                Map<?, ?> map = (Map<?, ?>) obj;
                TypeToken<?> keyType = expectedType.resolveType(Map.class.getTypeParameters()[0]);
                TypeToken<?> valueType = expectedType.resolveType(Map.class.getTypeParameters()[1]);

                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!matches(entry.getKey(), keyType.getType()) || !matches(entry.getValue(), valueType.getType())) {
                        return false;
                    }
                }

                return true;
            } else {
                // Not a container with generic types, just check base class
                return true;
            }
        }

        return false;
    }
}
