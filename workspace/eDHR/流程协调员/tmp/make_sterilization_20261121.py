from pathlib import Path
import json
import hashlib
import fitz
from pypdf import PdfReader
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib import colors
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer

root = Path(__file__).resolve().parents[1]
output = root / 'output' / 'pdf'
output.mkdir(parents=True, exist_ok=True)
pdfmetrics.registerFont(TTFont('SimulationChinese', 'C:/Windows/Fonts/msyh.ttc', subfontIndex=0))
body = ParagraphStyle('Body', fontName='SimulationChinese', fontSize=11, leading=21, spaceAfter=12, wordWrap='CJK')
title = ParagraphStyle('Title', parent=body, fontSize=21, leading=32, textColor=colors.HexColor('#a02020'), alignment=1)
notice = ParagraphStyle('Notice', parent=body, fontSize=14, leading=25, textColor=colors.HexColor('#a02020'))
results = []
for order, cycle, batch, application in [('881MO083075','1009200645','SIM202611210645','339'),('881MO097262','1009200646','SIM202611210646','340'),('881MO088489','1009200647','SIM202611210647','341')]:
    target = output / f'SIM-{cycle}-{order}-sterilization-software-simulation.pdf'
    if target.exists():
        raise FileExistsError(target)
    paragraphs = [
        ('软件模拟 · 灭菌报告', title),
        ('非真实实测／质量批准，不可作为真实质量依据', notice),
        (f'类别：灭菌报告（软件模拟附件）<br/>生产订单：{order}<br/>本轮标识：{cycle}<br/>生产批号：{batch}<br/>关联PQC申请：{application}<br/>限定环境：本地8081／测试1', body),
        ('用途：仅用于本轮正常生产至上市放行流程的软件附件上传与关联验证。本文件独立关联上述订单及批号，不适用于其他周期或订单。', body),
        ('模拟边界：本文件不是实际灭菌报告，不证明已实施灭菌，不包含真实灭菌参数、设备运行记录、实测结果、无菌结论或合格判定。未提供并核实的实际数据不予编造。', body),
        ('责任边界：无真实机构签章、签名或批准；不替代正式生产记录、PQC复核、质量决定及系统要求的身份验证，不掩盖任何未决异常。关联PQC申请编号仅作对象索引，不由本附件证明审批结果。', body),
        ('使用限制：仅供指定测试环境的软件模拟。资料上传不代表质量审批、上市放行或归档；正式状态以系统当前记录为准。', body),
        ('软件模拟，非真实报告记录，非真实实测／质量批准。', notice),
    ]
    SimpleDocTemplate(str(target), title=f'软件模拟灭菌报告-{cycle}', author='软件模拟附件', leftMargin=48, rightMargin=48, topMargin=42, bottomMargin=42).build([Paragraph(text, style) for text, style in paragraphs])
    reader = PdfReader(target, strict=True)
    text = ''.join(page.extract_text() for page in reader.pages)
    for value in [order,cycle,batch,application,'软件模拟','非真实实测','质量批准','灭菌报告']:
        if value not in text:
            raise ValueError(value)
    if len(reader.pages) != 1:
        raise ValueError('Expected one page')
    with fitz.open(target) as rendered:
        rendered[0].get_pixmap(matrix=fitz.Matrix(1.2,1.2)).save(target.with_suffix('.png'))
    results.append({'order':order,'cycle':cycle,'batch':batch,'file':str(target),'pages':1,'bytes':target.stat().st_size,'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'strict_parse':'PASS','object_text':'PASS'})
print(json.dumps(results,ensure_ascii=False,indent=2))
