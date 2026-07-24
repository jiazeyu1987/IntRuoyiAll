# 任务：发布目标主机配置化

## 任务目标

修正构建发布包和部署发布包链路中的目标主机来源：测试服、正式服、备份服 IP 只能来自运行控制台/发布配置，发布脚本不得内置固定 `.57/.58/.59` 映射；部署校验与 MinIO 域名校验必须按本次发布目标主机生成。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；缺少目标主机配置时直接失败。
- `是否从根因和长期维护角度解决`：是；目标主机作为运行控制配置传入发布脚本，脚本只消费参数。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: 构建发布包使用服务器侧目标配置 -> Given 运行控制台配置了 test/prod/backup 的发布主机 / When 点击“构建发布包” / Then 命令必须把三组主机传给 `publish-int-ruoyi.ps1`，发布脚本不得自行推导固定 IP。
- BDD: 缺少目标主机配置时失败 -> Given 构建或部署动作缺少目标主机 / When 执行发布脚本或运行控制台命令 / Then 必须 fail fast，说明缺少哪个主机配置以及影响。
- BDD: 部署校验按当前目标主机生成 -> Given 选择测试、正式或备份发布目标 / When 生成 post-import SQL 和运行时探针 URL / Then MinIO 域名校验使用当前 `ServerHost`，不得固定到某一个环境 IP。

## 里程碑

- [x] M1：补充任务文档和 RED 测试，证明当前脚本仍内置环境 IP。
- [x] M2：修改发布脚本参数、运行时 env 生成和运行控制台命令拼接。
- [x] M3：运行 Python/Java 回归并记录 GREEN 证据。
- [x] M4：执行任务收尾清理预览/应用并提交本任务改动。

## 预期验证

- `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_publish_script_uses_configured_target_hosts_instead_of_hardcoded_environment_ips -q`
- `python -X utf8 -m pytest script/tests/test_runtime_control_ops_scripts.py -q`
- `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence docs/environments/ci-cd-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260605-release-target-host-configuration --mode preview`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260605-release-target-host-configuration --mode apply`

## 当前状态

completed

## Current Status

completed
