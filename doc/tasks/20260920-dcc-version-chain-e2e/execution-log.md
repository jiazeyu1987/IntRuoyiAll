# 20260920 DCC 版本链完整 E2E Execution Log

## Rule Reads

- 已读取 `AGENTS.md`。
- 已读取 `docs/task-closeout-rules.md`、`docs/e2e-rules.md`、`docs/local-runtime.md`、`docs/branch-runtime-ports.md`、`docs/worktree-restrictions.md`、`docs/login-access.md`。
- E2E 业务动作必须由真实 Playwright 页面完成；API/数据库只允许被动证据和最终只读核验。

## BDD / RED / GREEN

- BDD 已记录于 `task.md`。
- RED/GREEN：待运行态和真实页面验证后补录。

## Runtime And Scope

- 分支：`int_qms`。
- 前端：`http://127.0.0.1:8061`；后端：`http://127.0.0.1:48061`。
- `8081/48081` 未使用、未停止、未重启；当前健康运行态未额外重启。
- 测试租户/账号：`芋道源码/admin`，密码不写入任务记录。
- 任务自有项目：项目 ID `262`，项目代码 `CODEX-DCC-PROD-20260920012155`，模板文件 `CODEX-DCC-MAJOR-20260920012234.docx`。
- 任务自有逻辑文件：文件编号 `DCC-VCHAIN-202609200151`；A/1 ID `2054545668044084001`，A/2 ID `2054545668044084002`，B/1 ID `2054545668044084003`。

## Milestone Log

- M1 GREEN：真实登录和运行态前置通过。
- M2 GREEN：真实上传页创建任务自有 A/1；创建响应 `/admin-api/dcc/controlled-files/submit` HTTP 200、业务码 0。证据：`artifacts/initial-version-chain-real.json`。
- M3 RED：首次小版本脚本在 A/2 生成后误以为 ACTIVE 浏览默认选中历史 A/1；这是验证定位时序问题，不是业务写入失败。
- M3 GREEN：真实页面检出 A/1、选择默认小版本、上传源文件、检入生成 A/2；响应 `/controlled-files/2054545668044084001/checkin` HTTP 200、业务码 0，返回 A/2、前驱 A/1、哈希 `4f75621c9396...`。随后真实页面切换指定 A/2 并提交审批，响应 `/controlled-files/2054545668044084002/submit` HTTP 200、业务码 0；四节点审批和文控签名完成，A/2 ACTIVE。证据：`artifacts/minor-version-real.json`、`artifacts/submit-minor-approval-real.json`、`artifacts/a2-approval-real.json`。
- M4 GREEN：真实页面从 ACTIVE A/2 检出，选择大版本、上传新源文件并检入生成 B/1；响应 `/controlled-files/2054545668044084002/checkin` HTTP 200、业务码 0，B/1 ID `2054545668044084003`；页面提交审批 HTTP 200、业务码 0。证据：`artifacts/major-version-real.json`。
- M5 GREEN：B/1 真实审批中心三次审核、文控批准签名及盖章 PDF 上传均 HTTP 200、业务码 0；最终签名返回 `taskActionResult=APPROVED`、`nextStatus=ACTIVE`、`evidenceStatus=VALID`。页面和最终浏览均显示 B/1 ACTIVE。
- M6 GREEN：B/1 详情页版本历史真实可见三条记录：B/1 直接来源 A/2、A/2 直接来源 A/1；哈希前后值均显示；操作者为 `瑛泰管理员 (admin)` 并显示时间；B/1 审批/发布结果显示已生效，A/1/A/2 显示已替代及后继版本。
- M7：已整理任务自有 JSON/PNG 证据；任务状态先置为 `ready_for_closeout`，待执行 cleanup preview/apply。
- CLOSEOUT GREEN：bundled Python 执行 `task_closeout.py --mode preview`，保留关键任务文档、脚本、JSON 和最终详情截图，删除中间步骤截图，无 warnings；随后 `--mode apply` 成功。
- CLOSEOUT 状态保持 `ready_for_closeout`：当前轮仅授权重启/执行 E2E，未授权 Git commit/push；未执行提交或推送，避免把共享工作区的其他脏改动混入。

## Final Chain Evidence

- 页面最终详情 URL：`/dcc/controlled-file/detail/2054545668044084003?management=1&from=browser...`
- 页面当前正式版本：`ACTIVE / B/1`。
- 版本历史来源：B/1 `直接来源 #2054545668044084002`；A/2 `直接来源 #2054545668044084001`。
- 页面哈希：B/1 `4f75621c9396 -> 4f75621c9396`；A/2 `4f75621c9396 -> 4f75621c9396`；A/1 初始源哈希为 `4f75621c9396`。
- 页面操作人/时间：B/1、A/2、A/1 均显示 `瑛泰管理员 (admin)` 及提交时间。
- 页面审批/发布：B/1 显示审批通过、已生效、当前有效；A/2 和 A/1 显示已生效、已替代及后继版本。

## Script Evidence

- `create-initial-version-real.e2e.cjs`：任务自有 A/1 创建。
- `minor-version-real.e2e.cjs`：A/1 检出、A/2 小版本检入。
- `submit-minor-approval-real.e2e.cjs`：页面选择指定 A/2 并提交审批。
- `verification-report.md`：最终验收摘要。
