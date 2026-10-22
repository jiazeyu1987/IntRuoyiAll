# DCC 检出检入完整 E2E

## Current Status
blocked

## Task Goal

使用 Playwright、真实前端页面、真实测试租户“芋道源码”和 `admin` 账号，完整验证 DCC 受控文件的检出、修改、检入流程。

## Milestones

1. 完成 E2E 规则、登录规则、端口规则和收尾规则核对。
2. 确认当前分支、前后端运行态、运行 Jar 来源和测试入口。
3. 通过真实页面找到任务自有或已确认可用的受控文件。
4. 通过真实页面完成检出，记录检出前后页面状态。
5. 通过真实页面完成有效内容或允许元数据修改并检入。
6. 通过真实页面确认下一小版本、直接来源、历史保留和检出锁释放。
7. 形成验证报告，区分 PASS、FAIL、BLOCKED 和未执行项。

## Expected Verification

- 页面真实完成检出。
- 检出不提前产生新小版本。
- 页面真实完成有效检入。
- 生成下一小版本，直接来源为检出前版本。
- 前一版本仍可在版本历史中查看。
- 检入后检出锁释放，页面不再显示当前用户持有开放检出。
- Playwright 保存 trace、关键步骤截图和控制台/pageerror/写请求证据。

## BDD

BDD: 受控文件检出后检入生成下一小版本 -> Given 当前最新工作小版本存在且当前账号具有编辑权限且没有开放检出；When 当前账号从真实页面检出该版本，完成有效内容或允许元数据修改并填写检入说明后提交检入；Then 页面显示检入成功，生成下一小版本并保留直接来源，旧版本仍可追溯且检出锁释放。

BDD: 后端 Java 17 运行包可从当前源码构建 -> Given 当前项目声明 Java 17 且 qms 后端必须由当前源码构建后再执行 E2E；When Maven 在 `testCompile` 阶段编译 MES 测试源码；Then 测试源码不得调用 Java 21 才有的 `List#getFirst()`，构建应继续进入后续模块。

RED: `scripts\runtime\start-branch-backend.ps1 -Slot 0 -Build` -> FAIL，`MesProBatchRecordExecutionFieldResponsibilityMapperTest.java:246` 调用 `List.getFirst()`，`yudao-server` 被跳过。

## Blocking Condition

- 构建阻断处理后，完整 Maven reactor 已成功生成当前源码 Jar。
- 按 qms 分支正式入口启动时，DCC 签名证据配置已通过显式启动参数提供；Spring 随后在 MES 独立凭证配置处 fail-fast。
- `MES_EDHR_INDEPENDENT_RECEIPT_ISSUER_SYSTEM` 与 `MES_EDHR_INDEPENDENT_RECEIPT_SIGNING_SECRET` 在当前进程、用户环境、机器环境及仓库可见 `.env` 配置中均不存在。
- 未猜测 issuer 或 signing secret，未关闭校验，未使用旧 Jar 或其它端口继续业务验证；真实检出、检入、历史和锁释放仍未执行。

## Latest Runtime Recovery

- 用户明确授权使用进程级临时 EDHR 配置；随机 secret 仅存在于 qms Java 子进程环境，未出现在命令行、仓库文件或任务证据中；仅记录 secret 长度 `64`。
- 已使用 `output\runtime\int_qms` 独立运行 Jar 重新启动 qms 后端：当前最终记录的临时 EDHR 运行态 PID `39892`，端口 `48061`，health `UP`，secret 长度 `64`，启动脚本未输出 secret 内容。
- 真实页面预检已越过此前 schema 缺列阻断：受控浏览列表自然请求返回 HTTP `200` / 业务码 `0`，总数 `31371`。
- 现有任务自有 CODEX 数据 `CODEX-DCC-20260808-1259` 对当前 `admin` 无检出权限：`requesterId=1074`，动作矩阵仅 `VIEW/PREVIEW/DOWNLOAD`，没有 `MAJOR_REVISION`，页面不显示“检出/检入”按钮。
- 宽关键词发现其它可检出的正式业务文档，但它们不是本任务数据，未执行写入。当前任务仍为 `blocked`，恢复条件是准备一条 `admin` 可检出的任务自有受控文件，或通过真实页面权限配置让当前 CODEX 任务数据对 `admin` 具备修订权限。

## Design Constraints

- 业务动作只能由 Playwright 操作真实前端页面完成。
- 不使用 `fetch`、`apiGet`、API client、数据库写入或 mock 替代页面动作。
- API 仅允许记录页面自然请求状态或做最终只读核验。
- 不创建、修改或清理共享业务基线数据；优先使用任务自有数据或页面明确可用的可追踪数据。
- 不停止或重启 `int_main` 后端；当前工作区分支为 `int_qms`，必须使用其合约端口，不得把 `8081/48081` 误当作本分支运行态。
- 不触碰本任务开始前已有的工作区改动。

## Cleanup Keep

- doc/tasks/20260918-dcc-checkin-checkout-e2e/task.md
- doc/tasks/20260918-dcc-checkin-checkout-e2e/execution-log.md
- doc/tasks/20260918-dcc-checkin-checkout-e2e/verification-report.md
- doc/tasks/20260918-dcc-checkin-checkout-e2e/read-only-probe.cjs
- doc/tasks/20260918-dcc-checkin-checkout-e2e/start-qms-backend-temp-edhr.ps1
- doc/tasks/20260918-dcc-checkin-checkout-e2e/login-diagnostic.cjs
- doc/tasks/20260918-dcc-checkin-checkout-e2e/browser-page-diagnostic.cjs
- doc/tasks/20260918-dcc-checkin-checkout-e2e/controlled-browser-full-e2e.cjs
