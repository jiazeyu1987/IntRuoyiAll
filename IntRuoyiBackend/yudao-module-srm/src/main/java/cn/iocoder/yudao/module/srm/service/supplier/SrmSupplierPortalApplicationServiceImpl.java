package cn.iocoder.yudao.module.srm.service.supplier;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.srm.controller.admin.supplieraccess.vo.SrmSupplierAccessSaveReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.supplierportal.vo.SrmSupplierPortalApplicationAuditReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.supplierportal.vo.SrmSupplierPortalApplicationPageReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.supplierportal.vo.SrmSupplierPortalApplicationRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.supplierportal.vo.SrmSupplierPortalApplicationSaveReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmErpSupplierDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmSupplierAccessDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmSupplierPortalApplicationDO;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmErpSupplierMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmSupplierAccessMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmSupplierPortalApplicationMapper;
import cn.iocoder.yudao.module.srm.enums.supplier.SrmSupplierAccessStatusEnum;
import cn.iocoder.yudao.module.srm.enums.supplier.SrmSupplierOnboardingStageStatusEnum;
import cn.iocoder.yudao.module.srm.enums.supplier.SrmSupplierPortalApplicationStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.srm.enums.ErrorCodeConstants.*;

@Service
@Validated
public class SrmSupplierPortalApplicationServiceImpl implements SrmSupplierPortalApplicationService {

    @Resource
    private SrmSupplierPortalApplicationMapper supplierPortalApplicationMapper;
    @Resource
    private SrmErpSupplierMapper srmErpSupplierMapper;
    @Resource
    private SrmSupplierAccessMapper supplierAccessMapper;

