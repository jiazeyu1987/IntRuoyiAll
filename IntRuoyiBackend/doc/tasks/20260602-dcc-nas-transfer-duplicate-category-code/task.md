# 任务：修复并验证 NAS QMS 目录转移闭环

## Task Goal

修复并验证 NAS 管理到 DCC 模块的完整闭环：在本机前端使用 NAS 用户名 `ceshi`、密码 `Kdlyx123` 测试连接成功，刷新目录显示 NAS 目录，选择 `1. QMS documents` 转移到 DCC；DCC 受控浏览中的 `1. QMS documents` 目录结构和文件必须与 NAS 下同名目录一致；随后在 DCC 目录管理删除父文件夹并确认已删除，再次从 NAS 转移 `1. QMS documents`，并再次确认 DCC 受控浏览结构和文件一致。若第 1-7 步任一失败，必须按根因修复后从第 1 步重新执行。

本任务接续已有缺陷线索：测试服务器 NAS 转移文件时曾出现 `Duplicate entry 'NASCAT-...' for key 'dcc_file_category.uk_dcc_file_category_code'`。修复方向仍然要求处理同一 NAS 目录对应的既有分类时保持幂等并复用已有分类，不得再次插入相同 `code`，不得通过 fallback、吞异常或默认成功掩盖失败。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260602-dcc-viewer-token-config-missing/task.md`
- 状态：`blocked`
- 影响：已明确阻塞并记录原因，本任务可继续；不接管或回滚该任务未跟踪产物。

## Milestones

- [x] M1: 建立任务记录，确认上一后端任务状态，并记录设计约束。
- [x] M2: 补齐 NAS `1. QMS documents` 转移、删除父文件夹、再次转移的 BDD 与自动化验证入口。
- [x] M3: 定位当前失败点和根因：删除父文件夹后残留同 code NAS 类别未复用，导致再次转移无法稳定重建 DCC 关系。
- [x] M4: 编写可复现失败点的回归测试，先得到 RED 失败证据。
- [x] M5: 最小实现修复，使 NAS 目录、DCC 目录、分类、文件转移和删除父文件夹响应保持一致且不引入 fallback/吞异常。
- [ ] M6: 运行目标测试、相关回归和完整 1-7 E2E，记录 GREEN/REGRESSION 证据。
- [ ] M7: 运行任务收尾清理预览，完成任务文档并按规范提交本任务改动。

## Expected Verification

- BDD 场景记录在 `execution-log.md`。
- RED：新增/更新测试在修复前失败，失败原因指向 1-7 闭环中的首个真实失败点。
- GREEN：目标测试通过，证明修复后的 NAS `1. QMS documents` 转移、删除、再次转移闭环可成功。
- REGRESSION：运行受影响模块的相关测试，确认现有目录、分类、文件转移、删除父文件夹路径未回归。
- E2E：Playwright 打开本机 `http://localhost:8081`，按用户给定 1-7 步执行真实用户路径；如失败，修复后必须从第 1 步重新执行。
- `bug-regression-fix-loop` 证据校验通过。
- `task-closeout-cleanup` 预览通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是在 NAS 转移、目录删除、再次转移链路中保证目录、分类、文件关系可重复重建，并按唯一业务编码查找复用已有分类，避免重复插入。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress

## Completed Work

