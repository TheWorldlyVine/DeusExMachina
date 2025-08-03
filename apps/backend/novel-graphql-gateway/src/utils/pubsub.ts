import { PubSub } from 'graphql-subscriptions';

// In production, use Redis or Google Pub/Sub for scalability
export const pubsub = new PubSub();