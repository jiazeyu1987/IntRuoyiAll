package cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc;

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

@TableName("mes_pqc_inspection_piece_detail")
@KeySequence("mes_pqc_inspection_piece_detail_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesPqcInspectionPieceDetailDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long taskId;
    private Integer sampleNo;
    private String itemCode;
    private String itemName;
    private String inspectionMethod;
    private String standardText;
    private String resultType;
    private String itemResult;
    private String measuredValue;
    private String judgement;
}
