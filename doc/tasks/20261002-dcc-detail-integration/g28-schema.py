"""G28 exact standalone three-sidecar MySQL contract. Offline, no transport/network/writes."""
import argparse
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import re

HERE=Path(__file__).resolve().parent
REPO=HERE.parents[2]
MIGRATION='20261003_dcc_legacy_source_name_occupancy'
SQL_SHA='621fe041064ac07c5bfe995abd556c3f9436b0253c883a3b7d2b33014e68de2a'
DATABASE='ruoyi-vue-pro'
UUID='92ca05d0-aec8-11f1-a944-02b4e226a5ef'
TABLES=['dcc_legacy_source_name_scope','dcc_legacy_source_name_evidence','dcc_source_name_reservation']
spec=importlib.util.spec_from_file_location('g21_postflight_frozen',HERE/'g21-postflight-schema.py')
base=importlib.util.module_from_spec(spec);spec.loader.exec_module(base)

def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def require(condition,message):
    if not condition:raise ValueError(message)

class CheckParser:
    def __init__(self,text):
        text=str(text)
        pattern=r"\s*((?:_[A-Za-z0-9]+)?'(?:''|\\.|[^'\\])*'|`[^`]+`|[A-Za-z_][A-Za-z0-9_]*|[0-9]+|<>|!=|<=|>=|[(),=<>])"
        self.tokens=[];at=0
        while at<len(text):
            match=re.match(pattern,text[at:])
            if not match:
                require(not text[at:].strip(),'unsupported CHECK token')
                break
            token=match.group(1)
            if re.match(r"^_[A-Za-z0-9]+'",token):token=token[token.index("'"):]
            if token.startswith("'"):
                payload=token[1:-1].replace("''","'").replace("\\'","'").replace('\\\\','\\')
                token="'"+payload.replace("'","''")+"'"
            self.tokens.append(token if token.startswith("'") else token.strip('`').lower());at+=match.end()
        self.at=0
    def peek(self):return self.tokens[self.at] if self.at<len(self.tokens) else None
    def eat(self,want=None):
        token=self.peek();require(token is not None and (want is None or token==want),'CHECK expected '+str(want));self.at+=1;return token
    def expr(self,minimum=0):
        token=self.eat()
        if token=='(':
            value=self.expr();self.eat(')')
        elif self.peek()=='(':
            self.eat('(');args=[]
            if self.peek()!=')':
                args.append(self.expr())
                while self.peek()==',':self.eat(',');args.append(self.expr())
            self.eat(')');value=['op','regexp',*args] if token=='regexp_like' and len(args)==2 else ['function',token,args]
        else:value=['literal' if token.startswith("'") or token.isdigit() else 'identifier',token]
        precedence={'or':1,'and':2,'=':3,'<>':3,'!=':3,'>':3,'<':3,'>=':3,'<=':3,'is':3,'in':3,'regexp':3}
        while self.peek() in precedence and precedence[self.peek()]>=minimum:
            op=self.eat()
            if op=='is':
                negate=self.peek()=='not'
                if negate:self.eat('not')
                self.eat('null');value=['notNull' if negate else 'isNull',value]
            elif op=='in':
                self.eat('(');values=[self.expr()]
                while self.peek()==',':self.eat(',');values.append(self.expr())
                self.eat(')');value=['in',value,values]
            else:value=['op','<>' if op=='!=' else op,value,self.expr(precedence[op]+1)]
        return value

def check_ast(text):
    p=CheckParser(text);value=p.expr();require(p.peek() is None,'unsupported CHECK tail');return value

