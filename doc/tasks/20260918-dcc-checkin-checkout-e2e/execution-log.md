# Execution Log

## Current Status
blocked

## 2026-09-18 Preflight

- 用户明确授权执行本轮真实 E2E。
- 已读取：`docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/worktree-restrictions.md`、`docs/branch-runtime-ports.md`、`docs/task-closeout-rules.md`。
- 已读取 DCC 检出检入验收资料：`docs/dcc-minimal-main-flow/acceptance.md`、`docs/product/dcc-windchill-version-phase1-user-flows.md`、`docs/product/dcc-windchill-version-phase1-acceptance-criteria.md`。
- 当前工作区分支：`int_qms`；仓库目录名为 `C:\IntRuoyiAll-int_main`，与分支名不一致。
- 任务开始前工作区存在大量既有修改和未跟踪文件；本任务不回滚、不清理、不提交这些改动。
- 现有监听端口初检：`8081` PID `31460`，`48081` PID `30292`；这两个端口属于 `int_main` 基准端口，不可直接作为当前 `int_qms` 分支运行态证据。
- `int_qms` 合约端口：前端 `8061`，后端 `48061`；待继续核对占用、进程来源和运行状态。

## TDD Evidence

- 本任务只执行既有功能的真实 E2E 验收，不修改生产代码；不新增 RED/GREEN 生产实现循环。

## 2026-09-18 Build Unblock Fix

- BDD: 后端 Java 17 运行包可从当前源码构建 -> Given 当前项目声明 Java 17 且 qms 后端必须由当前源码构建后再执行 E2E / When Maven 在 `testCompile` 阶段编译 MES 测试源码 / Then 测试源码不得调用 Java 21 才有的 `List#getFirst()`，构建应继续进入后续模块。
- RED: `scripts\runtime\start-branch-backend.ps1 -Slot 0 -Build` -> FAIL，`yudao-module-mes:testCompile` 在 `MesProBatchRecordExecutionFieldResponsibilityMapperTest.java:246` 报 `List#getFirst()` 找不到符号。
- IMPLEMENTATION: 将 `tenantInterceptors.getFirst()` 改为 Java 17 兼容的 `tenantInterceptors.get(0)`；测试前置已断言集合 size 为 1，语义不变。

## 2026-09-18 Read-Only Browser Probe

- 使用真实 Playwright 页面路径，浏览器为本机既有 Chrome，入口为 `http://127.0.0.1:8061`。
- 登录租户/账号标签：`芋道源码/admin`；密码通过进程环境注入，未写入任务文件、报告或截图。
- 登录页面自然请求：HTTP `200`，业务码 `0`。
- 权限信息自然请求：HTTP `200`，业务码 `0`。
- 目录树自然请求：HTTP `200`，业务码 `0`，返回 1 个根节点。
- 文件类别自然请求：HTTP `200`，业务码 `0`，返回 60 项。
- 受控浏览列表自然请求：`GET /admin-api/dcc/controlled-files/browser-page?pageNo=1&pageSize=20&latestVersionOnly=true`，HTTP `200`，业务码 `500`，消息为“系统异常”。
- 页面结果：显示“列表数据已失效”和“系统异常”，可见受控文件行数为 0；没有出现检出、检入或撤销检出按钮。
- 页面运行时错误：由 `getControlledFileBrowserPage` 触发的 `Error: 系统异常`，未出现 console error。
- 已保存证据：`e2e-artifacts/read-only-probe.json`、`e2e-artifacts/01-after-login.png`、`e2e-artifacts/02-browser-page.png`、`e2e-artifacts/read-only-probe-trace.zip`。
- 首次 Playwright 启动因 bundled Chromium 缺失失败；未安装新软件，改用已安装 Chrome 后完成上述真实页面探查。
- 当前后端 `48061` PID `8836` 的命令行加载 `IntRuoyiBackend/yudao-server/target/yudao-server-exec.jar`，不是独立运行副本；后端 health 为 `UP`，但该运行态不能证明当前工作区源码已加载。

## Blocker

