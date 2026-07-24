package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadPreviewReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadRespVO;

public interface DccControlledFileUploadService {

    DccControlledFileUploadRespVO uploadPreviewFile(Long userId, DccControlledFileUploadPreviewReqVO reqVO,
                                                    DccRequestAuditContext auditContext) throws Exception;

    DccControlledFileBinary readUploadPreviewOnlyOfficeFile(Long fileId, String token) throws Exception;
}
