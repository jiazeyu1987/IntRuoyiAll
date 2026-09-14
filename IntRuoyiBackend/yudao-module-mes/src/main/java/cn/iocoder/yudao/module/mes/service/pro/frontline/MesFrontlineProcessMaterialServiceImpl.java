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
import cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFeedbackMaterialBatchEvidence;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

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

@Service
public class MesFrontlineProcessMaterialServiceImpl implements MesFrontlineProcessMaterialService {

    private static final String BATCH_USE_CONFIGS_KEY = "batchUseConfigs";
    private static final String INPUT_MATERIAL_IDS_KEY = "inputMaterialIds";
    private static final String OUTPUT_MATERIAL_IDS_KEY = "outputMaterialIds";

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
        FrozenMaterialIds materialIds = parseFrozenMaterialIds(activeOrder, routeVersion, routeProcessId);
        List<Long> allIds = new ArrayList<>(materialIds.inputIds());
        allIds.addAll(materialIds.outputIds());
        Map<Long, MesMdItemDO> items = requireMaterialMasters(allIds);
        List<MesFrontlineProcessMaterial> result = new ArrayList<>();
        result.addAll(toMaterials(materialIds.inputIds(), items, workOrder.getId(),
                MesFrontlineProcessMaterial.ROLE_INPUT));
        result.addAll(toMaterials(materialIds.outputIds(), items, workOrder.getId(),
                MesFrontlineProcessMaterial.ROLE_OUTPUT));
        return List.copyOf(result);
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
        if (workOrder == null || !Objects.equals(workOrder.getId(), activeOrder.workOrderId())) {
            throw invalid("活跃订单生产工单缺失或身份不一致");
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

    private FrozenMaterialIds parseFrozenMaterialIds(
            ActiveOrderSnapshotResolver.ActiveOrderSnapshot activeOrder,
            MesProRouteVersionDO routeVersion,
            Long routeProcessId) {
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
        Object rawBatchUseConfigs = configSnapshots == null ? null : configSnapshots.get(BATCH_USE_CONFIGS_KEY);
        List<JSONObject> rows = normalizeRows(rawBatchUseConfigs);
        JSONObject matched = null;
        for (JSONObject row : rows) {
            if (row == null || !Objects.equals(row.getLong("routeProcessId"), routeProcessId)) {
                continue;
            }
            if (matched != null) {
                throw invalid("冻结工序批记录配置重复：" + routeProcessId);
            }
            matched = row;
        }
        if (matched == null) {
            return new FrozenMaterialIds(List.of(), List.of());
        }
        List<Long> inputIds = parseRoleMaterialIds(matched.get(INPUT_MATERIAL_IDS_KEY), "输入");
        List<Long> outputIds = parseRoleMaterialIds(matched.get(OUTPUT_MATERIAL_IDS_KEY), "输出");
        Set<Long> overlap = new LinkedHashSet<>(inputIds);
        overlap.retainAll(outputIds);
        if (!overlap.isEmpty()) {
            throw invalid("冻结工序输入输出物料重复：" + overlap);
        }
        return new FrozenMaterialIds(inputIds, outputIds);
    }

    private List<Long> parseRoleMaterialIds(Object rawMaterialIds, String roleName) {
        if (rawMaterialIds == null) {
            return List.of();
        }
        if (!(rawMaterialIds instanceof JSONArray materialIds)) {
            throw invalid("冻结工序" + roleName + "物料配置结构无效");
        }
        Set<Long> normalized = new LinkedHashSet<>();
        for (Object rawMaterialId : materialIds) {
            if (!(rawMaterialId instanceof Number number) || number.longValue() <= 0) {
                throw invalid("冻结工序报工物料身份缺失");
            }
            Long materialId = number.longValue();
            if (!normalized.add(materialId)) {
                throw invalid("冻结工序" + roleName + "物料重复：" + materialId);
            }
        }
        return List.copyOf(normalized);
    }

    private List<JSONObject> normalizeRows(Object rawConfigs) {
        List<JSONObject> rows = new ArrayList<>();
        if (rawConfigs == null) {
            return rows;
        }
        if (rawConfigs instanceof JSONObject object) {
            for (Object value : object.values()) {
                rows.add(toJsonObject(value));
            }
            return rows;
        }
        if (rawConfigs instanceof JSONArray array) {
            for (Object value : array) {
                rows.add(toJsonObject(value));
            }
            return rows;
        }
        throw invalid("冻结工序批记录配置结构无效");
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

    private Map<Long, MesMdItemDO> requireMaterialMasters(List<Long> materialIds) {
        if (materialIds.isEmpty()) {
            return Map.of();
        }
        Set<Long> expectedMaterialIds = new LinkedHashSet<>(materialIds);
        List<MesMdItemDO> items = itemMapper.selectListByIds(materialIds);
        Map<Long, MesMdItemDO> itemsById = items == null ? Map.of() : items.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toMap(MesMdItemDO::getId, item -> item,
                        (left, ignored) -> left, LinkedHashMap::new));
        if (!itemsById.keySet().equals(expectedMaterialIds)) {
            Set<Long> missing = new LinkedHashSet<>(expectedMaterialIds);
            missing.removeAll(itemsById.keySet());
            throw invalid("冻结工序报工物料主档缺失：" + missing);
        }
        return itemsById;
    }

    private List<MesFrontlineProcessMaterial> toMaterials(List<Long> materialIds,
                                                           Map<Long, MesMdItemDO> itemsById,
                                                           Long workOrderId,
                                                           String materialRole) {
        return materialIds.stream().map(itemsById::get)
                .map(item -> toMaterial(item, workOrderId, materialRole))
                .sorted(Comparator
                        .comparing(MesFrontlineProcessMaterial::materialCode,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(MesFrontlineProcessMaterial::materialId))
                .toList();
    }

    private MesFrontlineProcessMaterial toMaterial(MesMdItemDO item, Long workOrderId, String materialRole) {
        String code = normalize(item.getCode());
        String name = normalize(item.getName());
        if (code == null || name == null) {
            throw invalid("冻结工序报工物料缺少编码或名称：" + item.getId());
        }
        MesProFeedbackMaterialBatchEvidence evidence = MesFrontlineProcessMaterial.ROLE_INPUT.equals(materialRole)
                ? batchQueryService.resolveEvidence(workOrderId, code) : null;
        if (MesFrontlineProcessMaterial.ROLE_INPUT.equals(materialRole)
                && (evidence == null || evidence.batchCodes().isEmpty())) {
            throw invalid("输入物料正式领料批号缺失：" + code);
        }
        return new MesFrontlineProcessMaterial(item.getId(), code, name,
                normalize(item.getSpecification()), materialRole, null,
                evidence == null ? List.of() : evidence.batchCodes(),
                evidence == null ? null : evidence.requestedQuantity(),
                evidence == null ? null : evidence.actualQuantity(),
                evidence == null ? null : evidence.baseActualQuantity(),
                evidence == null ? List.of() : evidence.pickListIds(),
                evidence == null ? List.of() : evidence.pickListNos(),
                evidence == null ? List.of() : evidence.pickListItemIds(),
                evidence == null ? null : evidence.sourceSnapshotHash());
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

    private record FrozenMaterialIds(List<Long> inputIds, List<Long> outputIds) {
    }
}
