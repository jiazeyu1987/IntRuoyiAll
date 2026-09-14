package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MesReportAllocationSaveCommand {
    private Long eventId;
    private Long leaderUserId;
    private String leaderType;
    private Integer expectedVersion;
    private String idempotencyKey;
    private String allocationMode;
    private String reason;
    private String signaturePassword;
    private List<MesReportAllocationSaveLine> allocations;
}
