# 任务：测试服 DCC 受控浏览缓存写入失败复测与修复跟进

- Task ID: `20260630-test-server-dcc-browser-cache-write-failure-followup`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-30`
- Current Status: `blocked`

## Task Goal

针对测试服务器 DCC 受控浏览页仍然出现 `DCC 受控浏览本地缓存写入失败，请检查浏览器本地存储权限。` 的报错，先确认测试服当前运行版本与线上真实根因，再在不引入 fallback 的前提下完成最小修复或正确发布，并用真实测试服页面复验问题消失。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260630-erp-production-order-material-list-bidirectional-link\task.md`
- 状态：`blocked`
- 处理说明：用户已切换到测试服 DCC 紧急排障，本次新任务开始前已将上一前端任务显式阻塞，避免与当前测试服修复串线。

## 当前状态

- `blocked`
- 已确认测试服 `172.30.30.58` 真实运行 bundle 仍是旧版 DCC 目录缓存实现，根因不是浏览器禁用本地存储，而是测试服尚未部署目录缓存轻量化修复。
- 干净前端发布 worktree 已完成静态 RED -> GREEN 回归，并生成补发版发布输入；但用户随后切换到“排产改动不要再被 eDHR 编译/测试问题卡住”的更高优先级任务，本任务主线跟进被显式阻塞。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`、`D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`、`D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`、`D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`、`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-agent-checklist.md`、`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`。
  - 补发版发布 worktree 额外命中 `D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`，要求发布输入来自干净 worktree，不混入其他任务 hunk。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 本任务的中文文档、命令输出与远端检查记录统一按 UTF-8 读写；PowerShell 串联命令禁止使用 `&&`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`
  - 发布输入必须来自干净 worktree；同文件混入其他任务 hunk 时必须在隔离 worktree 中重放补丁并独立提交。
- `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - 真实测试服登录或 Playwright 复验前必须先执行官方 `login-preflight.mjs` 最小登录路径，不得自行猜测登录流程。
- `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`
  - 测试服访问统一以 `172.30.30.58`、`/opt/intruoyi/runtime` 为准；远端命令优先只读核验当前部署与健康状态。
- `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`
  - 若确认问题来自“修复未进入测试服运行态”，后续发布必须走正式测试服发布链路与 manifest/镜像/tag 核验，不得手工改服务器文件绕过。
- `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-agent-checklist.md`
  - 若进入真实发布步骤，必须先核对维护仓发布预览参数、候选包、远端 `.env IMAGE_TAG` 与前后端镜像 tag。
- `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`
  - 测试服故障优先按“产物契约 -> 发布脚本 -> 环境运行态”做只读定界；不要先手工改库或改远端文件。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。继续显式暴露真实缓存写入失败原因，不增加“写失败也继续当成功”的兼容分支。
- `是否从根因和长期维护角度解决`：是。先验证测试服是否仍跑旧 bundle；若已部署新代码仍报错，再按真实大目录数据补齐缓存模型或写入路径的根因修复。
- `是否存在临时补丁或绕过`：否。禁止手工改测试服静态资源、浏览器设置或本地存储数据来伪造通过。

## BDD 场景

- `BDD: 测试服运行态包含本次缓存修复 -> Given 本机已完成目录缓存轻量化修复 / When 核对测试服实际前端 bundle 或部署版本 / Then 能证明测试服运行态已包含对应修复，而不是继续跑旧代码。`
- `BDD: 测试服 DCC 浏览页在真实目录数据下不再报缓存写入失败 -> Given 用户登录测试服并进入 DCC 受控浏览页 / When 页面加载真实目录树、分类与状态缓存 / Then 页面不再弹出本地缓存写入失败提示。`
- `BDD: 若测试服仍报错则先以失败测试锁定新根因 -> Given 测试服已部署当前修复但真实页面仍报错 / When 补充针对该根因的定向测试 / Then 先得到可重复失败的 RED 证据，再做最小实现修复。`
- `BDD: 测试服发布后需验证真实运行版本一致 -> Given 本次若需要重新发布测试服 / When 发布完成 / Then 远端 `.env IMAGE_TAG`、前后端镜像 tag、前端入口与真实页面行为都与本次修复一致。`

## 里程碑

1. M1：创建任务文档、执行日志并补齐 experience-preflight 门禁。`completed`
2. M2：只读诊断测试服当前部署、真实报错与是否已含修复。`completed`
3. M3：在干净前端发布 worktree 中补 RED 测试并完成最小缓存轻量化修复。`completed`
4. M4：生成只包含本次修复的前端发布输入，交由维护仓重新构建并发布到测试服。`blocked`
5. M5：测试服真实页面复验并回填结论。`blocked`

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\dcc-browser-cache-write-failure-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\dcc-browser-remember-state-cache-static.spec.js`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260630-test-server-dcc-browser-cache-write-failure-followup\bug-regression-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260630-test-server-dcc-browser-cache-write-failure-followup\frontend-feature-evidence.md`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\preflight\login-preflight.mjs --base-url http://172.30.30.58:8081/ ...`
- 测试服只读核验：远端 `.env IMAGE_TAG`、前后端镜像 tag、目标静态 bundle 关键片段、前端入口可访问。
- 测试服真实页面复验：DCC 受控浏览页进入目标目录后不再出现缓存写入失败提示。

## Current Blockers

- 用户已切换到“排产改动不要再被 eDHR 编译/测试问题卡住”的新优先级任务；当前测试服 DCC 主线跟进暂停。
- 补发版发布输入与静态回归已就绪，但维护仓补发版与测试服真实回归尚未在本主工作区继续推进。
