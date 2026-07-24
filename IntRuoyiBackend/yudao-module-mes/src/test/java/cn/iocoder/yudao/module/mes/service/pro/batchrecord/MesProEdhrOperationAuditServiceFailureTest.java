package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrOperationAuditEventDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrOperationAuditEventMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditErrorCodeConstants.PRO_EDHR_OPERATION_AUDIT_WRITE_FAILED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class MesProEdhrOperationAuditServiceFailureTest extends BaseMockitoUnitTest {

    @Mock
    private MesProEdhrOperationAuditEventMapper auditEventMapper;
    @InjectMocks
    private MesProEdhrOperationAuditServiceImpl auditService;

    @Test
    void record_mapperFailureReturnsExplicitAuditWriteError() {
        when(auditEventMapper.selectListByObject("BATCH_EXECUTION", "1001")).thenReturn(List.of());
        doThrow(new IllegalStateException("db unavailable")).when(auditEventMapper).insert(any(MesProEdhrOperationAuditEventDO.class));

        assertServiceException(() -> auditService.record(new MesProEdhrOperationAuditCommand()
                        .setRequestId("req-write-fail")
                        .setObjectType("BATCH_EXECUTION")
                        .setObjectId("1001")
                        .setOperationType("SAVE")
                .setActorUserId(188L)
                        .setPermissionDecision("ALLOW")
                        .setResultStatus("SUCCESS")),
                PRO_EDHR_OPERATION_AUDIT_WRITE_FAILED, "db unavailable");
    }
}
