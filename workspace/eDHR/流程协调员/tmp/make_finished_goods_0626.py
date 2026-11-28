from pathlib import Path
import hashlib
import json
import fitz
from pypdf import PdfReader
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph

ROOT = Path(__file__).resolve().parents[1]
DESTINATION = ROOT / 'output/full-market-validation-20261118/documents'
EVIDENCE = ROOT / 'output/full-market-validation-20261118/finished-goods-validation'
WORK_ORDER = 'SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891'
DISCLAIMER = '模拟测试附件、非真实检验报告／记录、不可作为真实质量依据'
DESTINATION.mkdir(parents=True, exist_ok=True)
EVIDENCE.mkdir(parents=True, exist_ok=True)
pdfmetrics.registerFont(TTFont('SimSun', r'C:\Windows\Fonts\simsun.ttc', subfontIndex=0))
body = ParagraphStyle('Body', fontName='SimSun', fontSize=10.5, leading=16, wordWrap='CJK', spaceAfter=7)
title = ParagraphStyle('Title', parent=body, fontSize=19, leading=25, alignment=1, spaceAfter=12)
warning = ParagraphStyle('Warning', parent=body, textColor=colors.HexColor('#A12020'), fontSize=12, leading=18, alignment=1, spaceAfter=13)
heading = ParagraphStyle('Heading', parent=body, fontSize=12, leading=18, spaceBefore=5)
results = []
for category in ['成品检验报告', '成品检验记录']:
    destination = DESTINATION / f'SIM-1009200626-{category}-模拟测试.pdf'
    if destination.exists():
        raise FileExistsError(f'禁止覆盖已有附件：{destination}')
    story = [Paragraph(f'{category} - 模拟测试', title), Paragraph(DISCLAIMER, warning)]
    sections = [
        ('一、测试对象', [
            '环境：本地 localhost:8081；租户：测试1。',
            '本轮活跃订单／周期：1009200626；型号：AI30；本轮数量：5000件。',
            f'完整工单号：{WORK_ORDER}',
            f'附件类别：{category}；文档标识：SIM-1009200626-{category}。']),
        ('二、来源与用途', [
            '本附件为本轮完整上市放行流程模拟测试新编制材料，仅用于验证附件类别、对象关联与后续上传路径，不复用0625周期文件、签名或完成状态。',
            '对象与进度信息来自流程协调交接，非编制岗位独立页面核实；生产16条已复核、检验14条已复核及双100%均仅为交接陈述，不构成真实检验依据。']),
        ('三、本类别内容与结论边界', [
            f'本模拟{category}不载入真实检验项目、实测值、机构证明、签章、亲签或合格结论；未实施真实成品检验，不据此判定产品质量，不将附件编制视为检验执行或复核完成。']),
        ('四、交接异常披露（未经本人独立核实）', [
            '交接披露生产两处数量5501、参数0/1001，以及粗洗余501未跨单分配。以上均来自交接，非本人独立核实；未推断差额去向、未掩盖异常，未追认真实质量。',
            '上述事项保留供有权岗位按正式流程核对与处置；本附件不替代异常处置、数量确认或质量决定。']),
        ('五、状态与使用限制', [
            '本附件仅为本地准备件，未上传、未提交业务系统、未批准。接单时PQC生产放行尚未确认，本附件不证明后续放行结果。',
            '后续仅在PQC正式放行及本类别责任指派核实后，按接续任务办理上传；成品资料岗位不执行质量审批、生产放行、上市放行或归档。',
            '无真人亲签、无真实实测、无真实质量批准；不得作为真实质量依据。'])]
    for label, paragraphs in sections:
        story.append(Paragraph(label, heading))
        story.extend(Paragraph(text, body) for text in paragraphs)

    def footer(canvas, document):
        canvas.setFont('SimSun', 9)
        canvas.setFillColor(colors.HexColor('#A12020'))
        canvas.drawCentredString(A4[0] / 2, 23, f'模拟测试 / 周期1009200626 / {category} / 第{document.page}页')

    SimpleDocTemplate(str(destination), pagesize=A4, rightMargin=42, leftMargin=42, topMargin=30, bottomMargin=39, title=f'SIM-1009200626 {category} 模拟测试', author='成品资料岗位（模拟附件编制，非亲签）').build(story, onFirstPage=footer, onLaterPages=footer)
    reader = PdfReader(destination, strict=True)
    text = '\n'.join(page.extract_text() for page in reader.pages)
    for fragment in [DISCLAIMER, WORK_ORDER, '1009200626', 'AI30', '5000', category, '5501', '0/1001', '501', '非本人独立核实', '未上传']:
        if fragment not in text:
            raise ValueError(f'必要文字缺失：{fragment}')
    if not destination.read_bytes().startswith(b'%PDF-'):
        raise ValueError('不是有效PDF文件')
    pdf = fitz.open(destination)
    images = []
    for index, page in enumerate(pdf):
        image = EVIDENCE / f'{destination.stem}-page-{index + 1}.png'
        page.get_pixmap(matrix=fitz.Matrix(1.4, 1.4)).save(image)
        images.append(str(image))
    results.append({'category': category, 'path': str(destination), 'pages': len(reader.pages), 'bytes': destination.stat().st_size, 'sha256': hashlib.sha256(destination.read_bytes()).hexdigest(), 'required_text_checks': 'passed', 'renders': images})
(EVIDENCE / 'finished-goods-0626-format-validation.json').write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps(results, ensure_ascii=False, indent=2))
