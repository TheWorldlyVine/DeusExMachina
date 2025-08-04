import { describe, it, expect, beforeAll, afterAll } from 'vitest';
// @ts-expect-error - Playwright types will be available when package is installed
import { chromium, Browser, Page } from '@playwright/test';

describe('SPA Routing E2E Tests', () => {
  let browser: Browser;
  let page: Page;
  const baseUrl = process.env.E2E_BASE_URL || 'https://god-in-a-box.com';
  const appPath = '/novel-creator';

  beforeAll(async () => {
    browser = await chromium.launch();
    page = await browser.newPage();
  });

  afterAll(async () => {
    await browser.close();
  });

  describe('Direct Navigation', () => {
    it('should load the app root directly', async () => {
      const response = await page.goto(`${baseUrl}${appPath}/`);
      expect(response?.status()).toBe(200);
      
      // Wait for React app to mount
      await page.waitForSelector('#root', { timeout: 10000 });
      
      // Check that the app loaded
      const rootElement = await page.$('#root');
      expect(rootElement).toBeTruthy();
    });

    it('should load documents page directly', async () => {
      const response = await page.goto(`${baseUrl}${appPath}/documents`);
      expect(response?.status()).toBe(200);
      
      // Should not redirect to root
      expect(page.url()).toContain('/documents');
      
      // Wait for documents page to load
      await page.waitForSelector('[data-testid="documents-page"]', { timeout: 10000 });
    });

    it('should load editor page with document ID directly', async () => {
      const testDocId = 'test-doc-123';
      const response = await page.goto(`${baseUrl}${appPath}/editor/${testDocId}`);
      expect(response?.status()).toBe(200);
      
      // URL should be preserved
      expect(page.url()).toContain(`/editor/${testDocId}`);
      
      // Wait for editor to load
      await page.waitForSelector('[data-testid="editor-page"]', { timeout: 10000 });
    });

    it('should load settings page directly', async () => {
      const response = await page.goto(`${baseUrl}${appPath}/settings`);
      expect(response?.status()).toBe(200);
      
      expect(page.url()).toContain('/settings');
      await page.waitForSelector('[data-testid="settings-page"]', { timeout: 10000 });
    });
  });

  describe('Page Refresh', () => {
    it('should maintain documents route on refresh', async () => {
      // Navigate to documents
      await page.goto(`${baseUrl}${appPath}/documents`);
      await page.waitForSelector('[data-testid="documents-page"]');
      
      // Refresh the page
      await page.reload();
      
      // Should still be on documents page
      expect(page.url()).toContain('/documents');
      await page.waitForSelector('[data-testid="documents-page"]', { timeout: 10000 });
    });

    it('should maintain editor route with document ID on refresh', async () => {
      const testDocId = 'refresh-test-456';
      
      // Navigate to editor
      await page.goto(`${baseUrl}${appPath}/editor/${testDocId}`);
      await page.waitForSelector('[data-testid="editor-page"]');
      
      // Refresh the page
      await page.reload();
      
      // Should still be on editor page with same ID
      expect(page.url()).toContain(`/editor/${testDocId}`);
      await page.waitForSelector('[data-testid="editor-page"]', { timeout: 10000 });
    });

    it('should maintain nested route on refresh', async () => {
      // Navigate to a nested route
      await page.goto(`${baseUrl}${appPath}/settings/profile`);
      
      // Refresh the page
      await page.reload();
      
      // Should maintain the full path
      expect(page.url()).toContain('/settings/profile');
    });
  });

  describe('Browser Navigation', () => {
    it('should handle back/forward navigation correctly', async () => {
      // Start at root
      await page.goto(`${baseUrl}${appPath}/`);
      
      // Navigate to documents
      await page.goto(`${baseUrl}${appPath}/documents`);
      await page.waitForSelector('[data-testid="documents-page"]');
      
      // Navigate to editor
      await page.goto(`${baseUrl}${appPath}/editor/nav-test-789`);
      await page.waitForSelector('[data-testid="editor-page"]');
      
      // Go back
      await page.goBack();
      expect(page.url()).toContain('/documents');
      await page.waitForSelector('[data-testid="documents-page"]');
      
      // Go forward
      await page.goForward();
      expect(page.url()).toContain('/editor/nav-test-789');
      await page.waitForSelector('[data-testid="editor-page"]');
    });
  });

  describe('Static Assets', () => {
    it('should serve JavaScript files directly', async () => {
      // This would be the actual built JS file
      const response = await page.goto(`${baseUrl}${appPath}/assets/index.js`);
      
      if (response) {
        const contentType = response.headers()['content-type'];
        expect(contentType).toContain('javascript');
      }
    });

    it('should return 404 for non-existent static files', async () => {
      const response = await page.goto(`${baseUrl}${appPath}/assets/non-existent.js`);
      expect(response?.status()).toBe(404);
    });

    it('should not rewrite paths for static file extensions', async () => {
      const staticPaths = [
        '/assets/style.css',
        '/images/logo.png',
        '/fonts/roboto.woff2',
      ];

      for (const path of staticPaths) {
        const response = await page.goto(`${baseUrl}${appPath}${path}`, {
          waitUntil: 'domcontentloaded',
        });
        
        // Should not serve index.html for these paths
        const contentType = response?.headers()['content-type'] || '';
        expect(contentType).not.toContain('text/html');
      }
    });
  });

  describe('Cross-App Navigation', () => {
    it('should not interfere with other apps', async () => {
      // Navigate to web-app (if it exists)
      const webAppResponse = await page.goto(`${baseUrl}/web-app/dashboard`);
      
      if (webAppResponse?.status() === 200) {
        expect(page.url()).toContain('/web-app/dashboard');
        // Should not serve novel-creator's index.html
        const content = await page.content();
        expect(content).not.toContain('novel-creator');
      }
    });
  });

  describe('Performance', () => {
    it('should load index.html with proper cache headers', async () => {
      const response = await page.goto(`${baseUrl}${appPath}/documents`);
      
      if (response) {
        const headers = response.headers();
        
        // Check cache headers for HTML (should be short TTL)
        const cacheControl = headers['cache-control'];
        expect(cacheControl).toBeTruthy();
        
        // HTML should not be cached for long
        if (cacheControl.includes('max-age')) {
          const maxAge = parseInt(cacheControl.match(/max-age=(\d+)/)?.[1] || '0');
          expect(maxAge).toBeLessThanOrEqual(300); // 5 minutes or less
        }
      }
    });

    it('should preserve original path in headers', async () => {
      const response = await page.goto(`${baseUrl}${appPath}/editor/perf-test`);
      
      if (response) {
        const headers = response.headers();
        
        // Check if custom headers are present (if implemented)
        const originalPath = headers['x-original-path'];
        if (originalPath) {
          expect(originalPath).toContain('/editor/perf-test');
        }
      }
    });
  });
});

