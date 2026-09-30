# DCC 检入检出完整 E2E 验证报告

## Current Status
ready_for_closeout

## 环境

- 前端：`http://127.0.0.1:8081`
- 后端：`http://127.0.0.1:48081`
- 后端 health：HTTP `200`，`UP`
- 分支：`int_main`
- 运行 Jar：`output\runtime\int_main\backend-runtime-control-20260920-025034-dcc-version-fix.jar`
- 身份标签：`芋道源码/admin`
- 目标文件：`E2E-DCC-20260920093006` / `CODEX-DCC-E2E-TEMPLATE-20260920093005.docx`

## 验收结果

### 最终换文件闭环

| 业务阶段 | 结果 | 页面/自然请求证据 |
| --- | --- | --- |
| 真实登录 | PASS | 登录 POST HTTP `200`，业务码 `0` |
| A/1 第一次检出 | PASS | `checkout` HTTP `200`，业务码 `0`，`checkedOutBy=1` |
| A/1 撤销检出 | PASS | `checkout/cancel` HTTP `200`，业务码 `0`，锁恢复为空 |
| A/1 第二次检出 | PASS | `checkout` HTTP `200`，业务码 `0` |
| A/2 小版本上传与检入 | PASS | `upload-preview` 与 `checkin` 均 HTTP `200`，生成 `A/2 WORKING` |
| A/2 提交审批 | PASS | 页面切换版本下拉选择 A/2 后提交，`/2054545668044084005/submit` HTTP `200`，业务码 `0` |
| A/2 审批 | PASS | 审批中心真实完成 3 次评审和 1 次电子签名，均 HTTP `200`，业务码 `0` |
| A/2 大版本检出 | PASS | `checkout` HTTP `200`，业务码 `0` |
| B/1 大版本上传与检入 | PASS | `upload-preview` 与 `checkin` 均 HTTP `200`，生成 `B/1 WORKING` |
| B/1 提交审批 | PASS | 页面选择 B/1 后提交，`/2054545668044084006/submit` HTTP `200`，业务码 `0` |
| B/1 审批 | PASS | 审批中心真实完成 3 次评审和 1 次电子签名，均 HTTP `200`，业务码 `0` |
| 最终有效版本复核 | PASS | 页面只读复核显示 `B/1 ACTIVE`、`currentActiveVersionNo=B/1`，无当前用户检出锁 |

最终业务结论：完整检入检出和审批闭环 PASS。

证据：

- [创建文件](C:/IntRuoyiAll-int_main/doc/tasks/20260919-dcc-checkin-checkout-e2e/e2e-artifacts/create-task-owned-file-20260920093006.json)
- [A/1 首次完整链路](C:/IntRuoyiAll-int_main/doc/tasks/20260919-dcc-checkin-checkout-e2e/e2e-artifacts/dcc-checkin-checkout-full-20260920014457.json)
- [A/2 提交审批](C:/IntRuoyiAll-int_main/doc/tasks/20260919-dcc-checkin-checkout-e2e/e2e-artifacts/dcc-checkin-checkout-full-20260920015511.json)
- [A/2 审批](C:/IntRuoyiAll-int_main/doc/tasks/20260919-dcc-checkin-checkout-e2e/e2e-artifacts/approve-task-owned-file-20260920093012.json)
- [B/1 大版本检出检入](C:/IntRuoyiAll-int_main/doc/tasks/20260919-dcc-checkin-checkout-e2e/e2e-artifacts/dcc-checkin-checkout-full-20260920015814.json)
- [B/1 提交审批](C:/IntRuoyiAll-int_main/doc/tasks/20260919-dcc-checkin-checkout-e2e/e2e-artifacts/dcc-checkin-checkout-full-20260920015912.json)
- [B/1 审批](C:/IntRuoyiAll-int_main/doc/tasks/20260919-dcc-checkin-checkout-e2e/e2e-artifacts/approve-task-owned-file-20260920093013.json)
- [最终页面复核](C:/IntRuoyiAll-int_main/doc/tasks/20260919-dcc-checkin-checkout-e2e/e2e-artifacts/dcc-checkin-checkout-full-20260920020207.json)

所有验收写入均由 Playwright 真实页面完成；API 记录仅来自页面自然请求，未使用 API、APIRequest、数据库写入或 mock 承担业务动作。

