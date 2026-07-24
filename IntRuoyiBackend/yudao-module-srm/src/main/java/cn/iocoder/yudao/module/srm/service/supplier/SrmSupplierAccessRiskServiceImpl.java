package cn.iocoder.yudao.module.srm.service.supplier;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.srm.controller.admin.supplieraccess.vo.*;
import cn.iocoder.yudao.module.srm.controller.admin.supplierrisk.vo.*;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmErpSupplierDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmSupplierAccessDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmSupplierRiskDO;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmErpSupplierMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmSupplierAccessMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmSupplierRiskMapper;
import cn.iocoder.yudao.module.srm.enums.supplier.SrmSupplierAccessStatusEnum;
import cn.iocoder.yudao.module.srm.enums.supplier.SrmSupplierOnboardingStageStatusEnum;
import cn.iocoder.yudao.module.srm.enums.supplier.SrmSupplierRiskLevelEnum;
import cn.iocoder.yudao.module.srm.enums.supplier.SrmSupplierRiskSourceTypeEnum;
import cn.iocoder.yudao.module.srm.enums.supplier.SrmSupplierRiskStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.srm.enums.ErrorCodeConstants.*;

@Service
@Validated
public class SrmSupplierAccessRiskServiceImpl implements SrmSupplierAccessRiskService {

