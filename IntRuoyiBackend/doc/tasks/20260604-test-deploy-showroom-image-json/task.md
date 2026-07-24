# 任务：分析部署发布包到测试服后展厅图片 smoke 失败

## 任务目标

分析用户点击“部署发布包到测试服”后失败的原因。错误为测试服前端代理访问 `config_id=28` 的展厅封面图片时返回 HTTP 200 但 `Content-Type=application/json`，未返回 `image/*`。本任务先做只读诊断，不修改测试服数据库、MinIO、受保护默认文件配置 `infra_file_config.id=28` 或任何服务器数据。

## Previous Task Check

- 上一个同服务仓库任务：`doc/tasks/20260603-restore-release-code-before-dockerhub-preflight/task.md`
- 状态：`completed`
- 影响：上一任务已完成；本任务仅分析测试服部署失败原因。

## BDD 场景

- BDD: 测试服部署后展厅 smoke 图片应可读 -> Given 发布脚本已完成测试服服务启动与基础健康检查 / When 发布脚本通过前端代理访问 `config_id=28` 的展厅图片 / Then 响应必须为 `image/*`，否则应 fail fast 并暴露具体图片路径。
- BDD: 受保护展厅文件配置只读诊断 -> Given 默认展厅文件配置 `infra_file_config.id=28` 与 `showroom/%` 媒体 URL 受保护 / When 分析 smoke 图片失败 / Then 只能读取日志、manifest、配置与对象存在性，不得自动切换 bucket/domain/endpoint 或回填对象。

## Milestones

- [x] M1：建立任务文档并确认上一任务完成。
- [x] M2：定位本次运行控制台部署日志与失败 operation。
- [x] M3：核对发布脚本 smoke 选择逻辑与失败图片来源。
- [x] M4：只读判断失败属于数据包缺失、MinIO 绑定漂移还是脚本 smoke 选择问题。
- [x] M5：记录结论、证据与后续修复路径。

## Expected Verification

- 读取运行控制台最新部署日志，确认失败点和 releaseTag。
- 读取发布脚本 `Assert-RemoteShowroomSmokeImageContent` 逻辑，确认 smoke 图片选择规则。
- 如需测试服验证，仅执行只读 HTTP/SSH/SQL/MinIO 查询；禁止写入测试服数据。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本任务只分析失败原因，不隐藏 smoke 失败。
- `是否从根因和长期维护角度解决`：是。优先区分发布包是否包含数据、远端 MinIO 对象是否缺失、配置是否漂移、脚本是否选错样本。
- `是否存在临时补丁或绕过`：否。不改测试服数据、不回填对象、不切换受保护配置。

## 当前状态

completed

## 验证结果

- VERIFY：上一任务 `doc/tasks/20260603-restore-release-code-before-dockerhub-preflight/task.md` 状态为 `completed`。
- VERIFY：部署操作 `51bd4b72-a2fc-4486-949a-7db64f018eb4` 失败于最终展厅图片 smoke，失败 URL 为 `product-product_164-imported-cover.png`。
- VERIFY：发布包 `26-06-04 00:14:46` manifest 为 `publishScope=code-only`，不包含数据库 dump 或 MinIO 快照。
- VERIFY：失败 URL 响应体为 S3 `Access Key Id you provided does not exist`，不是对象 404。
- RED：目标契约测试失败，证明脚本在 code-only deploy-release 时没有为展厅文件配置重绑定读取目标 MinIO 凭据，且 SQL 可沿用旧凭据。
- GREEN：`python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "deploy_release_reads_remote_minio_credentials_for_showroom_file_rebind or publish_script_auto_rebinds_and_verifies_showroom_file_storage_after_restore" -q` -> PASS，`2 passed, 51 deselected`。
- GREEN：`python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS，`53 passed`。
- VERIFY：`git diff --check -- <相关文件>` -> PASS，仅提示 Git 将在下次触碰时把 LF 替换为 CRLF，无 whitespace error。

## Blockers

- 未重跑真实“部署发布包到测试服”，因为该操作会写入受保护的 `infra_file_config.id=28`；当前已完成本地代码修复与契约验证。

## Cleanup Keep

- `doc/tasks/20260604-test-deploy-showroom-image-json/bug-regression-evidence.md`
