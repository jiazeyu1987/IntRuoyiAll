# DCC 文档目录文件夹边框颜色

## Task Goal

根据截图反馈调整文控中心文档目录列表：有子文件夹的父文件夹使用绿色边框，没有子文件夹的文件夹使用黑色边框。

## Milestones

- [x] 定位文档目录页面组件、样式和现有目录树数据字段。
- [x] 先补充最小静态契约，证明目录项按是否存在子文件夹区分边框。
- [x] 实现目录项边框颜色逻辑，不改变目录 API、权限和表格结构。
- [x] 运行目标静态契约和必要前端验证，记录 RED/GREEN/回归证据。
- [x] 完成收尾检查、经验沉淀和提交推送门禁。

## Expected Verification

- `node tests/e2e/dcc-directory-folder-border-static.spec.js`
- 受影响前端文件的结构/样式检查。
- 若改动触及 TypeScript 编译链路，运行 `pnpm ts:check` 或记录明确阻塞原因。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按目录项正式子节点数据驱动样式。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 前端开发规则：保持现有 Vue/TypeScript、Element Plus、表格和样式模式；先记录 BDD，再执行 RED -> GREEN -> REGRESSION。
- Int 统一前端样式：遵循白色工作面、浅灰蓝边框和紧凑运维台风格；本次用户明确指定有子目录绿色、无子目录黑色，按用户要求覆盖局部边框色。
