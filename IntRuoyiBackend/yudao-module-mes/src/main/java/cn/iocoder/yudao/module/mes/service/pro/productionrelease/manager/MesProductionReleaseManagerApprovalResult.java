package cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProductionReleaseManagerApprovalResult {

    private MesProEdhrBatchExecutionDO batchExecution;
    private MesProEdhrReleaseTransactionDO releaseTransaction;
    private String applicationStatus;
    private Integer applicationVersion;
    private boolean replayed;
    private MesProcessPoolActiveOrderReleaseApplicationDO application;
    private MesProEdhrWorkTaskDO workTask;
    private String reportSnapshotHash;
    private String approvalPayloadHash;
    private java.time.LocalDateTime occurredAt;
}
