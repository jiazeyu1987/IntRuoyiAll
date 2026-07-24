package cn.iocoder.yudao.module.srm.service.tender;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.srm.controller.admin.tender.vo.*;

public interface SrmTenderProcurementService {

    SrmTenderProjectRespVO publishProject(SrmTenderPublishReqVO publishReqVO);

    SrmTenderProjectRespVO submitBid(SrmTenderSubmissionReqVO submissionReqVO);

    Long createExpert(SrmTenderExpertSaveReqVO createReqVO);

    void approveExpert(SrmTenderExpertAuditReqVO auditReqVO);

    SrmTenderProjectRespVO formCommittee(SrmTenderCommitteeReqVO committeeReqVO);

    SrmTenderProjectRespVO createCandidates(SrmTenderCandidateReqVO candidateReqVO);

    SrmTenderProjectRespVO confirmWinning(SrmTenderWinningReqVO winningReqVO);

    SrmTenderProjectRespVO getProject(Long id);

    PageResult<SrmTenderProjectRespVO> getProjectPage(SrmTenderProjectPageReqVO pageReqVO);
}
