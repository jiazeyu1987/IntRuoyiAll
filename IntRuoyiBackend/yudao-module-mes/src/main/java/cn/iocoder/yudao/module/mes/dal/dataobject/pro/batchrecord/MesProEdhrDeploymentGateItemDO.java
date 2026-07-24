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

@TableName("mes_pro_edhr_deployment_gate_item")
@KeySequence("mes_pro_edhr_deployment_gate_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrDeploymentGateItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private Long deploymentId;

    private String gateCode;

    private String gateName;

    private String gateStatus;

    private String evidenceSource;

    private String missingEvidence;

    private String ownerName;

    private String nextAction;

    private String signoffImpact;
}

