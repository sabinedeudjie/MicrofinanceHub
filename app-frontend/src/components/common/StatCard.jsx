import React from 'react';

/**
 * StatCard — Carte de statistique réutilisable
 * Affichée en haut de chaque page pour résumer les chiffres clés.
 */
const StatCard = ({ title, value, subtitle, icon: Icon, iconBg, iconColor, positive }) => {
  return (
    <div className="card flex items-center justify-between">
      {/* Partie gauche : titre et valeur */}
      <div>
        <p className="text-sm text-gray-500 font-medium">{title}</p>
        <p className="text-2xl font-bold text-gray-800 mt-1">{value}</p>
        {subtitle && (
          <p className={`text-xs mt-1 font-medium ${
            positive === undefined
              ? 'text-gray-400'
              : positive
              ? 'text-green-500'
              : 'text-red-500'
          }`}>
            {subtitle}
          </p>
        )}
      </div>

      {/* Partie droite : icône dans un cercle coloré */}
      {Icon && (
        <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${iconBg}`}>
          <Icon size={22} className={iconColor} />
        </div>
      )}
    </div>
  );
};

export default StatCard;
