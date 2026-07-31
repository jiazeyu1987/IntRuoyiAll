package cn.iocoder.yudao.module.mes.service.pro.processpool.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProcessPoolReviewCopyGenerateReqDTO {

    private Long eventId;
    private Long reviewerUserId;
    private Long reviewerSignatureId;
    private Long reviewerSignatureUserId;
    private String reviewerSignatureSnapshot;
    private List<MesProcessPoolReviewCopyFieldMappingDTO> fieldMappings;
}
