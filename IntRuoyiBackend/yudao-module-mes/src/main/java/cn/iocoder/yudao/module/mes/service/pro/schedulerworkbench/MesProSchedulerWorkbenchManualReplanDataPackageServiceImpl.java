package cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchManualReplanDataImportRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.holiday.MesCalHolidayDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.plan.MesCalPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.plan.MesCalPlanShiftDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdProductionLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationWorkerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowEdgeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProCapacityActualDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProCapacityPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProReplanExplanationSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProScheduleCalendarRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProScheduleIssueDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProTaskDependencyDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProTaskScheduleExtDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderOperationLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionMaterialListDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.materialstock.MesWmMaterialStockDO;
import cn.iocoder.yudao.module.mes.dal.mysql.cal.holiday.MesCalHolidayMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.cal.plan.MesCalPlanMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.cal.plan.MesCalPlanShiftMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdProductionLineMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMachineMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationWorkerMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteScheduleConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProCapacityActualMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProCapacityPlanMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProReplanExplanationSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProScheduleCalendarRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProScheduleIssueMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProTaskDependencyMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProTaskScheduleExtMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderOperationLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.task.MesProTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionMaterialListMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.materialstock.MesWmMaterialStockMapper;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_CONTENT_INVALID;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_FILE_EMPTY;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_FORMAT_UNSUPPORTED;

