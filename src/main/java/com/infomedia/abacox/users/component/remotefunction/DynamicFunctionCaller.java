package com.infomedia.abacox.users.component.remotefunction;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DynamicFunctionCaller {
    private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public static Object callFunction(Object instance, String functionName, Map<String, Object> arguments) throws Throwable {
        try {
            // Get all methods with matching name from the instance's class
            Method[] methods = instance.getClass().getMethods();
            List<Method> matchingMethods = new ArrayList<>();

            // Find all methods with matching name
            for (Method method : methods) {
                if (method.getName().equals(functionName)) {
                    matchingMethods.add(method);
                }
            }

            if (matchingMethods.isEmpty()) {
                throw new NoSuchMethodException(functionName);
            }

            // Find the best matching method based on parameter count and types
            Method targetMethod = findBestMatchingMethod(matchingMethods, arguments);
            if (targetMethod == null) {
                throw new NoSuchMethodException("No matching method found for: " + functionName);
            }

            Class<?>[] parameterTypes = targetMethod.getParameterTypes();
            Object[] args = new Object[parameterTypes.length];

            // If we have exactly one parameter and one argument, use it directly
            if (parameterTypes.length == 1 && arguments.size() == 1) {
                args[0] = arguments.values().iterator().next();
            } else {
                // Try to get parameter names
                String[] parameterNames = parameterNameDiscoverer.getParameterNames(targetMethod);

                if (parameterNames != null) {
                    // Use parameter names if available
                    for (int i = 0; i < parameterNames.length; i++) {
                        String paramName = parameterNames[i];
                        Object arg = arguments.get(paramName);
                        if (arg == null && !arguments.containsKey(paramName)) {
                            throw new IllegalArgumentException("Missing argument for parameter: " + paramName);
                        }
                        args[i] = convertType(arg, parameterTypes[i]);
                    }
                } else {
                    // Fallback to positional arguments
                    if (arguments.size() != parameterTypes.length) {
                        throw new IllegalArgumentException(
                                String.format("Expected %d arguments but got %d", parameterTypes.length, arguments.size()));
                    }
                    int i = 0;
                    for (Object arg : arguments.values()) {
                        args[i] = convertType(arg, parameterTypes[i]);
                        i++;
                    }
                }
            }

            return targetMethod.invoke(instance, args);

        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Function " + functionName + " not found", e);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Method findBestMatchingMethod(List<Method> methods, Map<String, Object> arguments) {
        if(arguments == null) {
            arguments = Map.of();
        }
        for (Method method : methods) {
            if (method.getParameterCount() == arguments.size()) {
                Class<?>[] paramTypes = method.getParameterTypes();
                boolean matches = true;
                int i = 0;
                for (Object arg : arguments.values()) {
                    if (arg != null && !isAssignableFrom(paramTypes[i], arg.getClass())) {
                        matches = false;
                        break;
                    }
                    i++;
                }
                if (matches) {
                    return method;
                }
            }
        }
        // If no exact match found, return the first method with matching parameter count
        Map<String, Object> finalArguments = arguments;
        return methods.stream()
                .filter(m -> m.getParameterCount() == finalArguments.size())
                .findFirst()
                .orElse(null);
    }

    private static boolean isAssignableFrom(Class<?> targetType, Class<?> sourceType) {
        if (targetType.isPrimitive()) {
            targetType = getPrimitiveWrapper(targetType);
        }
        return targetType.isAssignableFrom(sourceType);
    }

    private static Class<?> getPrimitiveWrapper(Class<?> primitiveType) {
        if (primitiveType == int.class) return Integer.class;
        if (primitiveType == long.class) return Long.class;
        if (primitiveType == double.class) return Double.class;
        if (primitiveType == float.class) return Float.class;
        if (primitiveType == boolean.class) return Boolean.class;
        if (primitiveType == byte.class) return Byte.class;
        if (primitiveType == char.class) return Character.class;
        if (primitiveType == short.class) return Short.class;
        return primitiveType;
    }

    private static Object convertType(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isAssignableFrom(value.getClass())) return value;

        // Add basic type conversion if needed
        if (targetType == String.class) {
            return value.toString();
        }
        // Add more type conversions as needed

        return value;
    }
}