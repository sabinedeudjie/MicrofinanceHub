import React, { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { Calculator, ArrowRight } from 'lucide-react';

/**
 * SimulateurPage (Client) — Simulateur de prêt
 *
 * Le client peut simuler un prêt avant de le demander :
 *   - Saisie du montant, durée et type de prêt
 *   - Calcul instantané de la mensualité et du coût total
 *   - Tableau d'amortissement simplifié
 *   - Bouton pour pré-remplir la demande
 */

// Taux par type de prêt — valeurs par défaut, à charger depuis config-service quand disponible
// TODO: GET /parametres/taux pour récupérer les taux configurés par l'admin
const TAUX_PAR_TYPE = {
  Agriculture: 5.0,
  Commerce:    4.5,
  Artisanat:   4.0,
  Éducation:   6.0,
  Santé:       3.5,
};

const SimulateurPage = () => {
  const navigate = useNavigate();
  const [montant, setMontant] = useState('');
  const [duree,   setDuree]   = useState('');
  const [type,    setType]    = useState('');

  // Calcul de la mensualité (formule d'amortissement constant)
  const simulation = useMemo(() => {
    const m = parseFloat(montant) || 0;
    const d = parseInt(duree) || 0;

    if (m <= 0 || d <= 0 || !type) return null;

    const taux = TAUX_PAR_TYPE[type] || 5;
    const tauxMensuel = taux / 100 / 12;

    const mensualite = tauxMensuel === 0
      ? m / d
      : m * tauxMensuel / (1 - Math.pow(1 + tauxMensuel, -d));

    const totalRembourse = mensualite * d;
    const totalInterets  = totalRembourse - m;

    // Premières lignes du tableau d'amortissement (max 6)
    const echeances = [];
    let solde = m;
    for (let i = 1; i <= Math.min(d, 6); i++) {
      const interets = solde * tauxMensuel;
      const capital  = mensualite - interets;
      solde -= capital;
      echeances.push({
        mois: i, mensualite: Math.round(mensualite),
        capital: Math.round(capital), interets: Math.round(interets),
        solde: Math.round(Math.max(solde, 0)),
      });
    }

    return { mensualite: Math.round(mensualite), totalRembourse: Math.round(totalRembourse), totalInterets: Math.round(totalInterets), taux, echeances };
  }, [montant, duree, type]);

  const fmt = (n) => n.toLocaleString('fr-FR');

  return (
    <div className="p-6 space-y-6">

      <div>
        <h1 className="text-2xl font-bold text-gray-800">Simulateur de Prêt</h1>
        <p className="text-gray-500 text-sm">Estimez vos mensualités avant de soumettre une demande</p>
      </div>

      <div className="grid grid-cols-2 gap-6">

        {/* Formulaire de simulation */}
        <div className="card space-y-4">
          <h3 className="font-semibold text-gray-700 flex items-center gap-2">
            <Calculator size={18} className="text-blue-500" /> Paramètres du Prêt
          </h3>

          <div>
            <label className="form-label">Montant souhaité (FCFA)</label>
            <input type="number" value={montant} onChange={e => setMontant(e.target.value)}
              placeholder="Ex : 500 000" min="50000" max="5000000" step="10000"
              className="form-input text-lg font-semibold" />
            <input type="range" value={montant || 50000} onChange={e => setMontant(e.target.value)}
              min="50000" max="5000000" step="50000" className="w-full mt-2 accent-blue-600" />
            <div className="flex justify-between text-xs text-gray-400 mt-0.5">
              <span>50 000</span><span>5 000 000 FCFA</span>
            </div>
          </div>

          <div>
            <label className="form-label">Durée de remboursement</label>
            <div className="flex items-center gap-3">
              <input type="number" value={duree} onChange={e => setDuree(e.target.value)}
                placeholder="Ex : 12" min="3" max="36"
                className="form-input w-24 text-lg font-semibold text-center" />
              <span className="text-gray-500">mois</span>
            </div>
            <input type="range" value={duree || 3} onChange={e => setDuree(e.target.value)}
              min="3" max="36" step="1" className="w-full mt-2 accent-blue-600" />
            <div className="flex justify-between text-xs text-gray-400 mt-0.5">
              <span>3 mois</span><span>36 mois</span>
            </div>
          </div>

          <div>
            <label className="form-label">Type de Prêt</label>
            <select value={type} onChange={e => setType(e.target.value)} className="form-input">
              <option value="">Sélectionner un type</option>
              {Object.entries(TAUX_PAR_TYPE).map(([t, r]) => (
                <option key={t} value={t}>{t} — {r}% / an</option>
              ))}
            </select>
          </div>
        </div>

        {/* Résultats de la simulation */}
        {simulation ? (
          <div className="space-y-4">
            {/* Mensualité mise en avant */}
            <div className="card text-center" style={{ background: 'linear-gradient(135deg, #1e3a8a, #2563eb)' }}>
              <p className="text-blue-200 text-sm">Mensualité estimée</p>
              <p className="text-4xl font-bold text-white mt-1">{fmt(simulation.mensualite)}</p>
              <p className="text-blue-200 text-sm">FCFA / mois</p>
            </div>

            {/* Détails */}
            <div className="card grid grid-cols-3 gap-4 text-center">
              <div>
                <p className="text-xs text-gray-400">Taux Annuel</p>
                <p className="text-xl font-bold text-gray-800">{simulation.taux}%</p>
              </div>
              <div>
                <p className="text-xs text-gray-400">Total à Rembourser</p>
                <p className="text-xl font-bold text-gray-800">{fmt(simulation.totalRembourse)}</p>
                <p className="text-xs text-gray-400">FCFA</p>
              </div>
              <div>
                <p className="text-xs text-gray-400">Total Intérêts</p>
                <p className="text-xl font-bold text-blue-600">{fmt(simulation.totalInterets)}</p>
                <p className="text-xs text-gray-400">FCFA</p>
              </div>
            </div>

            {/* Bouton soumettre la demande */}
            <button
              onClick={() => navigate('/client/prets', {
                state: { openModal: true, requestedAmount: montant, termMonths: duree, loanType: type }
              })}
              className="btn-primary w-full py-3 text-base font-semibold flex items-center justify-center gap-2">
              Soumettre cette Demande <ArrowRight size={18} />
            </button>
          </div>
        ) : (
          <div className="card flex items-center justify-center text-gray-400">
            <p>Renseignez les paramètres pour voir la simulation</p>
          </div>
        )}
      </div>

      {/* Tableau d'amortissement (6 premières lignes) */}
      {simulation && (
        <div className="card">
          <h3 className="font-semibold text-gray-700 mb-4">
            Aperçu du Tableau d'Amortissement
            <span className="text-xs font-normal text-gray-400 ml-2">(6 premières mensualités)</span>
          </h3>
          <table className="w-full text-sm">
            <thead>
              <tr className="text-xs text-gray-400 border-b border-gray-100">
                <th className="text-left pb-2">Mois</th>
                <th className="text-right pb-2">Mensualité (FCFA)</th>
                <th className="text-right pb-2">Capital (FCFA)</th>
                <th className="text-right pb-2">Intérêts (FCFA)</th>
                <th className="text-right pb-2">Solde Restant (FCFA)</th>
              </tr>
            </thead>
            <tbody>
              {simulation.echeances.map(e => (
                <tr key={e.mois} className="border-b border-gray-50">
                  <td className="py-2 text-gray-600">Mois {e.mois}</td>
                  <td className="py-2 text-right font-semibold text-gray-800">{fmt(e.mensualite)}</td>
                  <td className="py-2 text-right text-gray-600">{fmt(e.capital)}</td>
                  <td className="py-2 text-right text-blue-500">{fmt(e.interets)}</td>
                  <td className="py-2 text-right text-gray-600">{fmt(e.solde)}</td>
                </tr>
              ))}
              {parseInt(duree) > 6 && (
                <tr>
                  <td colSpan={5} className="py-2 text-center text-xs text-gray-400">
                    … {parseInt(duree) - 6} mensualités supplémentaires (soumettez la demande pour voir le tableau complet)
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default SimulateurPage;
