package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesProEdhrPermissionEvaluateReqVO {

    private Long scopeId;

    private String objectType;

    private String objectId;

    private Long batchExecutionId;

    private Long executionId;

    private Long workTaskId;

    private Long routeId;

    private Long routeProcessId;

    private String reportId;

    private String recordCategory;

    @NotEmpty(message = "权限能力不能为空")
    private List<String> abilities;
}
