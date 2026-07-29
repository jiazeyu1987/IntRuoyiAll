# 填写配置红框内容删除

## Task Goal

删除填写配置页面截图红框标注的顶部状态内容和右侧辅助表单映射提示文案，保持其余填写配置、原表单配置、辅助表单映射功能不变。

## Milestones

- [x] 定位填写配置页面组件、路由与现有测试入口
- [x] 复用并修正聚焦静态合同，锁定红框内容不再渲染
- [x] 修改前端组件删除目标内容
- [x] 运行聚焦验证并记录结果
- [ ] 收尾清理、经验沉淀、提交并推送

## Expected Verification

- RED：聚焦静态合同在旧实现上失败，证明仍渲染红框目标内容。
- GREEN：聚焦静态合同通过，证明目标内容已从源码/渲染契约中移除。
- REGRESSION：运行相邻前端静态检查或记录无法运行的明确阻塞。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，计划删除目标 UI 源头内容而非隐藏错误或绕过状态。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 已读取 `docs/experience-index.md`。
- 适用门禁：`docs/frontend-development.md#前端静态契约隔离门禁`，本次使用聚焦静态合同验证截图红框删除，不扩大到无关前端逻辑。
- 适用门禁：`docs/e2e-rules.md#Windows 换行与脚本行为同步`，同步更新真实 E2E 中等待已删除提示文案的定位。

## Cleanup Keep

- doc/tasks/20260729-fill-config-redbox-cleanup/frontend-feature-evidence.md
