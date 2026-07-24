package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrPrintExportAuditRespVO {

    private Long id;

    private String exportCode;

    private String filterSnapshotJson;

    private String resultStatus;

    private String evidenceHash;

    private String idempotencyKey;

    private Long exportedBy;

    private LocalDateTime exportedAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
