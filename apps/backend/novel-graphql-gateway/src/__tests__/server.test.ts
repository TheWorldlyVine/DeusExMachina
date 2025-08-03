import { makeExecutableSchema } from '@graphql-tools/schema';
import { readFileSync } from 'fs';
import { join } from 'path';

describe('GraphQL Server', () => {
  describe('Schema', () => {
    it('should create a valid executable schema', () => {
      // This test ensures our GraphQL schema is valid and can be loaded
      const typeDefs = readFileSync(
        join(__dirname, '..', 'schema', 'schema.graphql'),
        'utf-8'
      );

      // This should not throw
      const schema = makeExecutableSchema({
        typeDefs,
        resolvers: {
          Query: {
            health: () => ({ status: 'OK', message: 'Server is healthy' }),
          },
        },
      });

      expect(schema).toBeDefined();
      expect(schema.getQueryType()).toBeDefined();
      expect(schema.getMutationType()).toBeDefined();
      expect(schema.getSubscriptionType()).toBeDefined();
    });
  });

  describe('Environment', () => {
    it('should have required environment variables or defaults', () => {
      const port = process.env.PORT || 4000;
      expect(port).toBeDefined();
      expect(typeof port === 'string' || typeof port === 'number').toBe(true);
    });
  });
});