- 已建立本任务记录。
- 已确认上一后端任务处于 `blocked` 状态。
- 已将任务范围补齐为用户要求的 NAS `1. QMS documents` 1-7 完整闭环。
- 已补充回归测试并取得 RED 失败证据：删除父文件夹后残留同 code NAS 类别时，转移未按 code 复用并重新绑定。
- 完整 E2E 第一轮验证已暴露新增根因：删除 `1. QMS documents` 时后端已经完成删除，但前端 10 分钟内未收到 `delete-subtree` 响应，真实用户会停留在确认删除弹窗，必须降低删除父文件夹同步耗时并保持失败显式抛出。
- 已补充后续根因修复与测试证据：DCC 删除父文件夹改为分批删除受控文件；Infra 文件删除改为有界并发删除对象后批量删库；NAS 文件读取去掉 SMB `fileExists` 预检查，改由 `openFile` 作为真实读取判定，避免 NAS 可列出但探测返回 false 的特殊文件名被误判不存在。
- 已按用户确认切换到本机 `测试租户`/`tenant-id=122` 执行写入类 E2E；当前运行证据显示测试租户下仍有上次失败留下的 `1. QMS documents`，需在下一轮 E2E 开头清理后重新从第 1 步执行。
- 用户已清空 `yudao-dcc-e2e` 当前对象；已确认 MinIO 当前对象数为 `0`。清理后重跑的任务 25 被 11:12:54 后端重启打断，已记录失败证据；随后后端 `48081` 与 MinIO `9000` 连续健康检查通过，准备重新从第 1 步执行完整 E2E。
- 已补充本机运行态重启保护：DCC NAS full-loop E2E 运行时创建 `output/runtime/local-runtime-restart.guard`，本地重启脚本在停止 backend/frontend 前检测该 guard 并失败快显，避免外部重启打断测试租户真实写入链路。
- 已补充 E2E 证据加固：异常退出、未处理异常、未处理 rejection 均同步写出 `evidence.json`；DCC 受控文件全量分页比对期间每 30 秒记录 heartbeat，避免再次出现第一轮完成后无错误证据的静默退出。
- 已清理上一轮异常退出残留的 Playwright headless 浏览器进程，并补充 E2E 登录 token 一致性校验、前置清理目录树 heartbeat、删除父文件夹预检/提交 heartbeat，避免无效 refreshToken 循环或长等待再次静默卡住。
- 已将 E2E 登录 token 校验调整为同步真实页面会话 token：记录页面 token 是否与登录响应一致，但后续 API 校验使用页面当前 token；缺少页面 token 时仍失败快显。
- 本次提交前已复跑后端 DCC/Infra 目标单元测试、NAS 菜单 SQL 测试和前端 NAS 静态回归，均通过；完整 1-7 E2E 尚未在本次提交前重新跑通。

## Verification Evidence

