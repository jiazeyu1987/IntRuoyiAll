package cn.iocoder.yudao.module.mes.dal.dataobject.pro.mesprocess;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_pro_mes_process_catalog")
@KeySequence("mes_pro_mes_process_catalog_seq")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProMesProcessCatalogDO extends BaseDO {

    @TableId
    private Long id;

    private String sourceFileName;

    private String sourceSheetName;

    private Integer sourceRowNo;

    private Integer sortNo;

    private String catalogCode;

    private String productName;

    private String sourceMachineryCodes;

    private String mesProcessName;

    private String sourceMachineryName;

    private String sourceMachineryQuantity;

    @TableField("daily_capacity_10_5")
    private String dailyCapacity10_5;

    private String dailyWorkerQuantity;

    private String mesProcessCode;

    private String processPrice;

    private String feedbackFlag;

    private String batchRecordFlag;

    private String batchRecordProcessName;
}
