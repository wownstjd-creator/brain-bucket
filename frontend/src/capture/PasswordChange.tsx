import {useState} from 'react';
import {api} from './api';
export default function PasswordChange({onChanged}:{onChanged:()=>void}){
 const [busy,setBusy]=useState(false),[message,setMessage]=useState('');
 return <form onSubmit={async e=>{e.preventDefault();const data=new FormData(e.currentTarget);const current=String(data.get('current')),next=String(data.get('next'));
 if(next!==data.get('confirm')){setMessage('새 비밀번호 확인이 일치하지 않아요.');return;}
 setBusy(true);setMessage('');try{await api.changePassword(current,next);onChanged();}catch(error){setMessage((error as Error).message);}finally{setBusy(false);}}}>
 <h3>비밀번호 변경</h3><div className="field"><label>현재 비밀번호<input name="current" type="password" required autoComplete="current-password"/></label></div>
 <div className="field"><label>새 비밀번호<input name="next" type="password" required minLength={8} maxLength={72} autoComplete="new-password"/></label></div>
 <div className="field"><label>새 비밀번호 확인<input name="confirm" type="password" required minLength={8} maxLength={72} autoComplete="new-password"/></label></div>
 <p className="demo-note">8자 이상, UTF-8 기준 72바이트 이하. 변경하면 이 기기에서 로그아웃돼요.</p>
 <button className="primary" disabled={busy}>{busy?'변경 중…':'비밀번호 변경'}</button>{message&&<p role="alert" className="error">{message}</p>}</form>;
}
