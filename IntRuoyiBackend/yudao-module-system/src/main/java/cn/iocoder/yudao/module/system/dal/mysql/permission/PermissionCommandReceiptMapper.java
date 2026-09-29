package cn.iocoder.yudao.module.system.dal.mysql.permission;

import cn.iocoder.yudao.module.system.dal.dataobject.permission.PermissionCommandReceiptDO;
import org.apache.ibatis.annotations.*;

/** No update, delete, upsert, or inherited mutable Mapper API. */
@Mapper
public interface PermissionCommandReceiptMapper {
    @Select("SELECT * FROM system_gxp_command_receipt WHERE tenant_id=#{tenantId} "
            + "AND operation_id=#{operationId} AND source_key_sha256=#{digest} FOR UPDATE")
    PermissionCommandReceiptDO selectForUpdate(@Param("tenantId") Long tenantId,
            @Param("operationId") String operationId, @Param("digest") byte[] digest);

    @Insert("""
        INSERT INTO system_gxp_command_receipt
        (id,tenant_id,operation_id,subject_id,source_key,source_key_sha256,source_payload_hash,
         identity_json,source_json,audit_event_id,audit_event_hash,result_json,created_at_utc)
        VALUES (#{id},#{tenantId},#{operationId},#{subjectId},#{sourceKey},#{sourceKeySha256},
         #{sourcePayloadHash},#{identityJson},#{sourceJson},#{auditEventId},#{auditEventHash},
         #{resultJson},#{createdAtUtc})
        """)
    int insert(PermissionCommandReceiptDO receipt);
}
