# 任务：eDHR 批次打印版 PDF 前端收口

## 任务目标

- 将批次列表、详情、归档弹窗中的“下载PDF”统一改成“下载打印版PDF”。
- 保持现有 `latest -> download` 调用链不变，只原样透出后端“旧归档需重生成”的错误。
- 保持现有运维台样式与权限控制，不做无关 UI 重构。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-edhr-batch-progress-over-100-fix\task.md`
- 状态：`BLOCKED`
- 处理说明：该任务因当前更高优先级打印版 PDF 需求被打断，已在原任务文档中补记阻塞原因，不阻塞本次前端收口。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 前端改动必须先有可观察的静态/E2E 合同，再做最小实现。
  - 页面文案与操作区样式保持 IntPP 运维台风格，不引入额外卡片和重设计。
  - 真实下载/打印验证前必须先补 `GREEN: experience-preflight -> PASS` 并先跑官方登录预检。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。维持统一 API 链路，仅调整用户可见语义与错误提示，让真实后端阻塞可见。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 三个入口统一显示下载打印版PDF -> Given 用户位于批次列表、详情页或归档预览弹窗 / When 页面渲染下载入口 / Then 按钮文案统一显示“下载打印版PDF”。`
- `BDD: 旧归档错误原样提示 -> Given 后端返回“请先重新生成最终归档后再下载打印版 PDF” / When 用户点击下载或打印 / Then 前端 toast 或弹窗原样展示该错误，不改写为通用失败。`

## 里程碑

1. M1：补前端任务文档与执行日志。`COMPLETED`
2. M2：新增/更新静态合同锁定按钮文案与下载提示。`COMPLETED`
3. M3：最小修改列表页、详情页与弹窗文案。`COMPLETED`
4. M4：运行定向静态/E2E 验证并回填 evidence。`COMPLETED`

## 预期验证

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\edhr-final-archive-work-task-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\edhr-t11-final-archive-pdf-real-flow.e2e.js`

## 当前阻塞

- 无。当前 T11 下载/打印链路不涉及现场签名输入，已移除对 T9/T10/T11 签名密码环境变量的伪前置依赖；若后续脚本新增签名动作，再改为当次人工输入且不持久化。

## 最终验证

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\edhr-final-archive-work-task-static.spec.js` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/feedback/edhr-batch-execution --target-text 下载打印版PDF --timeout 90000` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/feedback/edhr-batch-execution/detail --timeout 90000` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\edhr-t11-final-archive-pdf-real-flow.e2e.js` -> PASS
