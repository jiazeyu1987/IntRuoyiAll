package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationPackageCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationPackagePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationPackageRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationRequirementItemCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationRequirementItemPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationRequirementItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationTraceEvaluateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationTraceIssueRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationTraceLinkCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationTraceLinkRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrValidationPackageDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrValidationRequirementItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrValidationTraceLinkDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrValidationPackageMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrValidationRequirementItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrValidationTraceLinkMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrValidationErrorCodeConstants.PRO_EDHR_VALIDATION_ITEM_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrValidationErrorCodeConstants.PRO_EDHR_VALIDATION_ITEM_TYPE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrValidationErrorCodeConstants.PRO_EDHR_VALIDATION_PACKAGE_CREATE_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrValidationErrorCodeConstants.PRO_EDHR_VALIDATION_PACKAGE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrValidationErrorCodeConstants.PRO_EDHR_VALIDATION_TRACE_GATE_BLOCKED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrValidationErrorCodeConstants.PRO_EDHR_VALIDATION_TRACE_LINK_INVALID;

@Service
public class MesProEdhrValidationServiceImpl implements MesProEdhrValidationService {

    private static final DateTimeFormatter PACKAGE_CODE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final String VALIDATION_STATUS_BLOCKED = "BLOCKED";
    private static final String VALIDATION_STATUS_PREPARED = "PREPARED";
    private static final String TRACE_STATUS_ACTIVE = "ACTIVE";
    private static final String TRACE_STATUS_BLOCKED = "BLOCKED";
    private static final String TRACE_STATUS_READY = "READY";
    private static final String ITEM_TYPE_URS = "URS";
    private static final String ITEM_TYPE_FRS = "FRS";
    private static final String ITEM_TYPE_RISK = "RISK";
    private static final String ITEM_TYPE_IQ = "IQ";
    private static final String ITEM_TYPE_OQ = "OQ";
    private static final String ITEM_TYPE_PQ = "PQ";
    private static final String LINK_TYPE_URS_FRS = "URS_FRS";
    private static final String LINK_TYPE_URS_RISK = "URS_RISK";
    private static final String LINK_TYPE_URS_VERIFICATION = "URS_VERIFICATION";
    private static final String NEXT_ACTION_EVALUATE = "登记URS/FRS/风险/IQ/OQ/PQ条目并补齐追溯关系后重新计算OQ Ready";
    private static final Set<String> ITEM_TYPES = Set.of(
            ITEM_TYPE_URS, ITEM_TYPE_FRS, ITEM_TYPE_RISK, ITEM_TYPE_IQ, ITEM_TYPE_OQ, ITEM_TYPE_PQ);
    private static final Set<String> VERIFICATION_ITEM_TYPES = Set.of(ITEM_TYPE_IQ, ITEM_TYPE_OQ, ITEM_TYPE_PQ);

    @Resource
    private MesProEdhrValidationPackageMapper packageMapper;
    @Resource
    private MesProEdhrValidationRequirementItemMapper itemMapper;
    @Resource
    private MesProEdhrValidationTraceLinkMapper traceLinkMapper;

