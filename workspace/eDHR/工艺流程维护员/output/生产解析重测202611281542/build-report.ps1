$ErrorActionPreference='Stop'
$root=$PSScriptRoot
$report=Join-Path $root ('report\'+(Get-Date -Format 'yyyyMMddHHmmss')+'.xlsx')
$path='打开本地服务器 http://localhost:8081，测试1租户，页面用户潘鑫。进入eDHR批记录的工艺流程，列表查询与新增查看五条目标路线；切换树形管理，展开待分配并选择目标路线，查找删除入口。删除并重新查询确认后，进入生产编制，按固定映射继续。'
$cases=[System.Collections.Generic.List[object]]::new()
function Add-Case($sheet,$name,$expected,$actual,$image){$cases.Add([pscustomobject]@{sheet=$sheet;name=$name;expected=$expected;actual=$actual;path=$path;image=$image})}
Add-Case '已通过' '环境及页面姓名' '确认本地开发、测试1、潘鑫。' '真实页面显示本地开发、测试1和潘鑫；沿用现有登录，没有重新核验账号字符串。' '03-未选项目操作限制.png'
Add-Case '已通过' '五个指定项目路线基线' '查清旧路线编号与版本。' 'IBPTR、PBAP、XSCX、PTCABC、QSN各有一条路线，编号为项目代码后接-GYLC-0001；全部第一版生效、第二版草稿、零个产品关联。' '01-路线列表.png'
Add-Case '已通过' '未选项目的操作限制' '未选项目不能导入、导出、保存和预览发布。' '四个入口均禁用。恢复草稿可见但未执行恢复旧内容。' '03-未选项目操作限制.png'
Add-Case '已通过' '目标项目占用提示' '目标项目显示已有路线。' '五个候选均显示已有工艺流程；选择IBPTR后导入入口启用，没有上传文件。' '04-项目已占用.png'
Add-Case '未通过' '删除五条旧路线前置' '真实前端删除五条旧测试路线，再查询确认。' '列表仅有编辑、复制、版本；树形详情有发布、修改、查看、版本和移动，没有找到删除入口。未提交删除，旧路线仍在。不能断定根因是权限还是功能设计。' '02-树形详情无删除入口.png'
Add-Case '存疑' '登录账号字符串独立核验' '核对登录账号为panxin。' '姓名潘鑫和测试1已核实，但页面没有显示账号字符串；未退出重登，不计为账号字符串独立验证通过。' '03-未选项目操作限制.png'
$nodes=@('Word导入与工序物料设备参数解析','错误文件和重新导入取消保留','流程名称与车间必填及保存','工序增删改排序和撤销','已有工序复用及重复拦截','输入输出物料搜索修正增删','设备目录复用新建及失败重载','设备组增删单选多选及开关恢复','参数增删改及缺项非法配置','已有工艺带入与编辑限制','逐工序试填预览与填写校验','保存草稿刷新恢复及替换确认','导出Word并重新导入完整恢复','待修正项定位与补正','复用确认取消返回与重新预览','工序设备工艺流程四页签预览','发布缺项拦截及取消不建档','确认发布结果编号与重复建档检查','配置并发布进入本次候选版本','逐道核对候选版本内容一致性','退出重开确认本轮流程终点','范围外资料前后对照')
foreach($node in $nodes){Add-Case '存疑' $node '依指南在本轮新建测试内容上完成并核验。' '未执行。规定先删除五条旧路线，当前没有前端删除入口，因此没有开始依赖此前置的新一轮导入、编辑或创建。覆盖五个指定项目，不以历史结果代替本轮通过。截图仅说明共同前置阻塞，不代表本节点已经执行。' '02-树形详情无删除入口.png'}
foreach($case in $cases){$case.path+='当前节点：'+$case.name+'。项目与来源对应：IBPTR用ID，PBAP用IDE，XSCX用IDI，PTCABC用IDML，QSN用IDPR。源文件在C:\Users\BJB110\Desktop\初始化文件包\产品中，各代码目录下的生产记录；ID、IDE、IDML为doc，IDI、IDPR为docx。'}
$cases | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $root 'cases.json') -Encoding utf8
officecli create $report
if($LASTEXITCODE -ne 0){throw '报告创建失败'}
$ops=[System.Collections.Generic.List[object]]::new()
$ops.Add(@{command='set';path='/Sheet1';props=@{name='已通过'}})
foreach($sheet in @('未通过','存疑')){$ops.Add(@{command='add';parent='/';type='sheet';props=@{name=$sheet}})}
foreach($sheet in @('已通过','未通过','存疑')){
 $ops.Add(@{command='set';path="/$sheet";props=@{freeze='A2';zoom='60';gridlines='false'}})
 $headers=@('测试内容','预期结果','本轮实际结果','完整测试路径','证据截图')
 for($index=0;$index -lt 5;$index++){
  $column=[string][char](65+$index)
  $ops.Add(@{command='set';path="/$sheet/${column}1";props=@{value=$headers[$index];bold='true';fill='16324F';'font.color'='FFFFFF';'font.name'='Calibri';wrapText='true'}})
  $ops.Add(@{command='set';path="/$sheet/col[$column]";props=@{width=[string]@(29,44,72,84,100)[$index]}})
 }
 $rowNumber=2
 foreach($case in @($cases | Where-Object sheet -EQ $sheet)){
  $values=@($case.name,$case.expected,$case.actual,$case.path,'')
  for($index=0;$index -lt 5;$index++){
   $column=[string][char](65+$index)
   $ops.Add(@{command='set';path="/$sheet/$column$rowNumber";props=@{value=$values[$index];wrapText='true';'alignment.vertical'='top';'font.name'='Calibri';'font.size'='11'}})
  }
  $ops.Add(@{command='set';path="/$sheet/row[$rowNumber]";props=@{height='260'}})
  $nextRow=$rowNumber+1
  $ops.Add(@{command='add';parent="/$sheet";type='picture';props=@{src=(Join-Path $root ('evidence\'+$case.image));anchor="E${rowNumber}:F${nextRow}";alt=$case.name}})
  $rowNumber++
 }
}
$batch=Join-Path $root 'report-operations.json'
$ops | ConvertTo-Json -Depth 8 | Set-Content $batch -Encoding utf8
officecli batch $report --input $batch --json | Set-Content (Join-Path $root 'report-generation.json') -Encoding utf8
if($LASTEXITCODE -ne 0){throw '报告填充失败'}
officecli validate $report | Set-Content (Join-Path $root 'report-validation.txt') -Encoding utf8
if($LASTEXITCODE -ne 0){throw '报告校验失败'}
officecli view $report html | Set-Content (Join-Path $root 'report-html.txt') -Encoding utf8
if($LASTEXITCODE -ne 0){throw '报告预览失败'}
officecli close $report
if($LASTEXITCODE -ne 0){throw '报告保存失败'}
$report | Set-Content (Join-Path $root 'report-path.txt') -Encoding utf8
$cases | Group-Object sheet | Select-Object Name,Count | Format-Table
Write-Output $report
