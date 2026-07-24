# 任务：电子批记录页面去除外层卡片

## 任务目标

- 去掉电子批记录三栏页面最外层 `ContentWrap` 卡片壳，避免左上角出现双层外部卡片边框。
- 保留内部三栏区域和每个子面板卡片，不调整已有批记录、报表、模板预览交互。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260627-showroom-hall-bu-canvas-layout\task.md`
- 状态：`已完成`
- 处理说明：本次任务仅修改电子批记录页面的页面外壳样式，不影响上一任务交付。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- 适用强制门禁：
  - 前端页面延续 IntPP 运维台风格，不新增装饰性卡片，不引入新的嵌套卡片层级。
  - 本次涉及 PowerShell 文件读取和命令记录，中文文件统一按 UTF-8 读写。
  - 本轮仅做本机静态验证，不触发真实 E2E、服务器写入、发布、恢复或高风险动作，因此无需 `experience-preflight`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，通过移除页面外层卡片壳解决重复边框来源，不增加条件分支。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 页面仅保留一层外部容器 -> Given 用户打开电子批记录三栏页面 / When 页面渲染完成 / Then 最外层 ContentWrap 不再显示卡片边框与内边距，仅保留内部三栏工作区卡片。`
- `BDD: 内部三栏卡片保持不变 -> Given 页面已去掉外层卡片 / When 用户查看批记录名称、报表名称、表单模板三栏 / Then 三个内部面板仍保持原有边框、标题和交互。`

## 里程碑

1. M1：补任务文档、命令记录和静态 RED 断言。
2. M2：移除电子批记录页面外层卡片壳。
3. M3：运行定向静态验证并回写结果。

## 最终验证结果

- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-electronic-batch-record-remove-outer-card\frontend-feature-evidence.md` -> PASS

## 完成记录

- 电子批记录页面最外层 `ContentWrap` 已改为无边框、无背景、无内边距外壳。
- 内部三栏 `batch-record-master-detail` 以及三个子面板卡片保持不变。
- 已补充静态契约断言，防止外层卡片回归。

## 预期验证

- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-electronic-batch-record-remove-outer-card\frontend-feature-evidence.md`

## Current Status

completed
