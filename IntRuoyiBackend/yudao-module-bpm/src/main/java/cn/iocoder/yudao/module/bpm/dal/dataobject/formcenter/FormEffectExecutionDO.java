package cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("bpm_form_effect_execution")
@KeySequence("bpm_form_effect_execution_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormEffectExecutionDO extends BaseDO {

    @TableId
    private Long id;

    private Long instanceId;

    private Long tenantId;

    private String executionCode;

    private String idempotencyKey;

    private String status;

    private String resultRef;

    private String failureReason;

}