    @Override
    public PageResult<MesProEdhrValidationPackageRespVO> getPackagePage(MesProEdhrValidationPackagePageReqVO reqVO) {
        return BeanUtils.toBean(packageMapper.selectPage(reqVO), MesProEdhrValidationPackageRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrValidationPackageRespVO createPackage(MesProEdhrValidationPackageCreateReqVO reqVO) {
        String packageCode = buildPackageCode();
        MesProEdhrValidationPackageDO validationPackage = new MesProEdhrValidationPackageDO()
                .setPackageCode(packageCode)
                .setPackageName(reqVO.getPackageName())
                .setCustomerProjectName(reqVO.getCustomerProjectName())
                .setCustomerName(reqVO.getCustomerName())
                .setSiteName(reqVO.getSiteName())
                .setSystemScope(reqVO.getSystemScope())
                .setValidationScope(reqVO.getValidationScope())
                .setReleaseTag(reqVO.getReleaseTag())
                .setSchemaVersion(reqVO.getSchemaVersion())
                .setTargetEnvironment(reqVO.getTargetEnvironment())
                .setValidationStatus(VALIDATION_STATUS_BLOCKED)
                .setOqReady(false)
                .setValidationOwnerName(reqVO.getValidationOwnerName())
                .setQaOwnerName(reqVO.getQaOwnerName())
                .setBlockedReason("验证包已创建，需登记URS/FRS/风险/IQ/OQ/PQ条目并完成追溯矩阵")
                .setTraceSummaryJson(buildTraceSummaryJson(packageCode, VALIDATION_STATUS_BLOCKED, 0, 0, 0, 0, 0, 0, 0, 1))
                .setRemark(reqVO.getRemark());
        if (packageMapper.insert(validationPackage) != 1 || validationPackage.getId() == null) {
            throw exception(PRO_EDHR_VALIDATION_PACKAGE_CREATE_FAILED);
        }
        return getPackageDetail(validationPackage.getId());
    }

    @Override
    public MesProEdhrValidationPackageRespVO getPackageDetail(Long id) {
        return BeanUtils.toBean(requirePackage(id), MesProEdhrValidationPackageRespVO.class);
    }

    @Override
    public PageResult<MesProEdhrValidationRequirementItemRespVO> getRequirementItemPage(
            MesProEdhrValidationRequirementItemPageReqVO reqVO) {
        requirePackage(reqVO.getPackageId());
        return BeanUtils.toBean(itemMapper.selectPage(reqVO), MesProEdhrValidationRequirementItemRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrValidationRequirementItemRespVO createRequirementItem(
            MesProEdhrValidationRequirementItemCreateReqVO reqVO) {
        requirePackage(reqVO.getPackageId());
        validateItemType(reqVO.getItemType());
        MesProEdhrValidationRequirementItemDO item = new MesProEdhrValidationRequirementItemDO()
                .setPackageId(reqVO.getPackageId())
                .setItemCode(reqVO.getItemCode())
                .setItemName(reqVO.getItemName())
                .setItemType(reqVO.getItemType())
                .setItemVersion(reqVO.getItemVersion())
                .setItemStatus(reqVO.getItemStatus())
                .setOwnerName(reqVO.getOwnerName())
                .setSignoffRole(reqVO.getSignoffRole())
                .setSourceDocument(reqVO.getSourceDocument())
                .setBusinessProcess(reqVO.getBusinessProcess())
                .setAcceptanceCriteria(reqVO.getAcceptanceCriteria())
                .setSort(reqVO.getSort() == null ? 0 : reqVO.getSort())
                .setRemark(reqVO.getRemark());
        if (itemMapper.insert(item) != 1 || item.getId() == null) {
            throw exception(PRO_EDHR_VALIDATION_PACKAGE_CREATE_FAILED);
        }
        return BeanUtils.toBean(itemMapper.selectById(item.getId()), MesProEdhrValidationRequirementItemRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrValidationTraceLinkRespVO createTraceLink(MesProEdhrValidationTraceLinkCreateReqVO reqVO) {
        requirePackage(reqVO.getPackageId());
        MesProEdhrValidationRequirementItemDO source = requireItemInPackage(reqVO.getPackageId(), reqVO.getSourceItemId());
        MesProEdhrValidationRequirementItemDO target = requireItemInPackage(reqVO.getPackageId(), reqVO.getTargetItemId());
        validateTraceLink(source, target, reqVO.getLinkType());

        MesProEdhrValidationTraceLinkDO traceLink = new MesProEdhrValidationTraceLinkDO()
                .setPackageId(reqVO.getPackageId())
                .setSourceItemId(source.getId())
                .setSourceItemCode(source.getItemCode())
                .setSourceItemType(source.getItemType())
                .setTargetItemId(target.getId())
                .setTargetItemCode(target.getItemCode())
                .setTargetItemType(target.getItemType())
                .setLinkType(reqVO.getLinkType())
                .setTraceStatus(TRACE_STATUS_ACTIVE)
                .setOwnerName(reqVO.getOwnerName())
                .setNextAction(reqVO.getNextAction())
                .setRemark(reqVO.getRemark());
        if (traceLinkMapper.insert(traceLink) != 1 || traceLink.getId() == null) {
            throw exception(PRO_EDHR_VALIDATION_TRACE_LINK_INVALID);
        }
        return BeanUtils.toBean(traceLinkMapper.selectById(traceLink.getId()), MesProEdhrValidationTraceLinkRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrValidationTraceEvaluateRespVO evaluateTrace(Long packageId) {
        MesProEdhrValidationPackageDO validationPackage = requirePackage(packageId);
        List<MesProEdhrValidationRequirementItemDO> items = itemMapper.selectListByPackageId(packageId);
        List<MesProEdhrValidationTraceLinkDO> traceLinks = traceLinkMapper.selectListByPackageId(packageId);
        List<MesProEdhrValidationRequirementItemDO> ursItems = items.stream()
                .filter(item -> ITEM_TYPE_URS.equals(item.getItemType()))
                .toList();

        Map<Long, Set<String>> targetTypesBySource = traceLinks.stream()
                .filter(link -> TRACE_STATUS_ACTIVE.equals(link.getTraceStatus()))
                .collect(Collectors.groupingBy(MesProEdhrValidationTraceLinkDO::getSourceItemId,
                        Collectors.mapping(MesProEdhrValidationTraceLinkDO::getTargetItemType,
                                Collectors.toCollection(LinkedHashSet::new))));
        List<MesProEdhrValidationTraceIssueRespVO> issues = buildTraceIssues(validationPackage, ursItems, targetTypesBySource);

        int ursCount = countType(items, ITEM_TYPE_URS);
        int frsCount = countType(items, ITEM_TYPE_FRS);
        int riskCount = countType(items, ITEM_TYPE_RISK);
        int iqCount = countType(items, ITEM_TYPE_IQ);
        int oqCount = countType(items, ITEM_TYPE_OQ);
        int pqCount = countType(items, ITEM_TYPE_PQ);
        boolean oqReady = issues.isEmpty();
        String validationStatus = oqReady ? VALIDATION_STATUS_PREPARED : VALIDATION_STATUS_BLOCKED;
        String traceStatus = oqReady ? TRACE_STATUS_READY : TRACE_STATUS_BLOCKED;
        String blockedReason = oqReady
                ? "追溯矩阵完整，可进入OQ Ready准备状态；本切片不生成验证结论或签核结果"
                : PRO_EDHR_VALIDATION_TRACE_GATE_BLOCKED.getMsg();
        String nextAction = oqReady
                ? "进入OQ脚本准备和验证执行计划复核"
                : NEXT_ACTION_EVALUATE;

        validationPackage.setValidationStatus(validationStatus)
                .setOqReady(oqReady)
                .setBlockedReason(blockedReason)
                .setTraceSummaryJson(buildTraceSummaryJson(validationPackage.getPackageCode(), validationStatus,
                        ursCount, frsCount, riskCount, iqCount, oqCount, pqCount, traceLinks.size(), issues.size()));
        if (packageMapper.updateById(validationPackage) != 1) {
            throw exception(PRO_EDHR_VALIDATION_PACKAGE_NOT_EXISTS);
        }

        return new MesProEdhrValidationTraceEvaluateRespVO()
                .setPackageId(validationPackage.getId())
                .setPackageCode(validationPackage.getPackageCode())
                .setValidationStatus(validationStatus)
                .setOqReady(oqReady)
                .setTraceStatus(traceStatus)
                .setUrsCount(ursCount)
                .setFrsCount(frsCount)
                .setRiskCount(riskCount)
                .setIqCount(iqCount)
                .setOqCount(oqCount)
                .setPqCount(pqCount)
                .setTraceLinkCount(traceLinks.size())
                .setBrokenTraceCount(issues.size())
                .setBrokenItems(issues)
                .setBlockedReason(blockedReason)
                .setSummary(oqReady
                        ? "所有URS均已追溯到FRS、风险和至少一个IQ/OQ/PQ验证项"
                        : "存在URS追溯断裂，OQ Ready保持阻塞")
                .setNextAction(nextAction);
    }

    private MesProEdhrValidationPackageDO requirePackage(Long id) {
        MesProEdhrValidationPackageDO validationPackage = id == null ? null : packageMapper.selectById(id);
        if (validationPackage == null) {
            throw exception(PRO_EDHR_VALIDATION_PACKAGE_NOT_EXISTS);
        }
        return validationPackage;
    }

    private MesProEdhrValidationRequirementItemDO requireItemInPackage(Long packageId, Long itemId) {
        MesProEdhrValidationRequirementItemDO item = itemId == null ? null : itemMapper.selectById(itemId);
        if (item == null || !packageId.equals(item.getPackageId())) {
            throw exception(PRO_EDHR_VALIDATION_ITEM_NOT_EXISTS);
        }
        return item;
    }

    private void validateItemType(String itemType) {
        if (!ITEM_TYPES.contains(itemType)) {
            throw exception(PRO_EDHR_VALIDATION_ITEM_TYPE_INVALID);
        }
    }

    private void validateTraceLink(MesProEdhrValidationRequirementItemDO source,
                                   MesProEdhrValidationRequirementItemDO target,
                                   String linkType) {
        if (source.getId().equals(target.getId()) || !ITEM_TYPE_URS.equals(source.getItemType())) {
            throw exception(PRO_EDHR_VALIDATION_TRACE_LINK_INVALID);
        }
        String expectedLinkType = resolveRequiredLinkType(target.getItemType());
        if (expectedLinkType == null || !expectedLinkType.equals(linkType)) {
            throw exception(PRO_EDHR_VALIDATION_TRACE_LINK_INVALID);
        }
    }

    private String resolveRequiredLinkType(String targetItemType) {
        if (ITEM_TYPE_FRS.equals(targetItemType)) {
            return LINK_TYPE_URS_FRS;
        }
        if (ITEM_TYPE_RISK.equals(targetItemType)) {
            return LINK_TYPE_URS_RISK;
        }
        if (VERIFICATION_ITEM_TYPES.contains(targetItemType)) {
            return LINK_TYPE_URS_VERIFICATION;
        }
        return null;
    }

    private List<MesProEdhrValidationTraceIssueRespVO> buildTraceIssues(
            MesProEdhrValidationPackageDO validationPackage,
            List<MesProEdhrValidationRequirementItemDO> ursItems,
            Map<Long, Set<String>> targetTypesBySource) {
        if (ursItems.isEmpty()) {
            return List.of(new MesProEdhrValidationTraceIssueRespVO()
                    .setPackageId(validationPackage.getId())
                    .setSourceItemCode("URS_MISSING")
                    .setSourceItemType(ITEM_TYPE_URS)
                    .setMissingItemType(ITEM_TYPE_URS)
                    .setMissingItemName("至少一条URS")
                    .setOwnerName(validationPackage.getValidationOwnerName())
                    .setSignoffRole("验证负责人")
                    .setNextAction("先登记URS条目，再评估追溯门禁")
                    .setBlockingReason("未登记URS，不能评估追溯矩阵")
                    .setSignoffImpact("阻断OQ Ready"));
        }

        return ursItems.stream()
                .flatMap(urs -> buildIssuesForUrs(validationPackage, urs, targetTypesBySource.get(urs.getId())).stream())
                .toList();
    }

    private List<MesProEdhrValidationTraceIssueRespVO> buildIssuesForUrs(
            MesProEdhrValidationPackageDO validationPackage,
            MesProEdhrValidationRequirementItemDO urs,
            Set<String> targetTypes) {
        Set<String> resolvedTargetTypes = targetTypes == null ? Set.of() : targetTypes;
        List<MissingTraceRequirement> missingRequirements = List.of(
                new MissingTraceRequirement(ITEM_TYPE_FRS, "FRS条目", !resolvedTargetTypes.contains(ITEM_TYPE_FRS),
                        "补齐URS到FRS追溯并确认FRS条目"),
                new MissingTraceRequirement(ITEM_TYPE_RISK, "风险条目", !resolvedTargetTypes.contains(ITEM_TYPE_RISK),
                        "补齐URS到风险条目追溯并完成风险评审"),
                new MissingTraceRequirement("IQ/OQ/PQ", "至少一个IQ/OQ/PQ验证项",
                        resolvedTargetTypes.stream().noneMatch(VERIFICATION_ITEM_TYPES::contains),
                        "补齐URS到IQ/OQ/PQ验证项追溯")
        );

        return missingRequirements.stream()
                .filter(MissingTraceRequirement::missing)
                .map(requirement -> new MesProEdhrValidationTraceIssueRespVO()
                        .setPackageId(validationPackage.getId())
                        .setSourceItemId(urs.getId())
                        .setSourceItemCode(urs.getItemCode())
                        .setSourceItemType(urs.getItemType())
                        .setMissingItemType(requirement.missingItemType())
                        .setMissingItemName(requirement.missingItemName())
                        .setOwnerName(urs.getOwnerName())
                        .setSignoffRole(urs.getSignoffRole())
                        .setNextAction(requirement.nextAction())
                        .setBlockingReason("URS缺少必需追溯目标")
                        .setSignoffImpact("阻断OQ Ready"))
                .toList();
    }

    private int countType(List<MesProEdhrValidationRequirementItemDO> items, String itemType) {
        return (int) items.stream().filter(item -> itemType.equals(item.getItemType())).count();
    }

    private String buildTraceSummaryJson(String packageCode, String validationStatus, int ursCount, int frsCount,
                                         int riskCount, int iqCount, int oqCount, int pqCount,
                                         int traceLinkCount, int brokenTraceCount) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("packageCode", packageCode);
        summary.put("validationStatus", validationStatus);
        summary.put("oqReady", VALIDATION_STATUS_PREPARED.equals(validationStatus));
        summary.put("ursCount", ursCount);
        summary.put("frsCount", frsCount);
        summary.put("riskCount", riskCount);
        summary.put("iqCount", iqCount);
        summary.put("oqCount", oqCount);
        summary.put("pqCount", pqCount);
        summary.put("traceLinkCount", traceLinkCount);
        summary.put("brokenTraceCount", brokenTraceCount);
        summary.put("firstSlice", "validation-package-trace-matrix");
        return JsonUtils.toJsonString(summary);
    }

    private String buildPackageCode() {
        return "EDHR-VAL-" + PACKAGE_CODE_TIME.format(LocalDateTime.now());
    }

    private record MissingTraceRequirement(String missingItemType, String missingItemName,
                                           boolean missing, String nextAction) {
    }
}
