# 任务：SRM NAS定位 后端增加生产部共享递归范围

## 任务目标

- 将 `SrmNasLocatorServiceImpl` 的正式刷新范围从单一受保护共享扩展为两个共享：
  - `\\172.30.30.4\质量体系文件`
  - `\\172.30.30.4\生产部`
- 状态接口、刷新任务记录、快照遍历和下载前共享校验必须同步采用统一的受保护共享集合。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-edhr-word-import-table-collapse-fix\task.md`
- 状态：`BLOCKED`
- 处理说明：用户切换到 SRM NAS定位 新需求，本轮先显式阻塞上一后端任务，再开展当前任务，避免违反“上一任务未完成不得直接开始新任务”的仓库基线。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。不保留“若生产部失败则仍当作单共享成功”的兼容逻辑。
- `是否从根因和长期维护角度解决`：是。将共享范围收口为统一常量集合，让状态展示、刷新遍历和受保护校验共享同一建模。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 双共享目录与文件进入同一成功快照 -> Given 质量体系文件 与 生产部 两个共享都可遍历 / When 运行刷新任务 / Then 成功快照必须同时包含两个共享根下的目录和文件。`
- `BDD: 生产部共享中不可读目录仅跳过该目录 -> Given 生产部 共享存在 access denied 子目录且用户已批准 readable-only 范围 / When 刷新继续遍历 / Then 仅跳过不可读子目录并保留其它共享内容。`
- `BDD: 状态接口展示双共享范围 -> Given 当前租户查询 NAS定位 状态 / When 返回状态对象 / Then `scopeShare` 必须展示双共享而非旧单共享字符串。`
- `BDD: 状态接口在 RUNNING 时返回阶段性进度 -> Given 双共享刷新任务正在执行 / When 前端轮询状态接口 / Then 响应必须包含当前共享、当前目录与已扫描目录数/文件数，供页面展示真实进度。`

## 里程碑

1. M1：补任务台账与 RED 单测。`COMPLETED`
2. M2：最小实现双共享刷新与统一校验。`COMPLETED`
3. M3：运行后端回归并补证据。`COMPLETED`
4. M4：补 RUNNING 任务阶段性进度回写与状态接口扩展。`COMPLETED`

## 预期验证

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-srm -am "-Dtest=SrmNasLocatorServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## 最终验证结果

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-srm -am "-Dtest=SrmNasLocatorServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS

## 当前结论

- 状态接口 `scopeShare` 已升级为双共享展示：`\\172.30.30.4\质量体系文件；\\172.30.30.4\生产部`。
- 刷新逻辑会分别使用同一服务器、同一账号密码连接两个受保护共享，并把结果合并进同一成功快照。
- 快照 `path/parentPath` 现包含共享前缀，因此下载会先从缓存路径解析目标共享，再按对应 share 回读文件。
- 即使 `生产部` 当前为空共享，根范围仍会被记录并展示；这属于正式范围表达，不是 fallback。
- 触发刷新前会自动将超时未结束的遗留 `RUNNING` 任务显式标记为 `FAILED`，避免服务重启后永久阻塞下一次刷新。
- 当前需要继续补足：运行中任务必须持续回写阶段性进度，避免状态接口只能给前端一个 `RUNNING` 常量。
- 已完成：运行中状态接口会返回当前共享、当前路径、已扫描目录/文件数与共享阶段序号，供前端轮询展示。
