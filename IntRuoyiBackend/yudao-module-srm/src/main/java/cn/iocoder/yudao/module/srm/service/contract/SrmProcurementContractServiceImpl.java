package cn.iocoder.yudao.module.srm.service.contract;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.srm.controller.admin.contract.vo.SrmProcurementContractCancelReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.contract.vo.SrmProcurementContractPageReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.contract.vo.SrmProcurementContractRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.contract.vo.SrmProcurementContractSaveReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.contract.SrmProcurementContractAttachmentDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.contract.SrmProcurementContractDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.contract.SrmProcurementContractPaymentDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.contract.SrmProcurementContractSigningDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.procurement.SrmSourcingProjectDO;
import cn.iocoder.yudao.module.srm.dal.mysql.contract.SrmProcurementContractAttachmentMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.contract.SrmProcurementContractMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.contract.SrmProcurementContractPaymentMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.contract.SrmProcurementContractSigningMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.procurement.SrmSourcingProjectMapper;
import cn.iocoder.yudao.module.srm.enums.coderule.SrmCodeRuleTargetFormEnum;
import cn.iocoder.yudao.module.srm.enums.contract.SrmProcurementContractSourceTypeEnum;
import cn.iocoder.yudao.module.srm.enums.contract.SrmProcurementContractStatusEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmProcurementMethodEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmSourcingProjectStatusEnum;
import cn.iocoder.yudao.module.srm.service.coderule.SrmCodeRuleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.srm.enums.ErrorCodeConstants.*;

@Service
@Validated
public class SrmProcurementContractServiceImpl implements SrmProcurementContractService {

    @Resource
    private SrmCodeRuleService codeRuleService;
    @Resource
    private SrmSourcingProjectMapper sourcingProjectMapper;
    @Resource
    private SrmProcurementContractMapper contractMapper;
    @Resource
    private SrmProcurementContractPaymentMapper paymentMapper;
    @Resource
    private SrmProcurementContractSigningMapper signingMapper;
    @Resource
    private SrmProcurementContractAttachmentMapper attachmentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SrmProcurementContractRespVO createContract(SrmProcurementContractSaveReqVO createReqVO) {
        validateCreateReq(createReqVO);
        SrmSourcingProjectDO source = validateContractableSource(createReqVO.getSourceType(), createReqVO.getSourceId());
        if (contractMapper.selectEffectiveBySource(createReqVO.getSourceType(), createReqVO.getSourceId()) != null
                || source.getContractId() != null) {
            throw exception(PROCUREMENT_CONTRACT_SOURCE_ALREADY_CONTRACTED);
        }

        LocalDateTime now = LocalDateTime.now();
        SrmProcurementContractDO contract = SrmProcurementContractDO.builder()
                .contractNo(codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.PROCUREMENT_CONTRACT.getTargetForm()))
                .contractTitle(createReqVO.getContractTitle())
                .sourceType(createReqVO.getSourceType())
                .sourceId(source.getId())
                .sourceNo(source.getProjectNo())
                .supplierId(source.getDealSupplierId())
                .supplierName(source.getDealSupplierName())
                .contractAmount(createReqVO.getContractAmount())
                .currency(createReqVO.getCurrency())
                .effectiveDate(createReqVO.getEffectiveDate())
                .expireDate(createReqVO.getExpireDate())
                .contractStatus(SrmProcurementContractStatusEnum.EFFECTIVE.getStatus())
                .createdBy(getRequiredLoginUserId())
                .createdName(getRequiredLoginUserNickname())
                .createdTime(now)
                .build();
        contract.setTenantId(getRequiredTenantId());
        contractMapper.insert(contract);

