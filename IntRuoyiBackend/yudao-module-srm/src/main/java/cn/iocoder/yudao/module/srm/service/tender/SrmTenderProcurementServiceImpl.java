package cn.iocoder.yudao.module.srm.service.tender;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.srm.controller.admin.supplieraccess.vo.SrmSupplierEligibilityRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.tender.vo.*;
import cn.iocoder.yudao.module.srm.dal.dataobject.procurement.SrmSourcingProjectDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.procurement.SrmSourcingProjectLineDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.tender.*;
import cn.iocoder.yudao.module.srm.dal.mysql.procurement.SrmSourcingProjectLineMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.procurement.SrmSourcingProjectMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.tender.*;
import cn.iocoder.yudao.module.srm.enums.coderule.SrmCodeRuleTargetFormEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmProcurementMethodEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmSourcingProjectStatusEnum;
import cn.iocoder.yudao.module.srm.enums.tender.*;
import cn.iocoder.yudao.module.srm.service.coderule.SrmCodeRuleService;
import cn.iocoder.yudao.module.srm.service.supplier.SrmSupplierAccessRiskService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.srm.enums.ErrorCodeConstants.*;

@Service
@Validated
public class SrmTenderProcurementServiceImpl implements SrmTenderProcurementService {

    @Resource
    private SrmCodeRuleService codeRuleService;
    @Resource
    private SrmSupplierAccessRiskService supplierAccessRiskService;
    @Resource
    private SrmSourcingProjectMapper sourcingProjectMapper;
    @Resource
    private SrmSourcingProjectLineMapper sourcingProjectLineMapper;
    @Resource
    private SrmTenderNoticeMapper tenderNoticeMapper;
    @Resource
    private SrmTenderDocumentMapper tenderDocumentMapper;
    @Resource
    private SrmTenderSubmissionMapper tenderSubmissionMapper;
    @Resource
    private SrmTenderExpertMapper tenderExpertMapper;
    @Resource
    private SrmTenderExpertApplicationMapper tenderExpertApplicationMapper;
    @Resource
    private SrmTenderCommitteeMemberMapper tenderCommitteeMemberMapper;
    @Resource
    private SrmTenderCandidateMapper tenderCandidateMapper;
    @Resource
    private SrmTenderWinningResultMapper tenderWinningResultMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SrmTenderProjectRespVO publishProject(SrmTenderPublishReqVO publishReqVO) {
        SrmSourcingProjectDO project = validateDraftTenderProject(publishReqVO.getProjectId());
        validatePublishRequest(publishReqVO);

        LocalDateTime now = LocalDateTime.now();
        project.setQuoteStartTime(publishReqVO.getSubmissionStartTime());
        project.setQuoteEndTime(publishReqVO.getSubmissionEndTime());
        project.setPublishAttachmentUrl(publishReqVO.getNoticeAttachmentUrl());
        project.setPublishedTime(now);
        project.setProjectStatus(SrmSourcingProjectStatusEnum.PUBLISHED.getStatus());
        sourcingProjectMapper.updateById(project);

        SrmTenderNoticeDO notice = SrmTenderNoticeDO.builder()
                .projectId(project.getId())
                .noticeTitle(publishReqVO.getNoticeTitle())
                .noticeAttachmentUrl(publishReqVO.getNoticeAttachmentUrl())
                .publishedTime(now)
                .build();
        notice.setTenantId(getRequiredTenantId());
        tenderNoticeMapper.insert(notice);

        SrmTenderDocumentDO document = SrmTenderDocumentDO.builder()
                .projectId(project.getId())
                .documentName(publishReqVO.getDocumentName())
                .documentAttachmentUrl(publishReqVO.getDocumentAttachmentUrl())
                .build();
        document.setTenantId(getRequiredTenantId());
        tenderDocumentMapper.insert(document);
        return buildProjectResp(project.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SrmTenderProjectRespVO submitBid(SrmTenderSubmissionReqVO submissionReqVO) {
        SrmSourcingProjectDO project = validatePublishedTenderProject(submissionReqVO.getProjectId());
        validateSubmissionWindow(project);
        if (submissionReqVO.getBidAmount() == null || submissionReqVO.getBidAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(TENDER_SUBMISSION_AMOUNT_INVALID);
        }
        if (tenderSubmissionMapper.selectByProjectIdAndSupplierId(project.getId(), submissionReqVO.getSupplierId()) != null) {
            throw exception(TENDER_SUBMISSION_SUPPLIER_DUPLICATE);
        }
        SrmSupplierEligibilityRespVO eligibility = supplierAccessRiskService.checkSupplierEligibility(submissionReqVO.getSupplierId());
        if (!Boolean.TRUE.equals(eligibility.getEligible())) {
            throw exception(SUPPLIER_ELIGIBILITY_BLOCKED, eligibility.getBlockedReason());
        }

        SrmTenderSubmissionDO submission = SrmTenderSubmissionDO.builder()
                .projectId(project.getId())
                .supplierId(eligibility.getSupplierId())
                .supplierName(eligibility.getSupplierName())
                .bidAmount(submissionReqVO.getBidAmount())
                .submissionStatus(SrmTenderSubmissionStatusEnum.SUBMITTED.getStatus())
                .attachmentUrl(submissionReqVO.getAttachmentUrl())
                .submittedBy(getRequiredLoginUserId())
                .submittedName(getRequiredLoginUserNickname())
                .submittedTime(LocalDateTime.now())
                .build();
        submission.setTenantId(getRequiredTenantId());
        tenderSubmissionMapper.insert(submission);
        return buildProjectResp(project.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createExpert(SrmTenderExpertSaveReqVO createReqVO) {
        SrmTenderExpertDO expert = SrmTenderExpertDO.builder()
                .expertName(createReqVO.getExpertName())
                .specialtyType(createReqVO.getSpecialtyType())
                .expertStatus(SrmTenderExpertStatusEnum.PENDING.getStatus())
                .build();
        expert.setTenantId(getRequiredTenantId());
        tenderExpertMapper.insert(expert);
        return expert.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveExpert(SrmTenderExpertAuditReqVO auditReqVO) {
        SrmTenderExpertDO expert = validateExpertExists(auditReqVO.getId());
        if (!SrmTenderExpertStatusEnum.PENDING.getStatus().equals(expert.getExpertStatus())) {
            throw exception(TENDER_EXPERT_STATUS_INVALID, SrmTenderExpertStatusEnum.getLabel(expert.getExpertStatus()));
        }
        expert.setExpertStatus(SrmTenderExpertStatusEnum.APPROVED.getStatus());
        expert.setAuditBy(getRequiredLoginUserId());
        expert.setAuditName(getRequiredLoginUserNickname());
        expert.setAuditTime(LocalDateTime.now());
        expert.setAuditRemark(auditReqVO.getAuditRemark());
        tenderExpertMapper.updateById(expert);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SrmTenderProjectRespVO formCommittee(SrmTenderCommitteeReqVO committeeReqVO) {
        SrmSourcingProjectDO project = validatePublishedTenderProject(committeeReqVO.getProjectId());
        List<Long> expertIds = distinctIds(committeeReqVO.getExpertIds());
        if (expertIds.size() != committeeReqVO.getExpertIds().size()) {
            throw exception(TENDER_COMMITTEE_MEMBER_DUPLICATE);
        }
        if (committeeReqVO.getRequiredExpertCount() == null || committeeReqVO.getRequiredExpertCount() <= 0
                || expertIds.size() < committeeReqVO.getRequiredExpertCount()) {
            throw exception(TENDER_COMMITTEE_MEMBER_INSUFFICIENT);
        }

        List<SrmTenderExpertDO> experts = new ArrayList<>();
        for (Long expertId : expertIds) {
            SrmTenderExpertDO expert = validateExpertExists(expertId);
            if (!SrmTenderExpertStatusEnum.APPROVED.getStatus().equals(expert.getExpertStatus())) {
                throw exception(TENDER_EXPERT_STATUS_INVALID, SrmTenderExpertStatusEnum.getLabel(expert.getExpertStatus()));
            }
            if (!Objects.equals(expert.getSpecialtyType(), committeeReqVO.getRequiredSpecialtyType())) {
                throw exception(TENDER_EXPERT_SPECIALTY_MISMATCH);
            }
            experts.add(expert);
        }

        SrmTenderExpertApplicationDO application = SrmTenderExpertApplicationDO.builder()
                .applicationNo(codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.EXPERT_DRAW_APPLICATION.getTargetForm()))
                .projectId(project.getId())
                .applicationMethod(committeeReqVO.getApplicationMethod())
                .requiredSpecialtyType(committeeReqVO.getRequiredSpecialtyType())
                .requiredExpertCount(committeeReqVO.getRequiredExpertCount())
                .appliedBy(getRequiredLoginUserId())
                .appliedName(getRequiredLoginUserNickname())
                .appliedTime(LocalDateTime.now())
                .build();
        application.setTenantId(getRequiredTenantId());
        tenderExpertApplicationMapper.insert(application);

        for (SrmTenderExpertDO expert : experts) {
            SrmTenderCommitteeMemberDO member = SrmTenderCommitteeMemberDO.builder()
                    .projectId(project.getId())
                    .applicationId(application.getId())
                    .expertId(expert.getId())
                    .expertName(expert.getExpertName())
                    .specialtyType(expert.getSpecialtyType())
                    .build();
            member.setTenantId(getRequiredTenantId());
            tenderCommitteeMemberMapper.insert(member);
        }

        project.setProjectStatus(SrmSourcingProjectStatusEnum.COMMITTEE_CONFIRMED.getStatus());
        sourcingProjectMapper.updateById(project);
        return buildProjectResp(project.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SrmTenderProjectRespVO createCandidates(SrmTenderCandidateReqVO candidateReqVO) {
        SrmSourcingProjectDO project = validateTenderProject(candidateReqVO.getProjectId());
        if (!SrmSourcingProjectStatusEnum.COMMITTEE_CONFIRMED.getStatus().equals(project.getProjectStatus())) {
            throw exception(TENDER_PROJECT_STATUS_INVALID, SrmSourcingProjectStatusEnum.getLabel(project.getProjectStatus()));
        }
        List<Long> submissionIds = distinctIds(candidateReqVO.getSubmissionIds());
        if (submissionIds.isEmpty()) {
            throw exception(TENDER_CANDIDATE_SUBMISSION_REQUIRED);
        }
        int rank = 1;
        for (Long submissionId : submissionIds) {
            SrmTenderSubmissionDO submission = tenderSubmissionMapper.selectById(submissionId);
            if (submission == null || !Objects.equals(submission.getProjectId(), project.getId())) {
                throw exception(TENDER_CANDIDATE_SUBMISSION_NOT_EXISTS);
            }
            if (tenderCandidateMapper.selectByProjectIdAndSubmissionId(project.getId(), submissionId) != null) {
                continue;
            }
            SrmTenderCandidateDO candidate = SrmTenderCandidateDO.builder()
                    .projectId(project.getId())
                    .submissionId(submission.getId())
                    .supplierId(submission.getSupplierId())
                    .supplierName(submission.getSupplierName())
                    .bidAmount(submission.getBidAmount())
                    .rankNo(rank++)
                    .candidateStatus(SrmTenderCandidateStatusEnum.CANDIDATE.getStatus())
                    .build();
            candidate.setTenantId(getRequiredTenantId());
            tenderCandidateMapper.insert(candidate);
        }
        project.setProjectStatus(SrmSourcingProjectStatusEnum.CANDIDATE_CONFIRMED.getStatus());
        sourcingProjectMapper.updateById(project);
        return buildProjectResp(project.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SrmTenderProjectRespVO confirmWinning(SrmTenderWinningReqVO winningReqVO) {
        if (StrUtil.isBlank(winningReqVO.getWinningRemark())) {
            throw exception(TENDER_WINNING_REMARK_REQUIRED);
        }
        SrmSourcingProjectDO project = validateTenderProject(winningReqVO.getProjectId());
        if (!SrmSourcingProjectStatusEnum.CANDIDATE_CONFIRMED.getStatus().equals(project.getProjectStatus())) {
            throw exception(TENDER_PROJECT_STATUS_INVALID, SrmSourcingProjectStatusEnum.getLabel(project.getProjectStatus()));
        }
        SrmTenderCandidateDO candidate = tenderCandidateMapper.selectById(winningReqVO.getCandidateId());
        if (candidate == null || !Objects.equals(candidate.getProjectId(), project.getId())) {
            throw exception(TENDER_WINNING_CANDIDATE_NOT_EXISTS);
        }

        candidate.setCandidateStatus(SrmTenderCandidateStatusEnum.WINNING.getStatus());
        tenderCandidateMapper.updateById(candidate);

        SrmTenderWinningResultDO result = SrmTenderWinningResultDO.builder()
                .projectId(project.getId())
                .candidateId(candidate.getId())
                .supplierId(candidate.getSupplierId())
                .supplierName(candidate.getSupplierName())
                .winningAmount(candidate.getBidAmount())
                .winningRemark(winningReqVO.getWinningRemark())
                .confirmedBy(getRequiredLoginUserId())
                .confirmedName(getRequiredLoginUserNickname())
                .confirmedTime(LocalDateTime.now())
                .build();
        result.setTenantId(getRequiredTenantId());
        tenderWinningResultMapper.insert(result);

        project.setProjectStatus(SrmSourcingProjectStatusEnum.WINNING_CONFIRMED.getStatus());
        project.setDealSupplierId(candidate.getSupplierId());
        project.setDealSupplierName(candidate.getSupplierName());
        project.setDealAmount(candidate.getBidAmount());
        project.setDealRemark(winningReqVO.getWinningRemark());
        project.setDealTime(result.getConfirmedTime());
        project.setContractId(null);
        sourcingProjectMapper.updateById(project);
        return buildProjectResp(project.getId());
    }

    @Override
    public SrmTenderProjectRespVO getProject(Long id) {
        return buildProjectResp(validateTenderProject(id).getId());
    }

    @Override
    public PageResult<SrmTenderProjectRespVO> getProjectPage(SrmTenderProjectPageReqVO pageReqVO) {
        PageResult<SrmSourcingProjectDO> pageResult = sourcingProjectMapper.selectTenderPage(pageReqVO);
        return new PageResult<>(pageResult.getList().stream()
                .map(project -> buildProjectResp(project.getId()))
                .collect(Collectors.toList()), pageResult.getTotal());
    }

    private SrmSourcingProjectDO validateTenderProject(Long id) {
        SrmSourcingProjectDO project = sourcingProjectMapper.selectById(id);
        if (project == null || !Objects.equals(project.getTenantId(), getRequiredTenantId())) {
            throw exception(TENDER_PROJECT_NOT_EXISTS);
        }
        if (!SrmProcurementMethodEnum.TENDER.getMethod().equals(project.getProjectType())) {
            throw exception(TENDER_PROJECT_TYPE_INVALID);
        }
        return project;
    }

    private SrmSourcingProjectDO validateDraftTenderProject(Long id) {
        SrmSourcingProjectDO project = validateTenderProject(id);
        if (!SrmSourcingProjectStatusEnum.DRAFT.getStatus().equals(project.getProjectStatus())) {
            throw exception(TENDER_PROJECT_STATUS_INVALID, SrmSourcingProjectStatusEnum.getLabel(project.getProjectStatus()));
        }
        return project;
    }

    private SrmSourcingProjectDO validatePublishedTenderProject(Long id) {
        SrmSourcingProjectDO project = validateTenderProject(id);
        if (!SrmSourcingProjectStatusEnum.PUBLISHED.getStatus().equals(project.getProjectStatus())) {
            throw exception(TENDER_PROJECT_STATUS_INVALID, SrmSourcingProjectStatusEnum.getLabel(project.getProjectStatus()));
        }
        return project;
    }

    private SrmTenderExpertDO validateExpertExists(Long id) {
        SrmTenderExpertDO expert = tenderExpertMapper.selectById(id);
        if (expert == null || !Objects.equals(expert.getTenantId(), getRequiredTenantId())) {
            throw exception(TENDER_EXPERT_NOT_EXISTS);
        }
        return expert;
    }

    private void validatePublishRequest(SrmTenderPublishReqVO reqVO) {
        if (StrUtil.isBlank(reqVO.getNoticeAttachmentUrl()) || StrUtil.isBlank(reqVO.getDocumentAttachmentUrl())) {
            throw exception(TENDER_PUBLISH_ATTACHMENT_REQUIRED);
        }
        if (reqVO.getSubmissionStartTime() == null || reqVO.getSubmissionEndTime() == null
                || !reqVO.getSubmissionEndTime().isAfter(reqVO.getSubmissionStartTime())) {
            throw exception(TENDER_SUBMISSION_WINDOW_INVALID);
        }
    }

    private void validateSubmissionWindow(SrmSourcingProjectDO project) {
        LocalDateTime now = LocalDateTime.now();
        if (project.getQuoteStartTime() == null || project.getQuoteEndTime() == null
                || now.isBefore(project.getQuoteStartTime()) || now.isAfter(project.getQuoteEndTime())) {
            throw exception(TENDER_SUBMISSION_WINDOW_CLOSED);
        }
    }

    private List<Long> distinctIds(List<Long> ids) {
        if (ids == null) {
            return Collections.emptyList();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private SrmTenderProjectRespVO buildProjectResp(Long projectId) {
        SrmSourcingProjectDO project = validateTenderProject(projectId);
        SrmTenderProjectRespVO respVO = new SrmTenderProjectRespVO();
        respVO.setId(project.getId());
        respVO.setProjectNo(project.getProjectNo());
        respVO.setProjectTitle(project.getProjectTitle());
        respVO.setProjectType(project.getProjectType());
        respVO.setProjectTypeLabel(SrmProcurementMethodEnum.getLabel(project.getProjectType()));
        respVO.setProjectStatus(project.getProjectStatus());
        respVO.setProjectStatusLabel(SrmSourcingProjectStatusEnum.getLabel(project.getProjectStatus()));
        respVO.setSourcePlanId(project.getSourcePlanId());
        respVO.setSourcePlanNo(project.getSourcePlanNo());
        respVO.setExpectedAmount(project.getExpectedAmount());
        respVO.setSubmissionStartTime(project.getQuoteStartTime());
        respVO.setSubmissionEndTime(project.getQuoteEndTime());
        respVO.setDealSupplierId(project.getDealSupplierId());
        respVO.setDealSupplierName(project.getDealSupplierName());
        respVO.setDealAmount(project.getDealAmount());
        respVO.setContractId(project.getContractId());
        respVO.setCreateTime(project.getCreateTime());
        respVO.setNotice(buildNoticeResp(tenderNoticeMapper.selectByProjectId(project.getId())));
        respVO.setDocument(buildDocumentResp(tenderDocumentMapper.selectByProjectId(project.getId())));
        respVO.setLines(sourcingProjectLineMapper.selectListByProjectId(project.getId()).stream()
                .map(this::buildLineResp)
                .collect(Collectors.toList()));
        respVO.setSubmissions(tenderSubmissionMapper.selectListByProjectId(project.getId()).stream()
                .map(this::buildSubmissionResp)
                .collect(Collectors.toList()));
        respVO.setCommitteeMembers(tenderCommitteeMemberMapper.selectListByProjectId(project.getId()).stream()
                .map(this::buildCommitteeMemberResp)
                .collect(Collectors.toList()));
        respVO.setCandidates(tenderCandidateMapper.selectListByProjectId(project.getId()).stream()
                .map(this::buildCandidateResp)
                .collect(Collectors.toList()));
        respVO.setWinningResult(buildWinningResultResp(tenderWinningResultMapper.selectByProjectId(project.getId())));
        return respVO;
    }

    private SrmTenderProjectRespVO.Line buildLineResp(SrmSourcingProjectLineDO line) {
        SrmTenderProjectRespVO.Line respVO = new SrmTenderProjectRespVO.Line();
        respVO.setId(line.getId());
        respVO.setSourcePlanLineId(line.getSourcePlanLineId());
        respVO.setLineNo(line.getLineNo());
        respVO.setMaterialId(line.getMaterialId());
        respVO.setMaterialCode(line.getMaterialCode());
        respVO.setMaterialName(line.getMaterialName());
        respVO.setQuantity(line.getQuantity());
        respVO.setUnit(line.getUnit());
        return respVO;
    }

    private SrmTenderProjectRespVO.Notice buildNoticeResp(SrmTenderNoticeDO notice) {
        if (notice == null) {
            return null;
        }
        SrmTenderProjectRespVO.Notice respVO = new SrmTenderProjectRespVO.Notice();
        respVO.setId(notice.getId());
        respVO.setNoticeTitle(notice.getNoticeTitle());
        respVO.setNoticeAttachmentUrl(notice.getNoticeAttachmentUrl());
        respVO.setPublishedTime(notice.getPublishedTime());
        return respVO;
    }

    private SrmTenderProjectRespVO.Document buildDocumentResp(SrmTenderDocumentDO document) {
        if (document == null) {
            return null;
        }
        SrmTenderProjectRespVO.Document respVO = new SrmTenderProjectRespVO.Document();
        respVO.setId(document.getId());
        respVO.setDocumentName(document.getDocumentName());
        respVO.setDocumentAttachmentUrl(document.getDocumentAttachmentUrl());
        return respVO;
    }

    private SrmTenderProjectRespVO.Submission buildSubmissionResp(SrmTenderSubmissionDO submission) {
        SrmTenderProjectRespVO.Submission respVO = new SrmTenderProjectRespVO.Submission();
        respVO.setId(submission.getId());
        respVO.setSupplierId(submission.getSupplierId());
        respVO.setSupplierName(submission.getSupplierName());
        respVO.setBidAmount(submission.getBidAmount());
        respVO.setSubmissionStatus(submission.getSubmissionStatus());
        respVO.setAttachmentUrl(submission.getAttachmentUrl());
        respVO.setSubmittedName(submission.getSubmittedName());
        respVO.setSubmittedTime(submission.getSubmittedTime());
        return respVO;
    }

    private SrmTenderProjectRespVO.CommitteeMember buildCommitteeMemberResp(SrmTenderCommitteeMemberDO member) {
        SrmTenderProjectRespVO.CommitteeMember respVO = new SrmTenderProjectRespVO.CommitteeMember();
        respVO.setId(member.getId());
        respVO.setApplicationId(member.getApplicationId());
        respVO.setExpertId(member.getExpertId());
        respVO.setExpertName(member.getExpertName());
        respVO.setSpecialtyType(member.getSpecialtyType());
        return respVO;
    }

    private SrmTenderProjectRespVO.Candidate buildCandidateResp(SrmTenderCandidateDO candidate) {
        SrmTenderProjectRespVO.Candidate respVO = new SrmTenderProjectRespVO.Candidate();
        respVO.setId(candidate.getId());
        respVO.setSubmissionId(candidate.getSubmissionId());
        respVO.setSupplierId(candidate.getSupplierId());
        respVO.setSupplierName(candidate.getSupplierName());
        respVO.setBidAmount(candidate.getBidAmount());
        respVO.setRankNo(candidate.getRankNo());
        respVO.setCandidateStatus(candidate.getCandidateStatus());
        return respVO;
    }

    private SrmTenderProjectRespVO.WinningResult buildWinningResultResp(SrmTenderWinningResultDO result) {
        if (result == null) {
            return null;
        }
        SrmTenderProjectRespVO.WinningResult respVO = new SrmTenderProjectRespVO.WinningResult();
        respVO.setId(result.getId());
        respVO.setCandidateId(result.getCandidateId());
        respVO.setSupplierId(result.getSupplierId());
        respVO.setSupplierName(result.getSupplierName());
        respVO.setWinningAmount(result.getWinningAmount());
        respVO.setWinningRemark(result.getWinningRemark());
        respVO.setConfirmedName(result.getConfirmedName());
        respVO.setConfirmedTime(result.getConfirmedTime());
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
