package cn.iocoder.yudao.module.system.api.signature;

/** Required synchronous authorization write in the user provisioning transaction. */
public interface UserSignatureAuthorizationApi {

    /** Grants signature authorization with an audit reason; repeated grants are idempotent. */
    void authorizeProvisionedUser(Long userId, String reason);
}
