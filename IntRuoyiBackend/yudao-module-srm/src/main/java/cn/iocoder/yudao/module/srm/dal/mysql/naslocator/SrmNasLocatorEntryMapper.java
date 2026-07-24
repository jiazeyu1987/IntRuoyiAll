package cn.iocoder.yudao.module.srm.dal.mysql.naslocator;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.srm.controller.admin.naslocator.vo.SrmNasLocatorPageReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.naslocator.SrmNasLocatorEntryDO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SrmNasLocatorEntryMapper extends BaseMapperX<SrmNasLocatorEntryDO> {

    default PageResult<SrmNasLocatorEntryDO> selectFilePage(Long successTaskId, SrmNasLocatorPageReqVO reqVO) {
        String keyword = StrUtil.trimToNull(reqVO.getKeyword());
        boolean wildcardMode = isWildcardKeyword(keyword);
        String likeKeyword = wildcardMode ? buildLikeKeyword(keyword) : null;
        long total = selectFileCount(successTaskId, keyword, wildcardMode, likeKeyword);
        if (total <= 0) {
            return new PageResult<>(List.<SrmNasLocatorEntryDO>of(), 0L);
        }
        long offset = (long) (reqVO.getPageNo() - 1) * reqVO.getPageSize();
        List<SrmNasLocatorEntryDO> records = selectFilePageList(successTaskId, keyword, wildcardMode, likeKeyword,
                offset, reqVO.getPageSize());
        return new PageResult<>(records, total);
    }

    long selectFileCount(@Param("successTaskId") Long successTaskId,
                         @Param("keyword") String keyword,
                         @Param("wildcardMode") boolean wildcardMode,
                         @Param("likeKeyword") String likeKeyword);

    List<SrmNasLocatorEntryDO> selectFilePageList(@Param("successTaskId") Long successTaskId,
                                                  @Param("keyword") String keyword,
                                                  @Param("wildcardMode") boolean wildcardMode,
                                                  @Param("likeKeyword") String likeKeyword,
                                                  @Param("offset") Long offset,
                                                  @Param("pageSize") Integer pageSize);

    default List<SrmNasLocatorEntryDO> selectListByRefreshTaskId(Long refreshTaskId) {
        return selectList(SrmNasLocatorEntryDO::getRefreshTaskId, refreshTaskId);
    }

    default int deleteByRefreshTaskId(Long refreshTaskId) {
        return delete(Wrappers.<SrmNasLocatorEntryDO>lambdaQuery()
                .eq(SrmNasLocatorEntryDO::getRefreshTaskId, refreshTaskId));
    }

    private static boolean isWildcardKeyword(String keyword) {
        return StrUtil.isNotBlank(keyword) && keyword.contains("*");
    }

    private static String buildLikeKeyword(String keyword) {
        StringBuilder builder = new StringBuilder(keyword.length());
        for (int i = 0; i < keyword.length(); i++) {
            char current = keyword.charAt(i);
            if (current == '*') {
                builder.append('%');
                continue;
            }
            if (current == '\\') {
                builder.append("\\\\");
                continue;
            }
            if (current == '%' || current == '_') {
                builder.append('\\');
            }
            builder.append(current);
        }
        return builder.toString();
    }
}
