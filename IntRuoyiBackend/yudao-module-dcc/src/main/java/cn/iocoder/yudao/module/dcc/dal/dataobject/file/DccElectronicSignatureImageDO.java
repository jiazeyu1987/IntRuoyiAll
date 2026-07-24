package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

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

import java.time.LocalDateTime;

@TableName("dcc_electronic_signature_image")
@KeySequence("dcc_electronic_signature_image_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccElectronicSignatureImageDO extends BaseDO {

    @TableId
    private Long id;

    private Long userId;
    private Integer versionNo;
    private Long fileId;
    private String fileUrl;
    private String storagePath;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String sha256;
    private String imageStatus;
    private Boolean active;
    private Long uploadedBy;
    private LocalDateTime uploadedAt;
    private LocalDateTime enabledAt;
    private LocalDateTime disabledAt;
    private String disableReason;
    private Integer referencedCount;
}
