package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

public record MesProEdhrReleaseDossierRequirementState(
        boolean incomingInspectionReportRequired,
        boolean sterilizationReportRequired,
        boolean finishedProductInspectionReportRequired,
        boolean finishedProductInspectionRecordRequired,
        String configHash) {
}
