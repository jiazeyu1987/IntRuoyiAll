package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RuntimeControlStatusResult {

    private String status;
    private String httpStatus;
    private String runtimeState;
    private String blockedReason;
    private String worktree;
    private Integer frontendPort;
    private Integer backendPort;
    private String currentReleaseTag;

    public RuntimeControlStatusResult(String status, String httpStatus, String runtimeState, String blockedReason) {
        this.status = status;
        this.httpStatus = httpStatus;
        this.runtimeState = runtimeState;
        this.blockedReason = blockedReason;
    }

    public static RuntimeControlStatusResult running(String httpStatus, String runtimeState) {
        return new RuntimeControlStatusResult("running", httpStatus, runtimeState, null);
    }

    public static RuntimeControlStatusResult error(String message) {
        return new RuntimeControlStatusResult("error", "ERROR", "unknown", message);
    }
}
