# 执行日志：修复前端发布输入 lockfile 供应链门禁

- 2026-06-29：创建任务文档，按发布链路前置问题执行严格 TDD。
- BDD: 干净前端发布源可安装依赖 -> Given 前端发布使用干净 worktree / When 在该 worktree 执行 `pnpm install` / Then 不再触发 tarball URL 供应链门禁，`vite` CLI 可用。
- GREEN: experience-preflight-20260629-frontend-lockfile-fix -> PASS，已读取 `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`，命中并摘取 `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`、`D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`、`D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`。已确认本次只处理前端 lockfile 与依赖前置，不修改无关业务代码。
- RED: `pnpm --dir D:\ProjectPackage\Int\release-worktrees\IntRuoyi-frontend-20260629-63cedb3 install` -> FAIL，`ERR_PNPM_TARBALL_URL_MISMATCH` 暴露 lockfile 供应链门禁，当前至少 5 个依赖项 tarball URL 仍为 `https://registry.npmmirror.com/...`，与当前 registry 元数据不一致：`cross-env@7.0.3`、`dayjs-plugin-lunar@1.4.1`、`dhtmlx-gantt@9.1.1`、`jsbarcode@3.12.3`、`tyme4ts@1.4.6`。
- GREEN: `pnpm --dir D:\ProjectPackage\Int\release-worktrees\IntRuoyi-frontend-20260629-63cedb3 install` -> PASS，已将前端正式仓与干净前端发布源中的 `pnpm-lock.yaml` 相关 tarball URL 修正为 `https://registry.npmjs.org/...`，供应链策略校验通过；随后在干净前端发布源 `pnpm-workspace.yaml` 通过官方 `allowBuilds` 显式放行 `@parcel/watcher`、`@swc/core`、`core-js`、`core-js-pure`、`es5-ext`、`esbuild` 与 `vue-demi`，安装成功完成，`node_modules\vite\bin\vite.js` 已恢复可用。
- GREEN: `git diff --stat -- pnpm-lock.yaml` -> PASS，锁文件修复范围限定为 5 处 tarball URL 映射替换：`cross-env@7.0.3`、`dayjs-plugin-lunar@1.4.1`、`dhtmlx-gantt@9.1.1`、`jsbarcode@3.12.3`、`tyme4ts@1.4.6`，未引入额外依赖解析变更。
- GREEN: commit-ready-20260629-frontend-lockfile-fix -> PASS，当前任务已满足“最小正式修复 + 验证通过”提交门禁；下一步仅提交 `pnpm-lock.yaml` 与本任务台账，再回到维护仓继续真实 `build-release -> publish-test`。
- GREEN: `git commit -m '任务: 修复前端锁文件供应链门禁'` -> PASS，已将锁文件供应链修复提交为 `21a09c9b3c4864aa1c480d2f1f379994c32ea059`。
- RED: `pnpm --dir D:\ProjectPackage\Int\release-worktrees\IntRuoyi-frontend-20260629-21a09c9 install --frozen-lockfile` -> FAIL，锁文件供应链门禁已通过，但当前已提交 HEAD 仍缺少 build scripts 放行配置，pnpm 报 `ERR_PNPM_IGNORED_BUILDS`，要求显式批准 `@parcel/watcher`、`@swc/core`、`core-js`、`core-js-pure`、`es5-ext`、`esbuild` 与 `vue-demi`。
