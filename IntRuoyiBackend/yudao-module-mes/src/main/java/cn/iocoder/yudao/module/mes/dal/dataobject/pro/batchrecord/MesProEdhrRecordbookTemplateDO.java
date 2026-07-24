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

@TableName("mes_pro_edhr_recordbook_template")
@KeySequence("mes_pro_edhr_recordbook_template_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrRecordbookTemplateDO extends BaseDO {

    @TableId
    private Long id;

    private String templateCode;

    private String templateName;

    private String templateVersion;

    private String recordbookType;

    private String entrySchemaJson;

    private String tagPolicyJson;

    private String status;

    private Long activeBy;

    private LocalDateTime activeAt;

    private Long disabledBy;

    private LocalDateTime disabledAt;

    private String remark;
}
