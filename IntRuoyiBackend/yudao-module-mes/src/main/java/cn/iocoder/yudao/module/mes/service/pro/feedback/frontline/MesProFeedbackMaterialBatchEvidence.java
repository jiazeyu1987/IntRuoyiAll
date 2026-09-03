package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import java.math.BigDecimal;
import java.util.List;

public record MesProFeedbackMaterialBatchEvidence(String materialCode,
                                                   List<String> batchCodes,
                                                   BigDecimal requestedQuantity,
                                                   BigDecimal actualQuantity,
                                                   BigDecimal baseActualQuantity,
                                                   List<Long> pickListIds,
                                                   List<Long> pickListItemIds,
                                                   String sourceSnapshotHash) {
}
