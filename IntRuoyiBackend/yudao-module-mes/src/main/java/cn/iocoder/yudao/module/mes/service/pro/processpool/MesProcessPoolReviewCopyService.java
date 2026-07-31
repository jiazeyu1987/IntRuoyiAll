package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolReviewCopyGenerateReqDTO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolReviewCopyGenerateFromRulesReqDTO;

public interface MesProcessPoolReviewCopyService {

    Long generateAndSubmitReviewCopy(MesProcessPoolReviewCopyGenerateReqDTO reqDTO);

    Long generateAndSubmitReviewCopyFromRules(MesProcessPoolReviewCopyGenerateFromRulesReqDTO reqDTO);
}
