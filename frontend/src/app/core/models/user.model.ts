export interface User {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  avatarId?: string | null;
  createdAt: string;
}

export interface UpdateUserRequest {
  username: string;
  firstName: string;
  lastName: string;
  avatarId: string | null;
}
