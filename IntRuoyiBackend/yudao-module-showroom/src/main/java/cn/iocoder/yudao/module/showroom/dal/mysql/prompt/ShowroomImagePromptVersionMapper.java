package cn.iocoder.yudao.module.showroom.dal.mysql.prompt;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.showroom.dal.dataobject.prompt.ShowroomImagePromptVersionDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ShowroomImagePromptVersionMapper extends BaseMapperX<ShowroomImagePromptVersionDO> {

    default ShowroomImagePromptVersionDO selectLatestBySceneCode(String sceneCode) {
        return selectOne(new LambdaQueryWrapperX<ShowroomImagePromptVersionDO>()
                .eq(ShowroomImagePromptVersionDO::getSceneCode, sceneCode)
                .orderByDesc(ShowroomImagePromptVersionDO::getVersionNo)
                .last("LIMIT 1"));
    }

    default List<ShowroomImagePromptVersionDO> selectListBySceneCode(String sceneCode) {
        return selectList(new LambdaQueryWrapperX<ShowroomImagePromptVersionDO>()
                .eq(ShowroomImagePromptVersionDO::getSceneCode, sceneCode)
                .orderByDesc(ShowroomImagePromptVersionDO::getVersionNo));
    }

    default int incrementUsage(Long id, LocalDateTime lastUsedAt) {
        return update(null, new LambdaUpdateWrapper<ShowroomImagePromptVersionDO>()
                .eq(ShowroomImagePromptVersionDO::getId, id)
                .setSql("use_count = COALESCE(use_count, 0) + 1")
                .set(ShowroomImagePromptVersionDO::getLastUsedAt, lastUsedAt));
    }
}
