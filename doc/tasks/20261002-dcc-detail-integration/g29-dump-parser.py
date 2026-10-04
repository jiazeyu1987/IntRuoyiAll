"""Strict fixed mysqldump --skip-extended-insert reader; no SQL execution or data rewriting."""
import importlib.util,re
from pathlib import Path
HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('g29_frozen_schema_helpers',HERE/'g21_structure_prerequisites.py');base=importlib.util.module_from_spec(spec);spec.loader.exec_module(base)

def require(ok,message):
 if not ok:raise ValueError(message)

def parse_schema(text,baseline):
 result={}
 for match in re.finditer(r'(?m)^CREATE TABLE(?: IF NOT EXISTS)? `([A-Za-z0-9_]+)`\s*\(',text):
  table=match.group(1);require(table in baseline and table not in result,'schema table scope duplicate/unknown')
  end=base.end_parenthesis(text,match.end()-1);columns=[];generated=[]
  for part in base.split_body(text[match.end():end]):
   field=re.match(r'^`([A-Za-z0-9_]+)`\s+([a-z]+)',part,re.I)
   if field:
    name=field.group(1);require(name not in columns,'schema column duplicate');columns.append(name)
    if re.search(r'\bGENERATED\s+ALWAYS\s+AS\s*\(',part,re.I):generated.append(name)
   else:require(re.match(r'^(?:PRIMARY KEY|UNIQUE KEY|KEY|CONSTRAINT|CHECK)\b',part,re.I),'unsupported actual mysqldump schema declaration')
  require(columns==baseline[table]['columns'],'schema ordered columns differ from actual baseline')
  result[table]={'columns':columns,'generatedColumns':generated,'insertColumns':[c for c in columns if c not in generated]}
 require(set(result)==set(baseline),'schema table definition coverage incomplete')
 return result

def quoted(text,start):
 require(start<len(text) and text[start]=="'",'expected mysqldump string literal')
 i=start+1
 while i<len(text):
  char=text[i]
  require(char not in '\r\n','physical newline in single-row dump literal')
  if char=='\\':
   require(i+1<len(text),'truncated escaped literal');i+=2;continue
  if char=="'":
   if i+1<len(text) and text[i+1]=="'":i+=2;continue
   return i+1
  i+=1
 raise ValueError('unterminated mysqldump literal')

def value_end(text,start):
 i=start
 while i<len(text) and text[i].isspace():i+=1
 require(i<len(text),'missing dump value')
 if text[i]=="'":return quoted(text,i)
 prefix=re.match(r'(?:_[A-Za-z0-9]+|[bBxX])\s*(?=\')',text[i:])
 if prefix:return quoted(text,i+prefix.end())
 scalar=re.match(r'(?:NULL\b|0[xX][0-9A-Fa-f]+|[-+]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][-+]?\d+)?)',text[i:])
 require(scalar is not None,'unsupported dump literal form');return i+scalar.end()

def insert_table(line,schema):
 require('\n' not in line.rstrip('\r\n') and '\r' not in line.rstrip('\r\n'),'one physical INSERT line required')
 match=re.match(r'^INSERT INTO `([A-Za-z0-9_]+)`\s*(?:\(([^()]*)\)\s*)?VALUES\s*\(',line)
 require(match is not None,'unsupported mysqldump INSERT syntax');table=match.group(1);require(table in schema,'dump table outside exact protected scope')
 shape=schema[table];raw_columns=match.group(2)
 if raw_columns is None:require(not shape['generatedColumns'],'generated columns require explicit INSERT column projection');columns=shape['columns']
 else:
  names=raw_columns.split(',');columns=[]
  for name in names:
   field=re.fullmatch(r'\s*`([A-Za-z0-9_]+)`\s*',name);require(field is not None,'explicit column syntax invalid');columns.append(field.group(1))
  require(len(columns)==len(set(columns)) and columns==shape['insertColumns'],'explicit ordered non-generated column projection differs')
 i=match.end();values=0
 while True:
  i=value_end(line,i);values+=1
  while i<len(line) and line[i].isspace():i+=1
  require(i<len(line),'truncated single-row INSERT')
  if line[i]==',':i+=1;continue
  require(line[i]==')','dump value separator invalid');i+=1;break
 require(values==len(columns),'dump single-row value arity differs from column projection')
 require(re.fullmatch(r'\s*;\s*',line[i:]) is not None,'multirow/trailing statement/unterminated INSERT forbidden')
 return table

def scan_data(stream,schema):
 counts={t:0 for t in schema};markers=[];ended=False
 for line in stream:
  marker=re.match(r'^-- Dumping data for table `([A-Za-z0-9_]+)`',line)
  if marker:
   require(marker.group(1) in schema and marker.group(1) not in markers,'data table marker duplicate/unknown');markers.append(marker.group(1))
  if line.startswith('INSERT INTO '):
   table=insert_table(line,schema);require(table in markers and markers[-1]==table,'INSERT must belong to current exact table marker');counts[table]+=1
  require(not re.match(r'^(?:CREATE|ALTER|DROP|REPLACE|UPDATE|DELETE|TRUNCATE)\s',line),'data dump has schema/business mutation outside fixed INSERT profile')
  if line.startswith('-- Dump completed on '):ended=True
 require(ended and set(markers)==set(schema),'complete exact data dump markers required')
 return counts
