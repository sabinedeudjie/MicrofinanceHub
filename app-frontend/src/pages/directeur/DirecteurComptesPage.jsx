import React, { useState, useEffect, useCallback } from 'react';
import { Wallet, Search, Settings2 } from 'lucide-react';
import Badge from '../../components/common/Badge';
import Modal from '../../components/common/Modal';
import { getComptesByClient, changerStatut } from '../../api/comptesApi';
import { getMyAgencyClients } from '../../api/agencyApi';
import { formatMontant, formatTypeCompte } from '../../utils/formatters';

const STATUTS = [
  { value: 'ACTIF',    label: 'Actif',    description: 'Compte opérationnel',                  color: 'text-green-700 bg-green-50 border-green-200' },
  { value: 'BLOQUE',   label: 'Bloqué',   description: 'Bloqué suite à activité suspecte',      color: 'text-red-700 bg-red-50 border-red-200' },
  { value: 'SUSPENDU', label: 'Suspendu', description: 'Suspendu temporairement',               color: 'text-orange-700 bg-orange-50 border-orange-200' },
  { value: 'INACTIF',  label: 'Inactif',  description: 'Inactif (+ de 12 mois sans opération)', color: 'text-gray-600 bg-gray-50 border-gray-200' },
  { value: 'FERME',    label: 'Fermé',    description: 'Compte définitivement fermé',           color: 'text-gray-800 bg-gray-100 border-gray-300' },
];

const DirecteurComptesPage = () => {
  const [comptes, setComptes]   = useState([]);
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState(null);
  const [search, setSearch]     = useState('');
  const [filterStatut, setFilterStatut] = useState('');

  const [modalStatut, setModalStatut]     = useState(false);
  const [compteAStatut, setCompteAStatut] = useState(null);
  const [nouveauStatut, setNouveauStatut] = useState('');
  const [statutLoading, setStatutLoading] = useState(false);
  const [statutError, setStatutError]     = useState('');

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await getMyAgencyClients();
      const clients = res.data?.clients ?? [];
      const clientMap = Object.fromEntries(clients.map(c => [c.clientId, c]));
      const clientIds = clients.map(c => c.clientId).filter(Boolean);

      const nested = await Promise.all(
        clientIds.map(id =>
          getComptesByClient(id, 0, 100)
            .then(r => {
              const items = r.data?.data?.content ?? r.data?.content ?? r.data ?? [];
              return items.map(c => ({
                ...c,
                clientNom: `${clientMap[id]?.clientFirstName ?? ''} ${clientMap[id]?.clientLastName ?? ''}`.trim(),
                clientEmail: clientMap[id]?.clientEmail,
              }));
            })
            .catch(() => [])
        )
      );
      setComptes(nested.flat());
    } catch {
      setError('Impossible de charger les comptes. Vérifiez que les services sont démarrés.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

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

  const comptesFiltres = comptes.filter(c => {
    const matchSearch = !search ||
      c.numeroCompte?.toLowerCase().includes(search.toLowerCase()) ||
      c.clientNom?.toLowerCase().includes(search.toLowerCase()) ||
      c.clientEmail?.toLowerCase().includes(search.toLowerCase());
    const matchStatut = !filterStatut || c.statut === filterStatut;
    return matchSearch && matchStatut;
  });

  return (
    <div className="p-6 space-y-6">

      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Comptes Bancaires</h1>
          <p className="text-gray-500 text-sm">Consultez et gérez les statuts des comptes clients</p>
        </div>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">{error}</div>
      )}

      <div className="card">
        <div className="flex items-center justify-between mb-4 gap-3 flex-wrap">
          <div>
            <h3 className="font-semibold text-gray-700 flex items-center gap-2">
              <Wallet size={16} className="text-emerald-600" /> Liste des Comptes
            </h3>
            <p className="text-xs text-gray-400">
              {loading ? 'Chargement…' : `${comptesFiltres.length} compte(s)`}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <select
              value={filterStatut}
              onChange={e => setFilterStatut(e.target.value)}
              className="form-input text-sm w-44">
              <option value="">Tous les statuts</option>
              {STATUTS.map(s => (
                <option key={s.value} value={s.value}>{s.label}</option>
              ))}
              <option value="EN_ATTENTE_VALIDATION">En attente</option>
              <option value="REJETE">Rejeté</option>
            </select>
            <div className="relative">
              <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
              <input value={search} onChange={e => setSearch(e.target.value)}
                placeholder="N° compte, client…" className="form-input pl-9 w-52 text-sm" />
            </div>
          </div>
        </div>

        {loading ? (
          <p className="text-center text-gray-400 py-10 text-sm">Chargement des comptes…</p>
        ) : comptesFiltres.length === 0 ? (
          <p className="text-center text-gray-400 py-10 text-sm">Aucun compte trouvé</p>
        ) : (
          <div className="table-container">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-xs text-gray-400 border-b border-gray-100">
                  <th className="text-left pb-3">N° Compte</th>
                  <th className="text-left pb-3">Client</th>
                  <th className="text-left pb-3">Type</th>
                  <th className="text-left pb-3">Solde</th>
                  <th className="text-left pb-3">Statut</th>
                  <th className="text-left pb-3">Action</th>
                </tr>
              </thead>
              <tbody>
                {comptesFiltres.map(c => (
                  <tr key={c.id ?? c.numeroCompte} className="border-b border-gray-50 hover:bg-gray-50">
                    <td className="py-3 text-xs font-mono text-gray-500">{c.numeroCompte}</td>
                    <td className="py-3">
                      <p className="font-medium text-gray-800 text-sm">
                        {c.clientNom || <span className="text-gray-400 font-mono text-xs">{c.clientId?.slice(0, 8)}…</span>}
                      </p>
                      {c.clientEmail && <p className="text-xs text-gray-400">{c.clientEmail}</p>}
                    </td>
                    <td className="py-3">
                      <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                        c.typeCompte === 'EPARGNE' || c.typeCompte === 'MICRO_EPARGNE'
                          ? 'bg-blue-100 text-blue-700' : 'bg-green-100 text-green-700'
                      }`}>{formatTypeCompte(c.typeCompte)}</span>
                    </td>
                    <td className="py-3 font-bold text-gray-800">{formatMontant(c.solde)} FCFA</td>
                    <td className="py-3"><Badge status={c.statut} /></td>
                    <td className="py-3">
                      {c.statut !== 'REJETE' ? (
                        <button onClick={() => ouvrirModalStatut(c)}
                          title="Changer le statut"
                          className="flex items-center gap-1.5 text-xs text-blue-600 hover:text-blue-800 font-medium">
                          <Settings2 size={13} /> Changer statut
                        </button>
                      ) : (
                        <span className="text-xs text-gray-300">—</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

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
    </div>
  );
};

export default DirecteurComptesPage;
