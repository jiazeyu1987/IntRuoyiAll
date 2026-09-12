package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/** Request to submit an existing latest WORKING iteration for approval. */
@Data
public class DccControlledFileSubmitIterationReqVO {

    @NotBlank(message = "idempotencyKey is required")
    private String idempotencyKey;

    private List<Long> selectedSignoffUserIds;
}
