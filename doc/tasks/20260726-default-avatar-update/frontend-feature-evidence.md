# Feature

将系统默认用户头像替换为用户提供的新 PNG 图标。

## Non-Goals

- 不修改用户已上传的自定义头像。
- 不修改后端用户头像字段或上传接口。
- 不引入兼容兜底或旧头像 fallback。

## Acceptance

- 当 `userStore.user.avatar` 为空时，顶部用户信息使用 `src/assets/imgs/default-avatar.png`。
- 当锁屏弹窗、锁屏页、头像裁剪组件没有用户头像时，使用同一默认头像 PNG。
- 当 AI 聊天用户消息没有用户头像时，使用同一默认头像 PNG。
- 默认头像文件内容与用户提供图片一致。

## Owned Files

- `IntRuoyiFronted/src/assets/imgs/default-avatar.png`
- `IntRuoyiFronted/src/layout/components/UserInfo/src/UserInfo.vue`
- `IntRuoyiFronted/src/layout/components/UserInfo/src/components/LockDialog.vue`
- `IntRuoyiFronted/src/layout/components/UserInfo/src/components/LockPage.vue`
- `IntRuoyiFronted/src/components/Cropper/src/CropperAvatar.vue`
- `IntRuoyiFronted/src/views/ai/chat/index/components/message/MessageList.vue`
- `IntRuoyiFronted/tests/e2e/default-avatar-asset-static.spec.js`

## API Contracts

- 无 API 合同变更。
- 数据状态保持现有逻辑：仅当用户头像字段为空时展示默认头像。

## BDD

- `BDD: 默认头像替换 -> Given 当前用户没有自定义头像 When 前端渲染默认头像 Then 顶部用户信息、锁屏、裁剪组件和 AI 聊天用户消息均使用新的默认头像 PNG 资源`

## RED

- `RED: node IntRuoyiFronted\tests\e2e\default-avatar-asset-static.spec.js -> FAIL, expected missing new default-avatar.png before implementation`

## GREEN

- `GREEN: node IntRuoyiFronted\tests\e2e\default-avatar-asset-static.spec.js -> PASS`
- `GREEN: rg -n -S "@/assets/imgs/avatar\.gif|assets/imgs/avatar\.gif" IntRuoyiFronted\src IntRuoyiFronted\tests --glob "!**/node_modules/**" -> no matches`

## Verification

- 静态契约校验新 PNG 文件存在、PNG 签名、472x472 尺寸、SHA256 `F7012CEEFC62703EE685C8D3AB419D2AB966063E9FBCFCB4E958C13D4A3A1102`。
- 静态契约校验所有默认头像兜底引用均指向 `@/assets/imgs/default-avatar.png`。
- 未运行全量 `pnpm ts:check` 或构建；本次变更只涉及静态资源路径和资产文件，且工作区已有大量无关脏改动。

## Blockers

- 实现和最小验证无阻塞。
- 工作区存在大量本任务外脏改动，未执行提交或推送，避免混入无关任务内容。
