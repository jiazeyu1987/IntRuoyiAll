# G56 项目文件检出导航

实际 RED 依据：真实前端 r4/026 从项目271/文件夹2的文件4026详情进入检出/检入，将服务端内部存储映射913876传给物理目录读取，返回 Controlled file directory does not exist；检出本身随后由真实页面成功，不把导航错误吞掉。

Given 正式文件投影有完整项目代码身份及逻辑文件夹身份，When 进入所选文件检出/检入，Then 使用现有版本操作页的正式文件、Master、编号精确定位，不把内部存储映射传作物理目录身份。

Given 项目投影任一身份缺失或错误，When 构造导航，Then 明确拒绝，不推测另一个身份、不退回目录读取。

Given 历史文件没有项目文件夹投影，When 导航，Then 保留现有真实物理目录定位及精确文件/Master校验。

边界：不修改目录服务、不吞 getDirectory 错误、不授予新权限、不改变所选正文或锁。既有合法列表读取仍由后端执行权限校验，精确版本选择须同时匹配正式文件及Master。

Owner root，仅 shared/working-browser-navigation.ts、workflow.ts 的位置字段类型、dcc-working-browser-navigation.test.cjs；Java owner独占Query/VO补正式详情位置事实。前端owner的培训待办代码不在本修复内。定向RED/GREEN后统一最终类型检查及构建。

经实际r4/040进一步发现旧详情并未返回projectFolderId，不能从绑定项目猜项目文件夹。最终位置合同：正常详情必有hasProjectStorageMapping严格bool；true为当前File自己的真实placement，同tenant/project/folder/storage验证后附folderId/Name；false明确正常历史physical位置。已绑定项目但没有placement的合法物理历史仍走物理目录；坏placement/当前mapped目录缺placement由BE明确拒绝，不转false。helper不允许unknown bool、true缺身份、false却带folder身份。当前运行旧Jar未含新字段，实际检入待新包，不以测试替代。

有效RED1两项失败→GREEN13；新增合同RED预期缺严格bool拒绝及合法bound-physical被误猜失败→GREEN；日志分开归档，不累加早轮数量。定向工作版本操作回归一次，最终types/build待本批源码冻结。

G58实际r7/021–022补证：正确项目位置不传物理目录后，原scope=current仍触发现有“请选择受控浏览目录”前置，未读取文件。项目位置必须明确选择既有global读取范围，以实际编号及所选File/Master定位；历史物理位置保留current目录范围。现有服务权限与精确版本匹配不变，不把无目录读取失败吞掉。新增真实政策RED为current应global，修复后原29操作回归验证；新前端types/build随G58冻结后执行。
