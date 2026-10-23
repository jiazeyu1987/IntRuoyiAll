# 执行记录

## 任务标识

- task-id: `20260919-dcc-checkin-checkout-e2e`
- 日期：2026-09-19
- 目标：DCC 检出、撤销检出、重新检出、小版本检入、大版本检入完整真实页面 E2E。

## 预检

- 规则文件：已读取 `docs/e2e-rules.md`、`docs/local-runtime.md`、`docs/login-access.md`、`docs/branch-runtime-ports.md`、`docs/task-closeout-rules.md`、`docs/dcc-minimal-main-flow/acceptance.md`。
- 业务动作边界：只允许 Playwright 真实前端页面完成写入动作。
- 初始状态：待记录端口、health、分支、运行进程和可用页面入口。

## BDD / TDD

- BDD 已写入 `task.md`。
- RED：本任务不修改生产代码；不执行生产代码 RED/GREEN。
- GREEN：以真实页面完成情况、页面刷新后的终态和 Playwright 证据为准。

## 运行记录

### 2026-09-19 真实页面 Discovery

- 入口：`http://127.0.0.1:8081`，租户/账号标签：`芋道源码/admin`。
- 发现目标：文件编号 `E2E-DCC-20260918234556`，文件名 `CODEX-DCC-MAJOR-20260918234457.docx`，当前版本 `C/1`，状态 `ACTIVE`。
- 页面自然数据：`requesterId=1`、允许动作包含 `MAJOR_REVISION`，真实页面显示“检出”。
- Discovery 未产生 DCC 写请求；保存 trace 和目标截图。

### 2026-09-19 Full E2E

- 第一次检出：PASS。页面点击“检出”，填写 `CODEX E2E checkout` 原因；自然 POST 返回 HTTP `200`、业务码 `0`，返回 `checkedOutBy=1`。
- 检出状态确认：PASS。页面最终显示“已由瑛泰管理员检出”“检入”“撤销检出”。
- 撤销检出：PASS。页面点击“撤销检出”，填写原因；自然 POST 返回 HTTP `200`、业务码 `0`，返回 `checkedOutBy=null`。
- 撤销后页面确认：PASS。页面最终恢复“检出”，没有“检入/撤销检出”。
- 第二次检出：PASS。自然 POST 返回 HTTP `200`、业务码 `0`，页面最终再次显示当前用户持有检出。
- 小版本源文件上传：PASS。通过真实检入弹窗文件控件上传仓库任务证据文件 `codex-upload-simulation-20260729.docx`；自然 `upload-preview` 返回 HTTP `200`、业务码 `0`，页面显示“已上传”。
- 小版本检入：PASS。页面填写修改说明/备注并点击“检入并生成小版本”；自然 POST 返回 HTTP `200`、业务码 `0`，生成 `C/2`、状态 `WORKING`，`currentActiveVersionNo=C/1`，检出锁为空。
- 大版本前置尝试：BLOCKED。继续从 `C/1` 点击“检出”时，页面自然 POST 返回 HTTP `200`、业务码 `1080000177`，文案为 `Controlled file number already has an unfinished workflow`；未继续重复提交。
- WORKING 收口：PASS。页面切换到 `C/2`，点击“提交审批”并确认；自然 POST `/dcc/controlled-files/2054545668044083988/submit` 返回 HTTP `200`、业务码 `0`。
- 最终页面复核：PASS。真实刷新后显示 `C/1 ACTIVE / 修改中`，页面仅显示“检出”，不显示当前用户“检入/撤销检出”，确认检出锁已释放。

### 证据

- Discovery：`e2e-artifacts/dcc-checkin-checkout-full-20260919073617.json`、对应 trace。
- Full 首轮检出证据：`e2e-artifacts/dcc-checkin-checkout-full-20260919073903.json`、对应 trace。
- Full 主要链路证据：`e2e-artifacts/dcc-checkin-checkout-full-20260919075712.json`、对应 trace。
- WORKING 提交审批：`e2e-artifacts/dcc-checkin-checkout-full-20260919080231.json`、对应 trace。
- 最终页面复核：`e2e-artifacts/dcc-checkin-checkout-full-20260919080336.json`、对应 trace。
- 关键截图均保存在同一 `e2e-artifacts` 目录；所有一次性脚本只监听页面自然响应，没有 `fetch`、`apiGet`、APIRequest 或数据库写入。

## 结果

