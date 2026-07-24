package cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("bpm_form_action_policy")
@KeySequence("bpm_form_action_policy_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormActionPolicyDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private String dataDomain;

    private String systemCode;

    private String objectType;

    private String actionCode;

    private String objectState;

    private String policyType;

    private String approvalMode;

    private String bpmProcessKey;

    private String effectExecutorCode;

    private String status;

    private String slotsJson;

    private String remark;

}
