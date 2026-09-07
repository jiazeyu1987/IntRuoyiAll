package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationImpactTaskDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationDeliveryDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationFollowupBatchMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationImpactTaskMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationDeliveryMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class DccPublicationFollowupStatusService {
    @Resource private DccPublicationFollowupBatchMapper batchMapper;
    @Resource private DccPublicationNotificationDeliveryMapper deliveryMapper;
    @Resource private DccPublicationImpactTaskMapper impactTaskMapper;

    public void refreshBatchStatus(Long tenantId, Long batchId) {
        lockBatch(tenantId, batchId);
        List<DccPublicationNotificationDeliveryDO> deliveries = Objects.requireNonNull(
                deliveryMapper.selectListByBatchIdForUpdate(tenantId, batchId),
                "notification deliveries must not be null");
        List<DccPublicationImpactTaskDO> tasks = Objects.requireNonNull(
                impactTaskMapper.selectListByBatchIdForUpdate(tenantId, batchId),
                "impact tasks must not be null");
        String status;
        if (deliveries.stream().anyMatch(row -> "FAILED".equals(row.getStatus()))) {
            status = "PARTIAL_FAILED";
        } else if (deliveries.stream().anyMatch(row -> "PENDING".equals(row.getStatus()))) {
            status = "PROCESSING";
        } else {
            boolean allImpactResolved = tasks.stream().allMatch(this::isImpactResolved);
            status = allImpactResolved ? "COMPLETED" : "READY";
        }
        if (batchMapper.updateStatus(tenantId, batchId, status) != 1) {
            throw new IllegalStateException("Publication follow-up batch status update failed");
        }
    }

    public void lockBatch(Long tenantId, Long batchId) {
        if (batchMapper.selectByIdAndTenantForUpdate(tenantId, batchId) == null) {
            throw new IllegalStateException("Publication follow-up batch not found during status refresh");
        }
    }

    private boolean isImpactResolved(DccPublicationImpactTaskDO task) {
        return "NOT_APPLICABLE".equals(task.getRevisionTrackingStatus())
                && "COMPLETED".equals(task.getTaskStatus())
                || "RESOLVED".equals(task.getRevisionTrackingStatus());
    }
}
