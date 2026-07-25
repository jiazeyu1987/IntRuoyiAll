# 20260726 Batch Record Import V14

## Task Goal

通过本机真实前后端页面，在批记录表单页签导入 `C:\Users\BJB110\Desktop\文档\批记录压力泵.doc`，使用新的 Word 表格识别逻辑解析并生成 V14 版本。

## Milestones

- [x] 建立任务记录并读取运行、登录、E2E、数据库和编码门禁。
- [x] 确认本机前后端运行态与目标页面入口。
- [x] 通过真实前端页面上传 Word 并触发解析。
- [x] 核验生成的批记录版本为 V14，且使用新的识别结果。
- [x] 记录截图、接口/数据库核验证据和收尾状态。

## Expected Verification

- 前端入口 `http://127.0.0.1:8081` 可访问，后端 `http://127.0.0.1:48081/actuator/health` 为 `UP`。
- Playwright 通过真实页面完成登录、进入批记录表单页签、上传指定 Word、触发解析生成版本。
- 页面或只读数据库/API 核验存在目标批记录 V14 版本。
- 记录页面截图、目标版本号、源文件路径和最终状态。

## Current Status

completed

## Verification Result

- PASS：本机前端 `http://127.0.0.1:8081` 可访问，后端 `http://127.0.0.1:48081/actuator/health` 返回 `{"status":"UP"}`。
- PASS：通过真实页面 `MES 系统 / eDHR批记录 / 批记录表单` 上传 `C:\Users\BJB110\Desktop\文档\批记录压力泵.doc`，精确选择产品名称 `球囊扩张压力泵`，生成批记录版本 `V14.0`。
- PASS：只读页面会话 API 核验 `球囊扩张压力泵 / V14.0` 返回 15 条批记录表单，状态为 `PENDING_APPROVAL`。
- Evidence：截图 `doc/tasks/20260726-batch-record-import-v14/artifacts/04-v14-final-verification.png`；只读核验 `doc/tasks/20260726-batch-record-import-v14/artifacts/v14-readonly-verification.json`。
- Note：首次自动化用模糊关键词 `压力泵` 命中了相邻产品 `按压式球囊扩充压力泵`，预检目标为 `V4.0`；后续已改为全局精确匹配项目名称，不使用表单特例，并以 `球囊扩张压力泵` 完成 V14 验证。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；导入失败、运行态缺失或权限缺失时直接阻塞并记录。
- `是否从根因和长期维护角度解决`：是；本任务只执行真实页面导入验证，不绕过前后端。
- `是否存在临时补丁或绕过`：否；禁止 API-only 或 SQL 直塞替代页面导入。

## 经验门禁

- Trigger: 批记录 Word 导入、版本生成、真实页面上传、V14 核验。
- Preflight check: 读取 `docs/backend-development.md#eDHR 批记录 Word 表格解析门禁`、`docs/e2e-rules.md`、`docs/local-runtime.md`、`docs/login-access.md`、`docs/database-rules.md`。
- Blocker: 本机前后端未运行、源 DOC 缺失、登录失败、页面入口缺失、目标租户/账号不适合写入或生成版本不是 V14 时停止。
- Verification: 真实页面截图 + 只读 API/DB 核验版本号和导入结果。
- Forbidden action: 禁止用表单名特例、API-only、SQL 直塞、mock 上传或改库伪造 V14。
