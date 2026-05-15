import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, User, Phone, CreditCard, FileText, Mail, MapPin, Briefcase, Eye } from 'lucide-react';
import Badge from '../../components/common/Badge';
import { getComptesByClient } from '../../api/comptesApi';
import { getTransactions } from '../../api/transactionsApi';
import { getClientById } from '../../api/clientsApi';
import { getClientLoans } from '../../api/loansApi';
import { getClientDocuments } from '../../api/documentsApi';
import { formatMontant, formatDate, formatTypeTransaction, isCredit } from '../../utils/formatters';

const val = (v) => v || '—';

const ClientDetailPage = () => {
  const navigate = useNavigate();
  const { id }   = useParams();

  const [client,       setClient]       = useState(null);
  const [comptes,      setComptes]      = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [prets,        setPrets]        = useState([]);
  const [docs,         setDocs]         = useState([]);
  const [loading,      setLoading]      = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      const [clientRes, comptesRes, pretsRes, docsRes] = await Promise.allSettled([
        getClientById(id),
        getComptesByClient(id, 0, 10),
        getClientLoans(id),
        getClientDocuments(id),
      ]);

      if (clientRes.status === 'fulfilled')  setClient(clientRes.value.data);
      const comptesList = comptesRes.status === 'fulfilled'
        ? (comptesRes.value.data?.data?.content ?? comptesRes.value.data?.content ?? [])
        : [];
      setComptes(comptesList);
      if (pretsRes.status === 'fulfilled')   setPrets(pretsRes.value.data ?? []);
      if (docsRes.status === 'fulfilled')    setDocs(docsRes.value.data ?? []);

      if (comptesList.length > 0) {
        try {
          const txRes = await getTransactions(comptesList[0].id, 0, 5);
          setTransactions(txRes.data?.data?.content ?? txRes.data?.content ?? []);
        } catch { /* ignore */ }
      }
      setLoading(false);
    };
    fetchData();
  }, [id]);

  const nom = client ? `${client.firstName ?? client.prenom} ${client.lastName ?? client.nom}` : `Client #${id}`;

  return (
    <div className="p-6 space-y-5">

      <button onClick={() => navigate('/admin/clients')}
        className="flex items-center gap-2 text-gray-500 hover:text-gray-700 text-sm font-medium">
        <ArrowLeft size={16} /> Retour à la liste
      </button>

      <div className="card flex items-center gap-4">
        <div className="w-14 h-14 bg-blue-100 rounded-full flex items-center justify-center">
          <User size={24} className="text-blue-600" />
        </div>
        <div className="flex-1">
          <h1 className="text-xl font-bold text-gray-800">{loading ? '...' : nom}</h1>
          <p className="text-gray-400 text-sm">ID : {id}</p>
        </div>
        {client?.status && <Badge status={client.status.toLowerCase()} />}
        {client?.creditScore != null && (
          <div className="text-right">
            <p className="text-xs text-gray-400">Score de crédit</p>
            <p className="text-lg font-bold text-blue-600">{client.creditScore}</p>
          </div>
        )}
      </div>

      <div className="grid grid-cols-3 gap-4">

        <div className="card col-span-2 space-y-4">
          <h3 className="font-semibold text-gray-700 flex items-center gap-2">
            <User size={16} className="text-blue-500" /> Informations Personnelles
          </h3>
          {loading ? (
            <p className="text-xs text-gray-400 text-center py-4">Chargement…</p>
          ) : client ? (
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <p className="text-gray-400 text-xs mb-0.5">Prénom</p>
                <p className="font-medium text-gray-700">{val(client.firstName ?? client.prenom)}</p>
              </div>
              <div>
                <p className="text-gray-400 text-xs mb-0.5">Nom</p>
                <p className="font-medium text-gray-700">{val(client.lastName ?? client.nom)}</p>
              </div>
              <div className="flex items-start gap-2">
                <Mail size={13} className="text-gray-400 mt-0.5" />
                <div>
                  <p className="text-gray-400 text-xs mb-0.5">Email</p>
                  <p className="font-medium text-gray-700">{val(client.email)}</p>
                </div>
              </div>
              <div className="flex items-start gap-2">
                <Phone size={13} className="text-gray-400 mt-0.5" />
                <div>
                  <p className="text-gray-400 text-xs mb-0.5">Téléphone</p>
                  <p className="font-medium text-gray-700">{val(client.phoneNumber)}</p>
                </div>
              </div>
              {client.address && (
                <div className="flex items-start gap-2 col-span-2">
                  <MapPin size={13} className="text-gray-400 mt-0.5" />
                  <div>
                    <p className="text-gray-400 text-xs mb-0.5">Adresse</p>
                    <p className="font-medium text-gray-700">{client.address}{client.city ? `, ${client.city}` : ''}</p>
                  </div>
                </div>
              )}
              {client.profession && (
                <div className="flex items-start gap-2">
                  <Briefcase size={13} className="text-gray-400 mt-0.5" />
                  <div>
                    <p className="text-gray-400 text-xs mb-0.5">Profession</p>
                    <p className="font-medium text-gray-700">{client.profession}</p>
                  </div>
                </div>
              )}
              {client.nationalId && (
                <div>
                  <p className="text-gray-400 text-xs mb-0.5">Pièce d'identité</p>
                  <p className="font-medium text-gray-700">{client.nationalId}</p>
                </div>
              )}
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center py-8 text-gray-300">
              <User size={28} />
              <p className="text-sm mt-2">Informations non disponibles</p>
            </div>
          )}
        </div>

        <div className="card">
          <h3 className="font-semibold text-gray-700 flex items-center gap-2 mb-4">
            <Phone size={16} className="text-blue-500" /> Ses Comptes
          </h3>
          {loading ? (
            <p className="text-xs text-gray-400 text-center py-4">Chargement…</p>
          ) : comptes.length === 0 ? (
            <p className="text-xs text-gray-300 text-center py-4">Aucun compte trouvé</p>
          ) : (
            <div className="space-y-3">
              {comptes.map(c => (
                <div key={c.id} className="bg-gray-50 rounded-xl p-4">
                  <div className="flex justify-between items-center mb-1">
                    <span className="text-xs text-gray-400 font-mono">{c.numeroCompte}</span>
                    <Badge status={c.statut} />
                  </div>
                  <p className="text-xs text-gray-500">{c.typeCompte}</p>
                  <p className="text-xl font-bold text-gray-800 mt-1">
                    {formatMontant(c.solde)} <span className="text-sm font-normal text-gray-400">FCFA</span>
                  </p>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="card">
        <h3 className="font-semibold text-gray-700 flex items-center gap-2 mb-4">
          <CreditCard size={16} className="text-blue-500" /> Historique des Prêts
        </h3>
        {loading ? (
          <p className="text-xs text-gray-400 text-center py-4">Chargement…</p>
        ) : prets.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-8 text-gray-300">
            <CreditCard size={28} />
            <p className="text-sm mt-2">Aucun prêt enregistré</p>
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-xs text-gray-400 border-b border-gray-100">
                <th className="text-left pb-3">Référence</th>
                <th className="text-left pb-3">Montant</th>
                <th className="text-left pb-3">Durée</th>
                <th className="text-left pb-3">Mensualité</th>
                <th className="text-left pb-3">Statut</th>
                <th className="text-left pb-3">Date décaiss.</th>
              </tr>
            </thead>
            <tbody>
              {prets.map(p => (
                <tr key={p.id} className="border-b border-gray-50">
                  <td className="py-2 font-mono text-xs text-blue-700">{p.loanNumber ?? p.id?.slice(0, 8)}</td>
                  <td className="py-2 font-semibold">{Number(p.amount ?? 0).toLocaleString('fr-FR')} FCFA</td>
                  <td className="py-2">{p.termMonths} mois</td>
                  <td className="py-2">{Number(p.monthlyPayment ?? 0).toLocaleString('fr-FR')} FCFA</td>
                  <td className="py-2">
                    <span className={`text-xs px-2 py-0.5 rounded-full ${
                      p.status === 'ACTIVE' ? 'bg-green-100 text-green-700' :
                      p.status === 'COMPLETED' ? 'bg-blue-100 text-blue-700' :
                      p.status === 'DEFAULTED' ? 'bg-red-100 text-red-600' :
                      'bg-gray-100 text-gray-600'
                    }`}>
                      {p.status}
                    </span>
                  </td>
                  <td className="py-2 text-xs text-gray-400">
                    {p.disbursementDate ? new Date(p.disbursementDate).toLocaleDateString('fr-FR') : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="card">
        <h3 className="font-semibold text-gray-700 flex items-center gap-2 mb-4">
          <FileText size={16} className="text-green-500" /> Documents KYC
          <span className="text-xs font-normal text-gray-400">({docs.length} document(s))</span>
        </h3>
        {loading ? (
          <p className="text-xs text-gray-400 text-center py-4">Chargement…</p>
        ) : docs.length === 0 ? (
          <p className="text-xs text-gray-300 text-center py-6">Aucun document enregistré</p>
        ) : (
          <div className="grid grid-cols-2 gap-3">
            {docs.map(d => (
              <div key={d.id} className="flex items-center gap-3 bg-gray-50 rounded-lg p-3">
                <FileText size={20} className="text-gray-400 shrink-0" />
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-700 truncate">{d.typeName ?? d.type}</p>
                  <p className="text-xs text-gray-400 truncate">{d.fileName}</p>
                  <p className="text-xs text-gray-400">
                    {d.uploadedAt ? new Date(d.uploadedAt).toLocaleDateString('fr-FR') : ''}
                  </p>
                </div>
                {d.fileUrl && (
                  <a href={d.fileUrl} target="_blank" rel="noreferrer"
                    className="p-1 hover:bg-blue-50 text-gray-400 hover:text-blue-600 rounded shrink-0">
                    <Eye size={14} />
                  </a>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="card">
        <h3 className="font-semibold text-gray-700 flex items-center gap-2 mb-4">
          <FileText size={16} className="text-blue-500" /> Dernières Transactions
        </h3>
        {loading ? (
          <p className="text-xs text-gray-400 text-center py-4">Chargement…</p>
        ) : transactions.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-8 text-gray-300">
            <FileText size={28} />
            <p className="text-sm mt-2">Aucune transaction</p>
          </div>
        ) : (
          <div className="space-y-2">
            {transactions.map(t => {
              const credit = isCredit(t.typeTransaction);
              return (
                <div key={t.id} className="flex items-center justify-between py-2 border-b border-gray-50">
                  <div>
                    <p className="font-medium text-sm text-gray-700">{formatTypeTransaction(t.typeTransaction)}</p>
                    <p className="text-xs text-gray-400">{formatDate(t.dateTransaction)} · {t.reference}</p>
                  </div>
                  <span className={`font-semibold text-sm ${credit ? 'text-green-600' : 'text-red-500'}`}>
                    {credit ? '+' : '-'}{formatMontant(t.montant)} FCFA
                  </span>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

export default ClientDetailPage;
