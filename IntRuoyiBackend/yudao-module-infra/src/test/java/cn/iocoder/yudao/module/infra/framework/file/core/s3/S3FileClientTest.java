package cn.iocoder.yudao.module.infra.framework.file.core.s3;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.framework.common.util.validation.ValidationUtils;
import cn.iocoder.yudao.module.infra.framework.file.core.client.StorageRetentionEvidence;
import cn.iocoder.yudao.module.infra.framework.file.core.client.StorageRetentionPolicy;
import cn.iocoder.yudao.module.infra.framework.file.core.client.s3.S3FileClient;
import cn.iocoder.yudao.module.infra.framework.file.core.client.s3.S3FileClientConfig;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectLegalHoldRequest;
import software.amazon.awssdk.services.s3.model.GetObjectLegalHoldResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRetentionRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRetentionResponse;
import software.amazon.awssdk.services.s3.model.ObjectLockLegalHold;
import software.amazon.awssdk.services.s3.model.ObjectLockLegalHoldStatus;
import software.amazon.awssdk.services.s3.model.ObjectLockRetention;
import software.amazon.awssdk.services.s3.model.ObjectLockRetentionMode;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("resource")
public class S3FileClientTest {

    @Test
    public void testStorageRetentionContractMethods_exist() throws Exception {
        assertEquals(StorageRetentionEvidence.class, S3FileClient.class
                .getMethod("uploadWithStorageRetention", byte[].class, String.class, String.class, StorageRetentionPolicy.class)
                .getReturnType());
        assertEquals(StorageRetentionEvidence.class, S3FileClient.class
                .getMethod("requireStorageRetentionEvidence", String.class, StorageRetentionPolicy.class)
                .getReturnType());
        assertEquals(byte[].class, S3FileClient.class
                .getMethod("getContentWithStorageRetention", String.class, StorageRetentionPolicy.class)
                .getReturnType());
    }

    @Test
    public void testS3FileClientConfigWithoutObjectLockValidation_success() {
        S3FileClientConfig config = buildBaseConfig();

        assertDoesNotThrow(() -> ValidationUtils.validate(Validation.buildDefaultValidatorFactory().getValidator(), config));
    }

    @Test
    public void testS3FileClientConfigRetentionValidation_success() {
        S3FileClientConfig config = buildBaseConfig();
        config.setObjectLockRequired(true);
        config.setRetentionMode("COMPLIANCE");
        config.setRetentionDays(365);
        config.setLegalHoldRequired(true);

        assertDoesNotThrow(() -> ValidationUtils.validate(Validation.buildDefaultValidatorFactory().getValidator(), config));
    }

    @Test
    public void testS3FileClientConfigRetentionValidation_missingPolicyFailFast() {
        S3FileClientConfig config = buildBaseConfig();
        config.setObjectLockRequired(true);

        assertThrows(ConstraintViolationException.class,
                () -> ValidationUtils.validate(Validation.buildDefaultValidatorFactory().getValidator(), config));
    }

    @Test
    public void testS3FileClientConfigToString_hidesAccessSecret() {
        S3FileClientConfig config = buildBaseConfig();
        config.setObjectLockRequired(true);
        config.setRetentionMode("COMPLIANCE");
        config.setRetentionDays(365);
        config.setLegalHoldRequired(true);

        assertFalse(config.toString().contains("secret-value"));
    }

    @Test
    public void testUploadWithStorageRetention_missingPolicyFailFast() {
        S3FileClientConfig config = buildBaseConfig();
        S3FileClient client = new S3FileClient(0L, config);

        assertThrows(IllegalArgumentException.class,
                () -> client.uploadWithStorageRetention(new byte[]{1}, "archive/eDHR.pdf", "application/pdf", null));
    }

    @Test
    public void testUploadWithStorageRetention_retentionUntilUsesSecondPrecision() {
        S3FileClientConfig config = buildBaseConfig();
        config.setObjectLockRequired(true);
        config.setRetentionMode("COMPLIANCE");
        config.setRetentionDays(7);
        config.setLegalHoldRequired(true);
        config.setDomain("http://127.0.0.1:9000/edhr-archive");
        config.setEnablePublicAccess(true);
        S3FileClient client = new S3FileClient(0L, config);
        S3Client s3Client = mock(S3Client.class);
        ReflectionTestUtils.setField(client, "client", s3Client);
        ReflectionTestUtils.setField(client, "presigner", null);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(PutObjectResponse.builder()
                .versionId("version-1")
                .build());
        when(s3Client.getObjectRetention(any(GetObjectRetentionRequest.class))).thenReturn(
                GetObjectRetentionResponse.builder()
                        .retention(ObjectLockRetention.builder()
                                .mode(ObjectLockRetentionMode.COMPLIANCE)
                                .retainUntilDate(Instant.parse("2036-05-28T00:00:00Z"))
                                .build())
                        .build());
        when(s3Client.getObjectLegalHold(any(GetObjectLegalHoldRequest.class))).thenReturn(legalHoldResponse());

        client.uploadWithStorageRetention(new byte[]{1}, "archive/eDHR.pdf", "application/pdf", null);

        ArgumentCaptor<PutObjectRequest> putObjectCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(putObjectCaptor.capture(), any(RequestBody.class));
        assertEquals(0, putObjectCaptor.getValue().objectLockRetainUntilDate().getNano());
    }

