package cn.iocoder.yudao.module.mes.controller.admin.pro.gxpaudit;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.controller.admin.gxpaudit.vo.GxpAuditEventPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.gxpaudit.vo.GxpAuditEventRelationRespVO;
import cn.iocoder.yudao.module.system.controller.admin.gxpaudit.vo.GxpAuditEventRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventRelationDO;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditEventPageQuery;

import java.util.List;
import java.util.Objects;

public final class MesGxpAuditControllerSupport {

    private MesGxpAuditControllerSupport() {
    }

    public static GxpAuditEventPageQuery toQuery(GxpAuditEventPageReqVO reqVO,
                                                  String scopeType,
                                                  Long scopeId) {
        GxpAuditEventPageQuery query = new GxpAuditEventPageQuery()
                .setScopeType(scopeType)
                .setScopeId(scopeId)
                .setDomain(reqVO.getDomain())
                .setSubjectType(reqVO.getSubjectType())
                .setSubjectId(reqVO.getSubjectId())
                .setOperationId(reqVO.getOperationId())
                .setAction(reqVO.getAction())
                .setResultStatus(reqVO.getResultStatus())
                .setActorId(reqVO.getActorId())
                .setSignaturePresent(reqVO.getSignaturePresent())
                .setOccurredAt(reqVO.getOccurredAt());
        query.setPageNo(reqVO.getPageNo());
        query.setPageSize(reqVO.getPageSize());
        return query;
    }

    public static PageResult<GxpAuditEventRespVO> toPage(PageResult<GxpAuditEventDO> page) {
        List<GxpAuditEventRespVO> list = page.getList() == null
                ? List.of()
                : page.getList().stream().map(MesGxpAuditControllerSupport::toResponse).toList();
        return new PageResult<>(list, page.getTotal());
    }

    public static GxpAuditEventRespVO toResponse(GxpAuditEventDO event) {
        GxpAuditEventRespVO response = BeanUtils.toBean(event, GxpAuditEventRespVO.class);
        response.setIntegrityStatus(event.getEventHash() == null || event.getEventHash().isBlank()
                ? "INVALID" : "NOT_VERIFIED");
        return response;
    }

    public static GxpAuditEventRespVO toResponse(GxpAuditEventDO event,
                                                  List<GxpAuditEventRelationDO> relations) {
        GxpAuditEventRespVO response = toResponse(event);
        response.setRelations(BeanUtils.toBean(relations, GxpAuditEventRelationRespVO.class));
        return response;
    }

    public static boolean containsRelation(List<GxpAuditEventRelationDO> relations,
                                           String targetType,
                                           Long targetId) {
        String expectedTargetId = String.valueOf(targetId);
        return relations != null && relations.stream().anyMatch(relation ->
                Objects.equals(targetType, relation.getTargetType())
                        && Objects.equals(expectedTargetId, relation.getTargetId()));
    }
}
