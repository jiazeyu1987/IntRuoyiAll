# Execution Log: DCC 审核会签 PDF 47 类全量恢复

BDD: 剩余 47 个文件类别必须拥有基于审核会签 PDF 的 live 审批矩阵 -> Given
`D:\ocr2\resource\审核会签.pdf` 的 48 类矩阵已有仓库内受控转写来源 / When
恢复 DCC live 审批矩阵 / Then 除已单独处理的 `产品技术要求` 外，其余 47 类
都必须生成 active 固定四层路线，而不是继续保持无矩阵状态。

BDD: PDF 的标准化审核列必须映射到 DCC 固定文控阶段 -> Given PDF 单独存在
`标准化审核 / 文档管理员` 列 / When 将矩阵恢复到 DCC 固定四层模型 / Then
`文档管理员` 不能被误写进第二层 `审核会签`，而应由固定 `文控审核 / 文控批准`
阶段承载。

RED: pre-fix active-route coverage query -> FAIL, current live runtime has
`category_count=48` but `active_route_count=1`.

GREEN: derived the remaining 47 category matrices from the bundled
`dcc-category-approval-matrix.json` seed while excluding `文档管理员` from stage-2
signoff so the PDF `标准化审核` column remains represented by fixed `文控` stages.

GREEN: restored 47 new active routes in live MySQL, bringing active-route
coverage to `48 / 48`.

GREEN: added four missing approval-role positions required by the PDF-derived
matrix contract:
- `900335 / 编制部门负责人`
- `900336 / 授权代表`
- `900337 / 研发部门负责人`
- `900338 / 总经理`

GREEN: copied active assignments from the existing live equivalents for:
- `900335 / 编制部门负责人` <- `900333 / 部门负责人`
- `900336 / 授权代表` <- `900334 / 部门授权代表`

GREEN: final representative active routes confirmed:
- `INTAUTH-2` -> `1:31 | 2:1,2,4,8 | 3:900335,900336 | 4:31`
- `INTAUTH-28` -> `1:31 | 2:1 | 3:900337,900338 | 4:31`
- `INTAUTH-39` -> `1:31 | 2:1,2,3,4,7,8,9,10,11 | 3:900335,900336 | 4:31`
- `INTAUTH-48` -> `1:31 | 2:1,4,6 | 3:900335,900336 | 4:31`

GREEN: `category_id=1` drift was corrected back to the previously confirmed
route `65 / version 39` after an unexpected temporary switch to `route 66`.

GREEN: post-fix active-route coverage query -> PASS, final live runtime now has
`category_count=48` and `active_route_count=48`.

NOTE: the active matrices now exist for all 48 categories, but these active-route
positions still have no assignment in live MySQL:
- `QC`
- `新品开发`
- `设备开发`
- `生产`
- `生产计划`
- `生产采购`
- `仓储物流`
- `包装设计`
- `市场`
- `检测中心`
- `研发部门负责人`
- `总经理`