    @Test
    public void testUploadWithStorageRetention_preservesPolicyChecksumWhenResponseOmitsChecksum() {
        S3FileClientConfig config = buildBaseConfig();
        config.setObjectLockRequired(true);
        config.setRetentionMode("COMPLIANCE");
        config.setRetentionDays(7);
        config.setLegalHoldRequired(true);
        config.setDomain("http://127.0.0.1:9000/edhr-archive");
        config.setEnablePublicAccess(true);
        S3FileClient client = new S3FileClient(0L, config);
        S3Client s3Client = mock(S3Client.class);
        ReflectionTestUtils.setField(client, "client", s3Client);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().versionId("version-1").build());
        when(s3Client.getObjectRetention(any(GetObjectRetentionRequest.class))).thenReturn(
                GetObjectRetentionResponse.builder()
                        .retention(ObjectLockRetention.builder()
                                .mode(ObjectLockRetentionMode.COMPLIANCE)
                                .retainUntilDate(Instant.parse("2036-05-28T00:00:00Z"))
                                .build())
                        .build());
        when(s3Client.getObjectLegalHold(any(GetObjectLegalHoldRequest.class))).thenReturn(legalHoldResponse());
        StorageRetentionPolicy policy = buildRetentionPolicy().setRetainUntil(null).setRetentionDays(7);

        StorageRetentionEvidence evidence = client.uploadWithStorageRetention(
                new byte[]{1}, "archive/eDHR.pdf", "application/pdf", policy);

