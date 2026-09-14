package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;

public interface MesOrderReleaseCompletenessService {

    MesOrderReleaseCompletenessCheck evaluateInspectionResult(MesProEdhrBatchExecutionDO batch);

    MesOrderReleaseCompletenessCheck evaluateDeviationClosed(MesProEdhrBatchExecutionDO batch);

    MesOrderReleaseCompletenessCheck evaluateReworkClosed(MesProEdhrBatchExecutionDO batch);

    MesOrderReleaseCompletenessCheck evaluateScrapRecorded(MesProEdhrBatchExecutionDO batch);

    MesOrderReleaseCompletenessCheck evaluateInventoryConsistency(MesProEdhrBatchExecutionDO batch);
}
