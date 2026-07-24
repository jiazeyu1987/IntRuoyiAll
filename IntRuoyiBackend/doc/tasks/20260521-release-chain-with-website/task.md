# 任务：三条发布链同时发布 Website 并回显访问路径

## Goal

- 在以下三条发布链执行时，同时发布 `D:\ProjectPackage\Website`：
  - 当前本地代码发布到测试服务器 `172.30.30.58`
  - 当前测试服务器版本提升到正式服务器 `172.30.30.57`
  - 当前本地代码直接发布到正式服务器 `172.30.30.57`
- 发布成功后，在操作 UI / 脚本输出中同时显示：
  - `IntRuoyi` 的访问路径
  - `Website` 前端的访问路径

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-test.ps1`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-test.bat`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-prod.bat`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-direct-to-prod.bat`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\promote-int-ruoyi-test-to-prod.ps1`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\int-ruoyi-test\docker-compose.yml`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\int-ruoyi-test\website.nginx.conf`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\OPS.md`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\README.md`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-release-chain-with-website\**`

## Non-Scope

- 不修改 `D:\ProjectPackage\Website` 的业务页面代码。
- 不执行真实测试发布、真实正式发布或真实测试提升正式。
- 不占用当前已被 `onlyoffice` 使用的 `8082` 端口。
- 不修改与发布链无关的 showroom / backup-ops / 业务后端逻辑。

## Previous Task Check

- Previous same-repo task record:
  `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-backup-ops-linux-runtime-rehearsal\task.md`
- Status before this task: `Blocked on 2026-05-21`
- Recorded blocker: 用户已切换到更高优先级发布链需求。
- Impact on this task: 上一任务已显式暂停，当前可以独立推进发布链和运维入口调整。

## Milestones

- [x] M1：检查上一同仓任务状态并创建本任务文档。
- [x] M2：记录 BDD 场景并补 RED 测试，覆盖 Website 一起发布与 URL 回显。
- [x] M3：实现测试发布链同时发布 Website。
- [x] M4：实现测试提升正式与直发正式同时发布 Website。
- [x] M5：验证脚本输出、更新文档并执行 cleanup 预览。

## Expected Verification

- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-direct-to-prod.bat cancel`
- `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-prod.bat cancel`
- `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\运维工具.bat help`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-release-chain-with-website --mode preview`

## Current Status

Completed on 2026-05-21.

## Current Findings

- `Website` 仓库是独立的 Vite 静态前端，根入口为 `/`，展厅入口为 `/showroom`。
- `Website` 当前没有现成并入 `IntRuoyi` 统一发布链的远端发布脚本或固定部署目录。
- 测试服务器 `172.30.30.58` 与正式服务器 `172.30.30.57` 的 `8082` 端口已被 `onlyoffice` 占用。
- 两台服务器当前 `8083` 未被占用，可作为 `Website` 前端独立发布端口候选。
- 当前 `publish-int-ruoyi-to-test.ps1` 只输出 `IntRuoyi` 前端和后端健康地址；尚未输出 `Website` 访问路径。
- 已将 `Website` 纳入三条发布链，统一以独立静态站点容器形式挂到 `8083`。
- `Website` 的远端入口统一为：
  - 根入口：`http://<server>:8083/`
  - 展厅入口：`http://<server>:8083/showroom`

## Final Verification Result

- PASS：`python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- PASS：PowerShell parser check for `script\deploy\publish-int-ruoyi-to-test.ps1` and `script\deploy\promote-int-ruoyi-test-to-prod.ps1`
- PASS：`cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-direct-to-prod.bat cancel`
- PASS：`cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-prod.bat cancel`
- PASS：`cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\运维工具.bat help`
- PASS：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-release-chain-with-website --mode preview`

## Cleanup Keep

- `doc/tasks/20260521-release-chain-with-website/task.md`
- `doc/tasks/20260521-release-chain-with-website/execution-log.md`
