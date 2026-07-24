package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;

@TableName("mes_pro_batch_record_template")
@KeySequence("mes_pro_batch_record_template_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProBatchRecordTemplateDO extends BaseDO {

    @TableId
    private Long id;

    private String templateCode;

    private String templateName;

    private Long importId;

    private Integer sort;

    private Integer status;

    private Long processId;

    private String processName;

    private String productName;

    private Integer sourceTableIndex;

    private String tableTitle;

    private String sheetLayoutJson;

    private String metaJson;

    private String remark;
}
