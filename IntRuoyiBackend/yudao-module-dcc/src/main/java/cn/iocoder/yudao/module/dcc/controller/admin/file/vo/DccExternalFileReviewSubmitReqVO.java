package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class DccExternalFileReviewSubmitReqVO extends DccControlledFileSubmitReqVO {

    @NotBlank(message = "externalSource is required")
    private String externalSource;

    @NotBlank(message = "externalOwner is required")
    private String externalOwner;

    @NotBlank(message = "reviewReason is required")
    private String reviewReason;

    @NotEmpty(message = "participantUserIds is required")
    private List<Long> participantUserIds;
}
