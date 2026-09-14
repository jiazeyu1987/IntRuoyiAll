package cn.iocoder.yudao.module.dcc.registrationcertificate.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileBinary;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;

public interface DccRegistrationCertificateFilePreviewService {

    DccControlledFilePreviewMetadataRespVO getPreviewMetadata(Long tenantId, Long userId, Long businessFileId,
                                                              DccRequestAuditContext auditContext);

    DccControlledFileBinary readPreviewFile(Long tenantId, Long userId, Long businessFileId, String viewerToken,
                                            String accessEventCode, String watermarkTraceCode,
                                            String viewerTokenId, String viewerTokenNonce,
                                            DccRequestAuditContext auditContext);
}