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
