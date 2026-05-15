import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { Lock, Eye, EyeOff, AlertTriangle } from 'lucide-react';
import { resetPassword } from '../../api/authApi';

const PASSWORD_RULES = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,}$/;

function checkPasswordStrength(pwd) {
  if (!pwd) return { score: 0, label: '', color: '' };
  const checks = [
    pwd.length >= 8,
    /[A-Z]/.test(pwd),
    /[a-z]/.test(pwd),
    /\d/.test(pwd),
    /[^A-Za-z\d]/.test(pwd),
  ];
  const score = checks.filter(Boolean).length;
  if (score <= 2) return { score, label: 'Faible',   color: 'bg-red-500' };
  if (score === 3) return { score, label: 'Moyen',   color: 'bg-yellow-500' };
  if (score === 4) return { score, label: 'Fort',    color: 'bg-blue-500' };
  return              { score, label: 'Très fort', color: 'bg-green-500' };
}

const ResetPasswordPage = () => {
  const [searchParams]  = useSearchParams();
  const navigate        = useNavigate();
  const token           = searchParams.get('token');

  const [form, setForm]                       = useState({ password: '', confirm: '' });
  const [showPassword, setShowPassword]       = useState(false);
  const [showConfirm, setShowConfirm]         = useState(false);
  const [error, setError]                     = useState('');
  const [loading, setLoading]                 = useState(false);

  const strength = checkPasswordStrength(form.password);

  useEffect(() => {
    if (!token) {
      setError("Lien invalide. Veuillez refaire une demande de réinitialisation.");
    }
  }, [token]);

  const handleChange = (e) => {
    setForm(prev => ({ ...prev, [e.target.name]: e.target.value }));
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!PASSWORD_RULES.test(form.password)) {
      setError("Le mot de passe doit contenir au moins 8 caractères, une majuscule, une minuscule, un chiffre et un caractère spécial.");
      return;
    }

    if (form.password !== form.confirm) {
      setError("Les mots de passe ne correspondent pas.");
      return;
    }

    setLoading(true);
    const result = await resetPassword(token, form.password);
    setLoading(false);

    if (result.success) {
      navigate('/login', {
        state: { success: "Mot de passe réinitialisé avec succès. Vous pouvez vous connecter." },
      });
    } else {
      setError(result.error);
    }
  };

  if (!token) {
    return (
      <div
        className="min-h-screen flex flex-col items-center justify-center px-4"
        style={{ background: 'linear-gradient(135deg, #1e3a8a 0%, #2563eb 100%)' }}
      >
        <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-8 text-center">
          <AlertTriangle size={48} className="text-red-400 mx-auto mb-4" />
          <h2 className="text-xl font-bold text-gray-800 mb-2">Lien invalide</h2>
          <p className="text-gray-500 text-sm mb-6">
            Ce lien de réinitialisation est invalide ou a expiré.
          </p>
          <Link to="/forgot-password" className="btn-primary inline-block px-6 py-2">
            Nouvelle demande
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div
      className="min-h-screen flex flex-col items-center justify-center px-4"
      style={{ background: 'linear-gradient(135deg, #1e3a8a 0%, #2563eb 100%)' }}
    >
      <div className="text-center mb-8">
        <img src="/logo.jpg" alt="MicroFinanceHub" className="w-56 mx-auto rounded-2xl shadow-lg" />
      </div>

      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-8">
        <h2 className="text-xl font-bold text-gray-800 mb-1">Nouveau mot de passe</h2>
        <p className="text-gray-500 text-sm mb-6">
          Choisissez un mot de passe sécurisé pour votre compte.
        </p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="form-label">Nouveau mot de passe</label>
            <div className="relative">
              <Lock size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
              <input
                type={showPassword ? 'text' : 'password'}
                name="password"
                value={form.password}
                onChange={handleChange}
                placeholder="••••••••"
                required
                minLength={8}
                autoFocus
                className="form-input pl-9 pr-10"
              />
              <button
                type="button"
                onClick={() => setShowPassword(v => !v)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 focus:outline-none"
                tabIndex={-1}
              >
                {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>

            {form.password && (
              <div className="mt-2">
                <div className="flex gap-1 mb-1">
                  {[1, 2, 3, 4, 5].map(i => (
                    <div
                      key={i}
                      className={`h-1 flex-1 rounded-full transition-all ${
                        i <= strength.score ? strength.color : 'bg-gray-200'
                      }`}
                    />
                  ))}
                </div>
                {strength.label && (
                  <p className="text-xs text-gray-500">
                    Force : <span className="font-medium">{strength.label}</span>
                  </p>
                )}
              </div>
            )}
          </div>

          <div>
            <label className="form-label">Confirmer le mot de passe</label>
            <div className="relative">
              <Lock size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
              <input
                type={showConfirm ? 'text' : 'password'}
                name="confirm"
                value={form.confirm}
                onChange={handleChange}
                placeholder="••••••••"
                required
                className="form-input pl-9 pr-10"
              />
              <button
                type="button"
                onClick={() => setShowConfirm(v => !v)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 focus:outline-none"
                tabIndex={-1}
              >
                {showConfirm ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
            {form.confirm && form.password !== form.confirm && (
              <p className="text-red-500 text-xs mt-1">Les mots de passe ne correspondent pas.</p>
            )}
          </div>

          {error && (
            <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{error}</p>
          )}

          <button
            type="submit"
            disabled={loading}
            className="btn-primary w-full py-3 text-base font-semibold disabled:opacity-60"
          >
            {loading ? 'Enregistrement...' : 'Réinitialiser le mot de passe'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default ResetPasswordPage;
