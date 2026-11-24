#!/usr/bin/env python3
"""Read-only roadmap structural checks and candidate selection."""
import json
import re
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[2]
HEAD = 'ID | Этап | Релиз | Приоритет | Статус | Зависимости | Владелец | Задача | Критерии | Проверки | Доказательства | Блокер / причина | Следующий шаг'.split(' | ')
STATUSES = {'planned','in_progress','review','blocked','done','cancelled'}

def known(v):
    return isinstance(v, str) and v.strip().lower() not in ('', '-', 'tbd', 'unresolved')

def cells(line):
    if not line.startswith('|') or not line.endswith('|'):
        return []
    return [x.strip() for x in line[1:-1].split('|')]

def validate(root, cfg):
    errors, tasks = [], {}
    mode = cfg.get('roadmap_format')
    sources = cfg.get('sources')
    if not isinstance(sources, dict): return ['Invalid roadmap sources'], []
    source = sources.get('roadmap', {})
    if not isinstance(source, dict): return ['Invalid roadmap source'], []
    if mode not in ('native','existing','external'):
        return ['Invalid roadmap_format'], []
    if mode != 'native':
        if source.get('kind') != ('external' if mode == 'external' else 'file'):
            errors.append('Roadmap source kind does not match format')
        if not known(cfg.get('roadmap_policy_note')): errors.append('Missing roadmap mapping and verification policy')
        return errors, []
    if source.get('kind') != 'file' or not known(source.get('value')):
        return ['Native roadmap needs local file source'], []
    if Path(source['value']).is_absolute(): return ['Native roadmap needs relative path'], []
    path = (root/source['value']).resolve()
    if root not in path.parents or not path.is_file(): return ['Missing or unsafe roadmap path'], []
    try: raw = path.read_text(encoding='utf-8')
    except (OSError, UnicodeError) as exc: return [str(exc)], []
    begin, end = '<!-- roadmap:begin -->', '<!-- roadmap:end -->'
    if raw.count(begin)!=1 or raw.count(end)!=1 or raw.index(begin)>raw.index(end):
        return ['Expected one marked roadmap table'], []
    lines = [x.strip() for x in raw.split(begin)[1].split(end)[0].splitlines() if x.strip()]
    if len(lines)<2 or cells(lines[0])!=HEAD or len(cells(lines[1]))!=len(HEAD) or not all(re.fullmatch(r':?-{3,}:?', x) for x in cells(lines[1])):
        return ['Invalid roadmap table header'], []
    for i,line in enumerate(lines[2:],1):
        row = cells(line)
        if len(row)!=len(HEAD) or not all(row): errors.append(f'Invalid roadmap row {i}'); continue
        id_,phase,release,prio,status,deps,owner,title,criteria,checks,evidence,reason,next_ = row
        if not re.fullmatch(r'[A-Z][A-Z0-9]*-[0-9]+',id_) or id_ in tasks:
            errors.append(f'Invalid or duplicate task ID: {id_}'); continue
        deps = [] if deps=='-' else [x.strip() for x in deps.split(',')]
        if len(set(deps)) != len(deps): errors.append(f'{id_}: duplicate dependencies')
        if prio not in ('P0','P1','P2','P3') or status not in STATUSES or not known(title): errors.append(f'{id_}: invalid priority/status/title')
        ready = all(known(x) for x in (phase,release,criteria,checks))
        if status in ('in_progress','review','done') and (not ready or not known(owner)):
            errors.append(f'{id_}: active/completed task lacks required fields')
        if status in ('review','done') and not known(evidence): errors.append(f'{id_}: missing evidence')
        if status in ('blocked','cancelled') and not known(reason): errors.append(f'{id_}: missing reason')
        if status=='blocked' and not known(next_): errors.append(f'{id_}: missing unblock step')
        tasks[id_] = (status,deps,prio,ready,owner,reason)
    for id_, (status,deps,*_) in tasks.items():
        for dep in deps:
            if dep not in tasks: errors.append(f'{id_}: unknown dependency {dep}')
            elif status in ('in_progress','review','done') and tasks[dep][0]!='done': errors.append(f'{id_}: unfinished dependency {dep}')
    # Kahn's algorithm avoids recursion limits on large roadmaps.
    indegree={k:0 for k in tasks}; children={k:[] for k in tasks}
    for k,(_,deps,*_) in tasks.items():
        for d in deps:
            if d in tasks: indegree[k]+=1; children[d].append(k)
    queue=[k for k,v in indegree.items() if not v]; count=0
    while queue:
        k=queue.pop(); count+=1
        for child in children[k]:
            indegree[child]-=1
            if not indegree[child]: queue.append(child)
    if count!=len(tasks): errors.append('Dependency cycle detected')
    candidates=[k for k,(s,d,p,r,o,b) in tasks.items() if s=='planned' and r and o=='-' and b=='-' and all(x in tasks and tasks[x][0]=='done' for x in d)]
    return errors, sorted(candidates,key=lambda k:tasks[k][2]) if not errors else []

def main():
    try:
        cfg=json.loads((ROOT/'.ai-framework/project.json').read_text(encoding='utf-8'))
        if not isinstance(cfg,dict) or not isinstance(cfg.get('sources'),dict): raise ValueError('Invalid configuration')
        if cfg.get('status')!='active': print('BLOCKED: activate configuration first'); return 1
        errors, candidates=validate(ROOT,cfg)
    except (OSError,ValueError,TypeError) as exc: print('FAIL:',exc); return 1
    if errors:
        for e in errors: print('FAIL:',e)
        return 1
    if cfg['roadmap_format']!='native': print('SKIPPED: roadmap content requires tracker-specific verification'); return 0
    print('STRUCTURE PASS: evidence and actual task state not verified')
    print('Candidates (not assignments): '+(', '.join(candidates) or 'none'))
    return 0
if __name__=='__main__': sys.exit(main())