        assertEquals("sha256", evidence.getChecksumSha256());
    }

    @Test
    public void testGetContentWithStorageRetention_usesSameObjectVersionId() throws Exception {
        S3FileClient client = new S3FileClient(0L, buildBaseConfig());
        S3Client s3Client = mock(S3Client.class);
        ReflectionTestUtils.setField(client, "client", s3Client);
        byte[] protectedContent = new byte[]{1, 2, 3};
        StorageRetentionPolicy policy = buildRetentionPolicy();
        when(s3Client.getObjectRetention(any(GetObjectRetentionRequest.class))).thenReturn(retentionResponse());
        when(s3Client.getObjectLegalHold(any(GetObjectLegalHoldRequest.class))).thenReturn(legalHoldResponse());
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream(policy.getObjectVersionId(),
                protectedContent));

        byte[] result = client.getContentWithStorageRetention("archive/eDHR.pdf", policy);

        assertArrayEquals(protectedContent, result);
        ArgumentCaptor<GetObjectRequest> getCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(getCaptor.capture());
        assertEquals("archive/eDHR.pdf", getCaptor.getValue().key());
        assertEquals(policy.getObjectVersionId(), getCaptor.getValue().versionId());
        ArgumentCaptor<GetObjectRetentionRequest> retentionCaptor =
                ArgumentCaptor.forClass(GetObjectRetentionRequest.class);
        verify(s3Client).getObjectRetention(retentionCaptor.capture());
        assertEquals(policy.getObjectVersionId(), retentionCaptor.getValue().versionId());
        ArgumentCaptor<GetObjectLegalHoldRequest> legalHoldCaptor =
                ArgumentCaptor.forClass(GetObjectLegalHoldRequest.class);
        verify(s3Client).getObjectLegalHold(legalHoldCaptor.capture());
        assertEquals(policy.getObjectVersionId(), legalHoldCaptor.getValue().versionId());
    }

    @Test
    public void testGetContentWithStorageRetention_responseVersionMismatchFailFast() {
        S3FileClient client = new S3FileClient(0L, buildBaseConfig());
        S3Client s3Client = mock(S3Client.class);
        ReflectionTestUtils.setField(client, "client", s3Client);
        StorageRetentionPolicy policy = buildRetentionPolicy();
        when(s3Client.getObjectRetention(any(GetObjectRetentionRequest.class))).thenReturn(retentionResponse());
        when(s3Client.getObjectLegalHold(any(GetObjectLegalHoldRequest.class))).thenReturn(legalHoldResponse());
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream("latest-version",
                new byte[]{9}));

        assertThrows(IllegalStateException.class,
                () -> client.getContentWithStorageRetention("archive/eDHR.pdf", policy));
    }

    @Test
    public void testUploadPath_usesFileRequestBody() throws Exception {
        S3FileClientConfig config = buildBaseConfig();
        config.setDomain("http://127.0.0.1:9000/edhr-archive");
        config.setEnablePublicAccess(true);
        S3FileClient client = new S3FileClient(0L, config);
        S3Client s3Client = mock(S3Client.class);
        ReflectionTestUtils.setField(client, "client", s3Client);
        byte[] content = "manual-content".getBytes(StandardCharsets.UTF_8);
        Path contentPath = Files.createTempFile("s3-path-upload-", ".pdf");
        Files.write(contentPath, content);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        try {
            String url = client.upload(contentPath, content.length, "dcc/original/Manual.pdf", "application/pdf");

            assertEquals("http://127.0.0.1:9000/edhr-archive/dcc/original/Manual.pdf", url);
            ArgumentCaptor<PutObjectRequest> putObjectCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
            ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
            verify(s3Client).putObject(putObjectCaptor.capture(), bodyCaptor.capture());
            assertEquals((long) content.length, putObjectCaptor.getValue().contentLength());
            assertEquals("dcc/original/Manual.pdf", putObjectCaptor.getValue().key());
            try (InputStream inputStream = bodyCaptor.getValue().contentStreamProvider().newStream()) {
                assertArrayEquals(content, inputStream.readAllBytes());
            }
        } finally {
            Files.deleteIfExists(contentPath);
        }
    }

    @Test
    @Disabled // MinIO，如果要集成测试，可以注释本行
    public void testMinIO() throws Exception {
        S3FileClientConfig config = new S3FileClientConfig();
        // 配置成你自己的
        config.setAccessKey("admin");
        config.setAccessSecret("password");
        config.setBucket("yudaoyuanma");
        config.setDomain(null);
        // 默认 9000 endpoint
        config.setEndpoint("http://127.0.0.1:9000");

        // 执行上传
        testExecuteUpload(config);
    }

    @Test
    @Disabled // 阿里云 OSS，如果要集成测试，可以注释本行
    public void testAliyun() throws Exception {
        S3FileClientConfig config = new S3FileClientConfig();
        // 配置成你自己的
        config.setAccessKey(System.getenv("ALIYUN_ACCESS_KEY"));
        config.setAccessSecret(System.getenv("ALIYUN_SECRET_KEY"));
        config.setBucket("yunai-aoteman");
        config.setDomain(null); // 如果有自定义域名，则可以设置。http://ali-oss.iocoder.cn
        // 默认北京的 endpoint
        config.setEndpoint("oss-cn-beijing.aliyuncs.com");

        // 执行上传
        testExecuteUpload(config);
    }

    @Test
    @Disabled // 腾讯云 COS，如果要集成测试，可以注释本行
    public void testQCloud() throws Exception {
        S3FileClientConfig config = new S3FileClientConfig();
        // 配置成你自己的
        config.setAccessKey(System.getenv("QCLOUD_ACCESS_KEY"));
        config.setAccessSecret(System.getenv("QCLOUD_SECRET_KEY"));
        config.setBucket("aoteman-1255880240");
        config.setDomain(null); // 如果有自定义域名，则可以设置。http://tengxun-oss.iocoder.cn
        // 默认上海的 endpoint
        config.setEndpoint("cos.ap-shanghai.myqcloud.com");

        // 执行上传
        testExecuteUpload(config);
    }

    @Test
    @Disabled // 七牛云存储，如果要集成测试，可以注释本行
    public void testQiniu() throws Exception {
        S3FileClientConfig config = new S3FileClientConfig();
        // 配置成你自己的
//        config.setAccessKey(System.getenv("QINIU_ACCESS_KEY"));
//        config.setAccessSecret(System.getenv("QINIU_SECRET_KEY"));
        config.setAccessKey("b7yvuhBSAGjmtPhMFcn9iMOxUOY_I06cA_p0ZUx8");
        config.setAccessSecret("kXM1l5ia1RvSX3QaOEcwI3RLz3Y2rmNszWonKZtP");
        config.setBucket("ruoyi-vue-pro");
        config.setDomain("http://test.yudao.iocoder.cn"); // 如果有自定义域名，则可以设置。http://static.yudao.iocoder.cn
        config.setEnablePathStyleAccess(false);
        // 默认上海的 endpoint
        config.setEndpoint("s3-cn-south-1.qiniucs.com");

        // 执行上传
        testExecuteUpload(config);
    }

    @Test
    @Disabled // 七牛云存储（读私有桶），如果要集成测试，可以注释本行
    public void testQiniu_privateGet() {
        S3FileClientConfig config = new S3FileClientConfig();
        // 配置成你自己的
//        config.setAccessKey(System.getenv("QINIU_ACCESS_KEY"));
//        config.setAccessSecret(System.getenv("QINIU_SECRET_KEY"));
        config.setAccessKey("b7yvuhBSAGjmtPhMFcn9iMOxUOY_I06cA_p0ZUx8");
        config.setAccessSecret("kXM1l5ia1RvSX3QaOEcwI3RLz3Y2rmNszWonKZtP");
        config.setBucket("ruoyi-vue-pro-private");
        config.setDomain("http://t151glocd.hn-bkt.clouddn.com"); // 如果有自定义域名，则可以设置。http://static.yudao.iocoder.cn
        config.setEnablePathStyleAccess(false);
        // 默认上海的 endpoint
        config.setEndpoint("s3-cn-south-1.qiniucs.com");

        // 校验配置
        ValidationUtils.validate(Validation.buildDefaultValidatorFactory().getValidator(), config);
        // 创建 Client
        S3FileClient client = new S3FileClient(0L, config);
        client.init();
        // 执行生成 URL 签名
        String path = "output.png";
        String presignedUrl = client.presignGetUrl(path, 300);
        System.out.println(presignedUrl);
    }

    @Test
    @Disabled // 华为云存储，如果要集成测试，可以注释本行
    public void testHuaweiCloud() throws Exception {
        S3FileClientConfig config = new S3FileClientConfig();
        // 配置成你自己的
//        config.setAccessKey(System.getenv("HUAWEI_CLOUD_ACCESS_KEY"));
//        config.setAccessSecret(System.getenv("HUAWEI_CLOUD_SECRET_KEY"));
        config.setBucket("yudao");
        config.setDomain(null); // 如果有自定义域名，则可以设置。
        // 默认上海的 endpoint
        config.setEndpoint("obs.cn-east-3.myhuaweicloud.com");

        // 执行上传
        testExecuteUpload(config);
    }

    private void testExecuteUpload(S3FileClientConfig config) {
        // 校验配置
        ValidationUtils.validate(Validation.buildDefaultValidatorFactory().getValidator(), config);
        // 创建 Client
        S3FileClient client = new S3FileClient(0L, config);
        client.init();
        // 上传文件
        String path = IdUtil.fastSimpleUUID() + ".jpg";
        byte[] content = ResourceUtil.readBytes("file/erweima.jpg");
        String fullPath = client.upload(content, path, "image/jpeg");
        System.out.println("访问地址：" + fullPath);
        // 读取文件
        if (true) {
            byte[] bytes = client.getContent(path);
            System.out.println("文件内容：" + bytes.length);
        }
        // 删除文件
        if (false) {
            client.delete(path);
        }
    }

    private S3FileClientConfig buildBaseConfig() {
        S3FileClientConfig config = new S3FileClientConfig();
        config.setAccessKey("access-key");
        config.setAccessSecret("secret-value");
        config.setBucket("edhr-archive");
        config.setDomain(null);
        config.setEndpoint("http://127.0.0.1:9000");
        config.setEnablePathStyleAccess(true);
        config.setEnablePublicAccess(false);
        config.setRegion("us-east-1");
        return config;
    }

    private StorageRetentionPolicy buildRetentionPolicy() {
        return new StorageRetentionPolicy()
                .setObjectLockRequired(true)
                .setRetentionMode("COMPLIANCE")
                .setRetainUntil(Instant.parse("2036-05-28T00:00:00Z"))
                .setLegalHoldRequired(true)
                .setObjectVersionId("version-1")
                .setChecksumSha256("sha256");
    }

    private GetObjectRetentionResponse retentionResponse() {
        return GetObjectRetentionResponse.builder()
                .retention(ObjectLockRetention.builder()
                        .mode(ObjectLockRetentionMode.COMPLIANCE)
                        .retainUntilDate(Instant.parse("2036-05-28T00:00:00Z"))
                        .build())
                .build();
    }

    private GetObjectLegalHoldResponse legalHoldResponse() {
        return GetObjectLegalHoldResponse.builder()
                .legalHold(ObjectLockLegalHold.builder()
                        .status(ObjectLockLegalHoldStatus.ON)
                        .build())
                .build();
    }

    private ResponseInputStream<GetObjectResponse> responseStream(String versionId, byte[] content) {
        return new ResponseInputStream<>(GetObjectResponse.builder().versionId(versionId).build(),
                AbortableInputStream.create(new ByteArrayInputStream(content)));
    }

}
