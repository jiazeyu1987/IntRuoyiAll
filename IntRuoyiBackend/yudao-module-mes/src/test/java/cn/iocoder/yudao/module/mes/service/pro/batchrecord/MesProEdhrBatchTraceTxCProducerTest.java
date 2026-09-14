package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrOperationAuditEventDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchProvisioningRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchTraceOutboxEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrOperationAuditEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesIndependentBatchPrerequisiteReceiptPort;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrBatchTraceTxCProducerTest {

    @Mock
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Mock
    private MesProEdhrOperationAuditEventMapper operationAuditEventMapper;

    @Test
    void readFormalInputRejectsAmbiguousSuccessfulProvisionAudit() throws Exception {
        TenantContextHolder.setTenantId(7L);
        try {
            MesProEdhrBatchTraceTxCProducer producer = new MesProEdhrBatchTraceTxCProducer(
                    batchExecutionMapper,
                    operationAuditEventMapper,
                    mock(MesProcessPoolActiveOrderPickListBindingMapper.class),
                    mock(MesProcessPoolActiveOrderPickListBindingItemMapper.class),
                    mock(MesProEdhrBatchTraceOutboxEventMapper.class),
                    mock(MesProEdhrBatchTraceabilityService.class),
                    mock(ApplicationEventPublisher.class),
                    mock(PlatformTransactionManager.class),
                    mock(MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort.class),
                    mock(MesIndependentBatchPrerequisiteReceiptPort.class),
                    mock(MesProEdhrBatchProvisioningRecordMapper.class));
            MesProEdhrBatchExecutionDO batch = new MesProEdhrBatchExecutionDO().setId(101L).setTenantId(7L);
            when(batchExecutionMapper.selectById(101L)).thenReturn(batch);
            MesProEdhrOperationAuditEventDO first = new MesProEdhrOperationAuditEventDO().setMetadataJson("{");
            MesProEdhrOperationAuditEventDO second = new MesProEdhrOperationAuditEventDO().setMetadataJson("{");
            when(operationAuditEventMapper.selectSuccessfulListByBatchExecutionIdAndOperation(
                    anyLong(), eq("OPEN")))
                    .thenReturn(List.of(first, second));

            MesProEdhrBatchTraceTxCCommand command = new MesProEdhrBatchTraceTxCCommand()
                    .setBatchExecutionId(101L)
                    .setProvisioningReceiptId(202L)
                    .setEventId("event-1")
                    .setIdempotencyKey("key-1");

            Method method = MesProEdhrBatchTraceTxCProducer.class
                    .getDeclaredMethod("readFormalInput", MesProEdhrBatchTraceTxCCommand.class);
            method.setAccessible(true);
            InvocationTargetException exception = assertThrows(InvocationTargetException.class, () ->
                    method.invoke(producer, command));

            assertEquals("Flow 6 successful provision audit is ambiguous", exception.getCause().getMessage());
        } finally {
            TenantContextHolder.clear();
        }
    }
}
