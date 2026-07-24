package cn.iocoder.yudao.module.dcc.signature.service.retention;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningResponse;
import software.amazon.awssdk.services.s3.model.GetObjectLockConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetObjectLockConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRetentionRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRetentionResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectLockConfiguration;
import software.amazon.awssdk.services.s3.model.ObjectLockEnabled;
import software.amazon.awssdk.services.s3.model.ObjectLockRetention;
import software.amazon.awssdk.services.s3.model.ObjectLockRule;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;
import java.util.Map;

public class SignatureGovernanceRetentionS3ObjectStore implements SignatureGovernanceRetentionObjectStore {

    private final SignatureGovernanceRetentionS3Properties properties;
    private final S3Client client;

    public SignatureGovernanceRetentionS3ObjectStore(SignatureGovernanceRetentionS3Properties properties) {
        this(properties, buildClient(properties));
    }

    SignatureGovernanceRetentionS3ObjectStore(SignatureGovernanceRetentionS3Properties properties, S3Client client) {
        this.properties = properties;
        this.client = client;
    }

    @Override
    public SignatureGovernanceRetentionBucketState readBucketState() {
        try {
            client.headBucket(HeadBucketRequest.builder().bucket(properties.getBucketName()).build());
        } catch (NoSuchBucketException ex) {
            return new SignatureGovernanceRetentionBucketState(false, false, false, false, null, false);
        } catch (S3Exception ex) {
            throw s3Failure(SignatureGovernanceRetentionBlockerCode.PERMISSION_MISSING,
                    "Cannot read configured retention bucket: " + errorMessage(ex), ex);
        }

        GetBucketVersioningResponse versioning = getBucketVersioning();
        GetObjectLockConfigurationResponse objectLockConfiguration = getObjectLockConfiguration();
        ObjectLockConfiguration configuration = objectLockConfiguration == null ? null
                : objectLockConfiguration.objectLockConfiguration();
        ObjectLockRule rule = configuration == null ? null : configuration.rule();
        String retentionMode = rule == null || rule.defaultRetention() == null || rule.defaultRetention().mode() == null
                ? null
                : rule.defaultRetention().mode().toString();
        return new SignatureGovernanceRetentionBucketState(
                true,
                versioning.status() == BucketVersioningStatus.ENABLED,
                configuration != null && configuration.objectLockEnabled() == ObjectLockEnabled.ENABLED,
                rule != null && rule.defaultRetention() != null,
                retentionMode,
                true);
    }

    @Override
    public SignatureGovernanceRetentionStoredObject readObject(String objectKey, String versionId) {
        try {
            HeadObjectResponse head = client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .versionId(versionId)
                    .build());
            GetObjectRetentionResponse retention = client.getObjectRetention(GetObjectRetentionRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .versionId(versionId)
                    .build());
            ResponseBytes<GetObjectResponse> objectBytes = client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .versionId(versionId)
                    .build());
            ObjectLockRetention objectRetention = retention.retention();
            return new SignatureGovernanceRetentionStoredObject(
                    objectKey,
                    versionId,
                    objectRetention == null || objectRetention.mode() == null ? null : objectRetention.mode().toString(),
                    objectRetention == null ? null : objectRetention.retainUntilDate(),
                    objectBytes.asByteArray(),
                    head.metadata() == null ? Map.of() : head.metadata());
        } catch (NoSuchKeyException ex) {
            return null;
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                return null;
            }
            throw s3Failure(SignatureGovernanceRetentionBlockerCode.RETENTION_VERIFICATION_FAILED,
                    "Cannot read configured retention object: " + errorMessage(ex), ex);
        }
    }

    private GetBucketVersioningResponse getBucketVersioning() {
        try {
            return client.getBucketVersioning(GetBucketVersioningRequest.builder()
                    .bucket(properties.getBucketName())
                    .build());
        } catch (S3Exception ex) {
            throw s3Failure(SignatureGovernanceRetentionBlockerCode.PERMISSION_MISSING,
                    "Cannot read configured retention bucket versioning: " + errorMessage(ex), ex);
        }
    }

    private GetObjectLockConfigurationResponse getObjectLockConfiguration() {
        try {
            return client.getObjectLockConfiguration(GetObjectLockConfigurationRequest.builder()
                    .bucket(properties.getBucketName())
                    .build());
        } catch (S3Exception ex) {
            String errorCode = ex.awsErrorDetails() == null || ex.awsErrorDetails().errorCode() == null
                    ? "" : ex.awsErrorDetails().errorCode();
            if (ex.statusCode() == 404 || "ObjectLockConfigurationNotFoundError".equals(errorCode)) {
                return null;
            }
            throw s3Failure(SignatureGovernanceRetentionBlockerCode.PERMISSION_MISSING,
                    "Cannot read configured retention bucket Object Lock configuration: " + errorMessage(ex), ex);
        }
    }

    private static S3Client buildClient(SignatureGovernanceRetentionS3Properties properties) {
        return S3Client.builder()
                .endpointOverride(URI.create(properties.getEndpoint()))
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.isPathStyleAccess())
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
    }

    private static SignatureGovernanceRetentionObjectStoreException s3Failure(
            SignatureGovernanceRetentionBlockerCode code, String message, Throwable cause) {
        return new SignatureGovernanceRetentionObjectStoreException(code, message, cause);
    }

    private static String errorMessage(S3Exception ex) {
        if (ex.awsErrorDetails() != null && ex.awsErrorDetails().errorMessage() != null) {
            return ex.awsErrorDetails().errorMessage();
        }
        return ex.getMessage();
    }
}
