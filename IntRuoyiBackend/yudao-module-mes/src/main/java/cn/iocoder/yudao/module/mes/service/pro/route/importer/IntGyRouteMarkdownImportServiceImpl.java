package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowEdgeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteOwnerPermissionService;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;

@Service
@Validated
public class IntGyRouteMarkdownImportServiceImpl implements IntGyRouteMarkdownImportService {

    private final IntGyRouteMarkdownParser parser;
    private final MesProRouteMapper routeMapper;
    private final MesProProcessMapper processMapper;
    private final MesProRouteProcessMapper routeProcessMapper;
    private final MesProRouteProcessFlowEdgeMapper routeProcessFlowEdgeMapper;
    private final MesProRouteOwnerPermissionService routeOwnerPermissionService;

    public IntGyRouteMarkdownImportServiceImpl(IntGyRouteMarkdownParser parser,
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
    public IntGyRouteMarkdownImportResult importMarkdown(String markdown, Integer processStatus,
                                                         String checkProcessCodesByRouteCodeJson) {
        validateProcessStatus(processStatus);
        IntGyRouteMarkdownParser.ParseResult parseResult = parseMarkdown(markdown);
        List<IntGyRouteMarkdownParser.Route> routes = parseResult.routes();
        Map<String, List<String>> checkProcessCodesByRouteCode = parseCheckProcessCodes(checkProcessCodesByRouteCodeJson);

        validateRouteStructure(routes);
        Map<String, Set<String>> checkProcessCodeSetByRouteCode = validateCheckProcessCodes(
                routes, checkProcessCodesByRouteCode);
        validateRouteCodesDoNotExist(routes);
        LinkedHashMap<String, String> processNameByCode = collectProcessNameByCode(routes);
        Map<String, MesProProcessDO> existingProcessByCode = validateProcesses(processNameByCode);

        Map<String, Long> processIdByCode = new LinkedHashMap<>();
        int processCreatedCount = 0;
        int processReusedCount = 0;
        for (Map.Entry<String, String> entry : processNameByCode.entrySet()) {
            MesProProcessDO existingProcess = existingProcessByCode.get(entry.getKey());
            if (existingProcess != null) {
                processIdByCode.put(entry.getKey(), existingProcess.getId());
                processReusedCount++;
                continue;
            }
            MesProProcessDO process = new MesProProcessDO();
            process.setCode(entry.getKey());
            process.setName(entry.getValue());
            process.setStatus(processStatus);
            processMapper.insert(process);
            processIdByCode.put(entry.getKey(), process.getId());
            processCreatedCount++;
        }

        int routeProcessCount = 0;
        List<String> routeCodes = new ArrayList<>(routes.size());
        for (IntGyRouteMarkdownParser.Route route : routes) {
            MesProRouteDO routeDO = new MesProRouteDO();
            routeDO.setCode(route.routeCode());
            routeDO.setName(route.routeName());
            routeDO.setStatus(CommonStatusEnum.DISABLE.getStatus());
            routeMapper.insert(routeDO);
            routeOwnerPermissionService.bindCurrentUserAsOwner(routeDO.getId());
            routeCodes.add(route.routeCode());

            List<MesProRouteProcessDO> routeProcesses = buildRouteProcesses(
                    route, routeDO.getId(), processIdByCode, checkProcessCodeSetByRouteCode.get(route.routeCode()));
            for (MesProRouteProcessDO routeProcess : routeProcesses) {
                routeProcessMapper.insert(routeProcess);
                routeProcessCount++;
            }
            insertLinearFlowEdges(routeDO.getId(), routeProcesses);
        }

        IntGyRouteMarkdownImportResult result = new IntGyRouteMarkdownImportResult();
        result.setRouteCount(routes.size());
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

    private IntGyRouteMarkdownParser.ParseResult parseMarkdown(String markdown) {
        if (StrUtil.isBlank(markdown)) {
            throw exception(PRO_ROUTE_IMPORT_FILE_EMPTY);
        }
        try {
            return parser.parse(markdown);
        } catch (IllegalArgumentException ex) {
            throw exception(PRO_ROUTE_IMPORT_INVALID_MARKDOWN, ex.getMessage());
        }
    }

    private void validateRouteStructure(List<IntGyRouteMarkdownParser.Route> routes) {
        Set<String> routeCodes = new HashSet<>();
        for (IntGyRouteMarkdownParser.Route route : routes) {
            if (!routeCodes.add(route.routeCode())) {
                throw exception(PRO_ROUTE_IMPORT_ROUTE_DUPLICATE, route.routeCode());
            }
            if (CollUtil.isEmpty(route.steps())) {
                throw exception(PRO_ROUTE_IMPORT_ROUTE_NO_STEP, route.routeCode());
            }
            Set<Integer> sequenceNumbers = new HashSet<>();
            long finalProcessCount = 0;
            for (IntGyRouteMarkdownParser.Step step : route.steps()) {
                if (!sequenceNumbers.add(step.sequenceNo())) {
                    throw exception(PRO_ROUTE_IMPORT_SEQUENCE_DUPLICATE, route.routeCode(), step.sequenceNo());
                }
                if (step.finalProcess()) {
                    finalProcessCount++;
                }
            }
            if (finalProcessCount != 1) {
                throw exception(PRO_ROUTE_IMPORT_FINAL_PROCESS_INVALID, route.routeCode());
            }
        }
    }

    private Map<String, List<String>> parseCheckProcessCodes(String checkProcessCodesByRouteCodeJson) {
        if (StrUtil.isBlank(checkProcessCodesByRouteCodeJson)) {
            return Map.of();
        }
        try {
            Map<String, List<String>> parsed = JsonUtils.parseObject(
                    checkProcessCodesByRouteCodeJson, new TypeReference<>() {
                    });
            if (parsed == null) {
                throw exception(PRO_ROUTE_IMPORT_CHECK_PROCESS_INVALID, checkProcessCodesByRouteCodeJson);
            }
            return parsed;
        } catch (RuntimeException ex) {
            throw exception(PRO_ROUTE_IMPORT_CHECK_PROCESS_INVALID, checkProcessCodesByRouteCodeJson);
        }
    }

    private Map<String, Set<String>> validateCheckProcessCodes(
            List<IntGyRouteMarkdownParser.Route> routes,
            Map<String, List<String>> checkProcessCodesByRouteCode) {
        Map<String, Set<String>> routeStepCodesByRouteCode = new LinkedHashMap<>();
        for (IntGyRouteMarkdownParser.Route route : routes) {
            Set<String> stepCodes = new HashSet<>();
            for (IntGyRouteMarkdownParser.Step step : route.steps()) {
                stepCodes.add(step.processCode());
            }
            routeStepCodesByRouteCode.put(route.routeCode(), stepCodes);
        }

        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (String routeCode : routeStepCodesByRouteCode.keySet()) {
            result.put(routeCode, Set.of());
        }
        for (Map.Entry<String, List<String>> entry : checkProcessCodesByRouteCode.entrySet()) {
            String routeCode = entry.getKey();
            if (StrUtil.isBlank(routeCode) || !routeStepCodesByRouteCode.containsKey(routeCode)) {
                throw exception(PRO_ROUTE_IMPORT_CHECK_PROCESS_INVALID, routeCode);
            }
            if (entry.getValue() == null) {
                throw exception(PRO_ROUTE_IMPORT_CHECK_PROCESS_INVALID, routeCode);
            }
            Set<String> validatedCodes = new LinkedHashSet<>();
            for (String processCode : entry.getValue()) {
                if (StrUtil.isBlank(processCode) || !routeStepCodesByRouteCode.get(routeCode).contains(processCode)) {
                    throw exception(PRO_ROUTE_IMPORT_CHECK_PROCESS_INVALID, routeCode + ":" + processCode);
                }
                validatedCodes.add(processCode);
            }
            result.put(routeCode, validatedCodes);
        }
        return result;
    }

    private void validateRouteCodesDoNotExist(List<IntGyRouteMarkdownParser.Route> routes) {
        for (IntGyRouteMarkdownParser.Route route : routes) {
            if (routeMapper.selectByCode(route.routeCode()) != null) {
                throw exception(PRO_ROUTE_IMPORT_ROUTE_EXISTS, route.routeCode());
            }
        }
    }

    private LinkedHashMap<String, String> collectProcessNameByCode(List<IntGyRouteMarkdownParser.Route> routes) {
        LinkedHashMap<String, String> processNameByCode = new LinkedHashMap<>();
        Map<String, String> processCodeByName = new LinkedHashMap<>();
        for (IntGyRouteMarkdownParser.Route route : routes) {
            for (IntGyRouteMarkdownParser.Step step : route.steps()) {
                String existingName = processNameByCode.putIfAbsent(step.processCode(), step.processNameCn());
                if (existingName != null && !Objects.equals(existingName, step.processNameCn())) {
                    throw exception(PRO_ROUTE_IMPORT_PROCESS_CONFLICT,
                            step.processCode(), existingName, step.processNameCn());
                }
                String existingCode = processCodeByName.putIfAbsent(step.processNameCn(), step.processCode());
                if (existingCode != null && !Objects.equals(existingCode, step.processCode())) {
                    throw exception(PRO_ROUTE_IMPORT_PROCESS_NAME_EXISTS, step.processNameCn());
                }
            }
        }
        return processNameByCode;
    }

    private Map<String, MesProProcessDO> validateProcesses(LinkedHashMap<String, String> processNameByCode) {
        Map<String, MesProProcessDO> existingProcessByCode = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : processNameByCode.entrySet()) {
            MesProProcessDO existingByCode = processMapper.selectByCode(entry.getKey());
            if (existingByCode != null) {
                if (!Objects.equals(existingByCode.getName(), entry.getValue())) {
                    throw exception(PRO_ROUTE_IMPORT_PROCESS_CONFLICT,
                            entry.getKey(), existingByCode.getName(), entry.getValue());
                }
                existingProcessByCode.put(entry.getKey(), existingByCode);
                continue;
            }
            MesProProcessDO existingByName = processMapper.selectByName(entry.getValue());
            if (existingByName != null && !Objects.equals(existingByName.getCode(), entry.getKey())) {
                throw exception(PRO_ROUTE_IMPORT_PROCESS_NAME_EXISTS, entry.getValue());
            }
        }
        return existingProcessByCode;
    }

    private List<MesProRouteProcessDO> buildRouteProcesses(
            IntGyRouteMarkdownParser.Route route,
            Long routeId,
            Map<String, Long> processIdByCode,
            Set<String> checkProcessCodes) {
        Set<String> safeCheckProcessCodes = checkProcessCodes == null ? Set.of() : checkProcessCodes;
        List<MesProRouteProcessDO> routeProcesses = new ArrayList<>(route.steps().size());
        for (IntGyRouteMarkdownParser.Step step : route.steps()) {
            Long processId = processIdByCode.get(step.processCode());
            MesProRouteProcessDO routeProcess = new MesProRouteProcessDO();
            routeProcess.setRouteId(routeId);
            routeProcess.setProcessId(processId);
            routeProcess.setSort(step.sequenceNo());
            routeProcess.setKeyFlag(step.finalProcess());
            routeProcess.setCheckFlag(safeCheckProcessCodes.contains(step.processCode()));
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

}
