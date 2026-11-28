$ErrorActionPreference='Stop'
$workspace='D:\ProjectPackage\IntRuoyi\IntRuoyiAll\workspace\协调部\edhr协调员'
$source='C:\Users\BJB110\.openmausbot\task-workspaces\2b231a02-2425-4afd-be40-252432d2b443\60b442b7-2811-41a6-9b57-174cfab7ba34\inbox\niuzhixiao\70d1f6c8-2b39-43ff-9eef-36c340052b19-returned\20261127154650.xlsx'
$evidence=Join-Path $workspace 'output\20261127主链测试证据'
$report=Join-Path $workspace ('report\'+(Get-Date -Format yyyyMMddHHmmss)+'.xlsx')
Copy-Item -LiteralPath $source -Destination $report
$commands=[System.Collections.Generic.List[object]]::new()
function Put($path,$props){$commands.Add(@{command='set';path=$path;props=$props})}
function Case($sheet,$row,$values,$image,$caption){
  $columns=@('A','B','C','D','E')
  for($index=0;$index -lt 5;$index++){Put ('/'+$sheet+'/'+$columns[$index]+$row) @{value=$values[$index];'alignment.wrapText'='true';'alignment.vertical'='top';'font.name'='微软雅黑';'font.size'='11'}}
  Put ('/'+$sheet+'/row['+$row+']') @{height='409'}
  if($image){$commands.Add(@{command='add';parent=('/'+$sheet);type='picture';props=@{src=(Join-Path $evidence $image);anchor=('F'+$row+':G'+($row+1));alt=$caption}})}
}
Put '/' @{title='本地六主链测试：前置阻塞，完整主链均未完成';description='本地测试1；六条主链完成数为零。通过项仅为只读入口检查，非质量批准。'}
Put '/未通过/A2' @{merge='false'}
$path='本地 http://localhost:8081，测试1，以zhangfeihong登录；eDHR批记录、生产执行、生产班长、活跃订单池、查看订单881MO000059；进入生产作业、选择本单、粗洗工序、点击员工姓名、查看全部33名候选，没有张飞红。不得替换其他身份或点击正式提交。'
Case '未通过' 2 @('共同前置01','指定张飞红进行生产','候选没有张飞红，无法按指南选择生产人员。订单881MO000059共912件，粗洗完成量为0。生产前置未通过，不代表下游功能发生故障。',$path,'未改选人员，未提交生产、复核或签名。配置责任人需核实候选资格和关联；具体根因未确定。') '01-员工候选缺少张飞红.png' '本轮生产候选缺少张飞红'
Case '已通过' 5 @('只读核查04','管理员可查到张飞红账号','账号zhangfeihong，姓名张飞红，部门组装2计时，角色列显示生产组长权限角色，状态启用且未锁定。','本地 http://localhost:8081，测试1，yingtaigroupadmin登录；系统管理、用户管理、新增筛选条件、用户名称包含zhangfeihong、查询，读取唯一用户行。','仅核实账号和可见角色，不证明已具备生产员工候选资格；没有修改人员或权限。') '04-张飞红用户角色.png' '本轮管理员只读查询张飞红用户行'
$guides=Get-ChildItem (Join-Path $workspace 'guide') -Filter '*.md'|Sort-Object Name
for($index=0;$index -lt 6;$index++){
  $guide=$guides[$index];$text=Get-Content $guide.FullName -Raw
  $title=($text -split '\r?\n')[0].TrimStart('#',' ')
  $chain=($text -split '## 测试主链')[1].Trim().Replace('→','，然后')
  $row=if($index -lt 3){12+$index}else{5+$index}
  $result=if($index -eq 0){'订单881MO000059已加入，粗洗完成量0，生产未提交；检验、评审、QA让步及后续生产未执行。'}elseif($index -lt 3){'本链尚未建立独立订单，生产及其后全部业务未执行。'}else{'本链未建立独立订单，仅完成偏差共用入口只读检查，正式偏差、调查、整改和后续业务未执行。'}
  $image=if($index -lt 3){'02-订单粗洗未提交.png'}else{$null}
  Case '存疑' $row @(('主链'+($index+1)),$title,$result,('共同阻塞复现：'+$path+' 恢复后按 '+$guide.Name+' 执行，完整预期路径：'+$chain),'六链均要求指定身份生产前置。尚未到达指南终点，不能判下游通过或失败。图片只证明共同阻塞或入口现状，不是本链下游执行证据。') $image '本轮粗洗完成量0，仅作共同阻塞证据'
}
Case '存疑' 15 @('根因待核实','生产候选缺失的原因','账号存在，用户列表显示生产组长权限角色，生产候选中却无本人。尚未核实实际候选筛选规则，不能断言新增某角色就能解决。','先按共同阻塞路径查看生产候选；再以测试1管理员进入系统管理、用户管理，按zhangfeihong筛选。由配置责任人核对人员资格、关联和候选条件。','未扩权、未改配置或代码；原错误身份历史记录未覆盖。配置正确恢复后先查询881MO000059最新状态，再续做；其他链需建立独立对象。') '04-张飞红用户角色.png' '账号存在不等于具备生产候选资格'
$batch=Join-Path $evidence '报告编辑批次.json'
$commands|ConvertTo-Json -Depth 8|Set-Content $batch -Encoding utf8
officecli batch $report --input $batch --json
if($LASTEXITCODE -ne 0){throw '报告编辑失败'}
officecli validate $report --json
if($LASTEXITCODE -ne 0){throw '报告结构校验失败'}
officecli view $report issues --json
if($LASTEXITCODE -ne 0){throw '报告问题检查失败'}
Write-Output ('REPORT_PATH='+$report)
