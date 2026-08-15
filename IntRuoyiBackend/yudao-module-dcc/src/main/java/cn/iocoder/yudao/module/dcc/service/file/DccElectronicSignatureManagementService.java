package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSignatureExportSummaryRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationAuditPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationAuditRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureImageRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignaturePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccSignatureAuthorizationRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccSignatureEvidenceRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccSignatureVerifyRespVO;
import org.springframework.web.multipart.MultipartFile;

public interface DccElectronicSignatureManagementService {

    PageResult<DccElectronicSignatureRespVO> getSignaturePage(DccElectronicSignaturePageReqVO reqVO);

    PageResult<DccElectronicSignatureAuthorizationRespVO> getAuthorizationPage(
            DccElectronicSignatureAuthorizationPageReqVO reqVO);

    DccSignatureAuthorizationRespVO updateAuthorization(Long userId, Boolean enabled, Long operatorId, String reason);

    DccSignatureAuthorizationRespVO unlockAuthorization(Long userId, Long operatorId, String reason);

    PageResult<DccElectronicSignatureAuthorizationAuditRespVO> getAuthorizationAuditPage(
            Long userId, DccElectronicSignatureAuthorizationAuditPageReqVO reqVO);

    DccElectronicSignatureImageRespVO getMySignatureImage(Long userId);

    DccElectronicSignatureImageRespVO uploadMySignatureImage(Long userId, MultipartFile file, Long operatorId, String reason);

    DccElectronicSignatureImageRespVO enableMySignatureImage(Long userId, Long imageId, Long operatorId, String reason);

    DccElectronicSignatureImageRespVO disableMySignatureImage(Long userId, Long operatorId, String reason);

    DccSignatureEvidenceRespVO getSignatureEvidenceDetail(Long signatureId);

    DccSignatureVerifyRespVO verifySignatureEvidence(Long signatureId);

    DccControlledFileSignatureExportSummaryRespVO getSignatureExportSummary(Long controlledFileId);

    DccControlledFileSignatureExportSummaryRespVO migratePublishedCopyBindings(
            Long controlledFileId, Long operatorUserId, String requestId);

    DccControlledFileSignatureExportSummaryRespVO reissuePublishedSignatureEvidence(
            Long controlledFileId, Long operatorUserId, String requestId, String reason);

    DccSignatureEvidenceExportArtifact exportSignatureEvidence(Long controlledFileId);
}
