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

@TableName("mes_pro_edhr_recordbook_entry")
@KeySequence("mes_pro_edhr_recordbook_entry_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrRecordbookEntryDO extends BaseDO {

    @TableId
    private Long id;

    private String entryCode;

    private Long recordbookId;

    private String recordbookCode;

    private Long templateId;

    private String templateCode;

    private String templateVersion;

    private String status;

    private Integer version;

    private String entryTitle;

    private String entryContentJson;

    private String tagSnapshotJson;

    private Long submittedBy;

    private LocalDateTime submittedAt;

    private LocalDateTime lockedAt;

    private String idempotencyKey;

    private String remark;
}
