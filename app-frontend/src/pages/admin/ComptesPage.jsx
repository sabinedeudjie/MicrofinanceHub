import React, { useState, useEffect, useCallback } from 'react';
import { Wallet, DollarSign, Search, Plus, Pencil, Trash2, Settings2 } from 'lucide-react';
import StatCard from '../../components/common/StatCard';
import Badge from '../../components/common/Badge';
import Modal from '../../components/common/Modal';
import { rechercherComptes, ouvrirCompte, changerStatut, modifierCompte, supprimerCompte } from '../../api/comptesApi';
import { getAllClients } from '../../api/clientsApi';
import { formatMontant, formatTypeCompte } from '../../utils/formatters';

const ComptesPage = () => {
  const [search, setSearch]   = useState('');
  const [comptes, setComptes] = useState([]);
  const [stats, setStats]     = useState({ totalActifs: 0, soldeTotal: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState(null);

  // liste des clients pour le select
  const [clients, setClients]             = useState([]);
  const [loadingClients, setLoadingClients] = useState(false);
  const [clientSearch, setClientSearch]   = useState('');

  const [modalOuverture, setModalOuverture] = useState(false);
  const [compteForm, setCompteForm]         = useState({ clientId: '', typeCompte: 'EPARGNE', devise: 'XAF', soldeInitial: '' });
  const [creating, setCreating]             = useState(false);
  const [createError, setCreateError]       = useState('');

  const [modalModif, setModalModif]     = useState(false);
  const [compteAModif, setCompteAModif] = useState(null);
  const [modifForm, setModifForm]       = useState({ description: '', soldeMinimum: '', plafond: '', tauxInteret: '' });
  const [modifLoading, setModifLoading] = useState(false);
  const [modifError, setModifError]     = useState('');

  const [modalSupp, setModalSupp]     = useState(false);
  const [compteASupp, setCompteASupp] = useState(null);
  const [suppLoading, setSuppLoading] = useState(false);

  const [modalStatut, setModalStatut]     = useState(false);
  const [compteAStatut, setCompteAStatut] = useState(null);
  const [nouveauStatut, setNouveauStatut] = useState('');
  const [statutLoading, setStatutLoading] = useState(false);
  const [statutError, setStatutError]     = useState('');

  const STATUTS = [
    { value: 'ACTIF',    label: 'Actif',    description: 'Compte opérationnel',                     color: 'text-green-700 bg-green-50 border-green-200' },
    { value: 'BLOQUE',   label: 'Bloqué',   description: 'Bloqué suite à activité suspecte',         color: 'text-red-700 bg-red-50 border-red-200' },
    { value: 'SUSPENDU', label: 'Suspendu', description: 'Suspendu temporairement',                  color: 'text-orange-700 bg-orange-50 border-orange-200' },
    { value: 'INACTIF',  label: 'Inactif',  description: 'Inactif (+ de 12 mois sans opération)',    color: 'text-gray-600 bg-gray-50 border-gray-200' },
    { value: 'FERME',    label: 'Fermé',    description: 'Compte définitivement fermé',              color: 'text-gray-800 bg-gray-100 border-gray-300' },
  ];

  const ouvrirModalStatut = (compte) => {
    setCompteAStatut(compte);
    const options = STATUTS.filter(s => s.value !== compte.statut);
    setNouveauStatut(options[0]?.value || '');
    setStatutError('');
    setModalStatut(true);
  };

  const handleChangerStatut = async () => {
    setStatutLoading(true);
    setStatutError('');
    try {
      await changerStatut(compteAStatut.id, nouveauStatut);
      setModalStatut(false);
      await fetchData();
    } catch (err) {
      setStatutError(err.response?.data?.message || 'Erreur lors du changement de statut');
    } finally {
      setStatutLoading(false);
    }
  };

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await rechercherComptes({ page: 0, size: 50 });
      const comptesList = res.data.data.content ?? [];
      setComptes(comptesList);
      const actifs     = comptesList.filter(c => c.statut === 'ACTIF').length;
      const soldeTotal = comptesList.reduce((sum, c) => sum + (c.solde || 0), 0);
      setStats({ totalActifs: actifs, soldeTotal });
    } catch {
      setError('Impossible de charger les comptes. Vérifiez que account-service est démarré (port 8082).');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  const openModalOuverture = async () => {
    setCompteForm({ clientId: '', typeCompte: 'EPARGNE', devise: 'XAF', soldeInitial: '' });
    setCreateError('');
    setClientSearch('');
    setModalOuverture(true);
    if (clients.length === 0) {
      setLoadingClients(true);
      try {
        const res = await getAllClients();
        setClients(res.data ?? []);
      } catch {
        // select restera vide
      } finally {
        setLoadingClients(false);
      }
    }
  };

  const handleOuvrirCompte = async (e) => {
    e.preventDefault();
    setCreating(true);
    setCreateError('');
    try {
      const clientSelectionne = clients.find(c => c.id === compteForm.clientId);
      await ouvrirCompte({
        clientId:     compteForm.clientId,
        typeCompte:   compteForm.typeCompte,
        devise:       compteForm.devise,
        soldeInitial: compteForm.soldeInitial ? Number(compteForm.soldeInitial) : 0,
        clientEmail:  clientSelectionne?.email || null,
        clientNom:    clientSelectionne ? `${clientSelectionne.firstName} ${clientSelectionne.lastName}` : null,
      });
      setModalOuverture(false);
      setCompteForm({ clientId: '', typeCompte: 'EPARGNE', devise: 'XAF', soldeInitial: '' });
      await fetchData();
    } catch (err) {
      setCreateError(err.response?.data?.message || 'Erreur lors de la création du compte');
    } finally {
      setCreating(false);
    }
  };

  const ouvrirModif = (compte) => {
    setCompteAModif(compte);
    setModifForm({
      description:  compte.description || '',
      soldeMinimum: compte.soldeMinimum != null ? String(compte.soldeMinimum) : '',
      plafond:      compte.plafond      != null ? String(compte.plafond)      : '',
      tauxInteret:  compte.tauxInteret  != null ? String(compte.tauxInteret)  : '',
    });
    setModifError('');
    setModalModif(true);
  };

  const handleModifier = async (e) => {
    e.preventDefault();
    setModifLoading(true);
    setModifError('');
    try {
      await modifierCompte(compteAModif.id, {
        ...(modifForm.description  && { description:  modifForm.description }),
        ...(modifForm.soldeMinimum && { soldeMinimum: Number(modifForm.soldeMinimum) }),
        ...(modifForm.plafond      && { plafond:      Number(modifForm.plafond) }),
        ...(modifForm.tauxInteret  && { tauxInteret:  Number(modifForm.tauxInteret) }),
      });
      setModalModif(false);
      await fetchData();
    } catch (err) {
      setModifError(err.response?.data?.message || 'Erreur lors de la modification');
    } finally {
      setModifLoading(false);
    }
  };

  const handleSupprimer = async () => {
    setSuppLoading(true);
    try {
      await supprimerCompte(compteASupp.id);
      setModalSupp(false);
      await fetchData();
    } catch (err) {
      alert(err.response?.data?.message || 'Erreur lors de la suppression');
    } finally {
      setSuppLoading(false);
    }
  };

  // map id → nom pour affichage dans le tableau
  const clientsMap = Object.fromEntries(
    clients.map(c => [c.id, `${c.firstName} ${c.lastName}`])
  );

  const filteredClients = clients.filter(c => {
    const term = clientSearch.toLowerCase();
    return (
      `${c.firstName} ${c.lastName}`.toLowerCase().includes(term) ||
      (c.phoneNumber && c.phoneNumber.includes(clientSearch)) ||
      (c.email && c.email.toLowerCase().includes(term))
    );
  });

  const comptesFiltres = comptes.filter(c =>
    c.numeroCompte?.toLowerCase().includes(search.toLowerCase()) ||
    String(c.clientId).includes(search) ||
    (clientsMap[c.clientId] ?? '').toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="p-6 space-y-6">

      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Gestion des Comptes</h1>
          <p className="text-gray-500 text-sm">Créez et gérez les comptes clients</p>
        </div>
        <button onClick={openModalOuverture} className="btn-primary flex items-center gap-2">
          <Plus size={16} /> Nouveau Compte
        </button>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <StatCard title="Comptes Actifs" value={loading ? '…' : String(stats.totalActifs)}                 icon={Wallet}     iconBg="bg-blue-100"  iconColor="text-blue-600" />
        <StatCard title="Solde Total"    value={loading ? '…' : formatMontant(stats.soldeTotal) + ' FCFA'} icon={DollarSign} iconBg="bg-green-100" iconColor="text-green-600" />
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">{error}</div>
      )}

      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h3 className="font-semibold text-gray-700">Liste des Comptes</h3>
            <p className="text-xs text-gray-400">
              {loading ? 'Chargement…' : `${comptesFiltres.length} compte(s) trouvé(s)`}
            </p>
          </div>
          <div className="relative">
            <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input value={search} onChange={e => setSearch(e.target.value)}
              placeholder="Numéro, nom ou client…" className="form-input pl-9 w-60 text-sm" />
          </div>
        </div>

        {loading ? (
          <p className="text-center text-gray-400 py-8 text-sm">Chargement des comptes…</p>
        ) : comptesFiltres.length === 0 ? (
          <p className="text-center text-gray-400 py-8 text-sm">
            {error ? 'Données indisponibles' : 'Aucun compte — cliquez sur "Nouveau Compte" pour en créer un'}
          </p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-xs text-gray-400 border-b border-gray-100">
                <th className="text-left pb-3">N° Compte</th>
                <th className="text-left pb-3">Client</th>
                <th className="text-left pb-3">Type</th>
                <th className="text-left pb-3">Solde</th>
                <th className="text-left pb-3">Devise</th>
                <th className="text-left pb-3">Statut</th>
                <th className="text-left pb-3">Actions</th>
              </tr>
            </thead>
            <tbody>
              {comptesFiltres.map(c => (
                <tr key={c.id} className="border-b border-gray-50 hover:bg-gray-50">
                  <td className="py-3 text-xs font-mono text-gray-500">{c.numeroCompte}</td>
                  <td className="py-3">
                    <p className="font-medium text-gray-800">
                      {clientsMap[c.clientId] ?? <span className="text-gray-400 text-xs font-mono">{c.clientId?.slice(0, 8)}…</span>}
                    </p>
                  </td>
                  <td className="py-3">
                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                      c.typeCompte === 'EPARGNE' || c.typeCompte === 'MICRO_EPARGNE'
                        ? 'bg-blue-100 text-blue-700'
                        : 'bg-green-100 text-green-700'
                    }`}>{formatTypeCompte(c.typeCompte)}</span>
                  </td>
                  <td className="py-3 font-bold text-gray-800">{formatMontant(c.solde)} FCFA</td>
                  <td className="py-3 text-xs text-gray-500">{c.devise}</td>
                  <td className="py-3"><Badge status={c.statut} /></td>
                  <td className="py-3">
                    <div className="flex items-center gap-1">
                      {c.statut !== 'REJETE' && (
                        <button onClick={() => ouvrirModalStatut(c)}
                          title="Changer le statut"
                          className="p-1.5 hover:bg-blue-50 text-gray-400 hover:text-blue-600 rounded-lg">
                          <Settings2 size={14} />
                        </button>
                      )}
                      <button onClick={() => ouvrirModif(c)}
                        title="Modifier les paramètres"
                        className="p-1.5 hover:bg-gray-100 text-gray-400 hover:text-gray-700 rounded-lg">
                        <Pencil size={14} />
                      </button>
                      <button onClick={() => { setCompteASupp(c); setModalSupp(true); }}
                        title="Supprimer le compte"
                        className="p-1.5 hover:bg-red-50 text-gray-400 hover:text-red-500 rounded-lg">
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Modale création de compte */}
      <Modal isOpen={modalOuverture} onClose={() => { setModalOuverture(false); setCreateError(''); setClientSearch(''); }}
        title="Ouvrir un Nouveau Compte" subtitle="Sélectionnez le client et le type de compte">
        <form onSubmit={handleOuvrirCompte} className="space-y-4">

          <div>
            <label className="form-label">Client *</label>
            <input
              type="text"
              placeholder="Rechercher par nom, email ou téléphone…"
              value={clientSearch}
              onChange={e => setClientSearch(e.target.value)}
              className="form-input mb-1 text-sm"
            />
            <select
              value={compteForm.clientId}
              onChange={e => setCompteForm(p => ({ ...p, clientId: e.target.value }))}
              required
              className="form-input"
              size={5}
            >
              <option value="">-- Sélectionner un client --</option>
              {loadingClients ? (
                <option disabled>Chargement…</option>
              ) : (
                filteredClients.map(c => (
                  <option key={c.id} value={c.id}>
                    {c.firstName} {c.lastName}{c.phoneNumber ? ` · ${c.phoneNumber}` : ''}
                  </option>
                ))
              )}
            </select>
            {!loadingClients && clients.length > 0 && filteredClients.length === 0 && (
              <p className="text-xs text-gray-400 mt-1">Aucun client correspondant</p>
            )}
          </div>

          <div>
            <label className="form-label">Type de Compte *</label>
            <select value={compteForm.typeCompte}
              onChange={e => setCompteForm(p => ({...p, typeCompte: e.target.value}))}
              className="form-input">
              <option value="COURANT">Courant</option>
              <option value="EPARGNE">Épargne</option>
              <option value="MICRO_EPARGNE">Micro-Épargne</option>
              <option value="DEPOT_A_TERME">Dépôt à Terme</option>
              <option value="CREDIT">Crédit</option>
            </select>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="form-label">Devise</label>
              <select value={compteForm.devise}
                onChange={e => setCompteForm(p => ({...p, devise: e.target.value}))}
                className="form-input">
                <option value="XAF">XAF (FCFA)</option>
                <option value="EUR">EUR</option>
                <option value="USD">USD</option>
              </select>
            </div>
            <div>
              <label className="form-label">Solde Initial (FCFA)</label>
              <input type="number" value={compteForm.soldeInitial}
                onChange={e => setCompteForm(p => ({...p, soldeInitial: e.target.value}))}
                placeholder="0" min="0" step="any" className="form-input" />
            </div>
          </div>

          {createError && (
            <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{createError}</p>
          )}
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => { setModalOuverture(false); setCreateError(''); setClientSearch(''); }} className="btn-secondary">
              Annuler
            </button>
            <button type="submit" disabled={creating || !compteForm.clientId} className="btn-primary disabled:opacity-60">
              {creating ? 'Création…' : 'Ouvrir le Compte'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Modale modification des paramètres */}
      <Modal isOpen={modalModif} onClose={() => { setModalModif(false); setModifError(''); }}
        title="Modifier les Paramètres"
        subtitle={compteAModif ? `Compte ${compteAModif.numeroCompte}` : ''}>
        <form onSubmit={handleModifier} className="space-y-4">
          <div>
            <label className="form-label">Description</label>
            <input value={modifForm.description}
              onChange={e => setModifForm(p => ({...p, description: e.target.value}))}
              placeholder="Ex: Compte épargne scolaire" className="form-input" />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="form-label">Solde Minimum (FCFA)</label>
              <input type="number" value={modifForm.soldeMinimum}
                onChange={e => setModifForm(p => ({...p, soldeMinimum: e.target.value}))}
                placeholder="Ex: 5000" min="0" step="any" className="form-input" />
            </div>
            <div>
              <label className="form-label">Plafond (FCFA)</label>
              <input type="number" value={modifForm.plafond}
                onChange={e => setModifForm(p => ({...p, plafond: e.target.value}))}
                placeholder="Ex: 2000000" min="0" step="any" className="form-input" />
            </div>
          </div>
          <div>
            <label className="form-label">Taux d'Intérêt (0 à 1, ex: 0.05 = 5%)</label>
            <input type="number" value={modifForm.tauxInteret}
              onChange={e => setModifForm(p => ({...p, tauxInteret: e.target.value}))}
              placeholder="Ex: 0.05" min="0" max="1" step="any" className="form-input" />
          </div>
          {modifError && (
            <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{modifError}</p>
          )}
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => { setModalModif(false); setModifError(''); }} className="btn-secondary">
              Annuler
            </button>
            <button type="submit" disabled={modifLoading} className="btn-primary disabled:opacity-60">
              {modifLoading ? 'Enregistrement…' : 'Enregistrer'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Modale changement de statut */}
      <Modal isOpen={modalStatut} onClose={() => { setModalStatut(false); setStatutError(''); }}
        title="Changer le Statut du Compte"
        subtitle={compteAStatut ? `Compte ${compteAStatut.numeroCompte}` : ''}>
        <div className="space-y-4">
          <div className="flex items-center gap-2 text-sm text-gray-500">
            <span>Statut actuel :</span>
            <Badge status={compteAStatut?.statut} />
          </div>

          <div>
            <label className="form-label">Nouveau Statut *</label>
            <div className="space-y-2 mt-1">
              {STATUTS.filter(s => s.value !== compteAStatut?.statut).map(s => (
                <label key={s.value}
                  className={`flex items-start gap-3 p-3 rounded-lg border cursor-pointer transition-all ${
                    nouveauStatut === s.value ? s.color + ' border-current' : 'border-gray-200 hover:bg-gray-50'
                  }`}>
                  <input type="radio" name="statut" value={s.value}
                    checked={nouveauStatut === s.value}
                    onChange={() => setNouveauStatut(s.value)}
                    className="mt-0.5" />
                  <div>
                    <p className="font-medium text-sm">{s.label}</p>
                    <p className="text-xs text-gray-500">{s.description}</p>
                  </div>
                </label>
              ))}
            </div>
          </div>

          {nouveauStatut === 'FERME' && (
            <div className="bg-amber-50 border border-amber-200 rounded-lg px-3 py-2 text-xs text-amber-700">
              La fermeture est définitive. Le solde doit être à zéro pour pouvoir supprimer le compte.
            </div>
          )}

          {statutError && (
            <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{statutError}</p>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <button onClick={() => { setModalStatut(false); setStatutError(''); }} className="btn-secondary">
              Annuler
            </button>
            <button onClick={handleChangerStatut} disabled={statutLoading || !nouveauStatut}
              className="btn-primary disabled:opacity-60">
              {statutLoading ? 'Enregistrement…' : 'Confirmer'}
            </button>
          </div>
        </div>
      </Modal>

      {/* Modale confirmation suppression */}
      <Modal isOpen={modalSupp} onClose={() => setModalSupp(false)}
        title="Supprimer le Compte"
        subtitle={compteASupp ? `Compte ${compteASupp.numeroCompte} — Solde : ${formatMontant(compteASupp?.solde ?? 0)} FCFA` : ''}>
        <div className="space-y-4">
          <p className="text-sm text-gray-700">
            Cette action est <strong>irréversible</strong>. Assurez-vous que le solde est à zéro avant de supprimer.
          </p>
          <div className="flex justify-end gap-3 pt-2">
            <button onClick={() => setModalSupp(false)} className="btn-secondary">Annuler</button>
            <button onClick={handleSupprimer} disabled={suppLoading}
              className="bg-red-500 hover:bg-red-600 text-white font-semibold px-5 py-2 rounded-xl disabled:opacity-60">
              {suppLoading ? 'Suppression…' : 'Confirmer la Suppression'}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default ComptesPage;
