package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO;
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

    PageResult<DccControlledFileRespVO> getControlledFilePage(Long userId, DccControlledFilePageReqVO reqVO);

    PageResult<DccControlledFileRespVO> getControlledFileBrowserPage(Long userId, DccControlledFilePageReqVO reqVO);

    List<DccControlledFileDO> listControlledFileBrowserCandidates(Long userId, DccControlledFilePageReqVO reqVO);

    DccControlledFileRespVO getControlledFile(Long userId, Long id);

    DccControlledFileAccessExplanationRespVO explainControlledFileAccess(Long userId, Long id);

    DccControlledFileUploadDirectoryTreeRespVO getUploadDirectoryTree(Long categoryId);

    List<DccControlledFileUploadNameOptionRespVO> listUploadNameOptions(Long dccProjectCodeId,
                                                                        Long fileTypeTaxonomyId);

    DccControlledFilePreviewMetadataRespVO getPreviewMetadata(Long userId, Long id,
                                                              DccRequestAuditContext auditContext);

    DccControlledFileBinary readPreviewFile(Long userId, Long id, String viewerToken, String accessEventCode,
                                            String watermarkTraceCode, String viewerTokenId,
                                            String viewerTokenNonce, DccRequestAuditContext auditContext);

    DccDownloadFileBinary readDownloadFile(Long userId, Long id, Boolean nonControlledWarningConfirmed,
                                           String downloadRequestId, DccRequestAuditContext auditContext);

    DccControlledFileBinary readOnlyOfficePreviewFile(Long id, String token,
                                                      DccRequestAuditContext auditContext) throws Exception;

    DccControlledFileScope identifyControlledFileScope(Long infraFileId);

    void assertBusinessFileAccess(Long userId, Long controlledFileId, BusinessFileAccessOperation operation,
                                  DccRequestAuditContext auditContext);
}
