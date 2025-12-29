import { userApi } from './api';

export interface Skill {
  id: string;
  name: string;
}

export const skillApi = {
  /**
   * Get all skills
   */
  getAllSkills: async (): Promise<Skill[]> => {
    const response = await userApi.request('/api/skills', {
      method: 'GET',
    });
    
    if (response.success && response.data) {
      return response.data;
    }
    throw new Error(response.message || 'Failed to fetch skills');
  },

  /**
   * Create new skill (Admin only)
   */
  createSkill: async (name: string): Promise<Skill> => {
    const response = await userApi.request('/api/skills', {
      method: 'POST',
      body: JSON.stringify({ name }),
    });
    
    if (response.success && response.data) {
      return response.data;
    }
    throw new Error(response.message || 'Failed to create skill');
  },

  /**
   * Delete skill (Admin only)
   */
  deleteSkill: async (skillId: string): Promise<void> => {
    const response = await userApi.request(`/api/skills/${skillId}`, {
      method: 'DELETE',
    });
    
    if (!response.success) {
      throw new Error(response.message || 'Failed to delete skill');
    }
  },
};
