# 任务：修复 Docker Hub 前置检查提示乱码

## 任务目标

修复发布脚本在 Docker Hub 基础镜像 metadata 前置检查失败时中文提示乱码的问题。Docker Hub 网络不通仍需 fail-fast，不引入镜像源 fallback；错误提示必须在 Windows PowerShell / Docker Desktop 日志中保持可读。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260603-dockerhub-auth-connectivity-failfast/task.md`
- 状态：`completed`
- 影响：上一个任务已提交发布前 Docker Hub metadata 检查；本任务只修复该错误提示的编码/可读性，不修改构建行为和基础镜像来源。

## BDD 场景

- BDD: Docker Hub 前置检查失败提示可读 -> Given Docker Desktop 无法访问 Docker Hub registry / When 发布脚本执行基础镜像 metadata 前置检查 / Then 失败信息必须包含 ASCII 错误码和可读的 Docker Desktop proxy/DNS/network 指引，不得出现乱码。
- BDD: Docker Hub 网络失败仍然 fail-fast -> Given Docker Hub 不可达 / When 前置检查失败 / Then 发布脚本仍必须失败，不得切换镜像源、使用缓存镜像或静默继续构建。

## Milestones

- [x] M1：建立任务文档并确认上一任务完成。
- [x] M2：补充 RED 契约测试。
- [x] M3：最小修改发布脚本输出编码与错误提示。
- [x] M4：运行 GREEN/回归验证并 closeout。

## Expected Verification

- `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "dockerhub or utf8"`
- `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py`
- 任务日志必须记录 RED/GREEN/REGRESSION。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。不改镜像源、不跳过 Docker Hub metadata 检查、不使用缓存镜像冒充成功。
- `是否从根因和长期维护角度解决`：是。根因是脚本面向 Windows 控制台输出中文时可能被错误编码；通过显式 UTF-8 初始化和 ASCII 错误码保证日志可读。
- `是否存在临时补丁或绕过`：否。只修复提示可读性，不改变失败条件。

## 当前状态

completed

## 已完成工作

- 为发布工具链测试新增 Docker Hub 前置检查提示可读性契约。
- 为发布工具链测试新增 UTF-8 控制台输出初始化契约。
- 发布脚本启动时显式设置 `[Console]::InputEncoding`、`[Console]::OutputEncoding` 与 `$OutputEncoding` 为 UTF-8 no BOM。
- Docker Hub 前置检查失败提示改为稳定 ASCII 错误码 `DOCKERHUB_PREFLIGHT_FAILED`，避免 Windows 控制台编码导致中文乱码。

## 验证结果

- RED：`python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "dockerhub or utf8"` -> FAIL，预期原因为脚本缺少 UTF-8 输出初始化且提示仍使用易乱码中文。
- GREEN：`python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "dockerhub or utf8"` -> PASS，2 passed。
- REGRESSION：`python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py` -> PASS，54 passed。
- GREEN：`python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260603-dockerhub-preflight-message-encoding\execution-log.md` -> PASS。
- CLOSEOUT PREVIEW：`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-dockerhub-preflight-message-encoding --mode preview` -> READY，keep `task.md` / `execution-log.md`，delete `<none>`，blocked `<none>`，warnings `<none>`。
- TDD GATE：`python -X utf8 tool\verify_tdd_compliance.py --task-dir doc\tasks\20260603-dockerhub-preflight-message-encoding --all-changed` -> PASS。

## Blockers

- none for message encoding fix.
- External verification gap: Docker Hub 连接失败仍需网络/代理/DNS 修复，本任务不引入 fallback。
