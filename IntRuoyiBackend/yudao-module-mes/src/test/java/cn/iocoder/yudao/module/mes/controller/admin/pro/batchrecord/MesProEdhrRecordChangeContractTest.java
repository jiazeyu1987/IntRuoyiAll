package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchVoidApprovalResolutionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchVoidApprovalResolutionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeRequestReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionArchiveDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionArchiveDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordChangeEventDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrRecordChangeEventMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordChangeService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MesProEdhrRecordChangeContractTest {

    @Test
    void controllerContract_exposesVoidReopenSupplementEndpointsAndPermissions() throws Exception {
        RequestMapping mapping = MesProEdhrRecordChangeController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/mes/pro/edhr-change"}, mapping.value());

        assertPost("requestVoidExecution", new Class[]{EdhrRecordChangeRequestReqVO.class},
                "/void-execution/request", "mes:pro-edhr-change:void");
        assertPost("approveVoidExecution", new Class[]{EdhrRecordChangeApproveReqVO.class},
                "/void-execution/approve", "mes:pro-edhr-change:approve");
        assertPost("requestVoidBatchExecution", new Class[]{EdhrRecordChangeRequestReqVO.class},
                "/void-batch-execution/request", "mes:pro-edhr-change:void");
        assertPost("resolveVoidBatchExecutionApproval", new Class[]{EdhrBatchVoidApprovalResolutionReqVO.class},
                "/void-batch-execution/approval-resolution", "mes:pro-edhr-change:void");
        assertPost("withdrawVoidBatchExecution", new Class[]{EdhrRecordChangeApproveReqVO.class},
                "/void-batch-execution/withdraw", "mes:pro-edhr-change:void");
        assertPost("requestReopenBatch", new Class[]{EdhrRecordChangeRequestReqVO.class},
                "/reopen-batch/request", "mes:pro-edhr-change:reopen");
        assertPost("approveReopenBatch", new Class[]{EdhrRecordChangeApproveReqVO.class},
                "/reopen-batch/approve", "mes:pro-edhr-change:approve");
        assertPost("requestReopenExecution", new Class[]{EdhrRecordChangeRequestReqVO.class},
                "/reopen-execution/request", "mes:pro-edhr-change:reopen");
        assertPost("approveReopenExecution", new Class[]{EdhrRecordChangeApproveReqVO.class},
                "/reopen-execution/approve", "mes:pro-edhr-change:approve");
        assertPost("requestSupplement", new Class[]{EdhrRecordChangeRequestReqVO.class},
                "/supplement/request", "mes:pro-edhr-change:supplement");
        assertPut("saveSupplementDraft", new Class[]{EdhrRecordChangeRequestReqVO.class},
                "/supplement/save-draft", "mes:pro-edhr-change:supplement");
        assertPost("submitSupplement", new Class[]{EdhrRecordChangeApproveReqVO.class},
                "/supplement/submit", "mes:pro-edhr-change:supplement");
        assertPost("approveSupplement", new Class[]{EdhrRecordChangeApproveReqVO.class},
                "/supplement/approve", "mes:pro-edhr-change:approve");

        Method page = MesProEdhrRecordChangeController.class.getDeclaredMethod("getPage", EdhrRecordChangePageReqVO.class);
        assertArrayEquals(new String[]{"/page"}, page.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-change:query')",
                page.getAnnotation(PreAuthorize.class).value());

        Method detail = MesProEdhrRecordChangeController.class.getDeclaredMethod("get", Long.class);
        assertArrayEquals(new String[]{"/get"}, detail.getAnnotation(GetMapping.class).value());
        assertEquals("id", detail.getParameters()[0].getAnnotation(RequestParam.class).value());
    }

    @Test
    void serviceContract_declaresControlledChangeMethods() throws Exception {
        MesProEdhrRecordChangeService.class.getDeclaredMethod("requestVoidExecution", EdhrRecordChangeRequestReqVO.class);
        MesProEdhrRecordChangeService.class.getDeclaredMethod("approveVoidExecution", EdhrRecordChangeApproveReqVO.class);
        MesProEdhrRecordChangeService.class.getDeclaredMethod("requestVoidBatchExecution",
                EdhrRecordChangeRequestReqVO.class);
        MesProEdhrRecordChangeService.class.getDeclaredMethod("resolveVoidBatchExecutionApproval",
                EdhrBatchVoidApprovalResolutionReqVO.class);
        MesProEdhrRecordChangeService.class.getDeclaredMethod("withdrawVoidBatchExecution",
                EdhrRecordChangeApproveReqVO.class);
        MesProEdhrRecordChangeService.class.getDeclaredMethod("handleVoidBatchExecutionApprovalCallback",
                String.class, String.class, String.class, String.class, Long.class);
        MesProEdhrRecordChangeService.class.getDeclaredMethod("requestReopenBatch", EdhrRecordChangeRequestReqVO.class);
        MesProEdhrRecordChangeService.class.getDeclaredMethod("approveReopenBatch", EdhrRecordChangeApproveReqVO.class);
        MesProEdhrRecordChangeService.class.getDeclaredMethod("requestReopenExecution", EdhrRecordChangeRequestReqVO.class);
        MesProEdhrRecordChangeService.class.getDeclaredMethod("approveReopenExecution", EdhrRecordChangeApproveReqVO.class);
        MesProEdhrRecordChangeService.class.getDeclaredMethod("requestSupplement", EdhrRecordChangeRequestReqVO.class);
        MesProEdhrRecordChangeService.class.getDeclaredMethod("saveSupplementDraft", EdhrRecordChangeRequestReqVO.class);
        MesProEdhrRecordChangeService.class.getDeclaredMethod("submitSupplement", EdhrRecordChangeApproveReqVO.class);
        MesProEdhrRecordChangeService.class.getDeclaredMethod("approveSupplement", EdhrRecordChangeApproveReqVO.class);
    }

    @Test
    void dataContract_exposesChangeEventAndArchiveValidityFields() throws Exception {
        requireGetter(MesProEdhrRecordChangeEventDO.class, "getChangeType");
        requireGetter(MesProEdhrRecordChangeEventDO.class, "getTargetScope");
        requireGetter(MesProEdhrRecordChangeEventDO.class, "getReasonText");
        requireGetter(MesProEdhrRecordChangeEventDO.class, "getRequestSignatureId");
        requireGetter(MesProEdhrRecordChangeEventDO.class, "getApprovalSignatureId");
        requireGetter(MesProEdhrRecordChangeEventDO.class, "getPreviousArchiveHash");
        requireGetter(MesProEdhrRecordChangeEventDO.class, "getNewArchiveHash");
        MesProEdhrRecordChangeEventMapper.class.getDeclaredMethod("selectPage", EdhrRecordChangePageReqVO.class);

        requireGetter(MesProBatchRecordExecutionDO.class, "getVoidedByChangeEventId");
        requireGetter(MesProBatchRecordExecutionDO.class, "getReopenedByChangeEventId");
        requireGetter(MesProBatchRecordExecutionDO.class, "getSupplementSourceExecutionId");
        requireGetter(MesProBatchRecordExecutionArchiveDO.class, "getArchiveValidStatus");
        requireGetter(MesProEdhrBatchExecutionArchiveDO.class, "getArchiveValidStatus");
    }

    @Test
    void voContract_exposesGateAndEvidenceFields() throws Exception {
        requireGetter(EdhrRecordChangeRequestReqVO.class, "getReasonCategory");
        requireGetter(EdhrRecordChangeRequestReqVO.class, "getReasonText");
        requireGetter(EdhrRecordChangeRequestReqVO.class, "getPassword");
        requireGetter(EdhrRecordChangeApproveReqVO.class, "getChangeEventId");
        requireGetter(EdhrRecordChangeApproveReqVO.class, "getPassword");
        requireGetter(EdhrRecordChangeRespVO.class, "getChangeType");
        requireGetter(EdhrRecordChangeRespVO.class, "getChangeStatus");
        requireGetter(EdhrRecordChangeRespVO.class, "getPreviousStatus");
        requireGetter(EdhrRecordChangeRespVO.class, "getNewStatus");
        requireGetter(EdhrRecordChangeRespVO.class, "getPreviousHeadHash");
        requireGetter(EdhrRecordChangeRespVO.class, "getNewHeadHash");
        requireGetter(EdhrRecordChangeRespVO.class, "getPreviousArchiveHash");
        requireGetter(EdhrRecordChangeRespVO.class, "getNewArchiveHash");
        requireGetter(EdhrRecordChangeRespVO.class, "getEffectiveAt");
        requireGetter(EdhrBatchExecutionRespVO.class, "getPendingVoidChangeEventId");
        requireGetter(EdhrBatchExecutionRespVO.class, "getPendingVoidChangeCode");
        requireGetter(EdhrBatchExecutionRespVO.class, "getPendingVoidProcessInstanceId");
        requireGetter(EdhrBatchExecutionRespVO.class, "getCanWithdrawVoidRequest");
        requireGetter(EdhrBatchVoidApprovalResolutionReqVO.class, "getBatchExecutionId");
        requireGetter(EdhrBatchVoidApprovalResolutionRespVO.class, "getPolicyId");
        requireGetter(EdhrBatchVoidApprovalResolutionRespVO.class, "getPolicyMode");
        requireGetter(EdhrBatchVoidApprovalResolutionRespVO.class, "getRequiresBpm");
        requireGetter(EdhrBatchVoidApprovalResolutionRespVO.class, "getBpmProcessKey");
        requireGetter(EdhrBatchVoidApprovalResolutionRespVO.class, "getEffectExecutorCode");
    }

    private static void assertPost(String methodName, Class<?>[] parameterTypes, String path, String permission)
            throws Exception {
        Method method = MesProEdhrRecordChangeController.class.getDeclaredMethod(methodName, parameterTypes);
        assertArrayEquals(new String[]{path}, method.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('" + permission + "')", method.getAnnotation(PreAuthorize.class).value());
    }

    private static void assertPut(String methodName, Class<?>[] parameterTypes, String path, String permission)
            throws Exception {
        Method method = MesProEdhrRecordChangeController.class.getDeclaredMethod(methodName, parameterTypes);
        assertArrayEquals(new String[]{path}, method.getAnnotation(PutMapping.class).value());
        assertEquals("@ss.hasPermission('" + permission + "')", method.getAnnotation(PreAuthorize.class).value());
    }

    private static void requireGetter(Class<?> type, String getterName) throws Exception {
        type.getDeclaredMethod(getterName);
    }
}