def prepare_contract():
    sql_path=REPO/'IntRuoyiBackend/sql/mysql'/f'{MIGRATION}.sql'
    require(sha(sql_path)==SQL_SHA,'standalone SQL drift')
    text=sql_path.read_text(encoding='utf-8');tables=base.parse_create_tables(text)
    require(list(tables)==TABLES,'exact sidecar names required')
    for name,table in tables.items():
        match=re.search(r'CREATE TABLE IF NOT EXISTS '+name+r'\s*\(',text);end=base.base.end_parenthesis(text,match.end()-1)
        parts=base.base.split_body(text[match.end():end]);checks=[]
        for part in parts:
            if re.match(r'CHECK\s*\(',part,re.I):
                checks.append(check_ast(part[part.index('(')+1:-1]));continue
            column=re.match(r'(\w+)\s+',part)
            if column and column.group(1) in table['columns']:
                shape=table['columns'][column.group(1)]
                precision=re.search(r'DATETIME\((\d+)\)',part,re.I);shape['precision']=int(precision.group(1)) if precision else None
                if 'CURRENT_TIMESTAMP(6)' in part.upper():shape['default']='CURRENT_TIMESTAMP(6)'
        table['checks']=checks;table['table']['rowFormat']='Dynamic'
        for ordinal,column in enumerate(table['columns'].values(),1):column.setdefault('precision',None);column['ordinal']=ordinal
        table.pop('anonymousIndexes')
    require(sum(len(t['columns']) for t in tables.values())==70,'70 exact columns required')
    return {'version':'G28-SCHEMA-1','sqlSha256':SQL_SHA,'migrationId':MIGRATION,'database':DATABASE,'serverUuid':UUID,'mysqlVersion':'8.0.40','tables':tables}

def validate_facts(contract,rows,phase,*,database=DATABASE):
    require(phase in {'pre','post'},'phase must be pre/post')
    require(database in {DATABASE,'dcc_intqms_g18_rehearsal'},'Root-selected existing local database required')
    runtime=[r for r in rows if r.get('kind')=='runtime']
    require(len(runtime)==1,'one real runtime row required');r=runtime[0]
    require(r.get('database')==database and r.get('serverUuid')==UUID and r.get('mysqlVersion')=='8.0.40'
            and r.get('pageSize')==16384 and str(r.get('rowFormat')).lower()=='dynamic','actual source identity/page/rowformat differs')
    require({'STRICT_TRANS_TABLES','NO_ENGINE_SUBSTITUTION'}<=set(r.get('sqlMode','').split(',')),'strict actual sqlMode required')
    ledger=[r for r in rows if r.get('kind')=='ledger']
    require(len(ledger)<=1,'ambiguous migration ledger')
    if ledger:require(ledger[0].get('migrationId')==MIGRATION and ledger[0].get('sha256')==SQL_SHA and ledger[0].get('status')=='APPLIED' and ledger[0].get('environment')=='test' and ledger[0].get('deleted')==0,'wrong/partial ledger blocks any execution')
    present={r.get('table') for r in rows if r.get('kind')=='table'}
    require(present==set(TABLES) or not present,'partial sidecar creation requires manual reviewed recovery')
    require(all(r.get('kind') in {'runtime','ledger','table','column','index','check','row_count'} for r in rows),'unknown schema fact')
    if not present:
        require(phase=='pre' and not ledger and len(rows)==1,'absent schema cannot have ledger/other shape facts');return 'FIRST_REQUIRED'
    for name,expected in contract['tables'].items():
        table_rows=[r for r in rows if r.get('kind')=='table' and r.get('table')==name]
        require(len(table_rows)==1,'duplicate/missing table fact')
        actual=table_rows[0]
        require(all(actual.get(k)==v for k,v in expected['table'].items()),'table engine/charset/collation/rowformat drift: '+name)
        columns=[r for r in rows if r.get('kind')=='column' and r.get('table')==name]
        require(len(columns)==len(expected['columns']) and {r.get('name') for r in columns}==set(expected['columns']),'exact column coverage differs: '+name)
        for column in columns:
            wanted=expected['columns'][column['name']]
            require(all(column.get(k)==v for k,v in wanted.items() if k not in {'default','expression','type','extra'}),'column shape differs: '+name+'.'+column['name'])
            actual_extra=column.get('extra') if isinstance(column.get('extra'),dict) else base.base.normalize_extra(column.get('extra'))
            require(base.base.normalize_type(column.get('type'))==wanted['type'] and actual_extra==wanted['extra'],'column type/extra differs')
            require(str(column.get('default')).lower()==str(wanted['default']).lower(),'column default differs')
            require(base.expression_ast(column.get('expression') or '')==base.expression_ast(wanted['expression']),'generated source AST differs')
        indexes=[r for r in rows if r.get('kind')=='index' and r.get('table')==name]
        grouped={}
        for index in indexes:
            require(index.get('prefix') is None and index.get('expression') is None and index.get('visible')=='YES','index prefix/expression/visibility differs')
            grouped.setdefault(index.get('name'),[]).append(index)
        require(set(grouped)==set(expected['indexes']),'exact index names differ')
        for index,shape in expected['indexes'].items():
            parts=sorted(grouped[index],key=lambda r:r.get('ordinal',0))
            require(len(parts)==len(shape['columns']),'exact index arity differs')
            for ordinal,(part,column) in enumerate(zip(parts,shape['columns']),1):
                require(part.get('ordinal')==ordinal and part.get('unique')==shape['unique'] and part.get('type')==shape['type'] and all(part.get(k)==v for k,v in column.items()),'index ordered shape differs')
        checks=[r for r in rows if r.get('kind')=='check' and r.get('table')==name]
        require(len(checks)==len(expected['checks']) and all(c.get('enforced')=='YES' for c in checks),'CHECK count/enforcement differs')
        require(sorted(json.dumps(check_ast(c['expression']),sort_keys=True) for c in checks)==sorted(json.dumps(c,sort_keys=True) for c in expected['checks']),'CHECK semantic AST differs')
        counts=[r for r in rows if r.get('kind')=='row_count' and r.get('table')==name]
        require(len(counts)==1 and counts[0].get('count')==0,'sidecar must remain empty; activation is outside DDL scope')
    require(not {r.get('table') for r in rows if 'table' in r}-set(TABLES),'unexpected table facts')
    require(ledger,'existing schema without valid ledger is partial execution, not repeat-ready')
    return 'REPEAT_ALLOWED' if phase=='pre' else 'POSTFLIGHT_PASS'

