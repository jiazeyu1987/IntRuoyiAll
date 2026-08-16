package cn.iocoder.yudao.module.mes.service.pro.productionrelease.report;

public interface MesProductionReleaseReportService {

    MesProductionReleaseReportAttachmentPrepareResult prepareAttachment(
            Long actorUserId, MesProductionReleaseReportAttachmentPrepareCommand command);

    MesProductionReleaseReportNodeCompleteResult complete(
            Long actorUserId, MesProductionReleaseReportNodeCompleteCommand command);
}
