package cn.iocoder.yudao.module.system.dal.dataobject.codextest;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("system_codex_test_checkpoint")
@KeySequence("system_codex_test_checkpoint_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CodexTestCheckpointDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long caseId;
    private Integer sort;
    private String name;
    private String expectedText;
    private String severity;
    private String remark;

}
