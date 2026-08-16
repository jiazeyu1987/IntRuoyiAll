package cn.iocoder.yudao.module.dcc.registrationcertificate;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class DccRegistrationCertificateDomainTest {

    private static final String DOMAIN = "cn.iocoder.yudao.module.dcc.registrationcertificate.domain.";
    private static final String ENUMS = "cn.iocoder.yudao.module.dcc.registrationcertificate.enums.";

    @Test
    void persistedCodeSetsShouldBeExactAndRejectUnknownValues() throws Exception {
        assertEnum("DccRegistrationCertificateMasterStatus",
                Set.of("DRAFT", "PENDING_FIRST_EFFECTIVE", "ACTIVE", "EXPIRED_UNRENEWED", "VOIDED"));
        assertEnum("DccRegistrationCertificateVersionType",
                Set.of("INITIAL_CERTIFICATE", "RENEWAL_CERTIFICATE"));
        assertEnum("DccRegistrationCertificateVersionStatus",
                Set.of("DRAFT", "PENDING_EFFECTIVE", "CURRENT", "OLD", "VOIDED"));
        assertEnum("DccRegistrationCertificateFileOwnerType",
                Set.of("VERSION", "CHANGE", "SUPPORTING_DOCUMENT"));
        assertEnum("DccRegistrationCertificateFileKind",
                Set.of("REGISTRATION_CERTIFICATE", "CHANGE_APPROVAL", "RENEWAL_ACCEPTANCE_RECEIPT",
                        "RENEWAL_SUPPLEMENT_NOTICE"));
        assertEnum("DccRegistrationCertificateFileStatus",
                Set.of("STAGED", "BOUND", "CLEANUP_REQUIRED", "VOIDED"));
        assertEnum("DccRegistrationCertificateAuditResult",
                Set.of("SUCCESS", "FAILURE"));

        Class<?> masterStatus = requiredClass(ENUMS + "DccRegistrationCertificateMasterStatus");
        Object draft = invokeStatic(masterStatus, "fromCode", new Class<?>[]{String.class}, "DRAFT");
        assertFalse((Boolean) invoke(draft, "isFormal"), "DRAFT must not be formal pending data");
        assertThrows(IllegalArgumentException.class,
                () -> invokeStatic(masterStatus, "fromCode", new Class<?>[]{String.class}, "UNKNOWN"));

        Class<?> versionStatus = requiredClass(ENUMS + "DccRegistrationCertificateVersionStatus");
        Object draftVersion = invokeStatic(versionStatus, "fromCode", new Class<?>[]{String.class}, "DRAFT");
        assertNull(invoke(draftVersion, "currentUniqueFlag"));
        assertNull(invoke(draftVersion, "pendingUniqueFlag"));
        Object current = invokeStatic(versionStatus, "fromCode", new Class<?>[]{String.class}, "CURRENT");
        assertEquals(1, invoke(current, "currentUniqueFlag"));
        assertNull(invoke(current, "pendingUniqueFlag"));
        Object pending = invokeStatic(versionStatus, "fromCode", new Class<?>[]{String.class}, "PENDING_EFFECTIVE");
        assertNull(invoke(pending, "currentUniqueFlag"));
        assertEquals(1, invoke(pending, "pendingUniqueFlag"));
    }

    @Test
    void productionRelationShouldRejectInvalidFlagAndAuthorityCombinations() throws Exception {
        Object enterpriseA = entrusted(10L, "Factory A");

        assertConstructionFails(false, false, List.of());
        assertConstructionFails(true, false, List.of());
        assertConstructionFails(false, true, List.of(enterpriseA));
        assertConstructionFails(true, false, List.of(enterpriseA, enterpriseA));
        assertConstructionFails(true, false, List.of(entrusted(null, "Factory")));
        assertConstructionFails(true, false, List.of(entrusted(11L, " ")));

        Object delegated = relation(true, false, List.of(enterpriseA));
        assertDoesNotThrow(() -> invoke(delegated, "assertProjectionMatches",
                new Class<?>[]{List.class}, List.of(enterpriseA)));
        assertThrows(IllegalArgumentException.class,
                () -> invoke(delegated, "assertProjectionMatches", new Class<?>[]{List.class}, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> invoke(delegated, "assertProjectionMatches", new Class<?>[]{List.class},
                        List.of(entrusted(10L, "Renamed Factory"))));

        Object enterpriseB = entrusted(12L, "Factory B");
        Object ordered = relation(true, false, List.of(enterpriseA, enterpriseB));
        assertDoesNotThrow(() -> invoke(ordered, "assertProjectionMatches",
                new Class<?>[]{List.class}, List.of(enterpriseA, enterpriseB)));
        assertThrows(IllegalArgumentException.class,
                () -> invoke(ordered, "assertProjectionMatches", new Class<?>[]{List.class},
                        List.of(enterpriseB, enterpriseA)));

        relation(false, true, List.of());
        relation(true, true, List.of(enterpriseA));
    }

    @Test
    void formalFactsAndAuthoritySetShouldBeImmutableDefensiveCopies() throws Exception {
        Object enterpriseA = entrusted(20L, "Factory A");
        List<Object> mutableAuthorities = new ArrayList<>(List.of(enterpriseA));
        Object relation = relation(true, false, mutableAuthorities);
        mutableAuthorities.clear();

        @SuppressWarnings("unchecked")
        List<Object> authorities = (List<Object>) invoke(relation, "entrustedEnterprises");
        assertEquals(1, authorities.size(), "authority source mutation must not alter formal facts");
        assertThrows(UnsupportedOperationException.class, () -> authorities.add(enterpriseA));

        Class<?> factsClass = requiredClass(DOMAIN + "DccRegistrationCertificateFormalFacts");
        Constructor<?> constructor = factsClass.getConstructor(Long.class, Long.class, relation.getClass(), List.class);
        List<Long> mutableFileIds = new ArrayList<>(List.of(100L, 101L));
        Object facts = constructor.newInstance(1L, 2L, relation, mutableFileIds);
        mutableFileIds.clear();
        @SuppressWarnings("unchecked")
        List<Long> boundFileIds = (List<Long>) invoke(facts, "boundFileIds");
        assertEquals(List.of(100L, 101L), boundFileIds);
        assertThrows(UnsupportedOperationException.class, () -> boundFileIds.add(102L));
        assertTrue(java.lang.reflect.Modifier.isFinal(factsClass.getModifiers()),
                "formal facts model must be final");
    }

    private static void assertEnum(String simpleName, Set<String> expected) throws Exception {
        Class<?> type = requiredClass(ENUMS + simpleName);
        Set<String> actual = Arrays.stream(type.getEnumConstants())
                .map(value -> ((Enum<?>) value).name())
                .collect(Collectors.toSet());
        assertEquals(expected, actual, simpleName + " must expose only the frozen persisted values");
        assertFalse(actual.contains("OTHER"));
        assertThrows(IllegalArgumentException.class,
                () -> invokeStatic(type, "fromCode", new Class<?>[]{String.class}, "OTHER"));
    }

    private static Object entrusted(Long enterpriseId, String enterpriseName) throws Exception {
        Class<?> type = requiredClass(DOMAIN + "DccRegistrationCertificateEntrustedEnterprise");
        return type.getConstructor(Long.class, String.class).newInstance(enterpriseId, enterpriseName);
    }

    private static Object relation(boolean entrusted, boolean self, List<?> enterprises) throws Exception {
        Class<?> type = requiredClass(DOMAIN + "DccRegistrationCertificateProductionRelation");
        try {
            return type.getConstructor(boolean.class, boolean.class, List.class)
                    .newInstance(entrusted, self, enterprises);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception checked) {
                throw checked;
            }
            throw exception;
        }
    }

    private static void assertConstructionFails(boolean entrusted, boolean self, List<?> enterprises) {
        assertThrows(IllegalArgumentException.class, () -> relation(entrusted, self, enterprises));
    }

    private static Object invokeStatic(Class<?> type, String methodName, Class<?>[] parameterTypes, Object... args)
            throws Exception {
        return invokeMethod(type.getMethod(methodName, parameterTypes), null, args);
    }

    private static Object invoke(Object target, String methodName) throws Exception {
        return invoke(target, methodName, new Class<?>[0]);
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args)
            throws Exception {
        return invokeMethod(target.getClass().getMethod(methodName, parameterTypes), target, args);
    }

    private static Object invokeMethod(Method method, Object target, Object... args) throws Exception {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception checked) {
                throw checked;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw exception;
        }
    }

    private static Class<?> requiredClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ignored) {
            return fail("required production contract is absent: " + className);
        }
    }
}
