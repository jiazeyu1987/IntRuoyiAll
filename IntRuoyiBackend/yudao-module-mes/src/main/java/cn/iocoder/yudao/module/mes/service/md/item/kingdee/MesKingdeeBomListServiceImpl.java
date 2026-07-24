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
    public int syncModifiedBetween(LocalDateTime windowStart, LocalDateTime windowEnd) {
        if (bomListMapper.selectCount() == 0) {
            return syncAll();
        }
        ErpKingdeeProperties properties = kingdeeConfigService.getEffectiveProperties();
        List<ErpKingdeeBomLine> lines = bomClient.fetchBomLinesModifiedBetween(properties, windowStart, windowEnd);
        return syncLines(lines);
    }

    private int syncLines(List<ErpKingdeeBomLine> lines) {
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < lines.size(); i++) {
            upsert(lines.get(i), i + 1, now);
        }
        return lines.size();
    }

    private void upsert(ErpKingdeeBomLine line, int lineNo, LocalDateTime now) {
        String lineKey = line.getChildMaterialNumber() + "|" + line.getNumerator() + "|" + line.getDenominator();
        MesKingdeeBomListDO existing = bomListMapper.selectBySourceLine(line.getFid(), lineKey);
        MesKingdeeBomListDO row = MesKingdeeBomListDO.builder()
                .sourceFormId(ErpKingdeeBomLine.FORM_ID)
                .sourceFid(line.getFid())
                .sourceLineKey(lineKey)
                .bomNumber(line.getBomVersion())
                .bomType("标准BOM")
                .documentStatus("已审核")
                .parentMaterialCode(line.getParentMaterialNumber())
                .parentMaterialName(line.getParentMaterialName())
                .parentMaterialSpecification(line.getParentMaterialSpecification())
                .parentQuantity(null)
                .lineNo(lineNo)
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
        if (existing == null) {
            bomListMapper.insert(row);
            return;
        }
        row.setId(existing.getId());
        bomListMapper.updateById(row);
    }

}
