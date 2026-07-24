package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES eDHR 主数据追溯分页 Response VO")
@Data
@Accessors(chain = true)
public class MesProBatchRecordDomainTracePageRespVO {

    private Long executionId;
    private String executionCode;
    private String workOrderCode;
    private String batchCode;
    private String status;
    private String domainTraceHash;
    private LocalDateTime verifiedAt;
    private Integer blockerCount;
    private Integer itemCount;
}
