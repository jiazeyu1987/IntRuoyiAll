package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

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
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@TableName("mes_pro_edhr_print_policy")
@KeySequence("mes_pro_edhr_print_policy_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrPrintPolicyDO extends BaseDO {

    @TableId
    private Long id;

    private String policyCode;

    private String policyName;

    private String businessType;

    private String templateType;

    private Integer firstPrintLimit;

    private Integer reprintLimit;

    private String reasonDictJson;

    private String watermarkTemplate;

    private String voidCopyWatermark;

    private String status;

    private LocalDateTime activeAt;

    private String remark;
}
