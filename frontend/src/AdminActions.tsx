import {useState} from 'react';
import type {BoardPost} from './types';
import {request} from './capture/api';

export default function AdminActions({post,onDone}:{post:BoardPost;onDone:()=>void}){
 const [eventAt,setEventAt]=useState(post.eventAt?.slice(0,19)||''),[deadlineAt,setDeadlineAt]=useState(post.deadlineAt?.slice(0,19)||'');
 const [start,setStart]=useState(post.postingStart||''),[end,setEnd]=useState(post.postingEndsAt||''),[reason,setReason]=useState(''),[error,setError]=useState(''),[busy,setBusy]=useState(false);
 const apply=async(action:'schedule'|'hide'|'restore')=>{
  if(!reason.trim()){setError('변경 사유를 입력해주세요.');return;}
  if(action==='schedule'&&(!start||!end||start>end)){setError('게시 시작일과 종료일을 확인해주세요.');return;}
  setBusy(true);setError('');
  try{await request(`/api/admin/posts/${post.id}/${action}`,action==='schedule'?'PUT':'POST',action==='schedule'?{eventAt:eventAt||null,deadlineAt:deadlineAt||null,postingStart:start,postingEnd:end,reason,version:post.version}:{reason,version:post.version});onDone();}
  catch(e){setError((e as Error).message);}finally{setBusy(false);}
 };
 return <details className="admin-actions"><summary>시간 수정{['APPROVED','HIDDEN'].includes(post.status)?' · 게시 관리':''}</summary><form onSubmit={e=>{e.preventDefault();void apply('schedule');}}><fieldset disabled={busy}>
  <p className="demo-note">한국 시간 기준입니다. 게시 종료일을 지나면 지난 전단으로 이동합니다. 시간만 수정할 때 승인·비공개 상태는 유지됩니다.</p>
  <label className="field">행사 일시<input type="datetime-local" step="1" value={eventAt} onChange={e=>setEventAt(e.target.value)}/></label>
  <label className="field">신청 마감 일시<input type="datetime-local" step="1" value={deadlineAt} onChange={e=>setDeadlineAt(e.target.value)}/></label>
  <div className="admin-date-pair"><label className="field">게시 시작일<input type="date" required value={start} onChange={e=>setStart(e.target.value)}/></label><label className="field">게시 종료일<input type="date" required min={start} value={end} onChange={e=>setEnd(e.target.value)}/></label></div>
  <label className="field">변경 사유<textarea required maxLength={1000} value={reason} onChange={e=>setReason(e.target.value)} placeholder="예: 주최 측 요청으로 행사 일시 변경"/></label>
  <button className="primary full" type="submit">{busy?'처리 중…':'시간 변경 저장'}</button>
  {post.status==='APPROVED'&&<><p className="demo-note">게시 내림 시 게시판·아카이브·공개 링크에서 숨겨집니다. 내용과 이력은 보존됩니다.</p><button type="button" className="secondary full admin-hide" onClick={()=>void apply('hide')}>대자보 내리기</button></>}
  {post.status==='HIDDEN'&&<><p className="demo-note">다시 게시하면 설정된 게시 기간에 따라 게시판 또는 아카이브에 노출됩니다.</p><button type="button" className="secondary full" onClick={()=>void apply('restore')}>다시 게시하기</button></>}
 </fieldset></form>{error&&<p className="error" role="alert">{error}</p>}</details>;
}
