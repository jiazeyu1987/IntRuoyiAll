package cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("gxp_audit_event_relation")
@KeySequence("gxp_audit_event_relation_seq")
@Data
public class GxpAuditEventRelationDO {

    @TableId
    private Long id;
    private Long tenantId;
    private Long eventId;
    private String relationType;
    private String targetType;
    private String targetId;
    private String targetVersion;
    private String targetHash;
    private LocalDateTime createdAtUtc;

}
