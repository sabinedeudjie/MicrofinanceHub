/** Formate un BigDecimal en nombre lisible : 2450000 → "2 450 000" */
export const formatMontant = (value) => {
  if (value == null) return '—';
  return new Intl.NumberFormat('fr-FR').format(value);
};

/** Formate une date ISO backend en "16 déc. 2025 14:30" */
export const formatDate = (isoString) => {
  if (!isoString) return '—';
  return new Date(isoString).toLocaleString('fr-FR', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
};

/** Traduit un TypeTransaction backend en libellé français */
export const formatTypeTransaction = (type) => {
  const labels = {
    DEPOT:             'Dépôt',
    RETRAIT:           'Retrait',
    VIREMENT_SORTANT:  'Virement sortant',
    VIREMENT_ENTRANT:  'Virement entrant',
  };
  return labels[type] || type;
};

/** Retourne true si la transaction est un crédit (montant positif) */
export const isCredit = (typeTransaction) =>
  typeTransaction === 'DEPOT' || typeTransaction === 'VIREMENT_ENTRANT';

/** Traduit un TypeCompte backend en libellé français */
export const formatTypeCompte = (type) => {
  const labels = {
    EPARGNE:          'Épargne',
    COURANT:          'Courant',
    DEPOT_A_TERME:    'Dépôt à terme',
    MICRO_EPARGNE:    'Micro-épargne',
  };
  return labels[type] || type;
};
