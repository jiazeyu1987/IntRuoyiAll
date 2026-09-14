package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import java.util.List;

public interface MesProEdhrFourMaterialGateService {

    List<String> REQUIRED_MATERIAL_TYPES = List.of(
            "INCOMING_INSPECTION_REPORT",
            "STERILIZATION_REPORT",
            "FINISHED_PRODUCT_INSPECTION_REPORT",
            "FINISHED_PRODUCT_INSPECTION_RECORD");

    MesProEdhrFourMaterialGateResult evaluate(Long batchExecutionId);

    default MesProEdhrFourMaterialGateResult requireMaterialsReady(Long batchExecutionId) {
        MesProEdhrFourMaterialGateResult result = evaluate(batchExecutionId);
        if (!result.ready()) {
            throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
                    MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_FOUR_MATERIAL_GATE_BLOCKED,
                    result.status());
        }
        return result;
    }
}
