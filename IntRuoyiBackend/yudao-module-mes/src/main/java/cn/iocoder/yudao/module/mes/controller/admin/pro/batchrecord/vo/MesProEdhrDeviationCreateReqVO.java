package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MesProEdhrDeviationCreateReqVO {

    /** Production-order selector used by deviation administrators. */
    @jakarta.validation.constraints.Positive
    private Long workOrderId;

    // Supplied by the order-scoped entry, never used to infer a batch.
    private Long activeOrderId;

    private Long batchExecutionId;

    @NotBlank
    @Pattern(regexp = "NORMAL|CRITICAL")
    private String level;

    private Long discoveryDepartmentId;
    @Size(max = 128)
    private String discoveryDepartmentName;
    private Long discovererId;
    @Size(max = 128)
    private String discovererName;
    private LocalDateTime discoveredAt;
    @Size(max = 255)
    private String discoveryLocation;
    @Size(max = 255)
    private String productName;
    @Size(max = 512)
    private String productSpecification;
    @Size(max = 255)
    private String equipmentOrSystem;
    private LocalDateTime reportedAt;
    /** Required only for the production leader entry; standalone creation assigns the initiator. */
    @jakarta.validation.constraints.Positive
    private Long receiverId;
    @Size(max = 128)
    private String receiverName;
    @Size(max = 8)
    private List<@NotBlank String> categoryCodes;
    @NotBlank
    @Size(max = 4000)
    private String description;
    @Size(max = 4000)
    private String emergencyAction;
    @NotBlank
    @Size(max = 4000)
    private String levelBasis;
    @NotBlank
    @Size(max = 128)
    private String idempotencyKey;

    /** Re-authenticated electronic-signature credential. It must never be persisted or included in payload hashes. */
    @ToString.Exclude
    private String signaturePassword;
}