- 受控浏览列表接口在真实页面自然加载阶段返回业务码 `500`，导致没有可安全选择的任务自有或已确认可用受控文件。
- 因无可见样本，未执行检出、修改、检入、版本历史和锁释放写入动作；不得把空列表或页面“系统异常”写成 E2E 通过。
- 继续执行前必须先恢复当前 `int_qms` 运行态的受控浏览列表读取，并重新用同一真实页面账号走完整链路。

## 2026-09-18 Resume: qms Runtime Recovery

- 先前 BLOCKED 记录保留不变；本次用户请求继续执行真实 E2E，重新进入验证。
- `48061` 当前监听 PID `8836`，命令行属于当前仓库 `C:\IntRuoyiAll-int_main` 的 `int_qms` profile，父进程为 `scripts\runtime\start-branch-backend.ps1 -Slot 0`；未发现同目录 Maven/package 并发进程。
- 当前旧运行 Jar 为 `IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`，启动时间 `2026-09-14 19:46:59`；工作区源码和前端页面已有后续修改，先按正式 qms branch backend 入口重建，再重新核对 PID、Jar、health 和页面。
- 计划：记录并停止 PID `8836`，运行 `scripts\runtime\start-branch-backend.ps1 -Slot 0 -Build`，成功后用同一 Playwright 会话重新登录并复验受控浏览列表；构建失败或运行态仍返回业务码 500 时保持 BLOCKED。

## 2026-09-18 Resume: qms Build Result

- 按既定 qms 入口执行：`scripts\runtime\start-branch-backend.ps1 -Slot 0 -Build`。
- 为解决进程环境缺失问题，使用仓库自带工具：Apache Maven `3.9.11`，JDK `17.0.20.1`；未下载或安装新依赖。
- Reactor 构建结果：`yudao-module-dcc` 为 `SUCCESS`，但 `yudao-module-mes` 在 `testCompile` 阶段失败，导致 `yudao-server` 为 `SKIPPED`，未生成可用于本轮验证的新版 `yudao-server-exec.jar`。
- 精确阻断错误：`IntRuoyiBackend\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\dal\mysql\pro\batchrecord\MesProBatchRecordExecutionFieldResponsibilityMapperTest.java:246` 调用 `tenantInterceptors.getFirst()`，其静态类型为 `java.util.List<com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor>`，编译器报告 `找不到符号: getFirst()`。
- 未绕过该错误执行 `maven.test.skip`、未使用旧 Jar 重新冒充源码运行态，也未修改该无关测试文件。
- 构建命令结束码为 `1`；qms 后端 `48061` 当前无监听、无本次构建启动的 Java 进程。`int_main` 的 `8081/48081` 未操作。
- 由于当前工作区源码没有进入可运行的 `yudao-server`，无法安全重新执行真实页面列表加载，也不能继续执行检出、修改、检入、版本历史和锁释放。

## Final Status

- `BLOCKED`：阻断点从“受控浏览列表自然请求业务码 500”进一步确认到“正式 qms 源码构建被现有 MES 测试编译错误阻断”。
- 既有 Playwright 只读证据、截图和 trace 保留；本轮没有产生业务写入，也没有伪造 PASS。

## 2026-09-18 Authorization: Build Blocker Recovery

- 用户明确授权处理导致 qms 标准构建失败的 MES 测试编译阻断。
- 工作区已有目标修复差异：`MesProBatchRecordExecutionFieldResponsibilityMapperTest.java:246` 将 `tenantInterceptors.getFirst()` 改为 `tenantInterceptors.get(0)`；未发现其它测试源码中的同类 Java `List.getFirst()` 调用。
- 本次验证门禁：先运行 `yudao-module-mes` 定向 `test-compile`，再重跑原 qms 标准 `start-branch-backend.ps1 -Slot 0 -Build`；不得使用 `-Dmaven.test.skip=true`、旧 Jar 或手工 Java 启动绕过完整构建。

## 2026-09-19 Authorization: Runtime Configuration Recovery

