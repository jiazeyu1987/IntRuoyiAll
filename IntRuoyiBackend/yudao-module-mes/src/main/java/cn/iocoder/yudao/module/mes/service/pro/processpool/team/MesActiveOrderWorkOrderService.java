package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@RequiredArgsConstructor
public class MesActiveOrderWorkOrderService {
    private final MesProcessPoolActiveOrderMapper orders;
    private final MesProWorkOrderMapper workOrders;
    private final MesMdItemMapper items;
    private final MesProcessPoolTeamMaintenanceAuditMapper audits;

    public MesProWorkOrderRespVO detail(Long actorId, Long activeOrderId) {
        var order = requireOwner(actorId, orders.selectById(activeOrderId));
        var workOrder = requireWorkOrder(workOrders.selectById(order.getWorkOrderId()));
        var result = BeanUtils.toBean(workOrder, MesProWorkOrderRespVO.class);
        result.setProductSpecification(workOrder.getMaterialSpecification());
        var item = items.selectById(workOrder.getProductId());
        if (item == null) throw invalidParamException("生产订单关联的产品不存在");
        result.setProductCode(item.getCode()).setProductName(item.getName());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void fillBatchCode(Long actorId, Long activeOrderId, String batchCode) {
        if (StrUtil.isBlank(batchCode) || batchCode.trim().length() > 64) {
            throw invalidParamException("批次号不能为空且不能超过64个字符");
        }
        var order = requireOwner(actorId, orders.selectByIdForUpdate(activeOrderId));
        // Lock the work order as well: the same work order can appear in multiple active orders.
        var workOrder = requireWorkOrder(workOrders.selectByIdForUpdate(order.getWorkOrderId()));
        if (StrUtil.isNotBlank(workOrder.getBatchCode())) {
            throw invalidParamException("该订单已有批次号，请刷新查看，不允许覆盖");
        }
        String value = batchCode.trim();
        var update = new MesProWorkOrderDO();
        update.setId(workOrder.getId());
        update.setBatchCode(value);
        if (workOrders.updateById(update) != 1) throw invalidParamException("批次号保存失败");
        TeamMaintenanceAuditSupport.insertAudit(audits, actorId, actorId, "FILL_WORK_ORDER_BATCH_CODE",
                "WORK_ORDER", workOrder.getId(), "SUCCESS", "补填生产订单批次号",
                workOrder.getBatchCode(), value);
    }

    private MesProcessPoolActiveOrderDO requireOwner(Long actorId, MesProcessPoolActiveOrderDO order) {
        if (actorId == null || order == null || !Objects.equals(actorId, order.getLeaderUserId())
                || !"ACTIVE".equals(order.getActiveStatus())) {
            throw invalidParamException("活跃订单不存在或无权操作");
        }
        return order;
    }

    private MesProWorkOrderDO requireWorkOrder(MesProWorkOrderDO order) {
        if (order == null) throw invalidParamException("生产订单不存在");
        return order;
    }
}
