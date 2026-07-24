# 任务：电子批记录表单宽度自适应真实页面核验

## 任务目标

- 在本机真实前端 `http://localhost:8081` 登录后进入电子批记录页面。
- 核验右侧真实 Jimu 表单预览在页面中的宽度自适应效果是否生效。
- 不做写入型操作，不改动业务数据，只做登录、打开页面和只读视觉检查。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-electronic-batch-record-fit-width\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已完成代码修复和静态契约验证；本次仅追加真实页面只读核验，不回退代码结果。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 真实 Playwright 登录前，必须先执行官方最小路径 `login-preflight.mjs`。
  - 长链路真实页面核验前，`execution-log.md` 必须先记录 `GREEN: experience-preflight -> PASS`。
  - 本次仅允许本机入口、测试租户只读核验，不得静默切换租户、账号或环境。
  - PowerShell 中文读写和命令记录统一按 UTF-8 处理。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本次只验证已交付修复在真实页面中的表现，不增加旁路逻辑。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 登录后电子批记录右侧表单按容器宽度铺满 -> Given 用户通过本机真实登录页进入电子批记录页面并选中一个可预览报表 / When 右侧真实 Jimu 预览加载完成 / Then 表单应在预览容器中按宽度等比铺满，而不是保持窄宽度。`
- `BDD: 页面变窄时表单继续等比缩放 -> Given 右侧真实 Jimu 预览已经显示 / When 浏览器视口缩窄或预览容器宽度减小 / Then 表单仍应保持等比缩放，不出现工具区回退。`

## 里程碑

1. M1：补任务文档、请求日志和 `experience-preflight` 前置。
2. M2：执行官方登录预检并进入目标页面。
3. M3：完成真实页面视觉核验并回写结论。

## 预期验证

- `node scripts/preflight/login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/batch-record-template --target-text 电子批记录`
- Playwright headed 真实页面打开并截图核验。

## 最终验证结果

- `node scripts/preflight/login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/batch-record-template --target-text 电子批记录` -> PASS
- Playwright 本机只读核验 -> PASS，宽屏下右侧表单已按预览容器铺满；窄屏下右侧容器自身仅剩约 `316px`，表单随容器同步缩小。

## 完成记录

- 已通过真实登录页进入 `http://localhost:8081/mes/pro/batch-record-template` 并选中报表核验。
- 宽屏实测：右侧预览容器约 `796px`，iframe 同步铺满，证明“不能放大”的问题已修复。
- 窄屏实测：右侧预览容器约 `316px`，iframe 与表单同步缩小；当前“小表单”现象来自三栏固定列宽挤压，而不是 fit-width 逻辑失效。
- 视觉核验截图输出到 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\`。
