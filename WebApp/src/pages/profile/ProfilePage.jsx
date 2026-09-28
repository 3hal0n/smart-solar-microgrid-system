// ============================================================
// File: ProfilePage.jsx
// Purpose: Self-service profile and security management for the
//          currently authenticated staff user (Backoffice / GridOperator).
//          Allows viewing account details, updating name & email,
//          and changing password securely.
// ============================================================

import { useEffect, useState } from 'react';
import { useAuth } from '../../context/AuthContext.jsx';
import { useToast } from '../../components/common/Toast.jsx';
import api from '../../services/api.js';
import Button from '../../components/common/Button.jsx';
import Input from '../../components/common/Input.jsx';
import Badge from '../../components/common/Badge.jsx';
import Icon from '../../components/common/Icon.jsx';

// Computes 2-letter initials from full name
function getInitials(name) {
  if (!name) return '??';
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join('');
}

export default function ProfilePage() {
  const { fullName: authFullName, role: authRole, userId, updateUser } = useAuth();
  const { show: showToast } = useToast();

  const [profile, setProfile] = useState({
    id: userId || '',
    username: '',
    fullName: authFullName || '',
    email: '',
    role: authRole || '',
    status: 'Active',
    createdAt: '',
  });

  const [isLoadingProfile, setIsLoadingProfile] = useState(true);

  // Edit details form state
  const [fullNameInput, setFullNameInput] = useState('');
  const [emailInput, setEmailInput] = useState('');
  const [isSavingDetails, setIsSavingDetails] = useState(false);

  // Password change form state
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showCurrentPass, setShowCurrentPass] = useState(false);
  const [showNewPass, setShowNewPass] = useState(false);
  const [showConfirmPass, setShowConfirmPass] = useState(false);
  const [isChangingPass, setIsChangingPass] = useState(false);
  const [passwordError, setPasswordError] = useState('');

  // Fetch current user's profile
  useEffect(() => {
    let isMounted = true;

    async function loadProfile() {
      setIsLoadingProfile(true);
      try {
        // Try /users/me first, fallback to /users/{id}
        let res;
        try {
          res = await api.get('/users/me');
        } catch {
          if (userId) {
            res = await api.get(`/users/${userId}`);
          }
        }

        if (isMounted && res?.data) {
          const data = res.data;
          setProfile(data);
          setFullNameInput(data.fullName || '');
          setEmailInput(data.email || '');
          if (data.fullName && data.fullName !== authFullName) {
            updateUser({ fullName: data.fullName });
          }
        }
      } catch {
        // Fallback to local auth context
        if (isMounted) {
          setFullNameInput(authFullName || '');
        }
      } finally {
        if (isMounted) setIsLoadingProfile(false);
      }
    }

    loadProfile();
    return () => {
      isMounted = false;
    };
  }, [userId, authFullName, updateUser]);

  // Handle saving personal info (Full Name & Email)
  const handleSaveDetails = async (e) => {
    e.preventDefault();
    if (!fullNameInput.trim()) {
      showToast('Full name is required', 'error');
      return;
    }
    if (!emailInput.trim()) {
      showToast('Email address is required', 'error');
      return;
    }

    setIsSavingDetails(true);
    try {
      const targetId = profile.id || userId;
      if (!targetId) {
        throw new Error('User ID not available.');
      }

      await api.put(`/users/${targetId}`, {
        fullName: fullNameInput.trim(),
        email: emailInput.trim(),
        role: profile.role || authRole,
      });

      setProfile((prev) => ({
        ...prev,
        fullName: fullNameInput.trim(),
        email: emailInput.trim(),
      }));

      updateUser({ fullName: fullNameInput.trim() });
      showToast('Profile information updated successfully.', 'success');
    } catch (err) {
      showToast(err.response?.data?.message || 'Failed to update profile.', 'error');
    } finally {
      setIsSavingDetails(false);
    }
  };

  // Handle password update
  const handleChangePassword = async (e) => {
    e.preventDefault();
    setPasswordError('');

    if (!currentPassword) {
      setPasswordError('Current password is required.');
      return;
    }
    if (!newPassword || newPassword.length < 6) {
      setPasswordError('New password must be at least 6 characters.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setPasswordError('New password and confirmation do not match.');
      return;
    }
    if (newPassword === currentPassword) {
      setPasswordError('New password must be different from current password.');
      return;
    }

    setIsChangingPass(true);
    try {
      const targetId = profile.id || userId;
      if (!targetId) {
        throw new Error('User ID not available.');
      }

      await api.put(`/users/${targetId}/change-password`, {
        currentPassword,
        newPassword,
      });

      showToast('Password changed successfully.', 'success');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to change password.';
      setPasswordError(msg);
      showToast(msg, 'error');
    } finally {
      setIsChangingPass(false);
    }
  };

  return (
    <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
      {/* Page Header */}
      <div className="mb-8 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-ink">Account Profile</h1>
          <p className="mt-1 text-sm text-muted">
            Manage your personal staff information, security credentials, and preferences.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Badge tone={profile.role === 'Backoffice' ? 'purple' : 'teal'}>
            {profile.role || authRole || 'Staff'}
          </Badge>
          <Badge tone={profile.status === 'Active' ? 'green' : 'amber'}>
            {profile.status || 'Active'}
          </Badge>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-8 lg:grid-cols-12">
        {/* Left Column: Profile Card & Overview (4 cols) */}
        <div className="lg:col-span-4 space-y-6">
          <div className="overflow-hidden rounded-2xl border border-line bg-surface p-6 shadow-card">
            <div className="flex flex-col items-center text-center">
              {/* Avatar */}
              <div className="relative mb-4 flex h-24 w-24 items-center justify-center rounded-full bg-gradient-to-tr from-primary to-accent-indigo text-2xl font-bold text-white shadow-lg ring-4 ring-primary-soft">
                {getInitials(profile.fullName || authFullName)}
              </div>

              <h2 className="text-lg font-bold text-ink">
                {profile.fullName || authFullName || 'Staff User'}
              </h2>
              <p className="text-xs font-medium text-muted">@{profile.username || 'user'}</p>

              <div className="mt-3 inline-flex items-center gap-1.5 rounded-full bg-surface-alt px-3 py-1 text-xs font-semibold text-slate-700">
                <span className="h-2 w-2 rounded-full bg-success" />
                {profile.role || authRole || 'Operator'}
              </div>
            </div>

            <div className="mt-6 border-t border-line pt-4 space-y-3 text-xs">
              <div className="flex items-center justify-between text-muted">
                <span>Email</span>
                <span className="font-medium text-ink truncate max-w-[180px]">
                  {profile.email || '—'}
                </span>
              </div>
              <div className="flex items-center justify-between text-muted">
                <span>Account Status</span>
                <span className="font-medium text-success">Active</span>
              </div>
              {profile.createdAt && (
                <div className="flex items-center justify-between text-muted">
                  <span>Joined</span>
                  <span className="font-medium text-ink">
                    {new Date(profile.createdAt).toLocaleDateString(undefined, {
                      year: 'numeric',
                      month: 'short',
                      day: 'numeric',
                    })}
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* Security Summary Tip */}
          <div className="rounded-2xl border border-line bg-surface-alt/60 p-5 text-xs text-muted leading-relaxed">
            <div className="flex items-center gap-2 font-semibold text-ink mb-1.5">
              <Icon name="lock" className="h-4 w-4 text-primary" />
              <span>Password Security</span>
            </div>
            Use at least 6 characters with mixed letters and numbers to protect grid operations from unauthorized access.
          </div>
        </div>

        {/* Right Column: Edit Forms (8 cols) */}
        <div className="lg:col-span-8 space-y-8">
          {/* Card 1: Personal Information */}
          <div className="rounded-2xl border border-line bg-surface p-6 sm:p-7 shadow-card">
            <div className="mb-6">
              <h2 className="text-lg font-bold text-ink">Personal Information</h2>
              <p className="mt-0.5 text-xs text-muted">
                Update your name and primary communication email.
              </p>
            </div>

            <form onSubmit={handleSaveDetails} className="space-y-4">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label className="mb-1.5 block text-xs font-medium text-ink">
                    Username <span className="text-muted font-normal">(Read-only)</span>
                  </label>
                  <input
                    type="text"
                    value={profile.username || '—'}
                    disabled
                    className="w-full rounded-lg border border-line bg-surface-alt px-3.5 py-2 text-sm text-muted cursor-not-allowed"
                  />
                </div>
                <div>
                  <label className="mb-1.5 block text-xs font-medium text-ink">
                    Assigned Role <span className="text-muted font-normal">(Read-only)</span>
                  </label>
                  <input
                    type="text"
                    value={profile.role || authRole || '—'}
                    disabled
                    className="w-full rounded-lg border border-line bg-surface-alt px-3.5 py-2 text-sm text-muted cursor-not-allowed"
                  />
                </div>
              </div>

              <div>
                <Input
                  label="Full Name *"
                  type="text"
                  value={fullNameInput}
                  onChange={(e) => setFullNameInput(e.target.value)}
                  placeholder="e.g. Migara Silva"
                  required
                />
              </div>

              <div>
                <Input
                  label="Email Address *"
                  type="email"
                  value={emailInput}
                  onChange={(e) => setEmailInput(e.target.value)}
                  placeholder="e.g. operator@smartgrid.lk"
                  required
                />
              </div>

              <div className="flex justify-end pt-2">
                <Button
                  type="submit"
                  variant="primary"
                  disabled={isSavingDetails || isLoadingProfile}
                  className="px-6 py-2 text-sm font-medium shadow-sm"
                >
                  {isSavingDetails ? 'Saving…' : 'Save Changes'}
                </Button>
              </div>
            </form>
          </div>

          {/* Card 2: Security & Password */}
          <div className="rounded-2xl border border-line bg-surface p-6 sm:p-7 shadow-card">
            <div className="mb-6">
              <h2 className="text-lg font-bold text-ink">Change Password</h2>
              <p className="mt-0.5 text-xs text-muted">
                Ensure your account is using a strong password to maintain system security.
              </p>
            </div>

            {passwordError && (
              <div className="mb-5 rounded-lg border border-error/30 bg-error-soft px-4 py-3 text-xs font-medium text-error">
                {passwordError}
              </div>
            )}

            <form onSubmit={handleChangePassword} className="space-y-4">
              <div>
                <div className="relative">
                  <Input
                    label="Current Password *"
                    type={showCurrentPass ? 'text' : 'password'}
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    placeholder="Enter current password"
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowCurrentPass(!showCurrentPass)}
                    className="absolute right-3 top-8 text-xs font-medium text-muted hover:text-ink transition-colors"
                  >
                    {showCurrentPass ? 'Hide' : 'Show'}
                  </button>
                </div>
              </div>

              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div className="relative">
                  <Input
                    label="New Password *"
                    type={showNewPass ? 'text' : 'password'}
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="At least 6 characters"
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowNewPass(!showNewPass)}
                    className="absolute right-3 top-8 text-xs font-medium text-muted hover:text-ink transition-colors"
                  >
                    {showNewPass ? 'Hide' : 'Show'}
                  </button>
                </div>

                <div className="relative">
                  <Input
                    label="Confirm New Password *"
                    type={showConfirmPass ? 'text' : 'password'}
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="Repeat new password"
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPass(!showConfirmPass)}
                    className="absolute right-3 top-8 text-xs font-medium text-muted hover:text-ink transition-colors"
                  >
                    {showConfirmPass ? 'Hide' : 'Show'}
                  </button>
                </div>
              </div>

              <div className="flex justify-end pt-2">
                <Button
                  type="submit"
                  variant="primary"
                  disabled={isChangingPass || !currentPassword || !newPassword || !confirmPassword}
                  className="px-6 py-2 text-sm font-medium shadow-sm"
                >
                  {isChangingPass ? 'Updating Password…' : 'Update Password'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
