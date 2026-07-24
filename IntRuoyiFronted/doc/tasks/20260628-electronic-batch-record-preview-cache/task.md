# 任务：电子批记录表单预览缓存

## 任务目标

- 将电子批记录页面右侧表单模板预览改为按报表缓存，避免用户每次点回同一报表都重新加载。
- 仅在该报表模板数据发生变化后失效缓存并重新获取预览地址。
- 保持现有真实 Jimu iframe 预览、签名位、单元格规则、重命名、删除与模拟填写行为不变。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-electronic-batch-record-simulate-entry\task.md`
- 状态：`IN_PROGRESS`
- 处理说明：该任务已在当前页面引入模拟填写入口，但前端仓当前存在未提交在途改动；本次仅在现有基础上追加预览缓存逻辑，不回退、不覆盖其已有修改。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- 适用强制门禁：
  - 保持当前运维台式三栏工作区样式，不新增无关卡片、视觉降级或旁路交互。
  - PowerShell 读取中文文件、记录命令与验证输出时统一按 UTF-8 处理。
  - 本轮仅做本机静态验证，不触发真实 Playwright、登录写入、服务器操作或其他高风险动作，因此无需 `experience-preflight`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接基于报表 `reportId + updateTime` 建立缓存版本，只有报表数据变更时才失效。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 重复切换同一报表复用预览缓存 -> Given 用户已加载某个电子批记录报表预览 / When 切换到其他报表后再次点回同一报表且该报表未更新 / Then 前端复用已缓存的预览地址，不再重新请求预览路径。`
- `BDD: 报表变更后预览缓存失效 -> Given 某个电子批记录报表已经有预览缓存 / When 用户执行会修改该报表模板数据的操作并再次查看预览 / Then 前端清理该报表缓存并重新请求最新预览路径。`

## 里程碑

1. M1：补任务文档、命令记录和静态 RED 契约。
2. M2：实现按 `reportId + updateTime` 复用预览缓存，并在相关变更链路触发失效。
3. M3：运行定向静态回归并回写结果。

## 预期验证

- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-electronic-batch-record-preview-cache\frontend-feature-evidence.md`

## 最终验证结果

- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-electronic-batch-record-preview-cache\frontend-feature-evidence.md` -> PASS

## 完成记录

- 右侧表单模板预览新增 `templatePreviewCache`，按 `reportId + updateTime` 复用已加载过的 Jimu 预览地址。
- 重复切回未更新的同一报表时，前端直接命中缓存，不再重新调用 `getDesignerPath`。
- 在导入、重命名、签名位保存、单元格规则保存、删除单报表、批量删除等会影响模板数据的链路上增加缓存失效。

## Current Status

completed
