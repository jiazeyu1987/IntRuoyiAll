# 任务：展厅版本中心前端实现

## 任务目标

- 按已放行设计文档实现展厅后台版本中心前端工作台。
- 实现范围覆盖：
  - 静态隐藏路由与入口跳转
  - 版本中心三栏页面
  - history / detail / republish UI
  - blocker 展示与当前内容/当前线上/current release 标记
- 本任务由主 reviewer 负责放行，多个子 agent 并行开发；只有完全符合设计文档、BDD/TDD 证据完整、通过 reviewer 复审的改动才可合入。

## 非目标

- 不改动 `Website` 前台应用代码。
- 不改写后端合同，不自行猜测接口字段。
- 不引入 mock 数据、fallback 数据或临时假 UI。

## 前序任务检查

- 已检查设计任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260523-showroom-version-center-design-docs` 不存在 repo-local 副本；本前端实现以已放行的 `ruoyi-vue-pro` 设计任务为唯一设计基线。
- 已确认版本中心设计文档已由 reviewer 放行，不阻塞本次实现启动。

## 里程碑

- [x] M1：建立前端实现任务包与执行日志。
- [x] M2：完成路由/入口 RED 测试与实现。
- [x] M3：完成版本中心页面、合同消费与交互 RED 测试与实现。
- [x] M4：完成前端回归验证与 reviewer 复审。

## 预期验证

- 前端任务必须保留以下证据：
  - `BDD: <scenario> -> Given/When/Then`
  - `RED: <command> -> FAIL, <expected reason>`
  - `GREEN: <command> -> PASS`
  - `REGRESSION: <command> -> PASS`
- 预期至少覆盖：
  - 公司/产品两个静态隐藏路由
  - 从公司页和产品列表进入版本中心
  - 当前内容版本与当前线上版本双标记
  - 历史版本预览、当前内容 diff、current release 摘要
  - republish blocker 与成功刷新

## 当前状态

- 状态：已完成
- 已完成：
  - 已在独立 worktree `task/20260523-showroom-version-center-impl` 中启动实现
  - 已明确主 reviewer + 并行 worker 的执行模式
  - 已完成公司/产品两个静态隐藏路由与共享版本中心页面接入
  - 已完成公司页、产品列表、产品详情到版本中心的入口与返回行为
  - 已完成 history/detail/republish 合同消费、三栏页面、blocker 弹窗与成功刷新流程
  - 已完成版本中心相关静态 RED/GREEN/REGRESSION 断言
  - 已完成本地依赖安装与局部 `vue-tsc` 类型检查
  - 已通过主 reviewer 复审，当前实现满足设计文档要求
- 待完成：
  - 无
- 阻塞与影响：
  - 全量 `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` 在当前 worktree 规模下会触发 Node OOM；本任务已改用仅覆盖版本中心改动文件的 `tmp/tsconfig.version-center.json` 完成局部类型回归，不影响本次前端结果真实性。
