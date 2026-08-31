package cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@TableName("mes_pro_feedback_material")
@KeySequence("mes_pro_feedback_material_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProFeedbackMaterialDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long feedbackId;
    private Long activeOrderId;
    private Long workOrderId;
    private Long routeId;
    private Long routeVersionId;
    private Long routeProcessId;
    private Long processId;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String materialSpecification;
    private BigDecimal bomQuantity;
    private BigDecimal outputQuantity;
    private BigDecimal lossQuantity;
    private String lossDetailsJson;
    private String selectedDeviceJson;
    private String deviceParameterReadingsJson;
    @Version
    private Integer version;
}
