from pathlib import Path
import json, hashlib
import fitz
from pypdf import PdfReader
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib import colors
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer
root = Path(__file__).resolve().parent
pdfmetrics.registerFont(TTFont("Chinese", "C:/Windows/Fonts/simsun.ttc", subfontIndex=0))
body = ParagraphStyle("Body", fontName="Chinese", fontSize=12, leading=22, spaceAfter=16)
title = ParagraphStyle("Title", parent=body, fontSize=20, leading=30, alignment=1)
warning = ParagraphStyle("Warning", parent=body, textColor=colors.HexColor("#B42318"), fontSize=16, leading=26, alignment=1)
results=[]
for order, cycle, batch in [("881MO083075","1009200645","SIM202611210645"),("881MO097262","1009200646","SIM202611210646"),("881MO088489","1009200647","SIM202611210647")]:
    file=root / f"SIM-{cycle}-{order}-来料检验报告-软件模拟.pdf"
    contents=[("来料检验报告（软件模拟）",title),("软件模拟、非真实实测/质量批准<br/>非真实报告，不可作为真实质量或上市依据",warning),(f"类别：来料检验报告<br/>生产订单：{order}<br/>生产批号：{batch}<br/>本轮周期：{cycle}<br/>环境：本地8081 / 测试1",body),("一、模拟测试用途<br/>仅用于本轮指定订单来料报告附件上传、保存和关联流程验证。此文件为本轮新制模拟附件，不复用旧周期材料，不代表真实来料检验完成。",body),("二、检验与结论边界<br/>未进行真实抽样或实测；不编造来料批号、供应商、检验标准、抽样数量、测量数据或合格结论。模拟附件流程核验不构成真实质量判定。",body),("三、签署与使用限制<br/>不含真实机构签章、检验员签名、审核签名或批准签名；不声明现实检验或批准日期。不得用于实际生产质量、产品上市或追溯合规依据。",body),("四、后续责任<br/>本文件仅为来料类别资料，不替代灭菌、成品检验报告、成品检验记录，不替代PQC生产放行、上市放行或归档。",body)]
    SimpleDocTemplate(str(file),pagesize=(595.28,841.89),leftMargin=45,rightMargin=45,topMargin=40,bottomMargin=40,title=f"{cycle} 来料检验报告 软件模拟").build([Paragraph(text,style) for text,style in contents])
    reader=PdfReader(file,strict=True)
    text="".join(page.extract_text() for page in reader.pages)
    assert len(reader.pages)==1
    assert all(value in text for value in [order,cycle,batch,"软件模拟","非真实实测/质量批准","不可作为真实质量"])
    rendered=fitz.open(file)
    rendered[0].get_pixmap(matrix=fitz.Matrix(1.3,1.3)).save(file.with_suffix(".png"))
    results.append({"order":order,"cycle":cycle,"batch":batch,"file":str(file),"bytes":file.stat().st_size,"pages":1,"strict_parse_and_text":"PASS","sha256":hashlib.sha256(file.read_bytes()).hexdigest()})
(root / "来料前三单-20261121-格式检查.json").write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding="utf-8")
print(json.dumps(results,ensure_ascii=False,indent=2))

