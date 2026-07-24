# Task: DCC 文件类别一次性导入 IntAuth

## Goal

让 DCC 文件类别支持从 IntAuth 读取当前文件类别并导入到本地 `dcc_file_category`，导入完成后 DCC 后续运行继续只依赖本地表，不再在文件类别列表或治理链路上运行时关联 IntAuth。

## Scope

- 确认当前后端仓库上一个任务已完成，再开始本任务。
- 在生产代码变更前创建本任务文档和执行日志。
- 为 DCC 增加一次性“从 IntAuth 导入文件类别”的后端能力。
- 保持 `GET /dcc/file-categories` 继续只读取本地表，不引入运行时同步。
- 导入时复用同名本地类别或已记录来源的本地类别，保留本地绑定和治理规则。
- 缺少 IntAuth 配置、请求失败或返回 payload 异常时 fail fast。
- 记录 BDD/TDD 证据并完成定向验证。

## Previous Task Check

- Previous backend repo task: `doc/tasks/20260513-mes-batch-template-import-phase1/task.md`
- Status before this task: completed.
- Impact: 当前 `ruoyi-vue-pro` 后端仓库没有未完成的前置任务阻塞本次 DCC 文件类别导入实现。

## Milestones

- [x] M1: 确认上一个后端仓库任务状态。
- [x] M2: 在生产代码变更前创建任务文档与执行日志。
- [x] M3: 记录 BDD 场景并补 RED 用例。
- [x] M4: 实现 IntAuth 文件类别一次性导入能力，且列表继续本地读取。
- [x] M5: 运行定向测试与证据校验。
- [x] M6: 更新任务状态并准备本任务范围内提交。

## Expected Verification

- 导入接口能从 IntAuth `GET /internal/quality-system/file-categories` 读取类别并写入本地 `dcc_file_category`。
- 已存在同名本地类别会被复用，避免丢失目录绑定、权限、分发、培训和审批路线等本地治理挂载。
- `GET /dcc/file-categories` 不依赖 IntAuth 客户端，继续只返回本地类别。
- 缺少 `yudao.dcc.int-auth` 配置、IntAuth 返回非法 payload 或请求失败时，接口明确报错，不做回退。

## Current Status

Completed. DCC 后端已新增一次性 IntAuth 文件类别导入能力，常规列表继续只读本地表；相关 RED/GREEN 证据、后端证据校验和本地数据库对齐核对已完成。

## Blocker And Impact

- Blocker: none currently.
- Impact:
  - 新增了显式一次性导入入口 `POST /dcc/file-categories/import-intauth`。
  - 常规 `GET /dcc/file-categories` 继续只读本地表，不再引入运行时 IntAuth 依赖。
  - 本地数据库已核对为 48 个有效 DCC 类别，与 IntAuth 当前 48 个活跃类别名称集合一致。

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccFileCategoryAdminServiceImplTest,DccIntAuthFileCategoryClientImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260515-dcc-file-category-import-from-intauth\backend-api-evidence.md` -> PASS
- Read-only cross-check:
  - `D:\ProjectPackage\Int\IntAuth\data\auth.db` active file categories: `48`
  - local `ruoyi-vue-pro` MySQL active non-E2E DCC categories: `48`
  - name-set diff: `missing_in_local=[]`, `extra_in_local=[]`
