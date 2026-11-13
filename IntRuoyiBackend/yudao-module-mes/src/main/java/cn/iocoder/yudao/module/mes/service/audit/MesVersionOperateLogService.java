package cn.iocoder.yudao.module.mes.service.audit;

import cn.iocoder.yudao.framework.common.biz.system.logger.dto.OperateLogCreateReqDTO;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.util.monitor.TracerUtils;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.system.service.logger.OperateLogService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 与版本写入共用事务，日志失败必须阻止业务提交。 */
@Service
@Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
public class MesVersionOperateLogService {
    @Resource
    private OperateLogService operateLogService;

    public void route(String operation, MesProRouteVersionDO before, MesProRouteVersionDO after, Long actorId) {
        JSONObject snapshot = JSON.parseObject(after.getRouteSnapshotJson());
        JSONObject details = new JSONObject(true);
        details.put("routeId", after.getRouteId());
        details.put("routeCode", snapshot.getString("routeCode"));
        details.put("routeName", snapshot.getString("routeName"));
        details.put("sourceVersionId", after.getSourceRouteVersionId());
        details.put("beforeVersionId", before == null ? null : before.getId());
        details.put("beforeVersionNo", before == null ? null : before.getVersionNo());
        details.put("versionId", after.getId());
        details.put("versionNo", after.getVersionNo());
        details.put("lifecycleStatus", after.getLifecycleStatus());
        details.put("snapshotSha256", after.getRouteSnapshotSha256());
        details.put("changeSummaryJson", after.getChangeSummaryJson());
        write("工艺流程", operation, after.getRouteId(), actorId,
                "工艺流程【" + snapshot.getString("routeCode") + " / " + snapshot.getString("routeName")
                        + "】" + operation + "；原版本：" + (before == null ? "无" : before.getVersionNo())
                        + "；目标版本：" + after.getVersionNo(), details);
    }

    public void qa(String operation, MesQaInspectionRegulationDO regulation,
                   MesQaInspectionRegulationVersionDO before, MesQaInspectionRegulationVersionDO after) {
        JSONObject details = new JSONObject(true);
        details.put("regulationId", regulation.getId());
        details.put("regulationCode", regulation.getRegulationCode());
        details.put("regulationName", regulation.getRegulationName());
        details.put("dccProjectCodeId", regulation.getDccProjectCodeId());
        details.put("ownerModule", regulation.getOwnerModule());
        details.put("beforeVersionId", before == null ? null : before.getId());
        details.put("beforeVersionNo", before == null ? null : before.getVersionNo());
        details.put("versionId", after.getId());
        details.put("versionNo", after.getVersionNo());
        details.put("lifecycleStatus", after.getLifecycleStatus());
        details.put("effectiveDate", after.getEffectiveDate());
        details.put("publishedAt", after.getPublishedAt());
        write("QA检验规程", operation, regulation.getId(), SecurityFrameworkUtils.getLoginUserId(),
                "QA检验规程【" + regulation.getRegulationCode() + " / " + regulation.getRegulationName()
                        + "】" + operation + "；DCC项目编号：" + regulation.getDccProjectCodeId()
                        + "；原版本：" + (before == null ? "无" : before.getVersionNo())
                        + "；目标版本：" + after.getVersionNo(), details);
    }

    private void write(String module, String operation, Long bizId, Long actorId, String action, JSONObject details) {
        if (actorId == null || bizId == null) {
            throw new IllegalStateException("版本操作日志缺少操作人或业务编号");
        }
        OperateLogCreateReqDTO log = new OperateLogCreateReqDTO();
        log.setUserId(actorId);
        log.setUserType(UserTypeEnum.ADMIN.getValue());
        log.setType(module);
        log.setSubType(operation);
        log.setBizId(bizId);
        log.setAction(action);
        log.setExtra(details.toJSONString());
        log.setTraceId(TracerUtils.getTraceId());
        HttpServletRequest request = ServletUtils.getRequest();
        if (request != null) {
            log.setRequestMethod(request.getMethod());
            log.setRequestUrl(request.getRequestURI());
            log.setUserIp(ServletUtils.getClientIP(request));
            log.setUserAgent(ServletUtils.getUserAgent(request));
        }
        operateLogService.createOperateLog(log);
    }
}
