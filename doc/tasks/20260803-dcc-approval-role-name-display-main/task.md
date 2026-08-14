# DCC Approval Route Role Name Display Main Sync

## Task Goal

把 DCC 审批路线列表主工作区代码同步为显示审批角色名称，而不是权限编码或 `审批角色#ID`。

## Milestones

- [x] M0: 确认主工作区仍是旧逻辑，隔离 worktree 修复未合入。
- [x] M1: 增加主工作区最小 RED 静态合同。
- [x] M2: 修正主工作区审批角色名称解析逻辑。
- [x] M3: 运行目标静态合同和 TypeScript 检查。
- [x] M4: 记录验证与剩余阻塞。
- [x] M5: 修复节点2首屏因未加载审批角色字典而显示 `-` 的回归。
- [x] M6: 通过本机真实页面只读 E2E 验证节点2显示审批角色名称。

## Expected Verification

- `node tests/e2e/dcc-controlled-file-routes-role-name-display-static.spec.js`
- `node tests/e2e/dcc-controlled-file-routes-node-columns-static.spec.js`
- `node tests/e2e/dcc-controlled-file-routes-list-display-static.spec.js`
- `pnpm ts:check`
- 本机 8081 真实页面只读 Playwright 校验：`/dcc/controlled-file/routes` 节点2按审批角色字典显示名称，且无 DCC 写请求。

## Current Status

blocked - 主工作区代码已修复，并通过静态回归、类型检查和本机真实页面只读 E2E；提交/推送仍被既有并行脏工作区和 GitHub HTTPS 443 网络问题阻塞。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本任务只修正正式审批角色显示来源。
- `是否从根因和长期维护角度解决`：是。按 `candidateSourceIds` 解析正式审批角色名称，并过滤技术标签。
- `是否存在临时补丁或绕过`：否。通过静态合同锁定显示逻辑。

## Verification Summary

- RED: `node tests/e2e/dcc-controlled-file-routes-role-name-display-static.spec.js -> FAIL`，旧主工作区缺少 `900332 -> 文控` 映射。
- GREEN: `node tests/e2e/dcc-controlled-file-routes-role-name-display-static.spec.js -> PASS`。
- RED: `node tests/e2e/dcc-controlled-file-routes-role-name-display-static.spec.js -> FAIL`，首屏查询未加载审批角色/用户字典，节点2中 `900834/900847/900859/900860/900844` 等正式审批角色只能退化为 `审批角色#ID` 并被技术标签过滤成 `-`。
- GREEN: `node tests/e2e/dcc-controlled-file-routes-role-name-display-static.spec.js -> PASS`，`handleQuery` 已在列表赋值前执行 `loadRouteSubjectLookups()`。
- REGRESSION: DCC 路线相邻静态合同全部通过。
- TYPECHECK: `pnpm ts:check -> PASS`。
- Runtime source check: `http://127.0.0.1:8081/src/views/dcc/controlled-file/shared/utils.ts` 和 `routes/index.vue` 已返回新逻辑，且运行态 `handleQuery` 包含 `await loadRouteSubjectLookups()` 后再调用 `getApprovalRoutePage(queryParams)`。
- REAL E2E: 本机 8081 `/dcc/controlled-file/routes` 只读 Playwright 校验 PASS；节点2可见文本逐行匹配审批角色名称，如 `编制人直接主管、QA、QMS、注册、文档管理员`，未出现 `审批角色#ID`、权限编码或整列 `-`，DCC 写请求数为 0。

## Closeout Blockers

- `E:\IntRuoyi` 在本任务开始前已有大量并行脏改动，不能安全提交或清理。
- GitHub HTTPS 443 当前不可达，之前 `git push` 同类任务分支已失败；本任务不进行强制提交、回滚或推送。
