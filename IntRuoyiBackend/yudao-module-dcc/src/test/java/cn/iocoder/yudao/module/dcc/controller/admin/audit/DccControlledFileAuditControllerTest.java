package cn.iocoder.yudao.module.dcc.controller.admin.audit;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.audit.vo.DccControlledFileAuditPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.audit.vo.DccControlledFileAuditRespVO;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAuditQuery;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAuditQueryService;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAuditRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileAuditControllerTest extends BaseMockitoUnitTest {

    private static final String PAGE_PATH = "/dcc/controlled-file-audits/page";
    private static final String AUDIT_PERMISSION = "dcc:controlled-file:audit:query";

    @Mock
    private DccControlledFileAuditQueryService auditQueryService;

    @InjectMocks
    private DccControlledFileAuditController controller;

    @Test
    void getAuditPage_requiresDedicatedAuditPermissionAndMapsQuery() throws Exception {
        DccControlledFileAuditRecord record = new DccControlledFileAuditRecord();
        record.setId(1L);
        record.setAccessEventId(11L);
        record.setAccessEventCode("AE-20260528-0001");
        record.setWatermarkTraceCode("WM-20260528-0001");
        record.setControlledFileId(1001L);
        record.setFileNumber("DCC-1001");
        record.setFileVersionNo("V1.0");
        record.setUserId(2001L);
        record.setActionType("PREVIEW");
        record.setPurpose("CONTROLLED_PREVIEW");
        record.setResult("DENIED");
        record.setFailureCode("CONTROLLED_FILE_VIEWER_TOKEN_INVALID");
        record.setReason("viewer token invalid");
        record.setSourceIp("10.0.0.8");
        record.setRequestId("req-audit-001");
        record.setUserAgent("Playwright");
        record.setOccurredAt(LocalDateTime.of(2026, 5, 28, 10, 0));
        when(auditQueryService.getAuditPage(any(DccControlledFileAuditQuery.class)))
                .thenReturn(new PageResult<>(List.of(record), 1L));

        Method method = findMappedMethod(GetMapping.class, PAGE_PATH);
        assertCommonResultPageType(method, DccControlledFileAuditRespVO.class);
        assertHasAuditPermission(method);

        DccControlledFileAuditPageReqVO reqVO = new DccControlledFileAuditPageReqVO();
        reqVO.setAccessEventCode("AE-20260528-0001");
        reqVO.setWatermarkTraceCode("WM-20260528-0001");
        reqVO.setControlledFileId(1001L);
        reqVO.setUserId(2001L);
        reqVO.setActionType("PREVIEW");
        reqVO.setResult("DENIED");
        reqVO.setFailureCode("CONTROLLED_FILE_VIEWER_TOKEN_INVALID");
        reqVO.setOccurredAt(new LocalDateTime[]{
                LocalDateTime.of(2026, 5, 28, 9, 0),
                LocalDateTime.of(2026, 5, 28, 11, 0)
        });

        CommonResult<PageResult<DccControlledFileAuditRespVO>> result = controller.getAuditPage(reqVO);

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertEquals(1L, result.getData().getTotal());
        DccControlledFileAuditRespVO row = result.getData().getList().get(0);
        assertEquals("AE-20260528-0001", row.getAccessEventCode());
        assertEquals("WM-20260528-0001", row.getWatermarkTraceCode());
        assertEquals("CONTROLLED_FILE_VIEWER_TOKEN_INVALID", row.getFailureCode());
        assertEquals("10.0.0.8", row.getSourceIp());
        assertEquals("req-audit-001", row.getRequestId());
        assertEquals("Playwright", row.getUserAgent());
        assertDoesNotExposeStorageFields(DccControlledFileAuditRespVO.class);

        ArgumentCaptor<DccControlledFileAuditQuery> queryCaptor =
                ArgumentCaptor.forClass(DccControlledFileAuditQuery.class);
        verify(auditQueryService).getAuditPage(queryCaptor.capture());
        DccControlledFileAuditQuery query = queryCaptor.getValue();
        assertEquals("AE-20260528-0001", query.getAccessEventCode());
        assertEquals("WM-20260528-0001", query.getWatermarkTraceCode());
        assertEquals(1001L, query.getControlledFileId());
        assertEquals(2001L, query.getUserId());
        assertEquals("PREVIEW", query.getActionType());
        assertEquals("DENIED", query.getResult());
        assertEquals("CONTROLLED_FILE_VIEWER_TOKEN_INVALID", query.getFailureCode());
        assertEquals(LocalDateTime.of(2026, 5, 28, 9, 0), query.getOccurredAt()[0]);
        assertEquals(LocalDateTime.of(2026, 5, 28, 11, 0), query.getOccurredAt()[1]);
    }

    private Method findMappedMethod(Class<? extends Annotation> mappingAnnotationType, String expectedFullPath) {
        return Arrays.stream(DccControlledFileAuditController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(mappingAnnotationType))
                .filter(method -> hasFullMappingPath(method.getAnnotation(mappingAnnotationType), expectedFullPath))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing endpoint mapping: " + expectedFullPath));
    }

    private boolean hasFullMappingPath(Annotation methodMapping, String expectedFullPath) {
        return classPrefixes().flatMap(prefix -> annotationPaths(methodMapping)
                        .map(methodPath -> normalizePath(prefix + "/" + methodPath)))
                .anyMatch(expectedFullPath::equals);
    }

    private Stream<String> classPrefixes() {
        RequestMapping requestMapping = DccControlledFileAuditController.class.getAnnotation(RequestMapping.class);
        if (requestMapping == null) {
            return Stream.of("");
        }
        return Stream.concat(Arrays.stream(requestMapping.value()), Arrays.stream(requestMapping.path()))
                .distinct();
    }

    private Stream<String> annotationPaths(Annotation annotation) {
        try {
            String[] value = (String[]) annotation.annotationType().getMethod("value").invoke(annotation);
            String[] path = (String[]) annotation.annotationType().getMethod("path").invoke(annotation);
            return Stream.concat(Arrays.stream(value), Arrays.stream(path)).distinct();
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Cannot inspect endpoint mapping annotation", ex);
        }
    }

    private void assertCommonResultPageType(Method method, Class<?> expectedRowType) {
        assertEquals(CommonResult.class, method.getReturnType());
        Type commonResultType = method.getGenericReturnType();
        assertTrue(commonResultType instanceof ParameterizedType);
        Type pageResultType = ((ParameterizedType) commonResultType).getActualTypeArguments()[0];
        assertTrue(pageResultType instanceof ParameterizedType);
        ParameterizedType pageResultParameterizedType = (ParameterizedType) pageResultType;
        assertEquals(PageResult.class, pageResultParameterizedType.getRawType());
        assertEquals(expectedRowType, pageResultParameterizedType.getActualTypeArguments()[0]);
    }

    private void assertHasAuditPermission(Method method) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Missing @PreAuthorize on backend API surface");
        assertTrue(preAuthorize.value().contains(AUDIT_PERMISSION),
                "Audit query must reject ordinary users without dedicated audit permission");
    }

    private void assertDoesNotExposeStorageFields(Class<?> type) {
        Set<String> fieldNames = Arrays.stream(type.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
        for (String forbidden : Set.of("storageFileId", "sourceFileId", "originalFileId", "publishedFileId",
                "filePath", "path", "fileUrl", "url", "configId", "cipherFileRef")) {
            assertFalse(fieldNames.contains(forbidden), "Audit response exposes storage field: " + forbidden);
        }
    }

    private String normalizePath(String path) {
        String normalized = path.replace('\\', '/').replaceAll("/{2,}", "/");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
