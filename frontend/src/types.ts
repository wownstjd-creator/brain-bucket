export const categories = ['동아리', '대회', '행사', '공지', '기타'] as const;
export type Category = typeof categories[number];
export type PostStatus = 'DRAFT' | 'PENDING' | 'APPROVED' | 'REJECTED' | 'HIDDEN';

// 화면용 모델. 이미지·게시 종료·아카이브 필드는 아직 백엔드 계약이 확정되지 않았다.
export interface BoardPost {
  id: string;
  title: string;
  organizer: string;
  category: Category;
  description: string;
  location: string;
  imageUrl: string;
  eventAt?: string;
  deadlineAt?: string;
  postingEndsAt: string;
  applicationUrl?: string;
  status: PostStatus;
  archiveYear?: string;
}