def fixture(contract,absent=False):
    rows=[{'kind':'runtime','database':DATABASE,'serverUuid':UUID,'mysqlVersion':'8.0.40','pageSize':16384,'rowFormat':'dynamic','sqlMode':'STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION'}]
    if absent:return rows
    for name,t in contract['tables'].items():
        rows.append({'kind':'table','table':name,**t['table']})
        for field,shape in t['columns'].items():rows.append({'kind':'column','table':name,'name':field,**copy.deepcopy(shape)})
        for index,shape in t['indexes'].items():
            for i,column in enumerate(shape['columns'],1):rows.append({'kind':'index','table':name,'name':index,'unique':shape['unique'],'type':shape['type'],'visible':'YES','ordinal':i,**copy.deepcopy(column)})
        # fixture uses source CHECK clauses to exercise parsing rather than serializing AST into SQL.
        source=(REPO/'IntRuoyiBackend/sql/mysql'/f'{MIGRATION}.sql').read_text(encoding='utf-8');match=re.search('CREATE TABLE IF NOT EXISTS '+name+r'\s*\(',source);end=base.base.end_parenthesis(source,match.end()-1)
        for part in base.base.split_body(source[match.end():end]):
            if part.startswith('CHECK'):rows.append({'kind':'check','table':name,'expression':part[part.index('(')+1:-1],'enforced':'YES'})
        rows.append({'kind':'row_count','table':name,'count':0})
    rows.append({'kind':'ledger','migrationId':MIGRATION,'sha256':SQL_SHA,'status':'APPLIED','environment':'test','deleted':0})
    return rows

