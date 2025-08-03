import { authResolvers } from './auth';
import { documentResolvers } from './document';
import { memoryResolvers } from './memory';
import { generationResolvers } from './generation';
import { subscriptionResolvers } from './subscriptions';
import { scalarResolvers } from './scalars';

export const resolvers = {
  ...scalarResolvers,
  Query: {
    ...authResolvers.Query,
    ...documentResolvers.Query,
    ...memoryResolvers.Query,
  },
  Mutation: {
    ...authResolvers.Mutation,
    ...documentResolvers.Mutation,
    ...memoryResolvers.Mutation,
    ...generationResolvers.Mutation,
  },
  Subscription: {
    ...subscriptionResolvers.Subscription,
  },
  // Type resolvers
  User: authResolvers.User,
  Document: documentResolvers.Document,
  Chapter: documentResolvers.Chapter,
  CharacterMemory: memoryResolvers.CharacterMemory,
  PlotMemory: memoryResolvers.PlotMemory,
  WorldMemory: memoryResolvers.WorldMemory,
};