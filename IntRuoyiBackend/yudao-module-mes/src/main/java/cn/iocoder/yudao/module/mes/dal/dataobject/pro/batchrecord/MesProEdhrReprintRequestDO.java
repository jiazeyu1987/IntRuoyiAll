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

@TableName("mes_pro_edhr_reprint_request")
@KeySequence("mes_pro_edhr_reprint_request_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrReprintRequestDO extends BaseDO {

    @TableId
    private Long id;

    private String requestCode;

    private Long printTaskId;

    private Long originalPrintTaskId;

    private String reprintReasonCode;

    private String reprintReason;

    private Integer usedReprintCount;

    private Integer reprintLimit;

    private String watermarkText;

    private String status;

    private String idempotencyKey;
}
