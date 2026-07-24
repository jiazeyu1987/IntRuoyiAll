package cn.iocoder.yudao.module.srm.service.procurement;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo.*;
import jakarta.validation.Valid;

public interface SrmProcurementPlanService {

    Long createProcurementPlan(@Valid SrmProcurementPlanSaveReqVO createReqVO);

    void submitProcurementPlan(Long id);

    void approveProcurementPlan(@Valid SrmProcurementPlanAuditReqVO auditReqVO);

    void rejectProcurementPlan(@Valid SrmProcurementPlanAuditReqVO auditReqVO);

    SrmSourcingProjectRespVO generateSourcingProject(@Valid SrmProcurementPlanGenerateReqVO generateReqVO);

    SrmProcurementPlanRespVO getProcurementPlan(Long id);

    PageResult<SrmProcurementPlanRespVO> getProcurementPlanPage(SrmProcurementPlanPageReqVO pageReqVO);
}
