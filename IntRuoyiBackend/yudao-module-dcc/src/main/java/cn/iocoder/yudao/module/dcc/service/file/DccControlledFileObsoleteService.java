package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteReqVO;

public interface DccControlledFileObsoleteService {

    void precheckObsoleteControlledFile(Long userId, Long id, DccControlledFileObsoleteReqVO reqVO);

    FormInstanceRespVO obsoleteControlledFile(Long userId, Long id, DccControlledFileObsoleteReqVO reqVO);

    void applyApprovedObsoleteControlledFile(Long userId, Long id, DccControlledFileObsoleteReqVO reqVO);
}
