# DCC 检出检入完整 E2E 验证报告

## Current Status
blocked

## Scope

- 环境：本机 `int_qms` 分支运行态
- 前端：`http://127.0.0.1:8061`
- 后端：`http://127.0.0.1:48061`
- 页面入口：`/dcc/controlled-file/browser?scope=global&pageNo=1&pageSize=20`
- 身份标签：`芋道源码/admin`
- 业务动作边界：只允许 Playwright 通过真实页面完成；未使用 fetch、API client 或数据库写入代替页面动作

## Preflight

- 前端端口 `8061`：监听，PID `18928`，Vite `branch-qms`。
- 后端端口 `48061`：监听，PID `8836`，Java 17。
- 后端 health：`{"status":"UP"}`。
- 当前后端 Jar：`IntRuoyiBackend/yudao-server/target/yudao-server-exec.jar`，不是独立运行副本；因此仅作为运行态风险记录，不能证明当前工作区源码已进入运行 Jar。
- 当前提交：`a9bcb6d36d96145ddc1252f111347b644b328deb`。

## Real Browser Evidence

| Checkpoint | Result | Evidence |
| --- | --- | --- |
| 真实登录 | PASS | 登录自然请求 HTTP 200，业务码 0 |
| 权限信息加载 | PASS | `get-permission-info` 自然请求 HTTP 200，业务码 0 |
| 目录树加载 | PASS | `dcc/directories/tree` 自然请求 HTTP 200，业务码 0 |
| 文件类别加载 | PASS | `dcc/file-categories` 自然请求 HTTP 200，业务码 0 |
| 受控浏览列表加载 | BLOCKED | `dcc/controlled-files/browser-page` HTTP 200，业务码 500，消息“系统异常” |
| 页面可见受控文件行 | BLOCKED | 页面显示“列表数据已失效”，行数 0 |
| 检出 | NOT RUN | 无可见目标行，未点击写入入口 |
| 检入 | NOT RUN | 未完成检出，未打开检入提交 |
| 版本历史/来源/锁释放 | NOT RUN | 前置列表读取失败 |

## Page Result

真实页面显示：

- “受控浏览”
- “列表数据已失效”
- “系统异常。已清空上一次结果，避免把旧文件误认为当前筛选结果。”
- 未出现检出、检入或撤销检出按钮

页面运行时错误堆栈指向前端 `getControlledFileBrowserPage`，不是空数组场景：

```text
Error: 系统异常
at createApiError (.../src/config/axios/service.ts:41:17)
at async getControlledFileBrowserPage (.../src/api/dcc/controlledFile/workflow.ts)
at async getList (.../src/views/dcc/controlled-file/browser/index.vue)
```

## Evidence Files

- [read-only-probe.cjs](C:/IntRuoyiAll-int_main/doc/tasks/20260918-dcc-checkin-checkout-e2e/read-only-probe.cjs)
- [read-only-probe.json](C:/IntRuoyiAll-int_main/doc/tasks/20260918-dcc-checkin-checkout-e2e/e2e-artifacts/read-only-probe.json)
- [01-after-login.png](C:/IntRuoyiAll-int_main/doc/tasks/20260918-dcc-checkin-checkout-e2e/e2e-artifacts/01-after-login.png)
- [02-browser-page.png](C:/IntRuoyiAll-int_main/doc/tasks/20260918-dcc-checkin-checkout-e2e/e2e-artifacts/02-browser-page.png)
- [read-only-probe-trace.zip](C:/IntRuoyiAll-int_main/doc/tasks/20260918-dcc-checkin-checkout-e2e/e2e-artifacts/read-only-probe-trace.zip)

## Conclusion

本轮真实 E2E 在受控浏览列表读取阶段阻塞，不能继续执行检出、检入和版本历史闭环。结论为 `BLOCKED`，不是产品流程 PASS，也不是“无数据通过”。

恢复条件：先修复或恢复当前 `int_qms` 运行态的受控浏览列表读取，并确认运行 Jar 来源与当前验证代码一致；随后使用同一租户/账号重新从真实受控浏览页面执行检出、有效修改、检入、历史和锁释放验证。

## Runtime Recovery Retry

- 已按 qms 正式入口执行 `scripts\runtime\start-branch-backend.ps1 -Slot 0 -Build`，并使用仓库自带 Maven `3.9.11` 与 JDK `17.0.20.1`。
- `yudao-module-dcc` 编译成功，但整包在 `yudao-module-mes` 测试源码编译阶段失败，错误为 `MesProBatchRecordExecutionFieldResponsibilityMapperTest.java:246` 对 `List` 调用不存在的 `getFirst()`；因此 `yudao-server` 被跳过，构建结束码为 `1`。
- 本次未使用 `maven.test.skip` 等绕过方式，未修改无关 MES 测试，未使用旧 Jar 继续做写入验证。
- 构建结束后 `48061` 无监听，qms 新运行态未形成；`int_main` 的 `8081/48081` 未操作。
- 最终仍为 `BLOCKED`：检出、有效修改、检入、版本历史、直接来源和锁释放均 `NOT RUN`。需要先修复或由项目负责人授权处理上述构建阻断，再重新从真实页面走完整链路。

