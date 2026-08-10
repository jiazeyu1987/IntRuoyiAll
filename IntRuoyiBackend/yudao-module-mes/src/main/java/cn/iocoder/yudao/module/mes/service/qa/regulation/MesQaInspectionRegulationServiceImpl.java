package cn.iocoder.yudao.module.mes.service.qa.regulation;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationProjectStatusRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationPublishedVersionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationSaveRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_ITEM_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_FINAL_APPLICABILITY_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_SNAPSHOT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_VERSION_CONFLICT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_VERSION_IMMUTABLE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_VERSION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_VERSION_NOT_PUBLISHED;

@Service
@Validated
public class MesQaInspectionRegulationServiceImpl implements MesQaInspectionRegulationService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_RETIRED = "RETIRED";
    private static final Set<String> ALLOWED_INSPECTION_TYPES = Set.of("FIRST", "PATROL", "FINAL");

    private final MesQaInspectionRegulationMapper regulationMapper;
    private final MesQaInspectionRegulationVersionMapper versionMapper;
    private final MesQaInspectionRegulationItemMapper itemMapper;
    private final MesQaInspectionRegulationItemEquipmentMapper itemEquipmentMapper;

    public MesQaInspectionRegulationServiceImpl(MesQaInspectionRegulationMapper regulationMapper,
                                                MesQaInspectionRegulationVersionMapper versionMapper,
                                                MesQaInspectionRegulationItemMapper itemMapper,
                                                MesQaInspectionRegulationItemEquipmentMapper itemEquipmentMapper) {
        this.regulationMapper = regulationMapper;
        this.versionMapper = versionMapper;
        this.itemMapper = itemMapper;
        this.itemEquipmentMapper = itemEquipmentMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesQaInspectionRegulationSaveRespVO saveDraft(MesQaInspectionRegulationSaveReqVO reqVO) {
        validateItems(reqVO);
        DraftContext context = saveDraftInternal(reqVO);
        return MesQaInspectionRegulationSaveRespVO.builder()
                .regulationId(context.regulation().getId())
                .draftVersionId(context.version().getId())
                .versionNo(context.version().getVersionNo())
                .lifecycleStatus(context.version().getLifecycleStatus())
                .immutable(false)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesQaInspectionRegulationPublishedVersionRespVO publish(MesQaInspectionRegulationSaveReqVO reqVO) {
        DraftContext context = saveDraftInternal(reqVO);
        LocalDateTime publishedAt = LocalDateTime.now();
        MesQaInspectionRegulationVersionDO currentPublished =
                versionMapper.selectCurrentPublishedByRegulationId(context.regulation().getId());
        if (currentPublished != null && !Objects.equals(currentPublished.getId(), context.version().getId())) {
            versionMapper.updateById(new MesQaInspectionRegulationVersionDO()
                    .setId(currentPublished.getId())
                    .setLifecycleStatus(STATUS_RETIRED)
                    .setRetiredAt(publishedAt));
        }

        context.version()
                .setLifecycleStatus(STATUS_PUBLISHED)
                .setPublishedAt(publishedAt);
        versionMapper.updateById(new MesQaInspectionRegulationVersionDO()
                .setId(context.version().getId())
                .setLifecycleStatus(STATUS_PUBLISHED)
                .setPublishedAt(publishedAt));

        context.regulation()
                .setLifecycleStatus(STATUS_PUBLISHED)
                .setCurrentVersionId(context.version().getId());
        regulationMapper.updateById(new MesQaInspectionRegulationDO()
                .setId(context.regulation().getId())
                .setLifecycleStatus(STATUS_PUBLISHED)
                .setCurrentVersionId(context.version().getId())
                .setRegulationCode(reqVO.getRegulationCode())
                .setRegulationName(reqVO.getRegulationName()));

        return buildPublishedVersionResp(context.regulation(), context.version(), context.items());
    }

    @Override
    public MesQaInspectionRegulationPublishedVersionRespVO getPublishedVersion(Long versionId) {
        MesQaInspectionRegulationVersionDO version = versionId == null
                ? versionMapper.selectLatestPublished()
                : versionMapper.selectById(versionId);
        if (version == null) {
            throw exception(QA_INSPECTION_REGULATION_VERSION_NOT_EXISTS, versionId);
        }
        if (!Objects.equals(version.getLifecycleStatus(), "PUBLISHED")) {
            throw exception(QA_INSPECTION_REGULATION_VERSION_NOT_PUBLISHED, version.getId());
        }

        MesQaInspectionRegulationDO regulation = regulationMapper.selectById(version.getRegulationId());
        if (regulation == null) {
            throw exception(QA_INSPECTION_REGULATION_NOT_EXISTS, version.getRegulationId());
        }

        JSONObject snapshot = parseSnapshot(version);
        List<MesQaInspectionRegulationItemDO> items = itemMapper.selectListByVersionId(version.getId());
        return buildPublishedVersionResp(regulation, version, snapshot, items);
    }

    private DraftContext saveDraftInternal(MesQaInspectionRegulationSaveReqVO reqVO) {
        MesQaInspectionRegulationDO regulation = resolveRegulation(reqVO);
        MesQaInspectionRegulationVersionDO existingVersion =
                versionMapper.selectByRegulationIdAndVersionNo(regulation.getId(), reqVO.getVersionNo());
        if (existingVersion != null && Objects.equals(existingVersion.getLifecycleStatus(), STATUS_PUBLISHED)) {
            throw exception(QA_INSPECTION_REGULATION_VERSION_IMMUTABLE, existingVersion.getId());
        }
        if (existingVersion != null && !Objects.equals(existingVersion.getLifecycleStatus(), STATUS_DRAFT)) {
            throw exception(QA_INSPECTION_REGULATION_VERSION_CONFLICT, existingVersion.getId());
        }

        String snapshotJson = buildSnapshotJson(reqVO);
        MesQaInspectionRegulationVersionDO version = existingVersion;
        if (version == null) {
            version = MesQaInspectionRegulationVersionDO.builder()
                    .regulationId(regulation.getId())
                    .versionNo(reqVO.getVersionNo())
                    .lifecycleStatus(STATUS_DRAFT)
                    .finalInspectionApplicable(reqVO.getFinalInspectionApplicable())
                    .finalInspectionNotApplicableReason(normalizeFinalInspectionReason(reqVO))
                    .snapshotJson(snapshotJson)
                    .build();
            versionMapper.insert(version);
        } else {
            version.setSnapshotJson(snapshotJson);
            version.setFinalInspectionApplicable(reqVO.getFinalInspectionApplicable());
            version.setFinalInspectionNotApplicableReason(normalizeFinalInspectionReason(reqVO));
            versionMapper.updateById(new MesQaInspectionRegulationVersionDO()
                    .setId(version.getId())
                    .setFinalInspectionApplicable(reqVO.getFinalInspectionApplicable())
                    .setFinalInspectionNotApplicableReason(normalizeFinalInspectionReason(reqVO))
                    .setSnapshotJson(snapshotJson));
            itemMapper.deleteByVersionId(version.getId());
            itemEquipmentMapper.deleteByVersionId(version.getId());
        }

        List<MesQaInspectionRegulationItemDO> items = new ArrayList<>();
        for (MesQaInspectionRegulationSaveReqVO.InspectionItem itemReqVO : CollUtil.emptyIfNull(reqVO.getItems())) {
            MesQaInspectionRegulationItemDO item = toItemDO(version.getId(), itemReqVO);
            itemMapper.insert(item);
            for (MesQaInspectionRegulationSaveReqVO.EquipmentOption equipmentOption :
                    CollUtil.emptyIfNull(itemReqVO.getEquipmentOptions())) {
                itemEquipmentMapper.insert(toItemEquipmentDO(version.getId(), itemReqVO, equipmentOption));
            }
            items.add(item);
        }
        return new DraftContext(regulation, version, items);
    }

    private MesQaInspectionRegulationDO resolveRegulation(MesQaInspectionRegulationSaveReqVO reqVO) {
        MesQaInspectionRegulationDO regulation = reqVO.getRegulationId() == null ? null
                : regulationMapper.selectById(reqVO.getRegulationId());
        if (reqVO.getRegulationId() != null && regulation == null) {
            throw exception(QA_INSPECTION_REGULATION_NOT_EXISTS, reqVO.getRegulationId());
        }
        if (regulation == null) {
            regulation = regulationMapper.selectByRouteProcess(reqVO.getProductId(), reqVO.getRouteId(),
                    reqVO.getRouteVersionId(), reqVO.getRouteProcessId(), reqVO.getProcessId());
        }
        if (regulation == null) {
            regulation = MesQaInspectionRegulationDO.builder()
                    .productId(reqVO.getProductId())
                    .routeId(reqVO.getRouteId())
                    .routeVersionId(reqVO.getRouteVersionId())
                    .routeProcessId(reqVO.getRouteProcessId())
                    .processId(reqVO.getProcessId())
                    .ownerModule(MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA)
                    .regulationCode(reqVO.getRegulationCode())
                    .regulationName(reqVO.getRegulationName())
                    .lifecycleStatus(STATUS_DRAFT)
                    .build();
            regulationMapper.insert(regulation);
            return regulation;
        }
        regulation.setRegulationCode(reqVO.getRegulationCode());
        regulation.setRegulationName(reqVO.getRegulationName());
        if (!Objects.equals(regulation.getLifecycleStatus(), STATUS_PUBLISHED)) {
            regulation.setLifecycleStatus(STATUS_DRAFT);
            regulationMapper.updateById(new MesQaInspectionRegulationDO()
                    .setId(regulation.getId())
                    .setLifecycleStatus(STATUS_DRAFT)
                    .setRegulationCode(reqVO.getRegulationCode())
                    .setRegulationName(reqVO.getRegulationName()));
        }
        return regulation;
    }

    private static MesQaInspectionRegulationPublishedVersionRespVO buildPublishedVersionResp(
            MesQaInspectionRegulationDO regulation, MesQaInspectionRegulationVersionDO version,
            List<MesQaInspectionRegulationItemDO> items) {
        return buildPublishedVersionResp(regulation, version, parseSnapshot(version), items);
    }

    private static MesQaInspectionRegulationPublishedVersionRespVO buildPublishedVersionResp(
            MesQaInspectionRegulationDO regulation, MesQaInspectionRegulationVersionDO version,
            JSONObject snapshot, List<MesQaInspectionRegulationItemDO> items) {
        List<MesQaInspectionRegulationPublishedVersionRespVO.InspectionRule> firstRules =
                rulesByType(items, "FIRST");
        List<MesQaInspectionRegulationPublishedVersionRespVO.InspectionRule> patrolRules =
                rulesByType(items, "PATROL");
        List<MesQaInspectionRegulationPublishedVersionRespVO.InspectionRule> finalRules =
                rulesByType(items, "FINAL");
        return MesQaInspectionRegulationPublishedVersionRespVO.builder()
                .regulationId(regulation.getId())
                .publishedVersionId(version.getId())
                .versionNo(version.getVersionNo())
                .publishedAt(version.getPublishedAt())
                .immutable(true)
                .regulationCode(regulation.getRegulationCode())
                .regulationName(regulation.getRegulationName())
                .productId(regulation.getProductId())
                .productName(firstText(snapshot, "productName", "productDisplayName", "productCode"))
                .routeId(regulation.getRouteId())
                .routeName(firstText(snapshot, "routeName", "routeCode"))
                .routeVersionId(regulation.getRouteVersionId())
                .routeVersionNo(firstText(snapshot, "routeVersionNo", "routeVersionName", "versionNo"))
                .routeProcessId(regulation.getRouteProcessId())
                .processId(regulation.getProcessId())
                .routeProcessName(firstText(snapshot, "routeProcessName", "processName", "routeProcessCode"))
                .batchRecordBindingSummary(resolveBatchRecordBindingSummary(snapshot))
                .finalInspectionApplicable(version.getFinalInspectionApplicable())
                .finalInspectionNotApplicableReason(version.getFinalInspectionNotApplicableReason())
                .firstInspectionRules(firstRules)
                .patrolInspectionRules(patrolRules)
                .finalInspectionRules(finalRules)
                .build();
    }

    @Override
    public List<MesQaInspectionRegulationProjectStatusRespVO> getProjectStatuses(Collection<Long> productIds) {
        List<Long> requestedProductIds = productIds == null
                ? Collections.emptyList()
                : productIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (requestedProductIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<MesQaInspectionRegulationDO>> regulationsByProduct =
                regulationMapper.selectListByProductIds(requestedProductIds).stream()
                        .collect(Collectors.groupingBy(MesQaInspectionRegulationDO::getProductId));
        return requestedProductIds.stream()
                .map(productId -> buildProjectStatus(productId, regulationsByProduct.get(productId)))
                .toList();
    }

    private static MesQaInspectionRegulationProjectStatusRespVO buildProjectStatus(
            Long productId, List<MesQaInspectionRegulationDO> regulations) {
        MesQaInspectionRegulationProjectStatusRespVO status = new MesQaInspectionRegulationProjectStatusRespVO();
        status.setProductId(productId);
        if (CollUtil.isEmpty(regulations)) {
            status.setConfigured(false);
            status.setRegulationCount(0);
            return status;
        }
        MesQaInspectionRegulationDO representative = regulations.stream()
                .max(Comparator
                        .comparingInt(MesQaInspectionRegulationServiceImpl::regulationStatusPriority)
                        .thenComparing(MesQaInspectionRegulationDO::getCurrentVersionId,
                                Comparator.nullsFirst(Long::compareTo))
                        .thenComparing(MesQaInspectionRegulationDO::getId,
                                Comparator.nullsFirst(Long::compareTo)))
                .orElseThrow();
        status.setConfigured(true);
        status.setRegulationCount(regulations.size());
        status.setRegulationId(representative.getId());
        status.setCurrentVersionId(representative.getCurrentVersionId());
        status.setRegulationCode(representative.getRegulationCode());
        status.setRegulationName(representative.getRegulationName());
        status.setLifecycleStatus(representative.getLifecycleStatus());
        return status;
    }

    private static int regulationStatusPriority(MesQaInspectionRegulationDO regulation) {
        if (Objects.equals(regulation.getLifecycleStatus(), STATUS_PUBLISHED)) {
            return 3;
        }
        if (Objects.equals(regulation.getLifecycleStatus(), STATUS_DRAFT)) {
            return 2;
        }
        return 1;
    }

    private static JSONObject parseSnapshot(MesQaInspectionRegulationVersionDO version) {
        if (StrUtil.isBlank(version.getSnapshotJson())) {
            throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, version.getId());
        }
        try {
            return JSON.parseObject(version.getSnapshotJson());
        } catch (RuntimeException ex) {
            throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, version.getId());
        }
    }

    private static List<MesQaInspectionRegulationPublishedVersionRespVO.InspectionRule> rulesByType(
            List<MesQaInspectionRegulationItemDO> items, String inspectionType) {
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }
        return items.stream()
                .filter(item -> Objects.equals(item.getInspectionType(), inspectionType))
                .map(item -> MesQaInspectionRegulationPublishedVersionRespVO.InspectionRule.builder()
                        .inspectionType(item.getInspectionType())
                        .itemCode(item.getItemCode())
                        .itemName(item.getItemName())
                        .inspectionMethod(item.getInspectionMethod())
                        .inspectionTool(item.getInspectionTool())
                        .samplingPlanText(item.getSamplingPlanText())
                        .standardText(item.getStandardText())
                        .resultType(item.getResultType())
                        .firstInspectionQuantity(item.getFirstInspectionQuantity())
                        .patrolInspectionRatio(item.getPatrolInspectionRatio())
                        .build())
                .toList();
    }

    private static String firstText(JSONObject snapshot, String... keys) {
        for (String key : keys) {
            String value = snapshot.getString(key);
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static String resolveBatchRecordBindingSummary(JSONObject snapshot) {
        for (String key : List.of("batchRecordReports", "batchRecordForms", "batchRecords")) {
            JSONArray records = snapshot.getJSONArray(key);
            if (records == null || records.isEmpty()) {
                continue;
            }
            String summary = records.stream()
                    .filter(JSONObject.class::isInstance)
                    .map(JSONObject.class::cast)
                    .map(MesQaInspectionRegulationServiceImpl::batchRecordName)
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .reduce((left, right) -> left + "，" + right)
                    .orElse(null);
            if (StrUtil.isNotBlank(summary)) {
                return summary;
            }
        }
        return null;
    }

    private static String batchRecordName(JSONObject record) {
        return firstText(record, "batchRecordReportName", "reportName", "batchRecordName", "formName", "name",
                "batchRecordReportId", "reportId");
    }

    private static void validateItems(MesQaInspectionRegulationSaveReqVO reqVO) {
        validateFinalInspectionApplicability(reqVO);
        if (CollUtil.isEmpty(reqVO.getItems())) {
            throw exception(QA_INSPECTION_REGULATION_ITEM_INVALID, "items");
        }
        reqVO.getItems().forEach(MesQaInspectionRegulationServiceImpl::validateItem);
    }

    private static void validateFinalInspectionApplicability(MesQaInspectionRegulationSaveReqVO reqVO) {
        if (reqVO.getFinalInspectionApplicable() == null) {
            throw exception(QA_INSPECTION_REGULATION_FINAL_APPLICABILITY_INVALID, "末检适用性未显式配置");
        }
        if (Boolean.FALSE.equals(reqVO.getFinalInspectionApplicable())
                && StrUtil.isBlank(reqVO.getFinalInspectionNotApplicableReason())) {
            throw exception(QA_INSPECTION_REGULATION_FINAL_APPLICABILITY_INVALID, "末检不适用依据不能为空");
        }
        if (Boolean.TRUE.equals(reqVO.getFinalInspectionApplicable())
                && StrUtil.isNotBlank(reqVO.getFinalInspectionNotApplicableReason())) {
            throw exception(QA_INSPECTION_REGULATION_FINAL_APPLICABILITY_INVALID,
                    "末检适用时不得填写不适用依据");
        }
    }

    private static void validateItem(MesQaInspectionRegulationSaveReqVO.InspectionItem item) {
        String inspectionType = normalizeInspectionType(item.getInspectionType());
        if (!ALLOWED_INSPECTION_TYPES.contains(inspectionType)) {
            throw exception(QA_INSPECTION_REGULATION_ITEM_INVALID, item.getInspectionType());
        }
        if (StrUtil.isBlank(item.getInspectionTool())) {
            throw exception(QA_INSPECTION_REGULATION_ITEM_INVALID, item.getItemCode() + ".inspectionTool");
        }
        if (StrUtil.isBlank(item.getSamplingPlanText())) {
            throw exception(QA_INSPECTION_REGULATION_ITEM_INVALID, item.getItemCode() + ".samplingPlanText");
        }
        if (("FIRST".equals(inspectionType) || "FINAL".equals(inspectionType))
                && (item.getFirstInspectionQuantity() == null || item.getFirstInspectionQuantity() <= 0)) {
            throw exception(QA_INSPECTION_REGULATION_ITEM_INVALID, item.getItemCode());
        }
        if ("PATROL".equals(inspectionType)
                && !positive(item.getPatrolInspectionRatio())
                && (item.getFirstInspectionQuantity() == null || item.getFirstInspectionQuantity() <= 0)) {
            throw exception(QA_INSPECTION_REGULATION_ITEM_INVALID, item.getItemCode());
        }
        if ("NUMERIC".equals(item.getResultType())
                && (item.getStandardLowerLimit() == null || item.getStandardUpperLimit() == null)) {
            throw exception(QA_INSPECTION_REGULATION_ITEM_INVALID, item.getItemCode());
        }
        boolean equipmentRequired = Boolean.TRUE.equals(item.getEquipmentRequired());
        boolean hasEquipmentOptions = CollUtil.isNotEmpty(item.getEquipmentOptions());
        if (equipmentRequired != hasEquipmentOptions) {
            throw exception(QA_INSPECTION_REGULATION_ITEM_INVALID,
                    item.getItemCode() + ".equipmentRequired/equipmentOptions");
        }
        for (MesQaInspectionRegulationSaveReqVO.EquipmentOption equipmentOption :
                CollUtil.emptyIfNull(item.getEquipmentOptions())) {
            validateEquipmentOption(item, equipmentOption);
        }
    }

    private static void validateEquipmentOption(MesQaInspectionRegulationSaveReqVO.InspectionItem item,
                                                MesQaInspectionRegulationSaveReqVO.EquipmentOption equipmentOption) {
        if (equipmentOption == null || equipmentOption.getEquipmentId() == null
                || equipmentOption.getEquipmentId() <= 0
                || StrUtil.isBlank(equipmentOption.getEquipmentCode())
                || StrUtil.isBlank(equipmentOption.getEquipmentName())
                || StrUtil.isBlank(equipmentOption.getEquipmentNumber())) {
            throw exception(QA_INSPECTION_REGULATION_ITEM_INVALID, item.getItemCode() + ".equipmentOption");
        }
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private static String normalizeInspectionType(String inspectionType) {
        if (StrUtil.startWith(inspectionType, "PATROL")) {
            return "PATROL";
        }
        return StrUtil.trim(inspectionType);
    }

    private static String buildSnapshotJson(MesQaInspectionRegulationSaveReqVO reqVO) {
        JSONObject snapshot = new JSONObject();
        snapshot.put("productName", reqVO.getProductName());
        snapshot.put("routeName", reqVO.getRouteName());
        snapshot.put("routeVersionNo", reqVO.getRouteVersionNo());
        snapshot.put("routeProcessName", reqVO.getRouteProcessName());
        snapshot.put("effectiveDate", reqVO.getEffectiveDate());
        snapshot.put("finalInspectionApplicable", reqVO.getFinalInspectionApplicable());
        snapshot.put("finalInspectionNotApplicableReason", normalizeFinalInspectionReason(reqVO));
        if (StrUtil.isNotBlank(reqVO.getBatchRecordBindingSummary())) {
            JSONArray batchRecordReports = new JSONArray();
            JSONObject batchRecord = new JSONObject();
            batchRecord.put("batchRecordReportName", reqVO.getBatchRecordBindingSummary());
            batchRecordReports.add(batchRecord);
            snapshot.put("batchRecordReports", batchRecordReports);
        }
        snapshot.put("inspectionItems", JSON.toJSON(reqVO.getItems()));
        return JSON.toJSONString(snapshot);
    }

    private static String normalizeFinalInspectionReason(MesQaInspectionRegulationSaveReqVO reqVO) {
        if (Boolean.FALSE.equals(reqVO.getFinalInspectionApplicable())) {
            return StrUtil.trim(reqVO.getFinalInspectionNotApplicableReason());
        }
        return null;
    }

    private static MesQaInspectionRegulationItemDO toItemDO(Long versionId,
                                                            MesQaInspectionRegulationSaveReqVO.InspectionItem item) {
        return MesQaInspectionRegulationItemDO.builder()
                .regulationVersionId(versionId)
                .inspectionType(normalizeInspectionType(item.getInspectionType()))
                .itemCode(item.getItemCode())
                .itemName(item.getItemName())
                .inspectionMethod(item.getInspectionMethod())
                .inspectionTool(item.getInspectionTool())
                .standardText(item.getStandardText())
                .samplingPlanText(item.getSamplingPlanText())
                .standardLowerLimit(item.getStandardLowerLimit())
                .standardUpperLimit(item.getStandardUpperLimit())
                .standardUnit(item.getStandardUnit())
                .standardPrecision(item.getStandardPrecision())
                .equipmentRequired(Boolean.TRUE.equals(item.getEquipmentRequired()))
                .resultType(item.getResultType())
                .firstInspectionQuantity(item.getFirstInspectionQuantity())
                .patrolInspectionRatio(item.getPatrolInspectionRatio())
                .build();
    }

    private static MesQaInspectionRegulationItemEquipmentDO toItemEquipmentDO(Long versionId,
                                                                              MesQaInspectionRegulationSaveReqVO.InspectionItem item,
                                                                              MesQaInspectionRegulationSaveReqVO.EquipmentOption equipmentOption) {
        return MesQaInspectionRegulationItemEquipmentDO.builder()
                .regulationVersionId(versionId)
                .inspectionType(normalizeInspectionType(item.getInspectionType()))
                .itemCode(item.getItemCode())
                .equipmentId(equipmentOption.getEquipmentId())
                .equipmentCode(StrUtil.trim(equipmentOption.getEquipmentCode()))
                .equipmentName(StrUtil.trim(equipmentOption.getEquipmentName()))
                .equipmentNumber(StrUtil.trim(equipmentOption.getEquipmentNumber()))
                .defaultFlag(equipmentOption.getDefaultFlag())
                .sort(equipmentOption.getSort())
                .build();
    }

    private record DraftContext(MesQaInspectionRegulationDO regulation,
                                MesQaInspectionRegulationVersionDO version,
                                List<MesQaInspectionRegulationItemDO> items) {
    }
}
