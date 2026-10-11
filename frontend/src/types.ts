export const categories = ['동아리', '대회', '행사', '공지', '기타'] as const;
export type Category = typeof categories[number];
export type PostStatus = 'DRAFT' | 'PENDING' | 'APPROVED' | 'REJECTED' | 'HIDDEN';

// 독립 예비본 BoardView API 응답 모델.
export interface BoardPost {
  id: string;
  version?: number;
  viewCount?: number|null;
  showViewCount?: boolean;
  publishedAt?: string;
  postingStart?: string;
  originalUrl?: string;
  evidenceUrl?: string;
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

