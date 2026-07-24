package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrFlowInterventionAdminReqVO {

    @NotBlank(message = "业务对象类型不能为空")
    private String businessObjectType;

    @NotBlank(message = "业务对象不能为空")
    private String businessObjectId;

    private String businessObjectCode;

    private String flowInstanceId;

    private String taskId;

    private String nodeKey;

    @NotBlank(message = "原状态不能为空")
    private String fromStatus;

    @NotBlank(message = "目标状态不能为空")
    private String toStatus;

    private String targetTaskId;

    private Long targetUserId;

    private String reasonCategory;

    @NotBlank(message = "管理员干预原因不能为空")
    private String reason;

    @NotBlank(message = "授权依据不能为空")
    private String authorizationBasis;

    @NotBlank(message = "签核证据不能为空")
    private String signoffEvidenceHash;

    @NotBlank(message = "幂等键不能为空")
    private String idempotencyKey;

    private String interventionSource;
}
