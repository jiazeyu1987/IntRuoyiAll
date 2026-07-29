# Verification Report

## Results

- PASS: `node tests\e2e\system-codex-test-node-chain-static.spec.js`
- PASS: `node tests\e2e\system-codex-test-management-static.spec.js`
- PASS: `node tests\e2e\system-codex-test-run-monitor-static.spec.js`
- PASS: `pnpm ts:check`
- PASS: `git diff --check`
- PASS: `scripts\preflight\branch-runtime-port-guard.ps1`
- PASS after local `int_main` merge `532ca35b`: `node tests\e2e\system-codex-test-node-chain-static.spec.js`
- PASS after local `int_main` merge `532ca35b`: `node tests\e2e\system-codex-test-management-static.spec.js`
- PASS after local `int_main` merge `532ca35b`: `node tests\e2e\system-codex-test-run-monitor-static.spec.js`
- PASS after local `int_main` merge `532ca35b`: `pnpm ts:check`

## Notes

- `pnpm ts:check` 首次失败原因是当前 worktree 缺少 `node_modules`，`cross-env` 不存在；执行 `pnpm install --frozen-lockfile --prefer-offline` 后依赖补齐，未修改 lockfile。
- 当前 worktree 首次缺少端口登记，已用正式 reserve 脚本登记 slot 13，仅用于守卫归属，不启动前后端服务。
- 远端 `git fetch origin int_main` 两次因连接重置失败；当前融合基于本地 `int_main`，远端推送需在网络恢复后完成。
- 本轮未运行真实 Playwright 写入型 E2E；改动是前端静态布局和刷新机制，已用聚焦静态合同与类型检查覆盖。
