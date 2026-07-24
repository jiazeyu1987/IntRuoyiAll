package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileMetadataUpdateReqVO;

public interface DccControlledFileMetadataUpdateService {

    String DOC_CONTROL_ROLE_CODE = "doc_control";

    void updateMetadata(Long userId, Long id, DccControlledFileMetadataUpdateReqVO reqVO);

}
