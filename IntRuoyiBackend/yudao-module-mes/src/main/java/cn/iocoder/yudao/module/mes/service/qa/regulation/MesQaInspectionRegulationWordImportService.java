package cn.iocoder.yudao.module.mes.service.qa.regulation;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationPublishedVersionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationSaveRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_DCC_PROJECT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_SNAPSHOT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_VERSION_CONFLICT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_VERSION_IMMUTABLE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_WORD_IMPORT_INVALID;

@Service
public class MesQaInspectionRegulationWordImportService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_RETIRED = "RETIRED";
    private static final String FINAL_NOT_APPLICABLE_REASON = "源 QA 模板未规定末检";
    private static final int MAX_BUSINESS_CODE_LENGTH = 64;

    private final DccProjectCodeMapper dccProjectCodeMapper;
    private final MesQaInspectionRegulationMapper regulationMapper;
    private final MesQaInspectionRegulationVersionMapper versionMapper;
    private final MesQaInspectionRegulationWordParser parser;
    private final MesQaInspectionRegulationService regulationService;

    public MesQaInspectionRegulationWordImportService(
            DccProjectCodeMapper dccProjectCodeMapper,
            MesQaInspectionRegulationMapper regulationMapper,
            MesQaInspectionRegulationVersionMapper versionMapper,
            MesQaInspectionRegulationWordParser parser,
            MesQaInspectionRegulationService regulationService) {
        this.dccProjectCodeMapper = dccProjectCodeMapper;
        this.regulationMapper = regulationMapper;
        this.versionMapper = versionMapper;
        this.parser = parser;
        this.regulationService = regulationService;
    }

    @Transactional(rollbackFor = Exception.class)
    public MesQaInspectionRegulationImportRespVO importWordDraft(
            MultipartFile file, Long dccProjectCodeId) {
        requireEnabledDccProject(dccProjectCodeId);
        String fileName = validateFile(file);
        MesQaInspectionRegulationWordParser.ParsedRegulation parsed =
                parser.parse(readContent(file), fileName);

        MesQaInspectionRegulationDO regulation =
                regulationMapper.selectByDccProjectCodeId(dccProjectCodeId);
        MesQaInspectionRegulationPublishedVersionRespVO baseline = null;
        String route = regulation == null ? "CREATE" : "UPGRADE";
        if (regulation != null) {
            baseline = validateVersionsAndLoadBaseline(regulation, parsed.versionNo());
        }

        BuildResult buildResult = buildSaveRequest(
                dccProjectCodeId, regulation, parsed, baseline);
        MesQaInspectionRegulationSaveRespVO saved =
                regulationService.saveDraft(buildResult.request());
        return MesQaInspectionRegulationImportRespVO.builder()
                .dccProjectCodeId(saved.getDccProjectCodeId())
                .regulationId(saved.getRegulationId())
                .draftVersionId(saved.getDraftVersionId())
                .regulationCode(parsed.regulationCode())
                .regulationName(parsed.regulationName())
                .versionNo(saved.getVersionNo())
                .effectiveDate(parsed.effectiveDate())
                .lifecycleStatus(saved.getLifecycleStatus())
                .route(route)
                .processCount(buildResult.request().getProcesses().size())
                .itemCount(parsed.items().size())
                .inheritedItemCount(buildResult.inheritedItemCount())
                .createdItemCount(parsed.items().size() - buildResult.inheritedItemCount())
                .build();
    }

    private MesQaInspectionRegulationPublishedVersionRespVO validateVersionsAndLoadBaseline(
            MesQaInspectionRegulationDO regulation, String importedVersionNo) {
        List<MesQaInspectionRegulationVersionDO> drafts =
                versionMapper.selectListDraftByRegulationId(regulation.getId());
        if (drafts.size() > 1) {
            throw exception(QA_INSPECTION_REGULATION_VERSION_CONFLICT,
                    "存在多个未发布草稿：" + drafts.stream()
                            .map(MesQaInspectionRegulationVersionDO::getVersionNo).toList());
        }
        MesQaInspectionRegulationVersionDO draft = drafts.isEmpty() ? null : drafts.get(0);
        if (draft != null && !Objects.equals(draft.getVersionNo(), importedVersionNo)) {
            throw exception(QA_INSPECTION_REGULATION_VERSION_CONFLICT,
                    "已有未发布草稿版本 " + draft.getVersionNo()
                            + "，请先处理后再导入版本 " + importedVersionNo);
        }

        MesQaInspectionRegulationVersionDO sameVersion =
                versionMapper.selectByRegulationIdAndVersionNo(regulation.getId(), importedVersionNo);
        if (sameVersion != null && Set.of(STATUS_PUBLISHED, STATUS_RETIRED)
                .contains(sameVersion.getLifecycleStatus())) {
            throw exception(QA_INSPECTION_REGULATION_VERSION_IMMUTABLE,
                    importedVersionNo + "（版本 ID " + sameVersion.getId() + "）");
        }
        if (sameVersion != null && !Objects.equals(STATUS_DRAFT, sameVersion.getLifecycleStatus())) {
            throw exception(QA_INSPECTION_REGULATION_VERSION_CONFLICT,
                    importedVersionNo + "（状态 " + sameVersion.getLifecycleStatus() + "）");
        }

        MesQaInspectionRegulationPublishedVersionRespVO baseline;
        if (draft != null) {
            baseline = regulationService.getCurrent(regulation.getDccProjectCodeId());
        } else if (regulation.getCurrentVersionId() != null) {
            baseline = regulationService.getPublishedVersion(
                    regulation.getDccProjectCodeId(), regulation.getCurrentVersionId());
        } else {
            throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, regulation.getId());
        }
        if (baseline == null || !Objects.equals(baseline.getRegulationId(), regulation.getId())) {
            throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, regulation.getId());
        }
        return baseline;
    }

    private BuildResult buildSaveRequest(
            Long dccProjectCodeId,
            MesQaInspectionRegulationDO regulation,
            MesQaInspectionRegulationWordParser.ParsedRegulation parsed,
            MesQaInspectionRegulationPublishedVersionRespVO baseline) {
        BaselineIndex baselineIndex = createBaselineIndex(baseline);
        boolean finalInspectionApplicable = baseline != null
                && Boolean.TRUE.equals(baseline.getFinalInspectionApplicable());
        String finalNotApplicableReason = finalInspectionApplicable
                ? null
                : baseline == null
                        ? FINAL_NOT_APPLICABLE_REASON
                        : requireText(baseline.getFinalInspectionNotApplicableReason(), "既有末检不适用依据");

        Map<String, ProcessGroup> processGroups = new LinkedHashMap<>();
        int globalItemIndex = 0;
        int inheritedItemCount = 0;
        Set<String> itemCodes = new LinkedHashSet<>();
        for (MesQaInspectionRegulationWordParser.ParsedItem parsedItem : parsed.items()) {
            globalItemIndex++;
            String processKey = normalizedName(parsedItem.processName());
            ProcessGroup processGroup = processGroups.computeIfAbsent(processKey,
                    ignored -> new ProcessGroup(parsedItem.processName(), new ArrayList<>()));
            ExistingItem existing = baselineIndex.itemByName().get(
                    itemKey(parsedItem.processName(), parsedItem.itemName()));
            String itemCode = existing == null
                    ? generatedCode(parsed.regulationCode(), "I", globalItemIndex)
                    : existing.item().getItemCode();
            if (!itemCodes.add(itemCode)) {
                throw exception(QA_INSPECTION_REGULATION_WORD_IMPORT_INVALID,
                        "检验项目编码重复：" + itemCode);
            }
            if (existing != null) {
                inheritedItemCount++;
            }
            processGroup.items().add(buildItem(parsedItem, existing, itemCode,
                    processGroup.items().size() + 1, finalInspectionApplicable, parsed.fileName()));
        }

        List<MesQaInspectionRegulationSaveReqVO.InspectionProcess> processes = new ArrayList<>();
        Set<String> processCodes = new LinkedHashSet<>();
        int processIndex = 0;
        for (Map.Entry<String, ProcessGroup> entry : processGroups.entrySet()) {
            processIndex++;
            String inheritedProcessCode = baselineIndex.processCodeByName().get(entry.getKey());
            String processCode = inheritedProcessCode == null
                    ? generatedCode(parsed.regulationCode(), "P", processIndex)
                    : inheritedProcessCode;
            if (!processCodes.add(processCode)) {
                throw exception(QA_INSPECTION_REGULATION_WORD_IMPORT_INVALID,
                        "QA 工序编码重复：" + processCode);
            }
            MesQaInspectionRegulationSaveReqVO.InspectionProcess process =
                    new MesQaInspectionRegulationSaveReqVO.InspectionProcess();
            process.setProcessCode(processCode);
            process.setProcessName(entry.getValue().processName());
            process.setSort(processIndex);
            process.setItems(entry.getValue().items());
            processes.add(process);
        }

        MesQaInspectionRegulationSaveReqVO request = new MesQaInspectionRegulationSaveReqVO();
        request.setRegulationId(regulation == null ? null : regulation.getId());
        request.setDccProjectCodeId(dccProjectCodeId);
        request.setRegulationCode(parsed.regulationCode());
        request.setRegulationName(parsed.regulationName());
        request.setVersionNo(parsed.versionNo());
        request.setEffectiveDate(parsed.effectiveDate());
        request.setFinalInspectionApplicable(finalInspectionApplicable);
        request.setFinalInspectionNotApplicableReason(finalNotApplicableReason);
        request.setInspectionTypeRules(baseline == null
                ? defaultInspectionTypeRules()
                : copyInspectionTypeRules(baseline.getInspectionTypeRules()));
        request.setProcesses(processes);
        return new BuildResult(request, inheritedItemCount);
    }

    private MesQaInspectionRegulationSaveReqVO.InspectionItem buildItem(
            MesQaInspectionRegulationWordParser.ParsedItem parsed,
            ExistingItem existing,
            String itemCode,
            int itemSort,
            boolean finalInspectionApplicable,
            String fileName) {
        MesQaInspectionRegulationPublishedVersionRespVO.InspectionItem inherited =
                existing == null ? null : existing.item();
        MesQaInspectionRegulationSaveReqVO.InspectionItem item =
                new MesQaInspectionRegulationSaveReqVO.InspectionItem();
        item.setItemSort(itemSort);
        item.setItemCode(itemCode);
        item.setItemName(parsed.itemName());
        item.setInspectionMethod(parsed.inspectionMethod());
        item.setInspectionTool(parsed.inspectionTool());
        item.setSamplingPlanText(parsed.samplingPlanText());
        item.setStandardText(parsed.standardText());
        item.setStandardLowerLimit(inherited == null ? null : inherited.getStandardLowerLimit());
        item.setStandardUpperLimit(inherited == null ? null : inherited.getStandardUpperLimit());
        item.setStandardUnit(inherited == null ? null : inherited.getStandardUnit());
        item.setStandardPrecision(inherited == null ? null : inherited.getStandardPrecision());
        item.setEquipmentRequired(inherited != null && Boolean.TRUE.equals(inherited.getEquipmentRequired()));
        item.setEquipmentOptions(inherited == null
                ? List.of()
                : copyEquipmentOptions(inherited.getEquipmentOptions()));
        item.setResultType(inherited == null ? "BOOLEAN" : inherited.getResultType());
        List<String> applicableTypes = new ArrayList<>();
        if (parsed.firstInspectionQuantity() != null) {
            applicableTypes.add("FIRST");
        }
        applicableTypes.add("PATROL");
        if (finalInspectionApplicable) {
            applicableTypes.add("FINAL");
        }
        item.setApplicableInspectionTypes(applicableTypes);
        item.setFirstInspectionQuantity(parsed.firstInspectionQuantity());
        item.setPatrolInspectionRatio(parsed.patrolInspectionRatio());
        item.setCritical(inherited != null && Boolean.TRUE.equals(inherited.getCritical()));
        item.setFailureRule(inherited == null ? null : inherited.getFailureRule());
        item.setSourceNote("导入自 QA 模板：" + fileName);
        item.setSourceOriginalItem(parsed.processName() + " / " + parsed.itemName());
        item.setSourceOriginalExcerpt(parsed.standardText());
        item.setSourceOriginalMethod(parsed.inspectionMethod());
        return item;
    }

    private static BaselineIndex createBaselineIndex(
            MesQaInspectionRegulationPublishedVersionRespVO baseline) {
        if (baseline == null) {
            return new BaselineIndex(Map.of(), Map.of());
        }
        if (CollUtil.isEmpty(baseline.getInspectionTypeRules()) || CollUtil.isEmpty(baseline.getProcesses())) {
            throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, baseline.getPublishedVersionId());
        }
        Map<String, String> processCodeByName = new LinkedHashMap<>();
        Map<String, ExistingItem> itemByName = new LinkedHashMap<>();
        for (MesQaInspectionRegulationPublishedVersionRespVO.InspectionProcess process
                : baseline.getProcesses()) {
            String processKey = normalizedName(process.getProcessName());
            String previousProcessCode = processCodeByName.putIfAbsent(processKey, process.getProcessCode());
            if (previousProcessCode != null && !Objects.equals(previousProcessCode, process.getProcessCode())) {
                throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID,
                        "同名 QA 工序存在多个编码：" + process.getProcessName());
            }
            for (MesQaInspectionRegulationPublishedVersionRespVO.InspectionItem item
                    : CollUtil.emptyIfNull(process.getItems())) {
                String key = itemKey(process.getProcessName(), item.getItemName());
                ExistingItem previous = itemByName.putIfAbsent(key, new ExistingItem(process, item));
                if (previous != null) {
                    throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID,
                            "同名检验项目不唯一：" + process.getProcessName() + " / " + item.getItemName());
                }
            }
        }
        return new BaselineIndex(processCodeByName, itemByName);
    }

    private static List<MesQaInspectionRegulationSaveReqVO.EquipmentOption> copyEquipmentOptions(
            List<MesQaInspectionRegulationPublishedVersionRespVO.EquipmentOption> source) {
        return CollUtil.emptyIfNull(source).stream().map(option -> {
            MesQaInspectionRegulationSaveReqVO.EquipmentOption target =
                    new MesQaInspectionRegulationSaveReqVO.EquipmentOption();
            target.setEquipmentId(option.getEquipmentId());
            target.setEquipmentCode(option.getEquipmentCode());
            target.setEquipmentName(option.getEquipmentName());
            target.setEquipmentNumber(option.getEquipmentNumber());
            target.setDefaultFlag(option.getDefaultFlag());
            target.setSort(option.getSort());
            return target;
        }).toList();
    }

    private static List<MesQaInspectionRegulationSaveReqVO.InspectionTypeRule> copyInspectionTypeRules(
            List<MesQaInspectionRegulationPublishedVersionRespVO.InspectionTypeRule> source) {
        if (CollUtil.isEmpty(source)) {
            throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, "inspectionTypeRules");
        }
        return source.stream().map(rule -> {
            MesQaInspectionRegulationSaveReqVO.InspectionTypeRule target =
                    new MesQaInspectionRegulationSaveReqVO.InspectionTypeRule();
            target.setKey(rule.getKey());
            target.setInspectionType(rule.getInspectionType());
            target.setLabel(rule.getLabel());
            target.setRoundLabel(rule.getRoundLabel());
            target.setRequired(rule.getRequired());
            target.setFixedQuantity(rule.getFixedQuantity());
            target.setNotApplicableReason(rule.getNotApplicableReason());
            target.setTaskRule(rule.getTaskRule());
            target.setReleaseGate(rule.getReleaseGate());
            return target;
        }).toList();
    }

    private static List<MesQaInspectionRegulationSaveReqVO.InspectionTypeRule>
    defaultInspectionTypeRules() {
        return List.of(
                rule("FIRST", "FIRST", "首检", "每个适用订单开始前", true,
                        "按发布规程固定数量生成首检任务", "缺少适用检验项目时不能发布", null),
                rule("PATROL_AM", "PATROL", "上午巡检", "上午班次独立轮次", true,
                        "按订单数量与项目抽样比例生成任务", "缺少适用检验项目时不能发布", null),
                rule("PATROL_PM", "PATROL", "下午巡检", "下午班次独立轮次", true,
                        "按订单数量与项目抽样比例生成任务", "缺少适用检验项目时不能发布", null),
                rule("FINAL", "FINAL", "末检", "订单结束前", false,
                        "启用末检时生成末检任务", "末检适用性必须明确", FINAL_NOT_APPLICABLE_REASON));
    }

    private static MesQaInspectionRegulationSaveReqVO.InspectionTypeRule rule(
            String key, String type, String label, String roundLabel, boolean required,
            String taskRule, String releaseGate, String notApplicableReason) {
        MesQaInspectionRegulationSaveReqVO.InspectionTypeRule rule =
                new MesQaInspectionRegulationSaveReqVO.InspectionTypeRule();
        rule.setKey(key);
        rule.setInspectionType(type);
        rule.setLabel(label);
        rule.setRoundLabel(roundLabel);
        rule.setRequired(required);
        rule.setTaskRule(taskRule);
        rule.setReleaseGate(releaseGate);
        rule.setNotApplicableReason(notApplicableReason);
        return rule;
    }

    private void requireEnabledDccProject(Long dccProjectCodeId) {
        DccProjectCodeDO project = dccProjectCodeId == null
                ? null : dccProjectCodeMapper.selectById(dccProjectCodeId);
        if (project == null || !Objects.equals(project.getStatus(), DccProjectCodeStatusConstants.ENABLE)) {
            throw exception(QA_INSPECTION_REGULATION_DCC_PROJECT_INVALID, dccProjectCodeId);
        }
    }

    private static String validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw exception(QA_INSPECTION_REGULATION_WORD_IMPORT_INVALID, "上传文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        String normalizedPath = originalName == null ? "" : originalName.replace('\\', '/');
        String fileName = normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1).trim();
        if (fileName.isEmpty() || !fileName.toLowerCase().endsWith(".docx")) {
            throw exception(QA_INSPECTION_REGULATION_WORD_IMPORT_INVALID, "仅支持 .docx 文件");
        }
        return fileName;
    }

    private static byte[] readContent(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw exception(QA_INSPECTION_REGULATION_WORD_IMPORT_INVALID,
                    "读取上传文件失败：" + MesQaInspectionRegulationWordParser.normalizeText(ex.getMessage()));
        }
    }

    private static String generatedCode(String regulationCode, String type, int index) {
        String code = regulationCode + "-" + type + String.format("%03d", index);
        if (code.length() > MAX_BUSINESS_CODE_LENGTH) {
            throw exception(QA_INSPECTION_REGULATION_WORD_IMPORT_INVALID,
                    "生成的业务编码超过 " + MAX_BUSINESS_CODE_LENGTH + " 个字符：" + code);
        }
        return code;
    }

    private static String itemKey(String processName, String itemName) {
        return normalizedName(processName) + "\u0000" + normalizedName(itemName);
    }

    private static String normalizedName(String value) {
        return MesQaInspectionRegulationWordParser.normalizeText(value);
    }

    private static String requireText(String value, String label) {
        String normalized = MesQaInspectionRegulationWordParser.normalizeText(value);
        if (normalized.isEmpty()) {
            throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, label);
        }
        return normalized;
    }

    private record ProcessGroup(String processName,
                                List<MesQaInspectionRegulationSaveReqVO.InspectionItem> items) {
    }

    private record ExistingItem(
            MesQaInspectionRegulationPublishedVersionRespVO.InspectionProcess process,
            MesQaInspectionRegulationPublishedVersionRespVO.InspectionItem item) {
    }

    private record BaselineIndex(Map<String, String> processCodeByName,
                                 Map<String, ExistingItem> itemByName) {
    }

    private record BuildResult(MesQaInspectionRegulationSaveReqVO request,
                               int inheritedItemCount) {
    }
}
