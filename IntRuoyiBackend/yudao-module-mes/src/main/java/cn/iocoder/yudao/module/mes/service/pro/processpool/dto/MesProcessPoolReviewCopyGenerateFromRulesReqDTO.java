package cn.iocoder.yudao.module.mes.service.pro.processpool.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MesProcessPoolReviewCopyGenerateFromRulesReqDTO {

    private Long eventId;
    private Long reviewerUserId;
    private Long reviewerSignatureId;
    private Long reviewerSignatureUserId;
    private String reviewerSignatureSnapshot;
}
