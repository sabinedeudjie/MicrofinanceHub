import React from 'react';

/**
 * Badge — Pastille de statut colorée
 * Utilisée dans les tableaux pour afficher le statut d'une entité.
 *
 */

// Correspondance statut → classes Tailwind (couleur fond + texte)
const STATUS_STYLES = {
  // Statuts positifs / actifs
  actif:                   'bg-green-100 text-green-700',
  active:                  'bg-green-100 text-green-700',
  approved:                'bg-green-100 text-green-700',
  completed:               'bg-green-100 text-green-700',
  successful:              'bg-green-100 text-green-700',
  completee:               'bg-green-100 text-green-700',
  a_jour:                  'bg-green-100 text-green-700',
  envoye:                  'bg-green-100 text-green-700',
  approuve:                'bg-green-100 text-green-700',
  excellent:               'bg-green-100 text-green-700',

  // Statuts en attente / en cours
  en_attente:              'bg-yellow-100 text-yellow-700',
  en_traitement:           'bg-yellow-100 text-yellow-700',
  pending:                 'bg-yellow-100 text-yellow-700',
  a_venir:                 'bg-yellow-100 text-yellow-700',
  en_attente_validation:   'bg-purple-100 text-purple-700',
  programme:               'bg-blue-100 text-blue-700',
  validation:              'bg-purple-100 text-purple-700',
  bonne:                   'bg-blue-100 text-blue-700',
  decaisse:                'bg-blue-100 text-blue-700',
  disbursed:               'bg-blue-100 text-blue-700',

  // Statuts notification
  en_cours:                'bg-blue-100 text-blue-700',
  echec:                   'bg-red-100 text-red-700',
  echec_definitif:         'bg-red-200 text-red-800',
  lue:                     'bg-gray-100 text-gray-500',

  // Statuts intermédiaires
  suspendu:                'bg-orange-100 text-orange-700',
  suspended:               'bg-orange-100 text-orange-700',
  moyenne:                 'bg-orange-100 text-orange-700',
  bloque:                  'bg-orange-100 text-orange-700',
  defaulted:               'bg-orange-100 text-orange-700',

  // Statuts négatifs / fermés
  ferme:                   'bg-gray-100 text-gray-600',
  annulee:                 'bg-gray-100 text-gray-600',
  inactive:                'bg-gray-100 text-gray-600',
  rejected:                'bg-red-100 text-red-700',
  rejete:                  'bg-red-100 text-red-700',
  failed:                  'bg-red-100 text-red-700',
  echouee:                 'bg-red-100 text-red-700',
  en_retard:               'bg-red-100 text-red-700',
  urgent:                  'bg-red-100 text-red-700',
  inactif:                 'bg-red-100 text-red-700',
  faible:                  'bg-red-100 text-red-700',
};

// Libellés pour chaque statut (si aucun label n'est passé)
const STATUS_LABELS = {
  actif:                   'Actif',
  active:                  'Actif',
  inactif:                 'Inactif',
  inactive:                'Inactif',
  approuve:                'Approuvé',
  approved:                'Approuvé',
  en_attente:              'En attente',
  en_attente_validation:   'En validation',
  en_traitement:           'En traitement',
  pending:                 'En attente',
  rejete:                  'Rejeté',
  rejected:                'Rejeté',
  decaisse:                'Décaissé',
  disbursed:               'Décaissé',
  completed:               'Terminé',
  defaulted:               'En défaut',
  validation:              'Validation',
  suspendu:                'Suspendu',
  suspended:               'Suspendu',
  bloque:                  'Bloqué',
  ferme:                   'Fermé',
  urgent:                  'Urgent',
  a_jour:                  'À jour',
  en_retard:               'En retard',
  programme:               'Programmé',
  envoye:                  'Envoyé',
  excellent:               'Excellente',
  bonne:                   'Bonne',
  moyenne:                 'Moyenne',
  faible:                  'Faible',
  successful:              'Réussi',
  completee:               'Complétée',
  failed:                  'Échoué',
  echouee:                 'Échouée',
  annulee:                 'Annulée',
  a_venir:                 'À venir',
  en_cours:                'En cours',
  echec:                   'Échec',
  echec_definitif:         'Échec définitif',
  lue:                     'Lue',
};

const Badge = ({ status, label }) => {
  // Normalisation du statut pour la correspondance (minuscules, espaces → _)
  const normalizedStatus = status?.toLowerCase().replace(/\s+/g, '_').replace(/-/g, '_') || '';
  const styleClass = STATUS_STYLES[normalizedStatus] || 'bg-gray-100 text-gray-600';
  const displayLabel = label || STATUS_LABELS[normalizedStatus] || status;

  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${styleClass}`}>
      {displayLabel}
    </span>
  );
};

export default Badge;
