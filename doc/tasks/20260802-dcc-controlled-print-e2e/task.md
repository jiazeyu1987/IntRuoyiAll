# 20260802 DCC 受控打印 E2E 验证

## Task Goal

验证 DCC 文控“受控打印”真实页面链路：从受控浏览或受控文件详情页对当前有效受控文件发起打印申请或打印动作，经过权限、审批或直接打印、水印/受控信息与记录生成后，形成可追溯打印记录。

## Scope

- 仅验证 DCC 文控受控打印场景。
- 不修复、不改造、不顺手处理上传、发布、审批中心、元数据、DCC 其它页面或 MES/eDHR 打印场景。
- 不使用 admin 账号绕过，不使用 API-only 或 SQL 改状态创建打印记录。
- 密码必须通过环境变量注入，任务文档和日志不得记录明文。

## Milestones

- [x] 读取 `AGENTS.md`、`docs/e2e-rules.md`、`docs/login-access.md`、`docs/frontend-development.md` 及触发规则。
- [x] 建立任务目录并记录 BDD、RED/GREEN、验证约束。
- [x] 核对本机运行态和 Playwright 前置。
- [x] 核对测试账号、任务自有 ACTIVE 受控文件与页面入口。
- [x] 使用 Playwright 真实页面尝试受控浏览/详情页受控打印路径。BLOCKED：受控浏览行无打印入口；真实点击文件号进入追溯详情后无受控打印/打印申请入口。
- [x] 验证打印输出受控信息、水印/编号/版本/打印人/打印时间。BLOCKED：页面无受控打印入口和必填表单，未触发最终打印件生成。
- [x] 验证打印记录/分发记录/操作日志以及无权限阻断。BLOCKED：未生成打印记录；负向账号可见同一 ACTIVE 文件但无打印入口。
- [x] 用只读 API/DB 核验打印记录、版本、份数、打印人、审批状态。BLOCKED：DB 仅确认当前有效版本和缺少受控打印记录表，不存在可核验的打印记录 ID/份数/审批状态。
- [x] 输出 `verification-report.md`。

## Expected Verification

- Playwright 登录有打印权限的非 admin 账号。
- 通过真实页面进入 DCC 受控浏览或详情页并定位任务自有 `ACTIVE` 文件。
- 页面真实点击打印/受控打印入口并填写用途、份数、接收部门/使用位置等必填信息。
- 若系统支持审批，使用审批账号走真实审批；若系统设计为直接打印，验证权限、水印和记录。
- 页面或打印输出展示打印编号、文件编号、当前有效版本、打印人、打印时间等受控信息。
- 打印记录/分发记录/操作日志出现本次打印记录。
- 无打印权限账号看不到按钮或收到明确权限拒绝。
- 只读 API/DB 仅用于最终核验，不用于创建打印记录或改状态。

## Applicable Experience Gates

- `docs/e2e-rules.md#dcc-文控审批处理入口门禁`：DCC 处理态必须来自真实页面入口，禁止 API-only 或只读 viewer 冒充。
- `docs/e2e-rules.md#e2e-脚本入口存在性门禁`：真实 E2E 必须区分页面入口缺失、静态合同、API wrapper 与真实路径 PASS。
- `docs/e2e-rules.md#playwright-浏览器可执行文件门禁`：优先使用本机 Chrome/Edge 可执行文件并记录来源。
- `docs/e2e-rules.md#playwright-目标链路与外部资源异常归因门禁`：目标链路错误必须记录，不得全局忽略。
- `docs/e2e-rules.md#playwright-快照与-daemon-收尾门禁`：任务自有 Playwright artifact 需要脱敏与清理归属判断。
- `docs/login-access.md`：非 admin、密码环境变量注入、登录失败必须记录实际入口和影响。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：本任务为独立验证，不进行生产代码修复；若缺入口/权限/数据/运行态，将记录 BLOCKED 和影响。
- `是否存在临时补丁或绕过`：否。

## Current Status

blocked

## Notes

- 初始 `git status --short --branch` 显示主工作区存在大量并行脏改动，本任务不提交、不回滚、不修改这些非任务文件；仅新增 `doc/tasks/20260802-dcc-controlled-print-e2e/` 下验证文档和任务自有证据。
- 本机前端 `http://127.0.0.1:8081/` 返回 HTTP 200，后端 `http://127.0.0.1:48081/actuator/health` 返回 `UP`，本机 Chrome 存在。
- 用户已提供密码注入方式；本任务使用 `$env:DCC_E2E_PASSWORD = -join (1..6 | ForEach-Object { [char]49 })`，未在日志或报告记录明文。
- Playwright 真实页面结果：`controlled-print-real-e2e-result.json` 为 `E2E_BLOCKED`。有权限非 admin 用户 `wangsiyu` 在受控浏览可定位任务自有 ACTIVE 文件，但行操作只有 `预览/下载`；从文件号真实点击进入追溯详情后无 `受控打印/打印申请/流程打印` 操作，且未出现打印用途、份数、接收部门、使用位置表单。
- 无打印权限路径：非 admin 用户 `zhangkeying` 已补浏览/预览权限但未补下载/打印权限，受控浏览可见同一文件且只有 `预览`，无打印入口。
- 只读 DB 核验：文件 `CODX-DCC-ORIG-20260802101521` 为 `ACTIVE`、版本 `V1.0`，master `current_active_controlled_file_id=2054545668044070287`；schema 仅发现 `dcc_approval_print_template`，未发现带份数/部门/使用位置的受控打印申请或记录表。
