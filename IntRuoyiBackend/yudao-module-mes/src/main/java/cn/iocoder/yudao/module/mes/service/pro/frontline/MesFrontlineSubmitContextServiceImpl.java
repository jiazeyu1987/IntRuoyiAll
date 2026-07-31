package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlineSubmitContextReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlineSubmitContextRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordbookDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.frontline.MesFrontlineDeviceAccountRouteBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrRecordbookMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.frontline.MesFrontlineDeviceAccountRouteBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.task.MesProTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProTaskStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED;

@Service
@Validated
public class MesFrontlineSubmitContextServiceImpl implements MesFrontlineSubmitContextService {

    private static final String RECORD_BOOK_STATUS_OPEN = "OPEN";

    private final MesFrontlineDeviceAccountContextService accountContextService;
    private final MesFrontlineDeviceAccountRouteBindingMapper routeBindingMapper;
    private final MesProTaskMapper taskMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProEdhrRecordbookMapper recordbookMapper;

    public MesFrontlineSubmitContextServiceImpl(MesFrontlineDeviceAccountContextService accountContextService,
                                                MesFrontlineDeviceAccountRouteBindingMapper routeBindingMapper,
                                                MesProTaskMapper taskMapper,
                                                MesProWorkOrderMapper workOrderMapper,
                                                MesProEdhrRecordbookMapper recordbookMapper) {
        this.accountContextService = accountContextService;
        this.routeBindingMapper = routeBindingMapper;
        this.taskMapper = taskMapper;
        this.workOrderMapper = workOrderMapper;
        this.recordbookMapper = recordbookMapper;
    }

    @Override
    public MesFrontlineSubmitContextRespVO resolve(Long loginUserId, MesFrontlineSubmitContextReqVO reqVO) {
        requireValue(loginUserId, "loginUserId");
        requireValue(reqVO, "reqVO");
        MesFrontlineRouteProcessCandidate candidate = accountContextService.requireAuthorizedProcess(
                loginUserId, reqVO.getRouteId(), reqVO.getRouteProcessId(), reqVO.getProcessId());
        MesProTaskDO task = requireTask(reqVO.getTaskId());
        requireTaskMatchesCandidate(task, candidate);
        MesProWorkOrderDO workOrder = requireWorkOrder(task.getWorkOrderId());
        MesFrontlineDeviceAccountRouteBindingDO binding = requireBinding(loginUserId, candidate);
        requireConfigured(binding.getDefaultApproveUserId(), "defaultApproveUserId");
        requireConfigured(binding.getRecordbookId(), "recordbookId");
        requireConfigured(binding.getFeedbackType(), "feedbackType");
        requireRecordbook(binding.getRecordbookId());

        return new MesFrontlineSubmitContextRespVO()
                .setWorkOrderId(workOrder.getId())
                .setWorkOrderCode(workOrder.getCode())
                .setWorkOrderName(workOrder.getName())
                .setTaskId(task.getId())
                .setTaskCode(task.getCode())
                .setItemId(task.getItemId())
                .setRouteId(candidate.routeId())
                .setRouteProcessId(candidate.routeProcessId())
                .setProcessId(candidate.processId())
                .setWorkstationId(candidate.workstationId())
                .setDeviceId(candidate.deviceId())
                .setApproveUserId(binding.getDefaultApproveUserId())
                .setRecordbookId(binding.getRecordbookId())
                .setFeedbackType(binding.getFeedbackType())
                .setScheduledQuantity(task.getQuantity())
                .setExpireDate(task.getEndTime());
    }

    private MesProTaskDO requireTask(Long taskId) {
        requireValue(taskId, "taskId");
        MesProTaskDO task = taskMapper.selectById(taskId);
        if (task == null || MesProTaskStatusEnum.isEndStatus(task.getStatus())) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "taskId=" + taskId);
        }
        requireConfigured(task.getWorkOrderId(), "task.workOrderId");
        requireConfigured(task.getRouteId(), "task.routeId");
        requireConfigured(task.getProcessId(), "task.processId");
        requireConfigured(task.getWorkstationId(), "task.workstationId");
        requireConfigured(task.getItemId(), "task.itemId");
        return task;
    }

    private MesProWorkOrderDO requireWorkOrder(Long workOrderId) {
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null
                || Objects.equals(workOrder.getStatus(), MesProWorkOrderStatusEnum.FINISHED.getStatus())
                || Objects.equals(workOrder.getStatus(), MesProWorkOrderStatusEnum.CANCELED.getStatus())) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "workOrderId=" + workOrderId);
        }
        return workOrder;
    }

    private MesFrontlineDeviceAccountRouteBindingDO requireBinding(Long loginUserId,
                                                                   MesFrontlineRouteProcessCandidate candidate) {
        MesFrontlineDeviceAccountRouteBindingDO binding =
                routeBindingMapper.selectEnabledByDeviceAccountUserIdAndRouteIdAndDeviceIdAndWorkstationId(
                        loginUserId, candidate.routeId(), candidate.deviceId(), candidate.workstationId());
        if (binding == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "route binding");
        }
        return binding;
    }

    private void requireRecordbook(Long recordbookId) {
        MesProEdhrRecordbookDO recordbook = recordbookMapper.selectById(recordbookId);
        if (recordbook == null || !RECORD_BOOK_STATUS_OPEN.equals(recordbook.getStatus())) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "recordbookId=" + recordbookId);
        }
    }

    private static void requireTaskMatchesCandidate(MesProTaskDO task, MesFrontlineRouteProcessCandidate candidate) {
        if (!Objects.equals(task.getRouteId(), candidate.routeId())
                || !Objects.equals(task.getProcessId(), candidate.processId())
                || !Objects.equals(task.getWorkstationId(), candidate.workstationId())) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED,
                    "task/process mismatch: taskId=" + task.getId());
        }
    }

    private static void requireConfigured(Object value, String fieldName) {
        if (value == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, fieldName);
        }
    }

    private static void requireValue(Object value, String fieldName) {
        if (value == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, fieldName);
        }
    }
}
