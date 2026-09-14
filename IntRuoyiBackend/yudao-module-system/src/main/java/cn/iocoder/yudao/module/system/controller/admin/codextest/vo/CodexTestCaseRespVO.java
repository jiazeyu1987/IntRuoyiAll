package cn.iocoder.yudao.module.system.controller.admin.codextest.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - Codex 测试项 Response VO")
@Data
public class CodexTestCaseRespVO {

    private Long id;
    private String name;
    private String project;
    private String nodeChainName;
    private Integer nodeChainSort;
    private String methodText;
    private String testDataText;
    private String analysisMode;
    private String defaultExecutionMode;
    private Boolean parallelSafe;
    private String status;
    private Integer sort;
    private Integer checkpointCount;
    private String lastExecutionStatus;
    private LocalDateTime lastExecutionTime;
    private LocalDateTime createTime;
    private List<Checkpoint> checkpoints;

    @Data
    public static class Checkpoint {
        private Long id;
        private Integer sort;
        private String name;
        private String expectedText;
        private String severity;
        private String remark;
    }

}
