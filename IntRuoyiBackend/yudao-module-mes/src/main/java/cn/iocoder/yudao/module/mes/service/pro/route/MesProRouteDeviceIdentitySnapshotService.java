package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftParameters.invalid;

/** 版本设备身份独立留存；生命周期冻结后不再读取当前名称补齐历史资料。 */
@Service
public class MesProRouteDeviceIdentitySnapshotService {
    @Resource private MesProRouteVersionMapper versions;
    @Resource private MesProcessPoolTeamDeviceMapper devices;
    @Resource private NamedParameterJdbcTemplate jdbc;

    public record Identity(Long deviceId, String deviceCode, String deviceName) {}

    @Transactional(rollbackFor = Exception.class)
    public void captureCreated(MesProRouteVersionDO version) {
        if (!jdbc.queryForList("SELECT route_version_id FROM mes_pro_route_device_identity_snapshot "
                + "WHERE tenant_id=:tenant AND route_version_id=:version", keys(version.getId())).isEmpty())
            throw invalid("路线版本设备资料已存在，不能重新初始化");
        write(version, null, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public void captureDraft(Long versionId) {
        captureDraft(versionId, null);
    }

    /** proposal 仅由已落库的整批变更调用，不从页面接收设备名称快照。 */
    @Transactional(rollbackFor = Exception.class)
    public void captureDraft(Long versionId, JSONObject proposal) {
        MesProRouteVersionDO version = versions.selectByIdForUpdate(versionId);
        if (version == null || Boolean.TRUE.equals(version.getActive()) || !"DRAFT".equals(version.getLifecycleStatus()))
            throw invalid("只有草稿版本可以保存设备资料");
        write(version, proposal, false);
    }

    private void write(MesProRouteVersionDO version, JSONObject proposal, boolean created) {
        Set<Long> ids = deviceIds(version);
        Map<Long, Identity> captured = new TreeMap<>();
        if (!ids.isEmpty()) for (var device : devices.selectBatchIds(ids)) {
            captured.put(device.getId(), new Identity(device.getId(), device.getDeviceCode(), device.getDeviceName()));
        }
        if (!captured.keySet().equals(ids)) throw invalid("路线版本引用的设备已不存在");
        if (proposal != null && "DEVICE".equals(proposal.getJSONObject("request").getString("subjectType"))) {
            JSONObject request = proposal.getJSONObject("request"), data = proposal.getJSONObject("data");
            Set<Long> affected = "REPLACE".equals(request.getString("action"))
                    ? Set.of(proposal.getLong("targetDeviceId"))
                    : new HashSet<>(proposal.getJSONArray("sourceDeviceIds").toJavaList(Long.class));
            for (Long id : ids) if (affected.contains(id))
                captured.put(id, new Identity(id, data.getString("code"), data.getString("name")));
        }
        JSONObject json = new JSONObject(true);
        captured.forEach((id, identity) -> {
            validate(identity, version.getId());
            JSONObject item = new JSONObject(true);
            item.put("deviceId", id);
            item.put("deviceCode", identity.deviceCode());
            item.put("deviceName", identity.deviceName());
            json.put(id.toString(), item);
        });
        String value = json.toJSONString();
        Map<String, Object> params = new HashMap<>(keys(version.getId()));
        params.put("json", value); params.put("sha", DigestUtil.sha256Hex(value));
        params.put("actor", Objects.toString(SecurityFrameworkUtils.getLoginUserId(), ""));
        boolean exists = !created && !jdbc.queryForList("SELECT route_version_id FROM mes_pro_route_device_identity_snapshot "
                + "WHERE tenant_id=:tenant AND route_version_id=:version", keys(version.getId())).isEmpty();
        if (exists) {
            jdbc.update("""
                UPDATE mes_pro_route_device_identity_snapshot SET identities_json=:json, identities_sha256=:sha,
                  capture_source='CANDIDATE', captured_at=CURRENT_TIMESTAMP, captured_by=:actor
                WHERE tenant_id=:tenant AND route_version_id=:version
                """, params);
            return;
        }
        if (jdbc.update("""
                INSERT INTO mes_pro_route_device_identity_snapshot
                  (tenant_id,route_version_id,identities_json,identities_sha256,capture_source,captured_by)
                VALUES (:tenant,:version,:json,:sha,'CANDIDATE',:actor)
                """, params) != 1) throw invalid("路线版本设备资料保存失败");
    }

    @Transactional(readOnly = true)
    public Map<Long, Identity> require(Long versionId, Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        var rows = jdbc.query("""
                SELECT identities_json,identities_sha256 FROM mes_pro_route_device_identity_snapshot
                WHERE tenant_id=:tenant AND route_version_id=:version
                """, keys(versionId), (rs, index) -> Map.of("json", rs.getString(1), "sha", rs.getString(2)));
        if (rows.size() != 1) throw invalid("路线版本 " + versionId + " 缺少设备资料快照，请完成版本设备资料迁移");
        var row = rows.get(0);
        if (!Objects.equals(row.get("sha"), DigestUtil.sha256Hex(row.get("json"))))
            throw invalid("路线版本设备资料快照校验失败");
        JSONObject json = JSON.parseObject(row.get("json"));
        Map<Long, Identity> result = new LinkedHashMap<>();
        for (Long id : ids) {
            JSONObject item = json.getJSONObject(id.toString());
            Identity identity = item == null ? null : new Identity(item.getLong("deviceId"),
                    item.getString("deviceCode"), item.getString("deviceName"));
            validate(identity, versionId);
            if (!Objects.equals(id, identity.deviceId())) throw invalid("路线版本设备身份不一致");
            result.put(id, identity);
        }
        return result;
    }

    private static void validate(Identity identity, Long versionId) {
        if (identity == null || identity.deviceId() == null || StrUtil.isBlank(identity.deviceCode())
                || StrUtil.isBlank(identity.deviceName()))
            throw invalid("路线版本 " + versionId + " 的设备身份资料不完整");
    }

    private static Map<String, Object> keys(Long versionId) {
        return Map.of("tenant", TenantContextHolder.getRequiredTenantId(), "version", versionId);
    }

    public static Set<Long> deviceIds(MesProRouteVersionDO version) {
        Set<Long> ids = new TreeSet<>();
        JSONObject root = JSON.parseObject(version.getRouteSnapshotJson());
        JSONObject configs = root == null ? null : root.getJSONObject("configSnapshots");
        var processes = configs == null ? null : configs.getJSONArray("productionProcessConfigs");
        if (processes == null) return ids;
        for (int i = 0; i < processes.size(); i++) {
            var groups = processes.getJSONObject(i).getJSONArray("deviceSelectionGroups");
            if (groups == null) throw invalid("路线版本缺少设备选择配置");
            for (int j = 0; j < groups.size(); j++) {
                var deviceIds = groups.getJSONObject(j).getJSONArray("deviceIds");
                if (deviceIds == null) throw invalid("路线版本缺少设备选择配置");
                for (Long id : deviceIds.toJavaList(Long.class)) {
                    if (id == null || id <= 0) throw invalid("路线版本设备身份无效");
                    ids.add(id);
                }
            }
        }
        return ids;
    }
}
