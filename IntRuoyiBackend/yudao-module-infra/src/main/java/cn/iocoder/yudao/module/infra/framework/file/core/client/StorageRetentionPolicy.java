package cn.iocoder.yudao.module.infra.framework.file.core.client;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.Instant;

/**
 * 单个存储对象必须满足的 Retention/Object Lock/legal hold 策略。
 */
@Data
@Accessors(chain = true)
public class StorageRetentionPolicy implements Serializable {

    /**
     * 是否要求 S3 Object Lock/Retention 证据。
     */
    @NotNull(message = "objectLockRequired 不能为空")
    private Boolean objectLockRequired;
    /**
     * Retention 模式，例如 COMPLIANCE、GOVERNANCE。
     */
    @NotBlank(message = "retentionMode 不能为空")
    private String retentionMode;
    /**
     * 从当前时间开始计算的保留天数。与 retainUntil 至少填写一个。
     */
    @Positive(message = "retentionDays 必须大于 0")
    private Integer retentionDays;
    /**
     * 明确的保留截止时间。与 retentionDays 至少填写一个。
     */
    private Instant retainUntil;
    /**
     * 是否要求 legal hold 为 ON。
     */
    @NotNull(message = "legalHoldRequired 不能为空")
    private Boolean legalHoldRequired;
    /**
     * 验证指定对象版本时必须传入版本号。
     */
    private String objectVersionId;
    /**
     * 调用方期望匹配的 SHA-256，可为空。
     */
    private String checksumSha256;

    @AssertTrue(message = "retentionDays 或 retainUntil 必须至少填写一个")
    @JsonIgnore
    public boolean isRetentionPeriodValid() {
        return retentionDays != null || retainUntil != null;
    }

    @AssertTrue(message = "objectLockRequired 为 true 时 retentionMode 不能为空")
    @JsonIgnore
    public boolean isObjectLockRetentionModeValid() {
        return !Boolean.TRUE.equals(objectLockRequired) || StrUtil.isNotBlank(retentionMode);
    }

}
