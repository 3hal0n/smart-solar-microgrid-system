// ============================================================
// File: UsersPage.jsx
// Purpose: Handles CRUD operations for system users (Backoffice/
//          GridOperator). Fetches users, displays them in a table,
//          provides modals for creation/updating, and includes
//          search and filter capabilities. Excludes Prosumers.
// Author: Migara (updated with Search, Filters, and Prosumer exclusion)
// ============================================================

import { useMemo, useState, useEffect, useCallback } from 'react';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import Toast from '../../components/common/Toast';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Badge from '../../components/common/Badge';
import Modal from '../../components/common/Modal';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import DataTable from '../../components/common/Table';
import StatCard from '../../components/common/StatCard';

const EMPTY_FORM = { username: '', password: '', role: 'GridOperator', fullName: '', email: '' };

export default function UsersPage() {
  const { role } = useAuth();
  const [users, setUsers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [toast, setToast] = useState({ message: '', tone: 'error' });

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [formData, setFormData] = useState(EMPTY_FORM);
  const [creating, setCreating] = useState(false);
  const [updating, setUpdating] = useState(false);

  const [deactivateTarget, setDeactivateTarget] = useState(null);
  const [deactivating, setDeactivating] = useState(false);

  // --- NEW: State for Search and Filters ---
  const [searchQuery, setSearchQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState('All');
  const [statusFilter, setStatusFilter] = useState('All');

  const fetchUsers = useCallback(async () => {
    setIsLoading(true);
    try {
      const response = await api.get('/users');
      setUsers(response.data);
    } catch (err) {
      setToast({ message: err.response?.data?.message || 'Failed to load users', tone: 'error' });
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (role === 'Backoffice') {
      fetchUsers();
    }
  }, [role, fetchUsers]);

  // --- NEW: Filter logic (Excludes Prosumers, applies search/filters) ---
  const filteredUsers = useMemo(() => {
    return users.filter((u) => {
      // 1. Exclude Prosumers entirely
      if (u.role === 'Prosumer') return false;

      // 2. Search by username (case-insensitive)
      if (searchQuery && !u.username.toLowerCase().includes(searchQuery.toLowerCase())) {
        return false;
      }

      // 3. Filter by role
      if (roleFilter !== 'All' && u.role !== roleFilter) {
        return false;
      }

      // 4. Filter by status
      if (statusFilter !== 'All' && u.status !== statusFilter) {
        return false;
      }

      return true;
    });
  }, [users, searchQuery, roleFilter, statusFilter]);

  // Summary now reflects the currently filtered view
  const summary = useMemo(
    () => ({
      total: filteredUsers.length,
      active: filteredUsers.filter((u) => u.status === 'Active').length,
      backoffice: filteredUsers.filter((u) => u.role === 'Backoffice').length,
      operators: filteredUsers.filter((u) => u.role === 'GridOperator').length,
    }),
    [filteredUsers],
  );

  const handleOpenCreate = () => {
    setEditingUser(null);
    setFormData(EMPTY_FORM);
    setIsModalOpen(true);
  };

  const handleOpenEdit = (user) => {
    setEditingUser(user);
    setFormData({
      username: user.username,
      password: '', 
      role: user.role,
      fullName: user.fullName,
      email: user.email,
    });
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setEditingUser(null);
    setFormData(EMPTY_FORM);
  };

  const handleCreateUser = async (e) => {
    e.preventDefault();
    setCreating(true);
    try {
      await api.post('/users', formData);
      setToast({ message: 'User created successfully!', tone: 'success' });
      handleCloseModal();
      await fetchUsers();
    } catch (err) {
      setToast({ message: err.response?.data?.message || 'Failed to create user', tone: 'error' });
    } finally {
      setCreating(false);
    }
  };

  const handleUpdateUser = async (e) => {
    e.preventDefault();
    if (!editingUser) return;
    setUpdating(true);
    try {
      const updatePayload = {
        fullName: formData.fullName,
        email: formData.email,
        role: formData.role,
      };
      await api.put(`/users/${editingUser.id}`, updatePayload);
      setToast({ message: 'User updated successfully!', tone: 'success' });
      handleCloseModal();
      await fetchUsers();
    } catch (err) {
      setToast({ message: err.response?.data?.message || 'Failed to update user', tone: 'error' });
    } finally {
      setUpdating(false);
    }
  };

  const handleConfirmDeactivate = async () => {
    if (!deactivateTarget) return;
    setDeactivating(true);
    try {
      await api.put(`/users/${deactivateTarget.id}/deactivate`);
      setToast({ message: 'User deactivated successfully!', tone: 'success' });
      setDeactivateTarget(null);
      await fetchUsers();
    } catch (err) {
      setToast({ message: err.response?.data?.message || 'Failed to deactivate user', tone: 'error' });
      setDeactivateTarget(null);
    } finally {
      setDeactivating(false);
    }
  };

  const columns = useMemo(
    () => [
      { key: 'username', header: 'Username', render: (u) => <span className="font-medium text-ink">{u.username}</span> },
      { key: 'fullName', header: 'Full name' },
      { key: 'email', header: 'Email' },
      {
        key: 'role',
        header: 'Role',
        render: (u) => <Badge tone={u.role === 'Backoffice' ? 'info' : 'neutral'}>{u.role}</Badge>,
      },
      {
        key: 'status',
        header: 'Status',
        render: (u) => <Badge tone={u.status === 'Active' ? 'success' : 'neutral'}>{u.status}</Badge>,
      },
      {
        key: 'actions',
        header: '',
        className: 'text-right',
        render: (u) => (
          <div className="flex justify-end gap-2">
            {u.status === 'Active' ? (
              <>
                <Button variant="secondary" size="sm" onClick={() => handleOpenEdit(u)}>
                  Edit
                </Button>
                <Button variant="danger-outline" size="sm" onClick={() => setDeactivateTarget(u)}>
                  Deactivate
                </Button>
              </>
            ) : (
              <span className="text-xs text-muted italic">No actions available</span>
            )}
          </div>
        ),
      },
    ],
    [handleOpenEdit],
  );

  if (role !== 'Backoffice') {
    return (
      <div className="mx-auto max-w-6xl px-6 py-8">
        <p className="rounded-md border border-error/30 bg-error-soft px-3 py-2 text-[13px] font-medium text-error">
          Access denied — only Backoffice users can manage system users.
        </p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl px-6 py-8">
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-xl font-semibold tracking-tight text-ink">User management</h1>
          <p className="mt-1 text-[13px] text-muted">Backoffice and Grid Operator staff accounts.</p>
        </div>
        <Button variant="primary" onClick={handleOpenCreate}>
          New user
        </Button>
      </div>

      <div className="mb-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatCard icon="user" label="Total staff" value={summary.total} hint="Filtered staff accounts" />
        <StatCard
          icon="pulse"
          label="Active"
          value={summary.active}
          hint={`${summary.total - summary.active} deactivated`}
        />
        <StatCard icon="hubs" label="Backoffice" value={summary.backoffice} hint="Admin accounts" />
        <StatCard icon="bolt" label="Grid operators" value={summary.operators} hint="Field accounts" />
      </div>

      {/* --- NEW: Search and Filter Controls --- */}
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-end">
        <div className="flex-1">
          <Input
            label="Search by username"
            placeholder="e.g., migara_admin"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
        <div className="w-full sm:w-48">
          <Input
            label="Filter by Role"
            as="select"
            value={roleFilter}
            onChange={(e) => setRoleFilter(e.target.value)}
          >
            <option value="All">All Roles</option>
            <option value="Backoffice">Backoffice</option>
            <option value="GridOperator">Grid Operator</option>
          </Input>
        </div>
        <div className="w-full sm:w-48">
          <Input
            label="Filter by Status"
            as="select"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="All">All Statuses</option>
            <option value="Active">Active</option>
            <option value="Deactivated">Deactivated</option>
          </Input>
        </div>
      </div>

      <DataTable
        columns={columns}
        data={filteredUsers}
        loading={isLoading}
        emptyMessage="No matching staff users found."
        rowKey={(u) => u.id}
      />

      <Modal
        open={isModalOpen}
        onClose={handleCloseModal}
        title={editingUser ? "Edit user" : "Create new user"}
        description={editingUser ? "Update user details." : "Provisions a Backoffice or Grid Operator staff account."}
      >
        <form onSubmit={editingUser ? handleUpdateUser : handleCreateUser} className="flex flex-col gap-4">
          {!editingUser && (
            <>
              <Input
                label="Username"
                required
                value={formData.username}
                onChange={(e) => setFormData({ ...formData, username: e.target.value })}
              />
              <Input
                label="Password"
                type="password"
                required
                value={formData.password}
                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
              />
            </>
          )}
          <Input
            label="Full name"
            required
            value={formData.fullName}
            onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
          />
          <Input
            label="Email"
            type="email"
            required
            value={formData.email}
            onChange={(e) => setFormData({ ...formData, email: e.target.value })}
          />
          <Input
            label="Role"
            as="select"
            value={formData.role}
            onChange={(e) => setFormData({ ...formData, role: e.target.value })}
          >
            <option value="Backoffice">Backoffice</option>
            <option value="GridOperator">Grid Operator</option>
          </Input>
          <div className="mt-2 flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={handleCloseModal} disabled={creating || updating}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" disabled={creating || updating}>
              {creating ? 'Creating…' : updating ? 'Updating…' : (editingUser ? 'Update user' : 'Create user')}
            </Button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={Boolean(deactivateTarget)}
        title="Deactivate user?"
        description={deactivateTarget ? `"${deactivateTarget.username}" will no longer be able to sign in.` : ''}
        confirmLabel="Deactivate"
        tone="danger"
        confirming={deactivating}
        onConfirm={handleConfirmDeactivate}
        onClose={() => setDeactivateTarget(null)}
      />

      <Toast message={toast.message} tone={toast.tone} onDismiss={() => setToast({ message: '', tone: 'error' })} />
    </div>
  );
}