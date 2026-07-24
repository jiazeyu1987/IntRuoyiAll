package cn.iocoder.yudao.module.dcc.service.directory;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectoryActiveNasTransferRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNasTransferTaskDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNasTransferTaskItemDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNasTransferTaskItemMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNasTransferTaskMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_DIRECTORY_DELETE_NAS_TRANSFER_ACTIVE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_DIRECTORY_NOT_EXISTS;

@Service
@Validated
public class DccDirectoryNasTransferGuardService {

    private static final String TASK_STATUS_WAITING = "WAITING";
    private static final String TASK_STATUS_UPLOADING = "UPLOADING";
    private static final String TASK_STATUS_RUNNING = "RUNNING";
    private static final String TASK_STATUS_CANCELLING = "CANCELLING";
    private static final String STOP_REASON = "Stopped before deleting DCC directory subtree";

    @Resource
    private DccFileDirectoryMapper directoryMapper;
    @Resource
    private DccControlledFileNasTransferTaskMapper taskMapper;
    @Resource
    private DccControlledFileNasTransferTaskItemMapper taskItemMapper;

    public void assertNoActiveTransfer(Long directoryId) {
        DccDirectoryActiveNasTransferRespVO activeTransfer = getActiveTransfer(directoryId);
        if (Boolean.TRUE.equals(activeTransfer.getActive())) {
            throw exception(FILE_DIRECTORY_DELETE_NAS_TRANSFER_ACTIVE);
        }
    }

    public DccDirectoryActiveNasTransferRespVO getActiveTransfer(Long directoryId) {
        DirectoryScope scope = resolveDirectoryScope(directoryId);
        DccControlledFileNasTransferTaskDO task = findActiveTask(scope);
        if (task == null) {
            return DccDirectoryActiveNasTransferRespVO.inactive();
        }
        return buildResponse(task, Boolean.TRUE);
    }

    @Transactional(rollbackFor = Exception.class)
    public DccDirectoryActiveNasTransferRespVO stopActiveTransfer(Long directoryId) {
        DirectoryScope scope = resolveDirectoryScope(directoryId);
        DccControlledFileNasTransferTaskDO task = findActiveTask(scope);
        if (task == null) {
            return DccDirectoryActiveNasTransferRespVO.inactive();
        }

        LocalDateTime now = LocalDateTime.now();
        if (TASK_STATUS_UPLOADING.equals(task.getStatus()) || TASK_STATUS_WAITING.equals(task.getStatus())) {
            taskItemMapper.cancelActiveItemsByTaskId(task.getId(), now);
            taskMapper.cancelWaitingOrCancellingTask(task.getId(), now, STOP_REASON);
            return DccDirectoryActiveNasTransferRespVO.inactive();
        }
        if (TASK_STATUS_RUNNING.equals(task.getStatus())) {
            taskMapper.requestCancelRunningTask(task.getId(), STOP_REASON);
            taskItemMapper.cancelWaitingItemsByTaskId(task.getId(), now);
        }
        DccControlledFileNasTransferTaskDO latestTask = taskMapper.selectById(task.getId());
        if (latestTask == null) {
            return DccDirectoryActiveNasTransferRespVO.inactive();
        }
        if (!isActiveTaskStatus(latestTask.getStatus())) {
            return DccDirectoryActiveNasTransferRespVO.inactive();
        }
        if (TASK_STATUS_CANCELLING.equals(latestTask.getStatus())
                && taskItemMapper.selectRunningItemsByTaskId(task.getId()).isEmpty()) {
            taskMapper.cancelWaitingOrCancellingTask(task.getId(), now, STOP_REASON);
            return DccDirectoryActiveNasTransferRespVO.inactive();
        }
        return buildResponse(latestTask, Boolean.TRUE);
    }

    private DccControlledFileNasTransferTaskDO findActiveTask(DirectoryScope scope) {
        for (DccControlledFileNasTransferTaskDO task : taskMapper.selectActiveTasksForDirectoryDeletion()) {
            List<DccControlledFileNasTransferTaskItemDO> items = taskItemMapper.selectListByTaskId(task.getId());
            if (matchesScope(task, items, scope)) {
                return task;
            }
        }
        return null;
    }

    private boolean matchesScope(DccControlledFileNasTransferTaskDO task,
                                 List<DccControlledFileNasTransferTaskItemDO> items,
                                 DirectoryScope scope) {
        for (DccControlledFileNasTransferTaskItemDO item : items) {
            if (item.getResolvedDirectoryId() != null && scope.directoryIds().contains(item.getResolvedDirectoryId())) {
                return true;
            }
            if (pathsOverlap(item.getNasPath(), scope.directoryPath())) {
                return true;
            }
        }
        for (String selectedPath : parseSelectedNasPaths(task.getSelectedNasPathsJson())) {
            if (pathsOverlap(selectedPath, scope.directoryPath())) {
                return true;
            }
        }
        return false;
    }

