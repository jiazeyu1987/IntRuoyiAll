package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;

/** Minimal identity for the QA reviewer picker; never exposes account credentials. */
@Data
public class MesQaReviewerCandidateRespVO {
    private Long id;
    private String username;
    private String nickname;
    private Long deptId;
    private Integer status;
}
