import { useEffect, useRef, useState } from 'react';
import { boardRepository } from './data/boardRepository';
import { categories, type BoardPost } from './types';
import Upload from './capture/PosterCapture';
import AccountPanel from './capture/AccountPanel';
import './capture/capture.css';

const statusText = { DRAFT: '작성 중', PENDING: '승인 대기', APPROVED: '게시 중', REJECTED: '반려', HIDDEN: '비공개' };
const dateText = (value?: string) => value ? value.slice(0, 10).replaceAll('-', '.') : '미정';
const go = (path: string) => { window.location.hash = path; };

function Icon({ name }: { name: string }) {
  const paths: Record<string, React.ReactNode> = {
    board: <><rect x="3" y="3" width="7" height="8" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="3" y="15" width="7" height="6" rx="1"/><rect x="14" y="12" width="7" height="9" rx="1"/></>,
    archive: <><rect x="3" y="4" width="18" height="4" rx="1"/><path d="M5 8v12h14V8M10 12h4"/></>,
    plus: <path d="M12 4v16M4 12h16"/>, search: <><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/></>,
    back: <path d="m14 5-7 7 7 7"/>, user: <><circle cx="12" cy="8" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2"/></>,
    close: <path d="m6 6 12 12M18 6 6 18"/>, image: <><rect x="3" y="3" width="18" height="18" rx="3"/><circle cx="8" cy="8" r="1.5"/><path d="m3 17 6-6 5 5 3-3 4 4"/></>,
  };
  return <svg viewBox="0 0 24 24" aria-hidden="true">{paths[name] || paths.board}</svg>;
}

function Sheet({ title, onClose, children }: { title: string; onClose: () => void; children: React.ReactNode }) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => { const dialog = ref.current!; dialog.showModal(); return () => dialog.close(); }, []);
  return <dialog ref={ref} className="sheet" onCancel={onClose} onClick={event => { if (event.target === event.currentTarget) onClose(); }}>
    <div className="sheet-head"><h2>{title}</h2><button onClick={onClose} aria-label="닫기"><Icon name="close"/></button></div>
    {children}
  </dialog>;
}

function PostDetail({ post, back }: { post: BoardPost; back: string }) {
  const [zoom, setZoom] = useState(false);
  return <main className="detail-page">
    <div className="detail-meta"><span>{post.category}</span><span>{post.archiveYear ? `${post.archiveYear} 지난 전단` : statusText[post.status]}</span></div>
    <button className="detail-image" onClick={() => setZoom(true)} aria-label={`${post.title} 전단 크게 보기`}><img src={post.imageUrl} alt={`${post.title} 전단`}/></button>
    <span className="image-hint">전단을 누르면 크게 볼 수 있어요</span>
    <section className="detail-content"><h1>{post.title}</h1><p className="organizer">{post.organizer}</p><p className="description">{post.description}</p>
      <dl><div><dt>장소</dt><dd>{post.location || '미정'}</dd></div>{post.eventAt && <div><dt>행사 날짜</dt><dd>{dateText(post.eventAt)}</dd></div>}{post.deadlineAt && <div><dt>신청 마감</dt><dd>{dateText(post.deadlineAt)}</dd></div>}<div><dt>게시 종료</dt><dd>{dateText(post.postingEndsAt)}</dd></div></dl>
      {post.applicationUrl && <a className="primary" href={post.applicationUrl} target="_blank" rel="noreferrer">신청 페이지 열기 ↗</a>}
      <p className="demo-note">화면 확인용 예시 전단입니다. 실제 행사·모집 정보가 아니에요.</p>
      <button className="secondary full" onClick={() => go(back)}>목록으로 돌아가기</button>
    </section>
    {zoom && <Sheet title="전단 크게 보기" onClose={() => setZoom(false)}><img className="zoom-image" src={post.imageUrl} alt={`${post.title} 전단 원본`}/></Sheet>}
  </main>;
}

