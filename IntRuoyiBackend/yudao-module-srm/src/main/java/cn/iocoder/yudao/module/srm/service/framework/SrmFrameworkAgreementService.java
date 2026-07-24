package cn.iocoder.yudao.module.srm.service.framework;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.srm.controller.admin.framework.vo.*;
import jakarta.validation.Valid;

public interface SrmFrameworkAgreementService {

    Long createFrameworkPlan(@Valid SrmFrameworkPlanSaveReqVO createReqVO);

    void submitFrameworkPlan(Long id);

    void approveFrameworkPlan(@Valid SrmFrameworkPlanAuditReqVO auditReqVO);

    void rejectFrameworkPlan(@Valid SrmFrameworkPlanAuditReqVO auditReqVO);

    SrmFrameworkAgreementRespVO createAgreement(Long frameworkPlanId);

    SrmFrameworkPlanRespVO getFrameworkPlan(Long id);

    PageResult<SrmFrameworkPlanRespVO> getFrameworkPlanPage(SrmFrameworkPlanPageReqVO pageReqVO);

    PageResult<SrmFrameworkAgreementRespVO> getAgreementPage(SrmFrameworkAgreementPageReqVO pageReqVO);
}
