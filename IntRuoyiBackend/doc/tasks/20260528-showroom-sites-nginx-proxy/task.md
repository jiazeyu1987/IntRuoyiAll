# 任务：展厅站点 /showroom/sites 代理配置提交

## 任务目标

- 提交当前后端仓库中展厅站点 Nginx 配置改动。
- 确保 `website.nginx.conf` 将 `/showroom/sites/` 请求代理到后端，支持当前 Website 运行时使用的展厅发布与资源路径。
- 补充发布工具测试断言，防止发布配置回退到缺少 `/showroom/sites/` 代理的状态。

## BDD 场景

- BDD: 展厅站点路径由 Website Nginx 代理到后端 -> Given Website 运行时请求 `/showroom/sites/` scoped release/assets routes, When 测试服发布 Nginx 配置被应用, Then 请求应代理到 `__BACKEND_ORIGIN__`，而不是落到静态站点 history fallback。
- BDD: 发布工具配置回归保护 -> Given 发布脚本打包 Website Nginx 配置, When 执行发布工具测试, Then 测试必须断言 `/showroom/sites/`、`/showroom/release/`、`/showroom/assets/` 均被支持。

## 里程碑

- [x] M1：确认后端仓库当前差异。
- [x] M2：补充 RED 证据，确认上一提交版本缺少 `/showroom/sites/` 配置。
- [x] M3：运行发布工具测试并通过。
- [x] M4：执行收尾清理预览。
- [x] M5：提交后端仓库改动。

## 预期验证

- RED: `python -c "import subprocess; text=subprocess.check_output(['git','show','HEAD:script/deploy/int-ruoyi-test/website.nginx.conf'], text=True, encoding='utf-8'); assert 'location /showroom/sites/' in text"` 应失败，证明上一提交缺少代理配置。
- GREEN: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` 应通过。
- GREEN: `git diff --check` 应通过。
- 提交后 `git status --short --branch` 仅剩不纳入提交的本地运行状态采样文件。

## 当前状态

completed

## 当前发现

- 当前差异包含 `script/deploy/int-ruoyi-test/website.nginx.conf` 与 `script/tests/test_publish_int_ruoyi_to_test_tooling.py`。
- `runtime/runtime-control/runtime-ops/capacity-status.json` 是本地运行状态采样文件，不纳入本次提交。
- 收尾清理预览通过：无删除项、无阻塞、无警告。
- 已提交后端仓库改动；提交后仅剩未跟踪的本地运行状态采样目录 `runtime/`。

## 最终结果

- `website.nginx.conf` 已增加 `/showroom/sites/` 代理和无尾斜杠重定向。
- 发布工具测试已增加 `/showroom/sites/` 配置断言。
- RED、GREEN、diff check 和收尾清理预览均通过。
