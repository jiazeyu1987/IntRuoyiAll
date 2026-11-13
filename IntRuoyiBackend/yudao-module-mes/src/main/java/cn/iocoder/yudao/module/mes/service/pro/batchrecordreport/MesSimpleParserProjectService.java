package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesRouteDccProjectBindingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** 解析页面的项目选择不受 DCC 项目成员权限限制，仍保留租户隔离。 */
@Service
@RequiredArgsConstructor
public class MesSimpleParserProjectService {
    private final DccProjectCodeMapper projectMapper;
    private final MesRouteDccProjectBindingMapper bindingMapper;

    public record ProjectOption(Long id, String projectCode, String projectName, String status,
                                boolean routeOccupied) {}

    public List<ProjectOption> options(String keyword) {
        String search = StrUtil.trimToEmpty(keyword).toLowerCase(Locale.ROOT);
        var occupied = bindingMapper.selectList().stream()
                .map(MesRouteDccProjectBindingDO::getDccProjectCodeId).collect(Collectors.toSet());
        return projectMapper.selectEnabledList().stream()
                .filter(project -> (project.getProjectCode() + " / " + project.getProjectName())
                        .toLowerCase(Locale.ROOT).contains(search))
                .map(project -> new ProjectOption(project.getId(), project.getProjectCode(),
                        project.getProjectName(), project.getStatus(), occupied.contains(project.getId())))
                .toList();
    }
}
