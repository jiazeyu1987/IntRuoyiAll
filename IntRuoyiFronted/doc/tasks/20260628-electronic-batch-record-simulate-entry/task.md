# 任务：电子批记录接入模拟填写入口

## 任务目标

- 在电子批记录页面右侧表单模板操作区增加 `模拟填写` 入口。
- 点击后进入系统现有的“左边模拟填写、右边显示填写结果”的页面，不重新开发新页面。
- 复用现有 eDHR 模板模拟填写页，并支持从电子批记录模板管理页按 `reportId` 直接打开。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-electronic-batch-record-preview-scroll-fix\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务修复了右侧预览滚动，本次继续在同一页面补入口，不回退其结果。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- 适用强制门禁：
  - 沿用现有紧凑操作区样式，只增加必要入口，不做无关改版。
  - PowerShell 命令和中文文件统一按 UTF-8 处理。
  - 本轮仅做本机静态验证，不触发真实 Playwright、登录写入、服务器操作或其他高风险动作，因此无需 `experience-preflight`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接复用现有模拟填写页，只扩展其输入参数来源。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 表单模板区显示模拟填写入口 -> Given 用户在电子批记录页面已选中某个报表 / When 查看右侧表单模板操作区 / Then 操作区显示 模拟填写 按钮。`
- `BDD: 从电子批记录页跳转现有模拟页 -> Given 用户在电子批记录页面点击 模拟填写 / When 跳转页面 / Then 系统进入现有 eDHR 模板模拟填写页，而不是新页面。`
- `BDD: 现有模拟页支持 reportId 直达 -> Given 模拟填写页收到 reportId 查询参数 / When 页面加载 / Then 直接按该 reportId 加载模板规则、签名位、左侧模板内填写和右侧表单显示。`

## 里程碑

1. M1：补任务文档、命令记录和静态 RED 契约。
2. M2：当前电子批记录页增加入口，并扩展现有模拟页支持 `reportId` 直达。
3. M3：运行定向静态回归并回写结果。

## 最终验证结果

- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS
- `node tests/e2e/edhr-batch-template-simulate-static.spec.js` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-electronic-batch-record-simulate-entry\frontend-feature-evidence.md` -> PASS

## 完成记录

- 电子批记录右侧表单模板操作区已新增 `模拟填写` 入口。
- 入口直接跳转现有 `/mes/pro/feedback/edhr-batch-execution/template-simulate` 页面。
- 现有模拟填写页已支持按 `reportId`、`reportName`、`batchRecordName` 直接打开，无需依赖批次执行 `id + taskId`。

## 预期验证

- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js`
- `node tests/e2e/edhr-batch-template-simulate-static.spec.js`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-electronic-batch-record-simulate-entry\frontend-feature-evidence.md`

## Current Status

completed
