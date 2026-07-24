package cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("bpm_business_approval_policy")
@KeySequence("bpm_business_approval_policy_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessApprovalPolicyDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private String dataDomain;

    private String systemCode;

    private String objectType;

    private String actionCode;

    private String objectState;

    private String policyMode;

    private String processDefinitionKey;

    private String effectExecutorCode;

    private String status;

    private String remark;

}
