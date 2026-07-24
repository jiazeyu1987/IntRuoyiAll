# 任务：发布测试服前端运行控制台按钮

## 任务目标

- 将已合并到前端 `int_main` 的运行控制台运维按钮发布到测试服务器。
- 仅替换测试服 `intruoyi-frontend` 镜像，不发布后端、不同步数据库、不同步 MinIO、不修改 Website。
- 发布后使用 `芋道源码/admin` 真实登录验证按钮可见。

## 非目标

- 不执行发布测试服、提升正式服、立即备份、回滚版本、恢复数据等高风险运维动作。
- 不运行后端整套发布脚本，避免带上后端仓库当前未提交的展厅改动。
- 不修改数据库权限；`admin` 的“运维工程师”角色已在后端任务中完成。

## BDD 场景

- BDD: 测试服前端显示运维按钮 -> Given 测试服前端部署最新 `int_main` 产物且 `admin` 拥有运行控制台运维权限, When `芋道源码/admin` 打开运行控制台, Then 页面显示 `发布测试服`、`提升正式服`、`立即备份`、`回滚版本`、`恢复数据`。
- BDD: 前端单独发布不影响后端数据 -> Given 后端仓库存在无关未提交改动, When 修复测试服按钮可见性, Then 只重建并替换 `intruoyi-frontend` 镜像，不重启后端、不修改 `.env` 的 `IMAGE_TAG`。

## 预期验证

- RED：真实页面当前只显示 `刷新/重启`，看不到五个运维按钮。
- GREEN：`node node_modules\vite\bin\vite.js build --mode test` 通过并生成 `dist-intruoyi-test`。
- GREEN：Docker 前端镜像构建、传输、加载并重启测试服 frontend 服务成功。
- GREEN：Playwright 真实登录 `芋道源码/admin` 后能看到五个运维按钮。

## 当前状态

- 状态：completed
- 已完成：
  - 已建立任务文档和 BDD 场景。
  - 已确认本地前端 `int_main` 代码包含运维按钮，测试服当前前端镜像仍不可见。
  - 已完成 `pnpm ts:check`。
  - 已使用测试服后端地址构建 `dist-intruoyi-test`。
  - 已重建并加载 `intruoyi-frontend:20260525_135729` 镜像，仅重启测试服 frontend 容器。
  - 已用 `芋道源码/admin` 真实登录验证五个运维按钮可见。
- 阻塞与影响：
  - 暂无。
