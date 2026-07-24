# 20260703 DCC 受控浏览左侧目录独立滚动修复

## Current Status

completed

## 任务目标

- 修复 DCC 受控浏览页左侧目录树内容过长时撑高整页，导致右侧文件列表区域随左侧目录滚动的问题。
- 左侧目录区域应拥有独立纵向滚动；右侧列表区域保持当前位置和布局稳定。
- 从布局容器根因解决，不引入 fallback、降级、吞异常或临时绕过。

## 里程碑

- [x] 读取 PowerShell / Windows shell 经验门禁。
- [x] 读取经验索引并摘取命中门禁。
- [x] 读取前端样式基线与缺陷修复合同。
- [x] 确认相邻目录树滚动状态任务已完成。
- [x] 为左侧目录独立滚动补充 RED 静态回归测试。
- [x] 最小修改受控浏览页布局样式。
- [x] 运行目标回归验证并记录结果。

## 预期验证

- 左侧目录列不再靠内容高度撑开整页。
- 左侧目录搜索结果和目录树共用同一个列内滚动上下文。
- 右侧文件列表 `browser-list-wrap` 与左侧目录滚动解耦，红色区域滚动时蓝色区域不被带动。
- GREEN：目标静态回归测试通过。
- GREEN：DCC browser 相邻展开/滚动静态测试继续通过。

## 经验门禁

- 命中 `docs/powershell-memory.md`：所有 PowerShell / Windows shell 命令按 UTF-8 读取，不使用 `&&`，不使用默认中文编码路径。
- 命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：保持 DCC 运营控制台式白底、轻边框、紧凑布局；本次只修布局滚动，不重做视觉风格。
- 本次不执行真实登录写入、服务器写入、发布、备份、恢复或高风险 E2E。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，通过页面级栅格和目录卡片内部 flex/overflow 约束，使左侧目录形成独立滚动上下文。
- `是否存在临时补丁或绕过`：否。

## 完成结果

- 已在 DCC 受控浏览页增加 `browser-page-layout` 页面级固定高度与 `overflow: hidden`，避免左侧长目录树撑高整页。
- 已将目录搜索结果与目录树包入 `browser-directory-scroll`，形成左侧独立纵向滚动上下文。
- 已将目录卡片 body 调整为有界 flex 列布局，确保滚动只发生在红色左侧区域内，右侧列表区域保持稳定。

## 最终验证

- GREEN：`node tests/e2e/dcc-browser-tree-expand-scroll-static.spec.js` -> PASS。
- GREEN：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check` -> PASS。
