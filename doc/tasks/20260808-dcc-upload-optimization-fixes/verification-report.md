# DCC 文件上传优化修复验证报告

## Outcome

DCC 任务范围 PASS：已按优化文档完成前端修复，并在真实 E2E 中验证“按文件编号查到现行版本后自动升版”。继续复跑时修正了真实 E2E 脚本响应监听顺序，并用静态合同锁定；通用 current-version 真实 E2E 已 PASS 且 DCC 写请求数为 0。精确 `zhaohaichen / IDI / 技术调研报告 / 按压式球囊扩充压力泵技术调研报告.pdf V1.0` 路径因本机缺少目标历史文件选项记录为 BLOCKED。

## 修复内容

1. 历史文件升版状态互斥：选择历史文件后不再无条件提示“将按升版提交”，而是提示系统会先匹配现行主档；找不到现行主档时显示阻断错误，并明确“不会创建新的 master 主档”。
2. 编号冲突预检阻断：现行版本查询返回编号链冲突时，页面记录 `currentVersionLookupError`，文件编号/版本预检显示“需处理”，提交前也会阻断。
3. 英文错误中文化：提交失败和现行版本查询失败会把 `Controlled file number conflicts with the existing logical document chain` 映射成中文编号链冲突提示。
4. 版本格式前端校验：版本号前端规则对齐后端解析器，允许 `V1.0`、`V2.0`、`1.0`、多段数字版本，非法值如 `abc` 会在前端阻断。
5. 生效日期规则明确：当前后端没有“不允许过去日期”的规则，本任务按允许补录处理；过去日期会在预检中显示“允许补录历史生效日期”。
6. 未分类允许发布：按用户确认保留未分类发布，文案改为“该类别未配置专属目录，按规则发布到‘未分类’。”

## Verification

- RED: `pnpm --dir IntRuoyiFronted e2e:dcc:upload-optimization:static` -> FAIL，首个失败为未分类提示仍是旧自动兜底文案。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:dcc:upload-optimization:static` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:dcc:upload-current-version:static` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:dcc:upload-project-taxonomy-revision:static` -> PASS。
- FINAL GREEN: `pnpm --dir IntRuoyiFronted e2e:dcc:upload-optimization:static` -> PASS。
- FINAL GREEN: `pnpm --dir IntRuoyiFronted e2e:dcc:upload-current-version:static` -> PASS。
- FINAL GREEN: `pnpm --dir IntRuoyiFronted e2e:dcc:upload-project-taxonomy-revision:static` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:dcc:upload-category-taxonomy-binding:static` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:dcc:upload-governance-ux:static` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:dcc:upload-name-version-autofill:static` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:dcc:upload-layout:static` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted ts:check` -> PASS。
- FINAL EXTERNAL BLOCKER: `pnpm --dir IntRuoyiFronted ts:check` -> FAIL，当前失败点为 `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue(5800,7)` 和 `(5806,7)` 找不到 `ensureSubmissionDateCondition`；该文件是本任务外改动，本任务未修改。
- GREEN: `git diff --check -- <task-owned files>` -> PASS；仅有 Git 换行提示，无空白错误。
- GREEN: `validate_bug_regression.py --evidence doc\tasks\20260808-dcc-upload-optimization-fixes\bug-regression-evidence.md` -> PASS。
- GREEN: `validate_frontend_feature.py --evidence doc\tasks\20260808-dcc-upload-optimization-fixes\frontend-feature-evidence.md` -> PASS。
- GREEN: `task_closeout.py --task-id 20260808-dcc-upload-optimization-fixes --mode preview` -> PASS，无 blocked/warnings。
- GREEN: `task_closeout.py --task-id 20260808-dcc-upload-optimization-fixes --mode apply` -> PASS，无文件删除。
- GREEN: UTF-8 复读任务文档、经验索引和前端经验文档 -> PASS。
- CONTINUE RED: `pnpm --dir IntRuoyiFronted e2e:dcc:upload-current-version:real` -> FAIL，真实 E2E 脚本先输入文件编号再注册 `current-version` 响应监听，导致漏听响应；未产生 DCC 写请求。
- CONTINUE GREEN: `pnpm --dir IntRuoyiFronted e2e:dcc:upload-current-version:static` -> PASS，新增断言锁定“先监听再输入”。
- CONTINUE GREEN: `pnpm --dir IntRuoyiFronted e2e:dcc:upload-optimization:static` -> PASS。
- CONTINUE GREEN: `pnpm --dir IntRuoyiFronted e2e:dcc:upload-project-taxonomy-revision:static` -> PASS。
- CONTINUE REAL GREEN: `pnpm --dir IntRuoyiFronted e2e:dcc:upload-current-version:real` -> PASS，`writeRequests=[]`。
- CONTINUE REAL BLOCKED: `node doc\tasks\20260808-dcc-upload-optimization-fixes\dcc-upload-history-revision-readonly.e2e.js` -> BLOCKED，本机目标项目和分类存在，但目标历史文件选项数为 0；`dccWriteRequests=[]`。
- CONTINUE GREEN: `pnpm --dir IntRuoyiFronted ts:check` -> PASS。

## Closeout

- 任务状态已更新为 `completed`。
- 长期经验已沉淀到 `docs/frontend-development.md#DCC 上传历史文件升版状态门禁`，并在 `docs/experience-index.md` 增加关键词路由；本轮 E2E 监听顺序经验已合并到 `docs/e2e-rules.md#Playwright 登录重定向与目标接口监听门禁`。
- 本轮继续复跑 `ts:check` 已 PASS；DCC 静态合同与通用真实 E2E 已通过。
- 精确历史文件 E2E 仍需当前 release/test-server 数据或在本机补齐目标历史文件后复验。
- 未执行 Git 提交、合并或推送；当前项目规则允许在用户未要求 Git 操作时不提交。

- REAL RED: `pnpm --dir IntRuoyiFronted e2e:dcc:upload-current-version:real` -> FAIL，文件编号命中现行版本后未显示升版状态。
- REAL GREEN: `pnpm --dir IntRuoyiFronted e2e:dcc:upload-current-version:real` -> PASS，页面显示“当前变更方式：升版”，`writeRequests=[]`。
- REAL BLOCKED: `node doc\tasks\20260808-dcc-upload-optimization-fixes\dcc-upload-history-revision-readonly.e2e.js` -> BLOCKED，本机目标项目和分类存在，但目标历史文件选项数为 0；未点击提交，`dccWriteRequests=[]`。

## Not Changed

- 未修改后端错误码、版本链服务、目录落位服务或数据库 seed。
- 未修复 DHF文件清单无正式类别、市场调研报告多正式类别、`smokeplan1` 昵称乱码；这些属于配置/数据治理项，需要独立授权和可写测试数据。
- 未执行真实页面写入 E2E；本任务没有提交审批、上传业务文件或操作远端服务器。
- 本轮新增的真实 E2E 均为只读验证；通用 current-version E2E `writeRequests=[]`，精确历史文件 E2E `dccWriteRequests=[]`。

## Residual Risk

当前本机数据仍缺少用户最初描述的唯一历史文件候选：项目 `按压式球囊扩充压力泵 / IDI / 1` 与分类 `技术文档 / 设计和开发策划阶段 / 技术调研报告` 可解析，但 `upload-name-options` 返回 0 个历史文件选项，因此无法在本机完整选择 `按压式球囊扩充压力泵技术调研报告.pdf V1.0`。若需要验证当前 release/test-server 数据，需要另行授权目标环境、账号和只读/写入边界后执行 Playwright 真实路径。
