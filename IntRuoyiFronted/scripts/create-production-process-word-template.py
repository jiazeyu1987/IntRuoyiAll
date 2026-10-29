"""Build the shared production process DOCX template using global OfficeCLI.

Run once when changing the layout; browser exports fill this saved template.
The command refuses to overwrite an existing template.
"""
import json
import subprocess
from pathlib import Path

target = Path(__file__).resolve().parents[1] / 'src/assets/templates/production-process.docx'
target.parent.mkdir(parents=True, exist_ok=True)


def run(*args, data=None):
    result = subprocess.run(['officecli', *map(str, args)], input=data,
                            encoding='utf-8', capture_output=True)
    if result.returncode:
        raise RuntimeError(result.stdout + result.stderr)
    return result.stdout


def batch(commands):
    run('batch', target, '--stop-on-error', data=json.dumps(commands, ensure_ascii=False))


def add(kind, props, parent='/body'):
    return dict(command='add', parent=parent, type=kind, props=props)


def paragraph(text, **props):
    return add('paragraph', dict(text=text, font='宋体', size='11', spaceAfter='6pt', **props))


def table(data, widths):
    return add('table', dict(data=data, colWidths=widths, width='9638', layout='fixed',
                            padding='90', **{'border.all': 'single;4;AAB7B1'}))


run('create', target)
batch([
    dict(command='set', path='/section[1]', props=dict(pageWidth='21cm', pageHeight='29.7cm',
         marginTop='1.8cm', marginBottom='1.8cm', marginLeft='2cm', marginRight='2cm')),
    add('paragraph', dict(text='生产工序解析记录', style='Title', font='宋体', size='20',
                         bold='true', align='center', spaceAfter='10pt')),
    paragraph('来源文件：{sourceFileName}'),
    table('产品名称,{productName};产品编码,{productCode};工序序号,{processNumber} / {processCount};工序名称,{processName};关键工序,{criticalProcess};结构版本,{schemaVersion}', '1800,7838'),
])
run('get', target, '/body/tbl[1]', '--depth', '1')

sections = [
    ('物料信息', '输入与输出物料，共 {materialCount} 项。',
     '方向,物料编码,物料名称,原始编码标签;{#materials}{direction},{code},{name},{sourceCodeLabel}{/materials}', '900,2600,3538,2600'),
    ('设备信息', '设备组共 {groupCount} 组，可选设备共 {equipmentCount} 项。',
     '设备组 / 选择模式,设备编码,设备名称,原始编码标签;{#equipment}{groupNumber} / {selectionMode},{code},{name},{sourceCodeLabel}{/equipment}', '2100,2400,2738,2400'),
    ('工艺参数', '工艺参数共 {parameterCount} 项；空白表示解析数据未提供。',
     '设备组,参数名称,参考值,实际值,录入配置;{#parameters}{groupNumber},{name},{referenceValue},{actualValue},{uiDetails}{/parameters}', '800,2000,1900,1400,3538'),
]
for index, (title, note, data, widths) in enumerate(sections, 2):
    batch([
        add('paragraph', dict(text=title, style='Heading1', font='宋体', size='18', bold='true',
                             color='17634F', spaceBefore='12pt', spaceAfter='6pt', keepNext='true')),
        paragraph(note, keepNext='true'),
        table(data, widths),
    ])
    run('get', target, f'/body/tbl[{index}]', '--depth', '1')
    batch([dict(command='set', path=f'/body/tbl[{index}]/tr[1]', props={'header': 'true'})])

batch([
    dict(command='set', path=f'/body/tbl[{table_index}]/tr[{row}]/tc[{col}]/p[1]',
         props={'font': '宋体', 'size': '10', 'spaceAfter': '3pt'})
    for table_index, (rows, cols) in enumerate([(6, 2), (2, 4), (2, 4), (2, 5)], 1)
    for row in range(1, rows + 1) for col in range(1, cols + 1)
])
batch([
    add('footer', dict(text='生产工序解析记录  |  ', align='center', font='宋体', size='9'), parent='/'),
])
run('add', target, '/footer[1]/p[1]', '--type', 'field', '--prop', 'fieldType=page')
run('close', target)
print(target)
