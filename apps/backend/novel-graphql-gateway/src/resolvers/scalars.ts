import { GraphQLScalarType, Kind } from 'graphql';

export const scalarResolvers = {
  JSON: new GraphQLScalarType({
    name: 'JSON',
    description: 'The `JSON` scalar type represents JSON values',
    serialize(value: any) {
      return value; // value sent to client
    },
    parseValue(value: any) {
      return value; // value from client input variables
    },
    parseLiteral(ast) {
      switch (ast.kind) {
        case Kind.STRING:
          return JSON.parse(ast.value);
        case Kind.BOOLEAN:
          return ast.value;
        case Kind.INT:
        case Kind.FLOAT:
          return parseFloat(ast.value);
        case Kind.OBJECT: {
          const value = Object.create(null);
          ast.fields.forEach((field) => {
            value[field.name.value] = parseLiteral(field.value);
          });
          return value;
        }
        case Kind.LIST:
          return ast.values.map(parseLiteral);
        case Kind.NULL:
          return null;
        default:
          return undefined;
      }
    },
  }),
};

function parseLiteral(ast: any): any {
  switch (ast.kind) {
    case Kind.STRING:
    case Kind.BOOLEAN:
      return ast.value;
    case Kind.INT:
    case Kind.FLOAT:
      return parseFloat(ast.value);
    case Kind.OBJECT: {
      const value = Object.create(null);
      ast.fields.forEach((field: any) => {
        value[field.name.value] = parseLiteral(field.value);
      });
      return value;
    }
    case Kind.LIST:
      return ast.values.map(parseLiteral);
    case Kind.NULL:
      return null;
    default:
      return undefined;
  }
}