## Runtime Recovery After Authorization

- 用户授权处理构建阻断后，使用仓库自带 Maven `3.9.11` 和 JDK `17.0.20.1` 重跑 `scripts\runtime\start-branch-backend.ps1 -Slot 0 -Build`；完整 reactor 构建成功，生成当前源码 `yudao-server-exec.jar`。
- 使用同一 qms 分支入口通过 `-ExtraArgs` 显式传入 DCC 电子签名证据配置。Spring 已越过该门禁，但在 MES 独立凭证配置处 fail-fast：

  `Could not resolve placeholder 'MES_EDHR_INDEPENDENT_RECEIPT_ISSUER_SYSTEM'`

- 当前 `application-local.yaml` 要求 `MES_EDHR_INDEPENDENT_RECEIPT_ISSUER_SYSTEM` 和 `MES_EDHR_INDEPENDENT_RECEIPT_SIGNING_SECRET`；只读核验确认两个变量在当前进程、用户环境、机器环境以及仓库可见 `.env` 配置中均不存在。
- 未猜测 issuer 或 signing secret，未使用单元测试 fixture 值，未关闭 fail-fast，未使用旧 Jar、API、数据库写入或其它端口绕过真实页面。
- 本次启动失败后 `48061` 无监听；`8061` 前端仍监听；`int_main` 的 `8081/48081` 未触碰。

## Final Coverage

| 阶段 | 结果 |
| --- | --- |
| Java 17 完整后端构建 | PASS |
| qms 当前源码后端启动 | BLOCKED |
| 真实登录 | PASS（既有 Playwright 证据） |
| 受控浏览列表重新读取 | NOT RUN，后端未健康启动 |
| 真实检出 | NOT RUN |
| 有效修改 | NOT RUN |
| 真实检入 | NOT RUN |
| 下一小版本/直接来源/历史 | NOT RUN |
| 检出锁释放 | NOT RUN |
| 本轮业务写入 | 0 |

## Updated Conclusion

本轮不能宣称检入检出 E2E 通过。构建阻断已解除，但当前环境缺少项目正式 MES 独立凭证，导致 qms 后端无法启动；在该配置由运行态维护者提供前，完整真实页面闭环保持 `BLOCKED`。

## Latest Runtime Recovery

- 临时 EDHR 配置：PASS。secret 由进程启动时随机生成，仅记录长度 `64`，未输出内容。
- qms 独立运行态：PASS。运行 Jar 为 `output\runtime\int_qms\branch-backend-runtime-20260919-002955.jar`，PID `19800`，端口 `48061` 持续监听，health HTTP `200` / `UP`，stderr 为空。
- DCC 受控浏览预检：BLOCKED。真实页面链路的自然查询触发后端 SQL 错误，缺少 `dcc_controlled_file.creation_idempotency_key`；此前同一 schema 还缺少 `predecessor_controlled_file_id`。
- 真实检出、有效修改、检入、下一版本、来源、历史和锁释放：NOT RUN。由于列表读取失败，没有安全的可追踪目标文件；本轮业务写入数为 `0`。
- 后续运行态复核发现 `48061` 已被并发 qms 进程 PID `38556` 接管；该进程不是本次临时 EDHR 启动动作创建的实例，因此不把其当前状态当作临时 secret 归属证明，也未强停该进程。

### Final Judgment

EDHR 临时配置已成功解除后端启动阻断，但 DCC 数据库 schema 未与当前源码同步，完整检入检出 E2E 仍为 `BLOCKED`。补齐正式 schema 迁移后，需重新用 Playwright 从真实页面执行全流程，不能用本次健康检查或 API 响应替代业务验收。

## Latest Browser Discovery After EDHR Restart

- qms 后端已重新以进程级临时 EDHR 配置启动；最终稳定进程 PID `39892`，Jar `output/runtime/int_qms/branch-backend-runtime-20260919-024740.jar`，health `UP`，临时 secret 只记录长度 `64`。
- 受控浏览列表已恢复：真实页面自然请求返回 HTTP `200` / 业务码 `0`，总数 `31371`，首屏 20 行可见。
- 任务自有 CODEX 数据查找：
  - `CODX-DCC-REV-FULL`：0 行。
  - `CODEX`：1 行，`CODEX-DCC-20260808-1259 / CODEX 文件上传流程测试 20260808`。
  - 该行无“检出/检入”按钮；自然响应显示 `requesterId=1074`，`allowedActions=[VIEW, PREVIEW, DOWNLOAD]`，不含 `MAJOR_REVISION`。
- 宽关键词只读筛选发现其它正式业务文档可检出，但它们不是本任务数据，未点击写入入口。

### Current Final Judgment

当前不能宣称完整检出/检入 E2E 通过。运行态和列表读取已恢复，但现有任务自有 CODEX 文件对 `admin` 无编辑/修订权限；按 E2E 规则不能借用正式业务文件，也不能通过 API/DB 写入制造成功。恢复条件是准备一条 `admin` 可检出的任务自有受控文件，或通过真实权限配置让当前 CODEX 任务数据对 `admin` 具备 `MAJOR_REVISION` 后，再重新从真实页面执行检出、检入、历史和锁释放闭环。
