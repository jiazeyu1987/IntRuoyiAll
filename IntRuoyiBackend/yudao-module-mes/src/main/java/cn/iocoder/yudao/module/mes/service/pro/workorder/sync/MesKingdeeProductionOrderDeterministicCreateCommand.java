package cn.iocoder.yudao.module.mes.service.pro.workorder.sync;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * Deterministic input for an AI E2E run. The base MES work order supplies product
 * and date; this command supplies the run-scoped external identity and quantity.
 */
@Data
@Accessors(chain = true)
public class MesKingdeeProductionOrderDeterministicCreateCommand {

    private String runId;

    private String slot;

    private BigDecimal quantity;

    private String batchNumber;
}
