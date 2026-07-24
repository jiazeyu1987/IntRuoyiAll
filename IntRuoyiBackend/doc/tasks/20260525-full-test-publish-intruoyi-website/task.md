# 任务：全量发布 IntRuoyi 与 Website 到测试服务器

## 任务目标

- 将 IntRuoyi 后端发布到测试服务器 `172.30.30.58`。
- 将 IntRuoyi 管理前端发布到测试服务器 `172.30.30.58:8081`。
- 将本机 MySQL `ruoyi-vue-pro` 数据库全量同步到测试服务器。
- 将本机 MinIO `yudao` 桶中的展厅相关文件全量同步到测试服务器。
- 将 `D:\ProjectPackage\Website` 构建产物发布到测试服务器 `172.30.30.58:8083`。

## 非目标

- 不发布正式服务器。
- 不使用 `skip-db`、`skip-minio` 或 `skip-data` 降级模式。
- 不修改业务代码、数据库结构或租户数据。
- 不绕过健康检查或用 mock 数据代替真实测试环境。

## 前置任务检查

- 最近同仓任务：`20260525-test-showroom-company-revision-schema-hotfix`。
- 上一任务状态：`blocked`。
- 影响：上一任务为测试服 schema 热修任务，本次用户明确要求全量发布并同步 MySQL；本次发布会按默认发布脚本替换测试服数据库，不混入旧任务的手工 schema 热修。

## 里程碑

- [x] M1：读取服务器与登录说明，建立任务记录并确认上一任务状态。
- [x] M2：执行测试服发布前状态检查与本地前置依赖检查。
- [x] M3：执行默认全量发布，包含后端、管理前端、Website、MySQL、MinIO。
- [x] M4：验证测试服后端、管理前端、Website 与展厅入口 HTTP 可用。
- [x] M5：记录发布证据、执行 closeout 预览，并按策略提交本任务文档。

## BDD 场景

- BDD: 全量发布测试服 -> Given 本机后端、管理前端、Website、MySQL 和 MinIO 均可访问, When 执行默认测试服发布脚本, Then 测试服后端健康检查、管理前端首页、Website 根路径与展厅路径均返回成功。
- BDD: 数据同步不降级 -> Given 用户要求同步所有展厅数据和 MySQL, When 执行发布, Then 不传入 `skip-db`、`skip-minio` 或 `skip-data` 参数，发布脚本执行数据库 dump/import 与 MinIO mirror。

## 预期验证

- 发布前：`script\deploy\show-int-ruoyi-test-status.bat`
- 发布：`D:\ProjectPackage\Int\IntRuoyi\publish-int-ruoyi-to-test.bat default`
- 发布后：
  - `http://172.30.30.58:48081/actuator/health`
  - `http://172.30.30.58:8081/`
  - `http://172.30.30.58:8083/`
  - `http://172.30.30.58:8083/showroom`
- 发布后：`script\deploy\show-int-ruoyi-test-status.bat`
- `python -X utf8 C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence doc/tasks/20260525-full-test-publish-intruoyi-website/ci-cd-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260525-full-test-publish-intruoyi-website --mode preview`

## Current Status

completed

## 当前状态

- 状态：completed
- 已完成：
  - 已读取 `docs/server-access.md` 与 `docs/login-access.md`。
  - 已确认默认发布脚本会构建后端、管理前端、Website，并同步 MySQL 与 MinIO。
  - 已确认上一同仓任务处于 blocked，不共享本次发布文档范围。
  - 已排查测试服登录页 `48081 ERR_CONNECTION_REFUSED`：当前后端端口、健康检查、租户解析与测试租户 API 登录均已恢复；故障与后端容器 2026-05-25 12:40:43 左右重启/未监听窗口相符。
  - 已排查手动发布展厅 `Java heap space`：测试服磁盘空间充足，根因是后端 JVM 堆上限仅 `512m` 且发布链路会一次性物化多份图片/音频资产；已将测试服运行时与发布脚本默认堆调整为 `-Xms1g -Xmx2g`。
  - 已排查产品管理/公司信息图片和语音加载失败：后端文件接口与 MinIO 对象正常，根因是管理前端 Nginx 对 `/admin-api/infra/file/...` 返回 SPA `index.html`，未代理到后端；已修复 Nginx 模板并热更新测试服前端容器。
  - 已修复 Website Nginx 模板，使 `/showroom/release/` 与 `/showroom/assets/` 代理到后端，不再落到 SPA `index.html`。
  - 已修复测试服发布脚本中本地 Vite 构建调用方式，避免 `Start-Process` 捕获 `pnpm` 时触发前端构建 OOM/exit code `-1`。
  - 已执行默认发布 `D:\ProjectPackage\Int\IntRuoyi\publish-int-ruoyi-to-test.bat default`，发布 tag `20260525_135729`，未使用 `skip-db`、`skip-minio` 或 `skip-data`。
  - 发布后后端健康检查、管理前端、Website 根路径、Website `/showroom`、Website release current 均返回 HTTP `200`。
  - 已在修复后的后端上通过管理前端真实路径重新发布展厅 release `20260525T061337Z-e03a7b68bf1a`，manifest `506` 个资产 HEAD 全量检查 failures `[]`。
- 阻塞与影响：
  - 暂无阻塞。

## 最终验证结果

- PASS: `script\deploy\show-int-ruoyi-test-status.bat` -> Runtime directory present, Backend health HTTP 200, Frontend status HTTP 200。
- PASS: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> 24 passed。
- PASS: `D:\ProjectPackage\Int\IntRuoyi\publish-int-ruoyi-to-test.bat default` -> tag `20260525_135729`，默认全量发布成功。
- PASS: `http://172.30.30.58:48081/actuator/health` -> HTTP 200。
- PASS: `http://172.30.30.58:8081/` -> HTTP 200。
- PASS: `http://172.30.30.58:8083/` -> HTTP 200。
- PASS: `http://172.30.30.58:8083/showroom` -> HTTP 200。
- PASS: `http://172.30.30.58:8083/showroom/release/current` -> release `20260525T061337Z-e03a7b68bf1a`。

## Cleanup Keep

- `doc/tasks/20260525-full-test-publish-intruoyi-website/ci-cd-evidence.md`
