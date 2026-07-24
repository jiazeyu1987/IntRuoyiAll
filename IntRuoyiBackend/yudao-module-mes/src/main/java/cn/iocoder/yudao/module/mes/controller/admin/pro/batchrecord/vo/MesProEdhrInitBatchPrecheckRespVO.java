package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class MesProEdhrInitBatchPrecheckRespVO {

    private Long initBatchId;

    private String status;

    private Integer manifestCount;

    private Integer issueCount;

    private Integer blockingIssueCount;

    private LocalDateTime precheckAt;

    private List<MesProEdhrInitIssueRespVO> issues;
}