    private DccDirectoryActiveNasTransferRespVO buildResponse(DccControlledFileNasTransferTaskDO task, Boolean active) {
        DccDirectoryActiveNasTransferRespVO response = new DccDirectoryActiveNasTransferRespVO();
        response.setActive(active);
        response.setTaskId(task.getId());
        response.setStatus(task.getStatus());
        response.setSelectedNasPaths(parseSelectedNasPaths(task.getSelectedNasPathsJson()));
        response.setRemainingPendingCount(toIntegerCount(taskItemMapper.selectPendingItemCountByTaskId(task.getId())));
        response.setLastFailureMessage(task.getLastFailureMessage());
        return response;
    }

    private DirectoryScope resolveDirectoryScope(Long directoryId) {
        DccFileDirectoryDO root = directoryMapper.selectById(directoryId);
        if (root == null) {
            throw exception(FILE_DIRECTORY_NOT_EXISTS);
        }
        List<DccFileDirectoryDO> directories = directoryMapper.selectList(
                new LambdaQueryWrapperX<DccFileDirectoryDO>()
                        .orderByAsc(DccFileDirectoryDO::getSort)
                        .orderByDesc(DccFileDirectoryDO::getId));
        Map<Long, DccFileDirectoryDO> directoriesById = directories.stream()
                .collect(Collectors.toMap(DccFileDirectoryDO::getId, item -> item,
                        (left, right) -> left, LinkedHashMap::new));
        Map<Long, List<DccFileDirectoryDO>> childrenByParentId = directories.stream()
                .filter(item -> item.getParentId() != null)
                .collect(Collectors.groupingBy(DccFileDirectoryDO::getParentId, LinkedHashMap::new, Collectors.toList()));
        Set<Long> directoryIds = new LinkedHashSet<>();
        List<Long> pendingIds = new ArrayList<>();
        pendingIds.add(directoryId);
        while (!pendingIds.isEmpty()) {
            Long currentId = pendingIds.remove(0);
            if (!directoryIds.add(currentId)) {
                continue;
            }
            childrenByParentId.getOrDefault(currentId, List.of()).stream()
                    .map(DccFileDirectoryDO::getId)
                    .forEach(pendingIds::add);
        }
        return new DirectoryScope(directoryIds, buildDirectoryPath(root, directoriesById));
    }

    private String buildDirectoryPath(DccFileDirectoryDO directory, Map<Long, DccFileDirectoryDO> directoriesById) {
        List<String> reversed = new ArrayList<>();
        DccFileDirectoryDO current = directory;
        while (current != null) {
            reversed.add(current.getName());
            current = current.getParentId() == null ? null : directoriesById.get(current.getParentId());
        }
        List<String> segments = new ArrayList<>(reversed.size());
        for (int index = reversed.size() - 1; index >= 0; index--) {
            segments.add(reversed.get(index));
        }
        return normalizePath(String.join("/", segments));
    }

    private List<String> parseSelectedNasPaths(String selectedNasPathsJson) {
        return JsonUtils.parseArray(StrUtil.blankToDefault(selectedNasPathsJson, "[]"), String.class);
    }

    private boolean pathsOverlap(String left, String right) {
        String normalizedLeft = normalizePath(left);
        String normalizedRight = normalizePath(right);
        if (StrUtil.isBlank(normalizedLeft) || StrUtil.isBlank(normalizedRight)) {
            return false;
        }
        return Objects.equals(normalizedLeft, normalizedRight)
                || normalizedLeft.startsWith(normalizedRight + "/")
                || normalizedRight.startsWith(normalizedLeft + "/");
    }

    private boolean isActiveTaskStatus(String status) {
        return TASK_STATUS_WAITING.equals(status)
                || TASK_STATUS_UPLOADING.equals(status)
                || TASK_STATUS_RUNNING.equals(status)
                || TASK_STATUS_CANCELLING.equals(status);
    }

    private String normalizePath(String rawPath) {
        String normalized = StrUtil.trimToEmpty(rawPath).replace("\\", "/");
        List<String> parts = new ArrayList<>();
        for (String token : normalized.split("/")) {
            String clean = StrUtil.trimToEmpty(token);
            if (StrUtil.isBlank(clean) || ".".equals(clean)) {
                continue;
            }
            if ("..".equals(clean)) {
                if (!parts.isEmpty()) {
                    parts.remove(parts.size() - 1);
                }
                continue;
            }
            parts.add(clean);
        }
        return String.join("/", parts);
    }

    private Integer toIntegerCount(long value) {
        try {
            return Math.toIntExact(value);
        } catch (ArithmeticException exception) {
            throw new IllegalStateException("active nas transfer pending count exceeds integer range: " + value,
                    exception);
        }
    }

    private record DirectoryScope(Set<Long> directoryIds, String directoryPath) {
    }
}
