# QA 规程产品下拉已配置项置顶高亮

## Task Goal

- 在 QA 规程配置产品下拉中，将已经配置好检验规程的产品排在最前，并以绿色样式标识，便于用户优先识别已配置产品。

## Milestones

- [ ] 定位 QA 规程配置页面产品下拉的数据来源、排序逻辑和样式入口。
- [ ] 补充聚焦静态合同，先证明当前下拉未锁定“已配置置顶 + 绿色标识”行为。
- [ ] 实现最小前端改动，保持正式产品候选和已有配置状态来源不变。
- [ ] 运行目标合同、相邻验证和静态检查，记录证据。

## Expected Verification

- 目标静态合同先 RED 后 GREEN，覆盖已配置产品排序优先级、绿色类名或标识、未配置产品仍保留在后。
- 运行受影响的 QA 规程相关静态合同。
- 运行 pnpm ts:check 或记录与当前任务无关的既有阻塞。
- 运行 git diff --check。

## Current Status

in_progress

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，计划复用正式配置状态字段驱动排序与样式。
- 是否存在临时补丁或绕过：否。

## Experience Gate

- docs/experience-index.md 已存在；命中关键词：QA 规程配置状态、已配置 QA 规程、待配置 QA 规程、产品级规则草稿。
- 适用门禁：QA 规程配置状态必须来自产品级规程记录。
- Preflight check：配置状态必须由后端 project-statuses 按正式 productMasterId/product_id 返回；前端不得用项目代码、产品名、前端常量或查询失败默认待配置替代。
- Blocker：状态接口失败、产品缺正式绑定、或页面以空状态/模板初始化数据判断配置状态时必须停止。
- Verification：前端静态合同必须锁定 project-statuses API、产品 ID 状态 Map、已配置优先排序、绿色状态类名，并运行相邻 QA 合同和 ts:check。