- 使用仓库自带 Maven `3.9.11` 与 JDK `17.0.20.1` 重跑 qms 标准构建；完整 Maven reactor 通过，包含 `yudao-module-dcc`、`yudao-module-mes` 和 `yudao-server`，生成 `IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`，构建完成时间为 `2026-09-19 00:17:54 +08:00`。
- 通过 `scripts\runtime\start-branch-backend.ps1 -Slot 0 -ExtraArgs` 显式传入仓库历史本地运行记录中的 DCC 签名证据配置；未修改源码配置文件，密钥原值不写入任务证据。
- Spring 已越过 DCC 签名证据缺失门禁并进入 Tomcat/数据源初始化；随后真实启动失败，根因是无法解析 `MES_EDHR_INDEPENDENT_RECEIPT_ISSUER_SYSTEM`。当前 `application-local.yaml` 同时要求 `MES_EDHR_INDEPENDENT_RECEIPT_SIGNING_SECRET`。
- 只读核验结果：两个 MES 配置变量在当前进程、用户环境、机器环境均不存在；仓库 `IntRuoyiBackend`、部署脚本目录及可见隐藏 `.env` 中也没有合法值。标准本地重启脚本只内置 DCC 配置，未提供 MES 独立凭证。
- 启动失败后 `48061` 无监听；qms 前端 `8061` 仍监听；`int_main` 的 `8081/48081` 未停止、未重启、未作为替代运行态使用。
- 未猜测 issuer 或 signing secret，未使用单元测试 fixture 值，未关闭 fail-fast，未使用旧 Jar、API、数据库写入或其它端口替代真实页面动作。
- 本轮真实 Playwright 业务写入数为 `0`。由于当前源码后端无法健康启动，未重新进入受控浏览列表，也未执行检出、修改、检入、版本历史、直接来源和锁释放。

## Final Status

- `BLOCKED`：构建阻断已解除，但当前 qms 后端缺少项目正式 MES 独立凭证配置，真实页面链路无法安全开始。
- 恢复条件：由运行态维护者提供正式 `MES_EDHR_INDEPENDENT_RECEIPT_ISSUER_SYSTEM` 和 `MES_EDHR_INDEPENDENT_RECEIPT_SIGNING_SECRET`，随后重新启动 `48061`、验证 health 与受控浏览列表，再从真实页面继续完整检入检出闭环。

## 2026-09-19 Temporary EDHR Runtime Start

- 用户明确要求启动一个进程级临时 EDHR 配置的 qms 后端；临时 signing secret 使用随机字节生成，只记录长度，不记录内容。
- 启动结果：`48061` 监听 PID `47184`，进程为仓库内 JDK 17 `java.exe`，运行 Jar 为 `output\runtime\int_qms\branch-backend-runtime-20260919-002955.jar`。
- 配置边界：EDHR issuer 为 `MES`；EDHR 临时 signing secret 长度为 `64`；secret 未出现在 Java 命令行参数中，未写入任务文档。
- 健康检查：`GET http://127.0.0.1:48061/actuator/health` 返回 `{"status":"UP"}`。
- 保护措施：后续启动脚本 `start-qms-backend-temp-edhr.ps1` 已保留在任务目录，脚本只在运行时生成 secret 并输出长度/状态；如果 `48061` 已被占用则 fail fast，不会启动第二个后端。

## 2026-09-19 Temporary EDHR Runtime Final Verification

- 用户明确要求使用进程级临时 EDHR 配置；通过系统随机数生成 48 字节 secret，Base64 后长度为 `64`。secret 只注入 Java 子进程环境，不写入命令行、仓库文件、用户环境或机器环境，日志和本记录不保存其内容。
- 清理并停止了已确认归属 `int_qms/48061` 的旧进程；未操作 `int_main` 的 `48081`。最终使用 `output\runtime\int_qms\branch-backend-runtime-20260919-002955.jar` 独立运行副本、JDK 17 绝对路径和进程级 EDHR 环境启动。
- 最终启动结果：PID `19800`，端口 `48061` `LISTENING`，`GET /actuator/health` 返回 HTTP `200` / `UP`；等待稳定窗口后 PID 和端口仍保持不变；stderr 文件长度为 `0`。
- 真实页面只读预检未替代业务动作：受控浏览自然查询仍因数据库 schema 缺列失败，后端日志为 `Unknown column 'creation_idempotency_key' in 'field list'`；此前同一运行态还报告过 `predecessor_controlled_file_id` 缺失。
- 因没有可见、可安全操作的受控文件，未点击检出、检入、撤销检出或版本历史写入入口；本轮业务写入数为 `0`。完整检入检出 E2E 继续保持 `BLOCKED`，不是 PASS。

