# 执行日志：定位展厅前台 live 预览图资产阻塞

BDD: 前台首页图片墙应显示已发布预览图 -> Given 数字展厅前台首页已从真实入口进入并读取 live hallEntries / When 检查 preview 资产链路 / Then 若 previewImageUrl 为空，必须明确指出是 preview 资产未发布还是图片文件根本不存在，而不是假装前台已准备就绪。

GREEN: `jshell --class-path mysql-connector-j ...` -> PASS，已连到真实运行库 `127.0.0.1:23306/ruoyi-vue-pro`

GREEN: `select count(*) from showroom_preview_asset_version where deleted = b'0'` -> PASS，结果为 `0`

GREEN: `select count(*) from infra_file where deleted = 0 and type like 'image/%'` -> PASS，结果为 `0`

FINDING: `/showroom/display/home` 的 `hallEntries.previewImageUrl` 为空，根因不是前台路由或 DTO 契约，而是 live preview 资产与图片文件都不存在。
