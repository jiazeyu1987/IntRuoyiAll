# 任务：修复 ERP 测试单重复校验字段兼容

- Task ID: `20260630-erp-test-order-duplicate-check-field-compat`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `completed`
- User Request: `提交前后端代码`

## Task Goal

修复金蝶 `PRD_MO` 生产订单查询在测试单重复校验与按单号查询场景下依赖不兼容字段的问题：重复校验与单号查单只请求当前正式环境稳定存在的字段；若金蝶返回嵌套错误对象，必须 fail fast 暴露真实错误，不得把元数据错误误判成“查无此单”或通用数组格式异常。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-schedule-calendar-capacity-horizon-renewal\task.md`
- 状态：`blocked`
- 处理说明：已按当前用户优先级切换显式阻塞，保留既有分析证据，不阻塞本次 ERP 客户端兼容修复提交。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 本次命中 `docs\powershell-memory.md` 与 `docs\integrations\kingdee-erp-official-docs.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - PowerShell 读取中文任务文档、日志与命令输出必须显式 UTF-8；命令串联不使用 `&&`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\integrations\kingdee-erp-official-docs.md`
  - 金蝶字段兼容修复必须以当前 `PRD_MO` 正式查询链路和真实错误响应为准，不臆造字段可用性，也不在接口失败时静默回退。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。当前修复通过缩小查询字段集合与暴露真实金蝶错误解决兼容问题，不增加任何兜底或吞异常。
- `是否从根因和长期维护角度解决`：是。按查询场景拆分稳定字段集合，避免单号查单/重复校验继续依赖增量同步才需要的扩展字段。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: ERP 测试单重复校验只请求稳定字段 -> Given 创建 ERP 测试单前需要按单号检查 PRD_MO 是否已存在 / When 后端执行重复校验查询 / Then 请求字段只包含 FBillNo，不再携带 FIssueType 等环境不兼容扩展字段。`
- `BDD: 单号查单只请求当前场景必需字段 -> Given 后端需要按 billNo 查询生产订单基础信息 / When 调用 ExecuteBillQuery / Then 请求字段只包含查单必需字段，不依赖冲领料、助记码、业务状态、图号、排产状态等扩展字段。`
- `BDD: 金蝶返回嵌套元数据错误时必须暴露真实原因 -> Given 金蝶 ExecuteBillQuery 返回数组包裹的 ResponseStatus 错误对象 / When 后端解析查询结果 / Then 直接抛出包含真实错误消息的异常，而不是继续按成功数组解析。`

## Milestones

1. M1：阻塞上一后端任务并建立本次任务文档。`completed`
2. M2：核对当前未提交 ERP 客户端改动与既有正式修复范围。`completed`
3. M3：运行定向测试验证字段兼容修复。`completed`
4. M4：执行 closeout 预览并整理提交边界。`completed`
5. M5：提交本次后端改动。`completed`

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-erp "-Dtest=ErpKingdeeProductionOrderClientImplTest" -Dsurefire.failIfNoSpecifiedTests=false test`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260630-erp-test-order-duplicate-check-field-compat --mode preview`

## Current Blockers

- 无。

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-erp "-Dtest=ErpKingdeeProductionOrderClientImplTest" -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260630-erp-test-order-duplicate-check-field-compat --mode preview` -> PASS
