package cn.iocoder.yudao.module.showroom.keyword.service;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.keyword.KeywordPageReqVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.keyword.KeywordPageRespVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.keyword.KeywordRespVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.keyword.KeywordSaveReqVO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.keyword.ShowroomKeywordDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.keyword.ShowroomKeywordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ShowroomKeywordService {

    private final ShowroomKeywordMapper keywordMapper;

    public ShowroomKeywordService(ShowroomKeywordMapper keywordMapper) {
        this.keywordMapper = keywordMapper;
    }

    public PageResult<KeywordPageRespVO> getPage(KeywordPageReqVO reqVO) {
        PageParam pageParam = new PageParam();
        pageParam.setPageNo(reqVO.getPageNo());
        pageParam.setPageSize(reqVO.getPageSize());
        PageResult<ShowroomKeywordDO> page = keywordMapper.selectPageByKeyword(pageParam, reqVO.getKeyword());
        List<KeywordPageRespVO> rows = page.getList().stream()
                .map(keyword -> new KeywordPageRespVO(
                        keyword.getId(),
                        keyword.getNameZh(),
                        keyword.getNameEn(),
                        keyword.getUpdateTime()))
                .toList();
        return new PageResult<>(rows, page.getTotal());
    }

    public KeywordRespVO get(Long id) {
        ShowroomKeywordDO keyword = requireOwnedKeyword(id);
        return new KeywordRespVO(keyword.getId(), keyword.getNameZh(), keyword.getNameEn(),
                keyword.getCreateTime(), keyword.getUpdateTime());
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(KeywordSaveReqVO reqVO) {
        String nameZh = normalizeRequired(reqVO.nameZh(), "SHOWROOM_KEYWORD_NAME_ZH_REQUIRED: 中文关键词不能为空");
        String nameEn = normalizeRequired(reqVO.nameEn(), "SHOWROOM_KEYWORD_NAME_EN_REQUIRED: 英文关键词不能为空");
        ensureUniqueNameZh(nameZh, null);
        ShowroomKeywordDO keyword = new ShowroomKeywordDO();
        keyword.setTenantId(TenantContextHolder.getRequiredTenantId());
        keyword.setNameZh(nameZh);
        keyword.setNameEn(nameEn);
        keywordMapper.insert(keyword);
        return keyword.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(KeywordSaveReqVO reqVO) {
        if (reqVO.id() == null) {
            throw new IllegalStateException("SHOWROOM_KEYWORD_ID_REQUIRED: keyword id is required");
        }
        ShowroomKeywordDO keyword = requireOwnedKeyword(reqVO.id());
        String nameZh = normalizeRequired(reqVO.nameZh(), "SHOWROOM_KEYWORD_NAME_ZH_REQUIRED: 中文关键词不能为空");
        String nameEn = normalizeRequired(reqVO.nameEn(), "SHOWROOM_KEYWORD_NAME_EN_REQUIRED: 英文关键词不能为空");
        ensureUniqueNameZh(nameZh, keyword.getId());
        keyword.setNameZh(nameZh);
        keyword.setNameEn(nameEn);
        keywordMapper.updateById(keyword);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ShowroomKeywordDO keyword = requireOwnedKeyword(id);
        keywordMapper.deleteById(keyword.getId());
    }

    private void ensureUniqueNameZh(String nameZh, Long currentId) {
        ShowroomKeywordDO existing = keywordMapper.selectByNameZh(nameZh);
        if (existing != null && (currentId == null || !currentId.equals(existing.getId()))) {
            throw new IllegalStateException("SHOWROOM_KEYWORD_DUPLICATE_ZH: 中文关键词已存在");
        }
    }

    private ShowroomKeywordDO requireOwnedKeyword(Long id) {
        if (id == null) {
            throw new IllegalStateException("SHOWROOM_KEYWORD_ID_REQUIRED: keyword id is required");
        }
        ShowroomKeywordDO keyword = keywordMapper.selectById(id);
        if (keyword == null) {
            throw new IllegalStateException("SHOWROOM_KEYWORD_NOT_FOUND: keyword not found");
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (!tenantId.equals(keyword.getTenantId())) {
            throw new IllegalStateException("SHOWROOM_KEYWORD_NOT_FOUND: keyword not found");
        }
        return keyword;
    }

    private static String normalizeRequired(String value, String errorMessage) {
        if (value == null) {
            throw new IllegalStateException(errorMessage);
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalStateException(errorMessage);
        }
        return normalized;
    }

}
