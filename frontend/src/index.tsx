import React, { useState, useEffect } from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Routes, Route, useNavigate, Navigate } from 'react-router-dom';

const getCookie = (name: string) => {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop()?.split(';').shift();
  return undefined;
};

const LoginPage: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    try {
      const response = await fetch('/api/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username, password }),
      });

      if (response.ok) {
        const data = await response.json();
        // Set the JWT cookie. In a real app we might want to set secure: true etc.
        // For simplicity we just set it here.
        document.cookie = `jwt=${data.token}; path=/; SameSite=Strict`;
        navigate('/');
      } else {
        const data = await response.json();
        setError(data.error || 'Login failed');
      }
    } catch (err) {
      setError('An error occurred during login');
    }
  };

  return (
    <div>
      <h1>Postgres Java WebUI</h1>
      <h2>Login</h2>
      <form onSubmit={handleLogin}>
        <div>
          <label htmlFor="username">Username:</label>
          <input
            type="text"
            id="username"
            name="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />
        </div>
        <div>
          <label htmlFor="password">Password:</label>
          <input
            type="password"
            id="password"
            name="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>
        {error && <p style={{ color: 'red' }}>{error}</p>}
        <button type="submit">Login</button>
      </form>
    </div>
  );
};

const RootPage: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [authenticated, setAuthenticated] = useState(false);
  const [databases, setDatabases] = useState<string[]>([]);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    const jwt = getCookie('jwt');
    if (!jwt) {
      navigate('/login');
      return;
    }
    
    setAuthenticated(true);

    const fetchDatabases = async () => {
      try {
        const response = await fetch('/api/databases', {
          headers: {
            'Authorization': `Bearer ${jwt}`
          }
        });

        if (response.ok) {
          const data = await response.json();
          setDatabases(data.databases || []);
        } else {
          const data = await response.json();
          setError(data.error || 'Failed to fetch databases');
        }
      } catch (err) {
        setError('An error occurred while fetching databases');
      } finally {
        setLoading(false);
      }
    };

    fetchDatabases();
  }, [navigate]);

  if (loading) return <div>Loading...</div>;
  if (!authenticated) return null;

  return (
    <div>
      <h1>Postgres Java WebUI</h1>
      {error && <p style={{ color: 'red' }}>{error}</p>}
      <h2>Your Databases</h2>
      {databases.length === 0 ? (
        <p>No databases found or access denied.</p>
      ) : (
        <ul>
          {databases.map((db) => (
            <li key={db}>{db}</li>
          ))}
        </ul>
      )}
    </div>
  );
};

const App: React.FC = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<RootPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
};

const rootElement = document.getElementById('root');
if (!rootElement) throw new Error('Failed to find the root element');
const root = ReactDOM.createRoot(rootElement);
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
