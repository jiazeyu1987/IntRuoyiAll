package cn.iocoder.yudao.module.dcc.approval;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccObsoleteApprovalSummaryEnricherTest {
    @Test void formalObsoleteUsesFileSummaryAndDetailOnlyWhileKeepingNativeTaskIdentity() {
        TenantContextHolder.setTenantId(1L);
        try {
            var forms=mock(FormActionInstanceMapper.class);var files=mock(DccControlledFileMapper.class);
            var adapter=mock(DccApprovalTaskAdapter.class);
            var enricher=new DccObsoleteApprovalSummaryEnricher(forms,files,adapter);
            var summary=ApprovalTaskSummary.builder().id("BPM:TODO:t1").moduleCode(ApprovalModuleCode.BPM)
                    .sourceTaskType("BPM_TASK_TODO").sourceTaskId("t1").businessKey("process-1")
                    .processInstanceId("process-1").currentNodeName("部门负责人").availableActions(Set.of("APPROVE","REJECT"))
                    .detailRoute("/bpm/process-instance/detail")
                    .detailQuery(Map.of("id", "process-1", "taskId", "t1")).build();
            var form=FormActionInstanceDO.builder().id(3L).tenantId(1L).bpmProcessInstanceId("process-1")
                    .systemCode("DCC").objectType("CONTROLLED_FILE").actionCode("OBSOLETE").objectId("10").objectVersion("A/1").build();
            var file=DccControlledFileDO.builder().id(10L).tenantId(1L).title("SOP正文").fileNumber("SOP-01").versionNo("A/1").build();
            when(forms.selectOwnershipByTenantAndProcessId(1L,"process-1")).thenReturn(List.of(form));
            when(files.selectById(10L)).thenReturn(file);
            when(adapter.buildDccBusinessContextTags(file,"部门负责人")).thenReturn(List.of("文件编号：SOP-01","版本：A/1","分类：SOP"));
            enricher.enrich(summary,"dcc-controlled-file-obsolete");
            assertEquals("SOP正文",summary.getBusinessTitle());assertEquals("SOP-01",summary.getBusinessCode());
            assertEquals(Set.of("OPEN_DETAIL"),summary.getAvailableActions());
            assertEquals("BPM_TASK_TODO",summary.getSourceTaskType());assertEquals("t1",summary.getSourceTaskId());
            assertEquals("process-1",summary.getBusinessKey());assertEquals("/bpm/process-instance/detail",summary.getDetailRoute());
            assertEquals(summary.getDetailRoute(), summary.getDecisionDetailRoute());
            assertEquals(Map.of("id", "process-1", "taskId", "t1"), summary.getDecisionDetailQuery());
            file.setTenantId(2L);
            assertThrows(IllegalStateException.class,()->enricher.enrich(summary,"dcc-controlled-file-obsolete"));
            file.setTenantId(1L);form.setObjectVersion("A/2");
            assertThrows(IllegalStateException.class,()->enricher.enrich(summary,"dcc-controlled-file-obsolete"));
            clearInvocations(forms,files);
            enricher.enrich(summary,"another-business-flow");enricher.enrich(summary,null);
            verifyNoInteractions(forms,files);
        } finally { TenantContextHolder.clear(); }
    }
}
