# Execution Log

## User Intent

- 用户要求：将前面识别为冲突的班组长业务统一成最新裁定口径。
- 用户裁定：
  - 订单来源不算冲突：ERP 每晚同步到本地生产订单列表，班组长输入纸质订单编号，在本地生产订单列表中过滤搜索并加入活跃订单池。
  - 调拨单同理：ERP 每晚同步到本地调拨单列表，班组长根据纸质调拨单号过滤查询并获取/关联调拨单。
  - 活跃订单是从本地生产订单列表中过滤搜索后加入、可参与报工分配的订单。
  - 工序完成数量按今日新口径：生产订单数量 × 工序生产系数。
  - 异常上报暂时只在活跃订单里上报。
  - FIFO 按活跃订单加入时间。
  - 班组长不能随意创建主设备，应从设备台账选择并绑定到工序。
  - 上下限用于提示、复核、异常判断、审核副本，不覆盖员工原始提交。

## Command Intent And Evidence

- 读取 `change-request-triage` 技能和 `change-contract.md`，按变更裁定记录。
- 读取 `product-requirements-docs` 技能，按产品需求文档更新。
- 读取 `docs/task-closeout-rules.md` 和 `docs/powershell-encoding.md`，按任务记录和 UTF-8 写入规则执行。

## BDD

BDD: 班组长业务口径统一 -> Given 用户裁定 ERP 数据先夜间同步到本地列表、班组长从本地列表加入订单和关联调拨单；When 更新班组长 PRD 和每日操作文档；Then 文档必须统一活跃订单定义、调拨单关联、工序生产系数、活跃订单异常上报、FIFO 排序、设备台账绑定和上下限不覆盖原始提交。

## Verification

- `validate_change_request.py --evidence docs\changes\20260801-team-leader-erp-local-list-unification.md` -> PASS，变更裁定记录有效。
- UTF-8 关键口径检查 -> PASS，`docs/changes/20260801-team-leader-erp-local-list-unification.md`、旧班组长 PRD、生产班组长每日操作文档、生产全流程角色文档均包含：本地生产订单列表、纸质订单编号、夜间同步、本地调拨单列表、纸质调拨单号、活跃订单加入时间、生产系数、设备台账、不覆盖员工原始提交。
- `git diff --check` -> PASS；Git 对旧 PRD 输出 LF/CRLF 规范化 warning，但无空白错误。
- `project-experience-consolidation` 检查：本次为业务口径文档统一，没有形成新的长期工程经验，不新增长期经验文档。


- `task_closeout.py --task-id 20260801-team-leader-mouth-unification --mode preview` -> PASS，无 delete、blocked、warnings。
- `task_closeout.py --task-id 20260801-team-leader-mouth-unification --mode apply` -> PASS，无删除项。
