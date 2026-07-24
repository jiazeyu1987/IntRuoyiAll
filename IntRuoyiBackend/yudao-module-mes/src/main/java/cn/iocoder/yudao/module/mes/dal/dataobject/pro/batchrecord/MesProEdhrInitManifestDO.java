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

@TableName("mes_pro_edhr_init_manifest")
@KeySequence("mes_pro_edhr_init_manifest_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrInitManifestDO extends BaseDO {

    @TableId
    private Long id;

    private Long initBatchId;

    private String packageType;

    private String manifestHash;

    private String sourceFileName;

    private String sourceFileUrl;

    private Long fileSize;

    private String checksumJson;

    private String manifestJson;

    private String uploadStatus;

    private Long uploadedBy;

    private LocalDateTime uploadedAt;
}
