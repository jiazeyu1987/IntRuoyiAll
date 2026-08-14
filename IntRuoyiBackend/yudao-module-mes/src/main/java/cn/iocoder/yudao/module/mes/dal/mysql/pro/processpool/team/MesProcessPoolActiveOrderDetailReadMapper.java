package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MesProcessPoolActiveOrderDetailReadMapper {

    List<MesTeamLeaderActiveOrderDetailReadDO> selectByActiveOrderId(
            @Param("activeOrderId") Long activeOrderId);
}
