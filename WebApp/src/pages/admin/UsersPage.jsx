// ============================================================
// File: UsersPage.jsx
// Purpose: Handles CRUD operations for system users (Backoffice/GridOperator).
//          Fetches users, displays them in a table, and provides 
//          modals for creation and deactivation.
// Author: Migara
// ============================================================

import { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Modal from '../../components/common/Modal';
import Toast from '../../components/common/Toast';

export default function UsersPage() {
  // FIX: Shalon's AuthContext exports 'role' directly, not a 'user' object.
  const { role } = useAuth(); 
  
  const [users, setUsers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [toast, setToast] = useState({ message: '', tone: 'error' });

  // Modal state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    role: 'GridOperator', // Default role
    fullName: '',
    email: ''
  });

  // Fetch users on mount
  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    setIsLoading(true);
    setError('');
    try {
      const response = await api.get('/users');
      setUsers(response.data);
    } catch (err) {
      setError('Failed to load users. Please try again.');
      setToast({ message: err.response?.data?.message || 'Failed to load users', tone: 'error' });
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateUser = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await api.post('/users', formData);
      setToast({ message: 'User created successfully!', tone: 'success' });
      setIsModalOpen(false);
      setFormData({ username: '', password: '', role: 'GridOperator', fullName: '', email: '' });
      fetchUsers(); // Refresh the list
    } catch (err) {
      setToast({ message: err.response?.data?.message || 'Failed to create user', tone: 'error' });
    }
  };

  const handleDeactivate = async (userId, username) => {
    if (!window.confirm(`Are you sure you want to deactivate ${username}?`)) {
      return;
    }
    try {
      await api.put(`/users/${userId}/deactivate`);
      setToast({ message: 'User deactivated successfully!', tone: 'success' });
      fetchUsers(); // Refresh the list
    } catch (err) {
      setToast({ message: err.response?.data?.message || 'Failed to deactivate user', tone: 'error' });
    }
  };

  // Check if the logged-in user is a Backoffice admin
  if (role !== 'Backoffice') {
    return (
      <div className="p-8 text-center text-red-600">
        Access Denied: Only Backoffice users can manage system users.
      </div>
    );
  }

  return (
    <div className="p-6 max-w-7xl mx-auto">
            {/* Create User Button - Using standard HTML button to ensure onClick fires reliably */}
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-800">User Management</h1>
        <button 
          onClick={() => {
            console.log("Create button clicked!"); // Debug log
            setIsModalOpen(true);
          }}
          className="bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 px-4 rounded transition-colors"
        >
          + Create New User
        </button>
      </div>

      {error && <div className="bg-red-100 text-red-700 p-3 rounded mb-4">{error}</div>}

      {isLoading ? (
        <div className="text-center py-8 text-gray-500">Loading users...</div>
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Username</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Full Name</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Email</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Role</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {users.length === 0 ? (
                <tr>
                  <td colSpan="6" className="px-6 py-4 text-center text-gray-500">No users found.</td>
                </tr>
              ) : (
                users.map((u) => (
                  <tr key={u.id} className={u.status === 'Deactivated' ? 'bg-gray-50' : ''}>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{u.username}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{u.fullName}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{u.email}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${u.role === 'Backoffice' ? 'bg-purple-100 text-purple-800' : 'bg-green-100 text-green-800'}`}>
                        {u.role}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                      <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${u.status === 'Active' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                        {u.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                      {u.status === 'Active' && (
                        <button 
                          onClick={() => handleDeactivate(u.id, u.username)}
                          className="text-red-600 hover:text-red-900"
                        >
                          Deactivate
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Create User Modal - Passing isOpen prop explicitly */}
      <Modal 
        isOpen={isModalOpen} 
        onClose={() => setIsModalOpen(false)} 
        title="Create New User"
      >
        <form onSubmit={handleCreateUser} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Username</label>
            <Input 
              type="text" 
              required 
              value={formData.username}
              onChange={(e) => setFormData({...formData, username: e.target.value})}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
            <Input 
              type="password" 
              required 
              value={formData.password}
              onChange={(e) => setFormData({...formData, password: e.target.value})}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Full Name</label>
            <Input 
              type="text" 
              required 
              value={formData.fullName}
              onChange={(e) => setFormData({...formData, fullName: e.target.value})}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
            <Input 
              type="email" 
              required 
              value={formData.email}
              onChange={(e) => setFormData({...formData, email: e.target.value})}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Role</label>
            <select 
              className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={formData.role}
              onChange={(e) => setFormData({...formData, role: e.target.value})}
            >
              <option value="Backoffice">Backoffice</option>
              <option value="GridOperator">Grid Operator</option>
            </select>
          </div>
          <div className="flex justify-end gap-2 pt-4">
            <button 
              type="button" 
              onClick={() => setIsModalOpen(false)}
              className="bg-gray-200 hover:bg-gray-300 text-gray-800 px-4 py-2 rounded"
            >
              Cancel
            </button>
            <button 
              type="submit" 
              className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded"
            >
              Create User
            </button>
          </div>
        </form>
      </Modal>

      {/* Toast Notification */}
      {toast.message && (
        <Toast 
          message={toast.message} 
          tone={toast.tone} 
          onDismiss={() => setToast({ message: '', tone: 'error' })} 
        />
      )}
    </div>
  );
}