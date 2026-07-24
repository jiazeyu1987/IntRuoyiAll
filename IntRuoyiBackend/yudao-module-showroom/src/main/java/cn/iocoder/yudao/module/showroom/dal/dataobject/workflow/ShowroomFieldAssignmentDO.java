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

@TableName("showroom_field_assignment")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowroomFieldAssignmentDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String targetType;

    private Long targetId;

    private String fieldCode;

    private Long assigneeUserId;

    private Long assignedBy;

    private String status;

    private Long notifyMessageId;

    private Long lastSavedRevisionId;

    private Long lastChangeRequestId;

    private LocalDateTime latestAutoSavedAt;

    private LocalDateTime submittedAt;

    private LocalDateTime createdAt;

    private LocalDateTime closedAt;

}
