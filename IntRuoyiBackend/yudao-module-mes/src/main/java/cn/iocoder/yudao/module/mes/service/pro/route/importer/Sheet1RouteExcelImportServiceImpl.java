package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowEdgeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteOwnerPermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_FILE_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_INVALID_EXCEL;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_PROCESS_STATUS_INVALID;

@Service
@Validated
public class Sheet1RouteExcelImportServiceImpl implements Sheet1RouteExcelImportService {

    private static final String ROUTE_CODE_PREFIX = "ROUTE-XLSX-";
    private static final String PROCESS_CODE_PREFIX = "PROC-XLSX-";
    private static final Pattern FIVE_DIGIT_SUFFIX_PATTERN = Pattern.compile("\\d{5}");

    private final Sheet1RouteExcelParser parser;
    private final MesProRouteMapper routeMapper;
    private final MesProProcessMapper processMapper;
    private final MesProRouteProcessMapper routeProcessMapper;
    private final MesProRouteProcessFlowEdgeMapper routeProcessFlowEdgeMapper;
    private final MesProRouteOwnerPermissionService routeOwnerPermissionService;

    public Sheet1RouteExcelImportServiceImpl(Sheet1RouteExcelParser parser,
                                             MesProRouteMapper routeMapper,
                                             MesProProcessMapper processMapper,
                                             MesProRouteProcessMapper routeProcessMapper,
                                             MesProRouteProcessFlowEdgeMapper routeProcessFlowEdgeMapper,
                                             MesProRouteOwnerPermissionService routeOwnerPermissionService) {
        this.parser = parser;
        this.routeMapper = routeMapper;
        this.processMapper = processMapper;
        this.routeProcessMapper = routeProcessMapper;
        this.routeProcessFlowEdgeMapper = routeProcessFlowEdgeMapper;
        this.routeOwnerPermissionService = routeOwnerPermissionService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Sheet1RouteExcelImportResult importExcel(MultipartFile file, Integer processStatus) {
        validateProcessStatus(processStatus);
        validateFile(file);
        Sheet1RouteExcelParser.ParseResult parseResult = parse(file);

        AtomicInteger nextRouteSequence = new AtomicInteger(
                maxSuffix(routeMapper.selectListByCodePrefix(ROUTE_CODE_PREFIX), ROUTE_CODE_PREFIX) + 1);
        AtomicInteger nextProcessSequence = new AtomicInteger(
                maxSuffix(processMapper.selectListByCodePrefix(PROCESS_CODE_PREFIX), PROCESS_CODE_PREFIX) + 1);

        LinkedHashSet<String> uniqueProcessNames = collectProcessNames(parseResult.routes());
        Map<String, Long> processIdByName = new LinkedHashMap<>();
        int processCreatedCount = 0;
        int processReusedCount = 0;
        for (String processName : uniqueProcessNames) {
            MesProProcessDO existingProcess = processMapper.selectByName(processName);
            if (existingProcess != null) {
                processIdByName.put(processName, existingProcess.getId());
                processReusedCount++;
                continue;
            }
            MesProProcessDO process = new MesProProcessDO();
            process.setCode(formatCode(PROCESS_CODE_PREFIX, nextProcessSequence.getAndIncrement()));
            process.setName(processName);
            process.setStatus(processStatus);
            processMapper.insert(process);
            processIdByName.put(processName, process.getId());
            processCreatedCount++;
        }

        List<String> routeCodes = new ArrayList<>(parseResult.routes().size());
        int routeProcessCount = 0;
        String sourceFileName = StrUtil.blankToDefault(file.getOriginalFilename(), "uploaded.xlsx");
        for (Sheet1RouteExcelParser.Route parsedRoute : parseResult.routes()) {
            MesProRouteDO route = new MesProRouteDO();
            route.setCode(formatCode(ROUTE_CODE_PREFIX, nextRouteSequence.getAndIncrement()));
            route.setName(parsedRoute.routeName());
            route.setDescription("来源文件: " + sourceFileName + " / Sheet1");
            route.setRemark(String.join("\n", parsedRoute.materialCodes()));
            route.setStatus(CommonStatusEnum.DISABLE.getStatus());
            routeMapper.insert(route);
            routeOwnerPermissionService.bindCurrentUserAsOwner(route.getId());
            routeCodes.add(route.getCode());

            List<MesProRouteProcessDO> routeProcesses = buildRouteProcesses(route.getId(), parsedRoute, processIdByName);
            for (MesProRouteProcessDO routeProcess : routeProcesses) {
                routeProcessMapper.insert(routeProcess);
                routeProcessCount++;
            }
            insertLinearFlowEdges(route.getId(), routeProcesses);
        }

        Sheet1RouteExcelImportResult result = new Sheet1RouteExcelImportResult();
        result.setRouteCount(parseResult.routes().size());
        result.setProcessCreatedCount(processCreatedCount);
        result.setProcessReusedCount(processReusedCount);
        result.setRouteProcessCount(routeProcessCount);
        result.setRouteCodes(routeCodes);
        return result;
    }

    private void validateProcessStatus(Integer processStatus) {
        if (processStatus == null || !ArrayUtil.contains(CommonStatusEnum.ARRAYS, processStatus)) {
            throw exception(PRO_ROUTE_IMPORT_PROCESS_STATUS_INVALID, processStatus);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw exception(PRO_ROUTE_IMPORT_FILE_EMPTY);
        }
    }

    private Sheet1RouteExcelParser.ParseResult parse(MultipartFile file) {
        try {
            return parser.parse(file.getInputStream());
        } catch (IOException ex) {
            throw exception(PRO_ROUTE_IMPORT_INVALID_EXCEL);
        }
    }

    private LinkedHashSet<String> collectProcessNames(List<Sheet1RouteExcelParser.Route> routes) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (Sheet1RouteExcelParser.Route route : routes) {
            for (Sheet1RouteExcelParser.Step step : route.steps()) {
                result.add(step.processName());
            }
        }
        return result;
    }

