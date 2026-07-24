package cn.iocoder.yudao.module.system.controller.admin.codextest.vo;

import lombok.Data;

import java.util.List;

@Data
public class CodexTestRunnerClaimRespVO {

    private List<Task> tasks;

    @Data
    public static class Task {
        private Long executionId;
        private Long executionCaseId;
        private Long targetTenantId;
        private String executionMode;
        private String caseName;
        private String methodText;
        private String testDataText;
        private List<Checkpoint> checkpoints;
    }

    @Data
    public static class Checkpoint {
        private Integer sort;
        private String name;
        private String expectedText;
        private String severity;
    }

}
