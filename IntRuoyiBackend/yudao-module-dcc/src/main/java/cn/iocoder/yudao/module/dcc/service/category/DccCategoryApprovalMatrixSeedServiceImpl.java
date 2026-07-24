package cn.iocoder.yudao.module.dcc.service.category;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryApprovalMatrixRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryApprovalMatrixSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccApprovalPositionDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccApprovalPositionMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CATEGORY_APPROVAL_MATRIX_POSITION_INACTIVE_OR_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_NOT_EXISTS;

@Service
@Validated
public class DccCategoryApprovalMatrixSeedServiceImpl implements DccCategoryApprovalMatrixSeedService {

    private static final String SEED_RESOURCE_PATH = "seeds/dcc-category-approval-matrix.json";

    private static final Map<String, String> POSITION_NAME_ALIASES = Map.of(
            "文档管理员", "文控"
    );

    @Resource
    private DccFileCategoryMapper categoryMapper;
    @Resource
    private DccApprovalPositionMapper positionMapper;
    @Resource
    private DccCategoryApprovalMatrixAdminService approvalMatrixAdminService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccCategoryApprovalMatrixImportResult importSeededMatrix() {
        List<SeededCategoryApprovalMatrix> seedItems = loadSeedItems();
        Map<String, DccFileCategoryDO> categoryByCode = categoryMapper.selectList().stream()
                .collect(java.util.stream.Collectors.toMap(DccFileCategoryDO::getCode, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<String, DccApprovalPositionDO> positionByName = positionMapper.selectList().stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .collect(java.util.stream.Collectors.toMap(
                        item -> normalizePositionName(item.getName()),
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        int seededCount = 0;
        int skippedCount = 0;
        LocalDateTime effectiveTime = LocalDateTime.now().withNano(0);

        for (SeededCategoryApprovalMatrix item : seedItems) {
            DccFileCategoryDO category = categoryByCode.get(item.getCategoryCode());
            if (category == null) {
                throw exception(FILE_CATEGORY_NOT_EXISTS);
            }
            List<Long> signoffPositionIds = resolvePositionIds(item.getSignoffPositionNames(), positionByName);
            List<Long> approvalPositionIds = resolvePositionIds(item.getApprovalPositionNames(), positionByName);
            DccCategoryApprovalMatrixRespVO currentMatrix = approvalMatrixAdminService.getApprovalMatrix(category.getId());
            if (Objects.equals(extractStageSubjectIds(currentMatrix, "SIGNOFF"), signoffPositionIds)
                    && Objects.equals(extractStageSubjectIds(currentMatrix, "APPROVAL"), approvalPositionIds)) {
                skippedCount++;
                continue;
            }

            DccCategoryApprovalMatrixSaveReqVO reqVO = new DccCategoryApprovalMatrixSaveReqVO();
            reqVO.setEffectiveTime(effectiveTime);
            reqVO.setRemark(item.getRemark());
            reqVO.setRules(List.of(
                    buildSeedRule("SIGNOFF", signoffPositionIds, item.getSignoffPositionNames()),
                    buildSeedRule("APPROVAL", approvalPositionIds, item.getApprovalPositionNames())
            ));
            approvalMatrixAdminService.saveApprovalMatrix(category.getId(), reqVO);
            seededCount++;
        }

        return new DccCategoryApprovalMatrixImportResult(seedItems.size(), seededCount, skippedCount);
    }

    private List<SeededCategoryApprovalMatrix> loadSeedItems() {
        try {
            ClassPathResource resource = new ClassPathResource(SEED_RESOURCE_PATH);
            return JsonUtils.parseArray(resource.getContentAsString(StandardCharsets.UTF_8), SeededCategoryApprovalMatrix.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read seeded approval matrix resource: " + SEED_RESOURCE_PATH, e);
        }
    }

    private List<Long> resolvePositionIds(List<String> positionNames, Map<String, DccApprovalPositionDO> positionByName) {
        return positionNames.stream()
                .map(this::normalizePositionName)
                .map(positionByName::get)
                .map(position -> {
                    if (position == null) {
                        throw exception(CATEGORY_APPROVAL_MATRIX_POSITION_INACTIVE_OR_MISSING);
                    }
                    return position.getId();
                })
                .toList();
    }

    private String normalizePositionName(String rawName) {
        String normalized = StrUtil.blankToDefault(rawName, "").trim();
        return POSITION_NAME_ALIASES.getOrDefault(normalized, normalized);
    }

    private List<Long> extractStageSubjectIds(DccCategoryApprovalMatrixRespVO matrix, String stageType) {
        if (matrix == null || matrix.getRules() == null) {
            return List.of();
        }
        return matrix.getRules().stream()
                .filter(rule -> stageType.equals(rule.getStageType()))
                .map(DccCategoryApprovalMatrixRespVO.Rule::getSubjectId)
                .filter(Objects::nonNull)
                .toList();
    }

    private DccCategoryApprovalMatrixSaveReqVO.Rule buildSeedRule(String stageType, List<Long> subjectIds,
                                                                  List<String> subjectNames) {
        DccCategoryApprovalMatrixSaveReqVO.Rule rule = new DccCategoryApprovalMatrixSaveReqVO.Rule();
        rule.setStageType(stageType);
        rule.setActive(true);
        rule.setSubjectType("DCC_POSITION");
        rule.setSubjectId(subjectIds.isEmpty() ? null : subjectIds.get(0));
        rule.setSubjectLabel(subjectNames == null || subjectNames.isEmpty()
                ? stageType
                : String.join(" / ", subjectNames));
        rule.setSubjectName(rule.getSubjectLabel());
        rule.setMarker("▲");
        return rule;
    }

    public static class SeededCategoryApprovalMatrix {
        private String categoryCode;
        private String categoryName;
        private List<String> signoffPositionNames;
        private List<String> approvalPositionNames;
        private String remark;

        public String getCategoryCode() {
            return categoryCode;
        }

        public void setCategoryCode(String categoryCode) {
            this.categoryCode = categoryCode;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public List<String> getSignoffPositionNames() {
            return signoffPositionNames;
        }

        public void setSignoffPositionNames(List<String> signoffPositionNames) {
            this.signoffPositionNames = signoffPositionNames;
        }

        public List<String> getApprovalPositionNames() {
            return approvalPositionNames;
        }

        public void setApprovalPositionNames(List<String> approvalPositionNames) {
            this.approvalPositionNames = approvalPositionNames;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }
}
