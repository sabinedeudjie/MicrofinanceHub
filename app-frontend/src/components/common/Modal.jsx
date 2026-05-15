import React, { useEffect } from 'react';
import { X } from 'lucide-react';

/**
 * Modal — Fenêtre modale générique
 * Peut être utilisée partout dans l'application pour afficher des formulaires
 * ou des confirmations par-dessus le contenu principal.
 */
const Modal = ({ isOpen, onClose, title, subtitle, children, size = 'md' }) => {

  // Fermeture avec la touche Echap pour l'accessibilité
  useEffect(() => {
    const handleEsc = (e) => { if (e.key === 'Escape') onClose(); };
    if (isOpen) document.addEventListener('keydown', handleEsc);
    return () => document.removeEventListener('keydown', handleEsc);
  }, [isOpen, onClose]);

  // On empêche le scroll du body quand la modale est ouverte
  useEffect(() => {
    document.body.style.overflow = isOpen ? 'hidden' : 'unset';
    return () => { document.body.style.overflow = 'unset'; };
  }, [isOpen]);

  if (!isOpen) return null;

  // Largeur selon le paramètre size
  const sizeClass = {
    sm: 'max-w-sm',
    md: 'max-w-lg',
    lg: 'max-w-2xl',
  }[size];

  return (
    // Fond semi-transparent derrière la modale
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ backgroundColor: 'rgba(0,0,0,0.45)' }}
      onClick={onClose} // Clic en dehors ferme la modale
    >
      {/* Contenu de la modale — stopPropagation empêche la fermeture si on clique à l'intérieur */}
      <div
        className={`bg-white rounded-2xl shadow-2xl w-full ${sizeClass} max-h-[90vh] overflow-y-auto`}
        onClick={(e) => e.stopPropagation()}
      >
        {/* En-tête avec titre et bouton de fermeture */}
        <div className="flex items-start justify-between p-6 border-b border-gray-100">
          <div>
            <h2 className="text-lg font-bold text-gray-800">{title}</h2>
            {subtitle && <p className="text-sm text-gray-500 mt-0.5">{subtitle}</p>}
          </div>
          <button
            onClick={onClose}
            className="p-1.5 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X size={18} className="text-gray-500" />
          </button>
        </div>

        {/* Corps de la modale */}
        <div className="p-6">{children}</div>
      </div>
    </div>
  );
};

export default Modal;
