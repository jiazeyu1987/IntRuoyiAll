# 执行记录

## 用户意图

将流程1-11所有尚未进入主干的代码融合到 `D:/IntRuoyiWorktree/xiufu20260826`，已在主干的代码不重复处理。

## BDD

BDD: 流程代码选择性融合 -> Given 目标 worktree 基于当前 int_main 且初始干净；When 逐个核对流程分支并融合不在主线的代码提交；Then 目标 worktree 包含应融合的流程代码且不包含已在主线的重复提交、无关文档或并行改动。

## 初始证据

- 目标 worktree：`D:/IntRuoyiWorktree/xiufu20260826`
- 目标分支：`codex/xiufu20260826`
- 初始基线：`2faf0f33234614f46867e5d23e450c41ef62cc1f`
- 主工作树：`E:/IntRuoyi`，本任务不修改。

## 命令记录

- 已读取 `docs/worktree-restrictions.md`、`docs/branch-runtime-ports.md`、`docs/powershell-memory.md`、`docs/task-closeout-rules.md`。
- 已创建目标 worktree，初始化期间等待 Git checkout 完成，最终状态干净。

## 候选提交核对

- 流程1、2、7、8、9、10、11：对应代码或验收提交已经在 `int_main`，不重复融合。
- 流程4：`ac93ad0f6` 的流程4代码与主干已有 Tx-A 结果持久化内容一致；流程4当前仍有后续收敛任务，不重复导入旧版本。
- 流程3：只融合 `b2c800336` 的流程3测试 fixture 文件。
- 流程5：只融合 `ae33715ed` 中流程5自有的测试 fixture 和测试 SQL；排除该提交中的 `docs/worktree-memory.md`。
- 流程6：融合 `9c74c6b0f` 和其后续 `7358d340e`，因为权威解析器和测试链尚未进入 `int_main`。
- 流程8、10旧分支基于更早主线，包含相对当前主线的删除，不能整体合并；其已验证代码以当前 `int_main` 为准。

## 选择性融合原则

只融合上述 task-owned 代码和测试文件。任何会删除当前主线较新代码、包含并行任务、运行产物或仅文档历史的分支均排除，并记录排除原因。

## 当前阻塞

- 初始目标 worktree 基线中的 runtime guard 是 v6，而共享登记表是 v7；已在目标 worktree 补齐 v7 runtime 基线，等待正常提交复验。

## 融合结果

- 已应用流程3 `b2c800336` 的测试 fixture，并按当前主线补齐签名密码 mock、删除过时的 `applyConfirmedAllocations` 断言。
- 已应用流程5 `ae33715ed` 的两个 task-owned 测试 fixture/SQL 文件，排除 `docs/worktree-memory.md`。
- 已应用流程6 `9c74c6b0f` 和 `7358d340e` 的权威建批解析器、入口合同、服务和测试代码。
- 流程1、2、4、7、8、9、10、11没有重复导入：对应代码已在主干，或旧分支相对当前主干会删除较新代码。

## 验证证据

- RED: 未加引号的 Maven `-Dtest` 多类参数 -> PowerShell 参数解析失败；随后改用引号包裹参数。
- RED: 首轮融合定向测试 -> 流程3 6 failures/2 errors，原因是当前主线新增签名密码必填及过时的完成服务断言；未修改生产代码。
- GREEN: `mvn -o -pl yudao-module-mes -am -DskipTests compile` -> `BUILD SUCCESS`，24/24 modules。
- GREEN: 流程3定向测试 -> `8/8 PASS`。
- GREEN: 流程3/5/6组合定向测试 -> `222/222 PASS`，0 failures、0 errors。
- GREEN: `git diff --check` -> PASS。
- NOT RUN: 完整流程4/7/8/10真实联调、数据库迁移、真实租户 Playwright E2E；本任务只做代码融合，不把定向测试冒充全链路验收。

## Runtime 基线复验

- 目标已按 v7 登记：slot 43，前端 `8258`，后端 `48258`。
- 目标 worktree 已同步 v7 所需的 runtime 文档和 guard/profile 文件。
- `branch-runtime-port-guard.ps1`：PASS。
- 未修改共享 runtime 登记表，不使用 `--no-verify`。
