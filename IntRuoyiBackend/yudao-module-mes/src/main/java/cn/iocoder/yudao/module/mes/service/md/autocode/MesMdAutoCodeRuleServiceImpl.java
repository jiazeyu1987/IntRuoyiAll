package cn.iocoder.yudao.module.mes.service.md.autocode;

import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.md.autocode.vo.rule.MesMdAutoCodeRulePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.autocode.vo.rule.MesMdAutoCodeRuleSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.autocode.MesMdAutoCodeRuleDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.autocode.MesMdAutoCodePartMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.autocode.MesMdAutoCodeRuleMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;

/**
 * MES 编码规则 Service 实现类
 *
 * @author 瑛泰源码
 */
@Service
@Validated
public class MesMdAutoCodeRuleServiceImpl implements MesMdAutoCodeRuleService {

    @Resource
    private MesMdAutoCodeRuleMapper ruleMapper;
    @Resource
    private MesMdAutoCodePartMapper partMapper;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService projectAccess;

    @Override
    public Long createAutoCodeRule(MesMdAutoCodeRuleSaveReqVO createReqVO) {
        // 校验规则编码的唯一性
        validateRuleScope(createReqVO.getProjectId(), createReqVO.getCode());
        validateRuleCodeUnique(null, createReqVO.getCode(), createReqVO.getProjectId());

        // 插入
        MesMdAutoCodeRuleDO rule = BeanUtils.toBean(createReqVO, MesMdAutoCodeRuleDO.class);
        ruleMapper.insert(rule);
        return rule.getId();
    }

    @Override
    public void updateAutoCodeRule(MesMdAutoCodeRuleSaveReqVO updateReqVO) {
        // 校验存在
        validateAutoCodeRuleExists(updateReqVO.getId());
        // 校验规则编码的唯一性
        var previous = ruleMapper.selectById(updateReqVO.getId());
        if (!java.util.Objects.equals(previous.getProjectId(), updateReqVO.getProjectId())) throw new IllegalArgumentException("不能修改规则所属项目");
        if (!java.util.Objects.equals(previous.getCode(), updateReqVO.getCode())) throw new IllegalArgumentException("不能修改规则编码");
        assertProjectAccess(previous, true);
        validateRuleCodeUnique(updateReqVO.getId(), updateReqVO.getCode(), updateReqVO.getProjectId());

        // 更新
        MesMdAutoCodeRuleDO updateObj = BeanUtils.toBean(updateReqVO, MesMdAutoCodeRuleDO.class);
        ruleMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAutoCodeRule(Long id) {
        // 校验存在
        validateAutoCodeRuleExists(id);

        // 删除规则
        assertProjectAccess(ruleMapper.selectById(id), true);
        ruleMapper.deleteById(id);
        // 级联删除规则组成
        partMapper.deleteByRuleId(id);
    }

    @Override
    public MesMdAutoCodeRuleDO getAutoCodeRule(Long id) {
        var rule = ruleMapper.selectById(id);
        assertProjectAccess(rule, false);
        return rule;
    }

    @Override
    public PageResult<MesMdAutoCodeRuleDO> getAutoCodeRulePage(MesMdAutoCodeRulePageReqVO pageReqVO) {
        if (pageReqVO.getProjectId() != null) projectAccess.assertProjectViewerOrAbove(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId(), pageReqVO.getProjectId());
        return ruleMapper.selectPage(pageReqVO, projectAccess.listReadableProjectIds(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId()));
    }

    @Override
    public List<MesMdAutoCodeRuleDO> getAutoCodeRuleListByStatus(Integer status) {
        var readable = projectAccess.listReadableProjectIds(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId());
        return ruleMapper.selectListByStatus(status).stream().filter(r -> r.getProjectId() == null || readable.contains(r.getProjectId())).toList();
    }

    @Override
    public MesMdAutoCodeRuleDO getAutoCodeRuleByCode(String code) {
        return ruleMapper.selectByCode(code);
    }

    @Override
    public void validateAutoCodeRuleExists(Long id) {
        if (ruleMapper.selectById(id) == null) {
            throw exception(AUTO_CODE_RULE_NOT_EXISTS);
        }
    }

    // ==================== 校验方法 ====================

    private void assertProjectAccess(MesMdAutoCodeRuleDO rule, boolean write) {
        if (rule == null || rule.getProjectId() == null) return;
        Long userId = cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId();
        if (write) projectAccess.assertProjectEditorOrOwner(userId, rule.getProjectId());
        else projectAccess.assertProjectViewerOrAbove(userId, rule.getProjectId());
    }

    private void validateRuleScope(Long projectId, String code) {
        if (MesProjectAutoCodeService.supports(code) != (projectId != null)) throw new IllegalArgumentException("工序、工艺流程规则必须指定项目，其余规则使用租户范围");
        if (projectId != null) projectAccess.assertProjectEditorOrOwner(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId(), projectId);
    }

    private void validateRuleCodeUnique(Long id, String code, Long projectId) {
        var query = new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<MesMdAutoCodeRuleDO>().eq(MesMdAutoCodeRuleDO::getCode, code);
        if (projectId == null) query.isNull(MesMdAutoCodeRuleDO::getProjectId); else query.eq(MesMdAutoCodeRuleDO::getProjectId, projectId);
        MesMdAutoCodeRuleDO rule = ruleMapper.selectOne(query);
        if (rule == null) {
            return;
        }
        if (ObjUtil.notEqual(id, rule.getId())) {
            throw exception(AUTO_CODE_RULE_CODE_DUPLICATE);
        }
    }

}
