package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryPermissionRuleSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryPermissionRuleDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryPermissionRuleMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CATEGORY_PERMISSION_RULE_MANUAL_REVIEW_APPROVE_FORBIDDEN;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(DccCategoryPermissionAdminServiceImpl.class)
class DccCategoryPermissionAdminServiceImplTest extends BaseDbUnitTest {

    @Resource
    private DccCategoryPermissionAdminServiceImpl permissionAdminService;
    @Resource
    private DccFileCategoryMapper categoryMapper;
    @Resource
    private DccFileCategoryPermissionRuleMapper permissionRuleMapper;

    @Test
    void replacePermissionRules_categoryMissing_throwsNotExists() {
        DccCategoryPermissionRuleSaveReqVO reqVO = new DccCategoryPermissionRuleSaveReqVO();
        reqVO.setActionType("VIEW");
        reqVO.setSubjectType("USER");
        reqVO.setSubjectId(100L);
        reqVO.setActive(Boolean.TRUE);

        assertServiceException(() -> permissionAdminService.replacePermissionRules(randomLongId(), List.of(reqVO)),
                FILE_CATEGORY_NOT_EXISTS);
    }

    @Test
    void replacePermissionRules_success() {
        DccFileCategoryDO category = createCategory("SOP", Boolean.FALSE, Boolean.FALSE);

        DccCategoryPermissionRuleSaveReqVO viewRule = new DccCategoryPermissionRuleSaveReqVO();
        viewRule.setActionType("VIEW");
        viewRule.setSubjectType("USER");
        viewRule.setSubjectId(100L);
        viewRule.setActive(Boolean.TRUE);
        viewRule.setRemark("viewer");
        DccCategoryPermissionRuleSaveReqVO uploadRule = new DccCategoryPermissionRuleSaveReqVO();
        uploadRule.setActionType("UPLOAD");
        uploadRule.setSubjectType("DEPT");
        uploadRule.setSubjectId(200L);
        uploadRule.setActive(Boolean.TRUE);
        uploadRule.setRemark("uploader");

        List<DccFileCategoryPermissionRuleDO> saved = permissionAdminService.replacePermissionRules(category.getId(),
                List.of(viewRule, uploadRule));

        assertEquals(2, saved.size());
        List<DccFileCategoryPermissionRuleDO> dbRules = permissionRuleMapper.selectList(
                DccFileCategoryPermissionRuleDO::getCategoryId, category.getId());
        assertEquals(2, dbRules.size());
        assertEquals(List.of("UPLOAD", "VIEW"),
                permissionAdminService.getPermissionRules(category.getId()).stream()
                        .map(DccFileCategoryPermissionRuleDO::getActionType)
                        .sorted()
                        .toList());
    }

    @Test
    void replacePermissionRules_sameBusinessKeyRebuildsWithoutUniqueKeyConflict() {
        DccFileCategoryDO category = createCategory("SOP-REBUILD", Boolean.FALSE, Boolean.FALSE);
        permissionRuleMapper.insert(DccFileCategoryPermissionRuleDO.builder()
                .categoryId(category.getId())
                .actionType("VIEW")
                .subjectType("DEPT")
                .subjectId(200L)
                .scopeType("GLOBAL")
                .active(Boolean.TRUE)
                .remark("old")
                .build());

        DccCategoryPermissionRuleSaveReqVO rebuiltRule = new DccCategoryPermissionRuleSaveReqVO();
        rebuiltRule.setActionType("VIEW");
        rebuiltRule.setSubjectType("DEPT");
        rebuiltRule.setSubjectId(200L);
        rebuiltRule.setScopeType("PRODUCT_GROUP");
        rebuiltRule.setActive(Boolean.TRUE);
        rebuiltRule.setRemark("rebuilt");

        List<DccFileCategoryPermissionRuleDO> saved = permissionAdminService.replacePermissionRules(category.getId(),
                List.of(rebuiltRule));

        assertEquals(1, saved.size());
        List<DccFileCategoryPermissionRuleDO> dbRules = permissionRuleMapper.selectList(
                DccFileCategoryPermissionRuleDO::getCategoryId, category.getId());
        assertEquals(1, dbRules.size());
        assertEquals("PRODUCT_GROUP", dbRules.get(0).getScopeType());
        assertEquals("rebuilt", dbRules.get(0).getRemark());
    }

    @Test
    void replacePermissionRules_preservesMatrixManagedRules() {
        DccFileCategoryDO category = createCategory("SOP-MATRIX-PRESERVE", Boolean.FALSE, Boolean.FALSE);
        permissionRuleMapper.insert(DccFileCategoryPermissionRuleDO.builder()
                .categoryId(category.getId())
                .actionType("REVIEW")
                .subjectType("USER")
                .subjectId(300L)
                .scopeType("GLOBAL")
                .active(Boolean.TRUE)
                .remark("matrix-review")
                .build());
        permissionRuleMapper.insert(DccFileCategoryPermissionRuleDO.builder()
                .categoryId(category.getId())
                .actionType("APPROVE")
                .subjectType("USER")
                .subjectId(301L)
                .scopeType("GLOBAL")
                .active(Boolean.TRUE)
                .remark("matrix-approve")
                .build());

        DccCategoryPermissionRuleSaveReqVO uploadRule = new DccCategoryPermissionRuleSaveReqVO();
        uploadRule.setActionType("UPLOAD");
        uploadRule.setSubjectType("USER");
        uploadRule.setSubjectId(100L);
        uploadRule.setActive(Boolean.TRUE);
        uploadRule.setRemark("uploader");

        permissionAdminService.replacePermissionRules(category.getId(), List.of(uploadRule));

        assertEquals(List.of("APPROVE", "REVIEW", "UPLOAD"),
                permissionRuleMapper.selectList(DccFileCategoryPermissionRuleDO::getCategoryId, category.getId())
                        .stream()
                        .map(DccFileCategoryPermissionRuleDO::getActionType)
                        .sorted()
                        .toList());
    }

    @Test
    void replacePermissionRules_reviewOrApproveRejected() {
        DccFileCategoryDO category = createCategory("SOP", Boolean.FALSE, Boolean.FALSE);
        DccCategoryPermissionRuleSaveReqVO reqVO = new DccCategoryPermissionRuleSaveReqVO();
        reqVO.setActionType("REVIEW");
        reqVO.setSubjectType("USER");
        reqVO.setSubjectId(100L);
        reqVO.setActive(Boolean.TRUE);

        assertServiceException(() -> permissionAdminService.replacePermissionRules(category.getId(), List.of(reqVO)),
                CATEGORY_PERMISSION_RULE_MANUAL_REVIEW_APPROVE_FORBIDDEN);
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
