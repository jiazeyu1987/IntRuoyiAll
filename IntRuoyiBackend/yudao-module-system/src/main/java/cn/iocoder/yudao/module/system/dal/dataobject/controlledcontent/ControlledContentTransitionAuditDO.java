package cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Controlled content lifecycle transition audit.
 */
@TableName("controlled_content_transition_audit")
@KeySequence("controlled_content_transition_audit_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ControlledContentTransitionAuditDO {

    @TableId
    private Long id;

    private Long tenantId;
    private Long versionRefId;
    private String contentType;
    private String contentKey;
    private String fromStatus;
    private String toStatus;
    private String domainFromStatus;
    private String domainToStatus;
    private String action;
    private String eventKey;
    private Long actorId;
    private String reason;
    private LocalDateTime createTime;

}
