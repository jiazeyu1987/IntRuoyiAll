package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceabilityRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceSourcePrecheckRespVO;

import java.util.List;

public interface MesProEdhrBatchTraceabilityService {

    MesProEdhrBatchTraceabilityRespVO capture(MesProEdhrBatchTraceCaptureCommand command);

    MesProEdhrBatchTraceabilityRespVO getTraceability(Long batchExecutionId);

    List<MesProEdhrBatchTraceabilityRespVO> listTraceability(Long activeOrderId, Long workOrderId,
                                                              Long pickListId, Long releaseApplicationId,
                                                              String entryType);

    MesProEdhrBatchTraceabilityRespVO appendReleaseDecision(MesProEdhrBatchTraceReleaseDecisionCommand command);

    /**
     * Reads the persisted source relation used by Flow8 before material gate evaluation.
     * Expected values are optional on the first read and mandatory as a complete witness
     * on subsequent reads.
     */
    MesProEdhrBatchTraceSourcePrecheckRespVO resolveSourcePrecheck(
            MesProEdhrBatchTraceSourcePrecheckCommand command);
}
