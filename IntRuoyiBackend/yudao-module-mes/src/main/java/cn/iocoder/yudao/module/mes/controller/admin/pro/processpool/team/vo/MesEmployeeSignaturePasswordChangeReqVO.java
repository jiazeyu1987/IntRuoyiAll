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
    @lombok.ToString.Exclude private String newPassword;
    @NotBlank @Pattern(regexp="PIN|PASSWORD") private String credentialType;
    @Pattern(regexp="[0-9]{6}",message="PIN 码必须为六位数字") @lombok.ToString.Exclude private String newPin;
}
