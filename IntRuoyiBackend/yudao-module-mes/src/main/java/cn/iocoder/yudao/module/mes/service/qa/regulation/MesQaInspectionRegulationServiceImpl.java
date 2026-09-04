package cn.iocoder.yudao.module.mes.service.qa.regulation;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationProjectStatusRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationPublishedVersionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationResetRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationSaveRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationVersionOptionRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_DCC_PROJECT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_DCC_PROJECT_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_FINAL_APPLICABILITY_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_ITEM_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_RESET_REFERENCED;
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
    private static final Set<String> ALLOWED_INSPECTION_TYPES = Set.of(
            "FIRST", "PATROL", "PATROL_AM", "PATROL_PM", "FINAL");
    private static final Set<String> ALLOWED_RESULT_TYPES = Set.of("BOOLEAN", "NUMERIC", "TEXT");
    private static final Map<String, Integer> INSPECTION_TYPE_ORDER = Map.of(
            "FIRST", 1, "PATROL", 2, "PATROL_AM", 2, "PATROL_PM", 3, "FINAL", 4);

    private final DccProjectCodeMapper dccProjectCodeMapper;
    private final MesQaInspectionRegulationMapper regulationMapper;
    private final MesQaInspectionRegulationVersionMapper versionMapper;
    private final MesQaInspectionRegulationProcessMapper processMapper;
    private final MesQaInspectionRegulationItemMapper itemMapper;
    private final MesQaInspectionRegulationItemEquipmentMapper itemEquipmentMapper;
    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesPqcInspectionTaskMapper pqcInspectionTaskMapper;

    public MesQaInspectionRegulationServiceImpl(
            DccProjectCodeMapper dccProjectCodeMapper,
            MesQaInspectionRegulationMapper regulationMapper,
            MesQaInspectionRegulationVersionMapper versionMapper,
            MesQaInspectionRegulationProcessMapper processMapper,
            MesQaInspectionRegulationItemMapper itemMapper,
            MesQaInspectionRegulationItemEquipmentMapper itemEquipmentMapper,
            MesProcessPoolActiveOrderMapper activeOrderMapper,
            MesPqcInspectionTaskMapper pqcInspectionTaskMapper) {
        this.dccProjectCodeMapper = dccProjectCodeMapper;
        this.regulationMapper = regulationMapper;
        this.versionMapper = versionMapper;
        this.processMapper = processMapper;
        this.itemMapper = itemMapper;
        this.itemEquipmentMapper = itemEquipmentMapper;
        this.activeOrderMapper = activeOrderMapper;
        this.pqcInspectionTaskMapper = pqcInspectionTaskMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesQaInspectionRegulationSaveRespVO saveDraft(MesQaInspectionRegulationSaveReqVO reqVO) {
        DccProjectCodeDO dccProjectCode = validateRequest(reqVO);
        DraftContext context = saveDraftInternal(reqVO, dccProjectCode);
        return MesQaInspectionRegulationSaveRespVO.builder()
                .dccProjectCodeId(dccProjectCode.getId())
                .regulationId(context.regulation().getId())
                .draftVersionId(context.version().getId())
                .versionNo(context.version().getVersionNo())
                .lifecycleStatus(context.version().getLifecycleStatus())
                .immutable(false)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesQaInspectionRegulationResetRespVO resetForTesting(Long dccProjectCodeId) {
        DccProjectCodeDO dccProjectCode = requireEnabledDccProjectCode(dccProjectCodeId);
        MesQaInspectionRegulationDO regulation =
                regulationMapper.selectByDccProjectCodeId(dccProjectCode.getId());
        if (regulation == null) {
            return MesQaInspectionRegulationResetRespVO.builder()
                    .dccProjectCodeId(dccProjectCode.getId())
                    .versionCount(0)
                    .processCount(0)
                    .itemCount(0)
                    .itemEquipmentCount(0)
                    .build();
        }

        List<Long> versionIds = versionMapper.selectListByRegulationId(regulation.getId()).stream()
                .map(MesQaInspectionRegulationVersionDO::getId)
                .filter(Objects::nonNull)
                .toList();
        Long activeOrderReferenceCount =
                activeOrderMapper.selectCountByQaRegulationOrVersionIds(regulation.getId(), versionIds);
        Long pqcTaskReferenceCount =
                pqcInspectionTaskMapper.selectCountByRegulationVersionIds(versionIds);
        if (positiveCount(activeOrderReferenceCount) || positiveCount(pqcTaskReferenceCount)) {
            throw exception(QA_INSPECTION_REGULATION_RESET_REFERENCED,
                    "activeOrder=" + activeOrderReferenceCount + ", pqcTask=" + pqcTaskReferenceCount);
        }

        int versionCount = versionIds.size();
        int processCount = toIntCount(processMapper.selectCountByVersionIds(versionIds));
        int itemCount = toIntCount(itemMapper.selectCountByVersionIds(versionIds));
        int itemEquipmentCount = toIntCount(itemEquipmentMapper.selectCountByVersionIds(versionIds));
        itemEquipmentMapper.deleteByVersionIds(versionIds);
        itemMapper.deleteByVersionIds(versionIds);
        processMapper.deleteByVersionIds(versionIds);
        versionMapper.deleteByRegulationId(regulation.getId());
        regulationMapper.deleteById(regulation.getId());

        return MesQaInspectionRegulationResetRespVO.builder()
                .dccProjectCodeId(dccProjectCode.getId())
                .regulationId(regulation.getId())
                .versionCount(versionCount)
                .processCount(processCount)
                .itemCount(itemCount)
                .itemEquipmentCount(itemEquipmentCount)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesQaInspectionRegulationPublishedVersionRespVO publish(MesQaInspectionRegulationSaveReqVO reqVO) {
        DccProjectCodeDO dccProjectCode = validateRequest(reqVO);
        DraftContext context = saveDraftInternal(reqVO, dccProjectCode);
        LocalDateTime publishedAt = LocalDateTime.now();

        List<MesQaInspectionRegulationVersionDO> publishedVersions = versionMapper
                .selectListByRegulationId(context.regulation().getId()).stream()
                .filter(version -> version != null && version.getId() != null
                        && Objects.equals(STATUS_PUBLISHED, version.getLifecycleStatus()))
                .toList();
        for (MesQaInspectionRegulationVersionDO currentPublished : publishedVersions) {
            versionMapper.updateById(new MesQaInspectionRegulationVersionDO()
                    .setId(currentPublished.getId())
                    .setLifecycleStatus(STATUS_RETIRED)
                    .setRetiredAt(publishedAt));
        }

        versionMapper.updateById(new MesQaInspectionRegulationVersionDO()
                .setId(context.version().getId())
                .setLifecycleStatus(STATUS_PUBLISHED)
                .setPublishedAt(publishedAt));
        context.version().setLifecycleStatus(STATUS_PUBLISHED).setPublishedAt(publishedAt);

        regulationMapper.updateById(new MesQaInspectionRegulationDO()
                .setId(context.regulation().getId())
                .setLifecycleStatus(STATUS_PUBLISHED)
                .setCurrentVersionId(context.version().getId())
                .setRegulationCode(StrUtil.trim(reqVO.getRegulationCode()))
                .setRegulationName(StrUtil.trim(reqVO.getRegulationName())));
        context.regulation()
                .setLifecycleStatus(STATUS_PUBLISHED)
                .setCurrentVersionId(context.version().getId())
                .setRegulationCode(StrUtil.trim(reqVO.getRegulationCode()))
                .setRegulationName(StrUtil.trim(reqVO.getRegulationName()));

        return buildVersionResp(context.regulation(), context.version());
    }

    @Override
    public MesQaInspectionRegulationPublishedVersionRespVO getPublishedVersion(
            Long dccProjectCodeId, Long versionId) {
        requireEnabledDccProjectCode(dccProjectCodeId);
        MesQaInspectionRegulationDO regulation = requireRegulation(dccProjectCodeId);
        MesQaInspectionRegulationVersionDO version = versionId == null
                ? versionMapper.selectLatestPublishedByRegulationId(regulation.getId())
                : versionMapper.selectById(versionId);
        if (version == null || !Objects.equals(version.getRegulationId(), regulation.getId())) {
            throw exception(QA_INSPECTION_REGULATION_VERSION_NOT_EXISTS, versionId);
        }
        boolean productionCurrent = versionId == null;
        boolean allowed = productionCurrent
                ? Objects.equals(STATUS_PUBLISHED, version.getLifecycleStatus())
                : Set.of(STATUS_DRAFT, STATUS_PUBLISHED, STATUS_RETIRED).contains(version.getLifecycleStatus());
        if (!allowed) {
            throw exception(QA_INSPECTION_REGULATION_VERSION_NOT_PUBLISHED, version.getId());
        }
        return buildVersionResp(regulation, version);
    }

    @Override
    public List<MesQaInspectionRegulationVersionOptionRespVO> listVersions(Long dccProjectCodeId) {
        requireEnabledDccProjectCode(dccProjectCodeId);
        MesQaInspectionRegulationDO regulation = regulationMapper.selectByDccProjectCodeId(dccProjectCodeId);
        if (regulation == null) {
            return List.of();
        }
        Long currentVersionId = regulation.getCurrentVersionId();
        return versionMapper.selectListByRegulationId(regulation.getId()).stream()
                .map(version -> MesQaInspectionRegulationVersionOptionRespVO.builder()
                        .dccProjectCodeId(dccProjectCodeId)
                        .regulationId(regulation.getId())
                        .versionId(version.getId())
                        .versionNo(version.getVersionNo())
                        .lifecycleStatus(version.getLifecycleStatus())
                        .effectiveDate(version.getEffectiveDate())
                        .publishedAt(version.getPublishedAt())
                        .retiredAt(version.getRetiredAt())
                        .currentPublished(Objects.equals(currentVersionId, version.getId())
                                && Objects.equals(STATUS_PUBLISHED, version.getLifecycleStatus()))
                        .build())
                .toList();
    }

    @Override
    public MesQaInspectionRegulationPublishedVersionRespVO getCurrent(Long dccProjectCodeId) {
        requireEnabledDccProjectCode(dccProjectCodeId);
        MesQaInspectionRegulationDO regulation = regulationMapper.selectByDccProjectCodeId(dccProjectCodeId);
        if (regulation == null) {
            return null;
        }
        MesQaInspectionRegulationVersionDO latestDraft =
                versionMapper.selectLatestDraftByRegulationId(regulation.getId());
        MesQaInspectionRegulationVersionDO version = latestDraft != null
                ? latestDraft
                : versionMapper.selectLatestPublishedByRegulationId(regulation.getId());
        if (version == null) {
            return null;
        }
        if (version == null || !Objects.equals(version.getRegulationId(), regulation.getId())) {
            throw exception(QA_INSPECTION_REGULATION_VERSION_NOT_EXISTS, version.getId());
        }
        if (!Set.of(STATUS_DRAFT, STATUS_PUBLISHED).contains(version.getLifecycleStatus())) {
            throw exception(QA_INSPECTION_REGULATION_VERSION_NOT_PUBLISHED, version.getId());
        }
        return buildVersionResp(regulation, version);
    }

    @Override
    public MesQaInspectionRegulationPublishedVersionRespVO getLockedVersionForOrder(
            Long dccProjectCodeId, Long qaRegulationId, Long qaRegulationVersionId) {
        MesQaInspectionRegulationDO regulation = requireLockedRegulation(dccProjectCodeId, qaRegulationId);
        MesQaInspectionRegulationVersionDO version =
                requireLockedVersion(regulation.getId(), qaRegulationVersionId);
        return buildVersionResp(regulation, version);
    }

    @Override
    public List<MesQaInspectionRegulationProjectStatusRespVO> getProjectStatuses(
            Collection<Long> dccProjectCodeIds) {
        List<Long> requestedIds = dccProjectCodeIds == null ? List.of() : dccProjectCodeIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (requestedIds.isEmpty()) {
            return List.of();
        }
        Map<Long, MesQaInspectionRegulationDO> regulationByDccProject = regulationMapper
                .selectListByDccProjectCodeIds(requestedIds).stream()
                .collect(Collectors.toMap(MesQaInspectionRegulationDO::getDccProjectCodeId,
                        Function.identity(), (left, right) -> left, LinkedHashMap::new));
        return requestedIds.stream()
                .map(dccProjectCodeId -> {
                    MesQaInspectionRegulationDO regulation = regulationByDccProject.get(dccProjectCodeId);
                    MesQaInspectionRegulationVersionDO latestDraft = regulation == null ? null
                            : versionMapper.selectLatestDraftByRegulationId(regulation.getId());
                    MesQaInspectionRegulationVersionDO published = regulation == null
                            ? null : versionMapper.selectLatestPublishedByRegulationId(regulation.getId());
                    return buildProjectStatus(dccProjectCodeId, regulation, latestDraft, published);
                })
                .toList();
    }

    private DraftContext saveDraftInternal(MesQaInspectionRegulationSaveReqVO reqVO,
                                           DccProjectCodeDO dccProjectCode) {
        MesQaInspectionRegulationDO regulation = resolveRegulation(reqVO, dccProjectCode);
        MesQaInspectionRegulationVersionDO version = versionMapper.selectByRegulationIdAndVersionNo(
                regulation.getId(), StrUtil.trim(reqVO.getVersionNo()));
        if (version != null && Objects.equals(version.getLifecycleStatus(), STATUS_PUBLISHED)) {
            throw exception(QA_INSPECTION_REGULATION_VERSION_IMMUTABLE, version.getId());
        }
        if (version != null && !Objects.equals(version.getLifecycleStatus(), STATUS_DRAFT)) {
            throw exception(QA_INSPECTION_REGULATION_VERSION_CONFLICT, version.getId());
        }

        String inspectionTypeRulesJson = JSON.toJSONString(reqVO.getInspectionTypeRules());
        String snapshotJson = JSON.toJSONString(reqVO);
        if (version == null) {
            version = MesQaInspectionRegulationVersionDO.builder()
                    .regulationId(regulation.getId())
                    .versionNo(StrUtil.trim(reqVO.getVersionNo()))
                    .lifecycleStatus(STATUS_DRAFT)
                    .effectiveDate(reqVO.getEffectiveDate())
                    .inspectionTypeRulesJson(inspectionTypeRulesJson)
                    .finalInspectionApplicable(reqVO.getFinalInspectionApplicable())
                    .finalInspectionNotApplicableReason(normalizeFinalInspectionReason(reqVO))
                    .snapshotJson(snapshotJson)
                    .build();
            versionMapper.insert(version);
        } else {
            versionMapper.updateById(new MesQaInspectionRegulationVersionDO()
                    .setId(version.getId())
                    .setEffectiveDate(reqVO.getEffectiveDate())
                    .setInspectionTypeRulesJson(inspectionTypeRulesJson)
                    .setFinalInspectionApplicable(reqVO.getFinalInspectionApplicable())
                    .setFinalInspectionNotApplicableReason(normalizeFinalInspectionReason(reqVO))
                    .setSnapshotJson(snapshotJson));
            version.setEffectiveDate(reqVO.getEffectiveDate())
                    .setInspectionTypeRulesJson(inspectionTypeRulesJson)
                    .setFinalInspectionApplicable(reqVO.getFinalInspectionApplicable())
                    .setFinalInspectionNotApplicableReason(normalizeFinalInspectionReason(reqVO))
                    .setSnapshotJson(snapshotJson);
            itemMapper.deletePhysicallyByVersionId(version.getId());
            processMapper.deletePhysicallyByVersionId(version.getId());
        }

        int finalInspectionQuantity = resolveFinalInspectionQuantity(reqVO);
        for (MesQaInspectionRegulationSaveReqVO.InspectionProcess processReq : reqVO.getProcesses()) {
            MesQaInspectionRegulationProcessDO process = MesQaInspectionRegulationProcessDO.builder()
                    .regulationVersionId(version.getId())
                    .processCode(StrUtil.trim(processReq.getProcessCode()))
                    .processName(StrUtil.trim(processReq.getProcessName()))
                    .sort(processReq.getSort())
                    .build();
            processMapper.insert(process);
            for (MesQaInspectionRegulationSaveReqVO.InspectionItem itemReq : processReq.getItems()) {
                for (String inspectionType : normalizedInspectionTypes(itemReq.getApplicableInspectionTypes())) {
                    itemMapper.insert(toItemDO(version.getId(), process.getId(), itemReq,
                            inspectionType, finalInspectionQuantity));
                }
            }
        }
        return new DraftContext(regulation, version);
    }

    private MesQaInspectionRegulationDO resolveRegulation(MesQaInspectionRegulationSaveReqVO reqVO,
                                                          DccProjectCodeDO dccProjectCode) {
        MesQaInspectionRegulationDO regulation = reqVO.getRegulationId() == null
                ? regulationMapper.selectByDccProjectCodeId(dccProjectCode.getId())
                : regulationMapper.selectById(reqVO.getRegulationId());
        if (reqVO.getRegulationId() != null && regulation == null) {
            throw exception(QA_INSPECTION_REGULATION_NOT_EXISTS, reqVO.getRegulationId());
        }
        if (regulation != null
                && !Objects.equals(regulation.getDccProjectCodeId(), dccProjectCode.getId())) {
            throw exception(QA_INSPECTION_REGULATION_DCC_PROJECT_INVALID, dccProjectCode.getId());
        }
        if (regulation != null) {
            if (!Objects.equals(regulation.getLifecycleStatus(), STATUS_PUBLISHED)) {
                regulationMapper.updateById(new MesQaInspectionRegulationDO()
                        .setId(regulation.getId())
                        .setLifecycleStatus(STATUS_DRAFT)
                        .setRegulationCode(StrUtil.trim(reqVO.getRegulationCode()))
                        .setRegulationName(StrUtil.trim(reqVO.getRegulationName())));
                regulation.setLifecycleStatus(STATUS_DRAFT)
                        .setRegulationCode(StrUtil.trim(reqVO.getRegulationCode()))
                        .setRegulationName(StrUtil.trim(reqVO.getRegulationName()));
            }
            return regulation;
        }
        regulation = MesQaInspectionRegulationDO.builder()
                .dccProjectCodeId(dccProjectCode.getId())
                .ownerModule(MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA)
                .regulationCode(StrUtil.trim(reqVO.getRegulationCode()))
                .regulationName(StrUtil.trim(reqVO.getRegulationName()))
                .lifecycleStatus(STATUS_DRAFT)
                .build();
        try {
            regulationMapper.insert(regulation);
        } catch (DuplicateKeyException ex) {
            if (hasConstraint(ex, "uk_mes_qa_regulation_active_dcc")) {
                throw exception(QA_INSPECTION_REGULATION_DCC_PROJECT_DUPLICATE, dccProjectCode.getId());
            }
            throw ex;
        }
        return regulation;
    }

    private MesQaInspectionRegulationPublishedVersionRespVO buildVersionResp(
            MesQaInspectionRegulationDO regulation, MesQaInspectionRegulationVersionDO version) {
        JSONObject snapshot = parseSnapshot(version);
        List<MesQaInspectionRegulationProcessDO> processes = processMapper.selectListByVersionId(version.getId());
        if (processes.isEmpty()) {
            throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, version.getId());
        }
        List<MesQaInspectionRegulationItemDO> items = itemMapper.selectListByVersionId(version.getId());
        return MesQaInspectionRegulationPublishedVersionRespVO.builder()
                .dccProjectCodeId(regulation.getDccProjectCodeId())
                .regulationId(regulation.getId())
                .publishedVersionId(version.getId())
                .versionNo(version.getVersionNo())
                .effectiveDate(version.getEffectiveDate())
                .publishedAt(version.getPublishedAt())
                .immutable(!Objects.equals(version.getLifecycleStatus(), STATUS_DRAFT))
                .lifecycleStatus(version.getLifecycleStatus())
                .regulationCode(firstNonBlank(snapshot.getString("regulationCode"), regulation.getRegulationCode()))
                .regulationName(firstNonBlank(snapshot.getString("regulationName"), regulation.getRegulationName()))
                .finalInspectionApplicable(version.getFinalInspectionApplicable())
                .finalInspectionNotApplicableReason(version.getFinalInspectionNotApplicableReason())
                .inspectionTypeRules(parseInspectionTypeRules(version))
                .processes(buildProcessResponses(processes, items, version.getId()))
                .build();
    }

    private static List<MesQaInspectionRegulationPublishedVersionRespVO.InspectionProcess> buildProcessResponses(
            List<MesQaInspectionRegulationProcessDO> processes,
            List<MesQaInspectionRegulationItemDO> items,
            Long versionId) {
        Map<Long, List<MesQaInspectionRegulationItemDO>> itemsByProcess = items.stream()
                .collect(Collectors.groupingBy(MesQaInspectionRegulationItemDO::getQaProcessId));
        Set<Long> processIds = processes.stream().map(MesQaInspectionRegulationProcessDO::getId).collect(Collectors.toSet());
        if (items.stream().anyMatch(item -> item.getQaProcessId() == null || !processIds.contains(item.getQaProcessId()))) {
            throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, versionId);
        }
        return processes.stream()
                .map(process -> MesQaInspectionRegulationPublishedVersionRespVO.InspectionProcess.builder()
                        .qaProcessId(process.getId())
                        .processCode(process.getProcessCode())
                        .processName(process.getProcessName())
                        .sort(process.getSort())
                        .items(buildItemResponses(itemsByProcess.get(process.getId())))
                        .build())
                .toList();
    }

    private static List<MesQaInspectionRegulationPublishedVersionRespVO.InspectionItem> buildItemResponses(
            List<MesQaInspectionRegulationItemDO> processItems) {
        if (CollUtil.isEmpty(processItems)) {
            return List.of();
        }
        Map<String, List<MesQaInspectionRegulationItemDO>> rowsByItemCode = processItems.stream()
                .sorted(Comparator.comparing(MesQaInspectionRegulationItemDO::getItemSort,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MesQaInspectionRegulationItemDO::getItemCode)
                        .thenComparing(MesQaInspectionRegulationItemDO::getInspectionType))
                .collect(Collectors.groupingBy(MesQaInspectionRegulationItemDO::getItemCode,
                        LinkedHashMap::new, Collectors.toList()));
        return rowsByItemCode.values().stream()
                .map(MesQaInspectionRegulationServiceImpl::buildItemResponse)
                .toList();
    }

    private static MesQaInspectionRegulationPublishedVersionRespVO.InspectionItem buildItemResponse(
            List<MesQaInspectionRegulationItemDO> rows) {
        MesQaInspectionRegulationItemDO source = rows.get(0);
        List<String> applicableTypes = rows.stream()
                .map(MesQaInspectionRegulationItemDO::getInspectionType)
                .distinct()
                .sorted(Comparator.comparingInt(type -> INSPECTION_TYPE_ORDER.getOrDefault(type, 99)))
                .toList();
        Integer firstQuantity = rows.stream()
                .filter(item -> Objects.equals(item.getInspectionType(), "FIRST"))
                .map(MesQaInspectionRegulationItemDO::getFirstInspectionQuantity)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
        BigDecimal patrolRatio = rows.stream()
                .filter(item -> isPatrolInspectionType(item.getInspectionType()))
                .map(MesQaInspectionRegulationItemDO::getPatrolInspectionRatio)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
        return MesQaInspectionRegulationPublishedVersionRespVO.InspectionItem.builder()
                .itemSort(source.getItemSort())
                .itemCode(source.getItemCode())
                .itemName(source.getItemName())
                .inspectionMethod(source.getInspectionMethod())
                .inspectionTool(source.getInspectionTool())
                .samplingPlanText(source.getSamplingPlanText())
                .standardText(source.getStandardText())
                .standardLowerLimit(source.getStandardLowerLimit())
                .standardUpperLimit(source.getStandardUpperLimit())
                .standardUnit(source.getStandardUnit())
                .standardPrecision(source.getStandardPrecision())
                .resultType(source.getResultType())
                .applicableInspectionTypes(applicableTypes)
                .firstInspectionQuantity(firstQuantity)
                .patrolInspectionRatio(patrolRatio)
                .critical(source.getCritical())
                .failureRule(source.getFailureRule())
                .sourceNote(source.getSourceNote())
                .sourceOriginalPage(source.getSourceOriginalPage())
                .sourceOriginalItem(source.getSourceOriginalItem())
                .sourceOriginalExcerpt(source.getSourceOriginalExcerpt())
                .sourceOriginalMethod(source.getSourceOriginalMethod())
                .equipmentOptions(List.of())
                .build();
    }

    private static List<MesQaInspectionRegulationPublishedVersionRespVO.InspectionTypeRule>
    parseInspectionTypeRules(MesQaInspectionRegulationVersionDO version) {
        if (StrUtil.isBlank(version.getInspectionTypeRulesJson())) {
            throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, version.getId());
        }
        try {
            List<MesQaInspectionRegulationSaveReqVO.InspectionTypeRule> sourceRules = JSON.parseArray(
                    version.getInspectionTypeRulesJson(), MesQaInspectionRegulationSaveReqVO.InspectionTypeRule.class);
            return sourceRules.stream()
                    .map(rule -> MesQaInspectionRegulationPublishedVersionRespVO.InspectionTypeRule.builder()
                            .key(rule.getKey())
                            .inspectionType(rule.getInspectionType())
                            .label(rule.getLabel())
                            .roundLabel(rule.getRoundLabel())
                            .required(rule.getRequired())
                            .fixedQuantity(rule.getFixedQuantity())
                            .notApplicableReason(rule.getNotApplicableReason())
                            .taskRule(rule.getTaskRule())
                            .releaseGate(rule.getReleaseGate())
                            .build())
                    .toList();
        } catch (RuntimeException ex) {
            throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, version.getId());
        }
    }

    private DccProjectCodeDO validateRequest(MesQaInspectionRegulationSaveReqVO reqVO) {
        DccProjectCodeDO dccProjectCode = requireEnabledDccProjectCode(reqVO.getDccProjectCodeId());
        if (StrUtil.isBlank(reqVO.getRegulationCode()) || StrUtil.isBlank(reqVO.getRegulationName())
                || StrUtil.isBlank(reqVO.getVersionNo())) {
            throw exception(QA_INSPECTION_REGULATION_ITEM_INVALID, "regulation header");
        }
        validateFinalInspectionApplicability(reqVO);
        validateInspectionTypeRules(reqVO.getInspectionTypeRules());
        validateProcesses(reqVO);
        return dccProjectCode;
    }

    private DccProjectCodeDO requireEnabledDccProjectCode(Long dccProjectCodeId) {
        if (dccProjectCodeId == null) {
            throw exception(QA_INSPECTION_REGULATION_DCC_PROJECT_INVALID, null);
        }
        DccProjectCodeDO projectCode = dccProjectCodeMapper.selectById(dccProjectCodeId);
        if (projectCode == null || !Objects.equals(projectCode.getStatus(), DccProjectCodeStatusConstants.ENABLE)) {
            throw exception(QA_INSPECTION_REGULATION_DCC_PROJECT_INVALID, dccProjectCodeId);
        }
        return projectCode;
    }

    private MesQaInspectionRegulationDO requireLockedRegulation(Long dccProjectCodeId, Long qaRegulationId) {
        if (dccProjectCodeId == null || dccProjectCodeMapper.selectById(dccProjectCodeId) == null) {
            throw exception(QA_INSPECTION_REGULATION_DCC_PROJECT_INVALID, dccProjectCodeId);
        }
        if (qaRegulationId == null) {
            throw exception(QA_INSPECTION_REGULATION_NOT_EXISTS, null);
        }
        MesQaInspectionRegulationDO regulation = regulationMapper.selectById(qaRegulationId);
        if (regulation == null) {
            throw exception(QA_INSPECTION_REGULATION_NOT_EXISTS, qaRegulationId);
        }
        if (!Objects.equals(regulation.getDccProjectCodeId(), dccProjectCodeId)) {
            throw exception(QA_INSPECTION_REGULATION_DCC_PROJECT_INVALID, dccProjectCodeId);
        }
        return regulation;
    }

    private MesQaInspectionRegulationVersionDO requireLockedVersion(Long qaRegulationId, Long qaRegulationVersionId) {
        if (qaRegulationVersionId == null) {
            throw exception(QA_INSPECTION_REGULATION_VERSION_NOT_EXISTS, null);
        }
        MesQaInspectionRegulationVersionDO version = versionMapper.selectById(qaRegulationVersionId);
        if (version == null || !Objects.equals(version.getRegulationId(), qaRegulationId)) {
            throw exception(QA_INSPECTION_REGULATION_VERSION_NOT_EXISTS, qaRegulationVersionId);
        }
        if (!Set.of(STATUS_PUBLISHED, STATUS_RETIRED).contains(version.getLifecycleStatus())) {
            throw exception(QA_INSPECTION_REGULATION_VERSION_NOT_PUBLISHED, version.getId());
        }
        return version;
    }

    private MesQaInspectionRegulationDO requireRegulation(Long dccProjectCodeId) {
        MesQaInspectionRegulationDO regulation = regulationMapper.selectByDccProjectCodeId(dccProjectCodeId);
        if (regulation == null) {
            throw exception(QA_INSPECTION_REGULATION_NOT_EXISTS, dccProjectCodeId);
        }
        return regulation;
    }

    private static void validateFinalInspectionApplicability(MesQaInspectionRegulationSaveReqVO reqVO) {
        if (reqVO.getFinalInspectionApplicable() == null) {
            throw exception(QA_INSPECTION_REGULATION_FINAL_APPLICABILITY_INVALID, "末检适用性未显式配置");
        }
        Set<String> actualTypes = actualInspectionTypes(reqVO);
        String requiredType = CollUtil.emptyIfNull(reqVO.getInspectionTypeRules()).stream()
                .filter(Objects::nonNull)
                .filter(rule -> Boolean.TRUE.equals(rule.getRequired()))
                .map(MesQaInspectionRegulationSaveReqVO.InspectionTypeRule::getInspectionType)
                .map(MesQaInspectionRegulationServiceImpl::normalizeInspectionType)
                .filter(type -> Objects.equals(type, "FINAL"))
                .findFirst()
                .orElse(null);
        if (Boolean.TRUE.equals(reqVO.getFinalInspectionApplicable())
                && (!actualTypes.contains("FINAL") || !Objects.equals(requiredType, "FINAL"))) {
            throw exception(QA_INSPECTION_REGULATION_FINAL_APPLICABILITY_INVALID,
                    "末检适用时必须配置 FINAL 检验规则和检验项目");
        }
        if (Boolean.FALSE.equals(reqVO.getFinalInspectionApplicable())
                && StrUtil.isBlank(reqVO.getFinalInspectionNotApplicableReason())) {
            throw exception(QA_INSPECTION_REGULATION_FINAL_APPLICABILITY_INVALID, "末检不适用依据不能为空");
        }
        if (Boolean.FALSE.equals(reqVO.getFinalInspectionApplicable())
                && (actualTypes.contains("FINAL") || Objects.equals(requiredType, "FINAL"))) {
            throw exception(QA_INSPECTION_REGULATION_FINAL_APPLICABILITY_INVALID,
                    "末检不适用时不得配置 FINAL 检验项目");
        }
        if (Boolean.TRUE.equals(reqVO.getFinalInspectionApplicable())
                && StrUtil.isNotBlank(reqVO.getFinalInspectionNotApplicableReason())) {
            throw exception(QA_INSPECTION_REGULATION_FINAL_APPLICABILITY_INVALID, "末检适用时不得填写不适用依据");
        }
    }

    private static Set<String> actualInspectionTypes(MesQaInspectionRegulationSaveReqVO reqVO) {
        if (reqVO == null || CollUtil.isEmpty(reqVO.getProcesses())) {
            return Set.of();
        }
        LinkedHashSet<String> actualTypes = new LinkedHashSet<>();
        for (MesQaInspectionRegulationSaveReqVO.InspectionProcess process : reqVO.getProcesses()) {
            if (process == null || CollUtil.isEmpty(process.getItems())) {
                continue;
            }
            for (MesQaInspectionRegulationSaveReqVO.InspectionItem item : process.getItems()) {
                if (item != null) {
                    actualTypes.addAll(normalizedInspectionTypes(item.getApplicableInspectionTypes()));
                }
            }
        }
        return actualTypes;
    }

    private static void validateInspectionTypeRules(
            List<MesQaInspectionRegulationSaveReqVO.InspectionTypeRule> rules) {
        if (CollUtil.isEmpty(rules)) {
            throw exception(QA_INSPECTION_REGULATION_ITEM_INVALID, "inspectionTypeRules");
        }
        Set<String> keys = new LinkedHashSet<>();
        for (MesQaInspectionRegulationSaveReqVO.InspectionTypeRule rule : rules) {
            if (rule == null || StrUtil.isBlank(rule.getKey()) || StrUtil.isBlank(rule.getInspectionType())
                    || !ALLOWED_INSPECTION_TYPES.contains(normalizeInspectionType(rule.getInspectionType()))
                    || !keys.add(StrUtil.trim(rule.getKey()))) {
                throw exception(QA_INSPECTION_REGULATION_ITEM_INVALID, "inspectionTypeRules");
            }
        }
    }

    private static void validateProcesses(MesQaInspectionRegulationSaveReqVO reqVO) {
        if (CollUtil.isEmpty(reqVO.getProcesses())) {
            throw exception(QA_INSPECTION_REGULATION_ITEM_INVALID, "processes");
        }
        Set<String> processCodes = new LinkedHashSet<>();
        Set<Integer> processSorts = new LinkedHashSet<>();
        Set<String> itemCodes = new LinkedHashSet<>();
        for (MesQaInspectionRegulationSaveReqVO.InspectionProcess process : reqVO.getProcesses()) {
            if (process == null || StrUtil.isBlank(process.getProcessCode()) || StrUtil.isBlank(process.getProcessName())
                    || process.getSort() == null || process.getSort() <= 0
                    || !processCodes.add(StrUtil.trim(process.getProcessCode()))
                    || !processSorts.add(process.getSort()) || CollUtil.isEmpty(process.getItems())) {
                throw exception(QA_INSPECTION_REGULATION_ITEM_INVALID, "processes");
            }
            Set<Integer> itemSorts = new LinkedHashSet<>();
            for (MesQaInspectionRegulationSaveReqVO.InspectionItem item : process.getItems()) {
                validateItem(item, itemCodes, itemSorts);
            }
        }
    }

    private static void validateItem(MesQaInspectionRegulationSaveReqVO.InspectionItem item,
                                     Set<String> itemCodes, Set<Integer> itemSorts) {
        if (item == null || item.getItemSort() == null || item.getItemSort() <= 0
                || !itemSorts.add(item.getItemSort()) || StrUtil.isBlank(item.getItemCode())
                || !itemCodes.add(StrUtil.trim(item.getItemCode())) || StrUtil.isBlank(item.getItemName())
                || StrUtil.isBlank(item.getInspectionMethod()) || StrUtil.isBlank(item.getInspectionTool())
                || StrUtil.isBlank(item.getSamplingPlanText()) || StrUtil.isBlank(item.getStandardText())
                || StrUtil.isBlank(item.getResultType()) || item.getCritical() == null) {
            throw exception(QA_INSPECTION_REGULATION_ITEM_INVALID,
                    item == null ? "item" : item.getItemCode());
        }
        String resultType = StrUtil.trim(item.getResultType());
        if (!ALLOWED_RESULT_TYPES.contains(resultType)) {
            throw exception(QA_INSPECTION_REGULATION_ITEM_INVALID, item.getItemCode() + ".resultType");
        }
        Set<String> applicableTypes = normalizedInspectionTypes(item.getApplicableInspectionTypes());
        if (applicableTypes.isEmpty()) {
            throw exception(QA_INSPECTION_REGULATION_ITEM_INVALID, item.getItemCode() + ".applicableInspectionTypes");
        }
        if (applicableTypes.contains("FIRST")
                && (item.getFirstInspectionQuantity() == null || item.getFirstInspectionQuantity() <= 0)) {
            throw exception(QA_INSPECTION_REGULATION_ITEM_INVALID, item.getItemCode() + ".firstInspectionQuantity");
        }
        if (applicableTypes.stream().anyMatch(MesQaInspectionRegulationServiceImpl::isPatrolInspectionType)
                && !positive(item.getPatrolInspectionRatio())) {
            throw exception(QA_INSPECTION_REGULATION_ITEM_INVALID, item.getItemCode() + ".patrolInspectionRatio");
        }
        if (Objects.equals(resultType, "NUMERIC")
                && (item.getStandardLowerLimit() == null || item.getStandardUpperLimit() == null)) {
            throw exception(QA_INSPECTION_REGULATION_ITEM_INVALID, item.getItemCode() + ".numericStandard");
        }
    }

    private static int resolveFinalInspectionQuantity(MesQaInspectionRegulationSaveReqVO reqVO) {
        if (!Boolean.TRUE.equals(reqVO.getFinalInspectionApplicable())) {
            return 0;
        }
        boolean hasFinalItem = reqVO.getProcesses().stream()
                .flatMap(process -> process.getItems().stream())
                .anyMatch(item -> normalizedInspectionTypes(item.getApplicableInspectionTypes()).contains("FINAL"));
        if (!hasFinalItem) {
            return 0;
        }
        return reqVO.getInspectionTypeRules().stream()
                .filter(rule -> Objects.equals(normalizeInspectionType(rule.getInspectionType()), "FINAL"))
                .map(MesQaInspectionRegulationSaveReqVO.InspectionTypeRule::getFixedQuantity)
                .filter(quantity -> quantity != null && quantity > 0)
                .findFirst()
                .orElseThrow(() -> exception(QA_INSPECTION_REGULATION_ITEM_INVALID, "FINAL.fixedQuantity"));
    }

    private static Set<String> normalizedInspectionTypes(List<String> inspectionTypes) {
        if (CollUtil.isEmpty(inspectionTypes)) {
            return Set.of();
        }
        LinkedHashSet<String> normalized = inspectionTypes.stream()
                .map(MesQaInspectionRegulationServiceImpl::normalizeInspectionType)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalized.stream().anyMatch(type -> !ALLOWED_INSPECTION_TYPES.contains(type))) {
            throw exception(QA_INSPECTION_REGULATION_ITEM_INVALID, "applicableInspectionTypes");
        }
        return normalized;
    }

    private static String normalizeInspectionType(String inspectionType) {
        return StrUtil.trim(inspectionType);
    }

    private static boolean isPatrolInspectionType(String inspectionType) {
        return Objects.equals(inspectionType, "PATROL")
                || Objects.equals(inspectionType, "PATROL_AM")
                || Objects.equals(inspectionType, "PATROL_PM");
    }

    private static MesQaInspectionRegulationItemDO toItemDO(
            Long versionId, Long qaProcessId, MesQaInspectionRegulationSaveReqVO.InspectionItem item,
            String inspectionType, int finalInspectionQuantity) {
        Integer fixedQuantity = Objects.equals(inspectionType, "FIRST")
                ? item.getFirstInspectionQuantity()
                : Objects.equals(inspectionType, "FINAL") ? finalInspectionQuantity : null;
        return MesQaInspectionRegulationItemDO.builder()
                .regulationVersionId(versionId)
                .qaProcessId(qaProcessId)
                .itemSort(item.getItemSort())
                .inspectionType(inspectionType)
                .itemCode(StrUtil.trim(item.getItemCode()))
                .itemName(StrUtil.trim(item.getItemName()))
                .inspectionMethod(StrUtil.trim(item.getInspectionMethod()))
                .inspectionTool(StrUtil.trim(item.getInspectionTool()))
                .samplingPlanText(StrUtil.trim(item.getSamplingPlanText()))
                .standardText(StrUtil.trim(item.getStandardText()))
                .standardLowerLimit(item.getStandardLowerLimit())
                .standardUpperLimit(item.getStandardUpperLimit())
                .standardUnit(StrUtil.trim(item.getStandardUnit()))
                .standardPrecision(item.getStandardPrecision())
                .equipmentRequired(false)
                .resultType(StrUtil.trim(item.getResultType()))
                .firstInspectionQuantity(fixedQuantity)
                .patrolInspectionRatio(isPatrolInspectionType(inspectionType)
                        ? item.getPatrolInspectionRatio() : null)
                .critical(item.getCritical())
                .failureRule(StrUtil.trim(item.getFailureRule()))
                .sourceNote(StrUtil.trim(item.getSourceNote()))
                .sourceOriginalPage(item.getSourceOriginalPage())
                .sourceOriginalItem(StrUtil.trim(item.getSourceOriginalItem()))
                .sourceOriginalExcerpt(StrUtil.trim(item.getSourceOriginalExcerpt()))
                .sourceOriginalMethod(StrUtil.trim(item.getSourceOriginalMethod()))
                .build();
    }

    private static MesQaInspectionRegulationProjectStatusRespVO buildProjectStatus(
            Long dccProjectCodeId, MesQaInspectionRegulationDO regulation,
            MesQaInspectionRegulationVersionDO latestDraft,
            MesQaInspectionRegulationVersionDO published) {
        MesQaInspectionRegulationProjectStatusRespVO status = new MesQaInspectionRegulationProjectStatusRespVO();
        status.setDccProjectCodeId(dccProjectCodeId);
        if (regulation == null) {
            status.setConfigured(false);
            status.setRegulationCount(0);
            status.setProductionReady(false);
            return status;
        }
        status.setConfigured(true);
        status.setRegulationCount(1);
        status.setRegulationId(regulation.getId());
        status.setCurrentVersionId(published == null ? null : published.getId());
        status.setRegulationCode(regulation.getRegulationCode());
        status.setRegulationName(regulation.getRegulationName());
        MesQaInspectionRegulationVersionDO editVersion = latestDraft != null ? latestDraft : published;
        status.setLifecycleStatus(editVersion == null ? regulation.getLifecycleStatus()
                : editVersion.getLifecycleStatus());
        status.setEditRegulationId(regulation.getId());
        status.setEditVersionId(editVersion == null ? null : editVersion.getId());
        boolean productionReady = published != null
                && Objects.equals(published.getRegulationId(), regulation.getId())
                && Objects.equals(published.getLifecycleStatus(), STATUS_PUBLISHED);
        status.setProductionReady(productionReady);
        status.setPublishedRegulationId(productionReady ? regulation.getId() : null);
        status.setPublishedVersionId(productionReady ? published.getId() : null);
        status.setPublishedVersionNo(productionReady ? published.getVersionNo() : null);
        return status;
    }

    private static boolean hasConstraint(Throwable throwable, String constraintName) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current.getMessage() != null && current.getMessage().contains(constraintName)) {
                return true;
            }
        }
        return false;
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

    private static String firstNonBlank(String first, String second) {
        return StrUtil.isNotBlank(first) ? first : second;
    }

    private static String normalizeFinalInspectionReason(MesQaInspectionRegulationSaveReqVO reqVO) {
        return Boolean.FALSE.equals(reqVO.getFinalInspectionApplicable())
                ? StrUtil.trim(reqVO.getFinalInspectionNotApplicableReason()) : null;
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private static boolean positiveCount(Long value) {
        return value != null && value > 0;
    }

    private static int toIntCount(Long value) {
        return Math.toIntExact(value == null ? 0L : value);
    }

    private record DraftContext(MesQaInspectionRegulationDO regulation,
                                MesQaInspectionRegulationVersionDO version) {
    }
}
