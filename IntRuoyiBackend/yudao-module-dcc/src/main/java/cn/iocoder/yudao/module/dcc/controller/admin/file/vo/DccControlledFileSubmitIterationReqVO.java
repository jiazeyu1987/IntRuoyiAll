package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.time.LocalDate;

/** Request to submit an existing latest WORKING iteration for approval. */
@Data
public class DccControlledFileSubmitIterationReqVO {

    @NotBlank(message = "idempotencyKey is required")
    private String idempotencyKey;
    /** Required actual formal intent: PARTIAL or REPLACEMENT. */
    private String revisionChangeType;
    private String changeDescription;

    private Boolean needTraining;

    /** CC-2: this application's explicitly planned date, not the selected body's previous date. */
    @NotNull(message = "effectiveDate is required")
    private LocalDate effectiveDate;

    private List<Long> selectedSignoffUserIds;
    private List<Long> selectedSignoffDepartmentIds;
    private cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes projectAttributes;
}
