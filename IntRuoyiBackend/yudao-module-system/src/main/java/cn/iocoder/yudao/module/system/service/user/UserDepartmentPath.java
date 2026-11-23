package cn.iocoder.yudao.module.system.service.user;

import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSaveReqVO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.validation.ValidationUtils;
import java.util.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/** 用户迁移文件携带从根到所属部门的路径，不使用源租户的部门编号关联目标租户。 */
public final class UserDepartmentPath {
    private static final String SEPARATOR = " / ";

    private UserDepartmentPath() { }

    public static String exportPath(Long id, Map<Long, DeptDO> cache, DeptService service) {
        if (id == null || DeptDO.PARENT_ID_ROOT.equals(id)) return null;
        LinkedList<String> names = new LinkedList<>();
        Set<Long> visited = new HashSet<>();
        while (!DeptDO.PARENT_ID_ROOT.equals(id)) {
            if (id == null || !visited.add(id)) throw invalidParamException("部门上级关系缺失或存在循环，无法导出完整部门路径");
            DeptDO dept = cache.get(id);
            if (dept == null) {
                dept = service.getDept(id);
                if (dept == null) throw invalidParamException("部门[" + id + "]不存在，无法导出完整部门路径");
                cache.put(id, dept);
            }
            if (dept.getName() == null || dept.getName().isBlank() || dept.getName().contains(SEPARATOR)) {
                throw invalidParamException("部门名称为空或包含路径分隔符“ / ”，无法导出部门路径");
            }
            names.addFirst(dept.getName());
            id = dept.getParentId();
        }
        return String.join(SEPARATOR, names);
    }

    public static List<String> parse(String path, String leafName) {
        List<String> names = Arrays.asList(path.split(java.util.regex.Pattern.quote(SEPARATOR), -1));
        if (names.stream().anyMatch(String::isBlank)
                || (leafName != null && !leafName.isBlank() && !names.get(names.size() - 1).equals(leafName))) {
            throw invalidParamException("部门路径无效或与部门名称不一致：" + path);
        }
        return names;
    }

    /** 数组格式可无损表达同一人负责多个部门，包括名称中含换行或标点的情况。 */
    public static List<String> parseLeaderPaths(String text) {
        if (text == null || text.isBlank()) return List.of();
        com.fasterxml.jackson.databind.JsonNode array;
        try {
            array = cn.iocoder.yudao.framework.common.util.json.JsonUtils.getObjectMapper()
                    .reader().with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readTree(text);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            var error = invalidParamException("负责部门路径格式错误，必须为完整部门路径的 JSON 字符串数组");
            error.initCause(ex);
            throw error;
        }
        if (array == null || !array.isArray()) {
            throw invalidParamException("负责部门路径格式错误，必须为完整部门路径的 JSON 字符串数组");
        }
        List<String> paths = new ArrayList<>();
        for (var item : array) {
            if (!item.isTextual() || item.textValue().isBlank()) {
                throw invalidParamException("负责部门路径必须为非空字符串");
            }
            String path = item.textValue();
            parse(path, null);
            if (paths.contains(path)) throw invalidParamException("负责部门路径重复：" + path);
            paths.add(path);
        }
        return paths;
    }

    public static Long resolve(String path, String leafName, Map<String, List<DeptDO>> departments,
                               DeptService service, boolean create) {
        Long parentId = DeptDO.PARENT_ID_ROOT;
        for (String name : parse(path, leafName)) {
            Long currentParent = parentId;
            List<DeptDO> matches = departments.getOrDefault(name, List.of()).stream()
                    .filter(dept -> currentParent.equals(dept.getParentId())).toList();
            if (matches.size() > 1) throw invalidParamException("同一上级下存在重名部门：" + path);
            if (!matches.isEmpty()) {
                parentId = matches.get(0).getId();
                continue;
            }
            if (!create) return null;
            DeptSaveReqVO request = new DeptSaveReqVO();
            request.setName(name);
            request.setParentId(parentId);
            request.setSort(0);
            request.setStatus(CommonStatusEnum.ENABLE.getStatus());
            ValidationUtils.validate(request);
            Long id = service.createDept(request);
            departments.computeIfAbsent(name, key -> new ArrayList<>())
                    .add(new DeptDO().setId(id).setName(name).setParentId(parentId)
                            .setSort(0).setStatus(CommonStatusEnum.ENABLE.getStatus()));
            parentId = id;
        }
        return parentId;
    }
}
