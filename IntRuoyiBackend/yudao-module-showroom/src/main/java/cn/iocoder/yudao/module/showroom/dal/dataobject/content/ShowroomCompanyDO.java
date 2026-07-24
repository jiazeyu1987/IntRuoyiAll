package cn.iocoder.yudao.module.showroom.dal.dataobject.content;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("showroom_company")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowroomCompanyDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String companyCode;

    private String displayName;

    private String displayNameEn;

    private String companyType;

    private Long currentRevisionId;

    private Integer currentRevisionNo;

    private Boolean incompleteFlag;

    private String status;

}
