package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.SimpleParserPublishReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.craft.vo.MesProCraftSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.MesProProcessCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRouteCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version.MesProRouteVersionCreateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.craft.MesProCraftDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.*;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.craft.MesProCraftMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.*;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.craft.*;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.service.pro.route.*;
import com.alibaba.fastjson.*;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftParameters.invalid;

/** 单次预览、单次事务确认；只创建新路线，不通过名称复用工序或覆盖现有路线。 */
@Service
public class MesSimpleParserPublishService {
    @Resource private MesProProcessService processService;
    @Resource private MesProCraftService craftService;
    @Resource private MesTeamLeaderRuntimeConfigService deviceService;
    @Resource private MesProRouteService routeService;
    @Resource private MesProRouteVersionWorkflowService workflow;
    @Resource private MesProRouteCandidateConfigService candidateConfig;
    @Resource private MesProProcessMapper processMapper;
    @Resource private MesProCraftMapper craftMapper;
    @Resource private MesProcessPoolTeamDeviceMapper deviceMapper;
    @Resource private MesDvMachineryMapper machineryMapper;
    @Resource private MesMdItemMapper itemMapper;
    @Resource private MesProRouteMapper routeMapper;
    @Resource private MesProRouteVersionMapper versionMapper;
    @Resource private JdbcTemplate jdbc;
    private final MesProRouteSnapshotCanonicalizer canonical = new MesProRouteSnapshotCanonicalizer();

    /** 台账与路线执行设备按编号组成同一目录，不按当前班组长过滤设备身份。 */
    @Transactional(readOnly = true)
    public List<JSONObject> listDevices() {
        identity();
        Map<String, JSONObject> catalog = new TreeMap<>();
        for (MesProcessPoolTeamDeviceDO device : deviceMapper.selectList(new LambdaQueryWrapperX<>())) {
            String code = device.getDeviceCode();
            if (catalog.containsKey(code)) throw invalid("设备编号不唯一：" + code);
            JSONObject item = new JSONObject();
            item.put("deviceCode", code); item.put("deviceName", device.getDeviceName());
            item.put("enabled", Boolean.TRUE.equals(device.getEnabled()) && "ENABLED".equals(device.getDeviceStatus()));
            catalog.put(code, item);
        }
        Set<String> machineryCodes = new HashSet<>();
        for (MesDvMachineryDO machinery : machineryMapper.selectList(new LambdaQueryWrapperX<>())) {
            String code = machinery.getCode();
            if (!machineryCodes.add(code)) throw invalid("设备台账编号不唯一：" + code);
            JSONObject item = catalog.computeIfAbsent(code, key -> {
                JSONObject added = new JSONObject(); added.put("deviceCode", key); added.put("enabled", true); return added;
            });
            item.put("deviceName", machinery.getName());
        }
        return new ArrayList<>(catalog.values());
    }

