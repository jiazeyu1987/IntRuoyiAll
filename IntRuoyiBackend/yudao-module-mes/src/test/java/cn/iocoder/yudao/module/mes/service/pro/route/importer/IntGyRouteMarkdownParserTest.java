package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class IntGyRouteMarkdownParserTest {

    private static final String PARSER_CLASS_NAME =
            "cn.iocoder.yudao.module.mes.service.pro.route.importer.IntGyRouteMarkdownParser";
    private static final Path CURRENT_EXPORT_FIXTURE = Path.of(
            "D:\\ProjectPackage\\Int\\IntGY\\doc\\exports\\current-two-imported-process-routes-20260512.md");

    @Test
    void parseCurrentExport_returnsTwoRoutesWithExpectedStepCountsAndFinalCodes() throws Exception {
        assertTrue(Files.isRegularFile(CURRENT_EXPORT_FIXTURE),
                "Missing real IntGY export fixture: " + CURRENT_EXPORT_FIXTURE);

        List<Object> routes = extractRoutes(parseMarkdown(CURRENT_EXPORT_FIXTURE));

        assertEquals(2, routes.size());
        RouteView firstRoute = routeView(routes, "ROUTE-YXN.044.02.1020");
        assertEquals(30, firstRoute.steps().size());
        assertEquals("W030", processCode(finalStep(firstRoute.steps())));

        RouteView secondRoute = routeView(routes, "ROUTE-YXN.069.001.1001");
        assertEquals(21, secondRoute.steps().size());
        assertEquals("B320", processCode(finalStep(secondRoute.steps())));
    }

    @Test
    void parseMalformedMarkdown_withoutStepsTableThrows() {
        String malformedMarkdown = """
                # Malformed IntGY process route export

                ## 1. ROUTE-BROKEN

                ### 路线摘要

                | 字段 | 值 |
                | --- | --- |
                | routeCode | ROUTE-BROKEN |
                | routeName | Missing required steps table |

                ### 当前发布版本

                | 字段 | 值 |
                | --- | --- |
                | routeVersionId | rv-broken |
                | versionStatus | published |
                """;

        assertThrows(IllegalArgumentException.class, () -> parseMarkdown(malformedMarkdown));
    }

    @Test
    void parseMalformedMarkdown_withInvalidFinalProcessFlagThrows() throws Exception {
        String malformedMarkdown = Files.readString(CURRENT_EXPORT_FIXTURE, StandardCharsets.UTF_8)
                .replace(" | FS | 0 |", " | FS | 2 |");

        assertThrows(IllegalArgumentException.class, () -> parseMarkdown(malformedMarkdown));
    }

    private static Object parseMarkdown(Path markdownPath) throws Exception {
        ParserInvocation invocation = parserInvocationFor(markdownPath);
        return invocation.invoke(markdownPath, Files.readString(markdownPath, StandardCharsets.UTF_8));
    }

    private static Object parseMarkdown(String markdown) throws Exception {
        ParserInvocation invocation = parserInvocationFor(markdown);
        return invocation.invoke(markdown, markdown);
    }

    private static ParserInvocation parserInvocationFor(Object preferredInput) throws Exception {
        Class<?> parserClass;
        try {
            parserClass = Class.forName(PARSER_CLASS_NAME);
        } catch (ClassNotFoundException ex) {
            throw new AssertionError("Expected parser class to exist: " + PARSER_CLASS_NAME, ex);
        }

        return methodsNamedParse(parserClass).stream()
                .map(method -> ParserInvocation.tryCreate(parserClass, method, preferredInput))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Expected " + PARSER_CLASS_NAME + " to expose a parse method accepting Path, File, String, "
                                + "CharSequence, or Reader"));
    }

    private static List<Method> methodsNamedParse(Class<?> parserClass) {
        List<Method> methods = new ArrayList<>();
        for (Method method : parserClass.getDeclaredMethods()) {
            if ("parse".equals(method.getName()) && method.getParameterCount() == 1) {
                method.setAccessible(true);
                methods.add(method);
            }
        }
        methods.sort(Comparator.comparingInt(IntGyRouteMarkdownParserTest::parseMethodPriority));
        return methods;
    }

    private static int parseMethodPriority(Method method) {
        Class<?> inputType = method.getParameterTypes()[0];
        if (Path.class.isAssignableFrom(inputType)) {
            return 0;
        }
        if (File.class.isAssignableFrom(inputType)) {
            return 1;
        }
        if (String.class.isAssignableFrom(inputType)) {
            return 2;
        }
        if (CharSequence.class.isAssignableFrom(inputType)) {
            return 3;
        }
        if (StringReader.class.isAssignableFrom(inputType) || java.io.Reader.class.isAssignableFrom(inputType)) {
            return 4;
        }
        return 100;
    }

    private static Object newParserInstance(Class<?> parserClass) throws Exception {
        Constructor<?> constructor = parserClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private static List<Object> extractRoutes(Object parseResult) {
        Object routes = readValue(parseResult, "routes", "routeList", "items")
                .orElse(parseResult);
        return toList(routes, "routes");
    }

    private static RouteView routeView(List<Object> routes, String routeCode) {
        return routes.stream()
                .map(RouteView::from)
                .filter(route -> routeCode.equals(route.routeCode()))
                .findFirst()
                .orElseGet(() -> fail("Expected route not found: " + routeCode));
    }

    private static Object finalStep(List<Object> steps) {
        return steps.stream()
                .filter(IntGyRouteMarkdownParserTest::isFinalProcess)
                .findFirst()
                .orElseGet(() -> fail("Expected one step marked as final process"));
    }

    private static boolean isFinalProcess(Object step) {
        Object value = readValue(step, "finalProcess", "isFinalProcess", "finalStep", "isFinal")
                .orElse(null);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() == 1;
        }
        return "1".equals(String.valueOf(value)) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private static String processCode(Object step) {
        return readValue(step, "processCode", "code")
                .map(Objects::toString)
                .orElseGet(() -> fail("Expected step to expose processCode"));
    }

    private static Optional<Object> readValue(Object source, String... names) {
        if (source == null) {
            return Optional.empty();
        }
        if (source instanceof Map<?, ?> map) {
            for (String name : names) {
                if (map.containsKey(name)) {
                    return Optional.ofNullable(map.get(name));
                }
            }
            return Optional.empty();
        }
        for (String name : names) {
            Optional<Object> getterValue = readGetter(source, name);
            if (getterValue.isPresent()) {
                return getterValue;
            }
            Optional<Object> fieldValue = readField(source, name);
            if (fieldValue.isPresent()) {
                return fieldValue;
            }
        }
        return Optional.empty();
    }

    private static Optional<Object> readGetter(Object source, String name) {
        String suffix = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        String[] getterNames = { "get" + suffix, name };
        for (String getterName : getterNames) {
            try {
                Method method = source.getClass().getMethod(getterName);
                method.setAccessible(true);
                return Optional.ofNullable(method.invoke(source));
            } catch (NoSuchMethodException ignored) {
                // Try the next conventional accessor name.
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError("Unable to read accessor " + getterName + " from "
                        + source.getClass().getName(), ex);
            }
        }
        return Optional.empty();
    }

    private static Optional<Object> readField(Object source, String name) {
        Class<?> type = source.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return Optional.ofNullable(field.get(source));
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (IllegalAccessException ex) {
                throw new AssertionError("Unable to read field " + name + " from " + source.getClass().getName(), ex);
            }
        }
        return Optional.empty();
    }

    private static List<Object> toList(Object value, String label) {
        if (value instanceof Collection<?> collection) {
            return new ArrayList<>(collection);
        }
        if (value != null && value.getClass().isArray()) {
            List<Object> items = new ArrayList<>(Array.getLength(value));
            for (int i = 0; i < Array.getLength(value); i++) {
                items.add(Array.get(value, i));
            }
            return items;
        }
        return fail("Expected " + label + " to be a collection or array");
    }

    private record RouteView(String routeCode, List<Object> steps) {

        private static RouteView from(Object route) {
            String routeCode = readValue(route, "routeCode", "code")
                    .map(Objects::toString)
                    .orElseGet(() -> fail("Expected route to expose routeCode"));
            List<Object> steps = readValue(route, "steps", "stepList", "processes", "routeProcesses")
                    .map(value -> toList(value, "steps"))
                    .orElseGet(() -> fail("Expected route " + routeCode + " to expose steps"));
            return new RouteView(routeCode, steps);
        }
    }

    private record ParserInvocation(Object target, Method method, InputKind inputKind) {

        private static Optional<ParserInvocation> tryCreate(Class<?> parserClass, Method method, Object preferredInput) {
            Class<?> inputType = method.getParameterTypes()[0];
            Optional<InputKind> inputKind = InputKind.from(inputType, preferredInput);
            if (inputKind.isEmpty()) {
                return Optional.empty();
            }
            try {
                Object target = Modifier.isStatic(method.getModifiers()) ? null : newParserInstance(parserClass);
                return Optional.of(new ParserInvocation(target, method, inputKind.get()));
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError("Expected " + parserClass.getName()
                        + " to have an accessible no-argument constructor for non-static parse methods", ex);
            } catch (Exception ex) {
                throw new AssertionError("Unable to create parser invocation for " + parserClass.getName(), ex);
            }
        }

        private Object invoke(Object preferredInput, String markdown) throws Exception {
            InvocationArgument argument = inputKind.argument(preferredInput, markdown);
            try {
                return method.invoke(target, argument.value());
            } catch (InvocationTargetException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                if (cause instanceof Error error) {
                    throw error;
                }
                throw new AssertionError("Parser threw checked exception", cause);
            } finally {
                argument.cleanup();
            }
        }
    }

    private record InvocationArgument(Object value, Path tempPath) {

        private void cleanup() throws Exception {
            if (tempPath != null) {
                Files.deleteIfExists(tempPath);
            }
        }
    }

    private enum InputKind {
        PATH,
        FILE,
        STRING,
        CHAR_SEQUENCE,
        READER;

        private static Optional<InputKind> from(Class<?> inputType, Object preferredInput) {
            if (preferredInput instanceof Path && Path.class.isAssignableFrom(inputType)) {
                return Optional.of(PATH);
            }
            if (preferredInput instanceof Path && File.class.isAssignableFrom(inputType)) {
                return Optional.of(FILE);
            }
            if (preferredInput instanceof String && Path.class.isAssignableFrom(inputType)) {
                return Optional.of(PATH);
            }
            if (preferredInput instanceof String && File.class.isAssignableFrom(inputType)) {
                return Optional.of(FILE);
            }
            if (String.class.isAssignableFrom(inputType)) {
                return Optional.of(STRING);
            }
            if (CharSequence.class.isAssignableFrom(inputType)) {
                return Optional.of(CHAR_SEQUENCE);
            }
            if (java.io.Reader.class.isAssignableFrom(inputType)) {
                return Optional.of(READER);
            }
            return Optional.empty();
        }

        private InvocationArgument argument(Object preferredInput, String markdown) throws Exception {
            return switch (this) {
                case PATH -> {
                    if (preferredInput instanceof Path path) {
                        yield new InvocationArgument(path, null);
                    }
                    Path tempPath = Files.createTempFile("intgy-route-malformed-", ".md");
                    Files.writeString(tempPath, markdown, StandardCharsets.UTF_8);
                    yield new InvocationArgument(tempPath, tempPath);
                }
                case FILE -> {
                    if (preferredInput instanceof Path path) {
                        yield new InvocationArgument(path.toFile(), null);
                    }
                    Path tempPath = Files.createTempFile("intgy-route-malformed-", ".md");
                    Files.writeString(tempPath, markdown, StandardCharsets.UTF_8);
                    yield new InvocationArgument(tempPath.toFile(), tempPath);
                }
                case STRING, CHAR_SEQUENCE -> new InvocationArgument(markdown, null);
                case READER -> new InvocationArgument(new StringReader(markdown), null);
            };
        }
    }
}
