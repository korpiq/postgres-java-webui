import React, { useState, useEffect } from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Routes, Route, useNavigate, Navigate, useParams, Link } from 'react-router-dom';

const getCookie = (name: string) => {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop()?.split(';').shift();
  return undefined;
};

const LoginPage: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [databases, setDatabases] = useState<string[]>([]);
  const [selectedDb, setSelectedDb] = useState('');
  const [step, setStep] = useState<'credentials' | 'databases'>('credentials');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleFetchDatabases = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    try {
      const response = await fetch('/api/databases', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username, password }),
      });

      if (response.ok) {
        const data = await response.json();
        setDatabases(data.databases || []);
        if (data.databases && data.databases.length > 0) {
          setSelectedDb(data.databases[0]);
        }
        setStep('databases');
      } else {
        const data = await response.json();
        setError(data.error || 'Failed to fetch databases');
      }
    } catch (err) {
      setError('An error occurred during database fetch');
    }
  };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    try {
      const response = await fetch('/api/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username, password, dbName: selectedDb }),
      });

      if (response.ok) {
        const data = await response.json();
        // Cookie is set by the backend with correct path and name
        navigate(data.redirect || `/db/${selectedDb}`);
      } else {
        const data = await response.json();
        setError(data.error || 'Login failed');
      }
    } catch (err) {
      setError('An error occurred during login');
    }
  };

  if (step === 'credentials') {
    return (
      <div>
        <h1>Postgres Java WebUI</h1>
        <h2>Login</h2>
        <form onSubmit={handleFetchDatabases}>
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
          <button type="submit">List Databases</button>
        </form>
      </div>
    );
  }

  return (
    <div>
      <h1>Postgres Java WebUI</h1>
      <h2>Select Database</h2>
      <form onSubmit={handleLogin}>
        <div>
          <label htmlFor="database">Database:</label>
          <select
            id="database"
            value={selectedDb}
            onChange={(e) => setSelectedDb(e.target.value)}
            required
          >
            {databases.map((db) => (
              <option key={db} value={db}>
                {db}
              </option>
            ))}
          </select>
        </div>
        {error && <p style={{ color: 'red' }}>{error}</p>}
        <button type="submit">Connect</button>
        <button type="button" onClick={() => setStep('credentials')}>Back</button>
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
    // On the root page, we don't have a specific dbName, so we don't know which cookie to look for.
    // However, the requirement says "Modify list databases endpoint to work with either username+password or JWT"
    // AND "In authenticated frontend pages, look for cookie by name of pogrejab_<dbName> where dbName comes from the URL path"
    
    // For the root page, maybe we should just allow ANY pogrejab_* cookie?
    // Or maybe the root page should use a generic 'jwt' cookie if we still want to list databases while logged in?
    
    // Re-reading: "Modify list databases endpoint to work with either username+password or JWT (for listing them while logged in, if possible without reconnecting)"
    
    // If I have a session for db1, I should be able to list databases.
    // Where is the JWT for that session? It's in pogrejab_db1 cookie.
    
    const cookies = document.cookie.split(';').map(c => c.trim());
    const authCookie = cookies.find(c => c.startsWith('pogrejab_'));
    const jwt = authCookie ? authCookie.split('=')[1] : undefined;

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
        } else if (response.status === 401 || response.status === 403) {
          // Clear all pogrejab cookies?
          document.cookie.split(";").forEach((c) => {
            const name = c.split("=")[0].trim();
            if (name.startsWith("pogrejab_")) {
              document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;`;
              // Try to clear with path /db/ too if we know it, but we don't here.
            }
          });
          navigate('/login');
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
            <li key={db}>
              <Link to={`/db/${db}`}>{db}</Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

const SchemasPage: React.FC = () => {
  const { dbName } = useParams<{ dbName: string }>();
  const [loading, setLoading] = useState(true);
  const [authenticated, setAuthenticated] = useState(false);
  const [schemas, setSchemas] = useState<string[]>([]);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    // Try to find a cookie for THIS specific database first
    let jwt = getCookie(`pogrejab_${dbName}`);
    
    // If not found, try to find ANY pogrejab_ cookie (at root path)
    if (!jwt) {
      const cookies = document.cookie.split(';').map(c => c.trim());
      const authCookie = cookies.find(c => c.startsWith(`pogrejab_${dbName}=`));
      if (authCookie) {
        jwt = authCookie.split('=')[1];
      }
    }

    if (!jwt) {
      navigate('/login');
      return;
    }

    setAuthenticated(true);

    const fetchSchemas = async () => {
      try {
        const response = await fetch(`/api/databases/${dbName}/schemas`, {
          headers: {
            'Authorization': `Bearer ${jwt}`
          }
        });

        if (response.ok) {
          const data = await response.json();
          setSchemas(data.schemas || []);
        } else if (response.status === 401 || response.status === 403) {
          navigate('/login');
        } else {
          const data = await response.json();
          setError(data.error || 'Failed to fetch schemas');
        }
      } catch (err) {
        setError('An error occurred while fetching schemas');
      } finally {
        setLoading(false);
      }
    };

    if (dbName) {
      fetchSchemas();
    }
  }, [navigate, dbName]);

  if (loading) return <div>Loading...</div>;
  if (!authenticated) return null;

  return (
    <div>
      <h1>Postgres Java WebUI</h1>
      <h2>Schemas in {dbName}</h2>
      {error && <p style={{ color: 'red' }}>{error}</p>}
      {schemas.length === 0 ? (
        <p>No schemas found or access denied.</p>
      ) : (
        <ul>
          {schemas.map((schema) => (
            <li key={schema}>{schema}</li>
          ))}
        </ul>
      )}
      <button onClick={() => navigate('/')}>Back to Databases</button>
    </div>
  );
};

const App: React.FC = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<RootPage />} />
        <Route path="/db/:dbName" element={<SchemasPage />} />
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
