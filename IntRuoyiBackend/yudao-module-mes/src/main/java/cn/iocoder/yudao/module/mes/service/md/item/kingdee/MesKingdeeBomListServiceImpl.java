package cn.iocoder.yudao.module.mes.service.md.item.kingdee;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeBomClient;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeBomLine;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import cn.iocoder.yudao.module.mes.controller.admin.md.item.vo.kingdee.MesKingdeeBomListPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.item.vo.kingdee.MesKingdeeBomListRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesKingdeeBomListDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesKingdeeBomListMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;

@Service
@Validated
public class MesKingdeeBomListServiceImpl implements MesKingdeeBomListService {

    @Resource
    private ErpKingdeeBomClient bomClient;
    @Resource
    private ErpKingdeeConfigService kingdeeConfigService;
    @Resource
    private MesKingdeeBomListMapper bomListMapper;

    @Override
    public PageResult<MesKingdeeBomListRespVO> getPage(MesKingdeeBomListPageReqVO pageReqVO) {
        return BeanUtils.toBean(bomListMapper.selectPage(pageReqVO), MesKingdeeBomListRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncAll() {
        ErpKingdeeProperties properties = kingdeeConfigService.getEffectiveProperties();
        List<ErpKingdeeBomLine> lines = bomClient.fetchBomLines(properties);
        return syncLines(lines);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesKingdeeBomListSyncResult syncAllSkipExisting() {
        return syncAllSkipExisting(null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesKingdeeBomListSyncResult syncAllSkipExisting(LocalDateTime windowStart, LocalDateTime windowEnd) {
        ErpKingdeeProperties properties = kingdeeConfigService.getEffectiveProperties();
        properties.validateBaseConfig();
        List<ErpKingdeeBomLine> lines = (windowStart == null ? bomClient.fetchBomLines(properties)
                : bomClient.fetchBomLinesModifiedBetween(properties, windowStart, windowEnd));
        MesKingdeeBomListSyncResult result = new MesKingdeeBomListSyncResult();
        LocalDateTime now = LocalDateTime.now();
        Set<String> existingFids = new HashSet<>();
        for (String fid : validateSnapshot(lines)) {
            if (bomListMapper.countBySourceFid(fid) > 0) { existingFids.add(fid); }
        }
        for (int i = 0; i < lines.size(); i++) {
            if (!existingFids.contains(lines.get(i).getFid())) {
                insertRow(lines.get(i), now);
                result.addCreated();
            } else {
                result.addSkipped();
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncModifiedBetween(LocalDateTime windowStart, LocalDateTime windowEnd) {
        if (bomListMapper.selectCount() == 0) {
            return syncAll();
        }
        ErpKingdeeProperties properties = kingdeeConfigService.getEffectiveProperties();
        List<ErpKingdeeBomLine> lines = bomClient.fetchBomLinesModifiedBetween(properties, windowStart, windowEnd);
        return syncLines(lines);
    }

    private Set<String> validateSnapshot(List<ErpKingdeeBomLine> lines) {
        Set<String> keys = new HashSet<>();
        Set<String> fids = new LinkedHashSet<>();
        for (ErpKingdeeBomLine line : lines) {
            if (line.getFid() == null || line.getFid().isBlank() || line.getEntryId() == null
                    || line.getEntryId().isBlank() || !keys.add(line.getFid() + "|" + line.getEntryId())) {
                throw new IllegalStateException("BOM snapshot has missing or duplicate ERP entry identity");
            }
            fids.add(line.getFid());
        }
        return fids;
    }

    private int syncLines(List<ErpKingdeeBomLine> lines) {
        LocalDateTime now = LocalDateTime.now();
        Set<String> fids = validateSnapshot(lines);
        // 仅替换本次完整读取的 BOM；一并清除旧用量组成的历史键和 ERP 已删除分录。
        fids.forEach(bomListMapper::deleteBySourceFid);
        for (int i = 0; i < lines.size(); i++) {
            insertRow(lines.get(i), now);
        }
        return lines.size();
    }

    private void insertRow(ErpKingdeeBomLine line, LocalDateTime now) {
        String lineKey = line.getEntryId();
        MesKingdeeBomListDO row = MesKingdeeBomListDO.builder()
                .sourceFormId(ErpKingdeeBomLine.FORM_ID)
                .sourceFid(line.getFid())
                .sourceLineKey(lineKey)
                .bomNumber(line.getBomVersion())
                .bomType(line.getBomType())
                .documentStatus("已审核")
                .parentMaterialCode(line.getParentMaterialNumber())
                .parentMaterialName(line.getParentMaterialName())
                .parentMaterialSpecification(line.getParentMaterialSpecification())
                .parentQuantity(line.getParentQuantity())
                .lineNo(line.getLineNo())
                .childMaterialCode(line.getChildMaterialNumber())
                .childMaterialName(line.getChildMaterialName())
                .childMaterialSpecification(line.getChildMaterialSpecification())
                .childUnitName(line.getChildUnitName())
                .numerator(line.getNumerator())
                .denominator(line.getDenominator())
                .sourceModifyTime(line.getSourceModifyTime())
                .lastSyncTime(now)
                .rawPayload(JsonUtils.toJsonString(line))
                .build();
        bomListMapper.insert(row);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncByCode(String code) {
        ErpKingdeeProperties properties = kingdeeConfigService.getEffectiveProperties();
        return syncLines(bomClient.fetchApprovedBomByParentMaterialNumber(properties, code));
    }

}
