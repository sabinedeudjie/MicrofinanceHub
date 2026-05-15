import React, { useState, useEffect } from 'react';
import { TrendingDown, TrendingUp, ArrowLeftRight, Search, CheckCircle, ChevronRight, Wallet, RefreshCw } from 'lucide-react';
import { getMyAgencyClients } from '../../api/agencyApi';
import { getComptesByClient } from '../../api/comptesApi';
import { effectuerDepot, effectuerRetrait, effectuerVirement } from '../../api/transactionsApi';
import { formatMontant } from '../../utils/formatters';

const ONGLETS = [
  { key: 'depot',    label: 'Dépôt',    icon: TrendingDown,   color: 'text-green-600',  bg: 'bg-green-50',  btn: 'bg-green-500 hover:bg-green-600' },
  { key: 'retrait',  label: 'Retrait',   icon: TrendingUp,     color: 'text-red-500',    bg: 'bg-red-50',    btn: 'bg-red-500 hover:bg-red-600' },
  { key: 'virement', label: 'Virement',  icon: ArrowLeftRight, color: 'text-blue-600',   bg: 'bg-blue-50',   btn: 'bg-blue-600 hover:bg-blue-700' },
];

const TYPE_LABELS = {
  EPARGNE:       'Épargne',
  COURANT:       'Courant',
  MICRO_EPARGNE: 'Micro-Épargne',
  DEPOT_A_TERME: 'Dépôt à Terme',
  CREDIT:        'Crédit',
};

