package cn.iocoder.yudao.module.mes.dal.dataobject.pro.mesprocess;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_pro_mes_process_catalog_machinery")
@KeySequence("mes_pro_mes_process_catalog_machinery_seq")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProMesProcessCatalogMachineryDO extends BaseDO {

    @TableId
    private Long id;

    private Long catalogId;

    private Integer machinerySortNo;

    private String machineryCode;

    private String machineryName;
}