- 检出/撤销检出/再次检出：PASS。
- 小版本检入生成 `C/2 WORKING`：PASS。
- WORKING 页面提交审批：PASS。
- 检入后检出锁释放：PASS。
- 大版本检入：BLOCKED，原因是 `C/2` 工作流尚未审批完成，后端正式规则拒绝从 `C/1` 再检出。
- 本轮没有修改生产代码；只新增本任务一次性 Playwright 脚本和证据文档。

### 收尾

- 任务状态按验收边界保持 `blocked`：大版本检入没有完成，不能改写为 `completed`。
- `task-closeout-cleanup --mode preview` 使用仓库绑定的 cleanup 脚本执行时返回：`BLOCKED task status is blocked; expected ready_for_closeout or completed`。
- 未执行 cleanup apply、Git 提交或推送；未删除 trace、截图、结果 JSON 或并行任务资产。

## 2026-09-19 换文件继续

- 用户要求换文件继续后，先排除已卡在 `C/2 修改中` 的 `E2E-DCC-20260918234556`，用真实受控浏览页面做只读 discovery。
- 现有任务自有候选核对：
  - `CODEX-DCC-20260808-1259`：页面可见但无“检出/检入”，动作矩阵仅 `VIEW/PREVIEW/DOWNLOAD`，未写入。
  - 历史 `CODX-DCC-*` 完整发布链路文件：当前 `8081` 受控浏览按文件号搜索为 0 行，未写入。
  - 排除两条本轮文件后，`E2E` 关键词无 `admin` 可检出/检入任务自有行。
- 真实页面创建新测试项目：PASS。通过 `产品建档申请` 创建并审批任务自有产品/项目，项目代码 `CODEX-DCC-PROD-20260919084653`，项目 ID `258`；随后通过项目代码页面配置 `admin` 权限和模板 `CODEX-DCC-MAJOR-20260919084750.docx`。
- 真实页面创建新受控文件：PASS。上传源文件、提交前校验和“创建受控文件”均走真实页面，生成任务自有文件 `E2E-DCC-20260919084910` / `A/1`；自然 `upload-preview` 与 `/controlled-files/submit` 均 HTTP `200`、业务码 `0`。
- 新文件继续检出：BLOCKED。该文件提交后不在受控浏览 `ACTIVE/ALL` 当前有效列表中；本次早期审批脚本过早读取页面，误判 `admin` 无待办，后续“继续修复”诊断已推翻该结论。本任务未冒用其它账号、未 API/DB 改审批状态。
- 本轮“换文件继续”新增证据：
  - `e2e-artifacts/create-task-owned-file-20260919084910.json`
  - `e2e-artifacts/approve-task-owned-file-20260919090110.json`
  - `e2e-artifacts/dcc-checkin-checkout-full-20260919085020.json`
  - `e2e-artifacts/dcc-checkin-checkout-full-20260919085845.json`
  - `e2e-artifacts/dcc-checkin-checkout-full-20260919090827.json`
- 结论保持 `blocked`：已换文件并完成新任务自有文件创建，但没有当前 `admin` 可继续审批至 `ACTIVE` 的页面待办，也没有另一条可检出任务自有文件；不能宣称完整检入检出流程 PASS。

## 2026-09-19 继续修复

- 重新诊断审批中心：真实页面 `/approval-center?moduleCode=DCC&viewType=TODO` 返回总数 `38`，其中包含 `E2E-DCC-20260919084910` 文控审核待办，任务 ID `fc738b6a-b406-11f1-bd27-b082e25ec548`；上一轮“admin 无待办”结论被推翻。
- 真实页面点击审批中心“审核”并填写电子签名后，`/admin-api/approval-center/tasks/review` 返回 HTTP `200`、业务码 `500`、消息“系统异常”。后端日志对应异常为 `software.amazon.awssdk.services.s3.model.NoSuchKeyException`，发生在 `DccControlledFileSignatureEvidenceServiceImpl.digestFile` 读取 `source_file_id=9198354931095`。
- 只读数据库核验：`E2E-DCC-20260919084910` 绑定 `source_file_id=9198354931095`，`infra_file.path=dcc/original/20260919/codex-upload-simulation-20260729.docx`；同一日期内该文件名已有多条相同 path 的 `infra_file` 记录。
- 代码根因：`FileServiceImpl.PATH_SUFFIX_TIMESTAMP_ENABLE` 默认值为 `false`，导致真实上传默认对象路径仅由日期与原文件名组成，同日同名上传会共用同一对象 key，不满足“生成上传 path，需要保证唯一”的本地代码注释和 DCC 签名证据读取要求。
- RED：`C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd -pl yudao-module-infra -Dtest=FileServiceImplTest test`，临时 `JAVA_HOME=C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17`。首次运行失败，`FileServiceImplTest` 3 个用例仍按旧路径断言。
- GREEN：恢复默认唯一后缀并调整 `FileServiceImplTest` 路径断言后，同一 Maven 命令 PASS，`Tests run: 38, Failures: 0, Errors: 0, Skipped: 0`。
- 未重启 `int_main` 后端，未替换当前运行 Jar；当前真实 E2E 仍不能继续到新文件 `ACTIVE`，需要用户明确授权后才能重启/换包并继续真实页面审批与检入检出。

