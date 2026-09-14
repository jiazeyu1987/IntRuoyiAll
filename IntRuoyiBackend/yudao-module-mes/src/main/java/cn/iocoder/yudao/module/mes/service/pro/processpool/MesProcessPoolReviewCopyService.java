package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolReviewCopyGenerateReqDTO;

public interface MesProcessPoolReviewCopyService {

    Long generateAndSubmitReviewCopy(MesProcessPoolReviewCopyGenerateReqDTO reqDTO);
}
