import { withFilter } from 'graphql-subscriptions';
import { pubsub } from '../utils/pubsub';
import { Context, requireAuth } from '../context';

interface SubscriptionContext extends Context {
  // Subscription-specific context
}

export const subscriptionResolvers = {
  Subscription: {
    documentUpdated: {
      subscribe: withFilter(
        () => pubsub.asyncIterator(['DOCUMENT_UPDATED']),
        (payload, variables, context: SubscriptionContext) => {
          // Check if user has access to this document
          requireAuth(context);
          return payload.documentUpdated.id === variables.documentId;
        }
      ),
    },
    
    sceneUpdated: {
      subscribe: withFilter(
        () => pubsub.asyncIterator(['SCENE_UPDATED']),
        (payload, variables, context: SubscriptionContext) => {
          requireAuth(context);
          return payload.sceneUpdated.documentId === variables.documentId;
        }
      ),
    },
    
    characterUpdated: {
      subscribe: withFilter(
        () => pubsub.asyncIterator(['CHARACTER_UPDATED']),
        (payload, variables, context: SubscriptionContext) => {
          requireAuth(context);
          return (
            payload.characterUpdated.projectId === variables.projectId &&
            payload.characterUpdated.characterId === variables.characterId
          );
        }
      ),
    },
    
    plotUpdated: {
      subscribe: withFilter(
        () => pubsub.asyncIterator(['PLOT_UPDATED']),
        (payload, variables, context: SubscriptionContext) => {
          requireAuth(context);
          return (
            payload.plotUpdated.projectId === variables.projectId &&
            payload.plotUpdated.plotId === variables.plotId
          );
        }
      ),
    },
    
    generationProgress: {
      subscribe: withFilter(
        (_, { requestId }) => pubsub.asyncIterator([`GENERATION_PROGRESS_${requestId}`]),
        (payload, variables, context: SubscriptionContext) => {
          requireAuth(context);
          return payload.generationProgress.requestId === variables.requestId;
        }
      ),
    },
    
    collaboratorJoined: {
      subscribe: withFilter(
        () => pubsub.asyncIterator(['COLLABORATOR_JOINED']),
        (payload, variables, context: SubscriptionContext) => {
          requireAuth(context);
          return payload.collaboratorJoined.documentId === variables.documentId;
        }
      ),
    },
    
    collaboratorLeft: {
      subscribe: withFilter(
        () => pubsub.asyncIterator(['COLLABORATOR_LEFT']),
        (payload, variables, context: SubscriptionContext) => {
          requireAuth(context);
          return payload.collaboratorLeft.documentId === variables.documentId;
        }
      ),
    },
    
    cursorMoved: {
      subscribe: withFilter(
        () => pubsub.asyncIterator(['CURSOR_MOVED']),
        (payload, variables, context: SubscriptionContext) => {
          requireAuth(context);
          // Don't send cursor updates from the same user
          return (
            payload.cursorMoved.documentId === variables.documentId &&
            payload.cursorMoved.userId !== context.user?.id
          );
        }
      ),
    },
  },
};