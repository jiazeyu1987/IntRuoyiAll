# 检验方法卡片显示修正

## Task Goal

根据截图中红框反馈，修正一线检验卡片的显示：红框内字号更小，只显示“检验方法”，卡片名称完整显示，不再用省略号截断。

## Milestones

- [x] 定位截图对应前端组件、映射函数和样式块
- [x] 先补充聚焦静态合同，覆盖检验方法只显示与名称完整显示
- [x] 实现最小前端修正，不改变 API、提交载荷或无关卡片行为
- [x] 运行目标合同、相关前端检查和 diff 检查
- [x] 更新验证报告并完成任务收尾记录

## Expected Verification

- 目标静态合同先 RED 后 GREEN
- 目标合同验证卡片只显示检验方法，不显示设备可选/已填统计等非方法摘要
- 目标合同验证卡片标题完整显示并使用更小字号，不被 `text-overflow: ellipsis` 截断
- `pnpm ts:check` 或记录明确的无关阻塞
- `git diff --check`
- `frontend-feature-delivery` evidence validator PASS

## Applicable Gates

- `docs/frontend-development.md#用户可见描述与内部编码隔离门禁`：截图可见区域必须按正式展示字段显示，禁止用编码、占位或 CSS 隐藏冒充正确显示。
- `docs/frontend-development.md#前端截图样式块静态契约门禁`：静态合同必须锁定目标选择器和样式块，不能用跨整文件宽泛正则证明样式修正。
- `docs/experience-index.md` 命中 `显示名称不显示编码`、`截图样式块`、`text-overflow` 相关经验入口，需把合同聚焦在目标红框卡片。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，目标是调整正式卡片展示与局部样式契约。
- 是否存在临时补丁或绕过：否。

## Current Status

completed

- 实现、验证、evidence validator 和 cleanup apply 均已完成；未执行 Git commit/push，因为当前项目 Git Policy 默认不提交，除非用户显式要求。
