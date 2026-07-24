package cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("bpm_business_approval_request")
@KeySequence("bpm_business_approval_request_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessApprovalRequestDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private Long policyId;

    private String policyMode;

    private String dataDomain;

    private String systemCode;

    private String objectType;

    private String objectId;

    private String objectVersion;

    private String actionCode;

    private String objectState;

    private String requestStatus;

    private Long applicantUserId;

    private String processDefinitionKey;

    private String processInstanceId;

    private String effectExecutorCode;

    private String lastEventKey;

    private String resultState;

    private String failureReason;

    private String businessContextJson;

}