| 业务阶段 | 结果 | 页面/自然请求证据 |
| --- | --- | --- |
| 真实登录 | PASS | 登录 POST HTTP `200`，业务码 `0` |
| 目标文件发现 | PASS | 页面显示 `C/1 ACTIVE`、真实“检出”按钮、允许 `MAJOR_REVISION` |
| 第一次检出 | PASS | POST `/checkout` HTTP `200`、业务码 `0`，返回 `checkedOutBy=1` |
| 检出后按钮状态 | PASS | 页面显示“检入”“撤销检出”及当前用户检出提示 |
| 撤销检出 | PASS | POST `/checkout/cancel` HTTP `200`、业务码 `0`，返回 `checkedOutBy=null` |
| 撤销后按钮状态 | PASS | 页面恢复“检出”，无“检入/撤销检出” |
| 第二次检出 | PASS | POST `/checkout` HTTP `200`、业务码 `0` |
| 小版本源文件上传 | PASS | 页面上传控件完成上传，`upload-preview` HTTP `200`、业务码 `0` |
| 小版本检入 | PASS | POST `/checkin` HTTP `200`、业务码 `0`，生成 `C/2 WORKING` |
| 小版本后的锁释放 | PASS | 返回对象 `checkedOutBy=null`，最终页面无当前用户检入/撤销检出 |
| WORKING 提交审批 | PASS | 页面切换到 `C/2` 后提交审批，POST `/2054545668044083988/submit` HTTP `200`、业务码 `0` |
| 大版本检入 | BLOCKED | 从 `C/1` 再检出返回业务码 `1080000177`：同文件已有未完成工作流 |

## 关键业务事实

- 小版本检入没有直接变成 `ACTIVE`，而是生成 `C/2 WORKING`；当前有效版本仍是 `C/1`。
- 页面通过“提交审批”完成 `C/2` 的后续入口，最终显示 `C/1 ACTIVE / 修改中`。
- 大版本继续动作被后端正式未完成工作流门禁阻止，未使用 API、数据库或其它文件绕过。
- 本轮业务写入均来自真实页面点击和上传控件；没有使用 `fetch`、`apiGet`、APIRequest、数据库写入或 mock。

## 阻断结论

本次已真实完成检出、撤销检出、再次检出、小版本检入和 WORKING 提交审批，检入锁释放也已通过页面刷新确认。由于 `C/2` 审批尚未完成，系统按正式规则禁止继续从 `C/1` 检出以开展大版本检入，故“完整大小版本闭环”结论为 `BLOCKED`，不是 PASS。

继续完成大版本需要先由有权账号通过真实审批页面完成 `C/2` 工作流，再从真实受控浏览页面重新检出 `C/1` 或按页面提供的正式升大版本入口继续；不能用接口或数据库直接改变状态。

## 证据文件

- [Playwright 脚本](C:/IntRuoyiAll-int_main/doc/tasks/20260919-dcc-checkin-checkout-e2e/dcc-checkin-checkout-full.e2e.cjs)
- [主要链路结果](C:/IntRuoyiAll-int_main/doc/tasks/20260919-dcc-checkin-checkout-e2e/e2e-artifacts/dcc-checkin-checkout-full-20260919075712.json)
- [WORKING 提交审批结果](C:/IntRuoyiAll-int_main/doc/tasks/20260919-dcc-checkin-checkout-e2e/e2e-artifacts/dcc-checkin-checkout-full-20260919080231.json)
- [最终页面复核结果](C:/IntRuoyiAll-int_main/doc/tasks/20260919-dcc-checkin-checkout-e2e/e2e-artifacts/dcc-checkin-checkout-full-20260919080336.json)
- 对应 `trace.zip` 和逐步截图位于同一 `e2e-artifacts` 目录。

## 换文件继续结果

用户要求换文件后，已通过真实页面准备第二条任务自有数据：

