import { createBrowserRouter, RouterProvider, Navigate } from 'react-router-dom';
import Login from './auth/Login.tsx';
import Home from './Page/Home.tsx';
import AdminDashboard from './Admin/AdminDahboard.tsx';

import { getUserRole, hasRole } from './auth/authService';

const PrivateRoute = ({ children, requiredRole = null }) => {
    // Non authentifié
    if (!getUserRole()) {
        return <Navigate to="/login" replace />;
    }

    // A le rôle requis → OK
    if (!requiredRole || hasRole(requiredRole)) {
        return children;
    }

    // N'a pas le rôle → redirection vers son interface principale
    const primaryRole = getUserRole().toLowerCase();
    const routes: Record<string, string> = {
        admin: '/admin',
        editor: '/editeur',
        user: '/user'
    };
    return <Navigate to={routes[primaryRole] || '/login'} replace />;
};

const router = createBrowserRouter([
  { path: '/', element: <Home /> },
  { path: '/login', element: <Login /> },

  {
    path: '/admin', element:
      <PrivateRoute requiredRole="ADMIN">
        <AdminDashboard />
      </PrivateRoute>
  },
  { path: '/editeur', element: <PrivateRoute requiredRole="EDITOR"><div>Page editeur</div></PrivateRoute> },
  { path: '/user', element: <PrivateRoute requiredRole="USER"><div>Page utilisateur</div></PrivateRoute> }
]);

// App principal
function App() {
  return <RouterProvider router={router} />;
}

export default App;
