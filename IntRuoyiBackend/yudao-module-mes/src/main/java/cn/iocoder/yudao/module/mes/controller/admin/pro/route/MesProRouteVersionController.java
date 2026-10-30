package cn.iocoder.yudao.module.mes.controller.admin.pro.route;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version.MesProRouteVersionBlockerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version.MesProRouteVersionCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version.MesProRouteVersionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version.MesProRouteVersionNameSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version.MesProRouteProductionProcessConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteCandidateConfigService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionBusinessApprovalSubmitService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionWorkflowService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE;

@Tag(name = "管理后台 - MES 工艺路线版本")
@RestController
@RequestMapping("/mes/pro/route-version")
@Validated
public class MesProRouteVersionController {

    @Resource
    private MesProRouteVersionBusinessApprovalSubmitService businessApprovalSubmitService;
    @Resource
    private MesProRouteVersionWorkflowService workflowService;
    @Resource
    private MesProRouteCandidateConfigService candidateConfigService;
    @Resource
    private MesProRouteVersionMapper routeVersionMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @GetMapping("/list-by-route")
    @Operation(summary = "查询工艺路线版本列表")
    @Parameter(name = "routeId", description = "工艺路线编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-route:version-query')")
    public CommonResult<List<MesProRouteVersionRespVO>> listByRouteId(@RequestParam("routeId") Long routeId) {
        return success(toVersionResponses(workflowService.listByRouteId(routeId)));
    }

    @GetMapping("/get")
    @Operation(summary = "查询工艺路线版本详情")
    @Parameter(name = "id", description = "路线版本编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-route:version-query')")
    public CommonResult<MesProRouteVersionRespVO> getVersion(@RequestParam("id") Long id) {
        return success(toVersionResponses(List.of(workflowService.getVersion(id))).get(0));
    }

    private List<MesProRouteVersionRespVO> toVersionResponses(List<MesProRouteVersionDO> versions) {
        Set<Long> userIds = versions.stream().map(MesProRouteVersionDO::getLastSavedBy)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> users = userIds.isEmpty() ? Map.of() : adminUserApi.getUserMap(userIds);
        return BeanUtils.toBean(versions, MesProRouteVersionRespVO.class, vo -> {
            AdminUserRespDTO user = vo.getLastSavedBy() == null ? null : users.get(vo.getLastSavedBy());
            if (user != null) {
                vo.setLastSavedUserName(user.getNickname());
            }
        });
    }

