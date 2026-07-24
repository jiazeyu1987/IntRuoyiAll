package cn.iocoder.yudao.module.srm.service.coderule;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.srm.controller.admin.coderule.vo.SrmCodeRulePageReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.coderule.vo.SrmCodeRuleSaveReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.coderule.SrmCodeRuleDO;
import jakarta.validation.Valid;

/**
 * SRM 编码规则 Service 接口。
 */
public interface SrmCodeRuleService {

    /**
     * 创建编码规则。
     *
     * @param createReqVO 创建信息
     * @return 规则编号
     */
    Long createCodeRule(@Valid SrmCodeRuleSaveReqVO createReqVO);

    /**
     * 更新编码规则。
     *
     * @param updateReqVO 更新信息
     */
    void updateCodeRule(@Valid SrmCodeRuleSaveReqVO updateReqVO);

    /**
     * 启停编码规则。
     *
     * @param id      规则编号
     * @param enabled 是否启用
     */
    void enableCodeRule(Long id, Boolean enabled);

    /**
     * 获得编码规则。
     *
     * @param id 规则编号
     * @return 编码规则
     */
    SrmCodeRuleDO getCodeRule(Long id);

    /**
     * 获得编码规则分页。
     *
     * @param pageReqVO 分页查询
     * @return 编码规则分页
     */
    PageResult<SrmCodeRuleDO> getCodeRulePage(SrmCodeRulePageReqVO pageReqVO);

    /**
     * 生成业务编号。
     *
     * @param targetForm 目标表单
     * @return 业务编号
     */
    String generateCode(String targetForm);

}
