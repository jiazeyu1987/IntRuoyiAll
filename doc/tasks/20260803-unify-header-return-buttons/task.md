# 20260803 Unify Header Return Buttons

## Task Goal

把截图红框中这类页面头部“返回xxx”入口统一为标准“返回”按钮，并扫描其它前端页面是否存在同类按钮；只调整按钮文案/样式语义，不改变返回行为、路由、接口、权限或保存链路。

## Milestones

1. 建立任务文档、记录 BDD/TDD 验收口径。
2. 扫描 `IntRuoyiFronted` 中页面头部、工作区头部、详情页头部的“返回xxx”按钮。
3. 先补任务专用静态契约，确认当前“返回表单模板”等长返回按钮会触发 RED。
4. 将命中的同类按钮统一为 `ep:arrow-left` + 文案“返回” + Scheme D 返回按钮样式。
5. 运行 GREEN 静态契约、相邻页面静态回归和 TypeScript 检查。
6. 更新验证报告并安全收尾，避免混入并行任务改动。

## Expected Verification

- RED: 新静态契约先失败，原因是头部返回按钮仍使用“返回表单模板”等长文案。
- GREEN: 新静态契约通过，证明命中页面统一为标准“返回”按钮。
- Regression: 运行 FormCenter 与 Scheme D 相邻静态契约；涉及 Vue/TS 时运行 `pnpm ts:check`。
- Source review: 确认 API、权限、路由、错误提示、保存/关闭/重读按钮未改变。

## Current Status

in_progress

## Design Constraints Check

- 是否引入 fallback/降级/吞异常：否。不得通过隐藏错误、默认成功或吞异常让页面看起来正常。
- 是否从根因和长期维护角度解决：是。通过统一静态契约约束同类头部返回按钮，而不是只改截图页面。
- 是否存在临时补丁或绕过：否。若存在同文件并行改动，采用选择性暂存或记录 blocker，不覆盖并行改动。

## Scope

- 前端范围：`IntRuoyiFronted` 下 Vue 页面、样式类和任务专用静态契约。
- 保护范围：后端、API wrapper、路由守卫、权限指令、请求参数、响应 DTO、业务状态、测试数据。
