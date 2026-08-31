package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFeedbackMaterialBatchQueryService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_PROCESS_MATERIAL_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_PROCESS_MATERIAL_REQUIRED;

@Service
public class MesFrontlineProcessMaterialServiceImpl implements MesFrontlineProcessMaterialService {

    private static final String PRODUCT_BOMS_CONFIG_KEY = "productBoms";

    private final ActiveOrderSnapshotResolver activeOrderSnapshotResolver;
    private final MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    private final MesProRouteVersionMapper routeVersionMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesMdItemMapper itemMapper;
    private final MesProFeedbackMaterialBatchQueryService batchQueryService;

    public MesFrontlineProcessMaterialServiceImpl(
            ActiveOrderSnapshotResolver activeOrderSnapshotResolver,
            MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper,
            MesProRouteVersionMapper routeVersionMapper,
            MesProWorkOrderMapper workOrderMapper,
            MesMdItemMapper itemMapper,
            MesProFeedbackMaterialBatchQueryService batchQueryService) {
        this.activeOrderSnapshotResolver = activeOrderSnapshotResolver;
        this.processSnapshotMapper = processSnapshotMapper;
        this.routeVersionMapper = routeVersionMapper;
        this.workOrderMapper = workOrderMapper;
        this.itemMapper = itemMapper;
        this.batchQueryService = batchQueryService;
    }

    @Override
    public List<MesFrontlineProcessMaterial> listFrozenMaterials(Long activeOrderId, Long routeId,
                                                                 Long routeProcessId, Long processId) {
        ActiveOrderSnapshotResolver.ActiveOrderSnapshot activeOrder = activeOrderSnapshotResolver
                .requireEffective(activeOrderId);
        requireIdentity(activeOrder, routeId, routeProcessId, processId);
        MesProWorkOrderDO workOrder = requireWorkOrder(activeOrder);
        MesProRouteVersionDO routeVersion = requireRouteVersion(activeOrder);
        List<FrozenProductBom> productBoms = parseFrozenProductBoms(activeOrder, routeVersion,
                workOrder.getProductId(), processId);
        return attachMaterialMasterData(productBoms, workOrder.getId());
    }

    private void requireIdentity(ActiveOrderSnapshotResolver.ActiveOrderSnapshot activeOrder, Long routeId,
                                 Long routeProcessId, Long processId) {
        if (!Objects.equals(activeOrder.routeId(), routeId)) {
            throw invalid("活跃订单与当前路线不一致");
        }
        MesProcessPoolActiveOrderProcessSnapshotDO processSnapshot = processSnapshotMapper
                .selectByActiveOrderAndProcess(activeOrder.activeOrderId(), routeProcessId, processId);
        if (processSnapshot == null
                || !Objects.equals(processSnapshot.getActiveOrderId(), activeOrder.activeOrderId())
                || !Objects.equals(processSnapshot.getWorkOrderId(), activeOrder.workOrderId())
                || !Objects.equals(processSnapshot.getRouteId(), activeOrder.routeId())
                || !Objects.equals(processSnapshot.getRouteVersionId(), activeOrder.routeVersionId())
                || !Objects.equals(processSnapshot.getRouteProcessId(), routeProcessId)
                || !Objects.equals(processSnapshot.getProcessId(), processId)) {
            throw invalid("当前工序不属于活跃订单冻结版本");
        }
    }

    private MesProWorkOrderDO requireWorkOrder(ActiveOrderSnapshotResolver.ActiveOrderSnapshot activeOrder) {
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(activeOrder.workOrderId());
        if (workOrder == null || !Objects.equals(workOrder.getId(), activeOrder.workOrderId())
                || workOrder.getProductId() == null || workOrder.getProductId() <= 0) {
            throw invalid("活跃订单生产工单缺少正式产品物料");
        }
        return workOrder;
    }

    private MesProRouteVersionDO requireRouteVersion(ActiveOrderSnapshotResolver.ActiveOrderSnapshot activeOrder) {
        MesProRouteVersionDO routeVersion = routeVersionMapper.selectById(activeOrder.routeVersionId());
        if (routeVersion == null || !Objects.equals(routeVersion.getId(), activeOrder.routeVersionId())
                || !Objects.equals(routeVersion.getRouteId(), activeOrder.routeId())
                || routeVersion.getRouteSnapshotJson() == null || routeVersion.getRouteSnapshotJson().isBlank()) {
            throw invalid("活跃订单锁定工艺版本缺失或身份不一致");
        }
        return routeVersion;
    }

