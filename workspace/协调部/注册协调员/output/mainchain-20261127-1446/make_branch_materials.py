from pathlib import Path
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

output = Path(__file__).parent
pdfmetrics.registerFont(TTFont('Hei', 'C:/Windows/Fonts/simhei.ttf'))
for purpose in ['renewal', 'change']:
    lines = ['模拟资料，仅系统测试，非真实注册证或监管批准', '本轮 SIM-REG-20261127-1446，沿用本线程自有旧样本', '证号 SIM-REG-20261126-1401-CE', '公司及持证主体 上海七木医疗器械有限公司', '项目 IDI，DCC 0313170202，型号 SIM-B，分类 II', '产品 按压式压力泵，英文 Simulated Pressure Pump', '产品代码 SIM-IDI，商品名 SIM-PUMP，公告机构 0123', '委托生产 否，自行生产 是']
    if purpose == 'renewal':
        lines += ['续证批准日期 2026-11-27，有效期至2031-01-20，其他字段不变']
    else:
        lines += ['变更产品名称为 按压式压力泵模拟变更，其余字段不变', '批准日期2026-11-26，有效期至2029-01-20']
    document = canvas.Canvas(str(output / f'previous-CE-{purpose}.pdf'))
    document.setFont('Hei', 11)
    for index, text in enumerate(lines):
        document.drawString(30, 800-index*30, text)
    document.save()
