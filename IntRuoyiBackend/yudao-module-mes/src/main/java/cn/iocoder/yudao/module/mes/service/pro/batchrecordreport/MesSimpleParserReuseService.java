package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.SimpleParserPublishReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.craft.MesProCraftDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.craft.MesProCraftMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftParameters;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftParameters.invalid;

/** 只推荐已启用的同名工序及参数完全一致的工艺，不改写解析结果。 */
@Service
public class MesSimpleParserReuseService {
    @Resource private MesProProcessMapper processMapper;
    @Resource private MesProCraftMapper craftMapper;
    @Resource private NamedParameterJdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public List<JSONObject> matches(SimpleParserPublishReqVO request) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        JSONObject root = JSON.parseObject(JSON.toJSONString(request.getRecognitionJson(),
                com.alibaba.fastjson.serializer.SerializerFeature.DisableCircularReferenceDetect));
        JSONArray inputs = root.getJSONArray("processes");
        if (inputs == null || inputs.isEmpty() || inputs.size() > 200) throw invalid("工序数量应为 1 至 200");
        Set<String> names = new LinkedHashSet<>();
        for (Object raw : inputs) {
            String name = ((JSONObject) raw).getString("name");
            if (name == null || name.isBlank()) throw invalid("请填写工序名称");
            names.add(name.trim());
        }
        // 数据库排序规则可能忽略大小写；读取后仍按名称精确比较。
        List<MesProProcessDO> processes = processMapper.selectList(new LambdaQueryWrapperX<MesProProcessDO>()
                .eq(MesProProcessDO::getStatus, 0).in(MesProProcessDO::getName, names)
                .orderByAsc(MesProProcessDO::getId));
        List<Long> ids = processes.stream().map(MesProProcessDO::getId).toList();
        Map<Long, List<MesProCraftDO>> crafts = ids.isEmpty() ? Map.of() : craftMapper.selectList(
                new LambdaQueryWrapperX<MesProCraftDO>().in(MesProCraftDO::getProcessId, ids)
                        .orderByAsc(MesProCraftDO::getId)).stream()
                .collect(Collectors.groupingBy(MesProCraftDO::getProcessId));
        Map<Long, List<JSONObject>> workshops = cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessWorkshopUsage.load(jdbc, ids);
        List<JSONObject> result = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            JSONObject input = inputs.getJSONObject(i);
            JSONArray groups = input.getJSONArray("equipmentGroups");
            if (groups == null) throw invalid("识别结果缺少设备组");
            JSONObject row = new JSONObject(true);
            row.put("processIndex", i); row.put("name", input.getString("name").trim());
            List<JSONObject> candidates = new ArrayList<>();
            for (MesProProcessDO process : processes) {
                if (!process.getName().trim().equals(row.getString("name"))) continue;
                JSONObject candidate = new JSONObject(true);
                candidate.put("processId", process.getId()); candidate.put("code", process.getCode());
                candidate.put("name", process.getName()); candidate.put("productName", process.getProductName());
                List<JSONObject> usage = workshops.getOrDefault(process.getId(), List.of());
                candidate.put("workshops", usage);
                candidate.put("currentWorkshop", usage.stream().anyMatch(w -> Objects.equals(
                        request.getWorkshopId(), w.getLong("id"))));
                candidate.put("groups", matchGroups(groups, crafts.getOrDefault(process.getId(), List.of())));
                candidates.add(candidate);
            }
            candidates.sort(Comparator.comparing((JSONObject c) -> !c.getBooleanValue("currentWorkshop"))
                    .thenComparing(c -> c.getLong("processId")));
            row.put("candidates", candidates); result.add(row);
        }
        return result;
    }

    static List<JSONObject> matchGroups(JSONArray groups, List<MesProCraftDO> crafts) {
        List<JSONObject> result = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) {
            JSONObject group = groups.getJSONObject(i);
            if (Boolean.FALSE.equals(group.getBoolean("equipmentRequired"))) continue;
            JSONArray source = group.getJSONArray("parameters");
            if (source == null) throw invalid("识别结果缺少工艺参数");
            if (source.isEmpty()) continue;
            JSONArray parameters = MesSimpleParserPublishService.parameters(source);
            String signature = MesSimpleParserPublishService.semanticParameters(parameters);
            List<JSONObject> matches = new ArrayList<>();
            for (MesProCraftDO craft : crafts) {
                JSONArray existing = MesProCraftParameters.normalize(JSON.parseArray(craft.getParametersJson()));
                if (!signature.equals(MesSimpleParserPublishService.semanticParameters(existing))) continue;
                JSONObject match = new JSONObject(true);
                match.put("id", craft.getId()); match.put("code", craft.getCode()); match.put("name", craft.getName());
                match.put("parameters", existing); matches.add(match);
            }
            JSONObject row = new JSONObject(true);
            row.put("groupIndex", i); row.put("parameters", parameters); row.put("crafts", matches);
            result.add(row);
        }
        return result;
    }

}
