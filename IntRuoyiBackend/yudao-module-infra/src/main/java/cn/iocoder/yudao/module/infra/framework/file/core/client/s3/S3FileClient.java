package cn.iocoder.yudao.module.infra.framework.file.core.client.s3;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.infra.framework.file.core.client.AbstractFileClient;
import cn.iocoder.yudao.module.infra.framework.file.core.client.StorageRetentionEvidence;
import cn.iocoder.yudao.module.infra.framework.file.core.client.StorageRetentionPolicy;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectLegalHoldRequest;
import software.amazon.awssdk.services.s3.model.GetObjectLegalHoldResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRetentionRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRetentionResponse;
import software.amazon.awssdk.services.s3.model.ObjectLockLegalHold;
import software.amazon.awssdk.services.s3.model.ObjectLockLegalHoldStatus;
import software.amazon.awssdk.services.s3.model.ObjectLockMode;
import software.amazon.awssdk.services.s3.model.ObjectLockRetention;
import software.amazon.awssdk.services.s3.model.ObjectLockRetentionMode;
import software.amazon.awssdk.services.s3.model.PutObjectLegalHoldRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRetentionRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 基于 S3 协议的文件客户端，实现 MinIO、阿里云、腾讯云、七牛云、华为云等云服务
 *
 * @author 瑛泰源码
 */
@Slf4j
public class S3FileClient extends AbstractFileClient<S3FileClientConfig> {

    private static final Duration EXPIRATION_DEFAULT = Duration.ofHours(24);

    private S3Client client;
    private S3Presigner presigner;

    public S3FileClient(Long id, S3FileClientConfig config) {
        super(id, config);
    }

