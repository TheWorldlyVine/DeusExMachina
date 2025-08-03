import { requireAuth } from '../context';
import { ResolverContext } from '../types/resolver';

export const authResolvers = {
  Query: {
    me: async (_: any, __: any, context: ResolverContext) => {
      requireAuth(context);
      return context.dataSources.authAPI.getCurrentUser();
    },
    
    user: async (_: any, { id }: { id: string }, context: ResolverContext) => {
      requireAuth(context);
      return context.dataSources.authAPI.getUser(id);
    },
  },
  
  Mutation: {
    register: async (_: any, { input }: any, context: ResolverContext) => {
      const result = await context.dataSources.authAPI.register(input);
      return {
        token: result.token,
        refreshToken: result.refreshToken,
        user: result.user,
      };
    },
    
    login: async (_: any, { input }: any, context: ResolverContext) => {
      const result = await context.dataSources.authAPI.login(input);
      return {
        token: result.token,
        refreshToken: result.refreshToken,
        user: result.user,
      };
    },
    
    refreshToken: async (_: any, { token }: { token: string }, context: ResolverContext) => {
      const result = await context.dataSources.authAPI.refreshToken(token);
      return {
        token: result.token,
        refreshToken: result.refreshToken,
        user: result.user,
      };
    },
    
    logout: async (_: any, __: any, context: ResolverContext) => {
      requireAuth(context);
      return context.dataSources.authAPI.logout();
    },
  },
  
  User: {
    projects: async (user: any, _: any, context: ResolverContext) => {
      // TODO: Implement project fetching
      return [];
    },
  },
};