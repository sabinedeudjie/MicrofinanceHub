import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Mail, ArrowLeft, CheckCircle } from 'lucide-react';
import { requestPasswordReset } from '../../api/authApi';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const ForgotPasswordPage = () => {
  const [email, setEmail]       = useState('');
  const [error, setError]       = useState('');
  const [loading, setLoading]   = useState(false);
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!EMAIL_REGEX.test(email.trim())) {
      setError("Veuillez saisir une adresse email valide.");
      return;
    }

    setLoading(true);
    const result = await requestPasswordReset(email.trim().toLowerCase());
    setLoading(false);

    if (result.success) {
      setSubmitted(true);
    } else {
      setError(result.error);
    }
  };

  return (
    <div
      className="min-h-screen flex flex-col items-center justify-center px-4"
      style={{ background: 'linear-gradient(135deg, #1e3a8a 0%, #2563eb 100%)' }}
    >
      <div className="text-center mb-8">
        <img src="/logo.jpg" alt="MicroFinanceHub" className="w-56 mx-auto rounded-2xl shadow-lg" />
      </div>

      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-8">
        {submitted ? (
          <div className="text-center py-4">
            <div className="flex justify-center mb-4">
              <CheckCircle size={48} className="text-green-500" />
            </div>
            <h2 className="text-xl font-bold text-gray-800 mb-3">Email envoyé</h2>
            <p className="text-gray-500 text-sm mb-6 leading-relaxed">
              Si un compte est associé à l'adresse <strong>{email}</strong>,
              vous recevrez un email avec un lien de réinitialisation valable <strong>1 heure</strong>.
            </p>
            <p className="text-gray-400 text-xs mb-6">
              Vérifiez aussi votre dossier spam si vous ne le trouvez pas.
            </p>
            <Link
              to="/login"
              className="inline-flex items-center gap-2 text-primary font-medium hover:underline text-sm"
            >
              <ArrowLeft size={15} />
              Retour à la connexion
            </Link>
          </div>
        ) : (
          <>
            <Link
              to="/login"
              className="inline-flex items-center gap-1 text-gray-400 hover:text-gray-600 text-sm mb-6"
            >
              <ArrowLeft size={14} />
              Retour
            </Link>

            <h2 className="text-xl font-bold text-gray-800 mb-1">Mot de passe oublié ?</h2>
            <p className="text-gray-500 text-sm mb-6">
              Saisissez votre adresse email pour recevoir un lien de réinitialisation.
            </p>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="form-label">Adresse Email</label>
                <div className="relative">
                  <Mail size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => { setEmail(e.target.value); setError(''); }}
                    placeholder="email@exemple.com"
                    required
                    autoFocus
                    className="form-input pl-9"
                  />
                </div>
              </div>

              {error && (
                <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{error}</p>
              )}

              <button
                type="submit"
                disabled={loading}
                className="btn-primary w-full py-3 text-base font-semibold disabled:opacity-60"
              >
                {loading ? 'Envoi en cours...' : 'Envoyer le lien'}
              </button>
            </form>
          </>
        )}
      </div>
    </div>
  );
};

export default ForgotPasswordPage;
