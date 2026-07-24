package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrWorkTaskAssignmentRuleRespVO {

    private Long id;

    private Long routeProcessId;

    private String scopeType;

    private Long scopeId;

    private String taskType;

    private Long assigneeUserId;

    private Long reviewUserId;

    private String candidateSourceType;

    private Long candidateSourceId;

    private Integer dueMinutes;

    private Boolean enabled;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
