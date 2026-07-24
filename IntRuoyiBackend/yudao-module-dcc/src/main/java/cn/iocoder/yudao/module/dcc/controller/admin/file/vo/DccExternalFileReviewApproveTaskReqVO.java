package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DccExternalFileReviewApproveTaskReqVO extends DccControlledFileApproveTaskReqVO {

    private String reviewConclusion;

    private String conclusionComment;

    private String outputUploadTicket;

    @JsonIgnore
    @Schema(hidden = true)
    private Long outputFileId;
}
