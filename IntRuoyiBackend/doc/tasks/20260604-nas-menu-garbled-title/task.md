# 任务：修复 NAS 菜单标题乱码

## 任务目标

修复本机后台左侧菜单中 `NAS??` / `NAS????` 乱码标题。标准菜单标题必须来自后端菜单数据，显示为 `NAS 管理`、`NAS 配置查询`、`NAS 配置保存`、`NAS 连接测试`；本机重启脚本必须能在运行库菜单标题漂移时重新应用 NAS 菜单 SQL。不得在前端增加显示兜底或吞掉数据错误。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260604-runtime-control-recovery-set-contract/task.md`
- 状态：`completed`
- 处理：上一任务已完成；本任务只修改 NAS 菜单 SQL、重启脚本迁移探针、对应测试和本机运行库菜单记录。

## BDD 场景

- BDD: NAS 菜单标题可读 -> Given 后端 `system_menu` 已存在 NAS 菜单 / When 前端加载动态菜单 / Then 页面菜单标题显示 `NAS 管理`，不得显示 `NAS??`。
- BDD: NAS 权限菜单标题可读 -> Given 后端存在 `infra:nas:*` 权限菜单 / When 管理员查看菜单树或角色权限 / Then 权限菜单显示 `NAS 配置查询`、`NAS 配置保存`、`NAS 连接测试`。
- BDD: 本机重启修复菜单标题漂移 -> Given 本机运行库 NAS 菜单标题被写成问号 / When 执行本机重启脚本的必需 MySQL 迁移检查 / Then 脚本探针失败并重跑 NAS 菜单 SQL，恢复标准标题。

## Milestones

- [x] M1：建立任务文档并确认上一后端任务已完成。
- [x] M2：新增 RED 回归测试，锁定菜单标题和本机迁移探针。
- [x] M3：修复 NAS 菜单 SQL 文案与本机重启迁移列表。
- [x] M4：修复本机运行库菜单记录并验证真实数据。
- [x] M5：运行目标测试、证据校验、收尾预览并提交本任务改动。

## Expected Verification

- RED/GREEN：`python -m pytest script/tests/test_system_nas_menu_sql.py script/tests/test_restart_int_ruoyi_local_schema.py -q`
- GREEN：本机 MySQL 查询 `system_menu.id IN (5900,5901,5902,5903)` 标题均为标准 UTF-8 文案。
- GREEN：bug regression evidence validator。
- GREEN：`git diff --check`。
- GREEN：task-closeout-cleanup 预览。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。通过菜单数据和必需迁移探针修复根因，不在前端做展示兜底。
- `是否从根因和长期维护角度解决`：是。标准 SQL 明确 UTF-8 文案，本机重启脚本以后能检测并修复已存在但标题漂移的运行库记录。
- `是否存在临时补丁或绕过`：否。本机运行库更新只把已损坏菜单记录恢复为标准菜单数据，不修改 NAS 文件、权限恢复、发布或租户业务数据。

## 当前状态

completed

## 验证结果

- REPRO：`SELECT id, name, HEX(name) FROM system_menu WHERE id IN (5900,5901,5902,5903)` -> 当前本机运行库为 `NAS??` / `NAS????`，十六进制含 `3F` 问号字节。
- RED：`python -m pytest script/tests/test_system_nas_menu_sql.py script/tests/test_restart_int_ruoyi_local_schema.py -q` -> FAIL，原因：SQL 仍为旧菜单文案，重启脚本缺少 NAS 菜单标题迁移探针。
- GREEN：`python -m pytest script/tests/test_system_nas_menu_sql.py script/tests/test_restart_int_ruoyi_local_schema.py -q` -> PASS，4 tests。
- GREEN：本机 MySQL 查询 `system_menu.id IN (5900,5901,5902,5903)` -> PASS，标题为 `NAS 管理`、`NAS 配置查询`、`NAS 配置保存`、`NAS 连接测试`，十六进制为 UTF-8 中文字节且不含 `3F` 问号字节。
- GREEN：Playwright + 本机 Chrome 只读登录 `http://localhost:8081/system/nas` -> PASS，左侧菜单显示 `NAS 管理`，页面未出现 `NAS??`。
- SCAN：`clear-frontend-copy` 针对 `yudao-ui-admin-vue3/src/views/system/nas` -> `garbled_text: 0`；其余 mixed_language 为 NAS/DCC/ACE 等必要技术术语与已有历史文案，不纳入本次乱码修复。
- REGRESSION：`git diff --check` -> PASS，仅 CRLF 工作区提示，无 whitespace error。
- GREEN：bug regression evidence validator -> PASS。
- CLOSEOUT PREVIEW：`task_closeout.py --task-id 20260604-nas-menu-garbled-title --mode preview` -> READY，keep `task.md` / `execution-log.md` / `bug-regression-evidence.md`，delete `<none>`，blocked `<none>`，warnings `<none>`。

## 剩余阻塞

- 暂无。

## Cleanup Keep

- `doc/tasks/20260604-nas-menu-garbled-title/bug-regression-evidence.md`
