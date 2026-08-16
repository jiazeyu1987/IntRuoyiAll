package cn.iocoder.yudao.module.mes.service.pro.productionrelease.report;

public interface MesProductionReleaseReportNodePort {

    MesProductionReleaseReportAttachmentPrepareResult prepareAttachment(
            MesProductionReleaseReportAttachmentPreparePortCommand command);

    MesProductionReleaseReportNodeEvidence complete(MesProductionReleaseReportNodePortCommand command);
}