| 阶段 | 结果 | 证据 |
| --- | --- | --- |
| 新建产品/项目 | PASS | 真实页面 `产品建档申请` 创建并审批项目 `CODEX-DCC-PROD-20260919084653`，项目 ID `258` |
| 配置模板/权限 | PASS | 真实页面保存 `admin` 权限与模板 `CODEX-DCC-MAJOR-20260919084750.docx` |
| 创建新受控文件 | PASS | 真实页面生成 `E2E-DCC-20260919084910` / `A/1`，`upload-preview` 与 `submit` 均成功 |
| 新文件审批待办 | PASS | 审批中心真实显示 `E2E-DCC-20260919084910` 文控审核待办，任务 ID `fc738b6a-b406-11f1-bd27-b082e25ec548` |
| 新文件审批动作 | BLOCKED | 真实页面“审核”提交后，后端在签名证据生成阶段读取源文件对象返回 S3 `NoSuchKey` |
| 同名上传路径修复 | PASS | 后端 `FileServiceImpl` 默认恢复唯一后缀目录；`FileServiceImplTest` 定向单测 38/38 PASS |
| 新文件检出 | BLOCKED | 当前运行后端仍是旧 Jar，未重启/换包前无法继续把新文件审批至 `ACTIVE` 可检出状态 |
| 其它任务自有候选 | BLOCKED | 排除既有阻塞文件和新建审批中文件后，`E2E` 范围没有其它 `admin` 可检出/检入行 |

新增证据文件：

- [新受控文件创建结果](C:/IntRuoyiAll-int_main/doc/tasks/20260919-dcc-checkin-checkout-e2e/e2e-artifacts/create-task-owned-file-20260919084910.json)
- [新文件审批中心检查](C:/IntRuoyiAll-int_main/doc/tasks/20260919-dcc-checkin-checkout-e2e/e2e-artifacts/approval-center-diagnostic-20260919092710.json)
- [新文件审核 500 结果](C:/IntRuoyiAll-int_main/doc/tasks/20260919-dcc-checkin-checkout-e2e/e2e-artifacts/approve-task-owned-file-20260919102130.json)
- [新文件受控浏览检索](C:/IntRuoyiAll-int_main/doc/tasks/20260919-dcc-checkin-checkout-e2e/e2e-artifacts/dcc-checkin-checkout-full-20260919085020.json)
- [排除后 E2E 候选检索](C:/IntRuoyiAll-int_main/doc/tasks/20260919-dcc-checkin-checkout-e2e/e2e-artifacts/dcc-checkin-checkout-full-20260919090827.json)

最终结论不变：本轮不能宣称完整 E2E PASS。第一条文件已完成检出/撤销/检入小版本并卡在未完成工作流；第二条文件已真实创建且 `admin` 有审批待办，但当前运行后端用旧上传路径策略生成的源文件对象不可读，真实审批停在签名证据读取 S3 `NoSuchKey`。代码修复和单测已完成，继续 E2E 需要先授权重启/替换运行包后重新创建或审批新任务自有文件。

## 继续修复：版本链收口

### 根因与修复

`E2E-DCC-20260920005230` 的 `A/2` 已经在真实审批页面完成最终动作并显示 `ACTIVE`，但版本链中仍有旧的非终态迭代。最终发布服务原先只收口 `WORKING` 版本，因此查询服务仍判定文件“修改中”，检出服务返回 `1080000177`。

修复为：候选版本完成最终发布时，将同一主链上版本号更低且仍处于非终态的遗留迭代统一标记为 `SUPERSEDED`。没有放宽检出门禁，也没有通过 API、数据库写入或迁移脚本修正 E2E 数据。

### RED/GREEN 与构建证据

- RED：遗留状态改为 `PENDING_DOC_CONTROL_REVIEW` 后，原实现未更新该记录，定向测试失败。
- GREEN：修复后 `DccControlledFileFinalizationServiceImplTest` 46/46 PASS。
- 查询回归：`DccControlledFileQueryServiceTest` 149/149 PASS。
- 模块构建：`yudao-module-dcc -DskipTests package` PASS。
- Playwright 脚本语法检查：两个任务脚本 `node --check` PASS。

### 当前验收边界

以上证明生产代码和定向回归已通过，但当前 `48081` 仍由其它任务自有旧 Jar 进程占用，按规则未停止或重启 `int_main` 后端。因此尚未用包含本修复的新 Jar 重新执行 `E2E-DCC-20260920005230` 的真实页面检出回归，任务仍为 `blocked`，不能宣称完整检入检出 E2E PASS。
