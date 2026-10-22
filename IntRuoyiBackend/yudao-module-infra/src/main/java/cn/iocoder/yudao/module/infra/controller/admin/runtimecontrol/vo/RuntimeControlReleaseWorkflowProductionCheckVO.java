package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuntimeControlReleaseWorkflowProductionCheckVO {
    private String code;
    private String status;
    private String message;
    private String evidenceRef;
}