def capture_sql():
    tables=','.join("'"+t+"'" for t in TABLES)
    return f"""SELECT JSON_OBJECT('kind','runtime','database',DATABASE(),'serverUuid',@@server_uuid,'mysqlVersion',VERSION(),'pageSize',@@innodb_page_size,'rowFormat',@@innodb_default_row_format,'sqlMode',@@session.sql_mode);
SELECT JSON_OBJECT('kind','ledger','migrationId',migration_id,'sha256',sha256,'status',status,'environment',target_environment,'deleted',deleted+0) FROM infra_release_migration WHERE migration_id='{MIGRATION}';
SELECT JSON_OBJECT('kind','table','table',TABLE_NAME,'engine',ENGINE,'charset',SUBSTRING_INDEX(TABLE_COLLATION,'_',1),'collation',TABLE_COLLATION,'rowFormat',ROW_FORMAT) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ({tables}) ORDER BY TABLE_NAME;
SELECT JSON_OBJECT('kind','column','table',TABLE_NAME,'name',COLUMN_NAME,'ordinal',ORDINAL_POSITION,'type',COLUMN_TYPE,'nullable',IS_NULLABLE,'charset',CHARACTER_SET_NAME,'collation',COLLATION_NAME,'default',COLUMN_DEFAULT,'precision',DATETIME_PRECISION,'extra',TRIM(REPLACE(EXTRA,'DEFAULT_GENERATED','')),'expression',GENERATION_EXPRESSION) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ({tables}) ORDER BY TABLE_NAME,ORDINAL_POSITION;
SELECT JSON_OBJECT('kind','index','table',TABLE_NAME,'name',INDEX_NAME,'ordinal',SEQ_IN_INDEX,'column',COLUMN_NAME,'prefix',SUB_PART,'unique',IF(NON_UNIQUE=0,CAST('true' AS JSON),CAST('false' AS JSON)),'type',INDEX_TYPE,'direction',COLLATION,'expression',EXPRESSION,'visible',IS_VISIBLE) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ({tables}) ORDER BY TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX;
SELECT JSON_OBJECT('kind','check','table',t.TABLE_NAME,'expression',c.CHECK_CLAUSE,'enforced',t.ENFORCED) FROM information_schema.TABLE_CONSTRAINTS t JOIN information_schema.CHECK_CONSTRAINTS c ON c.CONSTRAINT_SCHEMA=t.CONSTRAINT_SCHEMA AND c.CONSTRAINT_NAME=t.CONSTRAINT_NAME WHERE t.TABLE_SCHEMA=DATABASE() AND t.TABLE_NAME IN ({tables}) AND t.CONSTRAINT_TYPE='CHECK' ORDER BY t.TABLE_NAME,t.CONSTRAINT_NAME;
"""
def row_counts_sql():return ''.join(f"SELECT JSON_OBJECT('kind','row_count','table','{name}','count',COUNT(*)) FROM {name};\n" for name in TABLES)

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--prepare',action='store_true');parser.add_argument('--facts',type=Path);parser.add_argument('--phase',choices=['pre','post']);args=parser.parse_args()
    contract=prepare_contract()
    if args.prepare:
        (HERE/'g28-schema-contract.json').write_text(json.dumps(contract,indent=2)+'\n',encoding='utf-8');(HERE/'g28-schema-capture.sql').write_text(capture_sql(),encoding='utf-8');(HERE/'g28-schema-row-counts.sql').write_text(row_counts_sql(),encoding='utf-8')
        print('PREPARED_NO_DATABASE_EXECUTION')
    else:
        require(args.facts and args.phase,'facts/phase required');rows=[json.loads(line) for line in args.facts.read_text(encoding='utf-8').splitlines()];print(validate_facts(contract,rows,args.phase))
