package cn.iocoder.yudao.module.system.dal.dataobject.codextest;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("system_codex_test_case")
@KeySequence("system_codex_test_case_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CodexTestCaseDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String name;
    private String project;
    private String nodeChainName;
    private Integer nodeChainSort;
    private String methodText;
    private String testDataText;
    private String analysisMode;
    private String defaultExecutionMode;
    private Boolean parallelSafe;
    private String status;
    private Integer sort;

}
