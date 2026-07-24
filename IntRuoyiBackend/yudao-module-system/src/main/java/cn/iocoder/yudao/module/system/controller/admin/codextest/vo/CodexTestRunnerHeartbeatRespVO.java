package cn.iocoder.yudao.module.system.controller.admin.codextest.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CodexTestRunnerHeartbeatRespVO {

    private LocalDateTime serverTime;
    private List<Long> cancelExecutionCaseIds;

}
