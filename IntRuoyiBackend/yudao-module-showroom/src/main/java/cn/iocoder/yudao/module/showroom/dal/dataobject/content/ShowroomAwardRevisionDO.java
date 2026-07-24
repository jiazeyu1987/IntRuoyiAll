package cn.iocoder.yudao.module.showroom.dal.dataobject.content;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("showroom_award_revision")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowroomAwardRevisionDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long awardId;

    private Integer revisionNo;

    private String status;

    private String awardCodeSnapshot;

    private String nameCn;

    private String nameEn;

    private String descriptionZh;

    private String descriptionEn;

    private String issuer;

    private String awardDateText;

    private String coverImage;

    private Long submittedBy;

    private Long approvedBy;

    private LocalDateTime publishedAt;
}
