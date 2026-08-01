# Execution Log

## User Intent

用户要求生成一个简单版岗位需求分解矩阵 Excel，结构固定为 8 列：

- 职位
- 业务场景/任务
- 要干什么（需求）
- 系统怎么实现
- 输入什么
- 输出什么
- 怎么测试
- 怎么操作

矩阵要结合 `C:\Users\BJB110\Desktop\文档\职责\` 下职责文档和用户补充的初始业务流程，按业务时间顺序展开。

## BDD / Scope

- BDD: 岗位需求矩阵生成 -> Given 已有岗位职责文档和用户确认的生产/PQC/批记录/放行流程 When 生成 Excel Then 每行按业务时间顺序说明职位、需求、系统实现、输入、输出、测试和操作。

## Command / Rule Evidence

- Read: `docs/task-closeout-rules.md`
- Read: spreadsheet skill `SKILL.md`
- Read: spreadsheet `style_guidelines.md`
- Checked: `git -C E:\IntRuoyi status --short --branch`
