import { useEffect, useState } from 'react';
import { api, type Member } from './api';
export default function AccountPanel({onMember}:{onMember?:(member:Member|null)=>void}) {
  const [member,setMember]=useState<Member|null>(null),[signup,setSignup]=useState(false),[message,setMessage]=useState(''),[busy,setBusy]=useState(false),[open,setOpen]=useState(false);
  useEffect(()=>{let active=true;api.me().then(value=>{if(active){setMember(value);onMember?.(value);}}).catch(()=>{});return()=>{active=false;};},[]);
  return <section className="account-panel"><div className="account-summary"><strong>{member?`${member.displayName} · 로그인됨`:'텍스트 저장은 로그인 후 가능해요'}</strong><button type="button" className="text-button" onClick={()=>setOpen(!open)}>{open?'접기':member?'계정':'로그인 / 가입'}</button></div>
    {open && (member?<button type="button" className="secondary" disabled={busy} onClick={async()=>{setBusy(true);try{await api.logout();setMember(null);onMember?.(null);setMessage('로그아웃했어요.');}catch(e){setMessage((e as Error).message);}finally{setBusy(false);}}}>로그아웃</button>:<form onSubmit={async e=>{e.preventDefault();setBusy(true);setMessage('');const data=new FormData(e.currentTarget);try{if(signup){await api.signup(String(data.get('email')),String(data.get('password')),String(data.get('displayName')));setSignup(false);setMessage('회원가입이 완료됐어요. 로그인해주세요.');}else{const user=await api.login(String(data.get('email')),String(data.get('password')));setMember(user);onMember?.(user);setMessage('로그인했어요. 입력한 전단 내용은 그대로 유지됩니다.');}}catch(error){setMessage((error as Error).message);}finally{setBusy(false);}}}>
      {signup&&<div className="field"><label htmlFor="capture-name">닉네임</label><input id="capture-name" name="displayName" required maxLength={50}/></div>}
      <div className="field"><label htmlFor="capture-email">이메일</label><input id="capture-email" name="email" type="email" required maxLength={255} autoComplete="email"/></div>
      <div className="field"><label htmlFor="capture-password">비밀번호</label><input id="capture-password" name="password" type="password" required minLength={signup?8:undefined} maxLength={72} autoComplete={signup?'new-password':'current-password'}/></div>
      <button className="primary" disabled={busy}>{busy?'처리 중…':signup?'회원가입':'로그인'}</button><button type="button" className="text-button" disabled={busy} onClick={()=>{setSignup(!signup);setMessage('');}}>{signup?'로그인으로':'회원가입'}</button>
    </form>)}{message&&<p role="status" className="demo-note">{message}</p>}
  </section>;
}
