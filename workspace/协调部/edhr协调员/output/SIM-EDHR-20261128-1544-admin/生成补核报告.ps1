$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$reportFolder = Join-Path $root 'report'
New-Item -ItemType Directory -Path $reportFolder -Force | Out-Null
$stamp = [TimeZoneInfo]::ConvertTimeBySystemTimeZoneId([DateTimeOffset]::UtcNow, 'China Standard Time').ToString('yyyyMMddHHmmss')
$workbook = Join-Path $reportFolder "$stamp.xlsx"
$basePath = '本地 localhost:8081；测试1租户；本轮 SIM-EDHR-20261128-1544。'
$passed = @(
    @('生产组长入口和员工选择','01至03；张飞红','可开始本轮生产准备，不要求员工姓名也叫张飞红。','本人入口与员工下拉已核实；33名候选不等于每工序均适用，新工序仍须选择适用员工。','张飞红登录→生产班长→生产作业→员工选择；不创建旧对象。','06-张飞红账号员工候选.png'),
    @('检验填写入口','01至03；杨珊','已有检验填写和提交操作，可开始后续新对象检验。','角色有任务查询、巡检操作；本人实登可见检验填写及提交。仅查看旧记录，不提交。','杨珊登录→eDHR→生产执行→检验填报。','10-杨珊检验填写入口.png'),
    @('复核和不合格发起','01；吴学燕','已有复核和从原记录发起不合格审查的操作。','本人检验组长列表有详情、复核、不合格审查；角色工作台专用查询及创建权限已勾选。','吴学燕登录→eDHR→生产执行→检验组长→查看本人可见记录按钮。','11-吴学燕复核不合格审查按钮.png'),
    @('生产放行和不合格发起','02、03；朱利江','已有生产放行及原放行申请的不合格审查入口。','角色有放行通过、上传、驳回及不合格评审创建；本人页面显示放行与不合格审查。未操作旧申请。','朱利江登录→eDHR→生产执行→生产放行→查看申请按钮。','12-朱利江放行不合格审查按钮.png'),
    @('不合格处置授权','01、02、06；管理员','已核实具体处置权限，不只是能登录管理员。','租户管理员已勾选不合格评审查询、创建、处置及偏差管理；此前真实候选中包含该账号。新对象签名待到节点验证。','管理员登录→系统管理→角色管理→权限角色→菜单分类→租户管理员→菜单权限；只读取消。','20-管理员实际不合格处置授权.png'),
    @('来料资料上传入口','03；朱月浪','已有来料资料上传入口。','本人列表显示上传，进入旧对象可看到来料检文件类别；未上传任何文件。','朱月浪 zhuyuelang 登录→eDHR→生产执行→上市放行→上传入口；只读查看。','14-朱月浪上传入口.png'),
    @('灭菌资料上传入口','03；丁加美','已有上传入口和对应岗位角色。','本人上市放行列表有上传；对应角色含批次查询、上传及工作任务查询。未上传。','丁加美登录→eDHR→生产执行→上市放行→查看上传按钮。','15-丁加美上传入口.png'),
    @('成品两类资料接收','03；钱凤琴','成品检报告和成品检记录是两类独立任务。','本人工作台已有两个独立类别的旧任务，角色分别具备工作任务及上市放行入口；不把旧任务当本轮成功。','钱凤琴登录→个人工作台→分别查看成品检报告与成品检记录任务类别；不办理。','16-钱凤琴两类资料接收.png'),
    @('上市放行入口','03；宋媛','已有上市放行操作入口。','本人上市放行列表有详情、驳回、放行；角色有上市放行及批次查询。未批准任何旧批次。','宋媛登录→eDHR→生产执行→上市放行→只读列表。','13-宋媛上市放行按钮.png'),
    @('偏差调查岗位入口','04至06；牛志晓','偏差管理入口可用，不追加不合格处置权。','现有偏差管理员角色具有偏差管理及任务查询；本人能查询现有偏差、看到发起入口。','牛志晓登录→生产执行→偏差管理；原管理员角色页面跳转不可用不代表本业务入口不可用。','17-牛志晓偏差实际入口.png'),
    @('偏差核验岗位入口','04至06；王思雨','已完成强制首次改密，实登后偏差入口可用。','约定正式凭据被明确拒绝后，仅尝试一次初始凭据；按页面完成首次改密。查回测试1、王思雨和偏差入口。无重置。','王思雨登录→首次改密→重新登录→核对测试1和本人→生产执行→偏差管理。','18-王思雨改密后偏差入口.png'),
    @('偏差后续职责页面结构','04至06；仅结构核实','调查、核验、部门确认、QA关闭、质量批准节点确实存在。','只读旧偏差详情的处置与签署页，能见五种指定职责。证明页面支持，不证明本轮指定人已获准签署。','管理员→生产执行→偏差管理→既有偏差详情→处置与签署；只读，不更改。','09-偏差既有签署节点.png')
)
$failed = ,@(
    @('签名授权筛选报错','管理员查询；不据此判无签名权','输入指定账号并应用筛选后页面报系统异常。','页面保留旧列表且条件仍待应用，不能当作筛选成功。已重置条件，未改签名授权。','管理员登录→系统管理→电子签名→用户授权→增加用户包含条件→输入 yingtaigroupadmin→回车应用→系统异常。','22-签名授权筛选系统异常.png')
)
$uncertain = @(
    @('部门确认与质量批准的准确授权','04；尚磊、徐建海','未发现可精准补给的既有对应角色；不是已证实业务拒绝。','已查七类权限角色、组织角色及审批角色。二人现有可见入口为文控；偏差管理员为调查专岗、QA角色为另一职责，未乱加角色。后续指定候选及本人办理仍待核实。','管理员→系统管理→角色管理→逐类权限角色、组织角色、审批角色；尚磊或徐建海本人登录→查看菜单；新偏差调查后再检查指定人候选和任务，不要求创建前已有任务。','19-eDHR已有角色清单.png'),
    @('偏差等级名称与管理者节点','05；一般与关键链','页面普通、重大与指南一般、关键的对应关系未确认。','未自行等同；当前无可只读核实的本轮重大对象，管理者代表后续节点尚不能验证。不能据创建窗口断言不存在后续流程。','管理员→生产执行→偏差管理→发起偏差→只读等级选项→取消；新对象后续正式节点再检查管理者代表参与；不创建对象。','03-管理员发起偏差实际字段.png'),
    @('关键偏差转不合格入口','06；转审动作','已见业务限制说明，但未验证当前转审动作。','页面说明仅未关闭的关键偏差可转不合格审批；当前只见已处理普通旧对象，不能把状态不合适当权限拒绝。管理员处置权限已确认。','管理员→生产执行→偏差管理→查看未关闭关键偏差条件及列表；等本轮符合条件对象到节点后检查转审入口、来源和候选，不操作旧对象。','17-牛志晓偏差实际入口.png'),
    @('新对象数据范围和本人签名','01至03；节点检查，不阻止开始','角色授权和本人入口已足够开始，尚不等于整链通过。','新对象创建后才有正式接收、任务和签名；必须分别核对每类资料、实际员工适用范围及审批账号。旧记录按钮仅用于入口证据。','生产组长用 zhangfeihong 创建本轮独立活跃对象→各岗位重新查询该对象→逐节点核对数据可见、候选、任务与签名；发现拒绝只暂停受影响节点。','21-PQC组长具体操作授权.png')
)
$commands = [System.Collections.Generic.List[object]]::new()
$commands.Add(@{command='set';path='/Sheet1';props=@{name='已通过'}})
foreach ($sheet in @('未通过','存疑')) { $commands.Add(@{command='add';parent='/';type='sheet';props=@{name=$sheet}}) }
$groups = @(@{name='已通过';rows=$passed},@{name='未通过';rows=$failed},@{name='存疑';rows=$uncertain})
$headers = @('检查内容','主链及账号','结论和边界','实际依据或未完成原因','完整复现路径','证据截图')
$letters = @('A','B','C','D','E','F')
foreach ($group in $groups) {
    $sheet = $group.name
    $rowNumber = 1
    foreach ($headerIndex in 0..5) {
        $commands.Add(@{command='set';path="/$sheet/$($letters[$headerIndex])1";props=@{value=$headers[$headerIndex];bold='true';fill='17365D';'font.color'='FFFFFF';font='Microsoft YaHei';size='11'}})
    }
    $widths = @(25,25,42,60,65,75)
    foreach ($columnIndex in 0..5) { $commands.Add(@{command='set';path="/$sheet/col[$($letters[$columnIndex])]";props=@{width=[string]$widths[$columnIndex]}}) }
    foreach ($case in $group.rows) {
        $rowNumber++
        foreach ($columnIndex in 0..4) {
            $text = [string]$case[$columnIndex]
            if ($columnIndex -eq 4) { $text = $basePath + $text }
            $commands.Add(@{command='set';path="/$sheet/$($letters[$columnIndex])$rowNumber";props=@{value=$text;wrap='true';'alignment.vertical'='top';font='Microsoft YaHei';size='11'}})
        }
        $commands.Add(@{command='set';path="/$sheet/row[$rowNumber]";props=@{height='225'}})
        $commands.Add(@{command='add';parent="/$sheet";type='picture';props=@{src=(Join-Path $root $case[5]);anchor="F${rowNumber}:G$($rowNumber+1)";alt=$case[0];name="证据$rowNumber"}})
    }
    $commands.Add(@{command='set';path="/$sheet";props=@{freeze='A2';autoFilter="A1:F$rowNumber"}})
}
$batchFile = Join-Path $root '报告排版指令.json'
ConvertTo-Json -InputObject $commands.ToArray() -Depth 10 | Set-Content -LiteralPath $batchFile -Encoding utf8
& officecli create $workbook
if ($LASTEXITCODE -ne 0) { throw '创建工作簿失败' }
& officecli batch $workbook --input $batchFile --stop-on-error --json | Set-Content -LiteralPath (Join-Path $root '报告生成结果.json') -Encoding utf8
if ($LASTEXITCODE -ne 0) { throw '排版失败' }
& officecli close $workbook
if ($LASTEXITCODE -ne 0) { throw '保存工作簿失败' }
& officecli validate $workbook
if ($LASTEXITCODE -ne 0) { throw '结构验证失败' }
Write-Output "REPORT=$workbook"
