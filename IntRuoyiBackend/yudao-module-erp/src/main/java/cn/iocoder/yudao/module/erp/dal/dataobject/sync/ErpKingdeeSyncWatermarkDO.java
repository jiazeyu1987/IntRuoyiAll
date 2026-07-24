package cn.iocoder.yudao.module.erp.dal.dataobject.sync;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

@TableName("erp_kingdee_sync_watermark")
@KeySequence("erp_kingdee_sync_watermark_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpKingdeeSyncWatermarkDO extends BaseDO {

    @TableId
    private Long id;

    private String syncType;
    private LocalDateTime lastSuccessTime;

}
