package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolProductionReportRevisionLogBO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class MesProProductionReportRevisionLogDetailRespVO
        extends MesProProductionReportRevisionLogPageRespVO {

    private List<FieldChangeRespVO> changes;

    public static MesProProductionReportRevisionLogDetailRespVO from(
            MesProcessPoolProductionReportRevisionLogBO source) {
        MesProProductionReportRevisionLogDetailRespVO target =
                new MesProProductionReportRevisionLogDetailRespVO()
                        .setChanges(source.getChanges().stream()
                                .map(FieldChangeRespVO::from)
                                .toList());
        MesProProductionReportRevisionLogPageRespVO page = MesProProductionReportRevisionLogPageRespVO.from(source);
        target.setRevisionId(page.getRevisionId())
                .setEventId(page.getEventId())
                .setWorkOrderCode(page.getWorkOrderCode())
                .setWorkOrderName(page.getWorkOrderName())
                .setProcessCode(page.getProcessCode())
                .setProcessName(page.getProcessName())
                .setActualEmployeeName(page.getActualEmployeeName())
                .setSubmittedAt(page.getSubmittedAt())
                .setModifiedByName(page.getModifiedByName())
                .setModifiedAt(page.getModifiedAt())
                .setChangeReason(page.getChangeReason())
                .setSignatureConfirmed(page.getSignatureConfirmed())
                .setFieldCount(page.getFieldCount())
                .setChangeSummary(page.getChangeSummary());
        return target;
    }

    @Data
    @Accessors(chain = true)
    public static class FieldChangeRespVO {

        private String fieldName;
        private String beforeValue;
        private String afterValue;

        public static FieldChangeRespVO from(MesProcessPoolProductionReportRevisionLogBO.FieldChange source) {
            return new FieldChangeRespVO()
                    .setFieldName(source.getFieldName())
                    .setBeforeValue(source.getBeforeValue())
                    .setAfterValue(source.getAfterValue());
        }
    }
}
