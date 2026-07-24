package cn.iocoder.yudao.module.srm.service.supplier;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.srm.controller.admin.supplierportal.vo.SrmSupplierPortalApplicationAuditReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.supplierportal.vo.SrmSupplierPortalApplicationPageReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.supplierportal.vo.SrmSupplierPortalApplicationRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.supplierportal.vo.SrmSupplierPortalApplicationSaveReqVO;
import jakarta.validation.Valid;

public interface SrmSupplierPortalApplicationService {

    SrmSupplierPortalApplicationRespVO getCurrentApplication();

    SrmSupplierPortalApplicationRespVO saveDraft(@Valid SrmSupplierPortalApplicationSaveReqVO reqVO);

    SrmSupplierPortalApplicationRespVO submit(@Valid SrmSupplierPortalApplicationSaveReqVO reqVO);

    PageResult<SrmSupplierPortalApplicationRespVO> getApplicationPage(SrmSupplierPortalApplicationPageReqVO pageReqVO);

    void approve(@Valid SrmSupplierPortalApplicationAuditReqVO reqVO);

    void reject(@Valid SrmSupplierPortalApplicationAuditReqVO reqVO);

    boolean hasApprovedPortalApplication(Long supplierId);
}
