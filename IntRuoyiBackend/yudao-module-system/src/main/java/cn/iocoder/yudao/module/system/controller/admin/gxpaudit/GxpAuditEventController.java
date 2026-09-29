package cn.iocoder.yudao.module.system.controller.admin.gxpaudit;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.controller.admin.gxpaudit.vo.GxpAuditEventPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.gxpaudit.vo.GxpAuditEventRelationRespVO;
import cn.iocoder.yudao.module.system.controller.admin.gxpaudit.vo.GxpAuditEventRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventRelationDO;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditEventPageQuery;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 统一 GxP 审计事件")
@RestController
@RequestMapping("/system/gxp-audit-event")
@Validated
public class GxpAuditEventController {

    @Resource
    private GxpAuditQueryService queryService;

    @GetMapping("/page")
    @Operation(summary = "分页查询统一 GxP 审计事件")
    @PreAuthorize("@ss.hasPermission('system:gxp-audit:query')")
    public CommonResult<PageResult<GxpAuditEventRespVO>> page(@Valid GxpAuditEventPageReqVO reqVO) {
        PageResult<GxpAuditEventDO> page = queryService.page(toQuery(reqVO));
        return success(toPageResp(page));
    }

    @GetMapping("/get")
    @Operation(summary = "查询统一 GxP 审计事件详情")
    @Parameter(name = "id", description = "事件编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:gxp-audit:query')")
    public CommonResult<GxpAuditEventRespVO> get(@RequestParam("id") Long id) {
        GxpAuditEventDO event = queryService.get(id);
        if (event == null) {
            return success(null);
        }
        return success(toResp(event, queryService.listRelations(id)));
    }

    private GxpAuditEventPageQuery toQuery(GxpAuditEventPageReqVO reqVO) {
        GxpAuditEventPageQuery query = new GxpAuditEventPageQuery()
                .setScopeType(reqVO.getScopeType())
                .setScopeId(reqVO.getScopeId())
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

    private PageResult<GxpAuditEventRespVO> toPageResp(PageResult<GxpAuditEventDO> page) {
        PageResult<GxpAuditEventRespVO> result = BeanUtils.toBean(page, GxpAuditEventRespVO.class);
        result.getList().forEach(this::applyIntegrityStatus);
        return result;
    }

    private GxpAuditEventRespVO toResp(GxpAuditEventDO event, List<GxpAuditEventRelationDO> relations) {
        GxpAuditEventRespVO response = BeanUtils.toBean(event, GxpAuditEventRespVO.class);
        response.setRelations(BeanUtils.toBean(relations, GxpAuditEventRelationRespVO.class));
        applyIntegrityStatus(response);
        return response;
    }

    private void applyIntegrityStatus(GxpAuditEventRespVO response) {
        response.setIntegrityStatus(response.getEventHash() == null || response.getEventHash().isBlank()
                ? "MISSING_HASH" : "RECORDED");
    }
}
