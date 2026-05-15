import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CreditCard, Eye, EyeOff, ArrowLeft, Check } from 'lucide-react';
import { registerClient } from '../../api/authApi';

const PASSWORD_RULES = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,}$/;

function passwordStrength(pwd) {
  if (!pwd) return { score: 0, label: '', color: '' };
  const checks = [pwd.length >= 8, /[A-Z]/.test(pwd), /[a-z]/.test(pwd), /\d/.test(pwd), /[^A-Za-z\d]/.test(pwd)];
  const score = checks.filter(Boolean).length;
  if (score <= 2) return { score, label: 'Faible',   color: 'bg-red-500' };
  if (score === 3) return { score, label: 'Moyen',   color: 'bg-yellow-500' };
  if (score === 4) return { score, label: 'Fort',    color: 'bg-blue-500' };
  return              { score, label: 'Très fort', color: 'bg-green-500' };
}

const RegisterPage = () => {
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: '', motDePasse: '', confirmMotDePasse: '' });
  const [error, setError]     = useState('');
  const [loading, setLoading] = useState(false);
  const [showPwd, setShowPwd] = useState(false);
  const [showCfm, setShowCfm] = useState(false);

  const strength = passwordStrength(form.motDePasse);

  const set = (field) => (e) => {
    setForm(p => ({ ...p, [field]: e.target.value }));
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!PASSWORD_RULES.test(form.motDePasse)) {
      setError('Le mot de passe doit contenir au moins 8 caractères, une majuscule, une minuscule, un chiffre et un caractère spécial.');
      return;
    }
    if (form.motDePasse !== form.confirmMotDePasse) {
      setError('Les mots de passe ne correspondent pas.');
      return;
    }
    setLoading(true);
    const result = await registerClient(form);
    if (!result.success) {
      setError(result.error);
      setLoading(false);
      return;
    }
    navigate('/login', { state: { success: `Compte créé ! Connectez-vous avec ${form.email}` } });
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-center px-4 py-8"
      style={{ background: 'linear-gradient(135deg, #1e3a8a 0%, #2563eb 100%)' }}>

      <div className="text-center mb-6">
        <div className="w-14 h-14 bg-white/20 rounded-2xl flex items-center justify-center mx-auto mb-3">
          <CreditCard size={28} className="text-white" />
        </div>
        <h1 className="text-2xl font-bold text-white">Créer mon Compte</h1>
        <p className="text-blue-200 text-sm">MicroFinanceHub</p>
      </div>

      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm p-7">
        <div className="mb-5">
          <h2 className="text-lg font-bold text-gray-800">Activation de votre compte</h2>
          <p className="text-gray-500 text-sm mt-1">
            Votre profil a été créé par votre agent. Renseignez votre email et choisissez un mot de passe pour accéder à votre espace.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="form-label">Email *</label>
            <input
              type="email"
              value={form.email}
              onChange={set('email')}
              placeholder="votre@email.cm"
              required
              className="form-input"
            />
            <p className="text-xs text-gray-400 mt-1">
              Utilisez l'adresse email communiquée à votre agence.
            </p>
          </div>

          <div>
            <label className="form-label">Mot de passe *</label>
            <div className="relative">
              <input
                type={showPwd ? 'text' : 'password'}
                value={form.motDePasse}
                onChange={set('motDePasse')}
                placeholder="Min. 8 car. : A-Z, a-z, 0-9, #@!..."
                required
                minLength={8}
                className="form-input pr-10"
              />
              <button type="button" onClick={() => setShowPwd(v => !v)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600" tabIndex={-1}>
                {showPwd ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
            {form.motDePasse && (
              <div className="mt-2 space-y-1">
                <div className="flex gap-1">
                  {[1,2,3,4,5].map(i => (
                    <div key={i} className={`h-1 flex-1 rounded-full ${i <= strength.score ? strength.color : 'bg-gray-200'}`} />
                  ))}
                </div>
                <p className="text-xs text-gray-500">Force : <span className="font-medium">{strength.label}</span></p>
              </div>
            )}
          </div>

          <div>
            <label className="form-label">Confirmer le mot de passe *</label>
            <div className="relative">
              <input
                type={showCfm ? 'text' : 'password'}
                value={form.confirmMotDePasse}
                onChange={set('confirmMotDePasse')}
                placeholder="Retapez votre mot de passe"
                required
                className="form-input pr-10"
              />
              <button type="button" onClick={() => setShowCfm(v => !v)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600" tabIndex={-1}>
                {showCfm ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
            {form.confirmMotDePasse && form.confirmMotDePasse !== form.motDePasse && (
              <p className="text-xs text-red-500 mt-1">Les mots de passe ne correspondent pas.</p>
            )}
          </div>

          {error && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{error}</p>}

          <div className="flex justify-between pt-2">
            <button type="button" onClick={() => navigate('/login')}
              className="flex items-center gap-2 text-gray-500 hover:text-gray-700 text-sm font-medium">
              <ArrowLeft size={16} /> Connexion
            </button>
            <button type="submit" disabled={loading} className="btn-primary disabled:opacity-60 flex items-center gap-2">
              {loading ? 'Création...' : <><Check size={16} /> Créer mon Compte</>}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default RegisterPage;
