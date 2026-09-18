package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrOperationAuditEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchTraceOutboxEventDO;
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
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

    @Test
    void persistFailureBoundsReasonAndKeepsFullReasonInPayload() throws Exception {
        TenantContextHolder.setTenantId(7L);
        TransactionSynchronizationManager.initSynchronization();
        try {
            MesProEdhrBatchTraceOutboxEventMapper outboxEventMapper =
                    mock(MesProEdhrBatchTraceOutboxEventMapper.class);
            when(outboxEventMapper.selectByEventId("tx-c-invalid")).thenReturn(null);
            when(outboxEventMapper.selectByIdempotencyKey("tx-c-invalid")).thenReturn(null);
            doAnswer(invocation -> {
                invocation.getArgument(0, MesProEdhrBatchTraceOutboxEventDO.class).setId(303L);
                return 1;
            }).when(outboxEventMapper).insert(any(MesProEdhrBatchTraceOutboxEventDO.class));

            MesProEdhrBatchTraceTxCProducer producer = new MesProEdhrBatchTraceTxCProducer(
                    batchExecutionMapper,
                    operationAuditEventMapper,
                    mock(MesProcessPoolActiveOrderPickListBindingMapper.class),
                    mock(MesProcessPoolActiveOrderPickListBindingItemMapper.class),
                    outboxEventMapper,
                    mock(MesProEdhrBatchTraceabilityService.class),
                    mock(ApplicationEventPublisher.class),
                    mock(PlatformTransactionManager.class),
                    mock(MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort.class),
                    mock(MesIndependentBatchPrerequisiteReceiptPort.class),
                    mock(MesProEdhrBatchProvisioningRecordMapper.class));

            String fullReason = "正式来源映射失败|".repeat(100);
            Method method = MesProEdhrBatchTraceTxCProducer.class
                    .getDeclaredMethod("persistFailure", MesProEdhrBatchTraceTxCCommand.class,
                            String.class, String.class);
            method.setAccessible(true);
            method.invoke(producer, null, "TRACE_SOURCE_CONFLICT", fullReason);

            ArgumentCaptor<MesProEdhrBatchTraceOutboxEventDO> captor =
                    ArgumentCaptor.forClass(MesProEdhrBatchTraceOutboxEventDO.class);
            verify(outboxEventMapper).insert(captor.capture());
            MesProEdhrBatchTraceOutboxEventDO event = captor.getValue();
            assertEquals(fullReason, event.getReason());

            JSONObject payload = JSON.parseObject(event.getPayloadJson());
            assertEquals(fullReason, payload.getString("reason"));
            assertTrue(!payload.containsKey("reasonTruncated"));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TenantContextHolder.clear();
        }
    }
}
