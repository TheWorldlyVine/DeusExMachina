import { ApolloServerPlugin } from '@apollo/server';
import { GraphQLError } from 'graphql';
import {
  FieldNode,
  FragmentDefinitionNode,
  FragmentSpreadNode,
  InlineFragmentNode,
  OperationDefinitionNode,
  SelectionNode,
} from 'graphql';
import { Context } from '../context';

const MAX_COMPLEXITY = parseInt(process.env.GRAPHQL_COMPLEXITY_LIMIT || '1000');

export const complexityLimitPlugin: ApolloServerPlugin<Context> = {
  async requestDidStart() {
    return {
      async validationDidStart(requestContext) {
        // Access document from the request context
        const { document } = requestContext;
        if (!document) return;

        const complexity = calculateComplexity(
          document.definitions[0] as OperationDefinitionNode,
          document.definitions.reduce((acc, def) => {
            if (def.kind === 'FragmentDefinition') {
              acc[def.name.value] = def;
            }
            return acc;
          }, {} as Record<string, FragmentDefinitionNode>)
        );

        if (complexity > MAX_COMPLEXITY) {
          throw new GraphQLError(
            `Query complexity of ${complexity} exceeds maximum complexity of ${MAX_COMPLEXITY}`,
            {
              extensions: {
                code: 'QUERY_TOO_COMPLEX',
                complexity,
                maxComplexity: MAX_COMPLEXITY,
              },
            }
          );
        }
      },
    };
  },
};

function calculateComplexity(
  node: OperationDefinitionNode | FragmentDefinitionNode | InlineFragmentNode,
  fragments: Record<string, FragmentDefinitionNode>,
  depth: number = 0
): number {
  let complexity = 0;

  if (node.selectionSet) {
    node.selectionSet.selections.forEach((selection) => {
      complexity += calculateSelectionComplexity(selection, fragments, depth);
    });
  }

  return complexity;
}

function calculateSelectionComplexity(
  selection: SelectionNode,
  fragments: Record<string, FragmentDefinitionNode>,
  depth: number
): number {
  switch (selection.kind) {
    case 'Field':
      return calculateFieldComplexity(selection, fragments, depth);
    case 'FragmentSpread':
      return calculateFragmentSpreadComplexity(selection, fragments, depth);
    case 'InlineFragment':
      return calculateComplexity(selection, fragments, depth);
    default:
      return 0;
  }
}

function calculateFieldComplexity(
  field: FieldNode,
  fragments: Record<string, FragmentDefinitionNode>,
  depth: number
): number {
  let complexity = 1; // Base complexity for any field

  // Add complexity for depth
  complexity += depth * 0.5;

  // Add complexity for arguments
  if (field.arguments && field.arguments.length > 0) {
    complexity += field.arguments.length * 0.2;
  }

  // Special handling for list fields
  if (field.name.value.endsWith('s') || field.name.value.endsWith('List')) {
    complexity += 10; // Lists are more expensive
  }

  // Recursively calculate complexity for nested selections
  if (field.selectionSet) {
    field.selectionSet.selections.forEach((selection) => {
      complexity += calculateSelectionComplexity(selection, fragments, depth + 1);
    });
  }

  return complexity;
}

function calculateFragmentSpreadComplexity(
  fragmentSpread: FragmentSpreadNode,
  fragments: Record<string, FragmentDefinitionNode>,
  depth: number
): number {
  const fragment = fragments[fragmentSpread.name.value];
  if (!fragment) {
    return 0;
  }
  return calculateComplexity(fragment, fragments, depth);
}