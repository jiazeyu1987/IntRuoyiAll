package cn.iocoder.yudao.module.system.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

@Schema(description = "管理后台 - 登录前强制修改密码 Request VO")
public class AuthPreLoginPasswordChangeReqVO {

    @Schema(description = "账号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "登录账号不能为空")
    @Length(min = 4, max = 30, message = "账号长度为 4-30 位")
    @Pattern(regexp = "^[a-zA-Z0-9]{4,30}$", message = "账号格式为数字以及字母")
    private String username;

    @Schema(description = "当前密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "当前密码不能为空")
    private String oldPassword;

    @Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "新密码不能为空")
    private String newPassword;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
