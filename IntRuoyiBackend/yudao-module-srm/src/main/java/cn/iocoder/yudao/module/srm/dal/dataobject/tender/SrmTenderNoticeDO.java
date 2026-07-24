package cn.iocoder.yudao.module.srm.dal.dataobject.tender;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

@TableName("srm_tender_notice")
@KeySequence("srm_tender_notice_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmTenderNoticeDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long projectId;

    private String noticeTitle;

    private String noticeAttachmentUrl;

    private LocalDateTime publishedTime;
}