### 版本链收口 RED/GREEN

- Given：任务自有文件 `E2E-DCC-20260920005230` 的 `A/2` 已完成真实审批页面全部动作并为 `ACTIVE`；When：再次从真实受控浏览页面点击“检出”；Then：不应再显示“修改中”或返回 `1080000177`。
- RED：将最终发布服务已有“遗留工作版本”测试改为遗留 `PENDING_DOC_CONTROL_REVIEW` 版本，预期发布后该版本转为 `SUPERSEDED`；现有实现仅处理 `WORKING`，该断言应失败。
- GREEN：最终发布时改为收口所有非终态遗留迭代，已通过定向测试；未修改检出门禁、不使用迁移脚本或数据库写入修正 E2E 数据。
- 定向服务测试：`DccControlledFileFinalizationServiceImplTest` 46/46 PASS。
- 相关查询回归：`DccControlledFileQueryServiceTest` 149/149 PASS。
- 模块构建：`yudao-module-dcc -DskipTests package` PASS，生成 `yudao-module-dcc-2026.04-SNAPSHOT.jar`。
- 脚本语法：两个 Playwright 任务脚本 `node --check` PASS。
- 运行态限制：当前 `48081` 由其它任务自有 Jar 进程占用，且未获停止/重启 `int_main` 后端授权；因此本修复尚未在新 Jar 上重新执行真实页面 E2E，不能宣称完整流程 PASS。

## 2026-09-20 新文件完整闭环

- 用户授权重启后端；标准重启脚本在数据库迁移前置校验处因既有 IDI 路由约束失败，未停止旧进程。按运行规则核对端口归属后，使用本次已构建并校验 SHA-256 的运行 Jar 启动 `int_main`，`48081` health 为 `UP`，前端 `8081` 正常。
- 运行 Jar：`output/runtime/int_main/backend-runtime-control-20260920-025034-dcc-version-fix.jar`；SHA-256：`B0FE6273E2DD995A2019B6E9E28A0CEB2E4C284BC77771834330C77378238281`。
- 真实页面维护项目模板并新建任务自有文件：`E2E-DCC-20260920093006` / `A/1`。
- 完成 A/1 第一次检出、撤销检出、第二次检出；随后上传并检入小版本，生成 `A/2 WORKING`。
- 发现并修复前端版本下拉遗漏当前行 WORKING 版本的问题：有历史版本时，`getVersionOptions` 原先只返回 `versionHistory`，导致页面无法选择 A/2/B/1 提交审批。修复为合并当前行版本与历史版本。
- 前端 RED/回归：首次最终化脚本页面下拉无 A/2 选项；GREEN：修复后真实页面出现 `A/2、A/1`，提交审批 POST HTTP `200`、业务码 `0`。静态契约 PASS；`NODE_OPTIONS=--max-old-space-size=8192 pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` PASS。
- A/2 审批中心真实完成 3 次评审和 1 次电子签名，全部 HTTP `200`、业务码 `0`，A/2 成为当前有效版本。
- A/2 大版本检出、上传新源文件、大版本检入全部 PASS，生成 `B/1 WORKING`；再经页面提交审批和 3 次评审/1 次签名后，B/1 成为当前有效版本。
- 最终只读页面复核：`B/1 ACTIVE`、`currentActiveVersionNo=B/1`、`checkedOutBy=null`；页面无控制台错误或页面错误结论，完整业务闭环 PASS。
- 主要证据：`dcc-checkin-checkout-full-20260920014457.json`、`dcc-checkin-checkout-full-20260920015511.json`、`approve-task-owned-file-20260920093012.json`、`dcc-checkin-checkout-full-20260920015814.json`、`dcc-checkin-checkout-full-20260920015912.json`、`approve-task-owned-file-20260920093013.json`、`dcc-checkin-checkout-full-20260920020207.json`。

## 当前收尾边界

- 任务已达到 `ready_for_closeout`；未执行 Git 提交/推送和 cleanup apply，因为本轮未获得相应授权。
