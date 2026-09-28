// ============================================================
// File: ProsumersPage.jsx
// Purpose: Backoffice management of Prosumer profiles.
//          Allows viewing, creating, updating, and deactivating
//          prosumers. Reactivation is handled in PendingProsumersPage.
// Author: Rukshan
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

const EMPTY_FORM = { nic: '', fullName: '', email: '', phone: '', address: '', password: '' };

export default function ProsumersPage() {
  const { role } = useAuth();
  const [prosumers, setProsumers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [toast, setToast] = useState({ message: '', tone: 'error' });

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingProsumer, setEditingProsumer] = useState(null);
  const [formData, setFormData] = useState(EMPTY_FORM);
  const [creating, setCreating] = useState(false);
  const [updating, setUpdating] = useState(false);

  const [deactivateTarget, setDeactivateTarget] = useState(null);
  const [deactivating, setDeactivating] = useState(false);

  const fetchProsumers = useCallback(async () => {
    setIsLoading(true);
    try {
      const response = await api.get('/admin/prosumers');
      setProsumers(response.data);
    } catch (err) {
      setToast({ message: err.response?.data?.message || 'Failed to load prosumers', tone: 'error' });
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (role === 'Backoffice') {
      fetchProsumers();
    }
  }, [role, fetchProsumers]);

  const summary = useMemo(
    () => ({
      total: prosumers.length,
      active: prosumers.filter((p) => p.status === 'Active').length,
      pending: prosumers.filter((p) => p.status === 'PendingActivation').length,
      deactivated: prosumers.filter((p) => p.status === 'Deactivated').length,
    }),
    [prosumers],
  );

  const handleOpenCreate = () => {
    setEditingProsumer(null);
    setFormData(EMPTY_FORM);
    setIsModalOpen(true);
  };

  const handleOpenEdit = (prosumer) => {
    setEditingProsumer(prosumer);
    setFormData({
      nic: prosumer.nic,
      fullName: prosumer.fullName,
      email: prosumer.email,
      phone: prosumer.phone || '',
      address: prosumer.address || '',
      password: '',
    });
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setEditingProsumer(null);
    setFormData(EMPTY_FORM);
  };

  const handleCreateProsumer = async (e) => {
    e.preventDefault();
    setCreating(true);
    try {
      await api.post('/admin/prosumers', formData);
      setToast({ message: 'Prosumer created successfully!', tone: 'success' });
      handleCloseModal();
      await fetchProsumers();
    } catch (err) {
      setToast({ message: err.response?.data?.message || 'Failed to create prosumer', tone: 'error' });
    } finally {
      setCreating(false);
    }
  };

  const handleUpdateProsumer = async (e) => {
    e.preventDefault();
    if (!editingProsumer) return;
    setUpdating(true);
    try {
      const updatePayload = {
        fullName: formData.fullName,
        email: formData.email,
        phone: formData.phone,
        address: formData.address,
      };
      await api.put(`/admin/prosumers/${editingProsumer.nic}`, updatePayload);
      setToast({ message: 'Prosumer updated successfully!', tone: 'success' });
      handleCloseModal();
      await fetchProsumers();
    } catch (err) {
      setToast({ message: err.response?.data?.message || 'Failed to update prosumer', tone: 'error' });
    } finally {
      setUpdating(false);
    }
  };

  const handleConfirmDeactivate = async () => {
    if (!deactivateTarget) return;
    setDeactivating(true);
    try {
      await api.put(`/admin/prosumers/${deactivateTarget.nic}/deactivate`);
      setToast({ message: 'Prosumer deactivated successfully!', tone: 'success' });
      setDeactivateTarget(null);
      await fetchProsumers();
    } catch (err) {
      setToast({ message: err.response?.data?.message || 'Failed to deactivate prosumer', tone: 'error' });
      setDeactivateTarget(null);
    } finally {
      setDeactivating(false);
    }
  };

  const columns = useMemo(
    () => [
      { key: 'nic', header: 'NIC', render: (p) => <span className="font-mono text-xs text-ink">{p.nic}</span> },
      { key: 'fullName', header: 'Full name' },
      { key: 'email', header: 'Email' },
      { key: 'phone', header: 'Phone' },
      {
        key: 'status',
        header: 'Status',
        render: (p) => (
          <Badge tone={p.status === 'Active' ? 'success' : p.status === 'PendingActivation' ? 'warning' : 'neutral'}>
            {p.status}
          </Badge>
        ),
      },
      {
        key: 'actions',
        header: '',
        className: 'text-right',
        render: (p) => (
          <div className="flex justify-end gap-2">
            {p.status === 'Active' ? (
              <>
                <Button variant="secondary" size="sm" onClick={() => handleOpenEdit(p)}>
                  Edit
                </Button>
                <Button variant="danger-outline" size="sm" onClick={() => setDeactivateTarget(p)}>
                  Deactivate
                </Button>
              </>
            ) : p.status === 'Deactivated' ? (
              <span className="text-xs text-muted italic">Use Pending view to reactivate</span>
            ) : (
              <span className="text-xs text-muted italic">Pending activation</span>
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
          Access denied — only Backoffice users can manage prosumers.
        </p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl px-6 py-8">
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-xl font-semibold tracking-tight text-ink">Prosumer management</h1>
          <p className="mt-1 text-[13px] text-muted">Manage solar prosumer accounts and profiles.</p>
        </div>
        <Button variant="primary" onClick={handleOpenCreate}>
          New prosumer
        </Button>
      </div>

      <div className="mb-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatCard icon="user" label="Total prosumers" value={summary.total} hint="Registered accounts" />
        <StatCard icon="pulse" label="Active" value={summary.active} hint="Currently active" />
        <StatCard icon="clock" label="Pending" value={summary.pending} hint="Awaiting activation" />
        <StatCard icon="x-circle" label="Deactivated" value={summary.deactivated} hint="Deactivated accounts" />
      </div>

      <DataTable
        columns={columns}
        data={prosumers}
        loading={isLoading}
        emptyMessage="No prosumers found."
        rowKey={(p) => p.nic}
      />

      <Modal
        open={isModalOpen}
        onClose={handleCloseModal}
        title={editingProsumer ? 'Edit prosumer' : 'Create new prosumer'}
        description={editingProsumer ? 'Update prosumer profile details.' : 'Manually provision a prosumer account.'}
      >
        <form onSubmit={editingProsumer ? handleUpdateProsumer : handleCreateProsumer} className="flex flex-col gap-4">
          {!editingProsumer && (
            <>
              <Input
                label="NIC"
                required
                value={formData.nic}
                onChange={(e) => setFormData({ ...formData, nic: e.target.value })}
                placeholder="e.g., 200012345678 or 981234567V"
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
            label="Phone"
            value={formData.phone}
            onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
          />
          <Input
            label="Address"
            value={formData.address}
            onChange={(e) => setFormData({ ...formData, address: e.target.value })}
          />
          <div className="mt-2 flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={handleCloseModal} disabled={creating || updating}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" disabled={creating || updating}>
              {creating ? 'Creating…' : updating ? 'Updating…' : editingProsumer ? 'Update prosumer' : 'Create prosumer'}
            </Button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={Boolean(deactivateTarget)}
        title="Deactivate prosumer?"
        description={
          deactivateTarget ? `"${deactivateTarget.fullName}" will no longer be able to use the service. Only a Backoffice officer can reactivate this account.` : ''
        }
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