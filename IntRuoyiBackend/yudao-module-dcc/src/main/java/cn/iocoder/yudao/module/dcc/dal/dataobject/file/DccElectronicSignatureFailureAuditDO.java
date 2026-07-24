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

import java.time.LocalDateTime;

@TableName("dcc_electronic_signature_failure_audit")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccElectronicSignatureFailureAuditDO extends BaseDO {

    @TableId
    private Long id;
    private Long targetUserId;
    private Long controlledFileId;
    private Long revisionId;
    private String taskId;
    private String actionType;
    private String meaningCode;
    private String failureType;
    private String failureMessage;
    private LocalDateTime failedAt;
    private String remoteIp;
    private String userAgent;

}
