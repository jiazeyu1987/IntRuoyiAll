package cn.iocoder.yudao.module.dcc;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAccessLogDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileAccessEventDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileDownloadRecordDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileUploadPolicyDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileWatermarkTraceDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAccessLogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileAccessEventMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileDownloadRecordMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileTemporaryFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileUploadPolicyMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileWatermarkTraceMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DccProtectionSchemaTest extends BaseDbUnitTest {

    @Resource
    private DccControlledFileAccessEventMapper accessEventMapper;
    @Resource
    private DccControlledFileWatermarkTraceMapper watermarkTraceMapper;
    @Resource
    private DccControlledFileUploadPolicyMapper uploadPolicyMapper;
    @Resource
    private DccControlledFileTemporaryFileMapper temporaryFileMapper;
    @Resource
    private DccControlledFileDownloadRecordMapper downloadRecordMapper;
    @Resource
    private DccControlledFileAccessLogMapper accessLogMapper;

    @Test
    void protectionFoundationTablesAcceptMapperInsertsAndAuditExtensionFields() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 28, 9, 30);

        DccControlledFileAccessEventDO accessEvent = DccControlledFileAccessEventDO.builder()
                .accessEventCode("AE-20260528-0001")
                .controlledFileId(1001L)
                .fileVersionNo("V1.0")
                .userId(2001L)
                .accessType("PREVIEW")
                .purpose("CONTROLLED_PREVIEW")
                .result("SUCCESS")
                .sourceIp("127.0.0.1")
                .userAgent("JUnit")
                .requestId("req-schema-001")
                .occurredAt(now)
                .build();
        accessEventMapper.insert(accessEvent);
        assertNotNull(accessEvent.getId());

        DccControlledFileWatermarkTraceDO watermarkTrace = DccControlledFileWatermarkTraceDO.builder()
                .traceCode("WM-20260528-0001")
                .accessEventId(accessEvent.getId())
                .accessEventCode(accessEvent.getAccessEventCode())
                .controlledFileId(accessEvent.getControlledFileId())
                .fileNumber("DCC-001")
                .fileVersionNo("V1.0")
                .userId(2001L)
                .userIdentifier("u2001")
                .userDisplayName("Schema User")
                .deptId(3001L)
                .deptName("Quality")
                .tenantName("Test Tenant")
                .privacyMode("TRACE_CODE_ONLY")
                .watermarkPayloadJson("{\"traceCode\":\"WM-20260528-0001\"}")
                .issuedAt(now)
                .expiresAt(now.plusMinutes(30))
                .build();
        watermarkTraceMapper.insert(watermarkTrace);
        assertNotNull(watermarkTrace.getId());

        DccControlledFileUploadPolicyDO uploadPolicy = DccControlledFileUploadPolicyDO.builder()
                .policyCode("UP-CAT-SRC-V1")
                .scopeType("CATEGORY_PURPOSE")
                .categoryId(10L)
                .purpose("SOURCE_FILE")
                .maxBytes(10_485_760L)
                .enabled(Boolean.TRUE)
                .priority(400)
                .policyVersion("v1")
                .effectiveFrom(now)
                .changeReason("schema contract")
                .build();
        uploadPolicyMapper.insert(uploadPolicy);
        assertNotNull(uploadPolicy.getId());

        DccControlledFileTemporaryFileDO temporaryFile = DccControlledFileTemporaryFileDO.builder()
                .uploadTicket("UT-20260528-0001")
                .sessionId("session-001")
                .purpose("SOURCE_FILE")
                .uploaderId(2001L)
                .originalFileName("source.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .fileSha256("plain-sha256")
                .storageFileId(9001L)
                .status("UPLOADED")
                .expireTime(now.plusMinutes(30))
                .cleanupStatus("PENDING")
                .requestId("req-schema-002")
                .build();
        temporaryFileMapper.insert(temporaryFile);
        assertNotNull(temporaryFile.getId());

        DccControlledFileDownloadRecordDO downloadRecord = DccControlledFileDownloadRecordDO.builder()
                .downloadRequestId("DR-20260528-0001")
                .accessEventId(accessEvent.getId())
                .accessEventCode(accessEvent.getAccessEventCode())
                .controlledFileId(accessEvent.getControlledFileId())
                .fileVersionNo("V1.0")
                .userId(2001L)
                .policyVersion("download-v1")
                .encryptionStatus("READY")
                .encryptionPolicyVersion("enc-v1")
                .artifactId("artifact-001")
                .cipherFileRef("cipher/ref/001")
                .plainSha256("plain-sha256")
                .cipherSha256("cipher-sha256")
                .requestedAt(now)
                .encryptedAt(now.plusSeconds(5))
                .returnedAt(now.plusSeconds(10))
                .build();
        downloadRecordMapper.insert(downloadRecord);
        assertNotNull(downloadRecord.getId());

        DccControlledFileAccessLogDO accessLog = DccControlledFileAccessLogDO.builder()
                .controlledFileId(accessEvent.getControlledFileId())
                .accessEventId(accessEvent.getId())
                .accessEventCode(accessEvent.getAccessEventCode())
                .watermarkTraceCode(watermarkTrace.getTraceCode())
                .fileVersionNo("V1.0")
                .userId(2001L)
                .actionType("PREVIEW")
                .purpose("CONTROLLED_PREVIEW")
                .result("SUCCESS")
                .sourceIp("127.0.0.1")
                .requestId("req-schema-003")
                .userAgent("JUnit")
                .build();
        accessLogMapper.insert(accessLog);
        assertNotNull(accessLog.getId());

        assertEquals("AE-20260528-0001", accessLogMapper.selectById(accessLog.getId()).getAccessEventCode());
    }

}