    @Transactional(readOnly = true)
    public JSONObject preview(SimpleParserPublishReqVO request) {
        identity();
        String requestId = uuid(request.getRequestId());
        String requestHash = requestHash(request);
        List<Map<String, String>> receipts = jdbc.query(
                "SELECT request_hash,result_json FROM mes_simple_parser_publish_receipt "
                        + "WHERE tenant_id=? AND user_id=? AND request_id=?",
                (rs, index) -> {
                    Map<String, String> row = new HashMap<>();
                    row.put("request_hash", rs.getString("request_hash"));
                    row.put("result_json", rs.getString("result_json"));
                    return row;
                }, TenantContextHolder.getRequiredTenantId(), getLoginUserId(), requestId);
        if (!receipts.isEmpty()) {
            Map<String, String> receipt = receipts.get(0);
            if (!requestHash.equals(receipt.get("request_hash")))
                throw invalid("请求编号已用于其他发布内容，请重新预览并使用新的请求编号");
            if (receipt.get("result_json") != null) return JSON.parseObject(receipt.get("result_json"));
        }
        return plan(request, false).result;
    }

    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public JSONObject publish(SimpleParserPublishReqVO request) {
        identity();
        String requestId = uuid(request.getRequestId());
        String requestHash = requestHash(request);
        long tenant = TenantContextHolder.getRequiredTenantId(), user = getLoginUserId();
        // ponytail: 同一租户/用户/UUID 的数据库行锁即幂等锁；无 Redis 成功缓存。
        jdbc.update("INSERT INTO mes_simple_parser_publish_receipt (tenant_id,user_id,request_id,request_hash) "
                + "VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE request_id=request_id", tenant, user, requestId, requestHash);
        Map<String,Object> receipt = jdbc.queryForObject("SELECT request_hash,result_json FROM mes_simple_parser_publish_receipt "
                + "WHERE tenant_id=? AND user_id=? AND request_id=? FOR UPDATE", (rs, index) -> { Map<String,Object> row=new HashMap<>(); row.put("request_hash",rs.getString("request_hash")); row.put("result_json",rs.getString("result_json")); return row; }, tenant,user,requestId);
        if (!requestHash.equals(receipt.get("request_hash"))) throw invalid("请求编号已用于其他发布内容，请重新预览并使用新的请求编号");
        if (receipt.get("result_json") != null) return JSON.parseObject(receipt.get("result_json").toString());
        Plan plan = plan(request, true);
        if (!Objects.equals(request.getPreviewToken(), plan.result.getString("previewToken")))
            throw invalid("预览后所选工序、设备、工艺或物料已变化，请重新预览");
        if (!plan.blockers.isEmpty()) throw invalid(String.join("；", plan.blockers));

        Map<String,Long> deviceIds = new LinkedHashMap<>();
        for (Object raw : plan.result.getJSONArray("deviceRows")) {
            JSONObject row = (JSONObject) raw;
            // 路线保存执行设备 ID；台账已有但尚无执行记录时仅建立该记录，不新建台账。
            Long id = row.getLong("id");
            if (id == null) id = deviceService.createDevice(MesTeamDeviceSaveReqBO.builder()
                    .leaderUserId(user).deviceCode(row.getString("code")).deviceName(row.getString("name"))
                    .deviceStatus("ENABLED").build());
            deviceIds.put(row.getString("code"), id); row.put("id", id);
        }
        List<Long> processIds = new ArrayList<>();
        for (int i=0; i<plan.processes.size(); i++) {
            JSONObject row = plan.result.getJSONArray("processRows").getJSONObject(i);
            Long id = row.getLong("id");
            if (id == null) {
                MesProProcessCreateReqVO create = new MesProProcessCreateReqVO();
                create.setName(row.getString("name")); create.setStatus(CommonStatusEnum.ENABLE.getStatus());
                create.setProductName(plan.productName);
                id = processService.createProcess(create);
            }
            MesProProcessDO process = processMapper.selectById(id);
            processIds.add(id); row.put("id",id); row.put("code",process.getCode());
        }
        MesProRouteCreateReqVO routeRequest = new MesProRouteCreateReqVO();
        routeRequest.setName(request.getRouteName().trim()); routeRequest.setProcessIds(processIds);
        routeRequest.setRemark("简易解析确认创建，需完成路线治理配置后启用");
        Long routeId = routeService.createRouteWithProcesses(routeRequest);
        MesProRouteVersionDO active = versionMapper.selectActiveByRouteId(routeId);
        MesProRouteVersionCreateReqVO candidateRequest = new MesProRouteVersionCreateReqVO();
        candidateRequest.setRouteId(routeId); candidateRequest.setSourceRouteVersionId(active.getId());
        candidateRequest.setChangeReason("简易解析设备、工艺、物料及关键工序配置");
        MesProRouteVersionDO candidate = workflow.createCandidate(candidateRequest);
        JSONObject configs = JSON.parseObject(candidate.getRouteSnapshotJson()).getJSONObject("configSnapshots");
        JSONObject graph = configs.getJSONObject("flowGraph");
        List<JSONObject> nodes = graph.getJSONArray("nodes").toJavaList(JSONObject.class);
        nodes.sort(Comparator.comparing(n -> n.getInteger("sort")));
        if (nodes.size() != plan.processes.size()) throw invalid("新路线工序数量与预览不一致");
        JSONArray productions = new JSONArray(), batchUse = new JSONArray();
        for (int i=0; i<nodes.size(); i++) {
            JSONObject node = nodes.get(i), source = plan.processes.getJSONObject(i);
            Long processId = processIds.get(i), routeProcessId = node.getLong("routeProcessId");
            if (!Objects.equals(processId,node.getLong("processId"))) throw invalid("新路线工序顺序与预览不一致");
            node.put("keyFlag", source.getBoolean("criticalProcess"));
            JSONObject config = new JSONObject(true);
            config.put("routeProcessId",routeProcessId); config.put("processId",processId);
            config.put("processName",source.getString("name")); config.put("sort",i+1);
            config.put("overagePercent",BigDecimal.ZERO); config.put("lossReasons",new JSONArray());
            JSONArray groups = new JSONArray();
            List<MesProCraftParameters.Binding> bindings = new ArrayList<>();
            JSONArray sourceGroups = array(source,"equipmentGroups");
            for (int g=0; g<sourceGroups.size(); g++) {
                JSONObject sourceGroup=sourceGroups.getJSONObject(g);
                List<Long> ids=array(sourceGroup,"equipmentOptions").toJavaList(JSONObject.class).stream()
                        .map(d -> deviceIds.get(text(d,"code",64))).toList();
                JSONObject group=new JSONObject(true);
                group.put("deviceGroupKey","SIMPLE_"+(g+1)); group.put("selectionMode",sourceGroup.getString("selectionMode"));
                group.put("deviceIds",ids); group.put("sort",g+1); groups.add(group);
                if (array(sourceGroup,"parameters").isEmpty()) continue;
                JSONObject craftRow=craftRow(plan,i,g);
                JSONArray parameters=plan.parameters.get(i+":"+g);
                Long craftId=craftRow.getLong("id");
                MesProCraftDO craft;
                if (craftId == null) {
                    MesProCraftSaveReqVO create=new MesProCraftSaveReqVO();
                    create.setName(craftRow.getString("name")); create.setProcessId(processId);
                    create.setParameters(parameters); create.setReason("简易解析确认创建");
                    craft=craftService.create(create);
                } else craft=craftService.require(craftId);
                craftRow.put("id",craft.getId()); craftRow.put("code",craft.getCode());
                bindings.add(new MesProCraftParameters.Binding(craft.getId(),craft.getCode(),parameters,ids));
            }
            config.put("deviceSelectionGroups",groups); config.put("parameterRules",new JSONArray());
            if (!bindings.isEmpty()) MesProCraftParameters.bindAll(config,bindings);
            productions.add(config);
            JSONObject batch=new JSONObject(true);
            batch.put("routeProcessId",routeProcessId); batch.put("enabled",false);
            batch.put("inputMaterialIds",plan.materialIds.get(i+":inputs"));
            batch.put("outputMaterialIds",plan.materialIds.get(i+":outputs"));
            // 批记录、表单槽位与开始配置均不由 Word 推断。
            batch.put("batchRecordReports",new JSONArray()); batch.put("formBindings",new JSONArray());
            batch.put("batchRecordBindingSnapshotExplicit",true); batchUse.add(batch);
        }
        graph.put("nodes",nodes);
        candidateConfig.saveConfigSnapshots(candidate.getId(),candidate.getRouteSnapshotSha256(),Map.of(
                "flowGraph",graph,"productionProcessConfigSchemaVersion",1,
                "productionProcessConfigs",productions,"batchUseConfigs",batchUse));
        MesProRouteDO route=routeMapper.selectById(routeId);
        JSONObject result=plan.result;
        result.put("routeId",routeId); result.put("routeCode",route.getCode());
        result.put("routeVersionId",active.getId()); result.put("routeVersionNo",active.getVersionNo());
        result.put("routeCandidateVersionId",candidate.getId()); result.put("routeCandidateVersionNo",candidate.getVersionNo());
        result.put("routeStatus","DISABLED"); result.put("candidateStatus","DRAFT");
        result.put("processes",JSON.parseArray(result.getJSONArray("processRows").toJSONString())); result.put("devices",JSON.parseArray(result.getJSONArray("deviceRows").toJSONString()));
        result.put("crafts",JSON.parseArray(result.getJSONArray("craftRows").toJSONString()));
        if (jdbc.update("UPDATE mes_simple_parser_publish_receipt SET result_json=? WHERE tenant_id=? AND user_id=? AND request_id=?",
                result.toJSONString(),tenant,user,requestId)!=1) throw invalid("发布回执保存失败");
        return result;
    }