@Service
public class MesProSchedulerWorkbenchManualReplanDataPackageServiceImpl
        implements MesProSchedulerWorkbenchManualReplanDataPackageService {

    private static final String PACKAGE_VERSION = "scheduler-manual-replan-data.v1";

    @Resource
    private MesMdItemMapper itemMapper;
    @Resource
    private MesProProcessMapper processMapper;
    @Resource
    private MesProRouteMapper routeMapper;
    @Resource
    private MesProRouteVersionMapper routeVersionMapper;
    @Resource
    private MesProRouteProductMapper routeProductMapper;
    @Resource
    private MesProRouteProcessMapper routeProcessMapper;
    @Resource
    private MesProRouteProcessFlowEdgeMapper routeProcessFlowEdgeMapper;
    @Resource
    private MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Resource
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Resource
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Resource
    private MesProRouteScheduleConfigMapper routeScheduleConfigMapper;
    @Resource
    private MesMdProductionLineMapper productionLineMapper;
    @Resource
    private MesMdWorkstationMapper workstationMapper;
    @Resource
    private MesMdWorkstationMachineMapper workstationMachineMapper;
    @Resource
    private MesMdWorkstationWorkerMapper workstationWorkerMapper;
    @Resource
    private MesCalPlanMapper planMapper;
    @Resource
    private MesCalPlanShiftMapper planShiftMapper;
    @Resource
    private MesCalHolidayMapper holidayMapper;
    @Resource
    private MesProScheduleCalendarRuleMapper scheduleCalendarRuleMapper;
    @Resource
    private MesProCapacityPlanMapper capacityPlanMapper;
    @Resource
    private MesProCapacityActualMapper capacityActualMapper;
    @Resource
    private MesWmMaterialStockMapper materialStockMapper;
    @Resource
    private MesProWorkOrderMapper workOrderMapper;
    @Resource
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Resource
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Resource
    private MesKingdeeProductionMaterialListMapper productionMaterialListMapper;
    @Resource
    private MesProTaskMapper taskMapper;
    @Resource
    private MesProTaskScheduleExtMapper taskScheduleExtMapper;
    @Resource
    private MesProTaskDependencyMapper taskDependencyMapper;
    @Resource
    private MesProFeedbackMapper feedbackMapper;
    @Resource
    private MesProScheduleIssueMapper scheduleIssueMapper;
    @Resource
    private MesProScheduleOrderOperationLogMapper scheduleOrderOperationLogMapper;
    @Resource
    private MesProReplanExplanationSnapshotMapper replanExplanationSnapshotMapper;

    @Override
    public byte[] exportPackage() {
        ManualReplanDataPackage payload = new ManualReplanDataPackage();
        payload.setPackageVersion(PACKAGE_VERSION);
        payload.setItems(itemMapper.selectList());
        payload.setProcesses(processMapper.selectList());
        payload.setRoutes(routeMapper.selectList());
        payload.setRouteVersions(routeVersionMapper.selectList());
        payload.setRouteProducts(routeProductMapper.selectList());
        payload.setRouteProcesses(routeProcessMapper.selectList());
        payload.setRouteProcessFlowEdges(routeProcessFlowEdgeMapper.selectList());
        payload.setRouteFlowConfigs(routeFlowConfigMapper.selectList());
        payload.setRouteFlowProcessConfigs(routeFlowProcessConfigMapper.selectList());
        payload.setRouteFlowProcessBatchRecords(routeFlowProcessBatchRecordMapper.selectList());
        payload.setRouteScheduleConfigs(routeScheduleConfigMapper.selectList());
        payload.setProductionLines(productionLineMapper.selectList());
        payload.setWorkstations(workstationMapper.selectList());
        payload.setWorkstationMachines(workstationMachineMapper.selectList());
        payload.setWorkstationWorkers(workstationWorkerMapper.selectList());
        payload.setCalendarPlans(planMapper.selectList());
        payload.setCalendarPlanShifts(planShiftMapper.selectList());
        payload.setCalendarHolidays(holidayMapper.selectList());
        payload.setScheduleCalendarRules(scheduleCalendarRuleMapper.selectList());
        payload.setCapacityPlans(capacityPlanMapper.selectList());
        payload.setCapacityActuals(capacityActualMapper.selectList());
        payload.setMaterialStocks(materialStockMapper.selectList());
        payload.setWorkOrders(workOrderMapper.selectList());
        payload.setScheduleOrders(scheduleOrderMapper.selectList());
        payload.setScheduleOrderProcesses(scheduleOrderProcessMapper.selectList());
        payload.setProductionMaterialLists(productionMaterialListMapper.selectList());
        payload.setTasks(taskMapper.selectList());
        payload.setTaskScheduleExts(taskScheduleExtMapper.selectList());
        payload.setTaskDependencies(taskDependencyMapper.selectList());
        payload.setFeedbacks(feedbackMapper.selectList());
        payload.setScheduleIssues(scheduleIssueMapper.selectList());
        payload.setScheduleOrderOperationLogs(scheduleOrderOperationLogMapper.selectList());
        payload.setReplanExplanationSnapshots(replanExplanationSnapshotMapper.selectList());
        return JsonUtils.toJsonByte(payload);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProSchedulerWorkbenchManualReplanDataImportRespVO importPackage(byte[] content) {
        ManualReplanDataPackage payload = parsePayload(content);
        validatePayload(payload);

        int masterDataCount = 0;
        masterDataCount += upsertRows(itemMapper, payload.getItems(), "物料主数据");
        masterDataCount += upsertRows(processMapper, payload.getProcesses(), "工序主数据");
        masterDataCount += upsertRows(routeMapper, payload.getRoutes(), "工艺路线");
        masterDataCount += upsertRows(routeVersionMapper, payload.getRouteVersions(), "工艺路线版本");
        masterDataCount += upsertRows(routeProductMapper, payload.getRouteProducts(), "路线产品绑定");
        masterDataCount += upsertRows(routeProcessMapper, payload.getRouteProcesses(), "路线工序");
        masterDataCount += upsertRows(routeProcessFlowEdgeMapper, payload.getRouteProcessFlowEdges(), "路线流转关系");
        masterDataCount += upsertRows(routeFlowConfigMapper, payload.getRouteFlowConfigs(), "路线用途配置");
        masterDataCount += upsertRows(routeFlowProcessConfigMapper, payload.getRouteFlowProcessConfigs(), "路线用途工序配置");
        masterDataCount += upsertRows(routeFlowProcessBatchRecordMapper, payload.getRouteFlowProcessBatchRecords(), "路线批记录绑定");
        masterDataCount += upsertRows(routeScheduleConfigMapper, payload.getRouteScheduleConfigs(), "路线排产配置");
        masterDataCount += upsertRows(productionLineMapper, payload.getProductionLines(), "产线");
        masterDataCount += upsertRows(workstationMapper, payload.getWorkstations(), "工位");
        masterDataCount += upsertRows(workstationMachineMapper, payload.getWorkstationMachines(), "工位设备");
        masterDataCount += upsertRows(workstationWorkerMapper, payload.getWorkstationWorkers(), "工位人力");
        masterDataCount += upsertRows(planMapper, payload.getCalendarPlans(), "日历计划");
        masterDataCount += upsertRows(planShiftMapper, payload.getCalendarPlanShifts(), "日历班次");
        masterDataCount += upsertRows(holidayMapper, payload.getCalendarHolidays(), "日历假日");
        masterDataCount += upsertRows(scheduleCalendarRuleMapper, payload.getScheduleCalendarRules(), "排程日历规则");
        masterDataCount += upsertRows(capacityPlanMapper, payload.getCapacityPlans(), "计划产能");
        masterDataCount += upsertRows(capacityActualMapper, payload.getCapacityActuals(), "实际产能");
        masterDataCount += upsertRows(materialStockMapper, payload.getMaterialStocks(), "库存");

        int scheduleOrderDataCount = 0;
        scheduleOrderDataCount += upsertRows(workOrderMapper, payload.getWorkOrders(), "生产工单");
        scheduleOrderDataCount += upsertRows(scheduleOrderMapper, payload.getScheduleOrders(), "排产工单");
        scheduleOrderDataCount += upsertRows(scheduleOrderProcessMapper, payload.getScheduleOrderProcesses(), "排产工单工序快照");
        scheduleOrderDataCount += upsertRows(productionMaterialListMapper, payload.getProductionMaterialLists(), "生产用料清单");

        int runtimeDataCount = 0;
        runtimeDataCount += upsertRows(taskMapper, payload.getTasks(), "生产任务");
        runtimeDataCount += upsertRows(taskScheduleExtMapper, payload.getTaskScheduleExts(), "任务排产扩展");
        runtimeDataCount += upsertRows(taskDependencyMapper, payload.getTaskDependencies(), "任务依赖");
        runtimeDataCount += upsertRows(feedbackMapper, payload.getFeedbacks(), "报工记录");
        runtimeDataCount += upsertRows(scheduleIssueMapper, payload.getScheduleIssues(), "排产问题");
        runtimeDataCount += upsertRows(scheduleOrderOperationLogMapper, payload.getScheduleOrderOperationLogs(), "排产操作日志");
        runtimeDataCount += upsertRows(replanExplanationSnapshotMapper, payload.getReplanExplanationSnapshots(), "重排说明快照");

        MesProSchedulerWorkbenchManualReplanDataImportRespVO respVO =
                new MesProSchedulerWorkbenchManualReplanDataImportRespVO();
        respVO.setMasterDataCount(masterDataCount);
        respVO.setScheduleOrderDataCount(scheduleOrderDataCount);
        respVO.setRuntimeDataCount(runtimeDataCount);
        return respVO;
    }

    private ManualReplanDataPackage parsePayload(byte[] content) {
        if (content == null || content.length == 0) {
            throw exception(CONFIG_PACKAGE_FILE_EMPTY);
        }
        try {
            return JsonUtils.parseObject(content, ManualReplanDataPackage.class);
        } catch (RuntimeException ex) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "手动重排数据包 JSON 非法");
        }
    }

    private void validatePayload(ManualReplanDataPackage payload) {
        if (payload == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "手动重排数据包 JSON 非法");
        }
        if (!PACKAGE_VERSION.equals(payload.getPackageVersion())) {
            throw exception(CONFIG_PACKAGE_FORMAT_UNSUPPORTED, payload.getPackageVersion());
        }
        requireList(payload.getItems(), "items");
        requireList(payload.getProcesses(), "processes");
        requireList(payload.getRoutes(), "routes");
        requireList(payload.getRouteVersions(), "routeVersions");
        requireList(payload.getRouteProducts(), "routeProducts");
        requireList(payload.getRouteProcesses(), "routeProcesses");
        requireList(payload.getRouteProcessFlowEdges(), "routeProcessFlowEdges");
        requireList(payload.getRouteFlowConfigs(), "routeFlowConfigs");
        requireList(payload.getRouteFlowProcessConfigs(), "routeFlowProcessConfigs");
        requireList(payload.getRouteFlowProcessBatchRecords(), "routeFlowProcessBatchRecords");
        requireList(payload.getRouteScheduleConfigs(), "routeScheduleConfigs");
        requireList(payload.getProductionLines(), "productionLines");
        requireList(payload.getWorkstations(), "workstations");
        requireList(payload.getWorkstationMachines(), "workstationMachines");
        requireList(payload.getWorkstationWorkers(), "workstationWorkers");
        requireList(payload.getCalendarPlans(), "calendarPlans");
        requireList(payload.getCalendarPlanShifts(), "calendarPlanShifts");
        requireList(payload.getCalendarHolidays(), "calendarHolidays");
        requireList(payload.getScheduleCalendarRules(), "scheduleCalendarRules");
        requireList(payload.getCapacityPlans(), "capacityPlans");
        requireList(payload.getCapacityActuals(), "capacityActuals");
        requireList(payload.getMaterialStocks(), "materialStocks");
        requireList(payload.getWorkOrders(), "workOrders");
        requireList(payload.getScheduleOrders(), "scheduleOrders");
        requireList(payload.getScheduleOrderProcesses(), "scheduleOrderProcesses");
        requireList(payload.getProductionMaterialLists(), "productionMaterialLists");
        requireList(payload.getTasks(), "tasks");
        requireList(payload.getTaskScheduleExts(), "taskScheduleExts");
        requireList(payload.getTaskDependencies(), "taskDependencies");
        requireList(payload.getFeedbacks(), "feedbacks");
        requireList(payload.getScheduleIssues(), "scheduleIssues");
        requireList(payload.getScheduleOrderOperationLogs(), "scheduleOrderOperationLogs");
        requireList(payload.getReplanExplanationSnapshots(), "replanExplanationSnapshots");
    }

    private void requireList(List<?> rows, String fieldName) {
        if (rows == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "手动重排数据包缺少 " + fieldName);
        }
    }

    private <T> int upsertRows(BaseMapperX<T> mapper, List<T> rows, String label) {
        int count = 0;
        for (T row : rows) {
            Long id = extractId(row, label);
            if (mapper.selectById(id) == null) {
                mapper.insert(row);
            } else {
                mapper.updateById(row);
            }
            count++;
        }
        return count;
    }

    private Long extractId(Object row, String label) {
        if (row == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "手动重排数据包包含空" + label + "记录");
        }
        try {
            Method method = row.getClass().getMethod("getId");
            Object id = method.invoke(row);
            if (id == null) {
                throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "手动重排数据包" + label + "记录缺少 id");
            }
            if (!(id instanceof Number number)) {
                throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "手动重排数据包" + label + "记录 id 非数字");
            }
            return number.longValue();
        } catch (ReflectiveOperationException ex) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "手动重排数据包" + label + "记录缺少 getId");
        }
    }

    @Data
    public static class ManualReplanDataPackage {
        private String packageVersion;
        private List<MesMdItemDO> items;
        private List<MesProProcessDO> processes;
        private List<MesProRouteDO> routes;
        private List<MesProRouteVersionDO> routeVersions;
        private List<MesProRouteProductDO> routeProducts;
        private List<MesProRouteProcessDO> routeProcesses;
        private List<MesProRouteProcessFlowEdgeDO> routeProcessFlowEdges;
        private List<MesProRouteFlowConfigDO> routeFlowConfigs;
        private List<MesProRouteFlowProcessConfigDO> routeFlowProcessConfigs;
        private List<MesProRouteFlowProcessBatchRecordDO> routeFlowProcessBatchRecords;
        private List<MesProRouteScheduleConfigDO> routeScheduleConfigs;
        private List<MesMdProductionLineDO> productionLines;
        private List<MesMdWorkstationDO> workstations;
        private List<MesMdWorkstationMachineDO> workstationMachines;
        private List<MesMdWorkstationWorkerDO> workstationWorkers;
        private List<MesCalPlanDO> calendarPlans;
        private List<MesCalPlanShiftDO> calendarPlanShifts;
        private List<MesCalHolidayDO> calendarHolidays;
        private List<MesProScheduleCalendarRuleDO> scheduleCalendarRules;
        private List<MesProCapacityPlanDO> capacityPlans;
        private List<MesProCapacityActualDO> capacityActuals;
        private List<MesWmMaterialStockDO> materialStocks;
        private List<MesProWorkOrderDO> workOrders;
        private List<MesProScheduleOrderDO> scheduleOrders;
        private List<MesProScheduleOrderProcessDO> scheduleOrderProcesses;
        private List<MesKingdeeProductionMaterialListDO> productionMaterialLists;
        private List<MesProTaskDO> tasks;
        private List<MesProTaskScheduleExtDO> taskScheduleExts;
        private List<MesProTaskDependencyDO> taskDependencies;
        private List<MesProFeedbackDO> feedbacks;
        private List<MesProScheduleIssueDO> scheduleIssues;
        private List<MesProScheduleOrderOperationLogDO> scheduleOrderOperationLogs;
        private List<MesProReplanExplanationSnapshotDO> replanExplanationSnapshots;
    }
}
