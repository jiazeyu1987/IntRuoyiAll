package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class MesEmployeeSignaturePasswordChangeReqVO {
    @NotNull @Positive private Long employeeProfileId;
    @NotNull @Positive private Long activeOrderId;
    @NotNull @Positive private Long routeId;
    @NotNull @Positive private Long routeProcessId;
    @NotNull @Positive private Long processId;
    @NotBlank private String identityDomain;
    @NotBlank @lombok.ToString.Exclude private String currentPassword;
    @NotBlank @lombok.ToString.Exclude private String newPassword;
}
