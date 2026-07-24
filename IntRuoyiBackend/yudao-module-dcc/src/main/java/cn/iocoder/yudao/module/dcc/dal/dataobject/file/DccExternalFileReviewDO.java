package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("dcc_external_file_review")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccExternalFileReviewDO extends BaseDO {

    @TableId
    private Long id;
    private Long tenantId;
    private Long controlledFileId;
    private String externalSource;
    private String externalOwner;
    private String reviewReason;
    private String participantUserIds;
    private String reviewConclusion;
    private String conclusionComment;
    private Long outputFileId;
    private LocalDateTime closedTime;
}
