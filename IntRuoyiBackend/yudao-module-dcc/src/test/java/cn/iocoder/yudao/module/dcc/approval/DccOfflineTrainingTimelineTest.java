package cn.iocoder.yudao.module.dcc.approval;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.*;
import cn.iocoder.yudao.module.bpm.approval.service.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.service.file.DccOfflineTrainingRecordService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccOfflineTrainingTimelineTest {
    private final DccOfflineTrainingRecordService training = mock(DccOfflineTrainingRecordService.class);
    private final DccOfflineTrainingTaskDelegate delegate = new DccOfflineTrainingTaskDelegate(training);
    private DccControlledFileDO file() {
        TenantContextHolder.setTenantId(1L);
        var file = DccControlledFileDO.builder().id(10L).processInstanceId("round").requesterId(99L)
                .title("培训文件").fileNumber("RAIN").versionNo("R/3").status(DccOfflineTrainingRecordService.STATUS).build();
        file.setTenantId(1L);
        return file;
    }
    private ApprovalTaskTimelineQueryContext context() {
        return ApprovalTaskTimelineQueryContext.of(99L, ApprovalModuleCode.DCC,
                DccOfflineTrainingRecordService.SOURCE_TYPE,"DCC_OFFLINE_TRAINING_RECORD:1:10:round","10","round");
    }
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void trainingSummaryDeclaresTimelineAndUsesTheRealReceiveExecution() {
        var file=file();
        when(training.listForActor(99L)).thenReturn(List.of(file));
        when(training.canUpload(99L,file)).thenReturn(true);
        when(training.requireWaiting(file)).thenReturn(new DccOfflineTrainingRecordService.Waiting(file,"round","execution-1"));
        var summary=delegate.list(ApprovalTaskQueryContext.of(99L,ApprovalTaskViewType.TODO,ApprovalModuleCode.DCC,null,1,10)).get(0);
        assertEquals(java.util.Set.of(ApprovalTaskCapability.TIMELINE),summary.getCapabilities());
        var entry=delegate.trainingTimelineEntry(context(),file);
        assertEquals("execution-1",entry.getId()); assertEquals("TRAINING",entry.getNodeCode());
        assertEquals("CURRENT",entry.getAction()); assertNull(entry.getActedAt());
        assertEquals("FLOWABLE_EXECUTION",entry.getEvidenceType());
    }

    @ParameterizedTest @ValueSource(strings={"source","round","file","tenant","actor","global"})
    void fabricatedOrRevokedTrainingDutyCannotReadTimeline(String changed) {
        var file=file(); var context=context();
        switch(changed) {
            case "source" -> context.setSourceTaskId("DCC_OFFLINE_TRAINING_RECORD:1:10:earlier");
            case "round" -> context.setProcessInstanceId("earlier");
            case "file" -> context.setBusinessKey("11");
            case "tenant" -> TenantContextHolder.setTenantId(2L);
            case "actor" -> context.setLoginUserId(88L);
            case "global" -> context.setGlobalView(true);
        }
        assertThrows(IllegalStateException.class,()->delegate.trainingTimelineEntry(context,file));
        verify(training,never()).requireWaiting(file);
    }

    @Test void confirmedOrganizerCanReadTheSameLiveTrainingTimelineWithoutUploadDuty() {
        var file=file();
        when(training.canOrganizeTraining(99L,file)).thenReturn(true);
        when(training.requireWaiting(file)).thenReturn(new DccOfflineTrainingRecordService.Waiting(file,"round","execution-2"));
        assertEquals("execution-2",delegate.trainingTimelineEntry(context(),file).getId());
        assertEquals(99L,delegate.trainingTimelineEntry(context(),file).getActorUserId());
    }
}
