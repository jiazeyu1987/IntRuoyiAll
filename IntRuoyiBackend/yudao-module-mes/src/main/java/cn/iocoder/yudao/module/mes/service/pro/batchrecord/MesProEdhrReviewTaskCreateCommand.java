package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrReviewTaskCreateCommand {

    private String signatureCellKey;

    private Integer signatureRowIndex;

    private Integer signatureColumnIndex;

    private String reviewSourceType;

    private Long reviewSourceId;

    private String reviewSourceName;

    private String candidateSourceType;

    private Long candidateSourceId;

    private String candidateUserSnapshot;

    private Long assigneeUserId;

    private String bpmTaskId;
}
