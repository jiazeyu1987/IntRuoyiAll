package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport;

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

@TableName("mes_pro_batch_record_definition")
@KeySequence("mes_pro_batch_record_definition_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProBatchRecordDefinitionDO extends BaseDO {

    @TableId
    private Long id;

    private String batchRecordName;

    private String routeKey;

    private Long currentVersionId;

    private String remark;
}
