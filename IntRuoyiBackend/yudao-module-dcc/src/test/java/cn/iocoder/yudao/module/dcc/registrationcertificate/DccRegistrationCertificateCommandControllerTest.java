package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.certificate.DccRegistrationCertificateCommandController;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.certificate.vo.command.DccRegistrationCertificateDraftReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.certificate.vo.command.DccRegistrationCertificateFormalizeReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.certificate.vo.command.DccRegistrationCertificateUpdateDraftReqVO;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccRegistrationCertificateCommandControllerTest {

    @Test
    void commandController_exposesOnlyTheFrozenRoutesPermissionsAndRequiredIdempotencyHeader() throws Exception {
        RequestMapping root = DccRegistrationCertificateCommandController.class.getAnnotation(RequestMapping.class);
        assertEquals("/dcc/registration-certificates", root.value()[0]);

        Method create = DccRegistrationCertificateCommandController.class.getDeclaredMethod(
                "createDraft", String.class, DccRegistrationCertificateDraftReqVO.class);
        assertEquals("/drafts", create.getAnnotation(PostMapping.class).value()[0]);
        assertPermission(create, "dcc:registration-certificate:create");
        assertIdempotencyHeader(create, 0);

        Method update = DccRegistrationCertificateCommandController.class.getDeclaredMethod(
                "updateDraft", Long.class, String.class, DccRegistrationCertificateUpdateDraftReqVO.class);
        assertEquals("/drafts/{id}", update.getAnnotation(PutMapping.class).value()[0]);
        assertPermission(update, "dcc:registration-certificate:update");
        assertIdempotencyHeader(update, 1);

        Method delete = DccRegistrationCertificateCommandController.class.getDeclaredMethod(
                "deleteDraft", Long.class, String.class, Integer.class, Integer.class);
        assertEquals("/drafts/{id}", delete.getAnnotation(DeleteMapping.class).value()[0]);
        assertPermission(delete, "dcc:registration-certificate:delete-draft");
        assertIdempotencyHeader(delete, 1);

        Method formalize = DccRegistrationCertificateCommandController.class.getDeclaredMethod(
                "formalize", Long.class, String.class, DccRegistrationCertificateFormalizeReqVO.class);
        assertEquals("/{id}/formalize", formalize.getAnnotation(PostMapping.class).value()[0]);
        assertPermission(formalize, "dcc:registration-certificate:formalize");
        assertIdempotencyHeader(formalize, 1);
    }

    @Test
    void draftRequest_declaresTheAcceptedSchemaTextLengths() throws Exception {
        for (Map.Entry<String, Integer> expected : Map.of(
                "certificateNo", 128,
                "classification", 64,
                "registrantName", 255).entrySet()) {
            Size size = DccRegistrationCertificateDraftReqVO.class
                    .getDeclaredField(expected.getKey()).getAnnotation(Size.class);
            assertNotNull(size, expected.getKey() + " must declare an HTTP length boundary");
            assertEquals(expected.getValue(), size.max(), expected.getKey());
        }
    }

    @Test
    void lifecycleActionControllers_exposeApprovedRoutesPermissionsAndIdempotencyHeaders() throws Exception {
        Class<?> renewalController = assertControllerPresent(
                "cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.renewal"
                        + ".DccRegistrationCertificateRenewalController");
        assertRoot(renewalController, "/dcc/registration-certificates/{certificateId}/renewals");
        assertRoute(renewalController, "uploadCandidate", PostMapping.class, "",
                "dcc:registration-certificate:renewal:upload", 1);
        assertRoute(renewalController, "voidPendingCandidate", PostMapping.class, "/{pendingVersionId}/void",
                "dcc:registration-certificate:renewal:void", 1);

        Class<?> changeController = assertControllerPresent(
                "cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.change"
                        + ".DccRegistrationCertificateChangeController");
        assertRoot(changeController, "/dcc/registration-certificates/{certificateId}/changes");
        assertRoute(changeController, "applyChange", PostMapping.class, "",
                "dcc:registration-certificate:change:submit", 1);
        assertRoute(changeController, "voidCertificate", PostMapping.class, "/void",
                "dcc:registration-certificate:void", 1);

        Class<?> supportingController = assertControllerPresent(
                "cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.supportingdocument"
                        + ".DccRegistrationCertificateSupportingDocumentController");
        assertRoot(supportingController, "/dcc/registration-certificates/{certificateId}/supporting-documents");
        assertRoute(supportingController, "upload", PostMapping.class, "",
                "dcc:registration-certificate:supporting-document:upload", 1);
        assertNoDeclaredMethod(supportingController, "confirm");
        assertNoDeclaredMethod(supportingController, "reject");
    }

    private static Class<?> assertControllerPresent(String className) throws Exception {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new AssertionError(className + " must exist as a real admin API controller", ex);
        }
    }

    private static void assertRoot(Class<?> controller, String expectedRoot) {
        RequestMapping root = controller.getAnnotation(RequestMapping.class);
        assertNotNull(root, controller.getSimpleName() + " must declare RequestMapping");
        assertEquals(expectedRoot, root.value()[0]);
    }

    private static void assertRoute(Class<?> controller, String methodName,
                                    Class<? extends Annotation> mappingAnnotation,
                                    String expectedPath, String permission,
                                    int idempotencyParameterIndex) {
        Method method = null;
        for (Method candidate : controller.getDeclaredMethods()) {
            if (candidate.getName().equals(methodName)) {
                method = candidate;
                break;
            }
        }
        assertNotNull(method, controller.getSimpleName() + "." + methodName + " must exist");
        Annotation mapping = method.getAnnotation(mappingAnnotation);
        assertNotNull(mapping, methodName + " must declare " + mappingAnnotation.getSimpleName());
        assertEquals(expectedPath, firstMappingValue(mapping), methodName + " route");
        assertPermission(method, permission);
        assertIdempotencyHeader(method, idempotencyParameterIndex);
    }

    private static void assertNoDeclaredMethod(Class<?> controller, String methodName) {
        for (Method method : controller.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                throw new AssertionError(controller.getSimpleName() + "." + methodName
                        + " must not exist after direct-effective upload policy");
            }
        }
    }

    private static String firstMappingValue(Annotation mapping) {
        if (mapping instanceof PostMapping postMapping) {
            return postMapping.value().length == 0 ? "" : postMapping.value()[0];
        }
        if (mapping instanceof PutMapping putMapping) {
            return putMapping.value().length == 0 ? "" : putMapping.value()[0];
        }
        if (mapping instanceof DeleteMapping deleteMapping) {
            return deleteMapping.value().length == 0 ? "" : deleteMapping.value()[0];
        }
        throw new AssertionError("Unsupported mapping annotation " + mapping.annotationType());
    }

    private static void assertPermission(Method method, String permission) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, method.getName() + " must declare a permission");
        assertTrue(preAuthorize.value().contains("'" + permission + "'"));
    }

    private static void assertIdempotencyHeader(Method method, int parameterIndex) {
        RequestHeader header = findAnnotation(
                method.getParameterAnnotations()[parameterIndex], RequestHeader.class);
        assertNotNull(header, method.getName() + " must require Idempotency-Key");
        assertEquals("Idempotency-Key", header.value());
        assertTrue(header.required());
    }

    private static <T extends Annotation> T findAnnotation(Annotation[] annotations, Class<T> type) {
        for (Annotation annotation : annotations) {
            if (type.isInstance(annotation)) {
                return type.cast(annotation);
            }
        }
        return null;
    }
}
