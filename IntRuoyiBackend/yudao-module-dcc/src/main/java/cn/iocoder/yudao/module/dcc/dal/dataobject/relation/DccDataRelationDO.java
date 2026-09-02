package cn.iocoder.yudao.module.dcc.dal.dataobject.relation;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("dcc_data_relation")
@Data
@EqualsAndHashCode(callSuper = true)
public class DccDataRelationDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long productCatalogId;
    private Long projectCodeId;
    private Long registrationCertificateId;
    private String relationStatus;
    private String relationSource;
    private String relationRemark;
    private Long confirmedBy;
    private LocalDateTime confirmedTime;
}
