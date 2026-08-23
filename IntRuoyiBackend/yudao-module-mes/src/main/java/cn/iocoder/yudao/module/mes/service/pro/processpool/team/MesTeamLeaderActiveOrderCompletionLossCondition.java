package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/** Formal per-process loss fact supplied by Flow 5 for the completion Tx-A. */
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderCompletionLossCondition {

    public static final String REQUIRED = "REQUIRED";
    public static final String NO_LOSS = "NO_LOSS";
    public static final String BLOCKED = "BLOCKED";

    private Long processId;
    private String status;
    private Boolean hasActualLoss;
    private BigDecimal lossQuantity;
    private Long lossRecordId;
    private String zeroLossConfirmationSnapshot;
    private String sourceHash;
}
