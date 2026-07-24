package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePublishReqVO;

public interface DccControlledFilePublishService {

    FormInstanceRespVO publishControlledFile(Long userId, Long id, DccControlledFilePublishReqVO reqVO);
}
