# 任务：排产工单追溯弹框可读性优化

## 任务目标

- 优化 `/mes/pro/schedule-order` 排产工单列表中“追溯”按钮打开的弹框可读性。
- 保持现有操作日志 API、字段差异解析和追溯入口不变，只改造弹框信息层级、展示密度、空状态和样式。
- 追溯弹框需要让用户先看到当前排产编码、日志数量、最近操作，再按时间线阅读操作记录和字段差异。

## 当前状态

COMPLETED

## Current Status

completed

## 上一任务检查

- 上一个前端排产工单任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260702-schedule-order-process-dialog-right-overflow\task.md`
- 状态：`COMPLETED`
- 本次只处理排产工单追溯弹框，不混入排产工单主表、工艺路线、冻结、重排或后端 API 改动。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- 适用强制门禁：
  - 保持 IntPP 运维台风格，使用白色工作面、浅灰蓝边框、蓝色重点信息、紧凑表格和稳定间距。
  - 追溯弹框属于详情面板，允许使用摘要条、时间线和二级表格，但不得做无关视觉重做。
  - PowerShell 读取和记录中文文件时必须显式使用 UTF-8。
  - 本轮先做静态契约和代码回归，不执行真实登录、写入、服务器操作或长链路 E2E，因此无需高风险 `experience-preflight`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。只改造前端展示结构和样式，不改变错误处理、不新增兜底数据源。
- `是否从根因和长期维护角度解决`：是。根因是追溯弹框信息层级单薄，用户必须展开表格才能理解操作上下文；本次用摘要、时间线和字段差异区解决阅读路径。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 追溯弹框先展示排产工单上下文 -> Given 用户点击排产工单行内追溯 / When 弹框打开 / Then 弹框顶部展示排产编码、日志数量、最近操作和最近时间，用户无需先读表格即可识别当前对象。`
- `BDD: 操作日志按时间线阅读 -> Given 追溯接口返回多条操作日志 / When 弹框渲染 / Then 每条日志以时间线卡片展示操作类型、操作人、原因和排产编码，字段差异放在当前操作卡片内。`
- `BDD: 字段差异值长文本可读 -> Given 字段差异包含长备注、JSON 或日期 / When 用户展开追溯记录 / Then 新旧值使用可换行的等宽值块展示，不依赖单行 tooltip。`

## 里程碑

1. M1：建立任务台账并补充追溯弹框可读性 RED 静态回归。`COMPLETED`
2. M2：最小改造追溯弹框摘要、时间线、差异值展示和空状态。`COMPLETED`
3. M3：运行 GREEN 静态回归与前端契约验证。`COMPLETED`
4. M4：回写执行证据、命令记录和收尾预览。`COMPLETED`

## 预期验证

- `node tests/e2e/mes-schedule-order-trace-dialog-readability-static.spec.js`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260703-schedule-order-trace-dialog-readability/frontend-feature-evidence.md`

## 最终验证结果

- `node tests/e2e/mes-schedule-order-trace-dialog-readability-static.spec.js` -> PASS
- `node tests/e2e/mes-schedule-order-route-progress-dialog-width-static.spec.js; node tests/e2e/mes-schedule-order-trace-dialog-readability-static.spec.js` -> PASS
