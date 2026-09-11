package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseSourceFreezeTest {

    @Test
    void recordsTwoGitRootsAndThreeSourceRolesAtBothFreezePoints() throws Exception {
        ReflectionApi api = ReflectionApi.load();
        List<Object> roots = List.of(
                api.root("maintenance", "D:/release/m", "a".repeat(40), "a".repeat(40), false),
                api.root("application", "D:/release/a", "b".repeat(40), "b".repeat(40), false));
        List<Object> roles = List.of(
                api.role("maintenance", "maintenance", ".", "a".repeat(40)),
                api.role("backend", "application", "IntRuoyiBackend", "b".repeat(40)),
                api.role("frontend", "application", "IntRuoyiFronted", "b".repeat(40)));

        Object beforeBuild = api.verify("BEFORE_BUILD", roots, roles);
        Object beforePackage = api.verify("BEFORE_PACKAGE", roots, roles);

        assertEquals(2, ((List<?>) api.access(beforeBuild, "gitRoots")).size());
        assertEquals(3, ((List<?>) api.access(beforeBuild, "sourceRoles")).size());
        assertEquals("BEFORE_PACKAGE", api.access(beforePackage, "verificationPoint").toString());
    }

    @Test
    void blocksDirtyOrHeadDriftAtEitherFreezePoint() throws Exception {
        ReflectionApi api = ReflectionApi.load();
        List<Object> roles = List.of(
                api.role("maintenance", "maintenance", ".", "a".repeat(40)),
                api.role("backend", "application", "IntRuoyiBackend", "b".repeat(40)),
                api.role("frontend", "application", "IntRuoyiFronted", "b".repeat(40)));

        List<Object> dirtyRoots = List.of(
                api.root("maintenance", "D:/release/m", "a".repeat(40), "a".repeat(40), true),
                api.root("application", "D:/release/a", "b".repeat(40), "b".repeat(40), false));
        InvocationTargetException dirty = assertThrows(InvocationTargetException.class,
                () -> api.verify("BEFORE_BUILD", dirtyRoots, roles));
        assertTrue(dirty.getCause().getMessage().contains("SOURCE_DIRTY"));

        List<Object> driftRoots = List.of(
                api.root("maintenance", "D:/release/m", "a".repeat(40), "a".repeat(40), false),
                api.root("application", "D:/release/a", "b".repeat(40), "c".repeat(40), false));
        InvocationTargetException drift = assertThrows(InvocationTargetException.class,
                () -> api.verify("BEFORE_PACKAGE", driftRoots, roles));
        assertTrue(drift.getCause().getMessage().contains("SOURCE_HEAD_DRIFT"));
    }

    private record ReflectionApi(Class<?> contractClass, Class<?> pointClass, Constructor<?> rootConstructor,
                                 Constructor<?> roleConstructor, Method verifyMethod) {

        static ReflectionApi load() {
            try {
                Class<?> contract = Class.forName(
                        "cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow.ReleaseSourceFreezeContract");
                Class<?> point = Class.forName(contract.getName() + "$VerificationPoint");
                Class<?> root = Class.forName(contract.getName() + "$GitRootEvidence");
                Class<?> role = Class.forName(contract.getName() + "$SourceRoleEvidence");
                return new ReflectionApi(contract, point,
                        root.getConstructor(String.class, String.class, String.class, String.class, boolean.class),
                        role.getConstructor(String.class, String.class, String.class, String.class),
                        contract.getMethod("verify", point, List.class, List.class));
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError("required P1 source-freeze contract is missing", exception);
            }
        }

        Object root(String role, String path, String approvedCommit, String head, boolean dirty) throws Exception {
            return rootConstructor.newInstance(role, path, approvedCommit, head, dirty);
        }

        Object role(String role, String rootRole, String path, String commit) throws Exception {
            return roleConstructor.newInstance(role, rootRole, path, commit);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        Object verify(String point, List<Object> roots, List<Object> roles) throws Exception {
            Object enumValue = Enum.valueOf((Class<? extends Enum>) pointClass, point);
            return verifyMethod.invoke(null, enumValue, roots, roles);
        }

        Object access(Object value, String accessor) throws Exception {
            return value.getClass().getMethod(accessor).invoke(value);
        }
    }
}
