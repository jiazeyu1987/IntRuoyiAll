package cn.iocoder.yudao.module.system.controller.admin.user.vo.profile;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class UserSignatureSettingsReqVO {
    @NotBlank @lombok.ToString.Exclude private String password;
    @NotBlank @Pattern(regexp="PIN|PASSWORD", message="签名方式只能选择 PIN 或密码") private String credentialType;
    @Pattern(regexp="[0-9]{6}", message="PIN 码必须为六位数字") @lombok.ToString.Exclude private String newPin;
}
