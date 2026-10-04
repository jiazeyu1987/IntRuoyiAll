package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

/** Read-only directory option for one authorized controlled file and selected department. */
public record DccWorkflowDistributionRecipientRespVO(@JsonFormat(shape = JsonFormat.Shape.STRING) Long id, String name) {
}
