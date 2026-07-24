package cn.iocoder.yudao.module.dcc.service.audit;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAccessLogDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileAccessEventDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileWatermarkTraceDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAccessLogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileAccessEventMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileWatermarkTraceMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Import(DccControlledFileAuditQueryServiceImpl.class)
class DccControlledFileAuditQueryServiceTest extends BaseDbUnitTest {

    @Resource
    private DccControlledFileAuditQueryService auditQueryService;
    @Resource
    private DccControlledFileAccessEventMapper accessEventMapper;
    @Resource
    private DccControlledFileWatermarkTraceMapper watermarkTraceMapper;
    @Resource
    private DccControlledFileAccessLogMapper accessLogMapper;

    @Test
    void getAuditPage_tracesByWatermarkAndAccessEventWithoutStorageFields() {
        AuditFixture fixture = insertAudit("AE-20260528-PREVIEW", "WM-20260528-PREVIEW",
                1001L, "V1.0", 2001L, "PREVIEW", "CONTROLLED_PREVIEW", "SUCCESS",
                null, null, "10.0.0.8", "Playwright", "req-preview-001",
                LocalDateTime.of(2026, 5, 28, 10, 0));

        DccControlledFileAuditQuery traceQuery = new DccControlledFileAuditQuery();
        traceQuery.setWatermarkTraceCode("WM-20260528-PREVIEW");
        PageResult<DccControlledFileAuditRecord> tracePage = auditQueryService.getAuditPage(traceQuery);

        assertEquals(1L, tracePage.getTotal());
        DccControlledFileAuditRecord traceRow = tracePage.getList().get(0);
        assertEquals(fixture.accessLogId(), traceRow.getId());
        assertEquals("AE-20260528-PREVIEW", traceRow.getAccessEventCode());
        assertEquals("WM-20260528-PREVIEW", traceRow.getWatermarkTraceCode());
        assertEquals(1001L, traceRow.getControlledFileId());
        assertEquals("V1.0", traceRow.getFileVersionNo());
        assertEquals(2001L, traceRow.getUserId());
        assertEquals("PREVIEW", traceRow.getActionType());
        assertEquals("CONTROLLED_PREVIEW", traceRow.getPurpose());
        assertEquals("SUCCESS", traceRow.getResult());
        assertEquals("10.0.0.8", traceRow.getSourceIp());
        assertEquals("Playwright", traceRow.getUserAgent());
        assertEquals("req-preview-001", traceRow.getRequestId());
        assertEquals(LocalDateTime.of(2026, 5, 28, 10, 0), traceRow.getOccurredAt());
        assertEquals("DCC-1001", traceRow.getFileNumber());
        assertEquals("U2001", traceRow.getUserIdentifier());
        assertEquals("Quality User", traceRow.getUserDisplayName());
        assertEquals("Quality", traceRow.getDeptName());
        assertEquals("Test Tenant", traceRow.getTenantName());

        DccControlledFileAuditQuery eventQuery = new DccControlledFileAuditQuery();
        eventQuery.setAccessEventCode("AE-20260528-PREVIEW");
        PageResult<DccControlledFileAuditRecord> eventPage = auditQueryService.getAuditPage(eventQuery);

        assertEquals(1L, eventPage.getTotal());
        assertEquals("WM-20260528-PREVIEW", eventPage.getList().get(0).getWatermarkTraceCode());
        assertDoesNotExposeStorageFields(DccControlledFileAuditRecord.class);
    }

