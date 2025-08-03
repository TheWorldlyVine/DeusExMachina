import { ApolloServerPlugin } from '@apollo/server';

export const loggingPlugin: ApolloServerPlugin = {
  async requestDidStart() {
    const startTime = Date.now();
    
    return {
      async willSendResponse(requestContext) {
        const duration = Date.now() - startTime;
        const { request, response } = requestContext;
        
        // Log request details
        console.log({
          timestamp: new Date().toISOString(),
          operation: request.operationName,
          duration: `${duration}ms`,
          status: response.body.kind === 'single' && response.body.singleResult.errors 
            ? 'error' 
            : 'success',
          user: requestContext.contextValue?.user?.id || 'anonymous',
        });
        
        // Log errors if any
        if (response.body.kind === 'single' && response.body.singleResult.errors) {
          response.body.singleResult.errors.forEach((error) => {
            console.error({
              timestamp: new Date().toISOString(),
              operation: request.operationName,
              error: error.message,
              path: error.path,
              extensions: error.extensions,
            });
          });
        }
      },
    };
  },
};