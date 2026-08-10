package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderReleaseLossSourceReadResult {

    private List<ProcessLossSource> processSources;
    private List<MesTeamLeaderActiveOrderReleaseBlocker> blockers;

    @Data
    @Accessors(chain = true)
    public static class ProcessLossSource {

        private MesProcessPoolActiveOrderProcessSnapshotDO snapshot;
        private MesProFeedbackDO feedback;
        private MesProProcessPoolEventDO event;
        private MesProcessPoolReportAllocationDO allocation;
        private MesProcessPoolSubmissionReviewDO review;
        private List<LossDetail> lossDetails;
    }

    @Data
    @Accessors(chain = true)
    public static class LossDetail {

        private Long reasonId;
        private String reasonCode;
        private String reasonName;
        private BigDecimal quantity;
    }
}