    @Override
    protected void doInit() {
        // 补全 domain
        if (StrUtil.isEmpty(config.getDomain())) {
            config.setDomain(buildDomain());
        }
        // 初始化 S3 客户端
        // 优先级：配置的 region > 从 endpoint 解析的 region > 默认值 us-east-1
        String regionStr = resolveRegion();
        Region region = Region.of(regionStr);
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(config.getAccessKey(), config.getAccessSecret()));
        URI endpoint = URI.create(buildEndpoint());
        URI presignerEndpoint = URI.create(buildPresignerEndpoint());
        S3Configuration serviceConfiguration = S3Configuration.builder() // Path-style 访问
                .pathStyleAccessEnabled(Boolean.TRUE.equals(config.getEnablePathStyleAccess()))
                .chunkedEncodingEnabled(false) // 禁用分块编码，参见 https://t.zsxq.com/kBy57
                .build();
        client = S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(region)
                .endpointOverride(endpoint)
                .serviceConfiguration(serviceConfiguration)
                .build();
        presigner = S3Presigner.builder()
                .credentialsProvider(credentialsProvider)
                .region(region)
                .endpointOverride(presignerEndpoint)
                .serviceConfiguration(serviceConfiguration)
                .build();
    }

    @Override
    public String upload(byte[] content, String path, String type) {
        // 构造 PutObjectRequest
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(config.getBucket())
                .key(path)
                .contentType(type)
                .contentLength((long) content.length)
                .build();
        // 上传文件
        client.putObject(putRequest, RequestBody.fromBytes(content));
        // 拼接返回路径
        return presignGetUrl(path, null);
    }

    @Override
    public String upload(InputStream content, long size, String path, String type) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(config.getBucket())
                .key(path)
                .contentType(type)
                .contentLength(size)
                .build();
        client.putObject(putRequest, RequestBody.fromInputStream(content, size));
        return presignGetUrl(path, null);
    }

    @Override
    public String upload(Path content, long size, String path, String type) throws Exception {
        requireUploadPath(content, size);
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(config.getBucket())
                .key(path)
                .contentType(type)
                .contentLength(size)
                .build();
        client.putObject(putRequest, RequestBody.fromFile(content));
        return presignGetUrl(path, null);
    }

    @Override
    public StorageRetentionEvidence uploadWithStorageRetention(byte[] content, String path, String type,
                                                              StorageRetentionPolicy policy) {
        StorageRetentionPolicy effectivePolicy = requireStorageRetentionPolicy(policy);
        Instant retainUntil = resolveRetainUntil(effectivePolicy);
        PutObjectRequest.Builder putRequestBuilder = PutObjectRequest.builder()
                .bucket(config.getBucket())
                .key(path)
                .contentType(type)
                .contentLength((long) content.length);
        if (Boolean.TRUE.equals(effectivePolicy.getObjectLockRequired())) {
            putRequestBuilder.objectLockMode(toObjectLockMode(effectivePolicy.getRetentionMode()))
                    .objectLockRetainUntilDate(retainUntil);
            if (Boolean.TRUE.equals(effectivePolicy.getLegalHoldRequired())) {
                putRequestBuilder.objectLockLegalHoldStatus(ObjectLockLegalHoldStatus.ON);
            }
        }

        PutObjectResponse putResponse = client.putObject(putRequestBuilder.build(), RequestBody.fromBytes(content));
        String versionId = putResponse.versionId();
        if (Boolean.TRUE.equals(effectivePolicy.getObjectLockRequired()) && StrUtil.isEmpty(versionId)) {
            throw new IllegalStateException("S3 Object Lock 上传后未返回 object versionId，无法形成存储保留证据");
        }

        StorageRetentionPolicy verificationPolicy = copyPolicy(effectivePolicy)
                .setObjectVersionId(versionId)
                .setRetainUntil(retainUntil);
        putObjectRetention(path, verificationPolicy);
        putObjectLegalHold(path, verificationPolicy);
        return requireStorageRetentionEvidence(path, verificationPolicy)
                .setUrl(presignGetUrl(path, null))
                .setETag(putResponse.eTag())
                .setChecksumSha256(effectivePolicy.getChecksumSha256());
    }

    @Override
    public String move(String sourcePath, String targetPath, String type) {
        CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .bucket(config.getBucket())
                .copySource(buildCopySource(sourcePath))
                .key(targetPath)
                .build();
        client.copyObject(copyRequest);
        delete(sourcePath);
        return presignGetUrl(targetPath, null);
    }

    @Override
    public void delete(String path) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(config.getBucket())
                .key(path)
                .build();
        client.deleteObject(deleteRequest);
    }

    @Override
    public byte[] getContent(String path) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(config.getBucket())
                .key(path)
                .build();
        return IoUtil.readBytes(client.getObject(getRequest));
    }

    public String debugSummary() {
        return StrUtil.format("clientId={} bucket={} pathStyle={} endpoint={} domain={} region={}",
                getId(), config.getBucket(), config.getEnablePathStyleAccess(),
                config.getEndpoint(), config.getDomain(), config.getRegion());
    }

    public byte[] getContentWithFreshClient(String path) {
        S3FileClientConfig clonedConfig = JsonUtils.parseObject2(JsonUtils.toJsonString(config), S3FileClientConfig.class);
        S3FileClient freshClient = new S3FileClient(getId(), clonedConfig);
        freshClient.init();
        return freshClient.getContent(path);
    }

    private String buildCopySource(String sourcePath) {
        return config.getBucket() + "/" + encodeS3ObjectKey(sourcePath);
    }

    private String encodeS3ObjectKey(String sourcePath) {
        return Arrays.stream(sourcePath.split("/", -1))
                .map(segment -> URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"))
                .collect(Collectors.joining("/"));
    }

    @Override
    public byte[] getContentWithStorageRetention(String path, StorageRetentionPolicy policy) throws Exception {
        StorageRetentionPolicy effectivePolicy = requireStorageRetentionPolicy(policy);
        requireStorageRetentionEvidence(path, effectivePolicy);
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(config.getBucket())
                .key(path)
                .versionId(effectivePolicy.getObjectVersionId())
                .build();
        try (ResponseInputStream<GetObjectResponse> response = client.getObject(getRequest)) {
            String returnedVersionId = response.response().versionId();
            if (!StrUtil.equals(effectivePolicy.getObjectVersionId(), returnedVersionId)) {
                throw new IllegalStateException("S3 Object Lock 内容读取 versionId 与存储保留证据不一致");
            }
            return IoUtil.readBytes(response);
        }
    }

    @Override
    public StorageRetentionEvidence requireStorageRetentionEvidence(String path, StorageRetentionPolicy policy) {
        StorageRetentionPolicy effectivePolicy = requireStorageRetentionPolicy(policy);
        if (Boolean.TRUE.equals(effectivePolicy.getObjectLockRequired())
                && StrUtil.isEmpty(effectivePolicy.getObjectVersionId())) {
            throw new IllegalArgumentException("S3 Object Lock 证据验证必须提供 object versionId");
        }

        GetObjectRetentionRequest retentionRequest = GetObjectRetentionRequest.builder()
                .bucket(config.getBucket())
                .key(path)
                .versionId(effectivePolicy.getObjectVersionId())
                .build();
        GetObjectRetentionResponse retentionResponse = client.getObjectRetention(retentionRequest);
        ObjectLockRetention retention = retentionResponse.retention();

        GetObjectLegalHoldRequest legalHoldRequest = GetObjectLegalHoldRequest.builder()
                .bucket(config.getBucket())
                .key(path)
                .versionId(effectivePolicy.getObjectVersionId())
                .build();
        GetObjectLegalHoldResponse legalHoldResponse = client.getObjectLegalHold(legalHoldRequest);
        ObjectLockLegalHold legalHold = legalHoldResponse.legalHold();

        String retentionMode = retention != null ? retention.modeAsString() : null;
        Instant retainUntil = retention != null ? retention.retainUntilDate() : null;
        String legalHoldStatus = legalHold != null ? legalHold.statusAsString() : null;
        validateRetentionEvidence(effectivePolicy, retentionMode, retainUntil, legalHoldStatus);
        return new StorageRetentionEvidence()
                .setClientId(getId())
                .setProvider("S3")
                .setStorageType("S3")
                .setBucket(config.getBucket())
                .setPath(path)
                .setKey(path)
                .setObjectVersionId(effectivePolicy.getObjectVersionId())
                .setRetentionMode(retentionMode)
                .setRetainUntil(retainUntil)
                .setLegalHoldStatus(legalHoldStatus)
                .setVerifiedAt(Instant.now())
                .setChecksumSha256(effectivePolicy.getChecksumSha256());
    }

    @Override
    public String presignPutUrl(String path) {
        return presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(EXPIRATION_DEFAULT)
                .putObjectRequest(b -> b.bucket(config.getBucket()).key(path)).build())
                .url().toString();
    }

    @Override
    public String presignGetUrl(String url, Integer expirationSeconds) {
        // 1. 将 url 转换为 path
        String path = resolveObjectKeyForPresign(url);

        // 2.1 情况一：公开访问：无需签名
        // 考虑到老版本的兼容，所以必须是 config.getEnablePublicAccess() 为 false 时，才进行签名
        if (!BooleanUtil.isFalse(config.getEnablePublicAccess())) {
            return config.getDomain() + "/" + path;
        }

        // 2.2 情况二：私有访问：生成 GET 预签名 URL
        String finalPath = path;
        Duration expiration = expirationSeconds != null ? Duration.ofSeconds(expirationSeconds) : EXPIRATION_DEFAULT;
        URL signedUrl = presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .getObjectRequest(b -> b.bucket(config.getBucket()).key(finalPath)).build())
                .url();
        return signedUrl.toString();
    }

    @VisibleForTesting
    String resolveObjectKeyForPresign(String urlOrPath) {
        String domain = config.getDomain();
        if (StrUtil.isEmpty(domain)) {
            return urlOrPath;
        }
        String domainPrefix = domain + "/";
        if (!StrUtil.startWith(urlOrPath, domainPrefix)) {
            return urlOrPath;
        }
        String encodedPath = StrUtil.removePrefix(urlOrPath, domainPrefix);
        return HttpUtils.decodeUrlPath(removeQueryFromObjectKey(encodedPath));
    }

    private String removeQueryFromObjectKey(String path) {
        int queryIndex = path.indexOf('?');
        return queryIndex >= 0 ? path.substring(0, queryIndex) : path;
    }

    private void requireUploadPath(Path content, long size) throws Exception {
        Objects.requireNonNull(content, "文件路径不能为空");
        if (size < 0) {
            throw new IllegalArgumentException("文件大小不能小于 0");
        }
        if (!Files.isRegularFile(content)) {
            throw new IllegalArgumentException("文件路径必须指向普通文件: " + content);
        }
        long actualSize = Files.size(content);
        if (actualSize != size) {
            throw new IllegalStateException(StrUtil.format("S3 文件路径大小不一致: expected={}, actual={}, path={}",
                    size, actualSize, content));
        }
    }

    private StorageRetentionPolicy requireStorageRetentionPolicy(StorageRetentionPolicy policy) {
        StorageRetentionPolicy effectivePolicy = policy != null ? policy : config.buildStorageRetentionPolicy();
        if (effectivePolicy == null) {
            throw new IllegalArgumentException("存储保留策略不能为空");
        }
        if (!Boolean.TRUE.equals(effectivePolicy.getObjectLockRequired())) {
            throw new IllegalArgumentException("S3 Object Lock 必须显式启用");
        }
        if (StrUtil.isEmpty(effectivePolicy.getRetentionMode())) {
            throw new IllegalArgumentException("S3 Object Lock retentionMode 不能为空");
        }
        if (effectivePolicy.getRetentionDays() == null && effectivePolicy.getRetainUntil() == null) {
            throw new IllegalArgumentException("S3 Object Lock retentionDays 或 retainUntil 必须至少填写一个");
        }
        if (effectivePolicy.getRetentionDays() != null && effectivePolicy.getRetentionDays() <= 0) {
            throw new IllegalArgumentException("S3 Object Lock retentionDays 必须大于 0");
        }
        if (effectivePolicy.getLegalHoldRequired() == null) {
            throw new IllegalArgumentException("S3 Object Lock legalHoldRequired 不能为空");
        }
        return effectivePolicy;
    }

    private StorageRetentionPolicy copyPolicy(StorageRetentionPolicy policy) {
        return new StorageRetentionPolicy()
                .setObjectLockRequired(policy.getObjectLockRequired())
                .setRetentionMode(policy.getRetentionMode())
                .setRetentionDays(policy.getRetentionDays())
                .setRetainUntil(policy.getRetainUntil())
                .setLegalHoldRequired(policy.getLegalHoldRequired())
                .setObjectVersionId(policy.getObjectVersionId())
                .setChecksumSha256(policy.getChecksumSha256());
    }

    private Instant resolveRetainUntil(StorageRetentionPolicy policy) {
        if (policy.getRetainUntil() != null) {
            return ceilToSecond(policy.getRetainUntil());
        }
        if (policy.getRetentionDays() == null) {
            throw new IllegalArgumentException("S3 Object Lock retainUntil 不能为空");
        }
        return ceilToSecond(Instant.now().plus(Duration.ofDays(policy.getRetentionDays())));
    }

    private Instant ceilToSecond(Instant instant) {
        Instant truncated = instant.truncatedTo(ChronoUnit.SECONDS);
        return truncated.equals(instant) ? truncated : truncated.plusSeconds(1);
    }

    private void putObjectRetention(String path, StorageRetentionPolicy policy) {
        ObjectLockRetention retention = ObjectLockRetention.builder()
                .mode(toObjectLockRetentionMode(policy.getRetentionMode()))
                .retainUntilDate(resolveRetainUntil(policy))
                .build();
        PutObjectRetentionRequest request = PutObjectRetentionRequest.builder()
                .bucket(config.getBucket())
                .key(path)
                .versionId(policy.getObjectVersionId())
                .retention(retention)
                .build();
        client.putObjectRetention(request);
    }

    private void putObjectLegalHold(String path, StorageRetentionPolicy policy) {
        if (!Boolean.TRUE.equals(policy.getLegalHoldRequired())) {
            return;
        }
        ObjectLockLegalHold legalHold = ObjectLockLegalHold.builder()
                .status(ObjectLockLegalHoldStatus.ON)
                .build();
        PutObjectLegalHoldRequest request = PutObjectLegalHoldRequest.builder()
                .bucket(config.getBucket())
                .key(path)
                .versionId(policy.getObjectVersionId())
                .legalHold(legalHold)
                .build();
        client.putObjectLegalHold(request);
    }

    private void validateRetentionEvidence(StorageRetentionPolicy policy, String retentionMode,
                                           Instant retainUntil, String legalHoldStatus) {
        if (!StrUtil.equals(policy.getRetentionMode(), retentionMode)) {
            throw new IllegalStateException("S3 Object Lock retentionMode 与策略不一致");
        }
        if (retainUntil == null || !retainUntil.isAfter(Instant.now())) {
            throw new IllegalStateException("S3 Object Lock retainUntil 缺失或已过期");
        }
        if (policy.getRetainUntil() != null && retainUntil.isBefore(ceilToSecond(policy.getRetainUntil()))) {
            throw new IllegalStateException("S3 Object Lock retainUntil 早于策略要求");
        }
        if (Boolean.TRUE.equals(policy.getLegalHoldRequired())
                && !StrUtil.equals(ObjectLockLegalHoldStatus.ON.toString(), legalHoldStatus)) {
            throw new IllegalStateException("S3 Object Lock legal hold 未开启");
        }
    }

    private ObjectLockMode toObjectLockMode(String retentionMode) {
        ObjectLockMode mode = ObjectLockMode.fromValue(retentionMode);
        if (ObjectLockMode.UNKNOWN_TO_SDK_VERSION.equals(mode)) {
            throw new IllegalArgumentException("未知的 S3 Object Lock retentionMode：" + retentionMode);
        }
        return mode;
    }

    private ObjectLockRetentionMode toObjectLockRetentionMode(String retentionMode) {
        ObjectLockRetentionMode mode = ObjectLockRetentionMode.fromValue(retentionMode);
        if (ObjectLockRetentionMode.UNKNOWN_TO_SDK_VERSION.equals(mode)) {
            throw new IllegalArgumentException("未知的 S3 Object Lock retentionMode：" + retentionMode);
        }
        return mode;
    }

    /**
     * 基于 bucket + endpoint 构建访问的 Domain 地址
     *
     * @return Domain 地址
     */
    private String buildDomain() {
        // 如果已经是 http 或者 https，则不进行拼接.主要适配 MinIO
        if (HttpUtil.isHttp(config.getEndpoint()) || HttpUtil.isHttps(config.getEndpoint())) {
            return StrUtil.format("{}/{}", config.getEndpoint(), config.getBucket());
        }
        // 阿里云、腾讯云、华为云都适合。七牛云比较特殊，必须有自定义域名
        return StrUtil.format("https://{}.{}", config.getBucket(), config.getEndpoint());
    }

    /**
     * 节点地址补全协议头
     *
     * @return 节点地址
     */
    private String buildEndpoint() {
        // 如果已经是 http 或者 https，则不进行拼接
        if (HttpUtil.isHttp(config.getEndpoint()) || HttpUtil.isHttps(config.getEndpoint())) {
            return config.getEndpoint();
        }
        return StrUtil.format("https://{}", config.getEndpoint());
    }

    /**
     * presigner 节点地址
     *
     * @return 节点地址
     */
    private String buildPresignerEndpoint() {
        // 补全 domain
        if (StrUtil.isEmpty(config.getDomain())) {
            config.setDomain(buildDomain());
        }

        if (Boolean.TRUE.equals(config.getEnablePathStyleAccess())) {
            return StrUtil.removeSuffix(config.getDomain(), StrUtil.format("/{}", config.getBucket()));
        }
        return StrUtil.replace(config.getDomain(), StrUtil.format("://{}.", config.getBucket()), "://");
    }

    /**
     * 解析 AWS 区域
     * 优先级：配置的 region > 从 endpoint 解析的 region > 默认值 us-east-1
     *
     * @return 区域字符串
     */
    private String resolveRegion() {
        // 1. 如果配置了 region，直接使用
        if (StrUtil.isNotEmpty(config.getRegion())) {
            return config.getRegion();
        }

        // 2.1 尝试从 endpoint 中解析 region
        String endpoint = config.getEndpoint();
        if (StrUtil.isEmpty(endpoint)) {
            return "us-east-1";
        }

        // 2.2 移除协议头（http:// 或 https://）
        String host = endpoint;
        if (HttpUtil.isHttp(endpoint) || HttpUtil.isHttps(endpoint)) {
            try {
                host = URI.create(endpoint).getHost();
            } catch (Exception e) {
                // 解析失败，使用默认值
                return "us-east-1";
            }
        }
        if (StrUtil.isEmpty(host)) {
            return "us-east-1";
        }

        // 3.1 AWS S3 格式：s3.us-west-2.amazonaws.com 或 s3.amazonaws.com
        if (host.contains("amazonaws.com")) {
            // 匹配 s3.{region}.amazonaws.com 格式
            if (host.startsWith("s3.") && host.contains(".amazonaws.com")) {
                String regionPart = host.substring(3, host.indexOf(".amazonaws.com"));
                if (StrUtil.isNotEmpty(regionPart) && !regionPart.equals("accelerate")) {
                    return regionPart;
                }
            }
            // s3.amazonaws.com 或 s3-accelerate.amazonaws.com 使用默认值
            return "us-east-1";
        }
        // 3.2 阿里云 OSS 格式：oss-cn-beijing.aliyuncs.com
        if (host.contains(S3FileClientConfig.ENDPOINT_ALIYUN)) {
            // 匹配 oss-{region}.aliyuncs.com 格式
            if (host.startsWith("oss-") && host.contains("." + S3FileClientConfig.ENDPOINT_ALIYUN)) {
                String regionPart = host.substring(4, host.indexOf("." + S3FileClientConfig.ENDPOINT_ALIYUN));
                if (StrUtil.isNotEmpty(regionPart)) {
                    return regionPart;
                }
            }
        }
        // 3.3 腾讯云 COS 格式：cos.ap-shanghai.myqcloud.com
        if (host.contains(S3FileClientConfig.ENDPOINT_TENCENT)) {
            // 匹配 cos.{region}.myqcloud.com 格式
            if (host.startsWith("cos.") && host.contains("." + S3FileClientConfig.ENDPOINT_TENCENT)) {
                String regionPart = host.substring(4, host.indexOf("." + S3FileClientConfig.ENDPOINT_TENCENT));
                if (StrUtil.isNotEmpty(regionPart)) {
                    return regionPart;
                }
            }
        }

        // 3.4 其他情况（MinIO、七牛云等）使用默认值
        return "us-east-1";
    }

}
