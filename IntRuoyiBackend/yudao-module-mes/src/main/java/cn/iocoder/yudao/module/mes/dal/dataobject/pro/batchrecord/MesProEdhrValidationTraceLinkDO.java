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

@TableName("mes_pro_edhr_validation_trace_link")
@KeySequence("mes_pro_edhr_validation_trace_link_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrValidationTraceLinkDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private Long packageId;

    private Long sourceItemId;

    private String sourceItemCode;

    private String sourceItemType;

    private Long targetItemId;

    private String targetItemCode;

    private String targetItemType;

    private String linkType;

    private String traceStatus;

    private String ownerName;

    private String nextAction;

    private String remark;
}
