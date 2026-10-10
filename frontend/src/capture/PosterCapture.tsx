import { useEffect, useRef, useState } from 'react';
import { createWorker, type Worker } from 'tesseract.js';
import workerPath from 'tesseract.js/dist/worker.min.js?url';
import { categories, type BoardPost } from '../types';
import { defaultCorners, validCorners, type Point } from './geometry';
import { normalizePhoto, correctPhoto } from './image';
import { emptyFields, extractPoster, type PosterFields, type Extraction } from './extract';
import { api, type Member } from './api';
import AccountPanel from './AccountPanel';

const labels:Record<keyof PosterFields,string>={title:'제목',organizer:'주최자',category:'분류',description:'상세 설명',location:'장소',applicationUrl:'신청 링크',eventDate:'행사 날짜',eventTime:'행사 시간',deadlineDate:'신청 마감일',deadlineTime:'마감 시간',postingStart:'게시 시작일',postingEnd:'게시 종료일'};
const cornerNames=['왼쪽 위','오른쪽 위','오른쪽 아래','왼쪽 아래'];
export default function PosterCapture({editPost}:{editPost?:BoardPost}) {
  const [source,setSource]=useState(''),[result,setResult]=useState(editPost?.imageUrl||''),[showOriginal,setShowOriginal]=useState(false),[points,setPoints]=useState(defaultCorners),[enhance,setEnhance]=useState(true);
  const [fields,setFields]=useState<PosterFields>(()=>({...emptyFields(),...(editPost?{title:editPost.title,organizer:editPost.organizer,category:editPost.category,description:editPost.description,location:editPost.location,applicationUrl:editPost.applicationUrl||'',eventDate:editPost.eventAt?.slice(0,10)||'',eventTime:editPost.eventAt?.slice(11,16)||'',deadlineDate:editPost.deadlineAt?.slice(0,10)||'',deadlineTime:editPost.deadlineAt?.slice(11,16)||'',postingStart:editPost.postingStart||'',postingEnd:editPost.postingEndsAt.slice(0,10)}:{})}));
  const [rawText,setRawText]=useState(''),[extraction,setExtraction]=useState<Extraction>({fields:{},evidence:{},warnings:[]}),[year,setYear]=useState(new Date().getFullYear());
  const [busy,setBusy]=useState(false),[progress,setProgress]=useState(''),[error,setError]=useState(''),[confirmed,setConfirmed]=useState(false),[saved,setSaved]=useState(''),[saving,setSaving]=useState(false),[member,setMember]=useState<Member|null>(null);
  const [confidence,setConfidence]=useState<number|null>(null),[localNotice,setLocalNotice]=useState('');
  const stage=useRef<HTMLDivElement>(null),form=useRef<HTMLFormElement>(null),worker=useRef<Worker|null>(null),generation=useRef(0),draftId=useRef<string|undefined>(editPost?.id),memberId=useRef<number|undefined>(undefined),generated=useRef(new Set<keyof PosterFields>());
  const revision=useRef<number|undefined>(editPost?.version),uploaded=useRef<Record<string,string>>({});
  const [evidence,setEvidence]=useState(editPost?.evidenceUrl||''),[submitted,setSubmitted]=useState(false);
  useEffect(()=>()=>{generation.current++;void worker.current?.terminate();worker.current=null;},[]);
  const resetReview=()=>{setConfirmed(false);setSaved('');};
  const cancel=()=>{generation.current++;void worker.current?.terminate();worker.current=null;setBusy(false);setProgress('인식을 취소했어요. 다시 시도하거나 직접 입력할 수 있어요.');};
  const change=(key:keyof PosterFields,value:string)=>{generated.current.delete(key);setFields(old=>({...old,[key]:value}));resetReview();};
  const applyText=(text:string)=>{
    const parsed=extractPoster(text,year);setExtraction(parsed);
    setFields(old=>{const next={...old};for(const key of Object.keys(labels) as (keyof PosterFields)[]){if(!old[key]||generated.current.has(key)){next[key]=parsed.fields[key]||'';if(next[key])generated.current.add(key);else generated.current.delete(key);}}return next;});resetReview();
  };
  const selectFile=async(file?:File)=>{
    if(!file)return;cancel();const job=++generation.current;setBusy(true);setError('');setProgress('사진을 여는 중…');
    try{const url=await normalizePhoto(file);if(job!==generation.current)return;setSource(url);setResult('');setPoints(defaultCorners());setRawText('');setConfidence(null);setExtraction({fields:{},evidence:{},warnings:[]});const oldGenerated=new Set(generated.current);generated.current.clear();setFields(old=>{const next={...old};oldGenerated.forEach(key=>{next[key]='';});return next;});setProgress('네 모서리를 전단에 맞춰주세요.');resetReview();}catch(e){if(job===generation.current)setError((e as Error).message);}finally{if(job===generation.current)setBusy(false);}
  };
  const readPoster=async()=>{
    if(!source||!validCorners(points))return;const job=++generation.current;setBusy(true);setError('');resetReview();setProgress('선택한 영역을 반듯하게 펴고 보정하고 있어요…');
    try{
      const corrected=await correctPhoto(source,points,enhance);if(job!==generation.current)return;setResult(corrected);setShowOriginal(false);setProgress('한국어 인식을 준비하고 있어요. 첫 실행은 모델 다운로드가 필요합니다.');
      const current=await createWorker(['kor','eng'],1,{workerPath,logger:message=>{if(job===generation.current&&message.status==='recognizing text')setProgress(`포스터 글자를 읽고 있어요… ${Math.round(message.progress*100)}%`);}});
      if(job!==generation.current){await current.terminate();return;}worker.current=current;
      const {data}=await current.recognize(corrected);if(job!==generation.current)return;
      setRawText(data.text);setConfidence(Math.round(data.confidence));applyText(data.text);
      setProgress(data.text.trim()?'인식이 끝났어요. 날짜와 내용을 확인해주세요.':'글자를 찾지 못했어요. 더 선명한 사진을 선택하거나 직접 입력해주세요.');
    }catch(e){if(job===generation.current)setError(`글자 인식에 실패했어요. 인터넷 연결을 확인하거나 직접 입력해주세요. (${(e as Error).message})`);}
    finally{if(job===generation.current){await worker.current?.terminate();worker.current=null;setBusy(false);}}
  };
  const moveCorner=(index:number,x:number,y:number)=>{setPoints(old=>old.map((p,i)=>i===index?{x:Math.max(0,Math.min(1,x)),y:Math.max(0,Math.min(1,y))}:p));setResult('');resetReview();};
  const save=async(submit=false)=>{
    if(!member||!confirmed||!form.current?.reportValidity())return;
    if(!fields.title.trim()||!fields.organizer.trim()){setError('제목과 주최자를 입력해주세요.');return;}
    if((fields.eventDate&&!fields.eventTime)||(!fields.eventDate&&fields.eventTime)||(fields.deadlineDate&&!fields.deadlineTime)||(!fields.deadlineDate&&fields.deadlineTime)){setError('행사와 신청 마감은 날짜·시간을 함께 입력하거나 둘 다 비워주세요.');return;}
    if(submit&&(!result||!evidence||!fields.postingStart||!fields.postingEnd)){setError('보정한 전단·승인 증빙·게시 시작일과 종료일을 모두 확인해주세요.');return;}
    setSaving(true);setError('');setSaved('');try{
      let response=await api.save({title:fields.title,organizer:fields.organizer,category:fields.category,description:fields.description,location:fields.location,applicationUrl:fields.applicationUrl,eventAt:fields.eventDate?`${fields.eventDate}T${fields.eventTime}:00`:null,deadlineAt:fields.deadlineDate?`${fields.deadlineDate}T${fields.deadlineTime}:00`:null,showViewCount:true,postingStart:fields.postingStart||null,postingEnd:fields.postingEnd||null,version:revision.current},draftId.current);
      draftId.current=response.id;revision.current=response.version;
      for(const [kind,dataUrl] of [['POSTER',result],['ORIGINAL',source],['EVIDENCE',evidence]]){
        if(dataUrl.startsWith('data:')&&uploaded.current[kind]!==dataUrl){response=await api.upload(response.id,kind,dataUrl,revision.current);revision.current=response.version;uploaded.current[kind]=dataUrl;}
      }
      if(submit){response=await api.submit(response.id,revision.current);revision.current=response.version;setSubmitted(true);}
      setSaved(submit?`#${response.id} 승인 요청이 접수됐어요. 담당자가 승인하면 공개됩니다.`:`#${response.id} 사진과 내용을 임시 저장했어요. 아직 공개되지 않았어요.`);

    }catch(e){setError((e as Error).message);}finally{setSaving(false);}
  };
  const selectEvidence=async(file?:File)=>{if(!file)return;setBusy(true);setError('');try{setEvidence(await normalizePhoto(file));resetReview();}catch(e){setError((e as Error).message);}finally{setBusy(false);}};
  const download=()=>{
    if(!result)return;const anchor=document.createElement('a');anchor.href=result;anchor.download='대자보-보정본.jpg';anchor.click();setLocalNotice('보정한 사진을 기기에 다운로드했어요.');
  };
  const input=(key:keyof PosterFields,type='text',required=false,maxLength?:number)=><div className="field" key={key}><label htmlFor={`poster-${key}`}>{labels[key]}{required&&<span>필수</span>}{generated.current.has(key)&&<span className="ocr-badge">자동 입력 · 확인 필요</span>}</label><input id={`poster-${key}`} type={type} required={required} value={fields[key]} maxLength={maxLength} onChange={e=>change(key,e.target.value)}/>{generated.current.has(key)&&extraction.evidence[key]&&<small>읽은 문구: {extraction.evidence[key]}</small>}</div>;
  return <main className="capture-page"><div className="page-intro"><span className="eyebrow">사진 한 장으로 시작하기</span><h1>대자보 게시하기</h1><p>사진을 반듯하게, 정보는 간편하게.</p></div><ol className="capture-steps"><li className={source?'done':''}>1 사진 선택</li><li className={result?'done':''}>2 영역·보정</li><li className={rawText?'done':''}>3 정보 확인</li></ol>
    <div className="capture-layout"><section className="photo-column" aria-label="사진 선택 및 보정"><div className="source-actions"><label className="primary">파일 선택<input className="sr-only" type="file" accept="image/jpeg,image/png,image/webp" disabled={busy||saving||submitted} onChange={e=>{void selectFile(e.target.files?.[0]);e.target.value='';}}/></label><label className="secondary">사진 촬영<input className="sr-only" type="file" accept="image/*" capture="environment" disabled={busy||saving||submitted} onChange={e=>{void selectFile(e.target.files?.[0]);e.target.value='';}}/></label></div><p className="demo-note">JPG·PNG·WebP, 최대 20MB. 촬영은 지원하는 휴대폰에서 카메라를 열어요.</p>
      {!source&&!result&&<div className="capture-placeholder"><span>▧</span><h2>게시할 전단을 선택해주세요</h2><p>전단 전체가 잘 보이도록 찍으면<br/>글자를 더 정확하게 읽을 수 있어요.</p></div>}
      {(source||result)&&<><div className="crop-heading"><strong>{result?'보정한 전단':'네 모서리를 전단에 맞춰주세요'}</strong><button type="button" className="text-button" disabled={busy||saving||submitted} onClick={()=>{if(source){setResult('');setPoints(defaultCorners());resetReview();}else{setProgress('영역을 바꾸려면 사진을 다시 선택해주세요.');}}}>영역 다시 선택</button></div>
        {result?<><img className="corrected-photo" src={showOriginal?(source||editPost?.originalUrl):result} alt={showOriginal?'보정 전 원본 사진':'영역 및 밝기를 보정한 전단'}/><div className="source-actions"><button className="secondary" disabled={!source&&!editPost?.originalUrl} onClick={()=>setShowOriginal(!showOriginal)}>{showOriginal?'보정본 보기':'원본과 비교'}</button><button className="secondary" onClick={download}>보정본 다운로드</button></div></>:<div className="crop-stage" ref={stage}><img src={source} alt="모서리 조정 대상 전단 사진" draggable={false}/><svg className="crop-overlay" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true"><polygon points={points.map(p=>`${p.x*100},${p.y*100}`).join(' ')} /></svg>{points.map((point,index)=><button type="button" className="crop-handle" key={index} aria-label={`${cornerNames[index]} 모서리: 드래그 또는 방향키로 조정`} disabled={busy||saving||submitted} style={{left:`${point.x*100}%`,top:`${point.y*100}%`}} onPointerDown={e=>e.currentTarget.setPointerCapture(e.pointerId)} onPointerMove={e=>{if(!e.currentTarget.hasPointerCapture(e.pointerId))return;const rect=stage.current!.getBoundingClientRect();moveCorner(index,(e.clientX-rect.left)/rect.width,(e.clientY-rect.top)/rect.height);}} onPointerUp={e=>{if(e.currentTarget.hasPointerCapture(e.pointerId))e.currentTarget.releasePointerCapture(e.pointerId);}} onKeyDown={e=>{const shift=e.shiftKey ? .03 : .005;const delta:Record<string,Point>={ArrowLeft:{x:-shift,y:0},ArrowRight:{x:shift,y:0},ArrowUp:{x:0,y:-shift},ArrowDown:{x:0,y:shift}};if(delta[e.key]){e.preventDefault();moveCorner(index,point.x+delta[e.key].x,point.y+delta[e.key].y);}}}><span>{index+1}</span></button>)}</div>}
        {!validCorners(points)&&<p className="error">모서리가 교차했거나 영역이 너무 작아요. 다시 지정해주세요.</p>}
        <label className="check-row"><input type="checkbox" checked={enhance} disabled={busy||saving||submitted} onChange={e=>{setEnhance(e.target.checked);if(source)setResult('');resetReview();}}/>밝기·대비 자동 보정</label><label className="year-setting">연도가 없는 날짜의 기준 연도<input type="number" min={2000} max={2099} value={year} disabled={busy||saving||submitted} onChange={e=>setYear(Number(e.target.value))}/></label>
        <button type="button" className="primary full" disabled={busy||saving||submitted||!source||!validCorners(points)||year<2000||year>2099} onClick={()=>void readPoster()}>{busy?'사진 처리 중…':'자동 보정하고 글자 읽기'}</button>{busy&&<button type="button" className="secondary full" onClick={cancel}>인식 취소</button>}
      </>}
      <p role="status" aria-live="polite" className="processing-status">{progress}</p>{confidence!==null&&<p className="demo-note">OCR 평균 신뢰도 {confidence}% · 낮거나 잘못 읽힌 내용은 직접 수정해주세요.</p>}{localNotice&&<p role="status" className="demo-note">{localNotice}</p>}
      <details className="ocr-details"><summary>읽은 글자 확인·수정</summary><textarea aria-label="OCR 인식 원문" rows={8} value={rawText} onChange={e=>setRawText(e.target.value)} disabled={busy}/><button type="button" className="secondary full" disabled={busy||!rawText.trim()} onClick={()=>applyText(rawText)}>원문에서 입력 항목 다시 찾기</button><p className="demo-note">직접 수정한 항목은 덮어쓰지 않아요. 다시 자동 입력하려면 해당 항목을 비워주세요.</p></details>
      <p className="demo-note">사진 인식은 이 브라우저에서 처리해요. 첫 실행 시 인식 프로그램과 언어 모델을 내려받습니다. AI 해상도 확대 기능은 포함하지 않았어요.</p>
    </section><section className="information-column" aria-label="게시 정보 확인"><h2>내용을 확인해주세요</h2><p className="demo-note">읽은 정보는 자동으로 채워지고, 모든 항목을 수정할 수 있어요.</p>{extraction.warnings.length>0&&<ul className="recognition-warnings">{extraction.warnings.map(message=><li key={message}>{message}</li>)}</ul>}
      <form ref={form} onSubmit={e=>{e.preventDefault();void save();}}><fieldset disabled={busy||saving||submitted} className="capture-fields">
        {input('title','text',true,150)}{input('organizer','text',true,100)}<div className="field"><label htmlFor="poster-category">분류</label><select id="poster-category" value={fields.category} onChange={e=>change('category',e.target.value)}>{categories.map(category=><option key={category}>{category}</option>)}</select></div>
        {input('location','text',false,150)}{input('applicationUrl','url',false,2048)}<div className="date-pair">{input('eventDate','date')}{input('eventTime','time')}</div><div className="date-pair">{input('deadlineDate','date')}{input('deadlineTime','time')}</div><div className="date-pair">{input('postingStart','date')}{input('postingEnd','date')}</div>
        <div className="field"><label htmlFor="poster-description">상세 설명</label><textarea id="poster-description" rows={6} value={fields.description} maxLength={10000} onChange={e=>change('description',e.target.value)}/></div>
        <label className="check-row review-check"><input type="checkbox" checked={confirmed} onChange={e=>setConfirmed(e.target.checked)}/>원본과 비교해 제목·날짜·내용을 확인했어요.</label>
      </fieldset></form>
      <section className="approval-note"><strong>승인 증빙 첨부</strong><p>담당자가 확인할 게시 허가 자료를 이미지로 첨부해주세요. 원본과 증빙은 작성자·담당자만 볼 수 있습니다.</p><input aria-label="승인 증빙 이미지" type="file" accept="image/jpeg,image/png,image/webp" disabled={busy||saving||submitted} onChange={e=>{void selectEvidence(e.target.files?.[0]);e.target.value='';}}/>{evidence&&<img src={evidence} alt="승인 증빙 미리보기" style={{maxHeight:200,marginTop:12}}/>}</section>
      <AccountPanel onMember={user=>{setMember(user);if(memberId.current!==undefined&&memberId.current!==user?.id){draftId.current=undefined;revision.current=undefined;uploaded.current={};setSaved('');setSubmitted(false);}memberId.current=user?.id;}}/>
      <div className="publish-limit"><strong>담당자 승인 후 공개됩니다</strong><p>사진·내용을 임시 저장하거나, 확인을 마치고 승인 요청할 수 있어요. 승인된 전단을 수정하면 공개가 중단되고 재승인이 필요해요.</p></div>
      <button type="button" className="secondary full" disabled={!member||!confirmed||saving||busy||submitted} onClick={()=>void save(false)}>{saving?'저장 중…':'임시 저장'}</button><button type="button" className="primary full publish-button" disabled={!member||!confirmed||saving||busy||submitted} onClick={()=>void save(true)}>{submitted?'승인 요청 완료':'게시하기 · 승인 요청'}</button>
      {submitted&&<a className="secondary full" href="#/mine">내 대자보에서 확인하기</a>}
      {error&&<p role="alert" className="error">{error}</p>}{saved&&<p role="status" className="saved-notice">{saved}</p>}
      <p className="demo-note">저장하지 않은 변경은 화면을 떠나면 사라져요. 임시 저장 또는 승인 요청으로 보관해주세요.</p>
    </section></div>
  </main>;
}

