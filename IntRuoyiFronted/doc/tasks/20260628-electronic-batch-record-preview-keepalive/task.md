# 任务：电子批记录表单预览保活切换

## 任务目标

- 修复电子批记录页面右侧表单模板在已访问报表之间来回切换时仍反复转圈加载的问题。
- 已打开过的报表预览 iframe 必须保活，后续切换只做显示隐藏，不重复挂载。
- 保持现有真实 Jimu 预览、缓存失效规则和右侧操作区行为不变。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-electronic-batch-record-preview-cache\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已完成预览地址缓存；本次继续修复 iframe 节点重复挂载导致的二次转圈，不回退其结果。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- 适用强制门禁：
  - 保持三栏工作区和右侧预览区现有样式，不新增无关视觉改版。
  - PowerShell 中文读写、命令记录和验证输出统一按 UTF-8 处理。
  - 本轮仅做本机静态验证，不触发真实 Playwright、登录写入、服务器操作或其他高风险动作，因此无需 `experience-preflight`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接保留已访问报表 iframe 实例，避免因组件重挂载反复触发内部 loading。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 已访问报表切回不重新挂载 iframe -> Given 用户已经打开过多个电子批记录报表预览 / When 在这些已访问报表之间来回切换 / Then 页面复用已存在的 iframe，仅切换显示状态，不再次触发预览 loading。`
- `BDD: 新报表首次访问仍正常加载 -> Given 某个报表尚未建立预览 iframe / When 用户首次点击该报表 / Then 页面仍按真实 Jimu 预览链路加载，并在首次成功后纳入保活列表。`

## 里程碑

1. M1：补任务文档、命令记录和静态 RED 契约。
2. M2：将右侧预览改为已访问 iframe 保活切换。
3. M3：运行定向静态回归并回写结果。

## 预期验证

- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js`

## 最终验证结果

- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS

## 完成记录

- 右侧表单模板预览已改为 `templatePreviewFrames` 已访问列表，已打开过的 iframe 通过 `v-show` 切换显示，不再重复挂载。
- 新报表首次进入仍按真实 Jimu 预览链路加载，成功后纳入保活列表。
- 未改动现有缓存失效规则、操作区行为和真实预览地址来源。

## Current Status

completed
