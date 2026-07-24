package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProBatchRecordExecutionAttachmentChainVerifyResult {

    private boolean valid;

    private int checkedEventCount;

    private String headHash;

    @Builder.Default
    private List<Issue> issues = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Issue {

        private Long attachmentId;

        private String fieldPath;

        private String fieldKey;

        private String attachmentGroupKey;

        private Integer versionNo;

        private String issueCode;

        private String message;
    }
}
