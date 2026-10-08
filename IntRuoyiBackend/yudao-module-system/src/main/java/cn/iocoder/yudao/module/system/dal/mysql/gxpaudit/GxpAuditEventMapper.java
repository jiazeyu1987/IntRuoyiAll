package cn.iocoder.yudao.module.system.dal.mysql.gxpaudit;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditEventPageQuery;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GxpAuditEventMapper extends BaseMapperX<GxpAuditEventDO> {

    @Select("SELECT * FROM gxp_audit_event WHERE tenant_id = #{tenantId} LIMIT 1 FOR UPDATE")
    GxpAuditEventDO selectAnyByTenantForUpdate(Long tenantId);

    default PageResult<GxpAuditEventDO> selectPage(Long tenantId, GxpAuditEventPageQuery query) {
        return selectPageByScope(tenantId, query, null, null);
    }

    default PageResult<GxpAuditEventDO> selectPageByScope(Long tenantId,
                                                            GxpAuditEventPageQuery query,
                                                            java.util.List<Long> eventIds,
                                                            java.util.List<String> subjectIds) {
        LambdaQueryWrapperX<GxpAuditEventDO> wrapper = new LambdaQueryWrapperX<GxpAuditEventDO>()
                .eq(GxpAuditEventDO::getTenantId, tenantId)
                .eqIfPresent(GxpAuditEventDO::getDomain, query.getDomain())
                .eqIfPresent(GxpAuditEventDO::getSubjectType, query.getSubjectType())
                .eqIfPresent(GxpAuditEventDO::getSubjectId, query.getSubjectId())
                .eqIfPresent(GxpAuditEventDO::getOperationId, query.getOperationId())
                .eqIfPresent(GxpAuditEventDO::getAction, query.getAction())
                .eqIfPresent(GxpAuditEventDO::getResultStatus, query.getResultStatus())
                .eqIfPresent(GxpAuditEventDO::getActorId, query.getActorId())
                .orderByDesc(GxpAuditEventDO::getLedgerSequence);
        if (eventIds != null || subjectIds != null) {
            // Object-scoped pages are summaries. Full state and canonical bytes remain available
            // through selectByTenantIdAndId; do not load their LONGTEXT payloads for every list row.
            wrapper.select(GxpAuditEventDO.class, field ->
                    !"beforeStateJson".equals(field.getProperty())
                            && !"afterStateJson".equals(field.getProperty())
                            && !"canonicalEventJson".equals(field.getProperty()));
            if (eventIds == null || eventIds.isEmpty()) {
                if (subjectIds == null || subjectIds.isEmpty()) {
                    return PageResult.empty();
                }
                wrapper.in(GxpAuditEventDO::getSubjectId, subjectIds);
            } else if (subjectIds == null || subjectIds.isEmpty()) {
                wrapper.in(GxpAuditEventDO::getId, eventIds);
            } else {
                wrapper.and(item -> item.in(GxpAuditEventDO::getId, eventIds)
                        .or().in(GxpAuditEventDO::getSubjectId, subjectIds));
            }
        }
        if (query.getSignaturePresent() != null) {
            if (Boolean.TRUE.equals(query.getSignaturePresent())) {
                wrapper.isNotNull(GxpAuditEventDO::getSignatureRecordId)
                        .ne(GxpAuditEventDO::getSignatureRecordId, "");
            } else {
                wrapper.and(item -> item.isNull(GxpAuditEventDO::getSignatureRecordId)
                        .or().eq(GxpAuditEventDO::getSignatureRecordId, ""));
            }
        }
        if (query.getOccurredAt() != null && query.getOccurredAt().length == 2) {
            wrapper.between(GxpAuditEventDO::getServerOccurredAt,
                    query.getOccurredAt()[0], query.getOccurredAt()[1]);
        }
        return selectPage(query, wrapper);
    }

    @Select("SELECT * FROM gxp_audit_event WHERE tenant_id = #{tenantId} AND id = #{eventId}")
    GxpAuditEventDO selectByTenantIdAndId(Long tenantId, Long eventId);

    @Select("SELECT COALESCE(MAX(ledger_sequence), 0) FROM gxp_audit_event WHERE tenant_id = #{tenantId}")
    Long selectMaxLedgerSequence(Long tenantId);

    default GxpAuditEventDO selectLatestByTenantForUpdate(Long tenantId) {
        return selectOne(new LambdaQueryWrapperX<GxpAuditEventDO>()
                .eq(GxpAuditEventDO::getTenantId, tenantId)
                .orderByDesc(GxpAuditEventDO::getLedgerSequence)
                .last("LIMIT 1 FOR UPDATE"));
    }

    default GxpAuditEventDO selectByIdempotencyKeyForUpdate(Long tenantId, String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<GxpAuditEventDO>()
                .eq(GxpAuditEventDO::getTenantId, tenantId)
                .eq(GxpAuditEventDO::getIdempotencyKey, idempotencyKey)
                .last("FOR UPDATE"));
    }

    default GxpAuditEventDO selectLatestByTenant(Long tenantId) {
        return selectOne(new LambdaQueryWrapperX<GxpAuditEventDO>()
                .eq(GxpAuditEventDO::getTenantId, tenantId)
                .orderByDesc(GxpAuditEventDO::getLedgerSequence)
                .last("LIMIT 1"));
    }

    default GxpAuditEventDO selectByIdempotencyKey(Long tenantId, String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<GxpAuditEventDO>()
                .eq(GxpAuditEventDO::getTenantId, tenantId)
                .eq(GxpAuditEventDO::getIdempotencyKey, idempotencyKey));
    }

}
