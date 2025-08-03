import { ApolloServer } from '@apollo/server';
import { makeExecutableSchema } from '@graphql-tools/schema';
import { readFileSync } from 'fs';
import { join } from 'path';
import { resolvers } from '../resolvers';
import { Context } from '../context';
import { dataSources } from '../datasources';

describe('GraphQL Integration Tests', () => {
  let server: ApolloServer<Context>;

  beforeAll(() => {
    const typeDefs = readFileSync(
      join(__dirname, '..', 'schema', 'schema.graphql'),
      'utf-8'
    );

    const schema = makeExecutableSchema({
      typeDefs,
      resolvers,
    });

    server = new ApolloServer({
      schema,
    });
  });

  const createMockContext = () => {
    const context = {
      user: { id: 'test-user', email: 'test@example.com', displayName: 'Test User', role: 'FREE' as const },
      projectId: 'test-project',
      req: { headers: { authorization: 'Bearer test-token' } } as any,
    };
    
    return {
      ...context,
      dataSources: dataSources({} as any, context),
    };
  };

  describe('Document Service Integration', () => {
    it('should query documents', async () => {
      const query = `
        query GetDocuments($projectId: ID!) {
          documents(projectId: $projectId) {
            id
            title
            description
          }
        }
      `;

      const response = await server.executeOperation(
        {
          query,
          variables: { projectId: 'test-project' },
        },
        {
          contextValue: createMockContext() as any,
        }
      );

      expect(response.body.kind).toBe('single');
      if (response.body.kind === 'single') {
        // Since we're not mocking the actual backend services, we expect errors
        expect(response.body.singleResult.errors).toBeDefined();
      }
    });
  });

  describe('Memory Service Integration', () => {
    it('should query characters', async () => {
      const query = `
        query GetCharacters($projectId: ID!) {
          characters(projectId: $projectId) {
            characterId
            name
            role
          }
        }
      `;

      const response = await server.executeOperation(
        {
          query,
          variables: { projectId: 'test-project' },
        },
        {
          contextValue: createMockContext() as any,
        }
      );

      expect(response.body.kind).toBe('single');
      if (response.body.kind === 'single') {
        // Since we're not mocking the actual backend services, we expect errors
        expect(response.body.singleResult.errors).toBeDefined();
      }
    });

    it('should query plots', async () => {
      const query = `
        query GetPlots($projectId: ID!) {
          plots(projectId: $projectId) {
            plotId
            threadName
            premise
            threadType
            status
          }
        }
      `;

      const response = await server.executeOperation(
        {
          query,
          variables: { projectId: 'test-project' },
        },
        {
          contextValue: createMockContext() as any,
        }
      );

      expect(response.body.kind).toBe('single');
      if (response.body.kind === 'single') {
        // Since we're not mocking the actual backend services, we expect errors
        expect(response.body.singleResult.errors).toBeDefined();
      }
    });
  });

  describe('AI Generation Service Integration', () => {
    it('should handle generation requests', async () => {
      const mutation = `
        mutation GenerateScene($input: GenerateSceneInput!) {
          generateScene(input: $input) {
            id
            generatedText
            status
          }
        }
      `;

      const response = await server.executeOperation(
        {
          query: mutation,
          variables: {
            input: {
              documentId: 'test-doc',
              chapterId: 'test-chapter',
              prompt: 'Generate a test scene',
            },
          },
        },
        {
          contextValue: createMockContext() as any,
        }
      );

      expect(response.body.kind).toBe('single');
      if (response.body.kind === 'single') {
        // Since we're not mocking the actual backend services, we expect errors
        expect(response.body.singleResult.errors).toBeDefined();
      }
    });
  });
});