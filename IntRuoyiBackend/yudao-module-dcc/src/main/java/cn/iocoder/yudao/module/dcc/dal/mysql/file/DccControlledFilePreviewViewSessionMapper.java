package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFilePreviewViewSessionDO;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface DccControlledFilePreviewViewSessionMapper extends BaseMapperX<DccControlledFilePreviewViewSessionDO> {
    @Select("SELECT * FROM dcc_controlled_file_preview_view_session WHERE id=#{id} AND tenant_id=#{tenantId} AND deleted=0 FOR UPDATE")
    DccControlledFilePreviewViewSessionDO selectOwnedByIdForUpdate(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Select("SELECT * FROM dcc_controlled_file_preview_view_session WHERE access_event_id=#{eventId} AND user_id=#{userId} AND tenant_id=#{tenantId} AND client_session_id=#{clientId} AND deleted=0")
    DccControlledFilePreviewViewSessionDO selectClientAfterResourceLock(@Param("eventId") Long eventId,
            @Param("userId") Long userId, @Param("tenantId") Long tenantId, @Param("clientId") String clientId);

    @Select("SELECT * FROM dcc_controlled_file_preview_view_session WHERE controlled_file_id=#{fileId} AND resource_type=#{resourceType} AND user_id=#{userId} AND tenant_id=#{tenantId} AND ended_at IS NULL AND deleted=0")
    List<DccControlledFilePreviewViewSessionDO> selectActiveAfterResourceLock(@Param("fileId") Long fileId,
            @Param("resourceType") String resourceType, @Param("userId") Long userId, @Param("tenantId") Long tenantId);

    @Select("SELECT id FROM infra_file WHERE id=#{fileId} AND deleted=0 FOR UPDATE")
    Long lockInfraFile(@Param("fileId") Long fileId);

    @Select("SELECT id FROM dcc_controlled_file WHERE id=#{fileId} AND tenant_id=#{tenantId} AND deleted=0 FOR UPDATE")
    Long lockControlledFile(@Param("fileId") Long fileId, @Param("tenantId") Long tenantId);

    @Select("SELECT MIN(started_at) AS first_opened_at, MAX(started_at) AS last_opened_at, COALESCE(SUM(accumulated_seconds),0) AS accumulated_seconds, COUNT(*) AS opened_session_count FROM dcc_controlled_file_preview_view_session WHERE tenant_id=#{tenantId} AND resource_type=#{resourceType} AND controlled_file_id=#{fileId} AND file_version_no=#{version} AND user_id=#{userId} AND deleted=0")
    cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPreviewReadingSummary selectSummary(@Param("tenantId") Long tenantId,
            @Param("resourceType") String resourceType, @Param("fileId") Long fileId, @Param("version") String version,
            @Param("userId") Long userId);
}
