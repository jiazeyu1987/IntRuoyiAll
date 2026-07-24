package cn.iocoder.yudao.module.dcc.dal.dataobject.protection;

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

/**
 * DCC controlled file upload size policy.
 */
@TableName("dcc_controlled_file_upload_policy")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileUploadPolicyDO extends BaseDO {

    @TableId
    private Long id;
    private String policyCode;
    private String scopeType;
    private Long categoryId;
    private String purpose;
    private Long maxBytes;
    private Boolean enabled;
    private Integer priority;
    private String policyVersion;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private String changeReason;

}
