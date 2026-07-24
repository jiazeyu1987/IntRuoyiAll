# 执行日志：修复展厅菜单标题占位

INFO: skill -> 使用 `bug-regression-fix-loop` 与 `clear-frontend-copy`，并读取 bug 证据契约和前端文案标准。

INFO: experience-index -> matched `docs/login-access.md`, `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。

BDD: 展厅菜单不显示英文占位 -> Given 用户登录后台并展开展厅菜单 / When 菜单树渲染完成 / Then 任一可见菜单标题都不应显示 Please set title。

BDD: 缺失标题的展厅入口有中文业务名 -> Given 动态菜单包含展厅子路由 / When 前端合并动态路由与静态路由 / Then 该路由 meta.title 使用明确的简体中文业务标题。

GREEN: experience-preflight -> PASS，已确认本机前端 `http://localhost:8081/login?redirect=/index` 返回 200，后端 `http://127.0.0.1:48081/actuator/health` 返回 UP，`npx` 可用；本次真实 E2E 仅登录并读取菜单，不写入业务数据。

RED: node tests/e2e/showroom-menu-title-placeholder-static.spec.js -> FAIL，命中 `top-level single-page Layout shell must not drop the backend menu title`，确认前端生成单页父壳路由时丢弃了标题/图标。

GREEN: node tests/e2e/showroom-menu-title-placeholder-static.spec.js -> PASS，单页 Layout 父壳保留 `title/icon`，菜单标题渲染器已移除英文占位并对缺失标题 fail fast。

GREEN: copy-scan -> PASS，`rg` 与 `clear-frontend-copy` 扫描确认 `Please set title` 不再存在于用户可见源码；仅回归测试中保留禁止匹配断言。

GREEN: npm run ts:check -> PASS，`vue-tsc --noEmit -p tsconfig.relaxed.json` 通过。

BLOCKER-RESOLVED: local-backend-restart -> 本机 48081 后端一度不可用；`restart-int-ruoyi-local.ps1` 复制普通 `yudao-server.jar` 导致 Java 报 `没有主清单属性`。已通过后端任务 `20260624-restart-int-ruoyi-local-exec-jar` 修复为启动 `yudao-server-exec.jar`，后端健康检查恢复为 `{"status":"UP"}`。

GREEN: real-e2e -> PASS，Playwright 真实登录 `http://localhost:8081`，测试租户 `测试租户/aoteman/111111`，登录和权限接口均返回 HTTP 200 且 `tenant-id=122`；展开展厅菜单后 `Please set title` 数量为 0，菜单切片为 `展厅 / 公司信息 / 公司版本 / 产品管理 / 提示管理 / 展柜管理 / 讲解工作台 / 电子签名 / 供应商关系管理`。
