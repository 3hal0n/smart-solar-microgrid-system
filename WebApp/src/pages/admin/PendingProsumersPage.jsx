// ============================================================
// File: PendingProsumersPage.jsx
// Purpose: Backoffice view for pending activations and 
//          reactivations of deactivated prosumer accounts.
//          Enforces the rule that only Backoffice can reactivate.
// Author: Rukshan
// ============================================================

import { useMemo, useState, useEffect, useCallback } from 'react';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import Toast from '../../components/common/Toast';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import DataTable from '../../components/common/Table';
import StatCard from '../../components/common/StatCard';

export default function PendingProsumersPage() {
  const { role } = useAuth();
  const [prosumers, setProsumers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [toast, setToast] = useState({ message: '', tone: 'error' });

  const [activateTarget, setActivateTarget] = useState(null);
  const [activating, setActivating] = useState(false);

  const [reactivateTarget, setReactivateTarget] = useState(null);
  const [reactivating, setReactivating] = useState(false);

  const fetchProsumers = useCallback(async () => {
    setIsLoading(true);
    try {
      const response = await api.get('/admin/prosumers');
      const filtered = response.data.filter(
        (p) => p.status === 'PendingActivation' || p.status === 'Deactivated'
      );
      setProsumers(filtered);
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
      pending: prosumers.filter((p) => p.status === 'PendingActivation').length,
      deactivated: prosumers.filter((p) => p.status === 'Deactivated').length,
    }),
    [prosumers],
  );

  const handleConfirmActivate = async () => {
    if (!activateTarget) return;
    setActivating(true);
    try {
      await api.put(`/admin/prosumers/${activateTarget.nic}/activate`);
      setToast({ message: 'Prosumer activated successfully!', tone: 'success' });
      setActivateTarget(null);
      await fetchProsumers();
    } catch (err) {
      setToast({ message: err.response?.data?.message || 'Failed to activate prosumer', tone: 'error' });
      setActivateTarget(null);
    } finally {
      setActivating(false);
    }
  };

  const handleConfirmReactivate = async () => {
    if (!reactivateTarget) return;
    setReactivating(true);
    try {
      await api.put(`/admin/prosumers/${reactivateTarget.nic}/reactivate`);
      setToast({ message: 'Prosumer reactivated successfully!', tone: 'success' });
      setReactivateTarget(null);
      await fetchProsumers();
    } catch (err) {
      setToast({ message: err.response?.data?.message || 'Failed to reactivate prosumer', tone: 'error' });
      setReactivateTarget(null);
    } finally {
      setReactivating(false);
    }
  };

  const columns = useMemo(
    () => [
      { key: 'nic', header: 'NIC', render: (p) => <span className="font-mono text-xs text-ink">{p.nic}</span> },
      { key: 'fullName', header: 'Full name' },
      { key: 'email', header: 'Email' },
      { 
        key: 'createdAt', 
        header: 'Requested On', 
        render: (p) => new Date(p.createdAt).toLocaleDateString() 
      },
      {
        key: 'status',
        header: 'Status',
        render: (p) => (
          <Badge tone={p.status === 'PendingActivation' ? 'warning' : 'neutral'}>
            {p.status === 'PendingActivation' ? 'Pending Activation' : 'Deactivated'}
          </Badge>
        ),
      },
      {
        key: 'actions',
        header: '',
        className: 'text-right',
        render: (p) => (
          <div className="flex justify-end gap-2">
            {p.status === 'PendingActivation' ? (
              <Button variant="primary" size="sm" onClick={() => setActivateTarget(p)}>
                Activate
              </Button>
            ) : (
              <Button variant="secondary" size="sm" onClick={() => setReactivateTarget(p)}>
                Reactivate
              </Button>
            )}
          </div>
        ),
      },
    ],
    [],
  );

  if (role !== 'Backoffice') {
    return (
      <div className="mx-auto max-w-6xl px-6 py-8">
        <p className="rounded-md border border-error/30 bg-error-soft px-3 py-2 text-[13px] font-medium text-error">
          Access denied — only Backoffice users can manage prosumer activations.
        </p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl px-6 py-8">
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-xl font-semibold tracking-tight text-ink">Pending & Deactivated Prosumers</h1>
          <p className="mt-1 text-[13px] text-muted">Review and activate new registrations, or reactivate deactivated accounts.</p>
        </div>
        <Button variant="secondary" onClick={fetchProsumers} disabled={isLoading}>
          {isLoading ? 'Refreshing…' : 'Refresh'}
        </Button>
      </div>

      <div className="mb-6 grid grid-cols-2 gap-3 sm:grid-cols-2">
        <StatCard icon="clock" label="Pending Activation" value={summary.pending} hint="Awaiting Backoffice approval" />
        <StatCard icon="x-circle" label="Deactivated" value={summary.deactivated} hint="Require Backoffice reactivation" />
      </div>

      <DataTable
        columns={columns}
        data={prosumers}
        loading={isLoading}
        emptyMessage="No pending or deactivated prosumers found."
        rowKey={(p) => p.nic}
      />

      <ConfirmDialog
        open={Boolean(activateTarget)}
        title="Activate prosumer?"
        description={
          activateTarget ? `Approve "${activateTarget.fullName}" (${activateTarget.nic}) to access the system.` : ''
        }
        confirmLabel="Activate"
        tone="success"
        confirming={activating}
        onConfirm={handleConfirmActivate}
        onClose={() => setActivateTarget(null)}
      />

      <ConfirmDialog
        open={Boolean(reactivateTarget)}
        title="Reactivate prosumer?"
        description={
          reactivateTarget ? `Restore access for "${reactivateTarget.fullName}" (${reactivateTarget.nic}). Only Backoffice officers can perform this action.` : ''
        }
        confirmLabel="Reactivate"
        tone="primary"
        confirming={reactivating}
        onConfirm={handleConfirmReactivate}
        onClose={() => setReactivateTarget(null)}
      />

      <Toast message={toast.message} tone={toast.tone} onDismiss={() => setToast({ message: '', tone: 'error' })} />
    </div>
  );
}