describe('SPA Routing Integration with React Router', () => {
  let browser: Browser;
  let page: Page;
  const baseUrl = process.env.E2E_BASE_URL || 'https://god-in-a-box.com';
  const appPath = '/novel-creator';

  beforeAll(async () => {
    browser = await chromium.launch();
    page = await browser.newPage();
  });

  afterAll(async () => {
    await browser.close();
  });

  it('should handle client-side navigation after direct route access', async () => {
    // Directly access a deep route
    await page.goto(`${baseUrl}${appPath}/editor/client-nav-test`);
    await page.waitForSelector('[data-testid="editor-page"]');
    
    // Navigate to documents using client-side navigation
    const documentsLink = await page.$('a[href="/novel-creator/documents"]');
    if (documentsLink) {
      await documentsLink.click();
      
      // Should navigate without page reload
      await page.waitForSelector('[data-testid="documents-page"]');
      expect(page.url()).toContain('/documents');
    }
  });

  it('should handle authenticated routes correctly', async () => {
    // Try to access protected route
    await page.goto(`${baseUrl}${appPath}/editor/auth-test`);
    
    // Should either show login or editor based on auth state
    const isAuthenticated = await page.$('[data-testid="editor-page"]');
    const isLoginPage = await page.$('[data-testid="login-page"]');
    
    expect(isAuthenticated || isLoginPage).toBeTruthy();
  });
});