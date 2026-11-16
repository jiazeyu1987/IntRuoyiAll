package cn.iocoder.yudao.module.mes.service.pro.handoff;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.MesActiveOrderHandoffTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/** A reminder preserves the original QA task and never creates another approval. */
@Service
public class MesActiveOrderHandoffOverdueService {
    @Resource private MesActiveOrderHandoffTaskMapper tasks;
    @Resource private MesProWorkOrderMapper workOrders;
    @Resource private NotifyMessageSendApi notifyApi;
    @Resource private cn.iocoder.yudao.module.system.api.user.AdminUserApi users;

    @Transactional(rollbackFor = Exception.class)
    public int process(LocalDateTime now, int limit) {
        if (now == null || limit <= 0) throw new IllegalArgumentException("超期扫描时间及数量无效");
        Long tenant = TenantContextHolder.getRequiredTenantId();
        int count = 0;
        for (var task : tasks.due(tenant, now, limit)) {
            MesActiveOrderHandoffContract.validate(task);
            if (!Objects.equals(tenant, task.getTenantId())) throw new IllegalStateException("交接超期扫描租户不一致");
            var order = workOrders.selectById(task.getWorkOrderId());
            if (order == null || !Objects.equals(tenant, order.getTenantId())) throw new IllegalStateException("交接超期提醒缺少同租户正式工单");
            for (Long receiver : MesActiveOrderHandoffContract.candidates(task.getCandidateUserSnapshot())) {
                var user = users.getUser(receiver);
                if (user == null || !Objects.equals(user.getTenantId(), tenant)
                        || !cn.iocoder.yudao.framework.common.enums.CommonStatusEnum.isEnable(user.getStatus()))
                    throw new IllegalStateException("交接超期责任快照中的人员无效或不属于当前租户");
                var request = new NotifySendSingleToUserReqDTO();
                request.setUserId(receiver);
                request.setTemplateCode("MES_EDHR_WORK_TASK_OVERDUE");
                request.setTemplateParams(Map.of("workOrderCode", order.getCode(), "batchCode", Objects.toString(order.getBatchCode(), ""),
                        "processName", MesActiveOrderHandoffContract.label(task.getTaskType()),
                        "dueTime", task.getDueTime().toString(), "actionUrl", task.getActionUrl(), "workTaskId", task.getId()));
                Long messageId = notifyApi.sendSingleMessageToAdmin(request);
                if (messageId == null || messageId <= 0)
                    throw new IllegalStateException("QA交接超期提醒未取得有效站内信回执，任务=" + task.getId() + "，接收人=" + receiver);
            }
            if (tasks.markOverdueNotified(tenant, task.getId(), now) != 1) throw new IllegalStateException("交接超期通知状态已变化");
            count++;
        }
        return count;
    }
}
