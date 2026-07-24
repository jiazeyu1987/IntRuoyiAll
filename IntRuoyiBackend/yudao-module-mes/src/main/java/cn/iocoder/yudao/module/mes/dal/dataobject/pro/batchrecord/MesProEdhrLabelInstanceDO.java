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

import java.time.LocalDateTime;

@TableName("mes_pro_edhr_label_instance")
@KeySequence("mes_pro_edhr_label_instance_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrLabelInstanceDO extends BaseDO {

    @TableId
    private Long id;

    private String labelCode;

    private Long templateId;

    private String templateCode;

    private String templateVersion;

    private String businessType;

    private Long businessObjectId;

    private String businessObjectCode;

    private String renderSnapshotJson;

    private String parserVersion;

    private String status;

    private String printStatus;

    private String businessKeyHash;

    private Long generatedBy;

    private LocalDateTime generatedAt;

    private String remark;
}
