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
import Input from '../../components/common/Input';

const ITEMS_PER_PAGE = 6;

export default function PendingProsumersPage() {
  const { role } = useAuth();
  const [prosumers, setProsumers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [toast, setToast] = useState({ message: '', tone: 'error' });

  const [activateTarget, setActivateTarget] = useState(null);
  const [activating, setActivating] = useState(false);

  const [reactivateTarget, setReactivateTarget] = useState(null);
  const [reactivating, setReactivating] = useState(false);

  // Search and Pagination State
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);

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

  // Reset to page 1 when search query changes
  useEffect(() => {
    setCurrentPage(1);
  }, [searchQuery]);

  const summary = useMemo(
    () => ({
      pending: prosumers.filter((p) => p.status === 'PendingActivation').length,
      deactivated: prosumers.filter((p) => p.status === 'Deactivated').length,
    }),
    [prosumers],
  );

  // Filter prosumers based on search query
  const filteredProsumers = useMemo(() => {
    if (!searchQuery) return prosumers;
    const lowerQuery = searchQuery.toLowerCase();
    return prosumers.filter(
      (p) =>
        p.nic.toLowerCase().includes(lowerQuery) ||
        p.fullName.toLowerCase().includes(lowerQuery) ||
        p.email.toLowerCase().includes(lowerQuery)
    );
  }, [prosumers, searchQuery]);

  // Pagination logic
  const totalPages = Math.ceil(filteredProsumers.length / ITEMS_PER_PAGE);
  const currentItems = filteredProsumers.slice(
    (currentPage - 1) * ITEMS_PER_PAGE,
    currentPage * ITEMS_PER_PAGE
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
      { key: 'address', header: 'Address', className: 'hidden md:table-cell' }, 
      { key: 'email', header: 'Email', className: 'hidden lg:table-cell' }, 
      { 
        key: 'createdAt', 
        header: 'Requested On', 
        render: (p) => new Date(p.createdAt).toLocaleDateString(),
        className: 'hidden sm:table-cell' 
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
        header: 'Action',
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
      <div className="mx-auto max-w-6xl px-4 py-6 sm:px-6 sm:py-8">
        <p className="rounded-md border border-error/30 bg-error-soft px-3 py-2 text-[13px] font-medium text-error">
          Access denied - only Backoffice users can manage prosumer activations.
        </p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-6 sm:px-6 sm:py-8">
      {/* Header Section */}
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-xl font-semibold tracking-tight text-ink">Pending & Deactivated Prosumers</h1>
          <p className="mt-1 text-[13px] text-muted">Review and activate new registrations, or reactivate deactivated accounts.</p>
        </div>
        <Button variant="secondary" onClick={fetchProsumers} disabled={isLoading} className="flex items-center justify-center gap-2">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/><path d="M21 3v5h-5"/><path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16"/><path d="M3 21v-5h5"/></svg>
          {isLoading ? 'Refreshing…' : 'Refresh'}
        </Button>
      </div>

      {/* Stats Cards */}
      <div className="mb-6 grid grid-cols-2 gap-3 sm:grid-cols-2">
        <StatCard icon="clock" label="Pending Activation" value={summary.pending} hint="Awaiting Backoffice approval" />
        <StatCard icon="x-circle" label="Deactivated" value={summary.deactivated} hint="Require Backoffice reactivation" />
      </div>

      {/* Search Bar */}
      <div className="mb-4">
        <Input
          placeholder="Search by NIC, Name, or Email..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="w-full sm:max-w-sm"
        />
      </div>

      {/* Loading State */}
      {isLoading && (
        <div className="py-12 text-center text-sm text-muted">Loading prosumers…</div>
      )}

      {/* Empty State */}
      {!isLoading && currentItems.length === 0 && (
        <div className="rounded-lg border border-line bg-surface-alt py-12 text-center">
          <p className="text-sm text-muted">
            {searchQuery ? "No matching prosumers found." : "No pending or deactivated prosumers found."}
          </p>
        </div>
      )}

      {/* Desktop Table View (Hidden on Mobile) */}
      {!isLoading && currentItems.length > 0 && (
        <div className="hidden md:block overflow-x-auto rounded-lg border border-line">
          <DataTable
            columns={columns}
            data={currentItems}
            loading={isLoading}
            emptyMessage={searchQuery ? "No matching prosumers found." : "No pending or deactivated prosumers found."}
            rowKey={(p) => p.nic}
          />
        </div>
      )}

      {/* Mobile Cards View (Hidden on Desktop) */}
      {!isLoading && currentItems.length > 0 && (
        <div className="md:hidden space-y-3">
          {currentItems.map((p) => (
            <div
              key={p.nic}
              className="rounded-lg border border-line bg-surface p-4 shadow-sm"
            >
              {/* Card Header */}
              <div className="flex items-start justify-between gap-3 mb-3">
                <div className="min-w-0 flex-1">
                  <p className="font-semibold text-ink truncate">{p.fullName}</p>
                  <p className="font-mono text-xs text-muted mt-0.5">{p.nic}</p>
                </div>
                <Badge tone={p.status === 'PendingActivation' ? 'warning' : 'neutral'}>
                  {p.status === 'PendingActivation' ? 'Pending' : 'Deactivated'}
                </Badge>
              </div>

              {/* Card Details */}
              <div className="space-y-2 mb-4 pb-4 border-b border-line">
                {p.email && (
                  <div className="flex gap-2 text-sm">
                    <span className="text-muted w-20 shrink-0">Email:</span>
                    <span className="text-ink truncate">{p.email}</span>
                  </div>
                )}
                {p.address && (
                  <div className="flex gap-2 text-sm">
                    <span className="text-muted w-20 shrink-0">Address:</span>
                    <span className="text-ink line-clamp-2">{p.address}</span>
                  </div>
                )}
                {p.createdAt && (
                  <div className="flex gap-2 text-sm">
                    <span className="text-muted w-20 shrink-0">Requested:</span>
                    <span className="text-ink">{new Date(p.createdAt).toLocaleDateString()}</span>
                  </div>
                )}
              </div>

              {/* Card Actions */}
              <div className="flex items-center justify-end gap-2">
                {p.status === 'PendingActivation' ? (
                  <Button variant="primary" size="sm" onClick={() => setActivateTarget(p)} className="w-full sm:w-auto">
                    Activate Account
                  </Button>
                ) : (
                  <Button variant="secondary" size="sm" onClick={() => setReactivateTarget(p)} className="w-full sm:w-auto">
                    Reactivate Account
                  </Button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Pagination Controls */}
      {!isLoading && filteredProsumers.length > 0 && (
        <div className="mt-6 flex flex-col items-center justify-between gap-4 border-t border-line pt-4 sm:flex-row">
          <p className="text-sm text-muted">
            Showing {currentItems.length > 0 ? (currentPage - 1) * ITEMS_PER_PAGE + 1 : 0} to{' '}
            {Math.min(currentPage * ITEMS_PER_PAGE, filteredProsumers.length)} of {filteredProsumers.length} results
          </p>
          <div className="flex items-center gap-2">
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
              disabled={currentPage === 1}
            >
              Previous
            </Button>
            <span className="text-sm font-medium text-ink">
              Page {currentPage} of {totalPages || 1}
            </span>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
              disabled={currentPage === totalPages || totalPages === 0}
            >
              Next
            </Button>
          </div>
        </div>
      )}

      {/* Modals and Dialogs */}
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