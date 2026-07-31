# DCC 基础条目关联文档分类展开修复

## Task Goal

修复 `DCC基础条目 > 关联文档` 三栏导航：点击左侧阶段后，中间“文件类型”列必须显示 `DCC文件分类` 中该阶段节点展开后的直接子分类；右侧文件列表仍只显示当前基础条目关联文件中属于所选分类的文件。

## Milestones

1. `completed` - 建立任务文档、记录 BDD 和 Git 基线前置。
2. `completed` - 更新静态契约，先证明当前实现未从分类树阶段子节点构建文件类型列。
3. `completed` - 实现分类树路径解析与关联文档三栏分组修复。
4. `completed` - 运行目标静态契约、相邻回归和 TypeScript 检查。
5. `completed` - 完成收尾记录、经验沉淀、提交与推送。

## Expected Verification

- `pnpm e2e:dcc:project-code-associated-three-column:static`
- `pnpm e2e:dcc:category-lifecycle-stage:static`
- `pnpm e2e:dcc:file-type-taxonomy-basic-data:static`
- `pnpm e2e:dcc:file-type-taxonomy-tree-display:static`
- `pnpm e2e:dcc:file-type-taxonomy-unified-list-template:static`
- `pnpm ts:check`
- 若测试服务器真实登录和入口可用，再执行只读 Playwright 页面验证，确认中间列与 `DCC文件分类` 展开一致且无写请求。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本任务只使用正式 DCC 文件分类树与文件自身 `fileTypeTaxonomyId`/层级字段，不新增接口失败降级或默认成功。
- `是否从根因和长期维护角度解决`：是。根因是中间“文件类型”列只按关联文件已有 `fileTypeLevel3` 动态生成，未复用正式分类树阶段子节点。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 前端静态契约隔离门禁：本任务先更新聚焦静态契约并跑 RED，再实现 GREEN；若全量 `ts:check` 失败在无关历史问题上，需记录首个无关失败并保留目标契约证据。
- Windows 换行与脚本行为同步门禁：静态契约读取 Vue/SFC 源码时避免过窄缩进/换行断言，使用稳定 token 和正则锁定行为。
- PowerShell / Git 共同前置经验：命令不使用 `&&`；提交前检查分支、remote、脏工作区、暂存区；既有脏改动需单独基线提交并记录 hash 和文件清单。
- DCC 基础条目关联文档分类树门禁：中间“文件类型”列必须来自正式 `DCC文件分类` 的阶段直接子分类，关联文件只影响数量和右侧文件列表。

## Verification Summary

- `RED: pnpm e2e:dcc:project-code-associated-three-column:static -> FAIL, shared taxonomy helper buildDccFileTypeTaxonomyStageTypeNameMap missing.`
- `GREEN: pnpm e2e:dcc:project-code-associated-three-column:static -> PASS`
- `GREEN: pnpm e2e:dcc:category-lifecycle-stage:static -> PASS`
- `GREEN: pnpm e2e:dcc:file-type-taxonomy-basic-data:static -> PASS`
- `GREEN: pnpm e2e:dcc:file-type-taxonomy-tree-display:static -> PASS`
- `GREEN: pnpm e2e:dcc:file-type-taxonomy-unified-list-template:static -> PASS`
- `GREEN: pnpm ts:check -> PASS`
- Real test-server Playwright was not run in this implementation turn because the updated code has not been deployed to the test server and no explicit server operation/login verification scope was opened.
