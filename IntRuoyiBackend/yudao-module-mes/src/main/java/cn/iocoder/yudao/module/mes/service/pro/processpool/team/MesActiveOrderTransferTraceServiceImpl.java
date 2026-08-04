package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderTransferTraceDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderTransferTraceMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.transfer.MesWmTransferDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.transfer.MesWmTransferDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.transfer.MesWmTransferLineDO;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.transfer.MesWmTransferDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.transfer.MesWmTransferLineMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.transfer.MesWmTransferMapper;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.WM_TRANSFER_DETAIL_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.WM_TRANSFER_LINE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.WM_TRANSFER_NO_LINE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.WM_TRANSFER_NOT_EXISTS;

@Service
public class MesActiveOrderTransferTraceServiceImpl implements MesActiveOrderTransferTraceService {

    private final MesProcessPoolActiveOrderTransferTraceMapper transferTraceMapper;
    private final MesWmTransferMapper transferMapper;
    private final MesWmTransferLineMapper transferLineMapper;
    private final MesWmTransferDetailMapper transferDetailMapper;

    public MesActiveOrderTransferTraceServiceImpl(MesProcessPoolActiveOrderTransferTraceMapper transferTraceMapper,
                                                  MesWmTransferMapper transferMapper,
                                                  MesWmTransferLineMapper transferLineMapper,
                                                  MesWmTransferDetailMapper transferDetailMapper) {
        this.transferTraceMapper = transferTraceMapper;
        this.transferMapper = transferMapper;
        this.transferLineMapper = transferLineMapper;
        this.transferDetailMapper = transferDetailMapper;
    }

