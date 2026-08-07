package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolProductionReportRevisionLogBO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProProductionReportRevisionLogPageRespVO {

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

    public static MesProProductionReportRevisionLogPageRespVO from(
            MesProcessPoolProductionReportRevisionLogBO source) {
        return new MesProProductionReportRevisionLogPageRespVO()
                .setRevisionId(source.getRevisionId())
                .setEventId(source.getEventId())
                .setWorkOrderCode(source.getWorkOrderCode())
                .setWorkOrderName(source.getWorkOrderName())
                .setProcessCode(source.getProcessCode())
                .setProcessName(source.getProcessName())
                .setActualEmployeeName(source.getActualEmployeeName())
                .setSubmittedAt(source.getSubmittedAt())
                .setModifiedByName(source.getModifiedByName())
                .setModifiedAt(source.getModifiedAt())
                .setChangeReason(source.getChangeReason())
                .setSignatureConfirmed(source.getSignatureConfirmed())
                .setFieldCount(source.getFieldCount())
                .setChangeSummary(source.getChangeSummary());
    }
}
