from pathlib import Path
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
import fitz

output = Path(__file__).parent
pdfmetrics.registerFont(TTFont('SimHeiEmbedded', 'C:/Windows/Fonts/simhei.ttf'))
for purpose in ['certificate', 'attachment']:
    target = output / f'SIM-REG-20261126-1401-CE-{purpose}-R1.pdf'
    document = canvas.Canvas(str(target))
    document.setFont('SimHeiEmbedded', 12)
    lines = [
        '模拟测试资料 / SIMULATED TEST ONLY',
        '非真实注册证、非监管批准、不可用于临床或经营',
        '补正关联：申请701；正文显示验证；R1嵌入字体',
        f'材料用途：{purpose}',
        '公司及持证主体：上海七木医疗器械有限公司',
        '项目：IDI；DCC：0313170202；产品：按压式压力泵',
        '注册证号：SIM-REG-20261126-1401-CE',
        '分类：II；型号：SIM-A',
        '批准日期：2026-11-25；有效期至：2027-01-20',
        '英文名：Simulated Pressure Pump',
        '产品代码：SIM-IDI；商品名：SIM-PUMP；公告机构：0123',
        'Rev.0；签发：2026-11-25；自产：是；委托生产：否',
        '仅验证系统材料流转，不用于临床。',
    ]
    for index, line in enumerate(lines):
        document.drawString(36, 800 - index * 34, line)
    document.save()
    preview = fitz.open(target)
    assert 'SIMULATED TEST ONLY' in preview[0].get_text()
    preview[0].get_pixmap().save(str(target.with_suffix('.png')))
    print(target.name, target.stat().st_size)
