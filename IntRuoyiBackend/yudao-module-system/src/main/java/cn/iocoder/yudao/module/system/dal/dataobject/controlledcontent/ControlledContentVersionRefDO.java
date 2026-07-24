package cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Controlled content lifecycle version reference.
 */
@TableName("controlled_content_version_ref")
@KeySequence("controlled_content_version_ref_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ControlledContentVersionRefDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;
    private String contentType;
    private String contentKey;
    private Long nativeMasterId;
    private Long nativeVersionId;
    private String versionNo;
    private String canonicalStatus;
    private String domainStatus;
    private Long sourceVersionRefId;
    private Long sourceNativeVersionId;
    private Long successorVersionRefId;
    private Long successorNativeVersionId;
    private Integer activeUniqueFlag;
    private Integer openCandidateUniqueFlag;
    private String approvalProcessInstanceId;
    private LocalDateTime lastTransitionTime;

}
