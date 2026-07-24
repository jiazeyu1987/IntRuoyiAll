package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesProEdhrPermissionEvaluateCommand {

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

    private List<String> abilities;

    private Long actorUserId;

    private String actorUsername;

    private Long actorDeptId;

    private String permissionCode;

    private String actionName;
}