- RED：`mvn -pl yudao-module-dcc -Dtest=DccControlledFileNasTransferServiceTest#processWaitingTasks_rebindsExistingNasCategoryCodeAfterDirectoryDeleted test` -> FAIL，`reusedCategoryCount` 期望 1 实际 0。
- E2E FAIL：`node output/playwright/20260602-dcc-nas-transfer-full-loop-rerun/verify-full-loop.mjs` -> FAIL，第一轮转移 51 个目录、953 个文件且 DCC/NAS 一致；删除父文件夹点击确认后 `delete-subtree` 等待 600000ms 超时，浏览器请求最终 `net::ERR_ABORTED`，前端未关闭确认弹窗。
- GREEN：`node scripts/system-nas-management.test.mjs` -> PASS，NAS 管理页转移确认弹窗挂载、完成任务恢复、完整配置刷新按钮状态等静态回归通过。
- GREEN：`mvn -pl yudao-module-infra "-Dtest=NasBrowserServiceImplTest,FileServiceImplTest" test` -> PASS，36 个测试通过，覆盖 SMB `openFile` 读取判定和文件对象有界并发删除。
- GREEN：`mvn -pl yudao-module-dcc "-Dtest=DccDirectoryAdminServiceImplTest" test` -> PASS，覆盖 DCC 父目录删除分批处理。
- GREEN：`mvn -pl yudao-server -am -DskipTests package` -> PASS，后端运行 jar 已按当前源码重新打包。
- E2E FAIL：`20260602-dcc-nas-transfer-full-loop-test-tenant-final` -> FAIL，步骤 1、2、NAS 快照和第一次转移任务创建均通过；第一次转移期间本机后端/对象存储运行态被外部清理/重启打断，任务 24 最终 51 个目录完成、49 个文件完成、909 个文件失败，失败信息为 MinIO `127.0.0.1:9000` connection refused。该结果不能作为成功证据，必须清理后从第 1 步重跑。
- BLOCKER CHECK：2026-06-02 10:55 只读检查 `yudao-dcc-e2e` bucket 仍约 9.2G，其中 `dcc` 约 7.9G、`showroom` 约 1.3G；用户正在清理该 bucket，当前不得启动新的转移写入。
- E2E FAIL：`20260602-dcc-nas-transfer-full-loop-after-minio-current-clear` -> FAIL，清理后从测试租户重跑，步骤 1、2、NAS 快照和第一次转移任务创建均通过；E2E 运行中 11:12:54 后端被 `script\deploy\restart-ruoyi-local-component.ps1 -Component backend` 替换，任务 25 最终 51 个目录完成、958 个文件失败，失败信息为 MinIO `127.0.0.1:9000` connection refused。
- RECHECK：2026-06-02 11:24，MinIO 当前对象数 `0`，后端 `48081` 与 MinIO `9000` 连续健康检查通过，最近 10 分钟未发现新的后端重启脚本事件。
- RED：`python -m pytest script/tests/test_restart_ruoyi_bat_runtime_config.py -q` -> FAIL，新增运行态重启保护测试失败，重启脚本和 E2E 脚本尚未实现 guard。
- GREEN：`python -m pytest script/tests/test_restart_ruoyi_bat_runtime_config.py -q` -> PASS，9 passed。
- GREEN：PowerShell parser check for `script/deploy/restart-ruoyi-local-component.ps1` -> PASS。
- GREEN：Manual guard check -> PASS，存在 `output/runtime/local-runtime-restart.guard` 时本地 backend 重启脚本 exitCode=1 并失败快显。
- E2E FAIL：`20260602-dcc-nas-transfer-full-loop-after-restart-guard` -> FAIL，步骤 1、2、第一次转移任务 26 均通过，任务 26 创建 51 个目录和 958 个文件且失败 0；进入 `04-first` DCC 全量一致性比对期间 E2E Node 进程被外部结束，未写出 evidence，不能作为成功证据。
- RED：`python -m pytest script/tests/test_restart_ruoyi_bat_runtime_config.py -q` -> FAIL，新增 evidence/heartbeat 断言失败。
- GREEN：`python -m pytest script/tests/test_restart_ruoyi_bat_runtime_config.py -q` -> PASS，11 passed。
- GREEN：`D:\Programs\node.exe --check yudao-ui-admin-vue3/output/playwright/20260602-dcc-nas-transfer-full-loop-after-openfile-read-fix/verify-full-loop.mjs` -> PASS。
- E2E FAIL：`20260602-dcc-nas-transfer-full-loop-after-evidence-heartbeat` -> FAIL，guard 与登录成功后卡在前置清理，后端有残留 Playwright 浏览器旧 refreshToken 刷新错误；该轮被人工停止，不能作为成功证据。
- RED：`python -m pytest script/tests/test_restart_ruoyi_bat_runtime_config.py -q` -> FAIL，新增登录 token 校验和删除/目录树 heartbeat 断言失败。
- GREEN：`python -m pytest script/tests/test_restart_ruoyi_bat_runtime_config.py -q` -> PASS，12 passed。
- GREEN：`D:\Programs\node.exe --check yudao-ui-admin-vue3/output/playwright/20260602-dcc-nas-transfer-full-loop-after-openfile-read-fix/verify-full-loop.mjs` -> PASS。
- GREEN：`python -m pytest script/tests/test_system_nas_menu_sql.py -q` -> PASS，1 passed。
- GREEN：`mvn -pl yudao-module-infra -Dtest=NasBrowserServiceImplTest test` -> PASS，11 tests passed。
- GREEN：`mvn -pl yudao-module-dcc "-Dtest=DccControlledFileNasTransferServiceTest,DccControlledFileQueryServiceTest,DccControlledFileWorkflowServiceImplTest" test` -> PASS，121 tests passed。
- CLOSEOUT PREVIEW：`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260602-dcc-nas-transfer-duplicate-category-code --mode preview` -> PASS，status ready，无 delete/blocked/warnings。
- E2E FAIL：`20260602-dcc-nas-transfer-full-loop-after-token-cleanup-heartbeats` -> FAIL，登录响应成功但页面 token 与登录响应不一致，脚本 fail-fast 写出 evidence；后续确认应同步页面当前 token 而非要求相等。
- RED：`python -m pytest script/tests/test_restart_ruoyi_bat_runtime_config.py -q` -> FAIL，页面当前 token 同步断言失败。
- GREEN：`python -m pytest script/tests/test_restart_ruoyi_bat_runtime_config.py -q` -> PASS，12 passed。
- GREEN：`D:\Programs\node.exe --check yudao-ui-admin-vue3/output/playwright/20260602-dcc-nas-transfer-full-loop-after-openfile-read-fix/verify-full-loop.mjs` -> PASS。

## Remaining Blockers

- 完整 1-7 Playwright E2E 尚未在本次提交前重新跑通，后续仍需使用 `测试租户` 从步骤 1 重跑并记录最终通过证据。
