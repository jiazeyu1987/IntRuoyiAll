package cn.iocoder.yudao.module.dcc.controller.admin.signature.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - DCC电子签名授权 Response VO")
@Data
public class DccSignatureAuthorizationRespVO {

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
    private Long userId;

    @Schema(description = "用户账号", example = "zhangsan")
    private String username;

    @Schema(description = "用户昵称", example = "张三")
    private String nickname;

    @Schema(description = "用户显示名", example = "张三")
    private String userName;

    @Schema(description = "部门名称", example = "质量部")
    private String deptName;

    @Schema(description = "手机号", example = "18888888888")
    private String mobile;

    @Schema(description = "用户状态", example = "0")
    private Integer status;

    @Schema(description = "最近登录时间")
    private LocalDateTime loginDate;

    @Schema(description = "是否开通电子签名授权", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean electronicSignatureEnabled;

    @Schema(description = "授权状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "ENABLED")
    private String authorizationState;

    @Schema(description = "是否锁定", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private Boolean locked;

    @Schema(description = "锁定截止时间")
    private LocalDateTime lockedUntil;

    @Schema(description = "最近授权审计原因", example = "完成岗位电子签名授权")
    private String latestAuditReason;

    @Schema(description = "最近授权审计时间")
    private LocalDateTime latestAuditAt;

    @Schema(description = "最近授权审计操作人 ID", example = "1")
    private Long latestAuditOperatorId;

    @Schema(description = "最近授权审计操作人名称", example = "系统管理员")
    private String latestAuditOperatorName;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getLoginDate() {
        return loginDate;
    }

    public void setLoginDate(LocalDateTime loginDate) {
        this.loginDate = loginDate;
    }

    public Boolean getElectronicSignatureEnabled() {
        return electronicSignatureEnabled;
    }

    public void setElectronicSignatureEnabled(Boolean electronicSignatureEnabled) {
        this.electronicSignatureEnabled = electronicSignatureEnabled;
    }

    public String getAuthorizationState() {
        return authorizationState;
    }

    public void setAuthorizationState(String authorizationState) {
        this.authorizationState = authorizationState;
    }

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public String getLatestAuditReason() {
        return latestAuditReason;
    }

    public void setLatestAuditReason(String latestAuditReason) {
        this.latestAuditReason = latestAuditReason;
    }

    public LocalDateTime getLatestAuditAt() {
        return latestAuditAt;
    }

    public void setLatestAuditAt(LocalDateTime latestAuditAt) {
        this.latestAuditAt = latestAuditAt;
    }

    public Long getLatestAuditOperatorId() {
        return latestAuditOperatorId;
    }

    public void setLatestAuditOperatorId(Long latestAuditOperatorId) {
        this.latestAuditOperatorId = latestAuditOperatorId;
    }

    public String getLatestAuditOperatorName() {
        return latestAuditOperatorName;
    }

    public void setLatestAuditOperatorName(String latestAuditOperatorName) {
        this.latestAuditOperatorName = latestAuditOperatorName;
    }
}
