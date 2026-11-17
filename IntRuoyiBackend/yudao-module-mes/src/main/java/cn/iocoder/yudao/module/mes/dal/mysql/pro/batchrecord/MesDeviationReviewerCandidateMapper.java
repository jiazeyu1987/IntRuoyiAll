package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MesDeviationReviewerCandidateMapper {
    @Select("""
            SELECT id, tenant_id, username, nickname, status
            FROM system_users
            WHERE tenant_id = #{tenantId} AND status = 0 AND deleted = 0
            ORDER BY id
            """)
    List<AdminUserRespDTO> selectEnabledByTenant(@Param("tenantId") Long tenantId);
}
