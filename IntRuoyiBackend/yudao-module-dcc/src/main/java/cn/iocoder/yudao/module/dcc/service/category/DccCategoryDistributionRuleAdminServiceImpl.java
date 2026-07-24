package cn.iocoder.yudao.module.dcc.service.category;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryDistributionRuleSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDistributionRuleDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryDistributionRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.enums.DccDistributionMediumEnum;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Comparator;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DISTRIBUTION_MEDIUM_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_NOT_EXISTS;

@Service
@Validated
public class DccCategoryDistributionRuleAdminServiceImpl implements DccCategoryDistributionRuleAdminService {

    static final ErrorCode CATEGORY_DISTRIBUTION_RULE_REQUIRED =
            new ErrorCode(1_080_000_101, "当前文件类别要求配置至少一个启用的分发部门");

    @Resource
    private DccFileCategoryMapper categoryMapper;
    @Resource
    private DccFileCategoryDistributionRuleMapper distributionRuleMapper;
    @Resource
    private DeptApi deptApi;

    @Override
    public List<DccFileCategoryDistributionRuleDO> getDistributionRules(Long categoryId) {
        validateCategoryExists(categoryId);
        return distributionRuleMapper.selectList(DccFileCategoryDistributionRuleDO::getCategoryId, categoryId).stream()
                .sorted(Comparator.comparing(DccFileCategoryDistributionRuleDO::getDepartmentId))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<DccFileCategoryDistributionRuleDO> replaceDistributionRules(Long categoryId,
                                                                            List<DccCategoryDistributionRuleSaveReqVO> reqVOList) {
        DccFileCategoryDO category = validateCategoryExists(categoryId);
        validateRequiredDepartments(category.getDistributionRequired(), reqVOList);
        return doReplaceDistributionRules(categoryId, reqVOList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<DccFileCategoryDistributionRuleDO> importDistributionRules(Long categoryId,
                                                                           List<DccCategoryDistributionRuleSaveReqVO> reqVOList) {
        validateCategoryExists(categoryId);
        return doReplaceDistributionRules(categoryId, reqVOList);
    }

    private List<DccFileCategoryDistributionRuleDO> doReplaceDistributionRules(
            Long categoryId, List<DccCategoryDistributionRuleSaveReqVO> reqVOList) {
        validateDeptIds(reqVOList);
        distributionRuleMapper.deleteByCategoryIdHard(categoryId);
        List<DccFileCategoryDistributionRuleDO> rules = CollectionUtils.convertList(reqVOList, reqVO -> {
            DccFileCategoryDistributionRuleDO rule = BeanUtils.toBean(reqVO, DccFileCategoryDistributionRuleDO.class);
            rule.setCategoryId(categoryId);
            rule.setDistributionMedium(normalizeDistributionMedium(reqVO.getDistributionMedium()));
            return rule;
        });
        rules.forEach(distributionRuleMapper::insert);
        return getDistributionRules(categoryId);
    }

    private void validateRequiredDepartments(Boolean required, List<DccCategoryDistributionRuleSaveReqVO> reqVOList) {
        boolean hasActiveRule = reqVOList != null && reqVOList.stream().anyMatch(rule -> Boolean.TRUE.equals(rule.getActive()));
        if (Boolean.TRUE.equals(required) && !hasActiveRule) {
            throw exception(CATEGORY_DISTRIBUTION_RULE_REQUIRED);
        }
    }

    private void validateDeptIds(List<DccCategoryDistributionRuleSaveReqVO> reqVOList) {
        if (reqVOList == null || reqVOList.isEmpty()) {
            return;
        }
        List<Long> departmentIds = reqVOList.stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getActive()))
                .map(DccCategoryDistributionRuleSaveReqVO::getDepartmentId)
                .distinct()
                .toList();
        if (!departmentIds.isEmpty()) {
            deptApi.validateDeptList(departmentIds);
        }
    }

    private DccFileCategoryDO validateCategoryExists(Long categoryId) {
        DccFileCategoryDO category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw exception(FILE_CATEGORY_NOT_EXISTS);
        }
        return category;
    }

    private String normalizeDistributionMedium(String distributionMedium) {
        if (StrUtil.isBlank(distributionMedium)) {
            return DccDistributionMediumEnum.PUBLIC_FOLDER.getCode();
        }
        if (!DccDistributionMediumEnum.isValid(distributionMedium)) {
            throw exception(CONTROLLED_FILE_DISTRIBUTION_MEDIUM_INVALID);
        }
        return distributionMedium;
    }
}
