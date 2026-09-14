package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationImpactTaskDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationDeliveryDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationFollowupBatchMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationImpactTaskMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationDeliveryMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

class DccPublicationFollowupStatusServiceTest extends BaseMockitoUnitTest {
    @Mock private DccPublicationFollowupBatchMapper batchMapper;
    @Mock private DccPublicationNotificationDeliveryMapper deliveryMapper;
    @Mock private DccPublicationImpactTaskMapper impactTaskMapper;
    @InjectMocks private DccPublicationFollowupStatusService service;

    @Test
    void refreshBatchStatus_derivesProcessingPartialReadyAndCompletedFromChildFacts() {
        when(batchMapper.updateStatus(eq(1L), eq(7L), anyString())).thenReturn(1);
        when(batchMapper.selectByIdAndTenantForUpdate(1L, 7L)).thenReturn(
                cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationFollowupBatchDO.builder().id(7L).build());
        when(impactTaskMapper.selectListByBatchIdForUpdate(1L, 7L)).thenReturn(List.of(task("NOT_STARTED")));
        when(deliveryMapper.selectListByBatchIdForUpdate(1L, 7L)).thenReturn(List.of(delivery("PENDING")));
        service.refreshBatchStatus(1L, 7L);
        verify(batchMapper).updateStatus(1L, 7L, "PROCESSING");

        when(deliveryMapper.selectListByBatchIdForUpdate(1L, 7L)).thenReturn(List.of(delivery("FAILED")));
        service.refreshBatchStatus(1L, 7L);
        verify(batchMapper).updateStatus(1L, 7L, "PARTIAL_FAILED");

        when(deliveryMapper.selectListByBatchIdForUpdate(1L, 7L)).thenReturn(List.of(delivery("SENT")));
        service.refreshBatchStatus(1L, 7L);
        verify(batchMapper).updateStatus(1L, 7L, "READY");

        when(impactTaskMapper.selectListByBatchIdForUpdate(1L, 7L)).thenReturn(List.of(task("RESOLVED")));
        service.refreshBatchStatus(1L, 7L);
        verify(batchMapper).updateStatus(1L, 7L, "COMPLETED");
    }

    private DccPublicationNotificationDeliveryDO delivery(String status) {
        return DccPublicationNotificationDeliveryDO.builder().status(status).build();
    }

    private DccPublicationImpactTaskDO task(String trackingStatus) {
        return DccPublicationImpactTaskDO.builder().revisionTrackingStatus(trackingStatus).build();
    }
}
