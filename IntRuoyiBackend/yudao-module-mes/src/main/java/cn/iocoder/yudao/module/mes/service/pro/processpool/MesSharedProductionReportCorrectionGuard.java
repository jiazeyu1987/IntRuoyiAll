package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationReleaseStateService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.*;

/** Shared report bytes are evidence for the source and every positive CURRENT consumer. */
@Component
public class MesSharedProductionReportCorrectionGuard {
    @Resource private MesProcessPoolReportAllocationMapper allocations;
    @Resource private MesProcessPoolActiveOrderMapper orders;
    @Resource private MesProcessPoolActiveOrderCompletionReceiptMapper receipts;
    @Resource private MesReportAllocationReleaseStateService releases;
    @Resource private MesProEdhrNonconformanceReviewService freezes;

    public void assertEditable(MesProProcessPoolEventDO event) {
        Long tenant=TenantContextHolder.getTenantId();
        require(event!=null&&Objects.equals(tenant,event.getTenantId()),"共享报工来源租户不一致");
        var raw=JsonUtils.parseTree(event.getRawPayload());
        var sourceId=raw.get("activeOrderId");
        require(sourceId!=null&&sourceId.isIntegralNumber()&&sourceId.canConvertToLong()&&sourceId.asLong()>0,"共享报工缺少正式来源活跃订单");
        var current=allocations.selectListByEventIdForUpdate(event.getId());
        require(current!=null,"共享报工分配查询失败");
        var ids=new TreeSet<Long>();ids.add(sourceId.asLong());
        var positive=new ArrayList<MesProcessPoolReportAllocationDO>();
        for(var allocation:current) {
            require(allocation!=null&&Objects.equals(tenant,allocation.getTenantId())
                    &&Objects.equals(event.getId(),allocation.getEventId())
                    &&MesProcessPoolReportAllocationDO.LIFECYCLE_CURRENT.equals(allocation.getLifecycleStatus())
                    &&allocation.getAllocatedQuantity()!=null&&allocation.getAllocatedQuantity().compareTo(BigDecimal.ZERO)>=0,
                    "共享报工分配来源不一致");
            if(allocation.getAllocatedQuantity().signum()>0) {
                require(allocation.getActiveOrderId()!=null&&allocation.getActiveOrderId()>0,"共享报工目标活跃订单缺失");
                ids.add(allocation.getActiveOrderId());positive.add(allocation);
            }
        }
        var locked=new HashMap<Long,MesProcessPoolActiveOrderDO>();
        for(Long id:ids) {
            var order=orders.selectByIdForUpdate(id);
            require(order!=null&&Objects.equals(tenant,order.getTenantId())&&Objects.equals(id,order.getId()),"共享报工活跃订单身份不一致");
            require("ACTIVE".equals(order.getActiveStatus())&&"ACTIVE".equals(order.getBusinessStatus())
                    &&receipts.selectByActiveOrderIdForUpdate(id)==null,"共享报工已被完工或关闭订单消费，不能修改正文");
            freezes.ensureWorkOrderNotFrozen(order.getWorkOrderId(),"共享报工正文更正");locked.put(id,order);
        }
        var source=locked.get(sourceId.asLong());
        require(Objects.equals(event.getWorkOrderId(),source.getWorkOrderId())&&Objects.equals(event.getRouteId(),source.getRouteId()),
                "共享报工来源工单或路线不一致");
        for(var allocation:positive) {
            var target=locked.get(allocation.getActiveOrderId());
            require(Objects.equals(target.getWorkOrderId(),allocation.getWorkOrderId())
                    &&Objects.equals(event.getRouteId(),target.getRouteId())
                    &&Objects.equals(event.getRouteProcessId(),allocation.getRouteProcessId())
                    &&Objects.equals(event.getProcessId(),allocation.getProcessId()),"共享报工目标分配身份不一致");
        }
        require(releases.findReleaseApplicationLockedActiveOrderIdsForUpdate(ids).isEmpty(),"共享报工已有生产放行申请，不能修改正文");
    }
    private static void require(boolean valid,String message){if(!valid)throw new IllegalStateException(message);}
}
