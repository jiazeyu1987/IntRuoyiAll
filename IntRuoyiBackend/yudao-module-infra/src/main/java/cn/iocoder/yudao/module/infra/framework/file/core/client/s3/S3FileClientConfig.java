package cn.iocoder.yudao.module.infra.framework.file.core.client.s3;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClientConfig;
import cn.iocoder.yudao.module.infra.framework.file.core.client.StorageRetentionPolicy;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

/**
 * S3 文件客户端的配置类
 *
 * @author 瑛泰源码
 */
@Data
public class S3FileClientConfig implements FileClientConfig {

    public static final String ENDPOINT_QINIU = "qiniucs.com";
    public static final String ENDPOINT_ALIYUN = "aliyuncs.com";
    public static final String ENDPOINT_TENCENT = "myqcloud.com";
    public static final String ENDPOINT_VOLCES = "volces.com"; // 火山云（字节）

    /**
     * 节点地址
     * 1. MinIO：https://www.iocoder.cn/Spring-Boot/MinIO 。例如说，http://127.0.0.1:9000
     * 2. 阿里云：https://help.aliyun.com/document_detail/31837.html
     * 3. 腾讯云：https://cloud.tencent.com/document/product/436/6224
     * 4. 七牛云：https://developer.qiniu.com/kodo/4088/s3-access-domainname
     * 5. 华为云：https://console.huaweicloud.com/apiexplorer/#/endpoint/OBS
     * 6. 火山云：https://www.volcengine.com/docs/6349/107356
     */
    @NotNull(message = "endpoint 不能为空")
    private String endpoint;
    /**
     * 自定义域名
     * 1. MinIO：通过 Nginx 配置
     * 2. 阿里云：https://help.aliyun.com/document_detail/31836.html
     * 3. 腾讯云：https://cloud.tencent.com/document/product/436/11142
     * 4. 七牛云：https://developer.qiniu.com/kodo/8556/set-the-custom-source-domain-name
     * 5. 华为云：https://support.huaweicloud.com/usermanual-obs/obs_03_0032.html
     * 6. 火山云：https://www.volcengine.com/docs/6349/128983
     */
    @URL(message = "domain 必须是 URL 格式")
    private String domain;
    /**
     * 存储 Bucket
     */
    @NotNull(message = "bucket 不能为空")
    private String bucket;

    /**
     * 访问 Key
     * 1. MinIO：https://www.iocoder.cn/Spring-Boot/MinIO
     * 2. 阿里云：https://ram.console.aliyun.com/manage/ak
     * 3. 腾讯云：https://console.cloud.tencent.com/cam/capi
     * 4. 七牛云：https://portal.qiniu.com/user/key
     * 5. 华为云：https://support.huaweicloud.com/qs-obs/obs_qs_0005.html
     * 6. 火山云：https://console.volcengine.com/iam/keymanage/
     */
    @NotNull(message = "accessKey 不能为空")
    private String accessKey;
    /**
     * 访问 Secret
     */
    @NotNull(message = "accessSecret 不能为空")
    @ToString.Exclude
    private String accessSecret;

    /**
     * 是否启用 PathStyle 访问
     */
    @NotNull(message = "enablePathStyleAccess 不能为空")
    private Boolean enablePathStyleAccess;

    /**
     * 是否公开访问
     *
     * true：公开访问，所有人都可以访问
     * false：私有访问，只有配置的 accessKey 才可以访问
     */
    @NotNull(message = "是否公开访问不能为空")
    private Boolean enablePublicAccess;

    /**
     * 区域
     * 1. AWS S3：https://docs.aws.amazon.com/general/latest/gr/s3.html 例如说，us-east-1、us-west-2
     * 2. MinIO：可以填任意值，通常使用 us-east-1
     * 3. 阿里云：不需要填写，会自动识别
     * 4. 腾讯云：不需要填写，会自动识别
     * 5. 七牛云：不需要填写，会自动识别
     * 6. 华为云：不需要填写，会自动识别
     * 7. 火山云：不需要填写，会自动识别
     */
    private String region;

    /**
     * 对象级 Retention/Object Lock 策略。直接字段是配置表单来源，本字段承载对象版本等 typed policy 扩展。
     */
    private StorageRetentionPolicy retentionPolicy;

    /**
     * 是否要求 S3 Object Lock。
     */
    private Boolean objectLockRequired;

    /**
     * Retention 模式，例如 COMPLIANCE、GOVERNANCE。
     */
    private String retentionMode;

    /**
     * Retention 保留天数。与 retentionRetainUntil 至少填写一个。
     */
    @Positive(message = "retentionDays 必须大于 0")
    private Integer retentionDays;

    /**
     * 明确的 Retention 截止时间。与 retentionDays 至少填写一个。
     */
    private Instant retentionRetainUntil;

    /**
     * 是否要求 legal hold 为 ON。
     */
    private Boolean legalHoldRequired;

    @SuppressWarnings("RedundantIfStatement")
    @AssertTrue(message = "domain 不能为空")
    @JsonIgnore
    public boolean isDomainValid() {
        // 如果是七牛，必须带有 domain
        if (StrUtil.contains(endpoint, ENDPOINT_QINIU) && StrUtil.isEmpty(domain)) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("RedundantIfStatement")
    @AssertTrue(message = "objectLock retention policy 配置不完整")
    @JsonIgnore
    public boolean isObjectLockRetentionPolicyValid() {
        if (!Boolean.TRUE.equals(objectLockRequired)) {
            return true;
        }
        if (StrUtil.isEmpty(retentionMode)) {
            return false;
        }
        if (retentionDays == null && retentionRetainUntil == null) {
            return false;
        }
        return legalHoldRequired != null;
    }

    @JsonIgnore
    public StorageRetentionPolicy buildStorageRetentionPolicy() {
        return new StorageRetentionPolicy()
                .setObjectLockRequired(objectLockRequired)
                .setRetentionMode(retentionMode)
                .setRetentionDays(retentionDays)
                .setRetainUntil(retentionRetainUntil)
                .setLegalHoldRequired(legalHoldRequired)
                .setObjectVersionId(retentionPolicy != null ? retentionPolicy.getObjectVersionId() : null)
                .setChecksumSha256(retentionPolicy != null ? retentionPolicy.getChecksumSha256() : null);
    }

}