    @Override
    public SrmSupplierPortalApplicationRespVO getCurrentApplication() {
        return convert(supplierPortalApplicationMapper.selectByUserId(getRequiredTenantId(), getRequiredLoginUserId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SrmSupplierPortalApplicationRespVO saveDraft(SrmSupplierPortalApplicationSaveReqVO reqVO) {
        SrmSupplierPortalApplicationDO application = createOrReuseApplication(reqVO, false);
        return convert(application);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SrmSupplierPortalApplicationRespVO submit(SrmSupplierPortalApplicationSaveReqVO reqVO) {
        validateSubmitRequiredFields(reqVO);
        SrmSupplierPortalApplicationDO application = createOrReuseApplication(reqVO, true);
        return convert(application);
    }

    @Override
    public PageResult<SrmSupplierPortalApplicationRespVO> getApplicationPage(SrmSupplierPortalApplicationPageReqVO pageReqVO) {
        PageResult<SrmSupplierPortalApplicationDO> pageResult = supplierPortalApplicationMapper.selectPage(pageReqVO);
        return new PageResult<>(pageResult.getList().stream().map(this::convert).toList(), pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(SrmSupplierPortalApplicationAuditReqVO reqVO) {
        SrmSupplierPortalApplicationDO application = validateSubmittedApplication(reqVO.getId());
        Long auditUserId = getRequiredLoginUserId();
        String auditName = getRequiredLoginUserNickname();
        Long supplierId = ensureErpSupplier(application);
        ensureSupplierAccess(application, supplierId);

        application.setSupplierId(supplierId);
        application.setApplicationStatus(SrmSupplierPortalApplicationStatusEnum.APPROVED.getStatus());
        application.setAuditBy(auditUserId);
        application.setAuditName(auditName);
        application.setAuditTime(LocalDateTime.now());
        application.setAuditRemark(reqVO.getAuditRemark());
        supplierPortalApplicationMapper.updateById(application);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(SrmSupplierPortalApplicationAuditReqVO reqVO) {
        SrmSupplierPortalApplicationDO application = validateSubmittedApplication(reqVO.getId());
        if (StrUtil.isBlank(reqVO.getAuditRemark())) {
            throw exception(SUPPLIER_PORTAL_APPLICATION_REJECT_REMARK_REQUIRED);
        }
        application.setApplicationStatus(SrmSupplierPortalApplicationStatusEnum.REJECTED.getStatus());
        application.setAuditBy(getRequiredLoginUserId());
        application.setAuditName(getRequiredLoginUserNickname());
        application.setAuditTime(LocalDateTime.now());
        application.setAuditRemark(reqVO.getAuditRemark());
        supplierPortalApplicationMapper.updateById(application);
    }

    @Override
    public boolean hasApprovedPortalApplication(Long supplierId) {
        return !supplierPortalApplicationMapper.selectApprovedListBySupplierId(getRequiredTenantId(), supplierId).isEmpty();
    }

    private SrmSupplierPortalApplicationDO createOrReuseApplication(SrmSupplierPortalApplicationSaveReqVO reqVO, boolean submitted) {
        Long tenantId = getRequiredTenantId();
        Long userId = getRequiredLoginUserId();
        SrmSupplierPortalApplicationDO application = supplierPortalApplicationMapper.selectByUserId(tenantId, userId);
        if (application == null) {
            application = new SrmSupplierPortalApplicationDO();
            application.setTenantId(tenantId);
            application.setUserId(userId);
        } else {
            validateDraftEditable(application);
        }
        BeanUtils.copyProperties(reqVO, application);
        if (submitted) {
            application.setApplicationStatus(SrmSupplierPortalApplicationStatusEnum.SUBMITTED.getStatus());
            application.setSubmitterName(getRequiredLoginUserNickname());
            application.setSubmittedTime(LocalDateTime.now());
            application.setAuditBy(null);
            application.setAuditName(null);
            application.setAuditTime(null);
            application.setAuditRemark(null);
        } else if (application.getId() == null || !SrmSupplierPortalApplicationStatusEnum.APPROVED.getStatus().equals(application.getApplicationStatus())) {
            application.setApplicationStatus(SrmSupplierPortalApplicationStatusEnum.DRAFT.getStatus());
        }
        if (application.getId() == null) {
            supplierPortalApplicationMapper.insert(application);
        } else {
            supplierPortalApplicationMapper.updateById(application);
        }
        return application;
    }

    private void validateDraftEditable(SrmSupplierPortalApplicationDO application) {
        if (SrmSupplierPortalApplicationStatusEnum.SUBMITTED.getStatus().equals(application.getApplicationStatus())) {
            throw exception(SUPPLIER_PORTAL_APPLICATION_STATUS_INVALID, application.getApplicationStatus());
        }
        if (SrmSupplierPortalApplicationStatusEnum.APPROVED.getStatus().equals(application.getApplicationStatus())) {
            throw exception(SUPPLIER_PORTAL_APPLICATION_STATUS_INVALID, application.getApplicationStatus());
        }
    }

    private void validateSubmitRequiredFields(SrmSupplierPortalApplicationSaveReqVO reqVO) {
        if (StrUtil.hasBlank(
                reqVO.getCompanyName(),
                reqVO.getUnifiedSocialCreditCode(),
                reqVO.getContactName(),
                reqVO.getContactPhone(),
                reqVO.getContactEmail(),
                reqVO.getQualificationAttachmentUrls(),
                reqVO.getBankName(),
                reqVO.getBankAccount())) {
            throw exception(SUPPLIER_PORTAL_APPLICATION_SUBMIT_REQUIRED_FIELDS);
        }
        if (reqVO.getQualificationExpireDate() == null) {
            throw exception(SUPPLIER_PORTAL_APPLICATION_SUBMIT_REQUIRED_FIELDS);
        }
    }

    private SrmSupplierPortalApplicationDO validateSubmittedApplication(Long id) {
        SrmSupplierPortalApplicationDO application = supplierPortalApplicationMapper.selectById(id);
        if (application == null || !Objects.equals(application.getTenantId(), getRequiredTenantId())) {
            throw exception(SUPPLIER_PORTAL_APPLICATION_NOT_EXISTS);
        }
        if (!SrmSupplierPortalApplicationStatusEnum.SUBMITTED.getStatus().equals(application.getApplicationStatus())) {
            throw exception(SUPPLIER_PORTAL_APPLICATION_STATUS_INVALID, application.getApplicationStatus());
        }
        return application;
    }

    private Long ensureErpSupplier(SrmSupplierPortalApplicationDO application) {
        if (application.getSupplierId() != null) {
            SrmErpSupplierDO supplier = srmErpSupplierMapper.selectById(application.getSupplierId());
            if (supplier != null && Objects.equals(supplier.getTenantId(), getRequiredTenantId())) {
                fillSupplierMaster(supplier, application);
                srmErpSupplierMapper.updateById(supplier);
                return supplier.getId();
            }
        }
        SrmErpSupplierDO supplier = srmErpSupplierMapper.selectByTaxNo(getRequiredTenantId(), application.getUnifiedSocialCreditCode());
        if (supplier == null) {
            supplier = srmErpSupplierMapper.selectByExactName(getRequiredTenantId(), application.getCompanyName());
        }
        if (supplier == null) {
            supplier = new SrmErpSupplierDO();
            supplier.setTenantId(getRequiredTenantId());
            supplier.setStatus(CommonStatusEnum.ENABLE.getStatus());
        }
        fillSupplierMaster(supplier, application);
        if (supplier.getId() == null) {
            srmErpSupplierMapper.insert(supplier);
        } else {
            srmErpSupplierMapper.updateById(supplier);
        }
        return supplier.getId();
    }

    private void fillSupplierMaster(SrmErpSupplierDO supplier, SrmSupplierPortalApplicationDO application) {
        supplier.setName(application.getCompanyName());
        supplier.setContact(application.getContactName());
        supplier.setMobile(application.getContactPhone());
        supplier.setEmail(application.getContactEmail());
        supplier.setTaxNo(application.getUnifiedSocialCreditCode());
        supplier.setBankName(application.getBankName());
        supplier.setBankAccount(application.getBankAccount());
        supplier.setBankAddress(application.getBankAddress());
        supplier.setRemark("SRM门户审核导入");
    }

    private void ensureSupplierAccess(SrmSupplierPortalApplicationDO application, Long supplierId) {
        SrmSupplierAccessDO access = supplierAccessMapper.selectBySupplierId(getRequiredTenantId(), supplierId);
        if (access == null) {
            SrmSupplierAccessSaveReqVO saveReqVO = new SrmSupplierAccessSaveReqVO();
            saveReqVO.setSupplierId(supplierId);
            saveReqVO.setAccessRemark("SRM门户审核通过后自动建档");
            saveReqVO.setPortalContactName(application.getContactName());
            saveReqVO.setPortalContactPhone(application.getContactPhone());
            saveReqVO.setQualificationExpireDate(application.getQualificationExpireDate());

            SrmSupplierAccessDO create = BeanUtils.toBean(saveReqVO, SrmSupplierAccessDO.class);
            create.setTenantId(getRequiredTenantId());
            create.setAccessStatus(SrmSupplierAccessStatusEnum.PENDING.getStatus());
            create.setEnabled(true);
            create.setSampleTestStatus(SrmSupplierOnboardingStageStatusEnum.PENDING.getStatus());
            create.setTrialOrderStatus(SrmSupplierOnboardingStageStatusEnum.NOT_STARTED.getStatus());
            create.setSubmittedBy(application.getUserId());
            create.setSubmittedName(application.getSubmitterName());
            create.setSubmittedTime(application.getSubmittedTime());
            supplierAccessMapper.insert(create);
            return;
        }
        access.setPortalContactName(application.getContactName());
        access.setPortalContactPhone(application.getContactPhone());
        access.setQualificationExpireDate(application.getQualificationExpireDate());
        supplierAccessMapper.updateById(access);
    }

    private SrmSupplierPortalApplicationRespVO convert(SrmSupplierPortalApplicationDO application) {
        if (application == null) {
            return null;
        }
        SrmSupplierPortalApplicationRespVO respVO = BeanUtils.toBean(application, SrmSupplierPortalApplicationRespVO.class);
        respVO.setApplicationStatusLabel(SrmSupplierPortalApplicationStatusEnum.getLabel(application.getApplicationStatus()));
        return respVO;
    }

    private Long getRequiredLoginUserId() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            throw exception(SUPPLIER_LOGIN_CONTEXT_MISSING);
        }
        return userId;
    }

    private String getRequiredLoginUserNickname() {
        String nickname = SecurityFrameworkUtils.getLoginUserNickname();
        if (StrUtil.isBlank(nickname)) {
            throw exception(SUPPLIER_LOGIN_CONTEXT_MISSING);
        }
        return nickname;
    }

    private Long getRequiredTenantId() {
        return TenantContextHolder.getRequiredTenantId();
    }
}