    private List<MesProRouteProcessDO> buildRouteProcesses(Long routeId,
                                                           Sheet1RouteExcelParser.Route parsedRoute,
                                                           Map<String, Long> processIdByName) {
        List<MesProRouteProcessDO> routeProcesses = new ArrayList<>(parsedRoute.steps().size());
        for (int index = 0; index < parsedRoute.steps().size(); index++) {
            Sheet1RouteExcelParser.Step step = parsedRoute.steps().get(index);
            MesProRouteProcessDO routeProcess = new MesProRouteProcessDO();
            routeProcess.setRouteId(routeId);
            routeProcess.setProcessId(processIdByName.get(step.processName()));
            routeProcess.setSort(index + 1);
            routeProcess.setKeyFlag(false);
            routeProcess.setCheckFlag(false);
            routeProcesses.add(routeProcess);
        }
        return routeProcesses;
    }

    private void insertLinearFlowEdges(Long routeId, List<MesProRouteProcessDO> routeProcesses) {
        for (int index = 0; index + 1 < routeProcesses.size(); index++) {
            routeProcessFlowEdgeMapper.insert(MesProRouteProcessFlowEdgeDO.builder()
                    .routeId(routeId)
                    .graphVersion(1L)
                    .sourceRouteProcessId(routeProcesses.get(index).getId())
                    .targetRouteProcessId(routeProcesses.get(index + 1).getId())
                    .relationType("NORMAL")
                    .sort(index + 1)
                    .build());
        }
    }

    private int maxSuffix(List<?> items, String prefix) {
        int max = 0;
        for (Object item : items) {
            String code = item instanceof MesProRouteDO route ? route.getCode()
                    : item instanceof MesProProcessDO process ? process.getCode() : null;
            if (code == null || !code.startsWith(prefix)) {
                continue;
            }
            String suffix = code.substring(prefix.length());
            if (!FIVE_DIGIT_SUFFIX_PATTERN.matcher(suffix).matches()) {
                continue;
            }
            max = Math.max(max, Integer.parseInt(suffix));
        }
        return max;
    }

    private String formatCode(String prefix, int sequence) {
        return prefix + String.format("%05d", sequence);
    }
}
