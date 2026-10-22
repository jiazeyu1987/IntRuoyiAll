package cn.iocoder.yudao.module.system.dal.mysql.permission;

import cn.iocoder.yudao.module.system.dal.dataobject.permission.ApprovalAuthorityRevisionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDateTime;

@Mapper
public interface ApprovalAuthorityRevisionMapper {
    /** Called only by the transaction that creates the formal tenant; no request-time repair. */
    @Insert("INSERT INTO system_approval_authority_revision (tenant_id, revision, updated_at) "
            + "SELECT id, 1, CURRENT_TIMESTAMP FROM system_tenant WHERE id = #{tenantId} AND deleted = 0")
    int initializeNewTenant(@Param("tenantId") Long tenantId);

    @Select("SELECT tenant_id, revision, updated_at FROM system_approval_authority_revision "
            + "WHERE tenant_id = #{tenantId} FOR UPDATE")
    ApprovalAuthorityRevisionDO selectForUpdate(@Param("tenantId") Long tenantId);

    @Update("UPDATE system_approval_authority_revision SET revision = revision + 1, updated_at = #{updatedAt} "
            + "WHERE tenant_id = #{tenantId} AND revision = #{expectedRevision}")
    int increment(@Param("tenantId") Long tenantId, @Param("expectedRevision") Long expectedRevision,
                  @Param("updatedAt") LocalDateTime updatedAt);
}
