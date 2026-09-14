package cn.iocoder.yudao.module.system.controller.admin.codextest.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CodexTestRunnerStatusRespVO {

    private Boolean online;
    private String status;
    private Integer onlineCount;
    private Integer staleRunnerCount;
    private Integer currentRunningCount;
    private Boolean requiredCapabilitiesPresent;
    private Long latestRunnerSessionId;
    private String latestRunnerName;
    private String latestRunnerStatus;
    private LocalDateTime lastHeartbeatTime;
    private Long heartbeatAgeSeconds;
    private Integer heartbeatTimeoutSeconds;
    private String message;

}
