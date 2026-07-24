# Task: eDHR 批记录附加表单槽位

## Task Goal

在批记录模板页为每个批记录名称增加固定的损耗单、过程检验单、参数记录表 3 个附加表单槽位；槽位支持 Word 模板上传、解析成现有 eDHR 表单预览、删除和工艺路线工序按需绑定，不允许重复上传覆盖。

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；中文文件读写、命令输出和多行脚本均使用显式 UTF-8，禁止默认编码和 `&&`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；新增槽位保持蓝/中性色运营台风格，红边只用于未上传必需状态，不引入大面积重设计。
- 批记录 Word 表单识别：已读取 `docs/experience/batch-record-form-recognition.md`；解析逻辑必须基于 Word 表格结构、合并单元格和通用网格规则，不按文件名、产品名或样例文本硬编码。
- 后端接口交付：失败路径必须 fail fast；重复上传、绑定后删除、扩展名不支持、解析失败均返回明确错误，不允许 fallback、吞异常或默认成功。
- 数据库契约：本任务新增批记录报表槽位类型和工序附加表单绑定，必须补 SQL/表结构契约测试或等效门禁验证。
- 高风险动作：本任务本地代码与测试为主；真实 E2E 或大文件上传前需先记录 `GREEN: experience-preflight -> PASS` 或 blocker。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。无模板、重复上传、解析失败、删除受绑定保护均直接报错。
- `是否从根因和长期维护角度解决`：是。通过槽位类型元数据和工序绑定模型表达附加表单，而不是前端硬编码临时状态。
- `是否存在临时补丁或绕过`：否。

## Milestone List

1. completed：创建任务文档、记录 BDD 场景与门禁。
2. completed：新增后端槽位元数据、DOCX 解析器、上传/查询/删除接口和工序附加表单绑定。
3. completed：更新批记录模板页和工艺路线用途配置页。
4. completed：补充 RED/GREEN/REGRESSION 测试与前端类型检查。
5. completed：收尾清理预览已完成；提交因工作区存在历史重叠脏改而阻塞，未混入提交。

## Expected Verification

- `mvn.cmd -pl yudao-module-mes -Dtest=MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest test`
- `mvn.cmd -pl yudao-module-mes -Dtest=MesProRouteUseConfigControllerPermissionTest test`
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm.cmd ts:check`
- 前端静态/E2E 覆盖槽位渲染、上传后预览、重复上传阻止、删除、工序绑定 0-N 个附加表单。

## Current Status

Completed for scoped implementation and automated verification. Backend parser/service/controller/schema gates, frontend type check, and frontend static slot contracts passed. Real browser upload E2E is not executed in this pass because the current workspace contains broad pre-existing dirty changes and no running-state change is required for the scoped code verification. Git commit is blocked by pre-existing overlapping uncommitted changes in backend/frontend files, so no commit was created to avoid mixing unrelated work.
