package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileApplicationEvidenceMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.lang.reflect.Method;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccTrainingApplicationProgressTest {
    @Test void trainingRecipientReadsExactFileRoundTasksWithoutGenericBpmReadGrant() throws Exception {
        Class<?> type = assertDoesNotThrow(() -> Class.forName("cn.iocoder.yudao.module.dcc.service.file.DccTrainingApplicationProgressService"));
        Object service = type.getDeclaredConstructor().newInstance();
        var files = mock(DccControlledFileMapper.class);
        var training = mock(DccOfflineTrainingRecordService.class);
        var mappings = mock(DccControlledFileApplicationEvidenceMapper.class);
        var history = mock(DccApplicationHistoryGuard.class);
        var tasks = mock(BpmTaskService.class);
        // Exact source provenance is the external W05 port; this original suite verifies the narrow read consumer.
        var boundContracts = mock(DccBoundWorkflowContractResolver.class);
        for (var pair : List.of(new Object[]{"files",files}, new Object[]{"training",training},
                new Object[]{"mappings",mappings}, new Object[]{"history",history}, new Object[]{"tasks",tasks},
                new Object[]{"boundContracts",boundContracts}))
            ReflectionTestUtils.setField(service,(String)pair[0],pair[1]);
        var file = DccControlledFileDO.builder().id(10L).tenantId(1L).dccProjectCodeId(5L)
                .processInstanceId("training-round").processDefinitionKey(DccControlledFileProcessDefinitionKeys.REVISION)
                .status(DccOfflineTrainingRecordService.STATUS).build();
        when(files.selectById(10L)).thenReturn(file);
        when(training.canReadProgress(99L,file)).thenReturn(true);
        var mapping = new DccApplicationRoundSummary();mapping.setControlledFileId(10L);
        mapping.setApplicationType("REVISION");mapping.setBpmRound("training-round");mapping.setAttributeRound(1);
        when(mappings.selectApplicationRounds(1L,5L,10L)).thenReturn(List.of(mapping));
        when(boundContracts.resolveRoundFlowContract(file,mapping)).thenReturn(new DccBoundWorkflowContractResolver.RoundFlowContract(
                1L,10L,"REVISION",1,"training-round",file.getProcessDefinitionKey(),"exact-definition","exact-deployment","a".repeat(64),
                "LEGACY_V4_ACTION",DccWorkflowDefinitionCatalog.FlowRule.LEGACY_V4_TRAINING_BEFORE_DOCUMENT_CONTROL,true,
                null,null,DccBoundWorkflowContractResolver.RoundContractStatus.READY,null));
        var review = mock(HistoricTaskInstance.class);
        when(review.getId()).thenReturn("review-1");when(review.getProcessInstanceId()).thenReturn("training-round");
        when(review.getTenantId()).thenReturn("1");when(review.getTaskDefinitionKey()).thenReturn("MATRIX_REVIEW");
        when(review.getTaskLocalVariables()).thenReturn(java.util.Map.of("TASK_STATUS",2));
        when(tasks.getTaskListByProcessInstanceId("training-round",true)).thenReturn(List.of(review));
        Method read = type.getMethod("read",Long.class,Long.class,String.class);
        TenantContextHolder.setTenantId(1L);
        try {
            Object result = read.invoke(service,99L,10L,"training-round");
            assertEquals("10",ReflectionTestUtils.invokeMethod(result,"controlledFileId"));
            assertEquals("training-round",ReflectionTestUtils.invokeMethod(result,"bpmRound"));
            var projected = (List<?>) ReflectionTestUtils.invokeMethod(result,"tasks");
            assertEquals(Integer.valueOf(2),(Integer) ReflectionTestUtils.invokeMethod(projected.get(0),"status"));
            verify(history).require(file,"REVISION","training-round");
            assertThrows(java.lang.reflect.InvocationTargetException.class,()->read.invoke(service,99L,10L,"old-round"));
            when(review.getProcessInstanceId()).thenReturn("other-round");
            assertThrows(java.lang.reflect.InvocationTargetException.class,()->read.invoke(service,99L,10L,"training-round"));
            when(review.getProcessInstanceId()).thenReturn("training-round");
            when(review.getTaskLocalVariables()).thenReturn(java.util.Map.of());
            assertThrows(java.lang.reflect.InvocationTargetException.class,()->read.invoke(service,99L,10L,"training-round"));
            when(review.getTaskLocalVariables()).thenReturn(java.util.Map.of("TASK_STATUS",2));
            when(training.canReadProgress(99L,file)).thenReturn(false);
            assertThrows(java.lang.reflect.InvocationTargetException.class,()->read.invoke(service,99L,10L,"training-round"));
            when(training.canReadProgress(99L,file)).thenReturn(true);file.setTenantId(2L);
            assertThrows(java.lang.reflect.InvocationTargetException.class,()->read.invoke(service,99L,10L,"training-round"));
        } finally { TenantContextHolder.clear(); }
    }
}
