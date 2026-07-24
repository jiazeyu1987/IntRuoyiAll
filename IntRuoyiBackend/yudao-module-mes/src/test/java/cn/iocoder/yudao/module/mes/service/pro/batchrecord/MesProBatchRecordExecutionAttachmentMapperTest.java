package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionAttachmentMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MesProBatchRecordExecutionAttachmentMapperTest extends BaseDbUnitTest {

    private static final Long TENANT_ID = 122L;

    @Resource
    private MesProBatchRecordExecutionAttachmentMapper attachmentMapper;

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void insertAndQueryAttachmentLedgerByExecutionAndField() {
        MesProBatchRecordExecutionAttachmentDO attachment = MesProBatchRecordExecutionAttachmentDO.builder()
                .executionId(501L)
                .batchExecutionId(601L)
                .batchTaskId(701L)
                .workTaskId(801L)
                .rowIndex(1)
                .columnIndex(2)
                .fieldKey("visualEvidence")
                .fieldPath("sheet[0].rows[1].cells[2].visualEvidence")
                .fieldLabel("现场图片")
                .attachmentType("IMAGE")
                .attachmentGroupKey("R1C2-IMG-1")
                .attachmentAction("ADD")
                .versionNo(1)
                .fileId(901L)
                .fileUrl("http://127.0.0.1:48081/admin-api/infra/file/28/get/edhr/evidence.png")
                .storageConfigId(28L)
                .storagePath("edhr/501/evidence.png")
                .fileName("evidence.png")
                .contentType("image/png")
                .fileSize(2048L)
                .sha256("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                .storageRetentionJson("{\"fileId\":901}")
                .storageRetentionHash("abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789")
                .auditBatchId(1001L)
                .signatureId(1002L)
                .previousAttachmentHash(null)
                .attachmentHash("fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210")
                .operatorId(99L)
                .operatorName("aoteman")
                .operatedAt(LocalDateTime.of(2026, 6, 12, 11, 0))
                .reasonCategory("OPERATOR_ENTRY")
                .reasonText("现场上传")
                .tenantId(TENANT_ID)
                .build();

        attachmentMapper.insert(attachment);

        List<MesProBatchRecordExecutionAttachmentDO> byExecution =
                attachmentMapper.selectListByExecutionId(501L);
        assertEquals(1, byExecution.size());
        assertEquals("IMAGE", byExecution.get(0).getAttachmentType());
        assertEquals("fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210",
                byExecution.get(0).getAttachmentHash());

        List<MesProBatchRecordExecutionAttachmentDO> byField =
                attachmentMapper.selectListByExecutionField(501L,
                        "sheet[0].rows[1].cells[2].visualEvidence", "visualEvidence");
        assertEquals(1, byField.size());
        assertEquals("evidence.png", byField.get(0).getFileName());
    }
}
