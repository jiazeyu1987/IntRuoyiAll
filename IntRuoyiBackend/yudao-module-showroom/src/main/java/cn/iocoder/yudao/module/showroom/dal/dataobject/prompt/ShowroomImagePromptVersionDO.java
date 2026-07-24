package cn.iocoder.yudao.module.showroom.dal.dataobject.prompt;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("showroom_image_prompt_version")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowroomImagePromptVersionDO extends BaseDO {

    @TableId
    private Long id;

    private String sceneCode;

    private Integer versionNo;

    private String templateText;

    private String changeNote;

    private String placeholderCodesJson;

    private Integer useCount;

    private LocalDateTime lastUsedAt;
}
