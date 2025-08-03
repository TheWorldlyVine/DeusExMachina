import { ApolloServerPlugin } from '@apollo/server';
import depthLimit from 'graphql-depth-limit';
import { Context } from '../context';

const MAX_DEPTH = parseInt(process.env.GRAPHQL_DEPTH_LIMIT || '10');

export const depthLimitPlugin: ApolloServerPlugin<Context> = {
  async requestDidStart() {
    return {
      async validationDidStart(requestContext) {
        // Apply depth limit validation
        const validationRules = [depthLimit(MAX_DEPTH)];
        return validationRules;
      },
    };
  },
};