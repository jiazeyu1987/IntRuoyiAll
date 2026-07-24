package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

import java.util.List;

@Data
public class DccControlledFileTrainingStatusRespVO {

    private Long id;
    private Long departmentId;
    private String status;
    private List<DccControlledFileTrainingAssignmentRespVO> assignments;
}
