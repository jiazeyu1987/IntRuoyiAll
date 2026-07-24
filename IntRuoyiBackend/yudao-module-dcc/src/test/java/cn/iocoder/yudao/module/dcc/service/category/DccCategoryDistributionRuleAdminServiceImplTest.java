package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryDistributionRuleSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDistributionRuleDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryDistributionRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.enums.DccDistributionMediumEnum;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@Import(DccCategoryDistributionRuleAdminServiceImpl.class)
class DccCategoryDistributionRuleAdminServiceImplTest extends BaseDbUnitTest {

    @Resource
    private DccCategoryDistributionRuleAdminServiceImpl distributionRuleAdminService;
    @Resource
    private DccFileCategoryMapper categoryMapper;
    @Resource
    private DccFileCategoryDistributionRuleMapper distributionRuleMapper;
    @MockitoBean
    private DeptApi deptApi;

    @Test
    void replaceDistributionRules_categoryMissing_throwsNotExists() {
        DccCategoryDistributionRuleSaveReqVO reqVO = new DccCategoryDistributionRuleSaveReqVO();
        reqVO.setDepartmentId(10L);
        reqVO.setActive(Boolean.TRUE);

        assertServiceException(() -> distributionRuleAdminService.replaceDistributionRules(randomLongId(), List.of(reqVO)),
                FILE_CATEGORY_NOT_EXISTS);
    }

    @Test
    void replaceDistributionRules_requiredCategoryWithoutActiveDept_throwsExplicitFailure() {
        DccFileCategoryDO category = createCategory("SOP", Boolean.TRUE, Boolean.FALSE);

        DccCategoryDistributionRuleSaveReqVO reqVO = new DccCategoryDistributionRuleSaveReqVO();
        reqVO.setDepartmentId(10L);
        reqVO.setActive(Boolean.FALSE);

        assertServiceException(() -> distributionRuleAdminService.replaceDistributionRules(category.getId(), List.of(reqVO)),
                DccCategoryDistributionRuleAdminServiceImpl.CATEGORY_DISTRIBUTION_RULE_REQUIRED);
    }

    @Test
    void replaceDistributionRules_success() {
        DccFileCategoryDO category = createCategory("WI", Boolean.TRUE, Boolean.FALSE);

        DccCategoryDistributionRuleSaveReqVO reqVO = new DccCategoryDistributionRuleSaveReqVO();
        reqVO.setDepartmentId(10L);
        reqVO.setDistributionMedium(DccDistributionMediumEnum.PAPER.getCode());
        reqVO.setActive(Boolean.TRUE);

        List<DccFileCategoryDistributionRuleDO> saved = distributionRuleAdminService.replaceDistributionRules(
                category.getId(), List.of(reqVO));

        assertEquals(1, saved.size());
        verify(deptApi).validateDeptList(List.of(10L));
        assertEquals(List.of(10L),
                distributionRuleMapper.selectList(DccFileCategoryDistributionRuleDO::getCategoryId, category.getId())
                        .stream()
                        .map(DccFileCategoryDistributionRuleDO::getDepartmentId)
                        .toList());
        assertEquals(List.of(DccDistributionMediumEnum.PAPER.getCode()),
                distributionRuleMapper.selectList(DccFileCategoryDistributionRuleDO::getCategoryId, category.getId())
                        .stream()
                        .map(DccFileCategoryDistributionRuleDO::getDistributionMedium)
                        .toList());
    }

    @Test
    void replaceDistributionRules_sameDepartmentTwice_shouldReplaceInsteadOfDuplicateKeyFailure() {
        DccFileCategoryDO category = createCategory("SAME-DEPT", Boolean.TRUE, Boolean.FALSE);

        DccCategoryDistributionRuleSaveReqVO first = new DccCategoryDistributionRuleSaveReqVO();
        first.setDepartmentId(10L);
        first.setDistributionMedium(DccDistributionMediumEnum.PUBLIC_FOLDER.getCode());
        first.setActive(Boolean.TRUE);
        distributionRuleAdminService.replaceDistributionRules(category.getId(), List.of(first));

        DccCategoryDistributionRuleSaveReqVO second = new DccCategoryDistributionRuleSaveReqVO();
        second.setDepartmentId(10L);
        second.setDistributionMedium(DccDistributionMediumEnum.PAPER.getCode());
        second.setActive(Boolean.TRUE);

        List<DccFileCategoryDistributionRuleDO> saved = distributionRuleAdminService.replaceDistributionRules(
                category.getId(), List.of(second));

        assertEquals(1, saved.size());
        assertEquals(DccDistributionMediumEnum.PAPER.getCode(), saved.get(0).getDistributionMedium());
        assertEquals(1,
                distributionRuleMapper.selectList(DccFileCategoryDistributionRuleDO::getCategoryId, category.getId()).size());
    }

    @Test
    void importDistributionRules_requiredCategoryWithoutActiveDept_allowsEmptyRulesForPackageOverride() {
        DccFileCategoryDO category = createCategory("IMPORT-EMPTY", Boolean.TRUE, Boolean.FALSE);
        distributionRuleMapper.insert(DccFileCategoryDistributionRuleDO.builder()
                .categoryId(category.getId())
                .departmentId(10L)
                .distributionMedium(DccDistributionMediumEnum.PUBLIC_FOLDER.getCode())
                .active(Boolean.TRUE)
                .build());

        List<DccFileCategoryDistributionRuleDO> saved = distributionRuleAdminService.importDistributionRules(
                category.getId(), List.of());

        assertEquals(0, saved.size());
        assertEquals(0,
                distributionRuleMapper.selectList(DccFileCategoryDistributionRuleDO::getCategoryId, category.getId()).size());
    }

    private DccFileCategoryDO createCategory(String code, Boolean distributionRequired, Boolean trainingRequired) {
        DccFileCategoryDO category = DccFileCategoryDO.builder()
                .id(randomLongId())
                .code(code)
                .name(code + "-NAME")
                .parentId(null)
                .active(Boolean.TRUE)
                .sort(1)
                .source("LOCAL")
                .remark(code)
                .description(code + "-DESC")
                .lifecycleStage("PLAN")
                .distributionRequired(distributionRequired)
                .trainingRequired(trainingRequired)
                .build();
        categoryMapper.insert(category);
        return category;
    }
}
