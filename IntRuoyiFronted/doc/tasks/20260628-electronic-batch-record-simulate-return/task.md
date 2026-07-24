# 任务：模拟填写页来源返回按钮

## 任务目标

- 在 `eDHR模板模拟填写` 页面左上角增加返回按钮。
- 模拟填写页从不同入口进入时，点击返回后回到对应来源页面。
- 保持现有模拟填写页、模板加载合同和后端接口不变。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个相关前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-electronic-batch-record-simulate-entry\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已接入电子批记录到模拟填写页的入口；本次继续补齐“从来源页进入后可精确返回”的导航能力，不回退既有入口能力。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- 适用强制门禁：
  - 返回按钮和头部布局必须沿用当前运营页紧凑样式，不做无关视觉改版。
  - PowerShell 命令与中文文件读写必须显式按 UTF-8 处理。
  - 本轮只做本机静态验证，不触发真实 Playwright、登录写入、服务器操作或其他高风险动作，因此无需 `experience-preflight`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过入口统一传递来源路由，并在模拟填写页集中处理返回逻辑，避免写死单页面返回。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 模板说明页进入模拟填写后可返回来源页 -> Given 用户从 eDHR 批次模板说明页点击 模拟填写 / When 进入模拟填写页后点击左上角返回 / Then 页面返回到模板说明页。`
- `BDD: 电子批记录页进入模拟填写后可返回来源页 -> Given 用户从电子批记录表单模板区点击 模拟填写 / When 进入模拟填写页后点击左上角返回 / Then 页面返回到电子批记录模板管理页。`
- `BDD: 模拟填写页显示来源感知返回按钮 -> Given 模拟填写页收到来源页面路由信息 / When 页面渲染头部 / Then 左上角显示返回按钮并使用对应来源页文案。`

## 里程碑

1. M1：补任务文档、命令记录和返回契约 RED 测试。
2. M2：两个入口统一传递来源路由，并在模拟填写页增加返回按钮与回跳逻辑。
3. M3：运行定向静态回归并回写结果。

## 预期验证

- `node tests/e2e/edhr-batch-template-simulate-return-static.spec.js`
- `node tests/e2e/edhr-batch-template-simulate-static.spec.js`
- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-electronic-batch-record-simulate-return\frontend-feature-evidence.md`

## 最终验证结果

- `node tests/e2e/edhr-batch-template-simulate-return-static.spec.js` -> PASS
- `node tests/e2e/edhr-batch-template-simulate-static.spec.js` -> PASS
- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-electronic-batch-record-simulate-return\frontend-feature-evidence.md` -> PASS

## 完成记录

- `eDHR` 批次模板说明页进入模拟填写时，已透传当前页面 `fullPath` 和返回文案。
- 电子批记录模板管理页进入模拟填写时，已透传当前页面 `fullPath` 和返回文案。
- 模拟填写页左上角已新增返回按钮，并优先根据入口透传的 `returnTo` 返回来源页面。

## Current Status

completed
