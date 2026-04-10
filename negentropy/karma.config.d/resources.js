// Serve test resource .txt files so browser tests can load them via XHR at /base/kotlin/*.txt
config.files.push({
    pattern: config.basePath + '/kotlin/*.txt',
    watched: false,
    included: false,
    served: true,
    nocache: false
});

// Provide empty module for Node.js built-ins so webpack can bundle without errors.
// The readTestResource implementation uses typeof-window detection to route to the
// XHR path in browsers, so require('fs') is never actually called there.
config.webpack.externals = Object.assign(config.webpack.externals || {}, { fs: '{}' });

// Allow long-running tests (large file loading + processing) without browser disconnect.
config.browserDisconnectTimeout = 60000;
config.browserNoActivityTimeout = 120000;
config.pingTimeout = 120000;

// Extend Mocha per-test timeout to match the useMocha { timeout } setting in build.gradle.kts.
config.client = config.client || {};
config.client.mocha = config.client.mocha || {};
config.client.mocha.timeout = 120000;
