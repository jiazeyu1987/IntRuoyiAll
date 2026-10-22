package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DccSignoffAssignmentReqVO {
    @NotBlank private String taskId;
    @NotNull private Long assigneeUserId;
    @NotBlank private String password;
    @NotBlank private String reason;
    /** Selected related-file responsibilities, persisted inside this signed assignment transaction. */
    private java.util.List<cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.Arrangement> relationArrangements;
    /** New fixed versions appended in this signed assignment; never replaces an earlier stage's snapshot. */
    private java.util.List<@NotNull @jakarta.validation.constraints.Positive Long> supplementalRelatedControlledFileIds;
}