        for (SrmProcurementContractSaveReqVO.Payment paymentReq : createReqVO.getPayments()) {
            SrmProcurementContractPaymentDO payment = SrmProcurementContractPaymentDO.builder()
                    .contractId(contract.getId())
                    .paymentStage(paymentReq.getPaymentStage())
                    .paymentRatio(paymentReq.getPaymentRatio())
                    .paymentAmount(paymentReq.getPaymentAmount())
                    .dueDate(paymentReq.getDueDate())
                    .paymentRemark(paymentReq.getPaymentRemark())
                    .build();
            payment.setTenantId(getRequiredTenantId());
            paymentMapper.insert(payment);
        }
        for (SrmProcurementContractSaveReqVO.Signing signingReq : createReqVO.getSignings()) {
            SrmProcurementContractSigningDO signing = SrmProcurementContractSigningDO.builder()
                    .contractId(contract.getId())
                    .signingParty(signingReq.getSigningParty())
                    .signerName(signingReq.getSignerName())
                    .signingDate(signingReq.getSigningDate())
                    .signingRemark(signingReq.getSigningRemark())
                    .build();
            signing.setTenantId(getRequiredTenantId());
            signingMapper.insert(signing);
        }
        for (SrmProcurementContractSaveReqVO.Attachment attachmentReq : createReqVO.getAttachments()) {
            SrmProcurementContractAttachmentDO attachment = SrmProcurementContractAttachmentDO.builder()
                    .contractId(contract.getId())
                    .attachmentName(attachmentReq.getAttachmentName())
                    .attachmentUrl(attachmentReq.getAttachmentUrl())
                    .attachmentType(attachmentReq.getAttachmentType())
                    .build();
            attachment.setTenantId(getRequiredTenantId());
            attachmentMapper.insert(attachment);
        }