    @Resource
    private SrmSupplierAccessMapper supplierAccessMapper;
    @Resource
    private SrmSupplierRiskMapper supplierRiskMapper;
    @Resource(name = "srmErpSupplierMapper")
    private SrmErpSupplierMapper srmErpSupplierMapper;
    @Resource
    private SrmSupplierPortalApplicationService supplierPortalApplicationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSupplierAccess(SrmSupplierAccessSaveReqVO createReqVO) {
        SrmErpSupplierDO supplier = validateReferenceSupplier(createReqVO.getSupplierId());
        validateSupplierAccessDuplicate(null, createReqVO.getSupplierId());
        Long userId = getRequiredLoginUserId();
        String nickname = getRequiredLoginUserNickname();
        LocalDateTime now = LocalDateTime.now();

        SrmSupplierAccessDO access = BeanUtils.toBean(createReqVO, SrmSupplierAccessDO.class);
        access.setTenantId(getRequiredTenantId());
        access.setAccessStatus(SrmSupplierAccessStatusEnum.PENDING.getStatus());
        access.setEnabled(true);
        access.setPortalContactName(createReqVO.getPortalContactName());
        access.setPortalContactPhone(createReqVO.getPortalContactPhone());
        access.setQualificationExpireDate(createReqVO.getQualificationExpireDate());
        access.setSampleTestStatus(SrmSupplierOnboardingStageStatusEnum.PENDING.getStatus());
        access.setSampleAuditBy(null);
        access.setSampleAuditName(null);
        access.setSampleAuditTime(null);
        access.setSampleAuditRemark(null);
        access.setTrialOrderStatus(SrmSupplierOnboardingStageStatusEnum.NOT_STARTED.getStatus());
        access.setTrialAuditBy(null);
        access.setTrialAuditName(null);
        access.setTrialAuditTime(null);
        access.setTrialAuditRemark(null);
        access.setSubmittedBy(userId);
        access.setSubmittedName(nickname);
        access.setSubmittedTime(now);
        access.setAuditBy(null);
        access.setAuditName(null);
        access.setAuditTime(null);
        access.setAuditRemark(null);
        access.setDisabledBy(null);
        access.setDisabledName(null);
        access.setDisabledTime(null);
        supplierAccessMapper.insert(access);
        return access.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSupplierAccess(SrmSupplierAccessSaveReqVO updateReqVO) {
        SrmSupplierAccessDO access = validateSupplierAccessExists(updateReqVO.getId());
        validateReferenceSupplier(updateReqVO.getSupplierId());
        validateSupplierAccessDuplicate(updateReqVO.getId(), updateReqVO.getSupplierId());

        access.setSupplierId(updateReqVO.getSupplierId());
        access.setAccessRemark(updateReqVO.getAccessRemark());
        access.setPortalContactName(updateReqVO.getPortalContactName());
        access.setPortalContactPhone(updateReqVO.getPortalContactPhone());
        access.setQualificationExpireDate(updateReqVO.getQualificationExpireDate());
        supplierAccessMapper.updateById(access);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSupplierAccess(Long id) {
        SrmSupplierAccessDO access = validateSupplierAccessExists(id);
        // Supplier access must be re-creatable for the same supplier; physical delete
        // prevents historical logic-deleted rows from holding the tenant/supplier unique key.
        supplierAccessMapper.deleteByIdForce(access.getId(), getRequiredTenantId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveSupplierAccess(SrmSupplierAccessAuditReqVO auditReqVO) {
        SrmSupplierAccessDO access = validateSupplierAccessExists(auditReqVO.getId());
        validateReferenceSupplier(access.getSupplierId());
        Long userId = getRequiredLoginUserId();
        String nickname = getRequiredLoginUserNickname();
        validateNotSelfAudit(access, userId);
        if (!SrmSupplierOnboardingStageStatusEnum.PASSED.getStatus().equals(access.getSampleTestStatus())) {
            throw exception(SUPPLIER_ACCESS_SAMPLE_STAGE_BLOCKED);
        }
        if (!SrmSupplierOnboardingStageStatusEnum.PASSED.getStatus().equals(access.getTrialOrderStatus())) {
            throw exception(SUPPLIER_ACCESS_TRIAL_STAGE_BLOCKED);
        }
        if (!supplierRiskMapper.selectOpenHighRiskListBySupplierId(getRequiredTenantId(), access.getSupplierId()).isEmpty()) {
            throw exception(SUPPLIER_ACCESS_APPROVE_HIGH_RISK_BLOCKED);
        }
        access.setAccessStatus(SrmSupplierAccessStatusEnum.APPROVED.getStatus());
        access.setEnabled(true);
        access.setAuditBy(userId);
        access.setAuditName(nickname);
        access.setAuditTime(LocalDateTime.now());
        access.setAuditRemark(auditReqVO.getAuditRemark());
        supplierAccessMapper.updateById(access);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveSampleTest(SrmSupplierAccessAuditReqVO auditReqVO) {
        SrmSupplierAccessDO access = validateSupplierAccessExists(auditReqVO.getId());
        validateAccessPendingForStage(access);
        validatePortalApplicationApproved(access.getSupplierId());
        Long userId = getRequiredLoginUserId();
        String nickname = getRequiredLoginUserNickname();
        access.setSampleTestStatus(SrmSupplierOnboardingStageStatusEnum.PASSED.getStatus());
        access.setSampleAuditBy(userId);
        access.setSampleAuditName(nickname);
        access.setSampleAuditTime(LocalDateTime.now());
        access.setSampleAuditRemark(auditReqVO.getAuditRemark());
        if (!SrmSupplierOnboardingStageStatusEnum.PASSED.getStatus().equals(access.getTrialOrderStatus())) {
            access.setTrialOrderStatus(SrmSupplierOnboardingStageStatusEnum.PENDING.getStatus());
            access.setTrialAuditBy(null);
            access.setTrialAuditName(null);
            access.setTrialAuditTime(null);
            access.setTrialAuditRemark(null);
        }
        supplierAccessMapper.updateById(access);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectSampleTest(SrmSupplierAccessAuditReqVO auditReqVO) {
        SrmSupplierAccessDO access = validateSupplierAccessExists(auditReqVO.getId());
        validateAccessPendingForStage(access);
        validatePortalApplicationApproved(access.getSupplierId());
        if (StrUtil.isBlank(auditReqVO.getAuditRemark())) {
            throw exception(SUPPLIER_ACCESS_SAMPLE_REJECT_REMARK_REQUIRED);
        }
        Long userId = getRequiredLoginUserId();
        String nickname = getRequiredLoginUserNickname();
        access.setSampleTestStatus(SrmSupplierOnboardingStageStatusEnum.REJECTED.getStatus());
        access.setSampleAuditBy(userId);
        access.setSampleAuditName(nickname);
        access.setSampleAuditTime(LocalDateTime.now());
        access.setSampleAuditRemark(auditReqVO.getAuditRemark());
        access.setTrialOrderStatus(SrmSupplierOnboardingStageStatusEnum.NOT_STARTED.getStatus());
        access.setTrialAuditBy(null);
        access.setTrialAuditName(null);
        access.setTrialAuditTime(null);
        access.setTrialAuditRemark(null);
        supplierAccessMapper.updateById(access);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveTrialOrder(SrmSupplierAccessAuditReqVO auditReqVO) {
        SrmSupplierAccessDO access = validateSupplierAccessExists(auditReqVO.getId());
        validateAccessPendingForStage(access);
        validatePortalApplicationApproved(access.getSupplierId());
        if (!SrmSupplierOnboardingStageStatusEnum.PASSED.getStatus().equals(access.getSampleTestStatus())) {
            throw exception(SUPPLIER_ACCESS_SAMPLE_STAGE_BLOCKED);
        }
        Long userId = getRequiredLoginUserId();
        String nickname = getRequiredLoginUserNickname();
        access.setTrialOrderStatus(SrmSupplierOnboardingStageStatusEnum.PASSED.getStatus());
        access.setTrialAuditBy(userId);
        access.setTrialAuditName(nickname);
        access.setTrialAuditTime(LocalDateTime.now());
        access.setTrialAuditRemark(auditReqVO.getAuditRemark());
        supplierAccessMapper.updateById(access);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectTrialOrder(SrmSupplierAccessAuditReqVO auditReqVO) {
        SrmSupplierAccessDO access = validateSupplierAccessExists(auditReqVO.getId());
        validateAccessPendingForStage(access);
        validatePortalApplicationApproved(access.getSupplierId());
        if (!SrmSupplierOnboardingStageStatusEnum.PASSED.getStatus().equals(access.getSampleTestStatus())) {
            throw exception(SUPPLIER_ACCESS_SAMPLE_STAGE_BLOCKED);
        }
        if (StrUtil.isBlank(auditReqVO.getAuditRemark())) {
            throw exception(SUPPLIER_ACCESS_TRIAL_REJECT_REMARK_REQUIRED);
        }
        Long userId = getRequiredLoginUserId();
        String nickname = getRequiredLoginUserNickname();
        access.setTrialOrderStatus(SrmSupplierOnboardingStageStatusEnum.REJECTED.getStatus());
        access.setTrialAuditBy(userId);
        access.setTrialAuditName(nickname);
        access.setTrialAuditTime(LocalDateTime.now());
        access.setTrialAuditRemark(auditReqVO.getAuditRemark());
        supplierAccessMapper.updateById(access);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectSupplierAccess(SrmSupplierAccessAuditReqVO auditReqVO) {
        SrmSupplierAccessDO access = validateSupplierAccessExists(auditReqVO.getId());
        Long userId = getRequiredLoginUserId();
        String nickname = getRequiredLoginUserNickname();
        validateNotSelfAudit(access, userId);
        if (StrUtil.isBlank(auditReqVO.getAuditRemark())) {
            throw exception(SUPPLIER_ACCESS_REJECT_REMARK_REQUIRED);
        }
        access.setAccessStatus(SrmSupplierAccessStatusEnum.REJECTED.getStatus());
        access.setAuditBy(userId);
        access.setAuditName(nickname);
        access.setAuditTime(LocalDateTime.now());
        access.setAuditRemark(auditReqVO.getAuditRemark());
        supplierAccessMapper.updateById(access);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableSupplierAccess(SrmSupplierAccessEnableReqVO enableReqVO) {
        SrmSupplierAccessDO access = validateSupplierAccessExists(enableReqVO.getId());
        Long userId = getRequiredLoginUserId();
        String nickname = getRequiredLoginUserNickname();
        if (Boolean.FALSE.equals(enableReqVO.getEnabled()) && StrUtil.isBlank(enableReqVO.getOperationRemark())) {
            throw exception(SUPPLIER_ACCESS_DISABLE_REASON_REQUIRED);
        }
        access.setEnabled(enableReqVO.getEnabled());
        access.setAuditRemark(StrUtil.blankToDefault(enableReqVO.getOperationRemark(), access.getAuditRemark()));
        if (Boolean.FALSE.equals(enableReqVO.getEnabled())) {
            access.setDisabledBy(userId);
            access.setDisabledName(nickname);
            access.setDisabledTime(LocalDateTime.now());
        } else {
            access.setDisabledBy(null);
            access.setDisabledName(null);
            access.setDisabledTime(null);
        }
        supplierAccessMapper.updateById(access);
    }

    @Override
    public PageResult<SrmSupplierAccessRespVO> getSupplierAccessPage(SrmSupplierAccessPageReqVO pageReqVO) {
        Collection<Long> matchedSupplierIds = resolveSupplierIds(pageReqVO.getSupplierName());
        PageResult<SrmSupplierAccessDO> pageResult = supplierAccessMapper.selectPage(pageReqVO, matchedSupplierIds);
        return new PageResult<>(buildSupplierAccessRespList(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    public SrmSupplierProfileRespVO getSupplierProfile(Long supplierId) {
        SrmErpSupplierDO supplier = validateReferenceSupplier(supplierId);
        SrmSupplierAccessDO access = supplierAccessMapper.selectBySupplierId(getRequiredTenantId(), supplierId);
        List<SrmSupplierRiskDO> riskList = supplierRiskMapper.selectListBySupplierIds(Collections.singletonList(supplierId));
        if (access == null) {
            SrmSupplierProfileRespVO respVO = new SrmSupplierProfileRespVO();
            respVO.setSupplierId(supplierId);
            respVO.setSupplierName(supplier.getName());
            respVO.setAccessStatus(SrmSupplierAccessStatusEnum.PENDING.getStatus());
            respVO.setAccessStatusLabel("未建档");
            respVO.setEnabled(false);
            respVO.setQualificationStatusLabel("未登记");
            respVO.setSampleTestStatus(SrmSupplierOnboardingStageStatusEnum.NOT_STARTED.getStatus());
            respVO.setSampleTestStatusLabel(SrmSupplierOnboardingStageStatusEnum.getLabel(SrmSupplierOnboardingStageStatusEnum.NOT_STARTED.getStatus()));
            respVO.setTrialOrderStatus(SrmSupplierOnboardingStageStatusEnum.NOT_STARTED.getStatus());
            respVO.setTrialOrderStatusLabel(SrmSupplierOnboardingStageStatusEnum.getLabel(SrmSupplierOnboardingStageStatusEnum.NOT_STARTED.getStatus()));
            respVO.setOnboardingStageSummary("待建档");
            respVO.setEligibilitySummary("未建档");
            respVO.setOpenHighRiskCount(0L);
            respVO.setRiskList(Collections.emptyList());
            return respVO;
        }
        Long openHighRiskCount = supplierRiskMapper.selectOpenHighRiskCountBySupplierId(supplierId);
        SrmSupplierProfileRespVO respVO = new SrmSupplierProfileRespVO();
        respVO.setSupplierId(supplierId);
        respVO.setSupplierName(supplier.getName());
        respVO.setAccessId(access.getId());
        respVO.setAccessStatus(access.getAccessStatus());
        respVO.setAccessStatusLabel(SrmSupplierAccessStatusEnum.getLabel(access.getAccessStatus()));
        respVO.setEnabled(access.getEnabled());
        respVO.setPortalContactName(access.getPortalContactName());
        respVO.setPortalContactPhone(access.getPortalContactPhone());
        respVO.setQualificationExpireDate(access.getQualificationExpireDate());
        respVO.setQualificationStatusLabel(resolveQualificationStatusLabel(access));
        respVO.setSampleTestStatus(access.getSampleTestStatus());
        respVO.setSampleTestStatusLabel(SrmSupplierOnboardingStageStatusEnum.getLabel(access.getSampleTestStatus()));
        respVO.setTrialOrderStatus(access.getTrialOrderStatus());
        respVO.setTrialOrderStatusLabel(SrmSupplierOnboardingStageStatusEnum.getLabel(access.getTrialOrderStatus()));
        respVO.setOnboardingStageSummary(resolveOnboardingStageSummary(access));
        respVO.setEligibilitySummary(resolveEligibilitySummary(access, openHighRiskCount));
        respVO.setAccessRemark(access.getAccessRemark());
        respVO.setSubmittedName(access.getSubmittedName());
        respVO.setSubmittedTime(access.getSubmittedTime());
        respVO.setAuditName(access.getAuditName());
        respVO.setAuditTime(access.getAuditTime());
        respVO.setAuditRemark(access.getAuditRemark());
        respVO.setSampleAuditName(access.getSampleAuditName());
        respVO.setSampleAuditTime(access.getSampleAuditTime());
        respVO.setSampleAuditRemark(access.getSampleAuditRemark());
        respVO.setTrialAuditName(access.getTrialAuditName());
        respVO.setTrialAuditTime(access.getTrialAuditTime());
        respVO.setTrialAuditRemark(access.getTrialAuditRemark());
        respVO.setOpenHighRiskCount(openHighRiskCount);
        respVO.setRiskList(buildSupplierRiskRespList(riskList));
        return respVO;
    }

    @Override
    public SrmSupplierEligibilityRespVO checkSupplierEligibility(Long supplierId) {
        LocalDateTime checkedTime = LocalDateTime.now();
        Long tenantId = getRequiredTenantId();
        SrmSupplierAccessDO access = supplierAccessMapper.selectBySupplierId(tenantId, supplierId);
        List<SrmSupplierRiskDO> openHighRisks = supplierRiskMapper.selectOpenHighRiskListBySupplierId(tenantId, supplierId);

        SrmSupplierEligibilityRespVO respVO = new SrmSupplierEligibilityRespVO();
        respVO.setSupplierId(supplierId);
        respVO.setCheckedTime(checkedTime);
        respVO.setOpenHighRiskCount((long) openHighRisks.size());
        respVO.setOpenHighRiskSources(buildRiskSources(openHighRisks));

        SrmErpSupplierDO supplier = srmErpSupplierMapper.selectById(supplierId);
        if (supplier == null) {
            respVO.setEligible(false);
            respVO.setBlockedReason(exception(SUPPLIER_REFERENCE_NOT_EXISTS, supplierId).getMessage());
            return respVO;
        }
        if (!Objects.equals(supplier.getTenantId(), tenantId)) {
            respVO.setEligible(false);
            respVO.setBlockedReason(exception(SUPPLIER_REFERENCE_CROSS_TENANT, supplier.getName()).getMessage());
            return respVO;
        }
        respVO.setSupplierName(supplier.getName());
        if (CommonStatusEnum.isDisable(supplier.getStatus())) {
            respVO.setEligible(false);
            respVO.setBlockedReason(exception(SUPPLIER_REFERENCE_DISABLED, supplier.getName()).getMessage());
            return respVO;
        }
        if (access == null) {
            respVO.setEligible(false);
            respVO.setBlockedReason("当前租户尚未建立该供应商的准入档案");
            return respVO;
        }
        if (!supplierPortalApplicationService.hasApprovedPortalApplication(supplierId)) {
            respVO.setEligible(false);
            respVO.setBlockedReason("供应商尚未完成门户资料审核通过");
            return respVO;
        }

        respVO.setAccessStatus(access.getAccessStatus());
        respVO.setAccessStatusLabel(SrmSupplierAccessStatusEnum.getLabel(access.getAccessStatus()));
        respVO.setEnabled(access.getEnabled());

        if (!SrmSupplierAccessStatusEnum.APPROVED.getStatus().equals(access.getAccessStatus())) {
            respVO.setEligible(false);
            respVO.setBlockedReason("供应商准入状态为" + SrmSupplierAccessStatusEnum.getLabel(access.getAccessStatus()));
            return respVO;
        }
        if (Boolean.FALSE.equals(access.getEnabled())) {
            respVO.setEligible(false);
            respVO.setBlockedReason("供应商准入档案已停用");
            return respVO;
        }
        if (isQualificationExpired(access)) {
            respVO.setEligible(false);
            respVO.setBlockedReason("供应商资质已过期，需更新后才能继续业务操作");
            return respVO;
        }
        if (!openHighRisks.isEmpty()) {
            respVO.setEligible(false);
            respVO.setBlockedReason("供应商存在未处理高风险：" + String.join("；", respVO.getOpenHighRiskSources()));
            return respVO;
        }
        respVO.setEligible(true);
        respVO.setBlockedReason(null);
        return respVO;
    }

    @Override
    public void validateSupplierEligible(Long supplierId) {
        SrmSupplierEligibilityRespVO result = checkSupplierEligibility(supplierId);
        if (!Boolean.TRUE.equals(result.getEligible())) {
            throw exception(SUPPLIER_ELIGIBILITY_BLOCKED, result.getBlockedReason());
        }
    }

    @Override
    public List<SrmSupplierReferenceRespVO> getReferenceSupplierList(String keyword) {
        Long tenantId = getRequiredTenantId();
        return srmErpSupplierMapper.selectEnabledListByKeyword(keyword).stream()
                .filter(supplier -> Objects.equals(supplier.getTenantId(), tenantId))
                .map(supplier -> {
                    SrmSupplierReferenceRespVO respVO = new SrmSupplierReferenceRespVO();
                    respVO.setId(supplier.getId());
                    respVO.setName(supplier.getName());
                    return respVO;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSupplierRisk(SrmSupplierRiskSaveReqVO createReqVO) {
        validateReferenceSupplier(createReqVO.getSupplierId());
        validateRiskLevel(createReqVO.getRiskLevel());
        validateRiskSourceType(createReqVO.getSourceType());
        if (createReqVO.getSupplierAccessId() != null) {
            SrmSupplierAccessDO access = validateSupplierAccessExists(createReqVO.getSupplierAccessId());
            if (!Objects.equals(access.getSupplierId(), createReqVO.getSupplierId())) {
                throw exception(SUPPLIER_ACCESS_SUPPLIER_MISMATCH);
            }
        }

        Long userId = getRequiredLoginUserId();
        String nickname = getRequiredLoginUserNickname();
        LocalDateTime now = LocalDateTime.now();

        SrmSupplierRiskDO risk = BeanUtils.toBean(createReqVO, SrmSupplierRiskDO.class);
        risk.setTenantId(getRequiredTenantId());
        risk.setRiskStatus(SrmSupplierRiskStatusEnum.OPEN.getStatus());
        risk.setReportedBy(userId);
        risk.setReportedName(nickname);
        risk.setReportedTime(now);
        risk.setResolvedBy(null);
        risk.setResolvedName(null);
        risk.setResolvedTime(null);
        risk.setResolutionRemark(null);
        supplierRiskMapper.insert(risk);
        return risk.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolveSupplierRisk(SrmSupplierRiskResolveReqVO resolveReqVO) {
        SrmSupplierRiskDO risk = validateSupplierRiskExists(resolveReqVO.getId());
        if (SrmSupplierRiskStatusEnum.RESOLVED.getStatus().equals(risk.getRiskStatus())) {
            throw exception(SUPPLIER_RISK_ALREADY_RESOLVED);
        }
        if (StrUtil.isBlank(resolveReqVO.getResolutionRemark())) {
            throw exception(SUPPLIER_RISK_RESOLUTION_REMARK_REQUIRED);
        }
        Long userId = getRequiredLoginUserId();
        String nickname = getRequiredLoginUserNickname();
        risk.setRiskStatus(SrmSupplierRiskStatusEnum.RESOLVED.getStatus());
        risk.setResolvedBy(userId);
        risk.setResolvedName(nickname);
        risk.setResolvedTime(LocalDateTime.now());
        risk.setResolutionRemark(resolveReqVO.getResolutionRemark());
        supplierRiskMapper.updateById(risk);
    }

    @Override
    public PageResult<SrmSupplierRiskRespVO> getSupplierRiskPage(SrmSupplierRiskPageReqVO pageReqVO) {
        Collection<Long> matchedSupplierIds = resolveSupplierIds(pageReqVO.getSupplierName());
        PageResult<SrmSupplierRiskDO> pageResult = supplierRiskMapper.selectPage(pageReqVO, matchedSupplierIds);
        return new PageResult<>(buildSupplierRiskRespList(pageResult.getList()), pageResult.getTotal());
    }

    private SrmSupplierAccessDO validateSupplierAccessExists(Long id) {
        SrmSupplierAccessDO access = supplierAccessMapper.selectById(id);
        if (access == null || !Objects.equals(access.getTenantId(), getRequiredTenantId())) {
            throw exception(SUPPLIER_ACCESS_NOT_EXISTS);
        }
        return access;
    }

    private SrmSupplierRiskDO validateSupplierRiskExists(Long id) {
        SrmSupplierRiskDO risk = supplierRiskMapper.selectById(id);
        if (risk == null || !Objects.equals(risk.getTenantId(), getRequiredTenantId())) {
            throw exception(SUPPLIER_RISK_NOT_EXISTS);
        }
        return risk;
    }

    private SrmErpSupplierDO validateReferenceSupplier(Long supplierId) {
        SrmErpSupplierDO supplier = srmErpSupplierMapper.selectById(supplierId);
        if (supplier == null) {
            throw exception(SUPPLIER_REFERENCE_NOT_EXISTS, supplierId);
        }
        if (!Objects.equals(supplier.getTenantId(), getRequiredTenantId())) {
            throw exception(SUPPLIER_REFERENCE_CROSS_TENANT, supplier.getName());
        }
        if (CommonStatusEnum.isDisable(supplier.getStatus())) {
            throw exception(SUPPLIER_REFERENCE_DISABLED, supplier.getName());
        }
        return supplier;
    }

    private void validateSupplierAccessDuplicate(Long id, Long supplierId) {
        SrmSupplierAccessDO existing = supplierAccessMapper.selectBySupplierId(getRequiredTenantId(), supplierId);
        if (existing != null && !Objects.equals(existing.getId(), id)) {
            throw exception(SUPPLIER_ACCESS_DUPLICATE);
        }
    }

    private void validateRiskLevel(String riskLevel) {
        if (!SrmSupplierRiskLevelEnum.contains(riskLevel)) {
            throw exception(SUPPLIER_RISK_LEVEL_INVALID, riskLevel);
        }
    }

    private void validateRiskSourceType(String sourceType) {
        if (!SrmSupplierRiskSourceTypeEnum.contains(sourceType)) {
            throw exception(SUPPLIER_RISK_SOURCE_TYPE_INVALID, sourceType);
        }
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

    private Collection<Long> resolveSupplierIds(String supplierName) {
        if (StrUtil.isBlank(supplierName)) {
            return null;
        }
        Long tenantId = getRequiredTenantId();
        return srmErpSupplierMapper.selectListByKeyword(supplierName).stream()
                .filter(supplier -> Objects.equals(supplier.getTenantId(), tenantId))
                .map(SrmErpSupplierDO::getId)
                .collect(Collectors.toList());
    }

    private List<SrmSupplierAccessRespVO> buildSupplierAccessRespList(List<SrmSupplierAccessDO> accessList) {
        Map<Long, SrmErpSupplierDO> supplierMap = buildSupplierMap(accessList.stream()
                .map(SrmSupplierAccessDO::getSupplierId)
                .collect(Collectors.toSet()));
        return accessList.stream().map(access -> {
            SrmErpSupplierDO supplier = supplierMap.get(access.getSupplierId());
            Long openHighRiskCount = supplierRiskMapper.selectOpenHighRiskCountBySupplierId(access.getSupplierId());
            SrmSupplierAccessRespVO respVO = new SrmSupplierAccessRespVO();
            respVO.setId(access.getId());
            respVO.setSupplierId(access.getSupplierId());
            respVO.setSupplierName(resolveSupplierName(access.getSupplierId(), supplier));
            respVO.setAccessStatus(access.getAccessStatus());
            respVO.setAccessStatusLabel(SrmSupplierAccessStatusEnum.getLabel(access.getAccessStatus()));
            respVO.setEnabled(access.getEnabled());
            respVO.setAccessRemark(access.getAccessRemark());
            respVO.setPortalContactName(access.getPortalContactName());
            respVO.setPortalContactPhone(access.getPortalContactPhone());
            respVO.setQualificationExpireDate(access.getQualificationExpireDate());
            respVO.setQualificationStatusLabel(resolveQualificationStatusLabel(access));
            respVO.setSampleTestStatus(access.getSampleTestStatus());
            respVO.setSampleTestStatusLabel(SrmSupplierOnboardingStageStatusEnum.getLabel(access.getSampleTestStatus()));
            respVO.setTrialOrderStatus(access.getTrialOrderStatus());
            respVO.setTrialOrderStatusLabel(SrmSupplierOnboardingStageStatusEnum.getLabel(access.getTrialOrderStatus()));
            respVO.setOnboardingStageSummary(resolveOnboardingStageSummary(access));
            respVO.setOpenHighRiskCount(openHighRiskCount);
            respVO.setEligibilitySummary(resolveEligibilitySummary(access, openHighRiskCount));
            respVO.setSubmittedName(access.getSubmittedName());
            respVO.setSubmittedTime(access.getSubmittedTime());
            respVO.setAuditName(access.getAuditName());
            respVO.setAuditTime(access.getAuditTime());
            respVO.setAuditRemark(access.getAuditRemark());
            respVO.setDisabledName(access.getDisabledName());
            respVO.setDisabledTime(access.getDisabledTime());
            respVO.setCreateTime(access.getCreateTime());
            return respVO;
        }).collect(Collectors.toList());
    }

    private List<SrmSupplierRiskRespVO> buildSupplierRiskRespList(List<SrmSupplierRiskDO> riskList) {
        Map<Long, SrmErpSupplierDO> supplierMap = buildSupplierMap(riskList.stream()
                .map(SrmSupplierRiskDO::getSupplierId)
                .collect(Collectors.toSet()));
        return riskList.stream().map(risk -> {
            SrmErpSupplierDO supplier = supplierMap.get(risk.getSupplierId());
            SrmSupplierRiskRespVO respVO = new SrmSupplierRiskRespVO();
            respVO.setId(risk.getId());
            respVO.setSupplierId(risk.getSupplierId());
            respVO.setSupplierName(resolveSupplierName(risk.getSupplierId(), supplier));
            respVO.setRiskLevel(risk.getRiskLevel());
            respVO.setRiskLevelLabel(SrmSupplierRiskLevelEnum.getLabel(risk.getRiskLevel()));
            respVO.setRiskStatus(risk.getRiskStatus());
            respVO.setRiskStatusLabel(SrmSupplierRiskStatusEnum.getLabel(risk.getRiskStatus()));
            respVO.setSourceType(risk.getSourceType());
            respVO.setSourceTypeLabel(SrmSupplierRiskSourceTypeEnum.getLabel(risk.getSourceType()));
            respVO.setSourceId(risk.getSourceId());
            respVO.setSourceCode(risk.getSourceCode());
            respVO.setSourceName(risk.getSourceName());
            respVO.setRiskDescription(risk.getRiskDescription());
            respVO.setRiskRemark(risk.getRiskRemark());
            respVO.setReportedName(risk.getReportedName());
            respVO.setReportedTime(risk.getReportedTime());
            respVO.setResolvedName(risk.getResolvedName());
            respVO.setResolvedTime(risk.getResolvedTime());
            respVO.setResolutionRemark(risk.getResolutionRemark());
            return respVO;
        }).collect(Collectors.toList());
    }

    private Map<Long, SrmErpSupplierDO> buildSupplierMap(Set<Long> supplierIds) {
        Long tenantId = getRequiredTenantId();
        return srmErpSupplierMapper.selectListByIds(supplierIds).stream()
                .filter(supplier -> Objects.equals(supplier.getTenantId(), tenantId))
                .collect(Collectors.toMap(SrmErpSupplierDO::getId, supplier -> supplier));
    }

    private void validateNotSelfAudit(SrmSupplierAccessDO access, Long auditUserId) {
        if (Objects.equals(access.getSubmittedBy(), auditUserId)) {
            throw exception(SUPPLIER_ACCESS_SELF_AUDIT_FORBIDDEN);
        }
    }

    private void validateAccessPendingForStage(SrmSupplierAccessDO access) {
        if (!SrmSupplierAccessStatusEnum.PENDING.getStatus().equals(access.getAccessStatus())) {
            throw exception(SUPPLIER_ACCESS_STATUS_INVALID, access.getAccessStatus());
        }
    }

    private void validatePortalApplicationApproved(Long supplierId) {
        if (!supplierPortalApplicationService.hasApprovedPortalApplication(supplierId)) {
            throw exception(SUPPLIER_PORTAL_APPLICATION_NOT_APPROVED);
        }
    }

    private String resolveSupplierName(Long supplierId, SrmErpSupplierDO supplier) {
        return supplier != null ? supplier.getName() : "ERP供应商缺失#" + supplierId;
    }

    private String resolveEligibilitySummary(SrmSupplierAccessDO access, Long openHighRiskCount) {
        if (!SrmSupplierAccessStatusEnum.APPROVED.getStatus().equals(access.getAccessStatus())) {
            return resolveOnboardingStageSummary(access);
        }
        if (Boolean.FALSE.equals(access.getEnabled())) {
            return "已停用";
        }
        if (isQualificationExpired(access)) {
            return "资质已过期";
        }
        if (isQualificationExpiringSoon(access)) {
            return "资质待更新";
        }
        if (openHighRiskCount != null && openHighRiskCount > 0) {
            return "高风险阻断";
        }
        return "合格";
    }

    private String resolveOnboardingStageSummary(SrmSupplierAccessDO access) {
        if (SrmSupplierOnboardingStageStatusEnum.REJECTED.getStatus().equals(access.getSampleTestStatus())) {
            return "样品测试驳回";
        }
        if (SrmSupplierOnboardingStageStatusEnum.PENDING.getStatus().equals(access.getSampleTestStatus())) {
            return "样品测试中";
        }
        if (SrmSupplierOnboardingStageStatusEnum.REJECTED.getStatus().equals(access.getTrialOrderStatus())) {
            return "小批试用驳回";
        }
        if (SrmSupplierOnboardingStageStatusEnum.NOT_STARTED.getStatus().equals(access.getTrialOrderStatus())) {
            return "样品测试中";
        }
        if (SrmSupplierOnboardingStageStatusEnum.PENDING.getStatus().equals(access.getTrialOrderStatus())) {
            return "小批试用中";
        }
        if (SrmSupplierOnboardingStageStatusEnum.PASSED.getStatus().equals(access.getTrialOrderStatus())) {
            return "待准入审批";
        }
        return "待准入";
    }

    private String resolveQualificationStatusLabel(SrmSupplierAccessDO access) {
        if (access.getQualificationExpireDate() == null) {
            return "未登记";
        }
        if (isQualificationExpired(access)) {
            return "已过期";
        }
        if (isQualificationExpiringSoon(access)) {
            return "待更新";
        }
        return "有效";
    }

    private boolean isQualificationExpired(SrmSupplierAccessDO access) {
        return access.getQualificationExpireDate() != null
                && access.getQualificationExpireDate().isBefore(LocalDate.now());
    }

    private boolean isQualificationExpiringSoon(SrmSupplierAccessDO access) {
        return access.getQualificationExpireDate() != null
                && !isQualificationExpired(access)
                && !access.getQualificationExpireDate().isAfter(LocalDate.now().plusDays(30));
    }

    private List<String> buildRiskSources(List<SrmSupplierRiskDO> riskList) {
        return riskList.stream()
                .map(risk -> {
                    String sourceTypeLabel = SrmSupplierRiskSourceTypeEnum.getLabel(risk.getSourceType());
                    String sourceName = StrUtil.blankToDefault(risk.getSourceName(), sourceTypeLabel);
                    String sourceCode = StrUtil.blankToDefault(risk.getSourceCode(), "未登记来源编码");
                    return sourceTypeLabel + " / " + sourceName + " / " + sourceCode;
                })
                .collect(Collectors.toList());
    }
}
