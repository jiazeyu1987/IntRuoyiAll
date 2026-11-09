package cn.iocoder.yudao.module.dcc.approval;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccNativeApprovalSummaryEnricherTest {
    private final DccControlledFileMapper files = mock(DccControlledFileMapper.class);
    private final DccFileCategoryMapper categories = mock(DccFileCategoryMapper.class);
    private final DccApprovalTaskAdapter adapter = new DccApprovalTaskAdapter(
            null, null, null, files, categories, null,
            mock(DccProjectProductTaskDelegate.class), mock(DccOfflineTrainingTaskDelegate.class));
    private final DccNativeApprovalSummaryEnricher enricher = new DccNativeApprovalSummaryEnricher(files, adapter);
    private DccControlledFileDO file;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        file = DccControlledFileDO.builder().id(10L).tenantId(1L).title("文件正文.docx")
                .fileNumber("67").versionNo("B/7").categoryId(20L).stampedFileId(30L)
                .processInstanceId("later-resubmission").build();
        when(files.selectByIdIncludingDeleted(10L)).thenReturn(file);
        when(categories.selectById(20L)).thenReturn(DccFileCategoryDO.builder()
                .id(20L).name("文件类别").distributionRequired(false).build());
    }

    @AfterEach void cleanup() { TenantContextHolder.clear(); }

    private ApprovalTaskSummary summary() {
        return ApprovalTaskSummary.builder().id("BPM:BPM_PROCESS_INSTANCE:original-process")
                .moduleCode(ApprovalModuleCode.BPM).sourceTaskType("BPM_PROCESS_INSTANCE")
                .sourceTaskId("original-process").processInstanceId("original-process")
                .businessKey("10").businessTitle("DCC受控文件上传审批 67 / B/7")
                .businessStatus("MY_INITIATED").currentNodeName("我发起的")
                .detailRoute("/bpm/process-instance/detail").detailQuery(Map.of("id", "original-process"))
                .availableActions(Set.of("OPEN_DETAIL")).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"dcc-controlled-file-upload", "dcc-controlled-file-revision", "dcc-controlled-file-approval"})
    void initiatedFlowUsesExactVersionAndRealDccTagsWithoutChangingProcessIdentity(String key) {
        var row = summary();
        enricher.enrich(row, key);
        assertEquals("文件正文.docx", row.getBusinessTitle());
        assertEquals("67", row.getBusinessCode());
        assertEquals(List.of("文件编号：67", "版本：B/7", "分类：文件类别", "当前节点：我发起的", "盖章：已生成", "分发：不需要"), row.getBusinessContextTags());
        assertEquals("10", row.getBusinessKey());
        assertEquals("original-process", row.getProcessInstanceId());
        assertEquals("original-process", row.getSourceTaskId());
        assertEquals("MY_INITIATED", row.getBusinessStatus());
        assertEquals("/bpm/process-instance/detail", row.getDetailRoute());
        assertEquals(Map.of("id", "original-process"), row.getDetailQuery());
        assertEquals(Set.of("OPEN_DETAIL"), row.getAvailableActions());
        assertEquals(false, row.getBusinessDeleted());
        verify(files).selectByIdIncludingDeleted(10L);
    }

    @Test void softDeletedVersionRetainsItsMetadataAndDeletionMarker() {
        file.setDeleted(true);
        var row = summary();
        enricher.enrich(row, "dcc-controlled-file-upload");
        assertTrue(row.getBusinessDeleted());
        assertEquals("文件正文.docx", row.getBusinessTitle());
        assertEquals("版本：B/7", row.getBusinessContextTags().get(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "wrongTenant", "wrongId", "missingTitle", "missingCode"})
    void invalidFileBindingCannotLeakOrInventSummary(String failure) {
        switch (failure) {
            case "missing" -> when(files.selectByIdIncludingDeleted(10L)).thenReturn(null);
            case "wrongTenant" -> file.setTenantId(2L);
            case "wrongId" -> file.setId(11L);
            case "missingTitle" -> file.setTitle(" ");
            case "missingCode" -> file.setFileNumber(null);
        }
        assertThrows(IllegalStateException.class, () -> enricher.enrich(summary(), "dcc-controlled-file-upload"));
        verifyNoInteractions(categories);
    }

    @ParameterizedTest @NullSource @ValueSource(strings = {"", "process-uuid", "0", "-1", "99999999999999999999999999"})
    void invalidFileKeyFailsBeforeDataRead(String key) {
        assertThrows(IllegalStateException.class, () -> enricher.enrich(summary().setBusinessKey(key), "dcc-controlled-file-upload"));
        verifyNoInteractions(files, categories);
    }

    @ParameterizedTest @NullSource @ValueSource(strings = {"other-flow", "dcc-controlled-file-obsolete", "dcc-controlled-file-obsolete-approval"})
    void unrelatedAndObsoleteFlowsRemainWithTheirOwnEnricher(String key) {
        enricher.enrich(summary(), key);
        verifyNoInteractions(files, categories);
    }

    @ParameterizedTest @ValueSource(strings = {"BPM_TASK_TODO", "BPM_TASK_HISTORY", "BPM_PROCESS_INSTANCE_COPY"})
    void otherViewsDoNotInterpretTheirBusinessKeyAsAFileId(String source) {
        enricher.enrich(summary().setSourceTaskType(source).setBusinessKey("process-uuid"), "dcc-controlled-file-upload");
        verifyNoInteractions(files, categories);
    }
}
