package cn.iocoder.yudao.module.dcc.signature.service.retention;

public interface SignatureGovernanceRetentionObjectStore {

    SignatureGovernanceRetentionBucketState readBucketState();

    SignatureGovernanceRetentionStoredObject readObject(String objectKey, String versionId);
}
