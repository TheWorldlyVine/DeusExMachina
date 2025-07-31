<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${project_name} - ${environment}</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        
        .container {
            text-align: center;
            padding: 2rem;
            background: rgba(255, 255, 255, 0.1);
            border-radius: 20px;
            backdrop-filter: blur(10px);
            box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.37);
            border: 1px solid rgba(255, 255, 255, 0.18);
            max-width: 600px;
            margin: 0 1rem;
        }
        
        h1 {
            font-size: 3rem;
            margin-bottom: 1rem;
            text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
        }
        
        .subtitle {
            font-size: 1.5rem;
            margin-bottom: 2rem;
            opacity: 0.9;
        }
        
        .info {
            background: rgba(255, 255, 255, 0.2);
            border-radius: 10px;
            padding: 1.5rem;
            margin: 1rem 0;
        }
        
        .info h2 {
            margin-bottom: 1rem;
            font-size: 1.3rem;
        }
        
        .info-item {
            margin: 0.5rem 0;
            font-size: 0.95rem;
            opacity: 0.9;
        }
        
        .timestamp {
            margin-top: 2rem;
            font-size: 0.8rem;
            opacity: 0.7;
        }
        
        .status {
            display: inline-block;
            background: #4CAF50;
            padding: 0.5rem 1rem;
            border-radius: 20px;
            margin-top: 1rem;
            font-weight: bold;
        }
        
        @media (max-width: 768px) {
            h1 {
                font-size: 2rem;
            }
            .subtitle {
                font-size: 1.2rem;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>🚀 ${project_name}</h1>
        <p class="subtitle">Static Hosting Test Page</p>
        
        <div class="info">
            <h2>Deployment Information</h2>
            <div class="info-item">
                <strong>Environment:</strong> ${environment}
            </div>
            <div class="info-item">
                <strong>Status:</strong> <span class="status">✅ Active</span>
            </div>
            <div class="info-item">
                <strong>CDN:</strong> Google Cloud CDN
            </div>
            <div class="info-item">
                <strong>Storage:</strong> Google Cloud Storage
            </div>
        </div>
        
        <div class="info">
            <h2>Features</h2>
            <div class="info-item">✨ Global CDN distribution</div>
            <div class="info-item">🔒 HTTPS with auto-managed SSL</div>
            <div class="info-item">⚡ Edge caching for performance</div>
            <div class="info-item">🛡️ Cloud Armor security</div>
        </div>
        
        <p class="timestamp">
            Deployed at: ${timestamp}
        </p>
    </div>
    
    <script>
        // Simple connectivity test
        console.log('${project_name} static hosting is working!');
        console.log('Environment:', '${environment}');
        console.log('Deployment time:', '${timestamp}');
        
        // Test API connectivity (if available)
        if (window.location.pathname === '/') {
            fetch('/api/health')
                .then(response => {
                    console.log('API health check:', response.status);
                })
                .catch(error => {
                    console.log('API not configured or unavailable:', error.message);
                });
        }
    </script>
</body>
</html>