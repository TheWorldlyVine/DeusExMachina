import { ApolloServerPlugin } from '@apollo/server';
import depthLimit from 'graphql-depth-limit';
import { Context } from '../context';

const MAX_DEPTH = parseInt(process.env.GRAPHQL_DEPTH_LIMIT || '10');

export const depthLimitPlugin: ApolloServerPlugin<Context> = {
  async requestDidStart() {
    return {
      async validationDidStart(): Promise<void> {
        // Depth limit validation is handled through validation rules in the server config
        // This is just a placeholder for potential custom validation logic
      },
    };
  },
};