    @Test
    void getAuditPage_filtersByFileUserActionResultFailureCodeAndTimeRange() {
        insertAudit("AE-20260528-UPLOAD", "WM-20260528-UPLOAD",
                1001L, "V1.0", 2001L, "UPLOAD", "UPLOAD_PREVIEW", "SUCCESS",
                null, null, "10.0.0.1", "JUnit", "req-upload-001",
                LocalDateTime.of(2026, 5, 28, 9, 0));
        insertAudit("AE-20260528-OFFICE", "WM-20260528-OFFICE",
                1001L, "V1.0", 2002L, "OFFICE_READ", "ONLYOFFICE_READ", "DENIED",
                "CONTROLLED_FILE_VIEWER_TOKEN_INVALID", "viewer token invalid",
                "10.0.0.2", "OnlyOffice", "req-office-001",
                LocalDateTime.of(2026, 5, 28, 10, 0));
        insertAudit("AE-20260528-DOWNLOAD", null,
                1002L, "V2.0", 2001L, "DOWNLOAD", "CONTROLLED_DOWNLOAD", "ALLOWED",
                null, null, "10.0.0.3", "Browser", "req-download-001",
                LocalDateTime.of(2026, 5, 28, 11, 0));
        insertAudit("AE-20260528-DIRECT", null,
                1003L, "V3.0", 2003L, "DIRECT_LINK", "INFRA_DIRECT_LINK", "DENIED",
                "DCC_DIRECT_LINK_BLOCKED", "direct link blocked",
                "10.0.0.4", "curl", "req-direct-001",
                LocalDateTime.of(2026, 5, 28, 12, 0));
        insertAudit("AE-20260528-TOKEN", "WM-20260528-TOKEN",
                1004L, "V4.0", 2004L, "TOKEN_VALIDATE", "CONTROLLED_PREVIEW", "DENIED",
                "CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH", "token context mismatch",
                "10.0.0.5", "Browser", "req-token-001",
                LocalDateTime.of(2026, 5, 28, 13, 0));
        insertAudit("AE-20260528-TEMP", null,
                1005L, "V5.0", 2005L, "TEMP_CLEANUP", "UPLOAD_TEMPORARY_FILE", "SUCCESS",
                null, null, "10.0.0.6", "Scheduler", "req-temp-001",
                LocalDateTime.of(2026, 5, 28, 14, 0));

        DccControlledFileAuditQuery all = new DccControlledFileAuditQuery();
        all.setPageSize(20);
        assertEquals(6L, auditQueryService.getAuditPage(all).getTotal());

        DccControlledFileAuditQuery fileAndUser = new DccControlledFileAuditQuery();
        fileAndUser.setControlledFileId(1001L);
        fileAndUser.setUserId(2002L);
        assertEquals(List.of("OFFICE_READ"), actionTypes(fileAndUser));

        DccControlledFileAuditQuery actionType = new DccControlledFileAuditQuery();
        actionType.setActionType("DIRECT_LINK");
        assertEquals(List.of("DCC_DIRECT_LINK_BLOCKED"), failureCodes(actionType));

        DccControlledFileAuditQuery result = new DccControlledFileAuditQuery();
        result.setResult("DENIED");
        assertEquals(Set.of("OFFICE_READ", "DIRECT_LINK", "TOKEN_VALIDATE"),
                Set.copyOf(actionTypes(result)));

        DccControlledFileAuditQuery failureCode = new DccControlledFileAuditQuery();
        failureCode.setFailureCode("CONTROLLED_FILE_VIEWER_TOKEN_INVALID");
        PageResult<DccControlledFileAuditRecord> failurePage = auditQueryService.getAuditPage(failureCode);

        assertEquals(1L, failurePage.getTotal());
        DccControlledFileAuditRecord failed = failurePage.getList().get(0);
        assertEquals("10.0.0.2", failed.getSourceIp());
        assertEquals("OnlyOffice", failed.getUserAgent());
        assertEquals("req-office-001", failed.getRequestId());
        assertEquals("CONTROLLED_FILE_VIEWER_TOKEN_INVALID", failed.getFailureCode());

        DccControlledFileAuditQuery requestId = new DccControlledFileAuditQuery();
        requestId.setRequestId("req-direct-001");
        PageResult<DccControlledFileAuditRecord> requestPage = auditQueryService.getAuditPage(requestId);

        assertEquals(1L, requestPage.getTotal());
        assertEquals("DIRECT_LINK", requestPage.getList().get(0).getActionType());
        assertEquals("DCC_DIRECT_LINK_BLOCKED", requestPage.getList().get(0).getFailureCode());

        DccControlledFileAuditQuery timeRange = new DccControlledFileAuditQuery();
        timeRange.setOccurredAt(new LocalDateTime[]{
                LocalDateTime.of(2026, 5, 28, 10, 0),
                LocalDateTime.of(2026, 5, 28, 12, 0)
        });
        assertEquals(Set.of("OFFICE_READ", "DOWNLOAD", "DIRECT_LINK"),
                Set.copyOf(actionTypes(timeRange)));
    }

    @Test
    void getAuditPage_filtersNullEventLogsByCreateTimeWhenOccurredAtRangePresent() {
        LocalDateTime insideTime = LocalDateTime.of(2026, 5, 28, 10, 30);
        LocalDateTime outsideTime = LocalDateTime.of(2026, 5, 28, 12, 30);
        Long insideLogId = insertAccessLogWithoutAccessEvent(1101L, "V1.1", 2101L,
                "PREVIEW", "CONTROLLED_PREVIEW", "DENIED",
                "CONTROLLED_FILE_VIEWER_TOKEN_INVALID", "viewer token invalid",
                "10.0.1.1", "Legacy Browser", "req-legacy-preview-inside", insideTime);
        Long outsideLogId = insertAccessLogWithoutAccessEvent(1102L, "V1.2", 2102L,
                "DOWNLOAD", "CONTROLLED_DOWNLOAD", "DENIED",
                "DCC_DIRECT_LINK_BLOCKED", "direct link blocked",
                "10.0.1.2", "Legacy Browser", "req-legacy-download-outside", outsideTime);

        DccControlledFileAuditQuery timeRange = new DccControlledFileAuditQuery();
        timeRange.setPageSize(20);
        timeRange.setOccurredAt(new LocalDateTime[]{
                LocalDateTime.of(2026, 5, 28, 10, 0),
                LocalDateTime.of(2026, 5, 28, 11, 0)
        });

        PageResult<DccControlledFileAuditRecord> page = auditQueryService.getAuditPage(timeRange);

        assertEquals(1L, page.getTotal());
        DccControlledFileAuditRecord row = page.getList().get(0);
        assertEquals(insideLogId, row.getId());
        assertFalse(page.getList().stream().anyMatch(record -> outsideLogId.equals(record.getId())));
        assertNull(row.getAccessEventId());
        assertNull(row.getAccessEventCode());
        assertEquals(insideTime, row.getOccurredAt());
        assertEquals("10.0.1.1", row.getSourceIp());
        assertEquals("Legacy Browser", row.getUserAgent());
        assertEquals("req-legacy-preview-inside", row.getRequestId());
        assertEquals("CONTROLLED_FILE_VIEWER_TOKEN_INVALID", row.getFailureCode());
    }

