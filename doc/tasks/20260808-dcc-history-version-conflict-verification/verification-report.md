# DCC 历史文件升版状态矛盾核验报告

## Outcome

- **PARTIAL / BLOCKED**：当前本机 `int_main` 可确认多项配置与预检问题，但无法完整复现用户描述的“选择唯一历史文件后”闭环，因为目标历史文件在本机 `tenant_id=1 / zhaohaichen` 当前数据中不可选。
- **Scope**：真实页面只读复核 `/dcc/controlled-file/upload`；未点击“提交审批”，未上传文件，未发送 DCC 写请求。
- **Evidence**：`playwright-verification-result.json`、`upload-page-history-conflict.png`；脚本 `verify-dcc-history-conflict.cjs`。

## Findings

| Item | Result | Evidence |
| --- | --- | --- |
| 目标账号和页面 | CONFIRMED | `芋道源码/zhaohaichen` 登录并进入 `/dcc/controlled-file/upload`；后端 health `UP`，前端 HTTP 200；DCC 写请求 0。 |
| 目标 DCC 项目 | CONFIRMED | API 解析到 `按压式球囊扩充压力泵 / IDI / 1`，`id=129`；页面选择后产品编号自动回填 `IDI`。 |
| 目标三级分类 | CONFIRMED | `技术文档 / 设计和开发策划阶段 / 技术调研报告` 解析到 taxonomy `id=139`。 |
| 目标历史文件候选 | BLOCKED / NOT PRESENT LOCALLY | `upload-name-options.count=0`、`revisionCandidates.total=0`、项目关联文件搜索 `0`、受控浏览搜索 `0`；本机无法选择“按压式球囊扩充压力泵技术调研报告.pdf”。 |
| “升版 + 新建 master + 可提交 + 英文冲突”同屏闭环 | BLOCKED LOCALLY / CODE RISK CONFIRMED | 由于本机缺历史候选，无法真实页面完整复现；但前端模板允许历史选择提示“将按升版提交”同时在 `currentVersionInfo.matched=false` 时显示“将创建新的 master 主档”，预检仅按非空/重复/流程中判断“可提交”。 |
| 后端英文冲突错误路径 | CONFIRMED IN CODE | `getCurrentVersion` 与 `loadOrCreateMaster`/`validateChangeTypeAgainstCurrentVersion` 在多 master、无当前 active、NEW 命中现行链等场景抛 `CONTROLLED_FILE_FILE_NUMBER_CONFLICT`，错误文案为英文。 |
| DHF文件清单无正式 DCC 类别 | CONFIRMED | `技术文档 / 清单 / DHF文件清单` taxonomy `id=137`，active category count `0`。 |
| 市场调研报告多正式 DCC 类别 | CONFIRMED | taxonomy `id=138` 绑定 2 个 active 类别：`市场调研报告`、`风险管理计划`。 |
| 技术调研报告未绑定提交目录并落到未分类 | CONFIRMED | 类别 `DCC_FVM_DHF_002 / 技术调研报告` 的 `directoryId=null`；上传目录接口返回 `bindingDirectoryPath=未分类`、`defaultUnclassified=true`；页面预检显示“受控浏览目录可落位”。 |
| 审批链路数量 | CONFIRMED | 技术调研报告：审批岗位 2 个、会签/签核岗位 5 个。 |
| `abc` 版本 + 过去生效日期仍可提交 | CONFIRMED | 新编号下填写版本 `abc` 和生效日期 `2026-08-07` 后，“文件编号/版本”预检仍显示“可提交”。 |
| 版本格式只在后端提交时校验 | CONFIRMED IN CODE | 后端 `parseVersion` 无法解析时抛 `CONTROLLED_FILE_VERSION_INVALID`；前端表单规则仅要求 `versionNo` 非空。 |
| 会签人员弹窗列 | CONFIRMED | UI 表头为用户编号、用户名称、用户昵称、部门、手机号码、状态、创建时间。 |
| 会签人员分页 | PARTIALLY CONFIRMED / COUNT DRIFT | 当前本机为 `Total 2125`、`20/page`、107 页；与用户记录的 2148 / 108 页不一致，说明数据已漂移或环境不同。 |
| 会签筛选 | MOSTLY CONFIRMED | `username=zhaohaichen` 返回 1 条；不存在值返回 0；禁用状态返回 0；取消弹窗后输入框为空。若用宽泛关键词 `zhao`，当前返回 56 条，不是 1 条。 |
| `smokeplan1` 昵称乱码 | CONFIRMED | `smokeplan1` 当前启用，nickname 为 `???????`；同页还存在多个 `???????` 昵称。 |
| 附件入口 | CONFIRMED | 两个文件输入均为单文件：受控文件 accept Office/图纸源文件，图纸 PDF accept PDF。 |
| 培训要求开关 | CONFIRMED | 开关可切换：`el-switch` -> `el-switch is-checked` -> `el-switch`。 |

## Source Evidence

- Frontend UI contradiction: `IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue` lines 180-181, 191-235, 1203-1235, 1344-1354.
- Frontend validation gap: `IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue` lines 733-777 only require version/effective date presence; preflight uses non-empty file number/version.
- Submit error display: `IntRuoyiFronted/src/views/dcc/controlled-file/upload/submitter.ts` lines 181-188 returns raw error message; lines 267-275 only attach field error when message contains version/版本.
- Backend current-version and conflict: `DccControlledFileWorkflowServiceImpl.java` lines 219-238, 1294-1316, 1327-1380.
- Backend version parsing: `DccControlledFileWorkflowServiceImpl.java` lines 1541-1572.

## Final Assessment

- The **data/configuration issues are real** in the current local runtime: DHF missing category, market research duplicate category binding, technical research auto-unclassified landing, invalid version/past date preflight gap, and `smokeplan1` nickname corruption.
- The **exact historical-file contradiction is not locally reproducible** because the prerequisite history file is missing from the current local data under the specified project/classification. Static code still confirms the UI/backend design can produce the reported contradiction when that data exists.
- To confirm the user’s reported release environment exactly, the next step is an explicitly authorized test-server read-only Playwright run against `release-20260808-intmain-head-test-r260808a-r1`.
