# 任务：eDHR 灰色可填写空白单元格规则候选修复

- Task ID: `20260630-edhr-gray-fillable-cell-rules`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi-worktrees\edhr_table\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `completed`

## Task Goal

在新的 `edhr_table` 成对 worktree 中继续修复 eDHR Word 导入后的通用规则生成链路，让像截图里那种“灰色、应可填写”的空白单元格，能够被后端自动识别为可配置单元格规则候选，并在电子批记录模板页的 `单元格规则` 流程中直接可见、可选、可配置。

## Previous Task Check

- 上一个同仓 eDHR 任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-edhr-word-import-table-collapse-fix\task.md`
- 状态：`blocked`
- 处理说明：上一任务已完成组装Ⅰ导入结构恢复与比例修复，并把新的剩余问题“灰色可填写空白格候选缺失”显式交接到本任务；因此本任务作为新的后续需求继续推进。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 本任务涉及真实 E2E、登录验证、PowerShell 中文路径、成对 worktree、真实 Word 导入链路，必须命中并执行 `login-preflight`、`experience-preflight`、UTF-8 PowerShell 读写约束。
- `D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`
  - 必须创建前后端成对同名 worktree；根目录只协调，不在主工作区直接开发；必须登记 FE/BE 端口、worktree 路径、DB/Redis/文件服务目标和运行态归属。
- `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - 真实写入型 E2E 默认用 `测试租户/aoteman/111111`；`芋道源码/admin` 仅做最终只读复验；登录相关第一条正式命令必须先跑官方 `login-preflight.mjs`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - PowerShell 5.1 下所有中文文件读取必须显式 UTF-8；中文内容写入优先 `apply_patch`；命令串联不得使用 `&&`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；修复通用“结构化空白填写格候选恢复算法”和模板页规则消费一致性，不对单个表硬编码。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 灰色空白结构格自动成为规则候选 -> Given Word 导入后的结构化表格内存在灰色、空文本、应填写的骨架单元格 / When 后端生成 batchrecordreport JSON 与单元格规则候选 / Then 这些格子必须统一生成 fillForm 或等价规则候选，并出现在模板页 getCellRules 返回中。`
- `BDD: 非填写占位格不得误识别 -> Given 表格中存在纯 /、静态标题、说明文本、纯装饰格或纵向 merge 覆盖从属格 / When 后端计算候选规则 / Then 这些格子不得被误生成为可填写单元格规则候选。`
- `BDD: 模板页直接消费新增候选 -> Given getCellRules 已返回新增灰色空白格候选 / When 用户在电子批记录模板页打开 单元格规则 / Then 对应单元格必须可以看到、选中并进入编辑，不需要新增手工补录入口。`

## Milestones

1. M1：补建任务文档、经验门禁、request-command-log 与上一任务交接。`completed`
2. M2：创建 `edhr_table` 前后端成对 worktree，并登记端口/路径/运行态目标。`completed`
3. M3：定位后端规则候选生成链路，补充 RED 定向测试。`completed`
4. M4：实现通用灰色空白填写格候选恢复算法，并统一 fillForm / suggestions 来源。`completed`
5. M5：验证前端模板页规则弹窗能直接消费新增候选，必要时做最小消费侧修正。`completed`
6. M6：在 `edhr_table` 运行态完成真实导入、模板页规则配置验证，并用 `芋道源码/admin` 只读复验。`completed`
7. M7：提交任务改动、融合进 `int_main` 并删除 `edhr_table` worktree。`completed`

## Worktree Ledger

- worktree 名称：`edhr_table`
- 后端分支：`codex/edhr_table`
- 前端分支：`codex/edhr_table`
- 后端 worktree：`D:\ProjectPackage\Int\IntRuoyi-worktrees\edhr_table\ruoyi-vue-pro`
- 前端 worktree：`D:\ProjectPackage\Int\IntRuoyi-worktrees\edhr_table\yudao-ui-admin-vue3`
- FE 端口：`8139`
- BE 端口：`48139`
- 前端 baseUrl：`http://127.0.0.1:8139`
- 后端 actuator：`http://127.0.0.1:48139/actuator/health`
- DB 目标：`本机 ruoyi-vue-pro（待运行态确认）`
- Redis 目标：`本机默认 Redis（待运行态确认）`
- 文件服务目标：`本机默认文件服务（待运行态确认）`
- 验证样本：`C:\Users\BJB110\Desktop\2\2\RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc`
- 主验证页：`组装Ⅰ工序生产记录`

