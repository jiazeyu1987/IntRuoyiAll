package cn.iocoder.yudao.module.showroom.dal.dataobject.workflow;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("showroom_change_request")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowroomChangeRequestDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String targetType;

    private Long targetId;

    private Long targetRevisionId;

    private String moduleCode;

    private String requestType;

    private String submissionSource;

    private String status;

    private String processInstanceId;

    private Long submittedBy;

    private Long submitterDeptId;

    private LocalDateTime submittedAt;

    private Long supervisorUserId;

    private Long supervisorDeptId;

    private LocalDateTime supervisorActionAt;

    private Long gaoxinUserId;

    private LocalDateTime gaoxinActionAt;

    private String rejectionReason;

    private Long sourceAssignmentId;

}
