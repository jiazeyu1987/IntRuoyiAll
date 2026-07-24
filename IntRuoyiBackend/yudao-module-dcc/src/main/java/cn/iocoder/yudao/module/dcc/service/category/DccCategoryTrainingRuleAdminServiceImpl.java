package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryTrainingRuleSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryTrainingRuleDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryTrainingRuleMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Comparator;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_NOT_EXISTS;

@Service
@Validated
public class DccCategoryTrainingRuleAdminServiceImpl implements DccCategoryTrainingRuleAdminService {

    static final ErrorCode CATEGORY_TRAINING_RULE_REQUIRED =
            new ErrorCode(1_080_000_102, "当前文件类别要求配置至少一个启用的培训部门");

    @Resource
    private DccFileCategoryMapper categoryMapper;
    @Resource
    private DccFileCategoryTrainingRuleMapper trainingRuleMapper;
    @Resource
    private DeptApi deptApi;

    @Override
    public List<DccFileCategoryTrainingRuleDO> getTrainingRules(Long categoryId) {
        validateCategoryExists(categoryId);
        return trainingRuleMapper.selectList(DccFileCategoryTrainingRuleDO::getCategoryId, categoryId).stream()
                .sorted(Comparator.comparing(DccFileCategoryTrainingRuleDO::getDepartmentId))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<DccFileCategoryTrainingRuleDO> replaceTrainingRules(Long categoryId,
                                                                    List<DccCategoryTrainingRuleSaveReqVO> reqVOList) {
        DccFileCategoryDO category = validateCategoryExists(categoryId);
        validateRequiredDepartments(category.getTrainingRequired(), reqVOList);
        return doReplaceTrainingRules(categoryId, reqVOList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<DccFileCategoryTrainingRuleDO> importTrainingRules(Long categoryId,
                                                                   List<DccCategoryTrainingRuleSaveReqVO> reqVOList) {
        validateCategoryExists(categoryId);
        return doReplaceTrainingRules(categoryId, reqVOList);
    }

    private List<DccFileCategoryTrainingRuleDO> doReplaceTrainingRules(
            Long categoryId, List<DccCategoryTrainingRuleSaveReqVO> reqVOList) {
        validateDeptIds(reqVOList);
        trainingRuleMapper.delete(DccFileCategoryTrainingRuleDO::getCategoryId, categoryId);
        List<DccFileCategoryTrainingRuleDO> rules = CollectionUtils.convertList(reqVOList, reqVO -> {
            DccFileCategoryTrainingRuleDO rule = BeanUtils.toBean(reqVO, DccFileCategoryTrainingRuleDO.class);
            rule.setCategoryId(categoryId);
            return rule;
        });
        rules.forEach(trainingRuleMapper::insert);
        return getTrainingRules(categoryId);
    }

    private void validateRequiredDepartments(Boolean required, List<DccCategoryTrainingRuleSaveReqVO> reqVOList) {
        boolean hasActiveRule = reqVOList != null && reqVOList.stream().anyMatch(rule -> Boolean.TRUE.equals(rule.getActive()));
        if (Boolean.TRUE.equals(required) && !hasActiveRule) {
            throw exception(CATEGORY_TRAINING_RULE_REQUIRED);
        }
    }

    private void validateDeptIds(List<DccCategoryTrainingRuleSaveReqVO> reqVOList) {
        if (reqVOList == null || reqVOList.isEmpty()) {
            return;
        }
        List<Long> departmentIds = reqVOList.stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getActive()))
                .map(DccCategoryTrainingRuleSaveReqVO::getDepartmentId)
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
}
