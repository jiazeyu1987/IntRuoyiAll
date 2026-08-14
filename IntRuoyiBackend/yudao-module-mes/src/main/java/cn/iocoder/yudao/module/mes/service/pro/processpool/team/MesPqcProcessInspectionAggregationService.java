package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

public interface MesPqcProcessInspectionAggregationService {

    void aggregateApprovedPqcSubmission(Long eventId, Long reviewId);
}