    private List<String> actionTypes(DccControlledFileAuditQuery query) {
        query.setPageSize(20);
        return auditQueryService.getAuditPage(query).getList().stream()
                .map(DccControlledFileAuditRecord::getActionType)
                .collect(Collectors.toList());
    }

    private List<String> failureCodes(DccControlledFileAuditQuery query) {
        query.setPageSize(20);
        return auditQueryService.getAuditPage(query).getList().stream()
                .map(DccControlledFileAuditRecord::getFailureCode)
                .collect(Collectors.toList());
    }

    private AuditFixture insertAudit(String accessEventCode, String watermarkTraceCode,
                                     Long fileId, String versionNo, Long userId,
                                     String actionType, String purpose, String result,
                                     String failureCode, String reason,
                                     String sourceIp, String userAgent, String requestId,
                                     LocalDateTime occurredAt) {
        DccControlledFileAccessEventDO accessEvent = DccControlledFileAccessEventDO.builder()
                .accessEventCode(accessEventCode)
                .controlledFileId(fileId)
                .fileVersionNo(versionNo)
                .userId(userId)
                .accessType(actionType)
                .purpose(purpose)
                .result(result)
                .failureCode(failureCode)
                .failureReason(reason)
                .sourceIp(sourceIp)
                .userAgent(userAgent)
                .requestId(requestId)
                .occurredAt(occurredAt)
                .build();
        accessEventMapper.insert(accessEvent);
        assertNotNull(accessEvent.getId());

        if (watermarkTraceCode != null) {
            DccControlledFileWatermarkTraceDO watermarkTrace = DccControlledFileWatermarkTraceDO.builder()
                    .traceCode(watermarkTraceCode)
                    .accessEventId(accessEvent.getId())
                    .accessEventCode(accessEventCode)
                    .controlledFileId(fileId)
                    .fileNumber("DCC-" + fileId)
                    .fileVersionNo(versionNo)
                    .userId(userId)
                    .userIdentifier("U" + userId)
                    .userDisplayName("Quality User")
                    .deptId(3001L)
                    .deptName("Quality")
                    .tenantName("Test Tenant")
                    .privacyMode("TRACE_CODE_ONLY")
                    .watermarkPayloadJson("{\"traceCode\":\"" + watermarkTraceCode + "\"}")
                    .issuedAt(occurredAt)
                    .expiresAt(occurredAt.plusMinutes(15))
                    .build();
            watermarkTraceMapper.insert(watermarkTrace);
            assertNotNull(watermarkTrace.getId());
        }

        DccControlledFileAccessLogDO accessLog = DccControlledFileAccessLogDO.builder()
                .controlledFileId(fileId)
                .accessEventId(accessEvent.getId())
                .accessEventCode(accessEventCode)
                .watermarkTraceCode(watermarkTraceCode)
                .fileVersionNo(versionNo)
                .userId(userId)
                .actionType(actionType)
                .purpose(purpose)
                .result(result)
                .failureCode(failureCode)
                .reason(reason)
                .sourceIp(sourceIp)
                .requestId(requestId)
                .userAgent(userAgent)
                .build();
        accessLogMapper.insert(accessLog);
        assertNotNull(accessLog.getId());
        return new AuditFixture(accessLog.getId());
    }

    private Long insertAccessLogWithoutAccessEvent(Long fileId, String versionNo, Long userId,
                                                   String actionType, String purpose, String result,
                                                   String failureCode, String reason,
                                                   String sourceIp, String userAgent, String requestId,
                                                   LocalDateTime createTime) {
        DccControlledFileAccessLogDO accessLog = DccControlledFileAccessLogDO.builder()
                .controlledFileId(fileId)
                .fileVersionNo(versionNo)
                .userId(userId)
                .actionType(actionType)
                .purpose(purpose)
                .result(result)
                .failureCode(failureCode)
                .reason(reason)
                .sourceIp(sourceIp)
                .requestId(requestId)
                .userAgent(userAgent)
                .build();
        accessLog.setCreateTime(createTime);
        accessLogMapper.insert(accessLog);
        assertNotNull(accessLog.getId());

        DccControlledFileAccessLogDO update = new DccControlledFileAccessLogDO();
        update.setId(accessLog.getId());
        update.setCreateTime(createTime);
        accessLogMapper.updateById(update);
        return accessLog.getId();
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

    private record AuditFixture(Long accessLogId) {
    }
}
