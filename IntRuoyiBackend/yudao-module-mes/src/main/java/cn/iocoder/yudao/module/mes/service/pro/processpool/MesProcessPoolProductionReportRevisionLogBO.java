package cn.iocoder.yudao.module.mes.service.pro.processpool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProcessPoolProductionReportRevisionLogBO {

    private Long revisionId;
    private Long eventId;
    private String workOrderCode;
    private String workOrderName;
    private String processCode;
    private String processName;
    private String actualEmployeeName;
    private LocalDateTime submittedAt;
    private String modifiedByName;
    private LocalDateTime modifiedAt;
    private String changeReason;
    private Boolean signatureConfirmed;
    private Integer fieldCount;
    private String changeSummary;
    private List<FieldChange> changes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldChange {

        private String fieldName;
        private String beforeValue;
        private String afterValue;
    }
}
