package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_IDEMPOTENCY_CONFLICT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING;

/** Freezes every approved formal pick list belonging to the active order's production order. */
@Service
public class MesTeamLeaderActiveOrderPickListCompletionSourceService {

    private final MesFormalProductionPickListSourceResolver sourceResolver;
    private final MesProcessPoolActiveOrderPickListBindingMapper bindingMapper;
    private final MesProcessPoolActiveOrderPickListBindingItemMapper bindingItemMapper;

    public MesTeamLeaderActiveOrderPickListCompletionSourceService(
            MesFormalProductionPickListSourceResolver sourceResolver,
            MesProcessPoolActiveOrderPickListBindingMapper bindingMapper,
            MesProcessPoolActiveOrderPickListBindingItemMapper bindingItemMapper) {
        this.sourceResolver = sourceResolver;
        this.bindingMapper = bindingMapper;
        this.bindingItemMapper = bindingItemMapper;
    }

    public List<MesProcessPoolActiveOrderPickListBindingDO> freezeAll(
            MesProcessPoolActiveOrderDO activeOrder, Long actorUserId, String completionIdempotencyKey) {
        if (activeOrder == null || activeOrder.getWorkOrderId() == null) {
            throw sourceMissing(activeOrder, "WORK_ORDER_ID_REQUIRED");
        }
        final MesFormalProductionPickListSourceResolver.Resolution resolution;
        try {
            resolution = sourceResolver.resolve(activeOrder.getWorkOrderId());
        } catch (MesFormalProductionPickListSourceException exception) {
            throw sourceMissing(activeOrder, exception.getMessage());
        }
        List<MesFormalProductionPickListSourceResolver.Source> sources = resolution.sources();
        List<MesProcessPoolActiveOrderPickListBindingDO> existing = bindingMapper
                .selectListByActiveOrderId(activeOrder.getId());
        List<MesProcessPoolActiveOrderPickListBindingDO> current = existing == null ? List.of() : List.copyOf(existing);
        validateExistingSet(activeOrder, sources, current, false);
        Map<Long, MesProcessPoolActiveOrderPickListBindingDO> existingByPickListId = current.stream()
                .collect(Collectors.toMap(MesProcessPoolActiveOrderPickListBindingDO::getPickListId,
                        binding -> binding));
        LocalDateTime boundAt = LocalDateTime.now();
        for (MesFormalProductionPickListSourceResolver.Source source : sources) {
            if (existingByPickListId.containsKey(source.header().getId())) {
                continue;
            }
            MesProcessPoolActiveOrderPickListBindingDO binding = MesProcessPoolActiveOrderPickListBindingDO.builder()
                    .id(IdUtil.getSnowflakeNextId()).activeOrderId(activeOrder.getId())
                    .workOrderId(activeOrder.getWorkOrderId()).pickListId(source.header().getId())
                    .sourceFid(source.header().getSourceFid()).sourceBillNo(source.header().getSourceBillNo())
                    .sourceDocumentStatus(source.header().getDocumentStatus())
                    .sourceModifyTime(source.header().getSourceModifyTime()).sourceSnapshotHash(source.hash())
                    .bindingStatus("BOUND").boundBy(actorUserId).boundAt(boundAt)
                    .idempotencyKey(completionIdempotencyKey + ":PICK_LIST:" + source.header().getId())
                    .requestPayloadHash(DigestUtil.sha256Hex(activeOrder.getId() + "|" + source.hash()))
                    .bindingVersion(1).build();
            if (bindingMapper.insert(binding) != 1) {
                throw sourceMissing(activeOrder, "PICK_LIST_BINDING_INSERT_FAILED");
            }
            for (ErpKingdeeProductionPickListItemDO item : source.items()) {
                if (bindingItemMapper.insert(toBindingItem(binding.getId(), item)) != 1) {
                    throw sourceMissing(activeOrder, "PICK_LIST_BINDING_ITEM_INSERT_FAILED");
                }
            }
        }
        List<MesProcessPoolActiveOrderPickListBindingDO> completed = bindingMapper
                .selectListByActiveOrderId(activeOrder.getId());
        validateExistingSet(activeOrder, sources, completed == null ? List.of() : completed, true);
        return List.copyOf(completed);
    }

