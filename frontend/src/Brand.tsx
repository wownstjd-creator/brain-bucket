import './brand.css';

/** A taped-up poster, kept small so the actual posters remain the focus. */
export default function Brand() {
  return <a className="logo jjabo-brand" href="#/board" aria-label="짜보 · 대자보 홈">
    <img className="jjabo-mark" src="/assets/jjabo-mark.svg" width="42" height="46" alt=""/>
    <span className="jjabo-wordmark" aria-hidden="true">짜보<span className="jjabo-underline"/></span>
    <span className="jjabo-caption" aria-hidden="true">우리 학교<br/>소식 붙이는 곳</span>
  </a>;
}
