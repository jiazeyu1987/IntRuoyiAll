package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * eDHR 不合格评审租户级编号计数器。
 *
 * <p>流水跨月持续递增，月份只用于编号展示；租户编号是计数器的唯一键。</p>
 */
@TableName("mes_pro_edhr_nonconformance_review_counter")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProEdhrNonconformanceReviewCounterDO {

    @TableId
    private Long tenantId;

    private Long currentSerial;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
