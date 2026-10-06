package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCheckoutReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCheckinReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCancelCheckoutReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileAccessExplanationRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadDirectoryTreeRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadNameOptionRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.service.download.DccDownloadFileBinary;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessOperation;

import java.util.List;

public interface DccControlledFileQueryService {
    /** Internal read authority for a checked-out, truly cancelled native revision's source upload. */
    void assertCancelledRevisionCheckinUpload(Long userId, Long fileId, Long categoryId);
    List<DccApplicationRoundSummary> listApplicationRounds(Long userId, Long controlledFileId);
    DccFileRelationPermissions getRelationPermissions(Long userId, Long controlledFileId);
    DccControlledFileApplicationEvidence getApplicationEvidence(Long userId, Long controlledFileId,
                                                               String applicationType, String bpmRound);
    DccControlledFileRevisionOptions getRevisionOptions(Long userId, Long controlledBaselineId);
    PageResult<DccControlledFileSelectorRow> getControlledFileSelectorPage(Long userId, DccControlledFileSelectorQuery query);
    void assertRelationNameVisible(Long userId, Long id);
    void assertRelationContentReadable(Long userId, Long id);
    void assertRelationEditable(Long userId, Long id);

    PageResult<DccControlledFileRespVO> getControlledFilePage(Long userId, DccControlledFilePageReqVO reqVO);

    PageResult<DccControlledFileRespVO> getControlledFileBrowserPage(Long userId, DccControlledFilePageReqVO reqVO);

    List<DccControlledFileDO> listControlledFileBrowserCandidates(Long userId, DccControlledFilePageReqVO reqVO);

    DccControlledFileRespVO getControlledFile(Long userId, Long id);

    boolean canViewFileName(Long userId, DccControlledFileDO file);

    DccControlledFileRespVO checkoutControlledFile(Long userId, Long id);

    DccControlledFileRespVO checkoutControlledFile(Long userId, Long id,
                                                   DccControlledFileCheckoutReqVO reqVO);

    DccControlledFileRespVO checkinControlledFile(Long userId, Long id);

    DccControlledFileRespVO checkinControlledFile(Long userId, Long id,
                                                  DccControlledFileCheckinReqVO reqVO);

    DccControlledFileRespVO cancelCheckoutControlledFile(Long userId, Long id,
                                                         DccControlledFileCancelCheckoutReqVO reqVO);

    DccControlledFileAccessExplanationRespVO explainControlledFileAccess(Long userId, Long id);

    DccControlledFileUploadDirectoryTreeRespVO getUploadDirectoryTree(Long categoryId);

    List<DccControlledFileUploadNameOptionRespVO> listUploadNameOptions(Long dccProjectCodeId,
                                                                        Long fileTypeTaxonomyId);

    DccControlledFilePreviewMetadataRespVO getPreviewMetadata(Long userId, Long id,
                                                              DccRequestAuditContext auditContext);

    DccControlledFilePreviewMetadataRespVO getAttachmentPreviewMetadata(Long userId, Long id, Long attachmentId,
                                                                        DccRequestAuditContext auditContext);

    DccControlledFileBinary readPreviewFile(Long userId, Long id, String viewerToken, String accessEventCode,
                                            String watermarkTraceCode, String viewerTokenId,
                                            String viewerTokenNonce, DccRequestAuditContext auditContext);

    DccControlledFileBinary readAttachmentPreviewFile(Long userId, Long id, Long attachmentId, String viewerToken,
                                                      String accessEventCode, String watermarkTraceCode,
                                                      String viewerTokenId, String viewerTokenNonce,
                                                      DccRequestAuditContext auditContext);

    DccDownloadFileBinary readDownloadFile(Long userId, Long id, Boolean nonControlledWarningConfirmed,
                                           String downloadRequestId, DccRequestAuditContext auditContext);

    DccControlledFileBinary readOnlyOfficePreviewFile(Long id, String token,
                                                      DccRequestAuditContext auditContext) throws Exception;

    DccControlledFileScope identifyControlledFileScope(Long infraFileId);

    void assertBusinessFileAccess(Long userId, Long controlledFileId, BusinessFileAccessOperation operation,
                                  DccRequestAuditContext auditContext);
}
