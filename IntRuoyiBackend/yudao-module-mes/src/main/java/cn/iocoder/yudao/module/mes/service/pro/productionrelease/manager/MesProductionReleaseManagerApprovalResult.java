package cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
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
}
