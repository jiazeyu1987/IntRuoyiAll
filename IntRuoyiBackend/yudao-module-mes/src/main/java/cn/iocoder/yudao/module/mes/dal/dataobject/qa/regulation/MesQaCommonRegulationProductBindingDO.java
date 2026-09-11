package cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
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

@TableName("mes_qa_common_regulation_product_binding")
@KeySequence("mes_qa_common_regulation_product_binding_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesQaCommonRegulationProductBindingDO extends TenantBaseDO {

    public static final String SCOPE_COMMON_PACKAGING = "COMMON_PACKAGING";
    public static final String STATUS_ENABLED = "ENABLED";
    public static final String STATUS_DISABLED = "DISABLED";

    @TableId
    private Long id;

    private Long productId;
    private Long dccProjectCodeId;
    private Long commonRegulationSetId;
    private Long commonRegulationSetVersionId;
    private Long regulationId;
    private Long regulationVersionId;
    private String scopeCode;
    private String bindingStatus;
    private String activeBindingKey;
    private Integer version;
    private String idempotencyKey;
    private String changeReason;
}