    private List<FrozenProductBom> parseFrozenProductBoms(
            ActiveOrderSnapshotResolver.ActiveOrderSnapshot activeOrder,
            MesProRouteVersionDO routeVersion,
            Long productId,
            Long processId) {
        JSONObject snapshot;
        try {
            snapshot = JSON.parseObject(routeVersion.getRouteSnapshotJson());
        } catch (RuntimeException ex) {
            throw invalid("锁定工艺版本物料快照 JSON 无效");
        }
        if (snapshot == null || !Objects.equals(snapshot.getLong("routeId"), activeOrder.routeId())) {
            throw invalid("锁定工艺版本物料快照路线身份不一致");
        }
        JSONObject configSnapshots = snapshot.getJSONObject("configSnapshots");
        Object rawProductBoms = configSnapshots == null ? null : configSnapshots.get(PRODUCT_BOMS_CONFIG_KEY);
        List<JSONObject> rows = normalizeRows(rawProductBoms);
        Map<Long, FrozenProductBom> matched = new LinkedHashMap<>();
        for (JSONObject row : rows) {
            if (row == null || !Objects.equals(row.getLong("productId"), productId)
                    || !Objects.equals(row.getLong("processId"), processId)) {
                continue;
            }
            Long materialId = row.getLong("itemId");
            BigDecimal bomQuantity = row.getBigDecimal("quantity");
            if (materialId == null || materialId <= 0) {
                throw invalid("冻结工序报工物料身份缺失");
            }
            if (bomQuantity == null || bomQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw invalid("物料 " + materialId + " 的用料比例必须大于 0");
            }
            if (matched.putIfAbsent(materialId, new FrozenProductBom(materialId, bomQuantity)) != null) {
                throw invalid("冻结工序报工物料重复：" + materialId);
            }
        }
        if (matched.isEmpty()) {
            throw exception(PRO_FRONTLINE_PROCESS_MATERIAL_REQUIRED, activeOrder.activeOrderId(), processId);
        }
        return List.copyOf(matched.values());
    }

    private List<JSONObject> normalizeRows(Object rawProductBoms) {
        List<JSONObject> rows = new ArrayList<>();
        if (rawProductBoms instanceof JSONObject object) {
            for (Object value : object.values()) {
                rows.add(toJsonObject(value));
            }
            return rows;
        }
        if (rawProductBoms instanceof JSONArray array) {
            for (Object value : array) {
                rows.add(toJsonObject(value));
            }
            return rows;
        }
        return rows;
    }

    private JSONObject toJsonObject(Object value) {
        if (value instanceof JSONObject object) {
            return object;
        }
        try {
            return value == null ? null : JSON.parseObject(JSON.toJSONString(value));
        } catch (RuntimeException ex) {
            throw invalid("冻结工序报工物料快照结构无效");
        }
    }

    private List<MesFrontlineProcessMaterial> attachMaterialMasterData(List<FrozenProductBom> productBoms,
                                                                       Long workOrderId) {
        Set<Long> materialIds = productBoms.stream()
                .map(FrozenProductBom::materialId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<MesMdItemDO> items = itemMapper.selectListByIds(materialIds);
        Map<Long, MesMdItemDO> itemsById = items == null ? Map.of() : items.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toMap(MesMdItemDO::getId, item -> item,
                        (left, ignored) -> left, LinkedHashMap::new));
        if (!itemsById.keySet().equals(materialIds)) {
            Set<Long> missing = new LinkedHashSet<>(materialIds);
            missing.removeAll(itemsById.keySet());
            throw invalid("冻结工序报工物料主档缺失：" + missing);
        }
        Map<Long, BigDecimal> quantityByMaterial = productBoms.stream().collect(
                java.util.stream.Collectors.toMap(FrozenProductBom::materialId, FrozenProductBom::bomQuantity));
        return itemsById.values().stream()
                .map(item -> toMaterial(item, quantityByMaterial.get(item.getId()), workOrderId))
                .sorted(Comparator
                        .comparing(MesFrontlineProcessMaterial::materialCode,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(MesFrontlineProcessMaterial::materialId))
                .toList();
    }

    private MesFrontlineProcessMaterial toMaterial(MesMdItemDO item, BigDecimal bomQuantity, Long workOrderId) {
        String code = normalize(item.getCode());
        String name = normalize(item.getName());
        if (code == null || name == null) {
            throw invalid("冻结工序报工物料缺少编码或名称：" + item.getId());
        }
        return new MesFrontlineProcessMaterial(item.getId(), code, name,
                normalize(item.getSpecification()), bomQuantity,
                batchQueryService.listBatchCodes(workOrderId, code));
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static ServiceException invalid(String detail) {
        return exception(PRO_FRONTLINE_PROCESS_MATERIAL_INVALID, detail);
    }

    private record FrozenProductBom(Long materialId, BigDecimal bomQuantity) {
    }
}
