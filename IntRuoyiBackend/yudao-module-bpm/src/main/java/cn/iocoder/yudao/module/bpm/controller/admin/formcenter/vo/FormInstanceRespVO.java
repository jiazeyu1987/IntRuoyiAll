package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import lombok.Data;

@Data
public class FormInstanceRespVO {

    private Long id;

    private String instanceCode;

    private String status;

    private String bpmProcessInstanceId;

    private BusinessActionContextReqVO context;

}
