import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import React from 'react';
import ReactDOM from 'react-dom/client';
const App = () => {
    return (_jsxs("div", { children: [_jsx("h1", { children: "Postgres Java WebUI" }), _jsx("p", { children: "Empty front page" })] }));
};
const rootElement = document.getElementById('root');
if (!rootElement)
    throw new Error('Failed to find the root element');
const root = ReactDOM.createRoot(rootElement);
root.render(_jsx(React.StrictMode, { children: _jsx(App, {}) }));
//# sourceMappingURL=index.js.map