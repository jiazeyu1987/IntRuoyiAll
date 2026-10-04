package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DCC controlled file task assignee snapshot.
 */
@TableName("dcc_controlled_file_task_assignee_snapshot")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileTaskAssigneeSnapshotDO extends BaseDO {

    @TableId
    private Long id;
    private Long controlledFileId;
    private String nodeInstanceId;
    private String stageCode;
    private Integer stageNo;
    private Long departmentId;
    private String departmentName;
    private Long assigneeUserId;
    private String assigneeName;
    private String leaderConfigDigest;
    private String bpmTaskId;
    private String obligationId;
    private Long tenantId;
    private String processInstanceId;
    private Long leaderUserId;
    private Long assignmentSignatureId;
    private String assignmentPayloadHash;
    private java.time.LocalDateTime assignedTime;

}
