import type { BoardPost } from '../types';

const activePosts: BoardPost[] = [
  { id: 'music', title: '밴드 동아리 신입 부원 모집', organizer: '밴드 동아리', category: '동아리', imageUrl: '/posters/music.svg', description: '같이 연주할 새로운 멤버를 찾습니다. 악기를 처음 배우는 분도 환영해요. 관심 있는 파트와 자기소개를 준비해 주세요.', location: '학생회관 합주실', deadlineAt: '2026-10-30T23:59:00', postingEndsAt: '2026-10-30T23:59:00', status: 'APPROVED' },
  { id: 'hackathon', title: '교내 아이디어 해커톤', organizer: '학생 개발 모임', category: '대회', imageUrl: '/posters/hackathon.svg', description: '우리 학교의 불편함을 해결할 아이디어를 함께 만듭니다. 기획·디자인·개발에 관심 있는 학생 누구나 참여할 수 있어요.', location: '정보실', eventAt: '2026-11-07T10:00:00', deadlineAt: '2026-10-28T23:59:00', postingEndsAt: '2026-11-07T23:59:00', status: 'APPROVED' },
  { id: 'exhibition', title: '학생 작품 전시회', organizer: '학생 전시 기획팀', category: '행사', imageUrl: '/posters/exhibition.svg', description: '학생들이 직접 만든 작품을 전시합니다. 수업 사이 잠깐 들러 자유롭게 감상해 주세요. 별도 신청 없이 관람할 수 있습니다.', location: '중앙 로비', eventAt: '2026-11-02T12:00:00', postingEndsAt: '2026-11-06T23:59:00', status: 'APPROVED' },
  { id: 'festival', title: '가을 캠퍼스 축제', organizer: '축제 준비위원회', category: '행사', imageUrl: '/posters/festival.svg', description: '공연, 체험 부스, 작은 마켓이 함께하는 가을 축제. 편하게 들러 캠퍼스의 가을을 즐겨 주세요.', location: '야외 광장', eventAt: '2026-10-23T16:00:00', postingEndsAt: '2026-10-23T23:59:00', status: 'APPROVED' },
  { id: 'books', title: '독서 모임 문장', organizer: '독서 모임 문장', category: '동아리', imageUrl: '/posters/books.svg', description: '한 달에 한 권, 함께 읽고 이야기합니다. 책을 많이 읽지 않아도 괜찮아요. 각자의 생각을 편하게 나눌 분을 모집합니다.', location: '도서관 세미나실', deadlineAt: '2026-10-31T23:59:00', postingEndsAt: '2026-10-31T23:59:00', status: 'APPROVED' },
  { id: 'research', title: '학술제 참가팀 모집', organizer: '학술제 운영팀', category: '대회', imageUrl: '/posters/research.svg', description: '작은 질문에서 시작한 연구와 프로젝트를 소개해 주세요. 전공과 학년에 관계없이 개인 또는 팀으로 참여할 수 있습니다.', location: '학술제 행사장', eventAt: '2026-11-13T13:00:00', deadlineAt: '2026-10-16T23:59:00', postingEndsAt: '2026-11-13T23:59:00', status: 'APPROVED' },
];

const archivePosts: BoardPost[] = [
  { ...activePosts[3], id: 'archive-festival', title: '2025 가을 캠퍼스 축제', eventAt: '2025-10-24T16:00:00', deadlineAt: undefined, postingEndsAt: '2025-10-24T23:59:00', archiveYear: '2025' },
  { ...activePosts[2], id: 'archive-exhibition', title: '2026 봄 학생 작품 전시회', eventAt: '2026-05-18T12:00:00', postingEndsAt: '2026-05-22T23:59:00', archiveYear: '2026' },
  { ...activePosts[1], id: 'archive-hackathon', title: '2025 교내 아이디어 해커톤', eventAt: '2025-11-08T10:00:00', deadlineAt: '2025-10-28T23:59:00', postingEndsAt: '2025-11-08T23:59:00', archiveYear: '2025' },
];

const myPosts: BoardPost[] = [
  { ...activePosts[0], id: 'my-draft', title: '밴드 동아리 모집 · 작성 중', status: 'DRAFT' },
  { ...activePosts[5], id: 'my-pending', status: 'PENDING' },
  { ...activePosts[4], id: 'my-rejected', status: 'REJECTED' },
];

// API가 생기면 이 계층에서 응답을 BoardPost로 변환한다. 지금은 네트워크 요청이 없다.
export const boardRepository = {
  list: (archived = false) => archived ? archivePosts : activePosts,
  mine: () => myPosts,
  find: (id: string) => [...activePosts, ...archivePosts, ...myPosts].find(post => post.id === id),
};
