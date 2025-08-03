import { ApolloServer } from '@apollo/server';
import { makeExecutableSchema } from '@graphql-tools/schema';
import { readFileSync } from 'fs';
import { join } from 'path';
import { resolvers } from '../resolvers';
import { Context } from '../context';

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
          contextValue: {
            user: { id: 'test-user', email: 'test@example.com' },
            projectId: 'test-project',
            req: { headers: { authorization: 'Bearer test-token' } },
          } as Context,
        }
      );

      expect(response.body.kind).toBe('single');
      if (response.body.kind === 'single') {
        expect(response.body.singleResult.errors).toBeUndefined();
        expect(response.body.singleResult.data).toBeDefined();
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
          contextValue: {
            user: { id: 'test-user', email: 'test@example.com' },
            projectId: 'test-project',
            req: { headers: { authorization: 'Bearer test-token' } },
          } as Context,
        }
      );

      expect(response.body.kind).toBe('single');
      if (response.body.kind === 'single') {
        expect(response.body.singleResult.errors).toBeUndefined();
        expect(response.body.singleResult.data).toBeDefined();
      }
    });

    it('should query plots', async () => {
      const query = `
        query GetPlots($projectId: ID!) {
          plots(projectId: $projectId) {
            plotId
            title
            storyArc
          }
        }
      `;

      const response = await server.executeOperation(
        {
          query,
          variables: { projectId: 'test-project' },
        },
        {
          contextValue: {
            user: { id: 'test-user', email: 'test@example.com' },
            projectId: 'test-project',
            req: { headers: { authorization: 'Bearer test-token' } },
          } as Context,
        }
      );

      expect(response.body.kind).toBe('single');
      if (response.body.kind === 'single') {
        expect(response.body.singleResult.errors).toBeUndefined();
        expect(response.body.singleResult.data).toBeDefined();
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
          contextValue: {
            user: { id: 'test-user', email: 'test@example.com' },
            projectId: 'test-project',
            req: { headers: { authorization: 'Bearer test-token' } },
          } as Context,
        }
      );

      expect(response.body.kind).toBe('single');
      if (response.body.kind === 'single') {
        // We expect this to fail with proper error since we're not mocking the service
        expect(response.body.singleResult.errors).toBeDefined();
      }
    });
  });
});