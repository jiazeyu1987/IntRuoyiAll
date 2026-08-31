package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import java.util.List;

public interface MesProFeedbackMaterialBatchQueryService {

    List<String> listBatchCodes(Long workOrderId, String materialCode);
}
