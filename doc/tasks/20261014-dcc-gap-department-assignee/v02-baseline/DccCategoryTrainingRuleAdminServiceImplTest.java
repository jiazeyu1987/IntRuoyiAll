package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryTrainingRuleSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryTrainingRuleDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryTrainingRuleMapper;
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

@Import(DccCategoryTrainingRuleAdminServiceImpl.class)
class DccCategoryTrainingRuleAdminServiceImplTest extends BaseDbUnitTest {

    @Resource
    private DccCategoryTrainingRuleAdminServiceImpl trainingRuleAdminService;
    @Resource
    private DccFileCategoryMapper categoryMapper;
    @Resource
    private DccFileCategoryTrainingRuleMapper trainingRuleMapper;
    @MockitoBean
    private DeptApi deptApi;

    @Test
    void replaceTrainingRules_categoryMissing_throwsNotExists() {
        DccCategoryTrainingRuleSaveReqVO reqVO = new DccCategoryTrainingRuleSaveReqVO();
        reqVO.setDepartmentId(10L);
        reqVO.setActive(Boolean.TRUE);

        assertServiceException(() -> trainingRuleAdminService.replaceTrainingRules(randomLongId(), List.of(reqVO)),
                FILE_CATEGORY_NOT_EXISTS);
    }

    @Test
    void replaceTrainingRules_requiredCategoryWithoutActiveDept_throwsExplicitFailure() {
        DccFileCategoryDO category = createCategory("SOP", Boolean.FALSE, Boolean.TRUE);

        DccCategoryTrainingRuleSaveReqVO reqVO = new DccCategoryTrainingRuleSaveReqVO();
        reqVO.setDepartmentId(10L);
        reqVO.setActive(Boolean.FALSE);

        assertServiceException(() -> trainingRuleAdminService.replaceTrainingRules(category.getId(), List.of(reqVO)),
                DccCategoryTrainingRuleAdminServiceImpl.CATEGORY_TRAINING_RULE_REQUIRED);
    }

    @Test
    void replaceTrainingRules_success() {
        DccFileCategoryDO category = createCategory("WI", Boolean.FALSE, Boolean.TRUE);

        DccCategoryTrainingRuleSaveReqVO reqVO = new DccCategoryTrainingRuleSaveReqVO();
        reqVO.setDepartmentId(20L);
        reqVO.setActive(Boolean.TRUE);

        List<DccFileCategoryTrainingRuleDO> saved = trainingRuleAdminService.replaceTrainingRules(
                category.getId(), List.of(reqVO));

        assertEquals(1, saved.size());
        verify(deptApi).validateDeptList(List.of(20L));
        assertEquals(List.of(20L),
                trainingRuleMapper.selectList(DccFileCategoryTrainingRuleDO::getCategoryId, category.getId())
                        .stream()
                        .map(DccFileCategoryTrainingRuleDO::getDepartmentId)
                        .toList());
    }

    @Test
    void importTrainingRules_requiredCategoryWithoutActiveDept_allowsEmptyRulesForPackageOverride() {
        DccFileCategoryDO category = createCategory("IMPORT-EMPTY", Boolean.FALSE, Boolean.TRUE);
        trainingRuleMapper.insert(DccFileCategoryTrainingRuleDO.builder()
                .categoryId(category.getId())
                .departmentId(20L)
                .active(Boolean.TRUE)
                .build());

        List<DccFileCategoryTrainingRuleDO> saved = trainingRuleAdminService.importTrainingRules(
                category.getId(), List.of());

        assertEquals(0, saved.size());
        assertEquals(0,
                trainingRuleMapper.selectList(DccFileCategoryTrainingRuleDO::getCategoryId, category.getId()).size());
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
