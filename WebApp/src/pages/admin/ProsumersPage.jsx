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
  const [formErrors, setFormErrors] = useState({});
  const [creating, setCreating] = useState(false);
  const [updating, setUpdating] = useState(false);

  const [deactivateTarget, setDeactivateTarget] = useState(null);
  const [deactivating, setDeactivating] = useState(false);

  // Search and Pagination State
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(6);

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

  // Reset to page 1 when search query or pageSize changes
  useEffect(() => {
    setCurrentPage(1);
  }, [searchQuery, pageSize]);

  const summary = useMemo(
    () => ({
      total: prosumers.length,
      active: prosumers.filter((p) => p.status === 'Active').length,
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
  const totalPages = Math.max(1, Math.ceil(filteredProsumers.length / pageSize));
  const currentItems = filteredProsumers.slice(
    (currentPage - 1) * pageSize,
    currentPage * pageSize
  );

  // Form Validation Logic
  const validateForm = () => {
    const errors = {};
    
    if (!formData.nic.trim()) {
      errors.nic = 'NIC is required';
    } else if (!/^\d{9}[vVxX]$|^\d{12}$/.test(formData.nic.trim())) {
      errors.nic = 'Invalid NIC format (e.g., 200012345678 or 981234567V)';
    }

    if (!formData.fullName.trim()) {
      errors.fullName = 'Full name is required';
    }

    if (!formData.email.trim()) {
      errors.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email.trim())) {
      errors.email = 'Invalid email format';
    }

    if (!formData.phone.trim()) {
      errors.phone = 'Phone number is required';
    } else if (!/^\d{10}$/.test(formData.phone.trim().replace(/\s+/g, ''))) {
      errors.phone = 'Phone number must be exactly 10 digits';
    }

    if (!formData.address.trim()) {
      errors.address = 'Address is required';
    }

    if (!editingProsumer && !formData.password) {
      errors.password = 'Password is required';
    } else if (!editingProsumer && formData.password.length < 6) {
      errors.password = 'Password must be at least 6 characters';
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleOpenCreate = () => {
    setEditingProsumer(null);
    setFormData(EMPTY_FORM);
    setFormErrors({});
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
    setFormErrors({});
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setEditingProsumer(null);
    setFormData(EMPTY_FORM);
    setFormErrors({});
  };

  const handleCreateProsumer = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;
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
    if (!validateForm()) return;
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
      { key: 'address', header: 'Address', className: 'hidden md:table-cell' }, 
      { key: 'email', header: 'Email', className: 'hidden lg:table-cell' }, 
      { key: 'phone', header: 'Phone', className: 'hidden lg:table-cell' }, 
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
        header: 'Action',
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
              <span className="text-xs text-muted italic">Use Pending view</span>
            ) : (
              <span className="text-xs text-muted italic">Pending</span>
            )}
          </div>
        ),
      },
    ],
    [handleOpenEdit],
  );

  if (role !== 'Backoffice') {
    return (
      <div className="mx-auto max-w-6xl px-4 py-6 sm:px-6 sm:py-8">
        <p className="rounded-md border border-error/30 bg-error-soft px-3 py-2 text-[13px] font-medium text-error">
          Access denied - only Backoffice users can manage prosumers.
        </p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-6 sm:px-6 sm:py-8">
      {/* Header Section */}
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-xl font-semibold tracking-tight text-ink">Prosumer management</h1>
          <p className="mt-1 text-[13px] text-muted">Manage solar prosumer accounts and profiles.</p>
        </div>
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
          <Button variant="secondary" onClick={fetchProsumers} disabled={isLoading} className="flex items-center justify-center gap-2">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/><path d="M21 3v5h-5"/><path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16"/><path d="M3 21v-5h5"/></svg>
            Refresh
          </Button>
          <Button variant="primary" onClick={handleOpenCreate}>
            New prosumer
          </Button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="mb-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatCard icon="user" label="Total prosumers" value={summary.total} hint="Registered accounts" />
        <StatCard icon="pulse" label="Active" value={summary.active} hint="Currently active" />
        <StatCard icon="clock" label="Pending" value={summary.pending} hint="Awaiting activation" />
        <StatCard icon="alert" label="Deactivated" value={summary.deactivated} hint="Deactivated accounts" />
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
            {searchQuery ? "No matching prosumers found." : "No prosumers found."}
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
            emptyMessage={searchQuery ? "No matching prosumers found." : "No prosumers found."}
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
                <Badge tone={p.status === 'Active' ? 'success' : p.status === 'PendingActivation' ? 'warning' : 'neutral'}>
                  {p.status}
                </Badge>
              </div>

              {/* Card Details */}
              <div className="space-y-2 mb-4 pb-4 border-b border-line">
                {p.email && (
                  <div className="flex gap-2 text-sm">
                    <span className="text-muted w-16 shrink-0">Email:</span>
                    <span className="text-ink truncate">{p.email}</span>
                  </div>
                )}
                {p.phone && (
                  <div className="flex gap-2 text-sm">
                    <span className="text-muted w-16 shrink-0">Phone:</span>
                    <span className="text-ink">{p.phone}</span>
                  </div>
                )}
                {p.address && (
                  <div className="flex gap-2 text-sm">
                    <span className="text-muted w-16 shrink-0">Address:</span>
                    <span className="text-ink line-clamp-2">{p.address}</span>
                  </div>
                )}
              </div>

              {/* Card Actions */}
              <div className="flex items-center justify-end gap-2">
                {p.status === 'Active' ? (
                  <>
                    <Button variant="secondary" size="sm" onClick={() => handleOpenEdit(p)} className="flex-1 sm:flex-none">
                      Edit
                    </Button>
                    <Button variant="danger-outline" size="sm" onClick={() => setDeactivateTarget(p)} className="flex-1 sm:flex-none">
                      Deactivate
                    </Button>
                  </>
                ) : p.status === 'Deactivated' ? (
                  <span className="text-xs text-muted italic w-full text-center">Use Pending view to reactivate</span>
                ) : (
                  <span className="text-xs text-muted italic w-full text-center">Pending activation</span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Pagination Controls */}
      {!isLoading && filteredProsumers.length > 0 && (
        <div className="mt-6 flex flex-col sm:flex-row items-center justify-between gap-4 border-t border-line pt-4">
          <div className="flex items-center gap-3 text-xs text-muted">
            <span>
              Showing <span className="font-semibold text-ink">{currentItems.length > 0 ? (currentPage - 1) * pageSize + 1 : 0}</span> to{' '}
              <span className="font-semibold text-ink">
                {Math.min(currentPage * pageSize, filteredProsumers.length)}
              </span>{' '}
              of <span className="font-semibold text-ink">{filteredProsumers.length}</span> prosumers
            </span>
            <span className="text-line">|</span>
            <div className="flex items-center gap-1.5">
              <span>Per page:</span>
              <select
                aria-label="Prosumers per page"
                value={pageSize}
                onChange={(e) => setPageSize(Number(e.target.value))}
                className="rounded-md border border-line bg-surface px-2 py-1 text-xs font-medium text-ink focus:outline-none focus:ring-1 focus:ring-primary"
              >
                <option value={6}>6</option>
                <option value={12}>12</option>
                <option value={24}>24</option>
              </select>
            </div>
          </div>

          <div className="flex items-center gap-1.5">
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
              disabled={currentPage === 1}
              className="px-2.5"
            >
              ← Prev
            </Button>
            <div className="flex items-center gap-1">
              {Array.from({ length: totalPages }, (_, i) => i + 1).map((pageNum) => (
                <button
                  key={pageNum}
                  type="button"
                  onClick={() => setCurrentPage(pageNum)}
                  className={`min-w-[32px] h-8 rounded-md text-xs font-semibold transition-all ${
                    currentPage === pageNum
                      ? 'bg-primary text-white shadow-xs'
                      : 'border border-line bg-surface text-ink hover:bg-surface-alt'
                  }`}
                >
                  {pageNum}
                </button>
              ))}
            </div>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
              disabled={currentPage === totalPages}
              className="px-2.5"
            >
              Next →
            </Button>
          </div>
        </div>
      )}

      {/* Modals and Dialogs */}
      <Modal
        open={isModalOpen}
        onClose={handleCloseModal}
        title={editingProsumer ? 'Edit prosumer' : 'Create new prosumer'}
        description={editingProsumer ? 'Update prosumer profile details.' : 'Manually provision a prosumer account.'}
      >
        <form onSubmit={editingProsumer ? handleUpdateProsumer : handleCreateProsumer} className="flex flex-col gap-4">
          {!editingProsumer && (
            <>
              <div>
                <Input
                  label="NIC"
                  required
                  value={formData.nic}
                  onChange={(e) => {
                    setFormData({ ...formData, nic: e.target.value });
                    if (formErrors.nic) setFormErrors({ ...formErrors, nic: '' });
                  }}
                  placeholder="e.g., 200012345678 or 981234567V"
                />
                {formErrors.nic && <p className="mt-1 text-xs text-error">{formErrors.nic}</p>}
              </div>
              <div>
                <Input
                  label="Password"
                  type="password"
                  required
                  value={formData.password}
                  onChange={(e) => {
                    setFormData({ ...formData, password: e.target.value });
                    if (formErrors.password) setFormErrors({ ...formErrors, password: '' });
                  }}
                />
                {formErrors.password && <p className="mt-1 text-xs text-error">{formErrors.password}</p>}
              </div>
            </>
          )}
          <div>
            <Input
              label="Full name"
              required
              value={formData.fullName}
              onChange={(e) => {
                setFormData({ ...formData, fullName: e.target.value });
                if (formErrors.fullName) setFormErrors({ ...formErrors, fullName: '' });
              }}
            />
            {formErrors.fullName && <p className="mt-1 text-xs text-error">{formErrors.fullName}</p>}
          </div>
          <div>
            <Input
              label="Email"
              type="email"
              required
              value={formData.email}
              onChange={(e) => {
                setFormData({ ...formData, email: e.target.value });
                if (formErrors.email) setFormErrors({ ...formErrors, email: '' });
              }}
            />
            {formErrors.email && <p className="mt-1 text-xs text-error">{formErrors.email}</p>}
          </div>
          <div>
            <Input
              label="Phone"
              required
              value={formData.phone}
              onChange={(e) => {
                setFormData({ ...formData, phone: e.target.value });
                if (formErrors.phone) setFormErrors({ ...formErrors, phone: '' });
              }}
            />
            {formErrors.phone && <p className="mt-1 text-xs text-error">{formErrors.phone}</p>}
          </div>
          <div>
            <Input
              label="Address"
              required
              value={formData.address}
              onChange={(e) => {
                setFormData({ ...formData, address: e.target.value });
                if (formErrors.address) setFormErrors({ ...formErrors, address: '' });
              }}
            />
            {formErrors.address && <p className="mt-1 text-xs text-error">{formErrors.address}</p>}
          </div>
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