function Login() {
  return <main className="form-page auth-page"><div className="page-intro"><h1>내 계정</h1><p>백엔드를 실행한 상태에서 로그인·회원가입할 수 있어요.</p></div><AccountPanel/><a className="secondary full" href="#/board">로그인 없이 대자보 보기</a></main>;
}
export default function App() {
  const [route, setRoute] = useState(window.location.hash.slice(1) || '/board');
  const [category, setCategory] = useState('전체');
  const [query, setQuery] = useState('');
  const [searchOpen, setSearchOpen] = useState(false);
  const [sheet, setSheet] = useState<'filter' | 'account' | null>(null);
  const [year, setYear] = useState('전체');
  const mainRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const handler = () => { setRoute(window.location.hash.slice(1) || '/board'); setSheet(null); window.scrollTo(0, 0); };
    window.addEventListener('hashchange', handler); return () => window.removeEventListener('hashchange', handler);
  }, []);
  useEffect(() => { mainRef.current?.focus(); }, [route]);
  const archive = route === '/archive';
  const wall = route === '/board' || archive;
  const mine = route === '/mine';
  const post = route.startsWith('/post/') ? boardRepository.find(route.split('/')[2]) : undefined;
  const editPost = route.startsWith('/edit/') ? boardRepository.find(route.split('/')[2]) : undefined;
  const filtered = boardRepository.list(archive).filter(item => (category === '전체' || item.category === category) && (!archive || year === '전체' || item.archiveYear === year) && `${item.title} ${item.organizer} ${item.description} ${item.category}`.toLowerCase().includes(query.trim().toLowerCase()));
  const back = post?.archiveYear ? '/archive' : post?.id.startsWith('my-') || route.startsWith('/edit/') ? '/mine' : '/board';
  const title = archive ? '지난 전단' : mine ? '내 대자보' : route === '/upload' ? '올리기' : route.startsWith('/edit/') ? '수정하기' : route === '/login' ? '내 계정' : '대자보';
  return <div className={`app-shell ${route === '/upload' || editPost ? 'capture-shell' : ''}`}><a className="skip-link" href="#content" onClick={e => { e.preventDefault(); mainRef.current?.focus(); }}>본문으로 이동</a>
    <header className="app-header">{wall ? <a className="logo" href="#/board"><span className="brand-dot"/>한눈에</a> : <div className="header-title"><button onClick={() => go(back)} aria-label="목록으로 돌아가기"><Icon name="back"/></button><span>{post ? '대자보' : title}</span></div>}
      <div className="header-actions">{wall && <><button className="filter-button" onClick={() => setSheet('filter')} aria-label={`분류 선택: ${category}`}>{category}<span>⌄</span></button><button aria-label="대자보 검색" aria-expanded={searchOpen} onClick={() => setSearchOpen(!searchOpen)}><Icon name={searchOpen ? 'close' : 'search'}/></button></>}<button aria-label="내 계정 메뉴" onClick={() => setSheet('account')}><Icon name="user"/></button></div>
    </header>
    <div ref={mainRef} id="content" tabIndex={-1}>
      {wall && <>{searchOpen && <div className="search-row"><Icon name="search"/><input autoFocus type="search" value={query} onChange={e => setQuery(e.target.value)} placeholder="제목, 주최자, 내용 검색" aria-label="대자보 검색어"/>{query && <button onClick={() => setQuery('')} aria-label="검색어 지우기"><Icon name="close"/></button>}</div>}
        {archive && <div className="archive-bar"><div><h1>지난 전단</h1><p>지나간 소식도 여기에.</p></div><label><span className="sr-only">아카이브 연도</span><select value={year} onChange={e => setYear(e.target.value)}><option value="전체">모든 연도</option><option>2026</option><option>2025</option></select></label></div>}
        {(query || category !== '전체') && <div className="results-line"><span>{category !== '전체' ? category : '검색 결과'} · {filtered.length}개</span><button onClick={() => { setQuery(''); setCategory('전체'); }}>초기화</button></div>}
        <main className="poster-wall" aria-label={archive ? '지난 대자보' : '진행 중인 대자보'}>{filtered.map(item => <a className="poster" key={item.id} href={`#/post/${item.id}`} aria-label={`${item.title} 상세 보기`}><img src={item.imageUrl} alt={`${item.title} 전단`}/>{archive && <span className="poster-stamp">{item.archiveYear} 기록</span>}</a>)}</main>
        {!filtered.length && <div className="empty-state"><Icon name="search"/><h2>조건에 맞는 전단이 없어요</h2><p>다른 분류나 검색어로 찾아보세요.</p><button className="secondary" onClick={() => { setQuery(''); setCategory('전체'); setYear('전체'); }}>전체 전단 보기</button></div>}
        <p className="wall-caption">화면 체험용 예시 전단 · 실제 게시 정보가 아닙니다</p>
      </>}
      {post && <PostDetail key={post.id} post={post} back={back}/>}
      {(route === '/upload' || editPost) && <Upload key={route} editPost={editPost}/>}
      {route === '/login' && <Login/>}
      {mine && <main className="form-page"><div className="page-intro"><h1>내 대자보</h1><p>작성부터 게시까지, 한곳에서 확인해요.</p></div><div className="inline-note">작성 중 · 승인 대기 · 반려 상태를 보여주는 예시입니다.</div><div className="my-posts">{boardRepository.mine().map(item => <article className="my-post" key={item.id}><a href={`#/post/${item.id}`}><img src={item.imageUrl} alt=""/></a><div><span className={`status ${item.status.toLowerCase()}`}>{statusText[item.status]}</span><a href={`#/post/${item.id}`}><h2>{item.title}</h2></a><p>{item.category} · {item.organizer}</p>{item.status === 'REJECTED' && <p className="reject-reason">반려 사유 예시: 승인 증빙을 확인해주세요.</p>}{item.status !== 'PENDING' && <a className="edit-link" href={`#/edit/${item.id}`}>수정 화면 보기 →</a>}</div></article>)}</div><button className="primary full" onClick={() => go('/upload')}>새 대자보 작성하기</button></main>}
      {!wall && !post && route !== '/upload' && !editPost && route !== '/login' && !mine && <main className="empty-state"><h1>대자보를 찾을 수 없어요</h1><button className="primary" onClick={() => go('/board')}>게시판으로</button></main>}
    </div>
    {wall && <nav className="dock" aria-label="게시판 주요 기능">{[{path: '/board', icon: 'board', label: '대자보'}, {path: '/archive', icon: 'archive', label: '지난 전단'}, {path: '/upload', icon: 'plus', label: '올리기'}].map(item => <a key={item.path} href={`#${item.path}`} aria-current={route === item.path ? 'page' : undefined} className={route === item.path ? 'selected' : ''}><Icon name={item.icon}/><span>{item.label}</span></a>)}</nav>}
    {sheet === 'filter' && <Sheet title="어떤 소식을 찾나요?" onClose={() => setSheet(null)}><div className="category-options">{['전체', ...categories].map(item => <button key={item} className={category === item ? 'chosen' : ''} aria-pressed={category === item} onClick={() => { setCategory(item); setSheet(null); }}>{item}<span>{category === item ? '✓' : '→'}</span></button>)}</div></Sheet>}
    {sheet === 'account' && <Sheet title="내 게시판" onClose={() => setSheet(null)}><div className="account-menu"><p className="demo-note">로그인 없이 모든 공개 대자보를 볼 수 있어요.</p><button onClick={() => go('/mine')}>내 대자보 <span>작성 상태 예시 →</span></button><button onClick={() => go('/login')}>로그인 / 회원가입 <span>화면 보기 →</span></button><p className="demo-note">API 연결 전 화면 시제품입니다.</p></div></Sheet>}
  </div>;
}
