# UI Components

Shared React component library for the DeusExMachina platform.

## Installation

```bash
pnpm add @deusexmachina/ui-components
```

## Components

### Button
A versatile button component with multiple variants and states.

```tsx
import { Button } from '@deusexmachina/ui-components';

<Button variant="primary" size="md" onClick={handleClick}>
  Click me
</Button>
```

**Props:**
- `variant`: 'primary' | 'secondary' | 'ghost' | 'danger'
- `size`: 'sm' | 'md' | 'lg'
- `fullWidth`: boolean
- `loading`: boolean

### Card
A container component for grouping related content.

```tsx
import { Card } from '@deusexmachina/ui-components';

<Card variant="bordered" padding="lg">
  <h2>Card Title</h2>
  <p>Card content goes here</p>
</Card>
```

**Props:**
- `variant`: 'default' | 'bordered' | 'ghost'
- `padding`: 'none' | 'sm' | 'md' | 'lg'

### Input
A form input component with label and error handling.

```tsx
import { Input } from '@deusexmachina/ui-components';

<Input
  label="Email"
  type="email"
  error={errors.email}
  helperText="We'll never share your email"
/>
```

**Props:**
- `label`: string
- `error`: string
- `helperText`: string
- `fullWidth`: boolean

### Modal
A dialog component for overlaying content.

```tsx
import { Modal } from '@deusexmachina/ui-components';

<Modal
  isOpen={isOpen}
  onClose={handleClose}
  title="Modal Title"
  size="md"
>
  <p>Modal content</p>
</Modal>
```

**Props:**
- `isOpen`: boolean
- `onClose`: () => void
- `title`: string
- `size`: 'sm' | 'md' | 'lg' | 'xl'
- `closeOnOverlayClick`: boolean

### Spinner
A loading indicator component.

```tsx
import { Spinner } from '@deusexmachina/ui-components';

<Spinner size="md" color="primary" />
```

**Props:**
- `size`: 'sm' | 'md' | 'lg'
- `color`: 'primary' | 'white' | 'gray'

### Alert
A component for displaying important messages.

```tsx
import { Alert } from '@deusexmachina/ui-components';

<Alert variant="success" title="Success!" onClose={handleClose}>
  Your action was completed successfully.
</Alert>
```

**Props:**
- `variant`: 'info' | 'success' | 'warning' | 'error'
- `title`: string
- `onClose`: () => void

## Hooks

### useTheme
Hook for managing theme preferences.

```tsx
import { useTheme } from '@deusexmachina/ui-components';

const { theme, setTheme, resolvedTheme } = useTheme();
```

### useMediaQuery
Hook for responsive design.

```tsx
import { useMediaQuery, useIsMobile } from '@deusexmachina/ui-components';

const isLargeScreen = useMediaQuery('(min-width: 1024px)');
const isMobile = useIsMobile();
```

## Development

### Setup
```bash
pnpm install
```

### Build
```bash
pnpm run build
```

### Run tests
```bash
pnpm test
```

### Storybook
```bash
pnpm run storybook
```

## License

MIT