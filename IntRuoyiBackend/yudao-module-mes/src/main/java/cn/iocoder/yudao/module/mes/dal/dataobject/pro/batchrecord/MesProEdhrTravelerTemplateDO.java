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

@TableName("mes_pro_edhr_traveler_template")
@KeySequence("mes_pro_edhr_traveler_template_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrTravelerTemplateDO extends BaseDO {

    @TableId
    private Long id;

    private String templateCode;

    private String templateName;

    private String templateVersion;

    private String status;

    private String applicableProductCode;

    private Long applicableRouteId;

    private String applicableRouteCode;

    private Long applicableProcessId;

    private String applicableProcessCode;

    private String applicableProcessName;

    private LocalDateTime activeAt;

    private String remark;
}
