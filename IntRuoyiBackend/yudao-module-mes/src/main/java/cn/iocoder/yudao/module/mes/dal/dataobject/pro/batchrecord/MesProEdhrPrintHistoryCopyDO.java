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

@TableName("mes_pro_edhr_print_history_copy")
@KeySequence("mes_pro_edhr_print_history_copy_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrPrintHistoryCopyDO extends BaseDO {

    @TableId
    private Long id;

    private String copyCode;

    private Long sourcePrintTaskId;

    private String sourceObjectType;

    private String sourceObjectCode;

    private String copyReason;

    private String watermarkText;

    private String evidenceHash;

    private String idempotencyKey;
}