    private Plan plan(SimpleParserPublishReqVO request, boolean lock) {
        uuid(request.getRequestId());
        String routeName=requireText(request.getRouteName(),"路线名称",128);
        JSONObject root=JSON.parseObject(JSON.toJSONString(request.getRecognitionJson(), com.alibaba.fastjson.serializer.SerializerFeature.DisableCircularReferenceDetect));
        if (root==null || !(Objects.equals(2, root.getInteger("schemaVersion")) || Objects.equals(3, root.getInteger("schemaVersion")))) throw invalid("识别 JSON 版本无效");
        JSONArray processes=array(root,"processes");
        if (processes.isEmpty() || processes.size()>200) throw invalid("工序数量应为 1 至 200");
        String productName=text(object(root.get("product")),"name",128);
        Plan plan=new Plan(processes,productName);
        JSONArray processRows=new JSONArray(),deviceRows=new JSONArray(),craftRows=new JSONArray(),materialRows=new JSONArray(),unmatchedMaterials=new JSONArray(),warnings=new JSONArray(),state=new JSONArray();
        List<MesProRouteDO> routes=routeMapper.selectList(new LambdaQueryWrapperX<MesProRouteDO>()
                .apply("TRIM(name) = {0}",routeName).last(lock?"FOR UPDATE":""));
        if (!routes.isEmpty()) plan.blockers.add("路线名称已存在，请填写新的路线名称");
        state.add(routes);
        Map<Integer,SimpleParserPublishReqVO.ProcessSelection> selections=new HashMap<>();
        if (request.getProcessSelections()==null) throw invalid("工序选择列表不能为空对象");
        for (var selection:request.getProcessSelections()) {
            if (selection==null || selection.getProcessIndex()==null || selection.getProcessIndex()<0 || selection.getProcessIndex()>=processes.size()
                    || selections.put(selection.getProcessIndex(),selection)!=null) throw invalid("工序选择索引无效或重复");
        }
        Set<Long> reusedProcesses=new HashSet<>();
        Map<String,JSONObject> devices=new LinkedHashMap<>();
        for (int i=0;i<processes.size();i++) {
            JSONObject process=object(processes.get(i));
            String name=text(process,"name",128);
            if (!(process.get("criticalProcess") instanceof Boolean)) throw invalid("关键工序标记必须明确为 true 或 false");
            var selection=selections.get(i);
            Long processId=selection==null?null:selection.getProcessId();
            JSONObject processRow=row(name,processId); processRow.put("index",i);
            if (processId!=null) {
                MesProProcessDO existing=processMapper.selectOne(new LambdaQueryWrapperX<MesProProcessDO>()
                        .eq(MesProProcessDO::getId,processId).last(lock?"FOR UPDATE":""));
                if (existing==null || !Objects.equals(existing.getStatus(),0)) throw invalid("复用工序不存在或未启用："+processId);
                if (!existing.getName().trim().equals(name)) throw invalid("复用工序名称与校对结果不一致："+name);
                if (!reusedProcesses.add(processId)) throw invalid("同一路线不能重复复用同一个工序 ID");
                processRow.put("code",existing.getCode()); state.add(existing);
            }
            processRows.add(processRow);
            for(String direction:List.of("inputs","outputs")) {
                List<Long> ids=new ArrayList<>();
                JSONArray materials=array(process,direction);
                for(int materialIndex=0;materialIndex<materials.size();materialIndex++) {
                    JSONObject material=object(materials.get(materialIndex));
                    String code=optionalText(material,"code"), materialName=optionalText(material,"name");
                    String context="工序 "+(i+1)+"（"+name+"）"+(direction.equals("inputs")?"输入":"输出")
                            +"物料（编码："+(code==null?"未提供":code)+"，名称："+(materialName==null?"未提供":materialName)+"）：";
                    if (code==null && materialName==null) {
                        plan.blockers.add(context+"未提供编码或名称，无法确认目录物料");
                        continue;
                    }
                    if (code==null) materialName=text(material,"name",255);
                    var query=new LambdaQueryWrapperX<MesMdItemDO>();
                    if(code!=null) query.eq(MesMdItemDO::getCode,code); else query.eq(MesMdItemDO::getName,materialName);
                    List<MesMdItemDO> items=itemMapper.selectList(query.last(lock?"FOR UPDATE":""));
                    if(items.isEmpty()) {
                        unmatchedMaterials.add(context+"目录中不存在匹配物料，本次不绑定，可在工艺路线中补充");
                        continue;
                    }
                    if(items.size()!=1 || !Objects.equals(items.get(0).getStatus(),0)) {
                        plan.blockers.add(context+(items.size()>1?"目录匹配不唯一":"目录物料未启用"));
                        continue;
                    }
                    MesMdItemDO item=items.get(0);
                    if (code==null && !item.getName().trim().equals(materialName)) {
                        plan.blockers.add(context+"无编码物料名称必须与目录精确匹配");
                        continue;
                    }
                    if (code!=null && !Objects.equals(item.getName().trim(),materialName))
                        warnings.add("工序 "+(i+1)+" 物料 "+code+"：识别名称“"+(materialName==null?"未识别":materialName)+"”按目录显示为“"+item.getName()+"”");
                    JSONObject materialRow=row(item.getName(),item.getId());materialRow.put("code",item.getCode());
                    materialRow.put("sourceName",materialName);materialRow.put("processIndex",i);materialRow.put("direction",direction);materialRow.put("index",materialIndex);
                    materialRows.add(materialRow);
                    material.put("name",item.getName());material.put("code",item.getCode());
                    ids.add(item.getId());state.add(item);
                }
                if(new HashSet<>(ids).size()!=ids.size()) throw invalid("工序物料重复："+name);
                plan.materialIds.put(i+":"+direction,ids);
            }
            JSONArray groups=array(process,"equipmentGroups");
            Map<Integer,Long> craftSelections=new HashMap<>();
            if(selection!=null && selection.getCraftSelections()!=null) for(var chosen:selection.getCraftSelections()) {
                if(chosen==null || chosen.getGroupIndex()==null || chosen.getGroupIndex()<0 || chosen.getGroupIndex()>=groups.size()
                        || chosen.getCraftId()==null || craftSelections.put(chosen.getGroupIndex(),chosen.getCraftId())!=null) throw invalid("工艺选择索引无效或重复");
            }
            Set<String> processDeviceCodes=new HashSet<>(); Set<Long> processCraftIds=new HashSet<>();
            for(int g=0;g<groups.size();g++) {
                JSONObject group=object(groups.get(g));
                if(!("SINGLE".equals(group.getString("selectionMode")) || "MULTIPLE".equals(group.getString("selectionMode")))) throw invalid("设备组须明确单选或多选");
                JSONArray equipment=array(group,"equipmentOptions");
                if(equipment.isEmpty()) throw invalid("设备参数组必须选择有编号的设备："+name);
                for(Object raw:equipment) {
                    JSONObject equipmentOption=object(raw);
                    String code=text(equipmentOption,"code",64), deviceName=optionalText(equipmentOption,"name");
                    if(!processDeviceCodes.add(code)) throw invalid("同一工序设备重复分组："+code);
                    if(devices.containsKey(code)) {
                        JSONObject known=devices.get(code);
                        if ("CREATE".equals(known.getString("action")) && !Objects.equals(known.getString("name"),deviceName))
                            throw invalid("待新建设备编号对应多个名称，请明确名称："+code);
                        if (!Objects.equals(known.getString("name"),deviceName))
                            warnings.add("工序 "+(i+1)+" 设备 "+code+"：识别名称“"+(deviceName==null?"未识别":deviceName)+"”按目录显示为“"+known.getString("name")+"”");
                        equipmentOption.put("name",known.getString("name"));
                        continue;
                    }
                    List<MesProcessPoolTeamDeviceDO> matches=deviceMapper.selectList(new LambdaQueryWrapperX<MesProcessPoolTeamDeviceDO>()
                            .eq(MesProcessPoolTeamDeviceDO::getDeviceCode,code).last(lock?"FOR UPDATE":""));
                    if(matches.size()>1) throw invalid("设备编号不唯一："+code);
                    MesProcessPoolTeamDeviceDO device=matches.isEmpty()?null:matches.get(0);
                    if(device!=null && (!Boolean.TRUE.equals(device.getEnabled()) || !"ENABLED".equals(device.getDeviceStatus()))) throw invalid("设备未启用："+code);
                    List<MesDvMachineryDO> machineryMatches=machineryMapper.selectList(new LambdaQueryWrapperX<MesDvMachineryDO>()
                            .eq(MesDvMachineryDO::getCode,code).last(lock?"FOR UPDATE":""));
                    if(machineryMatches.size()>1) throw invalid("设备台账编号不唯一："+code);
                    MesDvMachineryDO machinery=machineryMatches.isEmpty()?null:machineryMatches.get(0);
                    String resolvedName=machinery!=null?machinery.getName():device!=null?device.getDeviceName():text(equipmentOption,"name",128);
                    if ((device!=null || machinery!=null) && !Objects.equals(resolvedName,deviceName))
                        warnings.add("工序 "+(i+1)+" 设备 "+code+"：识别名称“"+(deviceName==null?"未识别":deviceName)+"”按目录显示为“"+resolvedName+"”");
                    equipmentOption.put("name",resolvedName);
                    JSONObject deviceRow=row(resolvedName,device==null?null:device.getId()); deviceRow.put("code",code);
                    if(machinery!=null) {
                        deviceRow.put("machineryId",machinery.getId()); deviceRow.put("action","REUSE"); state.add(machinery);
                    }
                    devices.put(code,deviceRow); deviceRows.add(deviceRow); state.add(deviceRow); if(device!=null) state.add(device);
                }
                Long craftId=craftSelections.get(g);
                if (array(group,"parameters").isEmpty()) {
                    if (craftId!=null) throw invalid("无参数设备组不能复用含参数工艺");
                    continue;
                }
                JSONArray parameters=parameters(array(group,"parameters"));
                String craftName=name.substring(0, Math.min(name.length(), 110))+" / 参数组"+(g+1);
                JSONObject craftRow=row(craftName,craftId);
                if(craftId!=null) {
                    if(processId==null) throw invalid("复用工艺前必须明确选择所属工序");
                    if(!processCraftIds.add(craftId)) throw invalid("同一工序不能重复选择相同工艺");
                    MesProCraftDO craft=craftMapper.selectOne(new LambdaQueryWrapperX<MesProCraftDO>()
                            .eq(MesProCraftDO::getId,craftId).last(lock?"FOR UPDATE":""));
                    if(craft==null || !processId.equals(craft.getProcessId())) throw invalid("工艺不属于所选工序");
                    JSONArray existing=MesProCraftParameters.normalize(JSON.parseArray(craft.getParametersJson()));
                    if(!semanticParameters(parameters).equals(semanticParameters(existing))) throw invalid("复用工艺参数与校对结果不一致："+craft.getName());
                    parameters=existing; craftRow.put("name",craft.getName()); craftRow.put("code",craft.getCode()); state.add(craft);
                }
                validateParameters(parameters);
                plan.parameters.put(i+":"+g,parameters);
                craftRow.put("processIndex",i); craftRow.put("groupIndex",g); craftRow.put("parameterCount",parameters.size()); craftRows.add(craftRow);
            }
        }
        JSONObject result=plan.result;
        result.put("requestId",uuid(request.getRequestId())); result.put("routeName",routeName); result.put("processCount",processes.size());
        result.put("processRows",processRows); result.put("deviceRows",deviceRows); result.put("craftRows",craftRows);
        result.put("materialRows",materialRows); result.put("unmatchedMaterials",unmatchedMaterials); result.put("warnings",warnings);
        result.put("route",Map.of("name",routeName,"action","CREATE")); result.put("blockers",plan.blockers);
        result.put("previewToken",canonical.sha256(JSON.toJSONString(Map.of("requestHash",requestHash(request),"records",state,
                "tenant",TenantContextHolder.getRequiredTenantId(),"user",getLoginUserId()))));
        return plan;
    }

