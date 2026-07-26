# Execution Log

## User Intent

- 在“工序开始”节点左侧固定增加“批记录附件”页签。
- 点击后右侧显示 4 个流程负责人选择：来料检报告、灭菌报告、成品检报告、成品检记录。
- 默认创建 4 个权限角色：来料检报告上传1、灭菌报告上传1、成品检报告上传1、成品检记录上传1。
- 每个角色随机分配 2-4 个用户，用户来源只能是当前租户启用用户。
- 该能力只作用于“工序开始”节点。

## Environment Preflight

- Branch: `int_main`
- Remote: `origin https://github.com/jiazeyu1987/IntRuoyiAll.git`
- Initial status: `## int_main...origin/int_main [ahead 9]`
- Dirty worktree: no tracked/untracked files shown by initial `git status --short --branch`.

## BDD

- BDD: 工序开始批记录附件入口 -> Given 用户在路线流程图选中“工序开始”节点，When 查看左侧固定页签，Then 只能在该节点看到“批记录附件”入口并可打开右侧配置。
- BDD: 四项附件负责人配置 -> Given 用户打开“批记录附件”，When 查看右侧配置区，Then 系统展示来料检报告、灭菌报告、成品检报告、成品检记录 4 项及对应默认上传角色。
- BDD: 当前租户启用用户随机授权 -> Given 当前租户存在至少 2 个启用用户，When 初始化默认上传角色，Then 每个角色只分配当前租户启用用户且人数为 2-4。
- BDD: 启用用户不足失败 -> Given 当前租户启用用户少于 2 个，When 初始化默认上传角色，Then 初始化失败并返回明确错误，不使用停用用户或其他租户用户。
- BDD: 路线配置持久化 -> Given 用户保存路线配置，When 重新打开候选路线或发布版本，Then 4 项批记录附件负责人配置保持一致。

## RED/GREEN Evidence

- Pending.

## Blockers

- None at task start.
