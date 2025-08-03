import { ApolloServer } from '@apollo/server';
import { expressMiddleware } from '@apollo/server/express4';
import { ApolloServerPluginDrainHttpServer } from '@apollo/server/plugin/drainHttpServer';
import { makeExecutableSchema } from '@graphql-tools/schema';
import { WebSocketServer } from 'ws';
import { useServer } from 'graphql-ws/lib/use/ws';
import express from 'express';
import { createServer } from 'http';
import cors from 'cors';
import { readFileSync } from 'fs';
import { join } from 'path';
import dotenv from 'dotenv';

import { resolvers } from './resolvers';
import { context } from './context';
import { dataSources } from './datasources';
import { authMiddleware } from './middleware/auth';
import { rateLimitMiddleware } from './middleware/rateLimit';
import { depthLimitPlugin } from './plugins/depthLimit';
import { complexityLimitPlugin } from './plugins/complexityLimit';
import { loggingPlugin } from './plugins/logging';

// Load environment variables
dotenv.config();

const PORT = process.env.PORT || 4000;

async function startServer() {
  // Create Express app
  const app = express();

  // Apply middlewares
  app.use(cors({
    origin: process.env.ALLOWED_ORIGINS?.split(',') || ['http://localhost:3000'],
    credentials: true,
  }));
  app.use(express.json());
  app.use(rateLimitMiddleware);

  // Create HTTP server
  const httpServer = createServer(app);

  // Load GraphQL schema
  const typeDefs = readFileSync(
    join(__dirname, 'schema', 'schema.graphql'),
    'utf-8'
  );

  // Create executable schema
  const schema = makeExecutableSchema({
    typeDefs,
    resolvers,
  });

  // Create WebSocket server for subscriptions
  const wsServer = new WebSocketServer({
    server: httpServer,
    path: '/graphql',
  });

  // Set up WebSocket server
  const serverCleanup = useServer(
    {
      schema,
      context: async (ctx, msg, args) => {
        // Return context for subscriptions
        return context({ req: ctx.extra.request, connectionParams: ctx.connectionParams });
      },
    },
    wsServer
  );

  // Create Apollo Server
  const server = new ApolloServer({
    schema,
    plugins: [
      ApolloServerPluginDrainHttpServer({ httpServer }),
      {
        async serverWillStart() {
          return {
            async drainServer() {
              await serverCleanup.dispose();
            },
          };
        },
      },
      depthLimitPlugin,
      complexityLimitPlugin,
      loggingPlugin,
    ],
    introspection: process.env.NODE_ENV !== 'production',
  });

  // Start Apollo Server
  await server.start();

  // Apply Apollo middleware
  app.use(
    '/graphql',
    authMiddleware,
    expressMiddleware(server, {
      context: async ({ req }) => ({
        ...await context({ req }),
        dataSources: dataSources(),
      }),
    })
  );

  // Health check endpoint
  app.get('/health', (req, res) => {
    res.json({ status: 'healthy', service: 'novel-graphql-gateway' });
  });

  // Start HTTP server
  httpServer.listen(PORT, () => {
    console.log(`🚀 GraphQL server ready at http://localhost:${PORT}/graphql`);
    console.log(`🚀 Subscriptions ready at ws://localhost:${PORT}/graphql`);
  });

  // Graceful shutdown
  process.on('SIGTERM', async () => {
    console.log('SIGTERM signal received: closing HTTP server');
    await server.stop();
    httpServer.close(() => {
      console.log('HTTP server closed');
    });
  });
}

// Start the server
startServer().catch((err) => {
  console.error('Error starting server:', err);
  process.exit(1);
});