        source.setContractId(contract.getId());
        source.setProjectStatus(SrmSourcingProjectStatusEnum.CONTRACT_CREATED.getStatus());
        sourcingProjectMapper.updateById(source);
        return buildContractResp(contract.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelContract(SrmProcurementContractCancelReqVO cancelReqVO) {
        if (StrUtil.isBlank(cancelReqVO.getCancelReason())) {
            throw exception(PROCUREMENT_CONTRACT_CANCEL_REASON_REQUIRED);
        }
        SrmProcurementContractDO contract = validateContract(cancelReqVO.getId());
        if (!SrmProcurementContractStatusEnum.EFFECTIVE.getStatus().equals(contract.getContractStatus())) {
            throw exception(PROCUREMENT_CONTRACT_STATUS_INVALID,
                    SrmProcurementContractStatusEnum.getLabel(contract.getContractStatus()));
        }
        contract.setContractStatus(SrmProcurementContractStatusEnum.CANCELLED.getStatus());
        contract.setCancelledBy(getRequiredLoginUserId());
        contract.setCancelledName(getRequiredLoginUserNickname());
        contract.setCancelledTime(LocalDateTime.now());
        contract.setCancelReason(cancelReqVO.getCancelReason());
        contractMapper.updateById(contract);
        restoreSource(contract);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteContract(Long id) {
        SrmProcurementContractDO contract = validateContract(id);
        restoreSource(contract);
        contractMapper.deleteById(id);
    }

    @Override
    public SrmProcurementContractRespVO getContract(Long id) {
        return buildContractResp(validateContract(id).getId());
    }

    @Override
    public PageResult<SrmProcurementContractRespVO> getContractPage(SrmProcurementContractPageReqVO pageReqVO) {
        PageResult<SrmProcurementContractDO> pageResult = contractMapper.selectPage(pageReqVO);
        return new PageResult<>(pageResult.getList().stream()
                .map(contract -> buildContractResp(contract.getId()))
                .collect(Collectors.toList()), pageResult.getTotal());
    }

    private void validateCreateReq(SrmProcurementContractSaveReqVO reqVO) {
        if (reqVO.getSourceId() == null || StrUtil.isBlank(reqVO.getContractTitle()) || StrUtil.isBlank(reqVO.getCurrency())) {
            throw exception(PROCUREMENT_CONTRACT_HEADER_INVALID);
        }
        if (!SrmProcurementContractSourceTypeEnum.contains(reqVO.getSourceType())) {
            throw exception(PROCUREMENT_CONTRACT_SOURCE_TYPE_INVALID, reqVO.getSourceType());
        }
        if (reqVO.getContractAmount() == null || reqVO.getContractAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PROCUREMENT_CONTRACT_AMOUNT_INVALID);
        }
        if (reqVO.getEffectiveDate() == null || reqVO.getExpireDate() == null
                || reqVO.getExpireDate().isBefore(reqVO.getEffectiveDate())) {
            throw exception(PROCUREMENT_CONTRACT_DATE_INVALID);
        }
        if (reqVO.getPayments() == null || reqVO.getPayments().isEmpty()) {
            throw exception(PROCUREMENT_CONTRACT_PAYMENT_REQUIRED);
        }
        if (reqVO.getSignings() == null || reqVO.getSignings().isEmpty()) {
            throw exception(PROCUREMENT_CONTRACT_SIGNING_REQUIRED);
        }
        if (reqVO.getAttachments() == null || reqVO.getAttachments().isEmpty()) {
            throw exception(PROCUREMENT_CONTRACT_ATTACHMENT_REQUIRED);
        }
        for (SrmProcurementContractSaveReqVO.Payment payment : reqVO.getPayments()) {
            if (StrUtil.isBlank(payment.getPaymentStage())
                    || payment.getPaymentRatio() == null || payment.getPaymentRatio().compareTo(BigDecimal.ZERO) <= 0
                    || payment.getPaymentAmount() == null || payment.getPaymentAmount().compareTo(BigDecimal.ZERO) <= 0
                    || payment.getDueDate() == null) {
                throw exception(PROCUREMENT_CONTRACT_PAYMENT_INVALID);
            }
        }
        for (SrmProcurementContractSaveReqVO.Signing signing : reqVO.getSignings()) {
            if (StrUtil.isBlank(signing.getSigningParty()) || StrUtil.isBlank(signing.getSignerName())
                    || signing.getSigningDate() == null) {
                throw exception(PROCUREMENT_CONTRACT_SIGNING_INVALID);
            }
        }
        for (SrmProcurementContractSaveReqVO.Attachment attachment : reqVO.getAttachments()) {
            if (StrUtil.isBlank(attachment.getAttachmentName()) || StrUtil.isBlank(attachment.getAttachmentUrl())
                    || StrUtil.isBlank(attachment.getAttachmentType())) {
                throw exception(PROCUREMENT_CONTRACT_ATTACHMENT_INVALID);
            }
        }
    }

    private SrmSourcingProjectDO validateContractableSource(String sourceType, Long sourceId) {
        SrmSourcingProjectDO source = sourcingProjectMapper.selectById(sourceId);
        if (source == null || !Objects.equals(source.getTenantId(), getRequiredTenantId())) {
            throw exception(PROCUREMENT_CONTRACT_SOURCE_NOT_EXISTS);
        }
        if (SrmProcurementContractSourceTypeEnum.NON_BIDDING.getSourceType().equals(sourceType)) {
            if (!SrmProcurementMethodEnum.NON_BIDDING.getMethod().equals(source.getProjectType())) {
                throw exception(PROCUREMENT_CONTRACT_SOURCE_TYPE_INVALID, sourceType);
            }
            if (!SrmSourcingProjectStatusEnum.DEAL_CONFIRMED.getStatus().equals(source.getProjectStatus())) {
                throw exception(PROCUREMENT_CONTRACT_SOURCE_STATUS_INVALID,
                        SrmSourcingProjectStatusEnum.getLabel(source.getProjectStatus()));
            }
        } else if (SrmProcurementContractSourceTypeEnum.TENDER.getSourceType().equals(sourceType)) {
            if (!SrmProcurementMethodEnum.TENDER.getMethod().equals(source.getProjectType())) {
                throw exception(PROCUREMENT_CONTRACT_SOURCE_TYPE_INVALID, sourceType);
            }
            if (!SrmSourcingProjectStatusEnum.WINNING_CONFIRMED.getStatus().equals(source.getProjectStatus())) {
                throw exception(PROCUREMENT_CONTRACT_SOURCE_STATUS_INVALID,
                        SrmSourcingProjectStatusEnum.getLabel(source.getProjectStatus()));
            }
        }
        if (source.getDealSupplierId() == null || StrUtil.isBlank(source.getDealSupplierName()) || source.getDealAmount() == null) {
            throw exception(PROCUREMENT_CONTRACT_SOURCE_DEAL_REQUIRED);
        }
        return source;
    }

    private SrmProcurementContractDO validateContract(Long id) {
        SrmProcurementContractDO contract = contractMapper.selectById(id);
        if (contract == null || !Objects.equals(contract.getTenantId(), getRequiredTenantId())) {
            throw exception(PROCUREMENT_CONTRACT_NOT_EXISTS);
        }
        return contract;
    }

    private void restoreSource(SrmProcurementContractDO contract) {
        SrmSourcingProjectDO source = sourcingProjectMapper.selectById(contract.getSourceId());
        if (source == null || !Objects.equals(source.getTenantId(), getRequiredTenantId())) {
            throw exception(PROCUREMENT_CONTRACT_SOURCE_NOT_EXISTS);
        }
        if (source.getContractId() != null && !Objects.equals(source.getContractId(), contract.getId())) {
            throw exception(PROCUREMENT_CONTRACT_SOURCE_ALREADY_CONTRACTED);
        }
        String restoredStatus;
        if (SrmProcurementContractSourceTypeEnum.NON_BIDDING.getSourceType().equals(contract.getSourceType())) {
            restoredStatus = SrmSourcingProjectStatusEnum.DEAL_CONFIRMED.getStatus();
        } else if (SrmProcurementContractSourceTypeEnum.TENDER.getSourceType().equals(contract.getSourceType())) {
            restoredStatus = SrmSourcingProjectStatusEnum.WINNING_CONFIRMED.getStatus();
        } else {
            throw exception(PROCUREMENT_CONTRACT_SOURCE_TYPE_INVALID, contract.getSourceType());
        }
        // Explicit SET is required because entity update strategies skip null fields.
        sourcingProjectMapper.clearContractAndRestoreStatus(source.getId(), restoredStatus);
    }

    private SrmProcurementContractRespVO buildContractResp(Long contractId) {
        SrmProcurementContractDO contract = validateContract(contractId);
        SrmProcurementContractRespVO respVO = new SrmProcurementContractRespVO();
        respVO.setId(contract.getId());
        respVO.setContractNo(contract.getContractNo());
        respVO.setContractTitle(contract.getContractTitle());
        respVO.setSourceType(contract.getSourceType());
        respVO.setSourceTypeLabel(SrmProcurementContractSourceTypeEnum.getLabel(contract.getSourceType()));
        respVO.setSourceId(contract.getSourceId());
        respVO.setSourceNo(contract.getSourceNo());
        respVO.setSupplierId(contract.getSupplierId());
        respVO.setSupplierName(contract.getSupplierName());
        respVO.setContractAmount(contract.getContractAmount());
        respVO.setCurrency(contract.getCurrency());
        respVO.setEffectiveDate(contract.getEffectiveDate());
        respVO.setExpireDate(contract.getExpireDate());
        respVO.setContractStatus(contract.getContractStatus());
        respVO.setContractStatusLabel(SrmProcurementContractStatusEnum.getLabel(contract.getContractStatus()));
        respVO.setCreatedName(contract.getCreatedName());
        respVO.setCreatedTime(contract.getCreatedTime());
        respVO.setCancelledName(contract.getCancelledName());
        respVO.setCancelledTime(contract.getCancelledTime());
        respVO.setCancelReason(contract.getCancelReason());
        respVO.setPayments(paymentMapper.selectListByContractId(contract.getId()).stream()
                .map(this::buildPaymentResp)
                .collect(Collectors.toList()));
        respVO.setSignings(signingMapper.selectListByContractId(contract.getId()).stream()
                .map(this::buildSigningResp)
                .collect(Collectors.toList()));
        respVO.setAttachments(attachmentMapper.selectListByContractId(contract.getId()).stream()
                .map(this::buildAttachmentResp)
                .collect(Collectors.toList()));
        return respVO;
    }

    private SrmProcurementContractRespVO.Payment buildPaymentResp(SrmProcurementContractPaymentDO payment) {
        SrmProcurementContractRespVO.Payment respVO = new SrmProcurementContractRespVO.Payment();
        respVO.setId(payment.getId());
        respVO.setPaymentStage(payment.getPaymentStage());
        respVO.setPaymentRatio(payment.getPaymentRatio());
        respVO.setPaymentAmount(payment.getPaymentAmount());
        respVO.setDueDate(payment.getDueDate());
        respVO.setPaymentRemark(payment.getPaymentRemark());
        return respVO;
    }

    private SrmProcurementContractRespVO.Signing buildSigningResp(SrmProcurementContractSigningDO signing) {
        SrmProcurementContractRespVO.Signing respVO = new SrmProcurementContractRespVO.Signing();
        respVO.setId(signing.getId());
        respVO.setSigningParty(signing.getSigningParty());
        respVO.setSignerName(signing.getSignerName());
        respVO.setSigningDate(signing.getSigningDate());
        respVO.setSigningRemark(signing.getSigningRemark());
        return respVO;
    }

    private SrmProcurementContractRespVO.Attachment buildAttachmentResp(SrmProcurementContractAttachmentDO attachment) {
        SrmProcurementContractRespVO.Attachment respVO = new SrmProcurementContractRespVO.Attachment();
        respVO.setId(attachment.getId());
        respVO.setAttachmentName(attachment.getAttachmentName());
        respVO.setAttachmentUrl(attachment.getAttachmentUrl());
        respVO.setAttachmentType(attachment.getAttachmentType());
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
