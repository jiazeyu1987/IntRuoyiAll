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

@TableName("mes_pro_edhr_recordbook_tag_binding")
@KeySequence("mes_pro_edhr_recordbook_tag_binding_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrRecordbookTagBindingDO extends BaseDO {

    @TableId
    private Long id;

    private Long entryId;

    private Long recordbookId;

    private String tagCode;

    private String tagName;

    private String tagStatus;

    private Long boundBy;

    private LocalDateTime boundAt;

    private String remark;
}
