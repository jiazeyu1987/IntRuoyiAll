# 执行日志：构建发布包增加展厅构筑包选项

BDD: 默认构建发布包不包含展厅构筑包 -> Given 运维打开“构建发布包”弹窗 / When 未勾选“发布展厅构筑包”并提交 / Then 请求携带 `includeShowroomBuildPackage=false`，后端构建命令使用 `-Component intruoyi`。

BDD: 勾选展厅构筑包需要覆盖确认 -> Given 运维打开“构建发布包”弹窗 / When 勾选“发布展厅构筑包” / Then 前端提示“当前选中的展厅构筑包会覆盖服务器的展厅数据，是否继续？”，取消时恢复未选中，确认时保持选中。

BDD: 选中展厅构筑包构建完整发布包 -> Given 运维已确认发布展厅构筑包 / When 提交构建发布包 / Then 请求携带 `includeShowroomBuildPackage=true`，后端构建命令使用 `-Component full`，manifest 记录包含展厅构筑包。

BDD: 发布包部署按 manifest 组件范围执行 -> Given ReleasePackage manifest 记录 `component=intruoyi` / When 执行 deploy-release / Then 脚本只部署后端和管理前端，不复制、不重启、不校验 Website。

BDD: 缺少组件范围信息不得静默按 full 发布 -> Given ReleasePackage manifest 缺少 `component` 且未显式传入 `-Component` / When 执行 deploy-release / Then 脚本 fail fast 并提示重建发布包或显式指定组件。

INFO: 已检查最近运行控制台任务 `20260608-runtime-console-empty-repo-root` 为 completed；`20260608-backup-incremental-manifest-short-loop` 为 blocked，但阻塞项是测试服备份恢复闭环证据，和本次本地发布包组件范围控制无直接冲突。

RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest" test` -> FAIL，预期原因：`RuntimeControlActionReqVO` 缺少 `includeShowroomBuildPackage`，`RuntimeControlReleasePackageRespVO` 缺少 `component` 与 `includeShowroomBuildPackage`。

RED: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL，预期原因：`publish-int-ruoyi.ps1` 尚未支持 `intruoyi` 组件、manifest 组件字段和 deploy-release 组件范围解析。

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS，71 passed。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest" test` -> PASS，53 tests。

INFO: `pnpm ts:check` -> FAIL，Node 默认堆内存不足，`vue-tsc` 报 `JavaScript heap out of memory`，不是类型错误。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。

GREEN: `node tests\e2e\runtime-control-build-release-showroom-option-static.spec.js` -> PASS。

INFO: Playwright inline 验证首次因 PowerShell 5.1 管道编码导致中文选择器变成问号，已改用 Unicode escape；随后本地 8081 旧 Vite 缓存出现 `504 Outdated Optimize Dep`，已停止本次启动的旧进程并用 `pnpm dev -- --host 0.0.0.0 --port 8081 --force` 重启。

GREEN: Playwright 本机 `http://localhost:8081` 登录 `芋道源码/admin`，打开运行控制台，点击“构建发布包” -> PASS，`发布展厅构筑包` 默认未选；勾选出现覆盖提示；取消恢复未选；再次勾选并确认后保持选中；未点击提交，未触发实际构建发布。

GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260608-runtime-build-release-showroom-option\backend-api-evidence.md` -> PASS。

GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260608-runtime-build-release-showroom-option\frontend-feature-evidence.md` -> PASS。

GREEN: `python C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence doc\tasks\20260608-runtime-build-release-showroom-option\ci-cd-evidence.md` -> PASS。

CLOSEOUT: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260608-runtime-build-release-showroom-option --mode preview` -> PASS，keep task/evidence，delete/blocked/warnings 均为 none。

BLOCKED: 本机运行控制台页面当前所有状态卡均显示错误，`http://127.0.0.1:48081/actuator/health` 无法连接，且未发现监听 48081 的 `java.exe` 后端进程；本任务暂停，先恢复本机运行控制台后端运行态。
