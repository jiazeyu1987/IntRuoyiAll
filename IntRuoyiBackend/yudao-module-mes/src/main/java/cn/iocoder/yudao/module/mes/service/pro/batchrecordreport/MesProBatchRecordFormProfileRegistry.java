package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class MesProBatchRecordFormProfileRegistry {

    private final List<MesProBatchRecordFormProfile> profiles;

    @Autowired
    public MesProBatchRecordFormProfileRegistry(List<MesProBatchRecordFormProfile> profiles) {
        this.profiles = profiles == null ? List.of() : profiles.stream()
                .sorted(Comparator.comparingInt(MesProBatchRecordFormProfile::priority))
                .toList();
    }

    public Optional<MesProBatchRecordFormProfile> findSourceProfile(
            List<MesProBatchRecordParsedTable> sourceTables) {
        return profiles.stream()
                .filter(profile -> profile.supportsSourceTables(sourceTables))
                .findFirst();
    }

    public List<MesProBatchRecordParsedTable> normalizeSourceTables(String formSlotType,
                                                                    List<MesProBatchRecordParsedTable> sourceTables) {
        MesProBatchRecordFormProfile profile = findByFormSlotType(formSlotType).orElse(null);
        if (profile == null || !profile.supportsSourceTables(sourceTables)) {
            return sourceTables == null ? List.of() : sourceTables;
        }
        return profile.normalizeSourceTables(sourceTables);
    }

    public Optional<MesProBatchRecordFormProfile> findLegacyLayoutProfile(MesProBatchRecordReportDO metadata,
                                                                         JSONObject root) {
        return profiles.stream()
                .filter(profile -> profile.supportsLegacyLayout(metadata, root))
                .findFirst();
    }

    private Optional<MesProBatchRecordFormProfile> findByFormSlotType(String formSlotType) {
        String normalized = MesProBatchRecordFormSlotType.normalize(formSlotType);
        if (StrUtil.isBlank(normalized)) {
            return Optional.empty();
        }
        return profiles.stream()
                .filter(profile -> normalized.equals(profile.formSlotType()))
                .findFirst();
    }
}
