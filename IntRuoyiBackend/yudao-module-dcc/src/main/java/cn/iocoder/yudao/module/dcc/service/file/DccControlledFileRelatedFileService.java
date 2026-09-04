package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRelatedFileRespVO;

import java.util.List;

public interface DccControlledFileRelatedFileService {

    void validateAndBindRelatedFiles(Long controlledFileId, Long projectCodeId, List<Long> relatedControlledFileIds);

    List<DccControlledFileRelatedFileRespVO> listRelatedFiles(Long controlledFileId);

}
