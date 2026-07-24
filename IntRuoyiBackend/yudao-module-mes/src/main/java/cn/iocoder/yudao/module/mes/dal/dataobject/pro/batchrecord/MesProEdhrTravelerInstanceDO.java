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

@TableName("mes_pro_edhr_traveler_instance")
@KeySequence("mes_pro_edhr_traveler_instance_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrTravelerInstanceDO extends BaseDO {

    @TableId
    private Long id;

    private String travelerCode;

    private Long templateId;

    private String templateCode;

    private String templateVersion;

    private Long batchExecutionId;

    private String batchExecutionCode;

    private Long workOrderId;

    private String workOrderCode;

    private String batchCode;

    private Long productId;

    private String productCode;

    private String productName;

    private String serialNo;

    private String scopeType;

    private Long routeId;

    private String routeCode;

    private String routeName;

    private Long routeProcessId;

    private Integer routeProcessSort;

    private Long processId;

    private String processCode;

    private String processName;

    private String status;

    private String printStatus;

    private String businessKeyHash;

    private Long generatedBy;

    private LocalDateTime generatedAt;

    private String remark;
}
