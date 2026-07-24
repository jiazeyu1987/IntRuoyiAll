package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileProjectCodeRecognitionRespVO;

public interface DccControlledFileProjectCodeRecognitionService {

    DccControlledFileProjectCodeRecognitionRespVO recognizeProjectCode(Long userId, Long id);

    DccControlledFileProjectCodeRecognitionRespVO recognizeProjectCode(Long userId, Long id, Long claimTaskId);
}