## 2026-09-19 Runtime Ownership Recheck

- 后续复核发现 PID `19800` 已退出，`48061` 被另一个 qms 独立运行实例 PID `38556` 接管；该进程加载 `output\runtime\int_qms\branch-backend-runtime-20260919-022328.jar`，health 为 `UP`。
- PID `38556` 不是本次临时 EDHR 启动动作创建的进程，且不能在不读取敏感环境内容的前提下证明其 EDHR secret 来源；未停止该未知归属的并发运行态，也未把它计入临时 secret 证据。
- 本次临时 EDHR 启动动作在 PID `19800` 窗口内已报告 secret 长度 `64`、端口监听和 health `UP`；当前端口归属变化已记录，完整 DCC E2E 仍为 `BLOCKED`。

## 2026-09-19 Continue Verification: Temporary EDHR Runtime and Browser Discovery

- 用户继续要求验证后，发现 `48061` 上有一个普通 target Jar 进程占用端口；经命令行归属检查确认它属于当前 `int_qms/48061`，不是 `int_main/48081`，随后停止该分支后端进程。
- 重新使用 `doc\tasks\20260918-dcc-checkin-checkout-e2e\start-qms-backend-temp-edhr.ps1` 启动进程级临时 EDHR qms 后端；启动结果 PID `43544`，运行 Jar 为 `output\runtime\int_qms\branch-backend-runtime-20260919-023324.jar`，health `UP`，EDHR issuer `MES`，临时 secret 长度 `64`，secret 未输出内容且未放入命令行。
- 登录诊断、受控浏览列表诊断已通过真实页面自然请求恢复：`/admin-api/dcc/controlled-files/browser-page?pageNo=1&pageSize=20&latestVersionOnly=true` 返回 HTTP `200`、业务码 `0`，列表总数 `31371`，首屏 20 行可见；这证明此前 schema 缺列阻断在当前运行态不再复现。
- 新增真实页面 E2E 脚本 `controlled-browser-full-e2e.cjs`。脚本只使用 Playwright 登录、前端路由筛选和页面按钮点击；自然响应监听只做抓包式记录，不直接调用业务 API。
- Discovery 结果一：关键词 `CODX-DCC-REV-FULL` 无数据；关键词 `CODEX` 命中 1 条任务数据 `CODEX-DCC-20260808-1259 / CODEX 文件上传流程测试 20260808`，但页面没有“检出/检入”按钮。
- 该 CODEX 行的真实页面自然响应字段：`requesterId=1074`，`checkedOutBy=null`，动作矩阵 `allowedActions=[VIEW, PREVIEW, DOWNLOAD]`，不含 `MAJOR_REVISION`；当前 `admin` 账号既不是 requester，也没有该文件的大版本修订权限，因此前端按规则隐藏检出入口。
- Discovery 结果二：更宽关键词 `自动化,E2E,上传流程,测试` 中存在可检出的正式业务文档，例如 requester 为 `1` 的 `P-IDE-001 数显压力表采购技术要求 A3 自动化.pdf` 等；这些不是本任务数据，未点击写入入口，未借用正式文件完成流程。
- 本轮真实页面业务写入数仍为 `0`。当前阻断点从“列表不可读/schema 缺列”更新为“现有任务自有 CODEX 数据对 admin 无检出权限，且没有其它可安全操作的任务自有可检出行”。

## 2026-09-19 Runtime Final Stabilization

- 收尾检查时发现 `48061` 又被旧的 qms target Jar 启动片段接管；经命令行归属检查确认仅涉及当前仓库 `int_qms/48061`，未触碰 `int_main/48081`。
- 停止该 target Jar 后，重新使用安全脚本启动独立 runtime Jar；最终稳定进程 PID `39892`，运行 Jar `output\runtime\int_qms\branch-backend-runtime-20260919-024740.jar`，health `UP`，EDHR issuer `MES`，临时 secret 长度 `64`，secret 内容未输出。
