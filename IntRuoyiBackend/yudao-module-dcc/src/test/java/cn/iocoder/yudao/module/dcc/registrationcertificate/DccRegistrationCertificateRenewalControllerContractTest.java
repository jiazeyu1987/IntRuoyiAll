package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.renewal.DccRegistrationCertificateRenewalController;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.renewal.vo.DccRegistrationCertificateRenewalUploadReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.renewal.DccRegistrationCertificateRenewalService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccRegistrationCertificateRenewalControllerContractTest {

    @Test
    void renewalControllerSubmitsMultipartRequestIntoApprovalFlow() {
        RequestMapping root = DccRegistrationCertificateRenewalController.class.getAnnotation(RequestMapping.class);
        assertNotNull(root);
        assertEquals("/dcc/registration-certificates/{certificateId}/renewals", root.value()[0]);

        Constructor<?> constructor = DccRegistrationCertificateRenewalController.class.getDeclaredConstructors()[0];
        assertTrue(Arrays.asList(constructor.getParameterTypes()).contains(DccRegistrationCertificateRenewalService.class),
                "renewal controller must persist the renewal request through the renewal service");
        assertTrue(Arrays.asList(constructor.getParameterTypes()).contains(DccRegistrationCertificateApprovalService.class),
                "renewal controller must immediately start the formal approval flow");

        Method submit = Arrays.stream(DccRegistrationCertificateRenewalController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("submitForApproval"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("submitForApproval must be the row-level renewal action"));
        PostMapping post = submit.getAnnotation(PostMapping.class);
        assertNotNull(post);
        assertEquals("", post.value().length == 0 ? "" : post.value()[0]);
        assertNotNull(submit.getAnnotation(Transactional.class),
                "request persistence and BPM binding must share the same rollback boundary");
        PreAuthorize preAuthorize = submit.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertTrue(preAuthorize.value().contains("'dcc:registration-certificate:renewal:upload'"));
        assertHeader(submit.getParameterAnnotations()[1]);
        assertNotNull(findAnnotation(submit.getParameterAnnotations()[2], ModelAttribute.class),
                "multipart renewal form must be bound as ModelAttribute");
        assertNull(findAnnotation(submit.getParameterAnnotations()[2], RequestBody.class),
                "multipart renewal form must not be declared as RequestBody");
    }

    @Test
    void renewalUploadRequestOnlyContainsDatesCurrentVersionAndFile() throws Exception {
        Set<String> fields = Arrays.stream(DccRegistrationCertificateRenewalUploadReqVO.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of("expectedRowVersion", "currentVersionId",
                "approvalDate", "effectiveDate", "expiryDate", "file"), fields);
        assertField("expectedRowVersion", Integer.class, true, true);
        assertField("currentVersionId", Long.class, true, true);
        assertField("approvalDate", LocalDate.class, true, false);
        assertField("effectiveDate", LocalDate.class, true, false);
        assertField("expiryDate", LocalDate.class, true, false);
        assertField("file", MultipartFile.class, true, false);
    }

    private static void assertHeader(Annotation[] annotations) {
        RequestHeader header = findAnnotation(annotations, RequestHeader.class);
        assertNotNull(header);
        assertEquals("Idempotency-Key", header.value());
        assertTrue(header.required());
    }

    private static void assertField(String name, Class<?> expectedType,
                                    boolean requireNotNull, boolean requirePositive) throws Exception {
        Field field = DccRegistrationCertificateRenewalUploadReqVO.class.getDeclaredField(name);
        assertEquals(expectedType, field.getType(), name);
        assertEquals(requireNotNull, field.getAnnotation(NotNull.class) != null, name + " NotNull");
        assertEquals(requirePositive, field.getAnnotation(Positive.class) != null, name + " Positive");
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