## Expected Verification

- 后端定向测试：
  - `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldAutoFillStructuredHeaderBlankCellsWhenRowCarriesEntryCues,MesProBatchRecordCellRuleSupportTest#buildSuggestions_includesStructuredHeaderBlankCellsWithEntryCues,MesProBatchRecordReportServiceImplDbTest#getCellRules_returnsStructuredHeaderBlankSuggestions" -Dsurefire.failIfNoSpecifiedTests=false test`
- 前端静态/定向验证：
  - 确认模板页未对空文本格做二次过滤，`getCellRules -> 单元格规则弹窗` 可直接消费新增候选。
- 真实链路验证：
  - `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://127.0.0.1:8139 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/batch-record-template`
  - 导入 Word -> 打开模板页 -> 进入 `单元格规则` -> 验证灰色空白格可配置。

## Completed Work

- 已确认前端模板页当前只消费 `getCellRules` 返回的 `rules/suggestions`，且缺少规则时会直接阻止模板展示，因此本问题根因优先在后端候选生成链路。
- 已确认现有后端测试已经覆盖部分空白格自动 `fillForm`，说明系统具备空白可填写格识别能力，但当前灰色格未命中通用候选规则。
- 已补通用后端修复：对 `TABLE_HEADER` 类结构行不再一刀切禁止空白候选，而是按“存在结构化填写线索 + 存在空白格 + 非 summary/slash-only”判断是否自动补 `fillForm`。
- 已用定向回归验证 `JSON -> suggestions -> getCellRules` 三层均能返回这类结构化灰色空白格。
- 已补运行态阻塞修复：本地重启脚本中 `MES scheduler workbench permission split` 的 `ProbeSql` 改为 PowerShell 字面量 here-string，避免反引号字段名被转义成 `ole_id`。
- 已补运行态门禁测试：`test_restart_int_ruoyi_local_schema.py`、`test_system_nas_menu_sql.py`、`DccNasPermissionSnapshotCaptureServiceImplTest` 均通过，`mvn -pl yudao-server -am -DskipTests package` 通过。
- 已完成真实链路验证：`测试租户/aoteman` 导入真实 Word 后，在 `组装Ⅰ工序生产记录 -> 单元格规则` 中确认灰色空白格可以直接选中并进入规则编辑。
- 已完成最终只读复验前置：`芋道源码/admin` 通过官方 `login-preflight.mjs` 成功进入本机前端首页，满足“最终复验身份可用”的门禁。

## Final Verification

- 后端定向测试：通过。
- 脚本与发布链路门禁测试：通过。
- `yudao-server` 全量打包：通过。
- 测试租户真实导入与模板页规则配置：通过。
- `芋道源码/admin` 官方登录最小路径只读预检：通过。

## Final Closeout

- `codex/edhr_table` 已 rebase 到最新 `int_main` 并以 `--ff-only` 融合进主工作区。
- 融合后回归说明：
  - `mvn -pl yudao-module-mes ... test` 单独执行会因未带起 `erp` 依赖模块而误报编译错误，不属于本任务代码回归。
  - 使用正确的融合后验证命令 `mvn -pl yudao-module-mes -am ... test` 后，相关定向回归通过。
  - `python -X utf8 -m pytest script/tests/test_restart_int_ruoyi_local_schema.py script/tests/test_system_nas_menu_sql.py -q` 通过。
  - `mvn -pl yudao-server -am -DskipTests package` 通过。
- 本任务已满足融合后验证门禁，可继续删除 `edhr_table` worktree。
