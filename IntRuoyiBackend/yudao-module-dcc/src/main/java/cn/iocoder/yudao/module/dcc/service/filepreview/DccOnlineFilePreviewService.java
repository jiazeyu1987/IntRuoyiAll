package cn.iocoder.yudao.module.dcc.service.filepreview;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileBinary;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;

public interface DccOnlineFilePreviewService {

    DccControlledFilePreviewMetadataRespVO getPreviewMetadata(Long userId, Long fileId,
                                                              DccRequestAuditContext auditContext);

    DccControlledFileBinary readPreviewFile(Long userId, Long fileId, String viewerToken,
                                            String accessEventCode, String watermarkTraceCode,
                                            String viewerTokenId, String viewerTokenNonce,
                                            DccRequestAuditContext auditContext);

    DccControlledFileBinary readOnlyOfficePreviewFile(Long fileId, String token,
                                                      DccRequestAuditContext auditContext) throws Exception;
}