    @Override
    public MesProcessPoolActiveOrderTransferTraceDO recordTransferTrace(MesProcessPoolActiveOrderTransferTraceDO trace) {
        Objects.requireNonNull(trace, "trace");
        String idempotencyKey = trace.getIdempotencyKey();
        if (StrUtil.isBlank(idempotencyKey)) {
            throw new IllegalArgumentException("active order transfer trace idempotencyKey is required");
        }
        MesProcessPoolActiveOrderTransferTraceDO existing =
                transferTraceMapper.selectByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return existing;
        }
        try {
            transferTraceMapper.insert(trace);
            return trace;
        } catch (DuplicateKeyException ex) {
            MesProcessPoolActiveOrderTransferTraceDO concurrentExisting =
                    transferTraceMapper.selectByIdempotencyKey(idempotencyKey);
            if (concurrentExisting != null) {
                return concurrentExisting;
            }
            throw ex;
        }
    }

    @Override
    public List<MesProcessPoolActiveOrderTransferTraceDO> recordTransferTracesForActiveOrder(
            MesProcessPoolActiveOrderDO activeOrder, List<Long> transferIds) {
        Objects.requireNonNull(activeOrder, "activeOrder");
        if (transferIds == null || transferIds.isEmpty()) {
            return List.of();
        }
        requireActiveOrderIdentity(activeOrder);
        Set<Long> distinctTransferIds = new LinkedHashSet<>(transferIds);
        List<MesProcessPoolActiveOrderTransferTraceDO> traces = new ArrayList<>();
        for (Long transferId : distinctTransferIds) {
            if (transferId == null) {
                throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "transferId");
            }
            traces.addAll(recordTransfer(activeOrder, transferId));
        }
        return traces;
    }

    @Override
    public List<MesProcessPoolActiveOrderTransferTraceDO> listByActiveOrder(Long activeOrderId) {
        return transferTraceMapper.selectListByActiveOrderId(activeOrderId);
    }

    @Override
    public List<MesProcessPoolActiveOrderTransferTraceDO> listByActiveOrderAndSourceTypes(
            Long activeOrderId, Collection<String> sourceTypes) {
        return transferTraceMapper.selectListByActiveOrderIdAndSourceTypes(activeOrderId, sourceTypes);
    }

    private List<MesProcessPoolActiveOrderTransferTraceDO> recordTransfer(MesProcessPoolActiveOrderDO activeOrder,
                                                                         Long transferId) {
        MesWmTransferDO transfer = transferMapper.selectById(transferId);
        if (transfer == null) {
            throw exception(WM_TRANSFER_NOT_EXISTS);
        }
        List<MesWmTransferLineDO> lines = transferLineMapper.selectListByTransferId(transferId);
        if (lines == null || lines.isEmpty()) {
            throw exception(WM_TRANSFER_NO_LINE);
        }
        List<MesWmTransferDetailDO> details = transferDetailMapper.selectListByTransferId(transferId);
        if (details == null || details.isEmpty()) {
            throw exception(WM_TRANSFER_DETAIL_NOT_EXISTS);
        }
        Map<Long, MesWmTransferLineDO> lineById = new LinkedHashMap<>();
        for (MesWmTransferLineDO line : lines) {
            if (line != null && line.getId() != null) {
                lineById.put(line.getId(), line);
            }
        }
        List<MesProcessPoolActiveOrderTransferTraceDO> traces = new ArrayList<>();
        for (MesWmTransferDetailDO detail : details) {
            MesWmTransferLineDO line = requireLineForDetail(detail, lineById);
            traces.add(recordTransferTrace(toTransferTrace(activeOrder, transfer, line, detail)));
        }
        return traces;
    }

    private static void requireActiveOrderIdentity(MesProcessPoolActiveOrderDO activeOrder) {
        if (activeOrder.getId() == null || activeOrder.getWorkOrderId() == null
                || activeOrder.getRouteId() == null || activeOrder.getRouteVersionId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrder.transferTrace");
        }
    }

    private static MesWmTransferLineDO requireLineForDetail(MesWmTransferDetailDO detail,
                                                           Map<Long, MesWmTransferLineDO> lineById) {
        if (detail == null || detail.getId() == null || detail.getLineId() == null) {
            throw exception(WM_TRANSFER_LINE_NOT_EXISTS);
        }
        MesWmTransferLineDO line = lineById.get(detail.getLineId());
        if (line == null) {
            throw exception(WM_TRANSFER_LINE_NOT_EXISTS);
        }
        return line;
    }

    private static MesProcessPoolActiveOrderTransferTraceDO toTransferTrace(
            MesProcessPoolActiveOrderDO activeOrder,
            MesWmTransferDO transfer,
            MesWmTransferLineDO line,
            MesWmTransferDetailDO detail) {
        BigDecimal quantity = detail.getQuantity() != null ? detail.getQuantity() : line.getQuantity();
        Long itemId = detail.getItemId() != null ? detail.getItemId() : line.getItemId();
        Long batchId = detail.getBatchId() != null ? detail.getBatchId() : line.getBatchId();
        if (quantity == null || line.getMaterialStockId() == null || itemId == null || batchId == null
                || transfer.getStatus() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrder.transferTrace.sourceFields");
        }
        return MesProcessPoolActiveOrderTransferTraceDO.builder()
                .activeOrderId(activeOrder.getId())
                .workOrderId(activeOrder.getWorkOrderId())
                .routeId(activeOrder.getRouteId())
                .routeVersionId(activeOrder.getRouteVersionId())
                .sourceType(MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_TRANSFER)
                .direction("OUT")
                .transferId(transfer.getId())
                .transferLineId(line.getId())
                .transferDetailId(detail.getId())
                .materialStockId(line.getMaterialStockId())
                .batchId(batchId)
                .itemId(itemId)
                .quantity(quantity)
                .sourceObjectType("WM_TRANSFER_DETAIL")
                .sourceObjectId(String.valueOf(detail.getId()))
                .sourceObjectCode(transfer.getCode())
                .sourceStatus(String.valueOf(transfer.getStatus()))
                .sourceOccurredAt(transfer.getTransferDate())
                .idempotencyKey(toIdempotencyKey(activeOrder, transfer, line, detail))
                .sourceSnapshotJson(toSourceSnapshotJson(transfer, line, detail, quantity, itemId, batchId))
                .build();
    }

    private static String toIdempotencyKey(MesProcessPoolActiveOrderDO activeOrder, MesWmTransferDO transfer,
                                           MesWmTransferLineDO line, MesWmTransferDetailDO detail) {
        return "active-order-" + activeOrder.getId()
                + "-transfer-" + transfer.getId()
                + "-line-" + line.getId()
                + "-detail-" + detail.getId();
    }

    private static String toSourceSnapshotJson(MesWmTransferDO transfer, MesWmTransferLineDO line,
                                               MesWmTransferDetailDO detail, BigDecimal quantity,
                                               Long itemId, Long batchId) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("transferId", transfer.getId());
        snapshot.put("transferCode", transfer.getCode());
        snapshot.put("transferStatus", transfer.getStatus());
        snapshot.put("transferDate", transfer.getTransferDate());
        snapshot.put("transferLineId", line.getId());
        snapshot.put("transferDetailId", detail.getId());
        snapshot.put("materialStockId", line.getMaterialStockId());
        snapshot.put("itemId", itemId);
        snapshot.put("batchId", batchId);
        snapshot.put("quantity", quantity);
        return JsonUtils.toJsonString(snapshot);
    }
}
