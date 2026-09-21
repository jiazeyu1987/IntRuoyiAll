package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseDigestVectorTest {

    private static final String PACKAGE_DIGEST = "b3920c523e8739501931bd5b68811bf7350b2a742960023b2fe1b7925adb0273";
    private static final String MANIFEST_DIGEST = "bb44ed6ea9550e067c1a2af3a105a0989503b046646f26bd1bac19455bee2800";

    @Test
    void matchesTheLanguageIndependentDoubleDigestVector() throws Exception {
        ReflectionApi api = ReflectionApi.load();
        Object result = api.calculate(List.of(
                api.artifact("required-sql/001.sql", "SELECT 1;\n"),
                api.artifact("docker-compose.yml", "services:\n")));

        assertEquals(PACKAGE_DIGEST, api.access(result, "packageDigest"));
        assertEquals(MANIFEST_DIGEST, api.manifestDigest(
                ("{\"schemaVersion\":2,\"packageDigest\":\"" + PACKAGE_DIGEST + "\"}\n")
                        .getBytes(StandardCharsets.UTF_8)));

        @SuppressWarnings("unchecked")
        List<Object> artifacts = (List<Object>) api.access(result, "artifacts");
        assertEquals("docker-compose.yml", api.access(artifacts.get(0), "path"));
        assertEquals("fa73fccdc99ea835967c7dee7efbed1f3ca87260d92931850fdd66da8cb12f1d",
                api.access(artifacts.get(0), "sha256"));
        assertEquals("b4e0497804e46e0a0b0b8c31975b062152d551bac49c3c2e80932567b4085dcd",
                api.access(artifacts.get(1), "sha256"));
    }

    @Test
    void rejectsBackslashesAndCaseFoldedPathCollisions() throws Exception {
        ReflectionApi api = ReflectionApi.load();
        InvocationTargetException backslash = assertThrows(InvocationTargetException.class,
                () -> api.calculate(List.of(api.artifact("required-sql\\001.sql", "SELECT 1;\n"))));
        assertTrue(backslash.getCause().getMessage().contains("PACKAGE_PATH_INVALID"));

        InvocationTargetException collision = assertThrows(InvocationTargetException.class,
                () -> api.calculate(List.of(api.artifact("A/file.txt", "one"),
                        api.artifact("a/file.txt", "two"))));
        assertTrue(collision.getCause().getMessage().contains("PACKAGE_PATH_INVALID"));
    }

    @Test
    void foldsAsciiCaseOnlyAndKeepsNonAsciiUtf8BytesDistinct() throws Exception {
        ReflectionApi api = ReflectionApi.load();
        Object result = api.calculate(List.of(
                api.artifact("Z/Ä.txt", "UP\n"),
                api.artifact("z/ä.txt", "low\n")));

        assertEquals("050f30bf371902978119b425701935f0b98f461ee22803d75fffcc2c3844dd75",
                api.access(result, "packageDigest"));
    }

    private record ReflectionApi(Class<?> contract, Constructor<?> artifactConstructor, Method calculate,
                                 Method manifestDigest) {

        static ReflectionApi load() {
            try {
                Class<?> contract = Class.forName(
                        "cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow.ReleaseDigestContract");
                Class<?> artifact = Class.forName(contract.getName() + "$ArtifactBytes");
                return new ReflectionApi(contract, artifact.getConstructor(String.class, byte[].class),
                        contract.getMethod("calculate", List.class), contract.getMethod("manifestDigest", byte[].class));
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError("required P1 digest contract is missing", exception);
            }
        }

        Object artifact(String path, String content) throws Exception {
            return artifactConstructor.newInstance(path, content.getBytes(StandardCharsets.UTF_8));
        }

        Object calculate(List<Object> artifacts) throws Exception {
            return calculate.invoke(null, artifacts);
        }

        Object manifestDigest(byte[] bytes) throws Exception {
            return manifestDigest.invoke(null, (Object) bytes);
        }

        Object access(Object value, String accessor) throws Exception {
            return value.getClass().getMethod(accessor).invoke(value);
        }
    }
}
