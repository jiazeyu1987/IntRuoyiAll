# 任务：DCC 受控浏览本地缓存写入失败

## 任务目标

修复测试服务器 DCC 受控浏览页出现 `DCC 受控浏览本地缓存写入失败，请检查浏览器本地存储权限。` 的问题，确保目录浏览状态缓存继续可用，但不再因缓存体积过大或重复序列化导致真实页面报错。

## 当前状态

completed

## 前一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-scheduler-workbench-full-config-package\task.md`
- 状态：`completed`
- 处理说明：上一任务已完成；当前任务仅修改 DCC 受控浏览页缓存逻辑、定向静态测试与本任务文档，不覆盖前端仓其他未归属改动。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md` 与 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
  - 生产代码变更前必须先创建任务文档、执行日志、设计约束检查与 BDD 场景。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 中文任务文档、日志与测试文件统一按 UTF-8 读写。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - 保持当前 DCC 运营型目录树 + 列表骨架，不做无关视觉重排；仅修正缓存模型与状态恢复行为。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。继续显式暴露真实缓存读写错误，不增加静默降级或“写失败也当成功”的分支。
- `是否从根因和长期维护角度解决`：是。根因指向目录元数据缓存写入模型过重、重复嵌套序列化导致测试服数据规模下触发浏览器配额异常；本次将缓存收敛到最小必要结构。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 目录缓存只持久化最小必要节点 -> Given 浏览页已加载目录树与类别缓存 / When 页面写入本地缓存 / Then 缓存只保存目录显示所需的轻量节点字段，不重复嵌套整棵 children 树。`
- `BDD: 大目录数据下缓存写入仍可完成 -> Given 测试服务器目录树和类别数据量明显大于本机最小样本 / When 浏览页刷新目录与类别缓存 / Then 页面不再因本地缓存体积过大弹出写入失败提示。`
- `BDD: 状态记忆与目录展开仍然保留 -> Given 用户已选择目录、分页并展开若干目录 / When 用户刷新或重新进入浏览页 / Then 目录选中态、分页条件和展开态仍可从本地缓存恢复。`
- `BDD: 缓存异常继续显式暴露 -> Given 浏览器真实拒绝 localStorage 写入 / When 页面尝试写缓存 / Then 页面仍提示本地缓存写入失败，不静默吞错。`

## 里程碑

1. M1：创建任务文档并补齐回归证据骨架。`completed`
2. M2：补 RED 静态合同，锁定目录缓存不得重复持久化嵌套 children 树。`completed`
3. M3：实现最小缓存模型与浏览页重建逻辑。`completed`
4. M4：运行定向静态验证并回填证据。`completed`

## 预期验证

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\dcc-browser-remember-state-cache-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\dcc-browser-cache-write-failure-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json`

## 范围说明

- 只改 `src/views/dcc/controlled-file/browser/` 下的前端缓存逻辑、相关静态测试与本任务文档。
- 不改后端接口、数据库、权限或 DCC 文件预览/下载链路。

## 当前验证结论

- 定向静态回归已通过，证明目录缓存已收敛为轻量节点结构，不再直接持久化嵌套 `children` 树。
- 缓存恢复仍能重建目录树、类别缓存与展开态恢复。
- 全量 `vue-tsc` 未能作为本任务最终放行项，因为仓内既有无关错误 `src/views/mes/pro/edhr-batch/BatchExecutionTemplateSimulatePage.vue:158,305` 仍存在。
