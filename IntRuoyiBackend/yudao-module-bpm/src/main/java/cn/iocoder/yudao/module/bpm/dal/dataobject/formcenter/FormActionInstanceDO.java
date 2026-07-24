package cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("bpm_form_action_instance")
@KeySequence("bpm_form_action_instance_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormActionInstanceDO extends BaseDO {

    @TableId
    private Long id;

    private String instanceCode;

    private Long tenantId;

    private Long policyId;

    private Long applicantUserId;

    private String status;

    private String dataDomain;

    private String systemCode;

    private String objectType;

    private String objectId;

    private String objectVersion;

    private String actionCode;

    private String objectState;

    private String idempotencyKey;

    private String businessContextJson;

    private String formDataJson;

    private String bpmProcessInstanceId;

}
