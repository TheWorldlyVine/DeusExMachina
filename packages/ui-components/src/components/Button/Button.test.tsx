import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { Button } from './Button';

describe('Button', () => {
  it('renders children correctly', () => {
    render(<Button>Click me</Button>);
    expect(screen.getByRole('button')).toHaveTextContent('Click me');
  });

  it('applies variant classes', () => {
    const { rerender } = render(<Button variant="primary">Button</Button>);
    const button = screen.getByRole('button');
    const classes = button.className;
    expect(classes).toMatch(/primary/);

    rerender(<Button variant="secondary">Button</Button>);
    expect(button.className).toMatch(/secondary/);

    rerender(<Button variant="ghost">Button</Button>);
    expect(button.className).toMatch(/ghost/);

    rerender(<Button variant="danger">Button</Button>);
    expect(button.className).toMatch(/danger/);
  });

  it('applies size classes', () => {
    const { rerender } = render(<Button size="sm">Button</Button>);
    const button = screen.getByRole('button');
    expect(button.className).toMatch(/sm/);

    rerender(<Button size="md">Button</Button>);
    expect(button.className).toMatch(/md/);

    rerender(<Button size="lg">Button</Button>);
    expect(button.className).toMatch(/lg/);
  });

  it('applies fullWidth class when fullWidth prop is true', () => {
    render(<Button fullWidth>Button</Button>);
    expect(screen.getByRole('button').className).toMatch(/fullWidth/);
  });

  it('shows loading spinner when loading prop is true', () => {
    render(<Button loading>Button</Button>);
    const button = screen.getByRole('button');
    expect(button.className).toMatch(/loading/);
    expect(button).toBeDisabled();
  });

  it('disables button when disabled prop is true', () => {
    render(<Button disabled>Button</Button>);
    expect(screen.getByRole('button')).toBeDisabled();
  });

  it('handles click events', async () => {
    const handleClick = vi.fn();
    const user = userEvent.setup();
    
    render(<Button onClick={handleClick}>Click me</Button>);
    await user.click(screen.getByRole('button'));
    
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('does not fire click events when disabled', async () => {
    const handleClick = vi.fn();
    const user = userEvent.setup();
    
    render(<Button disabled onClick={handleClick}>Click me</Button>);
    await user.click(screen.getByRole('button'));
    
    expect(handleClick).not.toHaveBeenCalled();
  });

  it('does not fire click events when loading', async () => {
    const handleClick = vi.fn();
    const user = userEvent.setup();
    
    render(<Button loading onClick={handleClick}>Click me</Button>);
    await user.click(screen.getByRole('button'));
    
    expect(handleClick).not.toHaveBeenCalled();
  });

  it('forwards ref correctly', () => {
    const ref = vi.fn();
    render(<Button ref={ref}>Button</Button>);
    expect(ref).toHaveBeenCalled();
    expect(ref.mock.calls[0][0]).toBeInstanceOf(HTMLButtonElement);
  });
});