const DirecteurGuichetPage = () => {
  const [onglet,    setOnglet]    = useState('depot');
  const [search,    setSearch]    = useState('');
  const [clientSel, setClientSel] = useState(null);

  const [comptesClient,  setComptesClient]  = useState([]);
  const [compteSel,      setCompteSel]      = useState(null);
  const [loadingComptes, setLoadingComptes] = useState(false);

  const [form,        setForm]        = useState({ montant: '', modePaiement: 'ESPECES', numeroPaiement: '', description: '' });
  const [destination, setDestination] = useState('');
  const [confirme,    setConfirme]    = useState(false);
  const [submitting,  setSubmitting]  = useState(false);
  const [submitError, setSubmitError] = useState('');
  const [succes,      setSucces]      = useState(false);

  const [mesClients, setMesClients] = useState([]);

  useEffect(() => {
    getMyAgencyClients()
      .then(res => {
        const clients = (res.data?.clients ?? []).map(c => ({
          id:        c.clientId,
          firstName: c.clientFirstName,
          lastName:  c.clientLastName,
          email:     c.clientEmail,
        }));
        setMesClients(clients);
      })
      .catch(() => setMesClients([]));
  }, []);

  const clientsFiltres = mesClients.filter(c =>
    `${c.firstName} ${c.lastName}`.toLowerCase().includes(search.toLowerCase()) ||
    c.email?.toLowerCase().includes(search.toLowerCase()) ||
    c.id?.includes(search)
  );

  const handleSelectClient = async (client) => {
    setClientSel(client);
    setCompteSel(null);
    setComptesClient([]);
    setConfirme(false);
    setSucces(false);
    setSubmitError('');
    setForm({ montant: '', modePaiement: 'ESPECES', numeroPaiement: '', description: '' });
    setDestination('');
    setLoadingComptes(true);
    try {
      const res = await getComptesByClient(client.id, 0, 50);
      const items = res.data?.data?.content ?? res.data?.content ?? res.data ?? [];
      setComptesClient(items.filter(ct => ct.statut === 'ACTIF' || ct.statut === 'ACTIVE'));
    } catch {
      setComptesClient([]);
    } finally {
      setLoadingComptes(false);
    }
  };

  const handleSelectCompte = (compte) => {
    setCompteSel(compte);
    setConfirme(false);
    setSubmitError('');
    setForm({ montant: '', modePaiement: 'ESPECES', numeroPaiement: '', description: '' });
    setDestination('');
  };

  const handleSoumettre = async (e) => {
    e.preventDefault();
    if (!confirme) { setConfirme(true); return; }
    if (!compteSel) { setSubmitError('Sélectionnez un compte.'); return; }

    setSubmitting(true);
    setSubmitError('');
    try {
      if (onglet === 'depot') {
        await effectuerDepot(compteSel.id, {
          montant:   Number(form.montant),
          modeDepot: form.modePaiement,
          ...(form.modePaiement === 'MOBILE_MONEY' && { numeroPaiement: form.numeroPaiement }),
          ...(form.description && { description: form.description }),
        });
      } else if (onglet === 'retrait') {
        await effectuerRetrait(compteSel.id, {
          montant:     Number(form.montant),
          modeRetrait: form.modePaiement,
          ...(form.modePaiement === 'MOBILE_MONEY' && { numeroPaiement: form.numeroPaiement }),
          ...(form.description && { description: form.description }),
        });
      } else {
        await effectuerVirement(compteSel.id, {
          montant:                 Number(form.montant),
          numeroCompteDestination: destination,
          ...(form.description && { motif: form.description }),
        });
      }

      setSucces(true);
      setTimeout(() => {
        setSucces(false);
        setConfirme(false);
        setClientSel(null);
        setComptesClient([]);
        setCompteSel(null);
        setForm({ montant: '', modePaiement: 'ESPECES', numeroPaiement: '', description: '' });
        setDestination('');
      }, 2500);
    } catch (err) {
      setConfirme(false);
      setSubmitError(err.response?.data?.message || "Erreur lors de l'opération.");
    } finally {
      setSubmitting(false);
    }
  };

  const ongletActif = ONGLETS.find(o => o.key === onglet);

  return (
    <div className="p-6 space-y-5">

      <div>
        <h1 className="text-2xl font-bold text-gray-800">Guichet — Opérations</h1>
        <p className="text-gray-500 text-sm">Effectuez des opérations pour les clients de votre agence</p>
      </div>

      {/* Onglets opération */}
      <div className="flex gap-2">
        {ONGLETS.map(({ key, label, icon: Icon, color, bg }) => (
          <button key={key}
            onClick={() => { setOnglet(key); setConfirme(false); setSucces(false); setSubmitError(''); }}
            className={`flex items-center gap-2 px-5 py-2.5 rounded-xl font-medium text-sm transition-all ${
              onglet === key ? `${bg} ${color} shadow-sm` : 'bg-white text-gray-500 hover:bg-gray-50'
            }`}>
            <Icon size={16} /> {label}
          </button>
        ))}
      </div>

      <div className="grid grid-cols-2 gap-5">

        {/* Panneau gauche : sélection client */}
        <div className="card">
          <h3 className="font-semibold text-gray-700 mb-3">1. Sélectionner un Client</h3>
          <div className="relative mb-3">
            <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input value={search} onChange={e => setSearch(e.target.value)}
              placeholder="Nom, email ou ID…" className="form-input pl-9" />
          </div>
          <div className="space-y-2 max-h-96 overflow-y-auto">
            {clientsFiltres.length === 0 ? (
              <p className="text-center text-gray-300 text-sm py-8">
                {mesClients.length === 0 ? 'Aucun client dans cette agence' : 'Aucun résultat'}
              </p>
            ) : (
              clientsFiltres.map(c => (
                <button key={c.id}
                  onClick={() => handleSelectClient(c)}
                  className={`w-full text-left p-3 rounded-xl border transition-all ${
                    clientSel?.id === c.id
                      ? 'border-blue-500 bg-blue-50'
                      : 'border-gray-200 hover:bg-gray-50'
                  }`}>
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="font-medium text-gray-800 text-sm">{c.firstName} {c.lastName}</p>
                      <p className="text-xs text-gray-400">{c.email}</p>
                    </div>
                    {clientSel?.id === c.id && (
                      <span className="text-xs text-blue-600 font-medium">
                        {loadingComptes ? '…' : `${comptesClient.length} compte(s)`}
                      </span>
                    )}
                  </div>
                </button>
              ))
            )}
          </div>
        </div>

        {/* Panneau droit : étapes 2 & 3 */}
        <div className="card flex flex-col gap-4">

          {!clientSel ? (
            <div className="flex-1 flex items-center justify-center py-16 text-gray-300">
              <div className="text-center">
                <Wallet size={36} className="mx-auto mb-2" />
                <p className="text-sm">Sélectionnez un client</p>
              </div>
            </div>
          ) : loadingComptes ? (
            <p className="text-gray-400 text-sm text-center py-12">Chargement des comptes…</p>
          ) : comptesClient.length === 0 ? (
            <div className="text-center py-12 text-gray-400 text-sm space-y-1">
              <Wallet size={28} className="mx-auto mb-2" />
              <p>Ce client n'a pas de compte actif.</p>
              <p className="text-xs text-gray-300">Les comptes en attente de validation ne sont pas disponibles.</p>
            </div>
          ) : succes ? (
            <div className="flex-1 flex flex-col items-center justify-center py-12 text-center">
              <CheckCircle size={48} className="text-green-500 mb-3" />
              <p className="text-green-600 font-semibold">Opération effectuée avec succès !</p>
            </div>
          ) : (
            <>
              {/* Sélection du compte */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <h3 className="font-semibold text-gray-700 text-sm">
                    2. Choisir le compte — <span className="font-normal text-gray-500">{clientSel.firstName} {clientSel.lastName}</span>
                  </h3>
                  {compteSel && (
                    <button onClick={() => setCompteSel(null)}
                      className="text-xs text-blue-500 hover:underline flex items-center gap-1">
                      <RefreshCw size={11} /> Changer
                    </button>
                  )}
                </div>

                {!compteSel ? (
                  <div className="space-y-2">
                    {comptesClient.map(ct => (
                      <button key={ct.id} onClick={() => handleSelectCompte(ct)}
                        className="w-full text-left border border-gray-200 hover:border-blue-400 hover:bg-blue-50 rounded-xl px-4 py-3 transition-all group">
                        <div className="flex items-center justify-between">
                          <div>
                            <p className="text-sm font-semibold text-gray-800">
                              {TYPE_LABELS[ct.typeCompte] ?? ct.typeCompte}
                            </p>
                            <p className="text-xs text-gray-400 font-mono">{ct.numeroCompte}</p>
                          </div>
                          <div className="text-right">
                            <p className="text-sm font-bold text-gray-700">{formatMontant(ct.solde)} FCFA</p>
                            <ChevronRight size={14} className="text-gray-300 group-hover:text-blue-400 ml-auto mt-0.5" />
                          </div>
                        </div>
                      </button>
                    ))}
                  </div>
                ) : (
                  <div className={`rounded-xl px-4 py-3 border-2 border-blue-300 ${ongletActif.bg}`}>
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-sm font-semibold text-gray-800">
                          {TYPE_LABELS[compteSel.typeCompte] ?? compteSel.typeCompte}
                        </p>
                        <p className="text-xs text-gray-400 font-mono">{compteSel.numeroCompte}</p>
                      </div>
                      <p className="text-sm font-bold text-gray-700">{formatMontant(compteSel.solde)} FCFA</p>
                    </div>
                  </div>
                )}
              </div>

              {/* Étape 3 : formulaire opération */}
              {compteSel && (
                <form onSubmit={handleSoumettre} className="space-y-3 border-t border-gray-100 pt-4">
                  <h3 className={`font-semibold text-sm flex items-center gap-2 ${ongletActif.color}`}>
                    <ongletActif.icon size={15} /> 3. {ongletActif.label}
                  </h3>

                  <div>
                    <label className="form-label">Montant (FCFA) *</label>
                    <input type="number" value={form.montant}
                      onChange={e => setForm(p => ({...p, montant: e.target.value}))}
                      placeholder="Ex: 50 000" required min="0.01" step="any" className="form-input text-lg font-semibold" />
                  </div>

                  {onglet !== 'virement' && (
                    <div>
                      <label className="form-label">Mode de paiement</label>
                      <select value={form.modePaiement}
                        onChange={e => setForm(p => ({...p, modePaiement: e.target.value}))}
                        className="form-input">
                        <option value="ESPECES">Espèces (Guichet)</option>
                        <option value="MOBILE_MONEY">Mobile Money</option>
                      </select>
                    </div>
                  )}

                  {form.modePaiement === 'MOBILE_MONEY' && onglet !== 'virement' && (
                    <div>
                      <label className="form-label">Numéro Mobile Money *</label>
                      <input value={form.numeroPaiement}
                        onChange={e => setForm(p => ({...p, numeroPaiement: e.target.value}))}
                        placeholder="237XXXXXXXXX" required className="form-input" />
                    </div>
                  )}

                  {onglet === 'virement' && (
                    <div>
                      <label className="form-label">N° Compte destinataire *</label>
                      <input value={destination}
                        onChange={e => setDestination(e.target.value)}
                        placeholder="Ex: MFH-20250001" required className="form-input" />
                    </div>
                  )}

                  <div>
                    <label className="form-label">Description</label>
                    <input value={form.description}
                      onChange={e => setForm(p => ({...p, description: e.target.value}))}
                      placeholder="Motif de l'opération" className="form-input" />
                  </div>

                  {confirme && (
                    <div className={`rounded-xl p-3 ${ongletActif.bg} border border-current text-sm space-y-1`}>
                      <p className={`font-semibold ${ongletActif.color}`}>Confirmer l'opération ?</p>
                      <p className="text-gray-700">Client : <strong>{clientSel.firstName} {clientSel.lastName}</strong></p>
                      <p className="text-gray-700">Compte : <strong>{compteSel.numeroCompte}</strong> ({TYPE_LABELS[compteSel.typeCompte]})</p>
                      <p className="text-gray-700">Montant : <strong>{formatMontant(Number(form.montant))} FCFA</strong></p>
                      {onglet !== 'virement' && <p className="text-gray-700">Mode : <strong>{form.modePaiement}</strong></p>}
                      {onglet === 'virement' && <p className="text-gray-700">Destinataire : <strong>{destination}</strong></p>}
                    </div>
                  )}

                  {submitError && (
                    <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{submitError}</p>
                  )}

                  <button type="submit" disabled={submitting}
                    className={`w-full py-3 rounded-xl font-semibold text-white transition-colors disabled:opacity-60 ${ongletActif.btn}`}>
                    {submitting ? 'Traitement…' : confirme ? '✓ Confirmer' : `Effectuer le ${ongletActif.label}`}
                  </button>
                </form>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default DirecteurGuichetPage;
