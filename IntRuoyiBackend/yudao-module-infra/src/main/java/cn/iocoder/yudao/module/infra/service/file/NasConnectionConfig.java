package cn.iocoder.yudao.module.infra.service.file;

import cn.hutool.core.util.StrUtil;

/**
 * NAS 连接配置
 */
public record NasConnectionConfig(
        String server,
        Integer port,
        String share,
        String domain,
        String username,
        String password
) {

    public NasConnectionConfig {
        server = StrUtil.trimToEmpty(server);
        port = port == null || port <= 0 ? 445 : port;
        share = StrUtil.trimToEmpty(share);
        domain = StrUtil.trimToEmpty(domain);
        username = StrUtil.trimToEmpty(username);
        password = StrUtil.trimToEmpty(password);
    }

    public String rootUnc() {
        return "\\\\" + server + "\\" + share;
    }
}
