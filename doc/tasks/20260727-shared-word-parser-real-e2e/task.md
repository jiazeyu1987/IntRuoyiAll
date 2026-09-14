# 共享 Word 解析真实数据 E2E 验证

## Task Goal

使用 `resource` 目录下的真实 Word 数据，通过真实前端页面验证表单模板 Word 导入与批记录 Word 导入路径，确认文档中设计的共享解析验证方案具备可执行的真实数据 E2E 证据。

## Milestones

- [x] 建立任务记录并读取 E2E、登录、本地运行态和命中经验门禁。
- [x] 盘点 `resource` 目录中的真实 Word 数据和可用于验证的导入入口。
- [x] 确认本机前端、后端、登录和测试数据前置条件。
- [x] 通过 Playwright 操作真实页面执行 Word 导入验证。
- [x] 记录 E2E 结果、证据、阻塞项和清理状态。

## Expected Verification

- Playwright 通过真实前端页面登录本机环境。
- Word 文件来自 `resource` 目录，且验证记录包含文件路径和只读/写入范围。
- 表单模板导入仍走 `/form-center/templates/import-doc`，批记录导入仍走 `/mes/pro/batch-record-report/recognize-uploaded` 或 `/upload-extra-slot`。
- 验证不得使用 mock、API-only 或直接 SQL 写入替代页面路径。

## Current Status

blocked

## Blockers

- MES 批记录主 Word 导入真实页面已完成预检，但当前 `球囊扩张压力泵` 最新批记录版本为 `V3.0 / PENDING_APPROVAL`，预检返回 `allowedActions=[]` 且“确定”按钮禁用，无法继续保存导入；不得用 API-only、直接 SQL 或切换 admin 基线绕过。

## Verification Result

- 表单中心真实 E2E：PASS；`resource/过程检验记录.docx` 通过 `/mdm/form-center/template` 页面导入，接口 `/admin-api/form-center/templates/import-doc` 返回 `templateId=30`、`versionNo=V1.0`、`recognizedFields=56`。
- MES 批记录真实 E2E：BLOCKED；`resource/批记录压力泵.doc` 通过 `/mes/pro/batch-record-form-list` 页面上传并触发 `/admin-api/mes/pro/batch-record-report/recognize-uploaded/preflight`，但业务预检锁定在审批中版本，未触发保存导入。
- Evidence: `doc/tasks/20260727-shared-word-parser-real-e2e/real-e2e-evidence.json`；截图位于 `doc/tasks/20260727-shared-word-parser-real-e2e/artifacts/`。

## Cleanup Keep

doc/tasks/20260727-shared-word-parser-real-e2e/shared-word-parser-real-e2e.js
doc/tasks/20260727-shared-word-parser-real-e2e/real-e2e-evidence.json
doc/tasks/20260727-shared-word-parser-real-e2e/artifacts/form-center-20260727-shared-word-parser-real-e2e.png
doc/tasks/20260727-shared-word-parser-real-e2e/artifacts/mes-preflight-20260727-shared-word-parser-real-e2e.png

## 经验门禁

### eDHR 批记录 Word 表格解析门禁

- Trigger: 本任务使用真实 Word 文件执行批记录 Word 导入、共享 parser 相关验证、packed 物料矩阵和说明块识别路径。
- Preflight check: 使用 `resource` 下真实 Word 文件，验证路径必须走真实前端页面和真实上传接口。
- Blocker: 缺少真实源 DOC、测试 fixture 不存在、登录/运行态不可用或页面入口缺失时必须阻塞，不得使用 mock、API-only 或截图替代。
- Verification: 记录真实 Word 文件、登录身份标签、前端入口、上传接口、响应结果、后续只读核验和失败证据。
- Forbidden action: 禁止按表单名、工序名、文件名或模板名写硬编码特例；禁止只靠截图人工判断完成。
- Evidence: `docs/backend-development.md#edhr-批记录-word-表格解析门禁`。

### 官方登录前置与 admin-only 全量验证门禁

- Trigger: 本任务执行真实 Playwright 登录和写入型 Word 导入验证。
- Preflight check: 优先使用本机真实前端登录；写入型验证使用测试租户/账号，不能在 admin 基线数据上创建业务写入。
- Blocker: 登录脚本缺失、验证码开启、测试租户/账号不可用或必须写入 admin 基线数据时阻塞。
- Verification: 记录前端 URL、后端 URL、租户/账号标签和真实页面断言。
- Forbidden action: 禁止用 API-only、默认成功、mock 或 admin 基线写入替代真实 E2E。
- Evidence: `docs/e2e-rules.md#官方登录前置与-admin-only-全量验证门禁`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；E2E 失败必须记录真实失败点，不切换 mock、API-only 或其它接口。
- `是否从根因和长期维护角度解决`：是；验证真实页面路径和真实 Word 数据是否支撑后续共享 parser 实现。
- `是否存在临时补丁或绕过`：否；本任务为验证任务，不修改生产代码。
