import { ApolloServerPlugin } from '@apollo/server';
import depthLimit from 'graphql-depth-limit';

const MAX_DEPTH = parseInt(process.env.GRAPHQL_DEPTH_LIMIT || '10');

export const depthLimitPlugin: ApolloServerPlugin = {
  async requestDidStart() {
    return {
      async validationDidStart() {
        return async (requestContext) => {
          const errors = depthLimit(MAX_DEPTH)(requestContext.document);
          if (errors && errors.length > 0) {
            throw new Error(`Query depth limit of ${MAX_DEPTH} exceeded`);
          }
        };
      },
    };
  },
};