#!/usr/bin/env python3
"""Read-only PROJECT_ZERO state and readiness checks. No approval is generated."""
import argparse
from datetime import datetime
import hashlib
import json
from pathlib import Path
import sys
from roadmap import validate as validate_roadmap
ROOT = Path(__file__).resolve().parents[2]
TOPICS = set('purpose scope prototype architecture autonomy regression environment changes task_readiness uncertainty complexity risks'.split())

def known(v):
    return isinstance(v,str) and v.strip().lower() not in ('','-','tbd','unresolved','unknown')

def fingerprint(state):
    # Approval and workflow stage are not part of the reviewed proposal.
    payload={k:v for k,v in state.items() if k not in ('approval','status')}
    return hashlib.sha256(json.dumps(payload,ensure_ascii=False,sort_keys=True,separators=(',',':')).encode('utf-8')).hexdigest()

def validate(root, cfg, ready=False, proposal=False):
    errors=[]
    try:
        s=json.loads((root/'.ai-framework/project-zero.json').read_text(encoding='utf-8'))
        if not isinstance(s,dict): raise ValueError('state must be an object')
    except (OSError,ValueError) as e: return ['PROJECT_ZERO: '+str(e)],None
    if s.get('schema_version')!=1: errors.append('PROJECT_ZERO: invalid schema')
    status=s.get('status'); approved=status=='approved'; gate=ready or approved or proposal
    if status not in ('draft','review','approved'): errors.append('PROJECT_ZERO: invalid status')
    if s.get('project_kind') not in ('unknown','new','existing') or (gate and s.get('project_kind')=='unknown'): errors.append('PROJECT_ZERO: project kind unresolved or invalid')
    topics=s.get('topics')
    if not isinstance(topics,dict) or set(topics)!=TOPICS: errors.append('PROJECT_ZERO: expected 12 topics'); topics=topics if isinstance(topics,dict) else {}
    for k in sorted(TOPICS):
        t=topics.get(k)
        if not isinstance(t,dict): errors.append('PROJECT_ZERO: missing topic '+k); continue
        mode=t.get('status'); src=t.get('source'); note=t.get('note')
        if mode=='open':
            if gate: errors.append('PROJECT_ZERO: open topic '+k)
        elif mode=='not_applicable':
            if k!='prototype' or not known(note) or src is not None: errors.append('PROJECT_ZERO: invalid N/A '+k)
        elif mode=='resolved':
            if not isinstance(src,dict) or not known(src.get('value')): errors.append('PROJECT_ZERO: missing source '+k); continue
            if src.get('kind')=='file':
                path=Path(src['value']); target=(root/path).resolve()
                if path.is_absolute() or root not in target.parents or not target.is_file(): errors.append('PROJECT_ZERO: missing/unsafe source '+k)
            elif src.get('kind')=='external':
                if not known(note): errors.append('PROJECT_ZERO: external source needs note '+k)
            else: errors.append('PROJECT_ZERO: invalid source kind '+k)
        else: errors.append('PROJECT_ZERO: invalid topic status '+k)
    blockers=s.get('blockers'); ids=set()
    if not isinstance(blockers,list): errors.append('PROJECT_ZERO: blockers must be an array'); blockers=[]
    for b in blockers:
        if not isinstance(b,dict) or not all(known(b.get(k)) for k in ('id','description','next_step')) or b.get('status') not in ('open','resolved'):
            errors.append('PROJECT_ZERO: invalid blocker'); continue
        if b['id'] in ids: errors.append('PROJECT_ZERO: duplicate blocker '+b['id'])
        ids.add(b['id'])
        if gate and b['status']=='open': errors.append('PROJECT_ZERO: open blocker '+b['id'])
    if not known(s.get('next_step')): errors.append('PROJECT_ZERO: missing next step')
    if proposal and status != 'review': errors.append('PROJECT_ZERO: proposal must be in review')
    if ready and not approved: errors.append('PROJECT_ZERO: explicit approval required')
    a=s.get('approval')
    if approved:
        if not isinstance(a,dict) or not all(known(a.get(k)) for k in ('approved_by','confirmed_at','evidence','fingerprint')):
            errors.append('PROJECT_ZERO: missing approval evidence')
        else:
            try:
                dt=datetime.fromisoformat(a['confirmed_at'].replace('Z','+00:00'))
                if dt.tzinfo is None: raise ValueError('timezone required')
            except ValueError: errors.append('PROJECT_ZERO: invalid approval timestamp')
            if a['fingerprint']!=fingerprint(s): errors.append('PROJECT_ZERO: proposal changed since approval')
    elif a is not None: errors.append('PROJECT_ZERO: clear stale approval before review')
    if ready or proposal:
        if cfg.get('status')!='active': errors.append('PROJECT_ZERO: INIT must be active')
        if cfg.get('roadmap_format')=='native' and isinstance(cfg.get('sources'),dict):
            try:
                re, _=validate_roadmap(root,cfg); errors.extend(re)
                src=cfg['sources'].get('roadmap',{})
                if not re and s.get('project_kind')=='new':
                    raw=(root/src['value']).read_text(encoding='utf-8').split('<!-- roadmap:begin -->')[1].split('<!-- roadmap:end -->')[0]
                    if len([l for l in raw.splitlines() if l.strip()])<=2: errors.append('PROJECT_ZERO: new project roadmap has no tasks')
            except (OSError,ValueError,TypeError,KeyError,IndexError) as e: errors.append('PROJECT_ZERO: roadmap '+str(e))
    return errors,s

def main():
    p=argparse.ArgumentParser(description=__doc__); p.add_argument('--ready',action='store_true'); p.add_argument('--fingerprint',action='store_true'); a=p.parse_args()
    try:
        cfg=json.loads((ROOT/'.ai-framework/project.json').read_text(encoding='utf-8'))
        if not isinstance(cfg,dict): raise ValueError('configuration must be an object')
        errors,s=validate(ROOT,cfg,a.ready,proposal=a.fingerprint)
    except (OSError,ValueError,TypeError) as e: print('FAIL:',e); return 1
    if errors:
        for e in errors: print('FAIL:',e)
        return 1
    if a.fingerprint: print(fingerprint(s)); return 0
    print('PROJECT_ZERO structure PASS; approval authenticity and document contents require human review')
    print('Status:',s['status'],'| Next:',s['next_step'])
    return 0
if __name__=='__main__': sys.exit(main())
