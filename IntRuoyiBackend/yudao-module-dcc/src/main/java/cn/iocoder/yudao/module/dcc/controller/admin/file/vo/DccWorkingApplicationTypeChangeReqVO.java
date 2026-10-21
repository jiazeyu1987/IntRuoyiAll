package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

/** Explicit NEW rejected-working conversion; preview never persists this command. */
@Data
public class DccWorkingApplicationTypeChangeReqVO {
    @NotNull @Positive private Long targetFileTypeTaxonomyId;
    @Positive private Integer expectedSourceAttributeRound;
    @Positive private Long expectedSourceCategoryId;
    @Positive private Long expectedSourceTaxonomyId;
    private String expectedSourceSha256;
    private String expectedSourceMatrixDigest;
    private Long expectedApprovalRouteId;
    private Integer expectedApprovalRouteVersionNo;
    private String expectedApprovalMatrixDigest;
    private List<@NotNull @Positive Long> selectedSignoffDepartmentIds;
    private List<@NotNull @Positive Long> selectedDistributionDepartmentIds;
    @Size(max=64) private String requestId;
    @Size(max=512) private String changeReason;
    private Boolean confirmed;
}
