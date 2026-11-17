package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesDeviationAdministratorRespVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/** Authoritative role and permission reads: receiver eligibility must not use permission caches. */
@Mapper
public interface MesDeviationAdministratorMapper {
    String ELIGIBLE = """
            SELECT u.id, u.username, u.nickname
            FROM system_users u
            JOIN system_user_role ur ON ur.user_id=u.id AND ur.tenant_id=u.tenant_id AND ur.deleted=0
            JOIN system_tenant t ON t.id=u.tenant_id AND t.deleted=0 AND t.status=0
              AND t.expire_time > CURRENT_TIMESTAMP
            LEFT JOIN system_tenant_package p ON p.id=t.package_id AND p.deleted=0 AND p.status=0
            JOIN system_role r ON r.id=ur.role_id AND r.tenant_id=u.tenant_id AND r.deleted=0
            WHERE u.tenant_id=#{tenantId} AND u.deleted=0 AND u.status=0
              AND r.code='deviation_admin_permission' AND r.status=0
              AND EXISTS (SELECT 1 FROM system_role_menu rm JOIN system_menu m ON m.id=rm.menu_id
                WHERE rm.role_id=r.id AND rm.tenant_id=u.tenant_id AND rm.deleted=0
                  AND m.deleted=0 AND m.status=0
                  AND (t.package_id=0 OR JSON_CONTAINS(p.menu_ids, CAST(m.id AS CHAR), '$')) AND m.permission='mes:pro-edhr-deviation:query')
              AND EXISTS (SELECT 1 FROM system_role_menu rm JOIN system_menu m ON m.id=rm.menu_id
                WHERE rm.role_id=r.id AND rm.tenant_id=u.tenant_id AND rm.deleted=0
                  AND m.deleted=0 AND m.status=0
                  AND (t.package_id=0 OR JSON_CONTAINS(p.menu_ids, CAST(m.id AS CHAR), '$')) AND m.permission='mes:pro-edhr-deviation:handle')
              AND EXISTS (SELECT 1 FROM system_role_menu rm JOIN system_menu m ON m.id=rm.menu_id
                WHERE rm.role_id=r.id AND rm.tenant_id=u.tenant_id AND rm.deleted=0
                  AND m.deleted=0 AND m.status=0
                  AND (t.package_id=0 OR JSON_CONTAINS(p.menu_ids, CAST(m.id AS CHAR), '$')) AND m.permission='mes:pro-edhr-work-task:query')
            """;

    @org.apache.ibatis.annotations.Options(useCache = false, flushCache = org.apache.ibatis.annotations.Options.FlushCachePolicy.TRUE)
    @Select("SELECT DISTINCT id,username,nickname FROM (" + ELIGIBLE + ") candidates ORDER BY id")
    List<MesDeviationAdministratorRespVO> selectCandidates(@Param("tenantId") Long tenantId);

    @org.apache.ibatis.annotations.Options(useCache = false, flushCache = org.apache.ibatis.annotations.Options.FlushCachePolicy.TRUE)
    @Select(ELIGIBLE + " AND u.id=#{userId} FOR UPDATE")
    MesDeviationAdministratorRespVO selectEligibleForUpdate(@Param("tenantId") Long tenantId,
                                                           @Param("userId") Long userId);
}
