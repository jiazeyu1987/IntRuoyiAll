package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.iocoder.yudao.module.system.controller.admin.gxpaudit.GxpAuditEventController;
import cn.iocoder.yudao.module.system.controller.admin.gxpaudit.vo.GxpAuditEventPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.gxpaudit.vo.GxpAuditEventRespVO;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GxpAuditQueryContractTest {

    @Test
    void pageRequestShouldExposeOnlyApprovedScopeInputs() {
        Set<String> fields = Arrays.stream(GxpAuditEventPageReqVO.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertTrue(fields.containsAll(Set.of("scopeType", "scopeId", "operationId", "resultStatus", "occurredAt")));
        assertFalse(fields.contains("tenantId"));
    }

    @Test
    void responseShouldExposeIntegrityAndEvidenceFields() {
        Set<String> fields = Arrays.stream(GxpAuditEventRespVO.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertTrue(fields.containsAll(Set.of("eventHash", "previousEventHash", "signatureRecordId",
                "relationManifestJson", "evidenceManifestJson", "serverOccurredAt")));
    }

    @Test
    void controllerShouldExposeReadOnlyPageAndGetOnly() {
        assertEquals("/system/gxp-audit-event", GxpAuditEventController.class
                .getAnnotation(RequestMapping.class).value()[0]);
        Set<String> mappings = Arrays.stream(GxpAuditEventController.class.getDeclaredMethods())
                .flatMap(method -> Arrays.stream(method.getAnnotationsByType(GetMapping.class))
                        .map(annotation -> method.getName() + ":GET"))
                .collect(Collectors.toSet());
        assertTrue(mappings.contains("page:GET"));
        assertTrue(mappings.contains("get:GET"));
        assertTrue(Arrays.stream(GxpAuditEventController.class.getDeclaredMethods())
                .noneMatch(this::hasWriteMapping));
    }

    private boolean hasWriteMapping(Method method) {
        return method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class);
    }
}
