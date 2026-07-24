package cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("bpm_form_action_snapshot")
@KeySequence("bpm_form_action_snapshot_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormActionSnapshotDO extends BaseDO {

    @TableId
    private Long id;

    private Long instanceId;

    private Long tenantId;

    private String snapshotType;

    private Integer snapshotVersion;

    private String formDataJson;

    private String businessContextJson;

    private String attachmentIdsJson;

}