    static JSONArray parameters(JSONArray source) {
        if(source.isEmpty()) throw invalid("设备参数组必须至少包含一个参数");
        JSONArray result=new JSONArray(); Set<String> names=new HashSet<>();
        for(int i=0;i<source.size();i++) {
            JSONObject item=object(source.get(i)), ui=object(item.get("ui"));
            String name=text(item,"name",128);
            if(!names.add(name)) throw invalid("同组参数名称重复："+name);
            JSONObject p=new JSONObject(true);
            p.put("parameterCode",String.format(Locale.ROOT,"P%03d",i+1)); p.put("parameterName",optionalText(ui,"displayName")==null?name:text(ui,"displayName",128));
            p.put("standardText",text(item,"referenceValue",512)); p.put("unit",optionalText(ui,"unit")); p.put("sort",i+1);
            String control=text(ui,"control",16);
            switch(control) {
                case "number" -> {
                    BigDecimal value=decimal(ui,"defaultValue",true),step=decimal(ui,"step",true);
                    if(step.signum()<=0) throw invalid("数值步长必须大于零");
                    int scale=Math.max(0,step.stripTrailingZeros().scale());
                    // 当前路线参数模型保存小数位，不能静默丢弃任意步长。
                    if(step.compareTo(BigDecimal.ONE.scaleByPowerOfTen(-scale))!=0) throw invalid("当前工艺仅支持步长 1、0.1、0.01 等小数精度，请校对步长");
                    p.put("decimalScale",scale); p.put("defaultValue",value);
                    p.put("lowerLimit",decimal(ui,"min",false)); p.put("upperLimit",decimal(ui,"max",false));
                    boolean fractional = scale>0 || value.stripTrailingZeros().scale()>0
                            || (p.getBigDecimal("lowerLimit")!=null && p.getBigDecimal("lowerLimit").stripTrailingZeros().scale()>0)
                            || (p.getBigDecimal("upperLimit")!=null && p.getBigDecimal("upperLimit").stripTrailingZeros().scale()>0);
                    p.put("valueType",fractional?"DECIMAL":"INTEGER");
                }
                case "select" -> {
                    JSONArray options=array(ui,"options");
                    if(options.isEmpty() || options.stream().anyMatch(v -> !(v instanceof String || v instanceof Number))) throw invalid("下拉选项无效");
                    String value=text(ui,"defaultValue",512);
                    List<String> labels=options.stream().map(Object::toString).toList();
                    if(!labels.contains(value) || new HashSet<>(labels).size()!=labels.size()) throw invalid("下拉默认值或选项无效");
                    p.put("valueType","SELECT"); p.put("optionValuesJson",JSON.toJSONString(labels)); p.put("defaultText",value);
                }
                case "text" -> { p.put("valueType","TEXT_STANDARD"); p.put("defaultText",optionalText(ui,"defaultValue")); }
                default -> throw invalid("不支持的参数控件："+control);
            }
            result.add(p);
        }
        return MesProCraftParameters.normalize(result);
    }
    static String semanticParameters(JSONArray parameters) {
        List<JSONObject> values=new ArrayList<>();
        for(Object raw:MesProCraftParameters.normalize(parameters)) {
            JSONObject p=JSON.parseObject(JSON.toJSONString(raw)); p.remove("parameterCode"); p.remove("sort"); values.add(p);
        }
        values.sort(Comparator.comparing(p -> p.getString("parameterName")));
        return MesProCraftParameters.canonical(values);
    }
    private static void validateParameters(JSONArray parameters) {
        JSONObject config=new JSONObject(true); config.put("routeProcessId",1L); config.put("processId",1L);
        config.put("overagePercent",0); config.put("lossReasons",new JSONArray());
        config.put("deviceSelectionGroups",JSON.parseArray("[{\"deviceGroupKey\":\"check\",\"selectionMode\":\"SINGLE\",\"deviceIds\":[1]}]"));
        config.put("parameterRules",MesProCraftParameters.expand(config,parameters,List.of(1L)));
        JSONObject snapshot=new JSONObject(true); snapshot.put("productionProcessConfigSchemaVersion",1);
        snapshot.put("productionProcessConfigs",List.of(config)); snapshot.put("flowGraph",Map.of("nodes",List.of(Map.of("routeProcessId",1L,"processId",1L))));
        MesProRouteCandidateConfigServiceImpl.validateProductionProcessConfigs(0L,JSON.parseObject(snapshot.toJSONString()));
    }
    private String requestHash(SimpleParserPublishReqVO request) {
        JSONObject value=(JSONObject)JSON.toJSON(request); value.remove("previewToken"); value.put("requestId",uuid(request.getRequestId()));
        return canonical.sha256(value.toJSONString());
    }
    private static JSONObject craftRow(Plan plan,int process,int group) {
        return plan.result.getJSONArray("craftRows").toJavaList(JSONObject.class).stream()
                .filter(r -> r.getIntValue("processIndex")==process && r.getIntValue("groupIndex")==group).findFirst().orElseThrow();
    }
    private static JSONObject row(String name,Long id) { JSONObject r=new JSONObject(true);r.put("name",name);r.put("action",id==null?"CREATE":"REUSE");if(id!=null)r.put("id",id);return r; }
    private static JSONObject object(Object value) { if(!(value instanceof Map<?,?>)) throw invalid("识别对象结构无效");return (JSONObject)JSON.toJSON(value); }
    private static JSONArray array(JSONObject object,String key) { Object value=object.get(key);if(!(value instanceof List<?>))throw invalid("识别结果缺少数组："+key);return (JSONArray)JSON.toJSON(value); }
    private static String text(JSONObject object,String key,int max) { return requireText(optionalText(object,key),key,max); }
    private static String requireText(String value,String name,int max) { if(value==null || value.isBlank() || value.trim().length()>max)throw invalid(name+"不能为空且长度不能超过"+max);return value.trim(); }
    private static String optionalText(JSONObject object,String key) { Object value=object.get(key);if(value==null)return null;if(!(value instanceof String || value instanceof Number))throw invalid(key+"类型无效");String s=value.toString().trim();return s.isEmpty()?null:s; }
    private static BigDecimal decimal(JSONObject object,String key,boolean required) {
        String text=optionalText(object,key);if(text==null){if(required)throw invalid("缺少数值："+key);return null;}
        try{return new BigDecimal(text);}catch(NumberFormatException ex){throw invalid("数值无效："+key);}
    }
    private static String uuid(String value) { if(value==null || !value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))throw invalid("请求编号必须为 UUID");return UUID.fromString(value).toString(); }
    private static void identity() { TenantContextHolder.getRequiredTenantId();if(getLoginUserId()==null)throw invalid("缺少当前登录用户"); }
    private static class Plan {
        final JSONObject result=new JSONObject(true);final JSONArray processes;final String productName;
        final List<String> blockers=new ArrayList<>();final Map<String,JSONArray> parameters=new HashMap<>();final Map<String,List<Long>> materialIds=new HashMap<>();
        Plan(JSONArray processes,String productName){this.processes=processes;this.productName=productName;}
    }
}
