package cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.productionrelease.MesReleaseTaskNotifyDeliveryDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.productionrelease.MesReleaseTaskNotifyDeliveryMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MesReleaseTaskNotificationService {
    @Resource private MesReleaseTaskNotifyDeliveryMapper deliveryMapper;
    @Resource private PermissionApi permissionApi;
    @Resource private MesProEdhrWorkTaskMapper workTaskMapper;
    @Resource private cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesMarketReleaseAccess marketReleaseAccess;

    public boolean canRetry(Long actor,MesReleaseTaskNotifyDeliveryDO row) {
        return false; // eDHR inbox delivery has been retired; historical receipts remain readable.
    }

    public List<MesReleaseTaskNotifyDeliveryDO> listOwnPendingOutcomes(Long tenantId, Long actorId) {
        MesReleaseTaskNotificationContract.tenant(tenantId);
        if (actorId == null || actorId <= 0
                || !permissionApi.hasAnyPermissions(actorId, MesReleaseTaskNotificationContract.QUERY_PERMISSION)) {
            throw new cn.iocoder.yudao.framework.common.exception.ServiceException(403, "没有本人放行结果通知查询权限");
        }
        return deliveryMapper.selectOwnPendingOutcomes(tenantId, actorId);
    }

    public List<MesReleaseTaskNotifyDeliveryDO> listForTask(Long tenantId, Long actorId, Long workTaskId) {
        MesReleaseTaskNotificationContract.tenant(tenantId);
        var deliveries = deliveryMapper.selectTaskDeliveries(tenantId, workTaskId);
        var ownOutcomes = deliveries.stream().filter(row -> MesReleaseTaskNotificationContract.outcome(row.getEventType())
                && java.util.Objects.equals(row.getUserId(), actorId)).toList();
        if (!ownOutcomes.isEmpty() && permissionApi.hasAnyPermissions(actorId, MesReleaseTaskNotificationContract.QUERY_PERMISSION)) {
            return ownOutcomes;
        }
        MesProEdhrWorkTaskDO task = workTaskMapper.selectById(workTaskId);
        if(task!=null && task.getBatchExecutionId()!=null) marketReleaseAccess.requireReadable(task.getBatchExecutionId());
        boolean original=task!=null && task.getBatchExecutionId()!=null && marketReleaseAccess.get(task.getBatchExecutionId(),actorId).original();
        if(!original || !permissionApi.hasAnyPermissions(actorId,MesReleaseTaskNotificationContract.QUERY_PERMISSION))
            MesReleaseTaskNotificationContract.access(permissionApi, tenantId, actorId, task);
        return deliveries.stream().filter(row -> !MesReleaseTaskNotificationContract.outcome(row.getEventType())).toList();
    }
}