    private void validateExistingSet(MesProcessPoolActiveOrderDO activeOrder,
                                     List<MesFormalProductionPickListSourceResolver.Source> sources,
                                     List<MesProcessPoolActiveOrderPickListBindingDO> existing,
                                     boolean requireComplete) {
        Map<Long, MesFormalProductionPickListSourceResolver.Source> expected = sources.stream()
                .collect(Collectors.toMap(source -> source.header().getId(), source -> source));
        Map<Long, MesProcessPoolActiveOrderPickListBindingDO> actual = existing.stream().collect(
                Collectors.toMap(MesProcessPoolActiveOrderPickListBindingDO::getPickListId, binding -> binding));
        if (!expected.keySet().containsAll(actual.keySet())
                || requireComplete && !expected.keySet().equals(actual.keySet())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_IDEMPOTENCY_CONFLICT,
                    activeOrder.getId(), "PICK_LIST_SOURCE_SET_CHANGED");
        }
        for (Map.Entry<Long, MesProcessPoolActiveOrderPickListBindingDO> actualEntry : actual.entrySet()) {
            MesFormalProductionPickListSourceResolver.Source source = expected.get(actualEntry.getKey());
            MesProcessPoolActiveOrderPickListBindingDO binding = actualEntry.getValue();
            List<MesProcessPoolActiveOrderPickListBindingItemDO> frozenItems = bindingItemMapper.selectListByBindingId(binding.getId());
            if (!Objects.equals(binding.getSourceSnapshotHash(), source.hash())
                    || frozenItems == null || frozenItems.size() != source.items().size()) {
                throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_IDEMPOTENCY_CONFLICT,
                        activeOrder.getId(), "PICK_LIST_SOURCE_CHANGED:" + actualEntry.getKey());
            }
            Map<Long, MesProcessPoolActiveOrderPickListBindingItemDO> frozenBySourceId = frozenItems.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(MesProcessPoolActiveOrderPickListBindingItemDO::getPickListItemId,
                            item -> item, (left, right) -> {
                                throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_IDEMPOTENCY_CONFLICT,
                                        activeOrder.getId(), "PICK_LIST_FROZEN_ITEM_DUPLICATED:" + actualEntry.getKey());
                            }));
            if (frozenBySourceId.size() != source.items().size()) {
                throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_IDEMPOTENCY_CONFLICT,
                        activeOrder.getId(), "PICK_LIST_FROZEN_ITEM_SET_CHANGED:" + actualEntry.getKey());
            }
            for (ErpKingdeeProductionPickListItemDO sourceItem : source.items()) {
                MesProcessPoolActiveOrderPickListBindingItemDO frozen = frozenBySourceId.get(sourceItem.getId());
                if (!sameFrozenItem(frozen, sourceItem)) {
                    throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_IDEMPOTENCY_CONFLICT,
                            activeOrder.getId(), "PICK_LIST_FROZEN_ITEM_CHANGED:" + sourceItem.getId());
                }
            }
        }
    }

    private MesProcessPoolActiveOrderPickListBindingItemDO toBindingItem(Long bindingId,
                                                                          ErpKingdeeProductionPickListItemDO item) {
        return MesProcessPoolActiveOrderPickListBindingItemDO.builder()
                .id(IdUtil.getSnowflakeNextId()).bindingId(bindingId).pickListItemId(item.getId())
                .sourceEntryId(item.getSourceEntryId()).sourceLineKey(item.getSourceLineKey())
                .materialNumber(item.getMaterialNumber()).materialName(item.getMaterialName())
                .materialSpecification(item.getMaterialSpecification()).unitName(item.getUnitName())
                .requestedQuantity(item.getRequestedQuantity()).actualQuantity(item.getActualQuantity())
                .baseActualQuantity(item.getBaseActualQuantity()).lotNumber(item.getLotNumber())
                .productionOrderNo(item.getProductionOrderNo()).productionOrderLineNo(item.getProductionOrderLineNo())
                .sourceModifyTime(item.getSourceModifyTime())
                .itemSnapshotHash(MesFormalProductionPickListSourceResolver.itemSnapshotHash(item))
                .build();
    }

    private static boolean sameFrozenItem(MesProcessPoolActiveOrderPickListBindingItemDO frozen,
                                          ErpKingdeeProductionPickListItemDO source) {
        return frozen != null && Objects.equals(frozen.getPickListItemId(), source.getId())
                && Objects.equals(frozen.getSourceEntryId(), source.getSourceEntryId())
                && Objects.equals(frozen.getSourceLineKey(), source.getSourceLineKey())
                && Objects.equals(frozen.getMaterialNumber(), source.getMaterialNumber())
                && Objects.equals(frozen.getMaterialName(), source.getMaterialName())
                && Objects.equals(frozen.getMaterialSpecification(), source.getMaterialSpecification())
                && Objects.equals(frozen.getUnitName(), source.getUnitName())
                && sameDecimal(frozen.getRequestedQuantity(), source.getRequestedQuantity())
                && sameDecimal(frozen.getActualQuantity(), source.getActualQuantity())
                && sameDecimal(frozen.getBaseActualQuantity(), source.getBaseActualQuantity())
                && Objects.equals(frozen.getLotNumber(), source.getLotNumber())
                && Objects.equals(frozen.getProductionOrderNo(), source.getProductionOrderNo())
                && Objects.equals(frozen.getProductionOrderLineNo(), source.getProductionOrderLineNo())
                && Objects.equals(frozen.getSourceModifyTime(), source.getSourceModifyTime())
                && Objects.equals(frozen.getItemSnapshotHash(),
                MesFormalProductionPickListSourceResolver.itemSnapshotHash(source));
    }

    private static boolean sameDecimal(java.math.BigDecimal left, java.math.BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }

    private RuntimeException sourceMissing(MesProcessPoolActiveOrderDO activeOrder, String reason) {
        return exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING,
                activeOrder == null ? null : activeOrder.getId(), reason);
    }

}
