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

@TableName("mes_pro_edhr_recordbook")
@KeySequence("mes_pro_edhr_recordbook_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrRecordbookDO extends BaseDO {

    @TableId
    private Long id;

    private String recordbookCode;

    private String recordbookName;

    private Long templateId;

    private String templateCode;

    private String templateName;

    private String templateVersion;

    private String recordbookType;

    private String status;

    private Long ownerUserId;

    private Long ownerDeptId;

    private String businessScope;

    private String businessObjectType;

    private Long businessObjectId;

    private String businessObjectCode;

    private LocalDateTime openedAt;

    private LocalDateTime closedAt;

    private Integer entryCount;

    private String remark;
}
