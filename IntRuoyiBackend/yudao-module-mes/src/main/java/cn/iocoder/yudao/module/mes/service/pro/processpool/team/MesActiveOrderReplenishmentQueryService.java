package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionReplenishmentListDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING;

/** Order-level replenishment documents for display and completion evidence; no process allocation. */
@Service
public class MesActiveOrderReplenishmentQueryService {
    private final MesProcessPoolActiveOrderMapper orders;
    private final MesProWorkOrderMapper workOrders;
    private final ErpKingdeeProductionReplenishmentListItemMapper items;
    private final ErpKingdeeProductionReplenishmentListMapper headers;
    private final MesPackagingLeaderAccess packagingAccess;

    public MesActiveOrderReplenishmentQueryService(MesProcessPoolActiveOrderMapper orders,
            MesProWorkOrderMapper workOrders, ErpKingdeeProductionReplenishmentListItemMapper items,
            ErpKingdeeProductionReplenishmentListMapper headers, MesPackagingLeaderAccess packagingAccess) {
        this.orders = orders;
        this.workOrders = workOrders;
        this.items = items;
        this.headers = headers;
        this.packagingAccess = packagingAccess;
    }

    public record Line(String itemId, String documentId, String billNo, String productionOrderNo,
                       String materialCode, String materialName, String lotNumber,
                       BigDecimal actualQuantity, String documentStatus, String sourceHash) {}

    @Transactional(readOnly = true)
    public List<Line> list(Long actorId, Long activeOrderId) {
        var order = orders.selectById(activeOrderId);
        if (order == null || !(Objects.equals(order.getLeaderUserId(), actorId)
                || packagingAccess.canAccess(actorId, order))) {
            throw invalid(activeOrderId, "无权查看该订单的补料单");
        }
        return readSources(order.getWorkOrderId());
    }

    public List<Line> readSources(Long workOrderId) {
        var workOrder = workOrders.selectById(workOrderId);
        if (workOrder == null || StrUtil.isBlank(workOrder.getCode())) {
            throw invalid(workOrderId, "生产工单或订单编号不存在");
        }
        var sourceItems = items.selectListByProductionOrderNo(workOrder.getCode());
        if (sourceItems == null) {
            throw new IllegalStateException("生产补料单查询未返回结果集合");
        }
        if (sourceItems.isEmpty()) {
            return List.of();
        }
        if (sourceItems.stream().anyMatch(item -> item == null || item.getId() == null
                || item.getProductionReplenishmentListId() == null
                || !Objects.equals(workOrder.getCode(), item.getProductionOrderNo()))) {
            throw invalid(workOrderId, "补料明细与生产订单身份不一致");
        }
        List<Long> headerIds = sourceItems.stream().map(item -> item.getProductionReplenishmentListId())
                .distinct().sorted().toList();
        var sourceHeaders = headers.selectBatchIds(headerIds);
        if (sourceHeaders == null) {
            throw new IllegalStateException("生产补料单表头查询未返回结果集合");
        }
        Map<Long, ErpKingdeeProductionReplenishmentListDO> byId = sourceHeaders.stream()
                .collect(Collectors.toMap(ErpKingdeeProductionReplenishmentListDO::getId, Function.identity()));
        if (!byId.keySet().containsAll(headerIds)) {
            throw invalid(workOrderId, "补料单表头缺失");
        }
        return sourceItems.stream().sorted(Comparator.comparing(item -> item.getId()))
                .map(item -> {
                    var header = byId.get(item.getProductionReplenishmentListId());
                    String hash = DigestUtil.sha256Hex(JsonUtils.toJsonString(header) + "|" + JsonUtils.toJsonString(item));
                    return new Line(item.getId().toString(), header.getId().toString(), header.getSourceBillNo(),
                            item.getProductionOrderNo(), item.getMaterialNumber(), item.getMaterialName(),
                            item.getLotNumber(), item.getActualQuantity(), header.getDocumentStatus(), hash);
                }).toList();
    }

    private static RuntimeException invalid(Long objectId, String reason) {
        return exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING, objectId, reason);
    }
}
