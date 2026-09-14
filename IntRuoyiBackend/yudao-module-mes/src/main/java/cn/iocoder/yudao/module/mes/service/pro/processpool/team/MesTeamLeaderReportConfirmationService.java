package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

public interface MesTeamLeaderReportConfirmationService {

    MesTeamLeaderReportAllocationPreview previewFifoAllocation(MesTeamLeaderReportAllocationPreviewReqBO reqBO);

    Long confirmSubmission(MesTeamLeaderReportConfirmationReqBO reqBO);
}
