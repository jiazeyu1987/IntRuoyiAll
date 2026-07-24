package cn.iocoder.yudao.module.srm.service.supplier;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.srm.controller.admin.supplieraccess.vo.*;
import cn.iocoder.yudao.module.srm.controller.admin.supplierrisk.vo.*;

import jakarta.validation.Valid;

import java.util.List;

public interface SrmSupplierAccessRiskService {

    Long createSupplierAccess(@Valid SrmSupplierAccessSaveReqVO createReqVO);

    void updateSupplierAccess(@Valid SrmSupplierAccessSaveReqVO updateReqVO);

    void deleteSupplierAccess(Long id);

    void approveSupplierAccess(@Valid SrmSupplierAccessAuditReqVO auditReqVO);

    void rejectSupplierAccess(@Valid SrmSupplierAccessAuditReqVO auditReqVO);

    void approveSampleTest(@Valid SrmSupplierAccessAuditReqVO auditReqVO);

    void rejectSampleTest(@Valid SrmSupplierAccessAuditReqVO auditReqVO);

    void approveTrialOrder(@Valid SrmSupplierAccessAuditReqVO auditReqVO);

    void rejectTrialOrder(@Valid SrmSupplierAccessAuditReqVO auditReqVO);

    void enableSupplierAccess(@Valid SrmSupplierAccessEnableReqVO enableReqVO);

    PageResult<SrmSupplierAccessRespVO> getSupplierAccessPage(SrmSupplierAccessPageReqVO pageReqVO);

    SrmSupplierProfileRespVO getSupplierProfile(Long supplierId);

    SrmSupplierEligibilityRespVO checkSupplierEligibility(Long supplierId);

    void validateSupplierEligible(Long supplierId);

    List<SrmSupplierReferenceRespVO> getReferenceSupplierList(String keyword);

    Long createSupplierRisk(@Valid SrmSupplierRiskSaveReqVO createReqVO);

    void resolveSupplierRisk(@Valid SrmSupplierRiskResolveReqVO resolveReqVO);

    PageResult<SrmSupplierRiskRespVO> getSupplierRiskPage(SrmSupplierRiskPageReqVO pageReqVO);
}