    @GetMapping("/name")
    @Operation(summary = "查询工艺路线版本名称")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:version-query')")
    public CommonResult<Map<String, Object>> getVersionName(@RequestParam("id") Long id) {
        MesProRouteVersionDO version = workflowService.getVersion(id);
        JSONObject snapshot = JSON.parseObject(version.getRouteSnapshotJson());
        if (snapshot == null || cn.hutool.core.util.StrUtil.isBlank(snapshot.getString("routeName"))
                || cn.hutool.core.util.StrUtil.isBlank(version.getRouteSnapshotSha256())) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, id);
        }
        return success(Map.of("routeVersionId", id, "routeId", version.getRouteId(),
                "name", snapshot.getString("routeName"), "routeSnapshotSha256", version.getRouteSnapshotSha256()));
    }

    @PostMapping("/name/save")
    @Operation(summary = "保存工艺路线草稿名称")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:version-update')")
    public CommonResult<Boolean> saveVersionName(@Valid @RequestBody MesProRouteVersionNameSaveReqVO reqVO) {
        candidateConfigService.saveRouteName(reqVO.getRouteVersionId(),
                reqVO.getExpectedRouteSnapshotSha256(), reqVO.getName());
        return success(Boolean.TRUE);
    }

    @GetMapping("/production-process-config")
    @Operation(summary = "查询工艺路线版本生产工序配置")
    @Parameter(name = "id", description = "路线版本编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-route:version-query')")
    public CommonResult<Map<String, Object>> getProductionProcessConfig(@RequestParam("id") Long id) {
        MesProRouteVersionDO version = routeVersionMapper.selectById(id);
        if (version == null) {
            throw exception(PRO_ROUTE_VERSION_NOT_EXISTS);
        }
        JSONObject snapshot = JSON.parseObject(version.getRouteSnapshotJson());
        JSONObject configSnapshots = snapshot == null ? null : snapshot.getJSONObject("configSnapshots");
        Integer schemaVersion = configSnapshots == null
                ? null : configSnapshots.getInteger("productionProcessConfigSchemaVersion");
        JSONArray configs = configSnapshots == null
                ? null : configSnapshots.getJSONArray("productionProcessConfigs");
        if (!Integer.valueOf(1).equals(schemaVersion) || configs == null
                || version.getRouteSnapshotSha256() == null) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, id);
        }
        return success(Map.of(
                "routeVersionId", id,
                "routeSnapshotSha256", version.getRouteSnapshotSha256(),
                "schemaVersion", schemaVersion,
                "productionProcessConfigs", configs));
    }

    @GetMapping("/blockers")
    @Operation(summary = "查询工艺路线候选版本发布阻断")
    @Parameter(name = "id", description = "候选路线版本编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-route:version-query')")
    public CommonResult<MesProRouteVersionBlockerRespVO> getPublishBlockers(@RequestParam("id") Long id) {
        return success(workflowService.getPublishBlockers(id));
    }

    @PostMapping("/create-candidate")
    @Operation(summary = "创建工艺路线候选版本")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:version-create')")
    public CommonResult<MesProRouteVersionRespVO> createCandidate(@Valid @RequestBody MesProRouteVersionCreateReqVO reqVO) {
        return success(BeanUtils.toBean(workflowService.createCandidate(reqVO), MesProRouteVersionRespVO.class));
    }

    @PostMapping("/production-process-config/save")
    @Operation(summary = "保存工艺路线候选版本生产工序配置")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:version-update')")
    public CommonResult<Boolean> saveProductionProcessConfig(
            @Valid @RequestBody MesProRouteProductionProcessConfigSaveReqVO reqVO) {
        candidateConfigService.saveConfigSnapshots(reqVO.getRouteVersionId(),
                reqVO.getExpectedRouteSnapshotSha256(), Map.of(
                "productionProcessConfigSchemaVersion", reqVO.getSchemaVersion(),
                "productionProcessConfigs", reqVO.getProductionProcessConfigs()));
        return success(Boolean.TRUE);
    }

    @PostMapping("/submit")
    @Operation(summary = "提交工艺路线候选版本发布审批")
    @Parameter(name = "id", description = "候选路线版本编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-route:version-submit')")
    public CommonResult<MesProRouteVersionRespVO> submitCandidate(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(businessApprovalSubmitService.submitAndPublishCandidate(id),
                MesProRouteVersionRespVO.class));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "撤回工艺路线候选版本审核")
    @Parameter(name = "id", description = "候选路线版本编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-route:version-withdraw')")
    public CommonResult<MesProRouteVersionRespVO> withdrawCandidate(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(workflowService.withdrawCandidate(id), MesProRouteVersionRespVO.class));
    }

    @PostMapping("/reopen")
    @Operation(summary = "重新打开已驳回工艺路线候选版本")
    @Parameter(name = "id", description = "候选路线版本编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-route:version-reopen')")
    public CommonResult<MesProRouteVersionRespVO> reopenRejectedCandidate(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(workflowService.reopenRejectedCandidate(id), MesProRouteVersionRespVO.class));
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消工艺路线候选版本")
    @Parameter(name = "id", description = "候选路线版本编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-route:version-cancel')")
    public CommonResult<MesProRouteVersionRespVO> cancelCandidate(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(workflowService.cancelCandidate(id), MesProRouteVersionRespVO.class));
    }

    @PostMapping("/submit-publish")
    @Operation(summary = "提交工艺路线候选版本发布审批")
    @Parameter(name = "id", description = "候选路线版本编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-route:version-submit')")
    public CommonResult<MesProRouteVersionRespVO> submitAndPublishCandidate(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(businessApprovalSubmitService.submitAndPublishCandidate(id),
                MesProRouteVersionRespVO.class));
    }
}
