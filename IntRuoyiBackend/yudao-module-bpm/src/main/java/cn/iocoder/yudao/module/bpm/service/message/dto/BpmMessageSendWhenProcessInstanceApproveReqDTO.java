package cn.iocoder.yudao.module.bpm.service.message.dto;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * BPM 发送流程实例被通过 Request DTO
 */
@Data
public class BpmMessageSendWhenProcessInstanceApproveReqDTO {

    /**
     * 流程实例的编号
     */
    @NotEmpty(message = "流程实例的编号不能为空")
    private String processInstanceId;
    /**
     * 流程实例的名字
     */
    @NotEmpty(message = "流程实例的名字不能为空")
    private String processInstanceName;
    /**
     * 流程定义 key
     */
    @NotEmpty(message = "流程定义标识不能为空")
    private String processDefinitionKey;
    @NotNull(message = "发起人的用户编号")
    private Long startUserId;

}
