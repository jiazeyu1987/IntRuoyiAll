package cn.iocoder.yudao.module.system.dal.mysql.gxpaudit;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventRelationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GxpAuditEventRelationMapper extends BaseMapperX<GxpAuditEventRelationDO> {

    @Select({
            "<script>",
            "SELECT DISTINCT event_id FROM gxp_audit_event_relation",
            "WHERE tenant_id = #{tenantId}",
            "AND target_id = #{targetId}",
            "AND target_type IN",
            "<foreach collection='targetTypes' item='targetType' open='(' separator=',' close=')'>",
            "#{targetType}",
            "</foreach>",
            "ORDER BY event_id",
            "</script>"
    })
    List<Long> selectEventIdsByTargets(Long tenantId, List<String> targetTypes, String targetId);

    default List<GxpAuditEventRelationDO> selectListByTenantIdAndEventId(Long tenantId, Long eventId) {
        return selectList(new LambdaQueryWrapperX<GxpAuditEventRelationDO>()
                .eq(GxpAuditEventRelationDO::getTenantId, tenantId)
                .eq(GxpAuditEventRelationDO::getEventId, eventId)
                .orderByAsc(GxpAuditEventRelationDO::getId));
    }
}
