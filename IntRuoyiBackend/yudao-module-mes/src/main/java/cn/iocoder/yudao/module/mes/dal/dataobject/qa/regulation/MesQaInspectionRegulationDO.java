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

@TableName("mes_qa_inspection_regulation")
@KeySequence("mes_qa_inspection_regulation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesQaInspectionRegulationDO extends TenantBaseDO {

    public static final String OWNER_MODULE_MES_QA = "MES_QA";

    @TableId
    private Long id;

    private Long dccProjectCodeId;
    private Long productId;
    private Long routeId;
    private Long routeVersionId;
    private Long routeProcessId;
    private Long processId;
    private String ownerModule;
    private String regulationCode;
    private String regulationName;
    private String lifecycleStatus;
    private Long currentVersionId;
}
