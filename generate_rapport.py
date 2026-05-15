#!/usr/bin/env python3
"""Génère le rapport Word complet pour MicrofinanceHub."""

import io
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
import matplotlib.patheffects as pe
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch
import numpy as np
from docx import Document
from docx.shared import Inches, Pt, RGBColor, Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_ALIGN_VERTICAL, WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn
from docx.oxml import OxmlElement
import datetime

# ─── Couleurs thème ──────────────────────────────────────────────────────────
VERT     = (0.024, 0.306, 0.243)   # #064e3b
VERT_L   = (0.064, 0.618, 0.486)   # emerald-600
BLEU     = (0.09,  0.44,  0.73)
ORANGE   = (0.85,  0.40,  0.10)
GRIS     = (0.94,  0.94,  0.94)
BLANC    = (1.0,   1.0,   1.0)
ROUGE    = (0.72,  0.11,  0.11)

def fig_to_stream(fig):
    buf = io.BytesIO()
    fig.savefig(buf, format='png', dpi=150, bbox_inches='tight')
    buf.seek(0)
    plt.close(fig)
    return buf

# ══════════════════════════════════════════════════════════════════════════════
# DIAGRAMME 1 : Architecture Globale Microservices
# ══════════════════════════════════════════════════════════════════════════════
def draw_architecture_globale():
    fig, ax = plt.subplots(figsize=(16, 10))
    ax.set_xlim(0, 16); ax.set_ylim(0, 10); ax.axis('off')
    fig.patch.set_facecolor('#f8f9fa')

    def box(x, y, w, h, label, sub='', color=VERT, lc=BLANC, fc=BLANC, fs=8):
        ax.add_patch(FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.05",
                     linewidth=1.5, edgecolor=color, facecolor=fc))
        ax.text(x+w/2, y+h/2 + (0.12 if sub else 0), label,
                ha='center', va='center', fontsize=fs, fontweight='bold', color=color)
        if sub:
            ax.text(x+w/2, y+h/2 - 0.15, sub,
                    ha='center', va='center', fontsize=6, color='#666666')

    def cloud(x, y, w, h, label, color=BLEU):
        ax.add_patch(FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.1",
                     linewidth=2, edgecolor=color, facecolor=(*color[:3], 0.12), linestyle='--'))
        ax.text(x+w/2, y+h+0.12, label, ha='center', va='bottom',
                fontsize=8, fontweight='bold', color=color)

    def arrow(x1, y1, x2, y2, col='#555555'):
        ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
            arrowprops=dict(arrowstyle='->', color=col, lw=1.5))

    # ── CLIENTS (gauche) ──────────────────────────────────────────────
    cloud(0.1, 1.0, 2.4, 7.5, 'CLIENTS', color=(0.4, 0.4, 0.4))
    box(0.3, 7.2, 2.0, 0.7, '🌐 Navigateur', 'React SPA', (0.4,0.4,0.4))
    box(0.3, 6.2, 2.0, 0.7, '📱 Application', 'Mobile / Tablette', (0.4,0.4,0.4))
    box(0.3, 5.2, 2.0, 0.7, '🔧 Systèmes tiers', 'CamPay / Twilio', (0.4,0.4,0.4))

    # ── FRONTEND ──────────────────────────────────────────────────────
    box(2.9, 6.4, 2.2, 1.2, '⚛ Frontend React', ':3000', VERT_L, fc=(*VERT_L, 0.06))

    # ── API GATEWAY ───────────────────────────────────────────────────
    box(5.4, 6.4, 2.2, 1.2, '🔀 API Gateway', ':8091', BLEU, fc=(*BLEU, 0.06), fs=8)

    # ── INFRA (Spring Cloud) ──────────────────────────────────────────
    cloud(5.2, 4.5, 2.6, 1.6, 'Spring Cloud', color=ORANGE)
    box(5.4, 4.7, 1.1, 1.1, '⚙ Config', ':8000', ORANGE, fc=(*ORANGE, 0.08), fs=7)
    box(6.6, 4.7, 1.1, 1.1, '📋 Eureka', ':8761', ORANGE, fc=(*ORANGE, 0.08), fs=7)

    # ── MICROSERVICES ─────────────────────────────────────────────────
    cloud(8.2, 1.0, 7.6, 8.5, 'Microservices Métier', color=VERT)

    services = [
        ('🔐 Auth',       ':8080', 8.4,  8.0),
        ('👤 Client',     ':8081', 9.8,  8.0),
        ('🏦 Account',    ':8082', 11.2, 8.0),
        ('💳 Loan',       ':8083', 12.6, 8.0),
        ('💰 Repayment',  ':8084', 14.0, 8.0),
        ('📊 Reporting',  ':8085', 8.4,  6.0),
        ('🏢 Agency',     ':8086', 9.8,  6.0),
        ('⚙ Config.MFI', ':8087', 11.2, 6.0),
        ('💸 Transaction',':8088', 12.6, 6.0),
        ('🔔 Notif.',     ':8089', 14.0, 6.0),
    ]
    for label, port, sx, sy in services:
        box(sx, sy, 1.3, 0.85, label, port, VERT, fc=(*VERT, 0.05), fs=6.5)

    # ── INFRA DATA ────────────────────────────────────────────────────
    cloud(8.2, 2.0, 7.6, 3.5, 'Infrastructure de Données', color=(0.5, 0.2, 0.6))
    box(8.4,  3.2, 1.5, 1.0, '🐘 PostgreSQL', ':5433', (0.5,0.2,0.6), fc=(0.5,0.2,0.6,0.06), fs=7)
    box(10.1, 3.2, 1.5, 1.0, '🐇 RabbitMQ', ':5672', (0.5,0.2,0.6), fc=(0.5,0.2,0.6,0.06), fs=7)
    box(11.8, 3.2, 1.5, 1.0, '⚡ Redis', ':6379', (0.5,0.2,0.6), fc=(0.5,0.2,0.6,0.06), fs=7)
    box(13.5, 3.2, 1.5, 1.0, '☁ Google\nDrive', 'API', (0.5,0.2,0.6), fc=(0.5,0.2,0.6,0.06), fs=7)

    # ── FLÈCHES ───────────────────────────────────────────────────────
    arrow(2.5, 7.0, 2.9, 7.0); arrow(2.5, 6.0, 2.9, 6.5)
    arrow(5.1, 7.0, 5.4, 7.0)
    arrow(7.6, 7.0, 8.2, 8.2)
    arrow(6.5, 6.4, 6.8, 6.2, ORANGE); arrow(6.8, 6.2, 8.2, 7.5)
    for sx, sy in [(s[2]+0.65, s[3]) for s in services]:
        ax.plot([sx], [sy-0.85], 'v', color=VERT, markersize=4, alpha=0.5)

    ax.set_title('Architecture Globale — MicrofinanceHub',
                 fontsize=14, fontweight='bold', color=VERT[0] and '#064e3b', pad=12)
    return fig_to_stream(fig)

# ══════════════════════════════════════════════════════════════════════════════
# DIAGRAMME 2 : Architecture Technique Stack
# ══════════════════════════════════════════════════════════════════════════════
def draw_stack_technique():
    fig, ax = plt.subplots(figsize=(14, 8))
    ax.set_xlim(0, 14); ax.set_ylim(0, 8); ax.axis('off')
    fig.patch.set_facecolor('#f8f9fa')

    cols = [
        ('FRONTEND',       1.0,  ['React 18', 'Axios / REST', 'Tailwind CSS', 'React Router', 'Lucide Icons', 'JWT (localStorage)'], VERT_L),
        ('BACKEND',        4.5,  ['Spring Boot 3.4.4', 'Spring Cloud 2024', 'Spring Security', 'Spring Data JPA', 'OpenFeign', 'JJWT 0.12.6'], BLEU),
        ('MESSAGERIE',     8.0,  ['RabbitMQ 3.13', 'AMQP Protocol', 'Échanges direct/fanout', 'Consumers async', 'Dead Letter Queue', 'Management UI'], (0.7, 0.3, 0.0)),
        ('INFRA / DEVOPS', 11.4, ['Docker + Compose', 'PostgreSQL 16', 'Redis 7', 'Eureka Discovery', 'Config Server', 'GitHub'], (0.4, 0.2, 0.6)),
    ]

    for title, x, items, col in cols:
        # En-tête
        ax.add_patch(FancyBboxPatch((x, 6.5), 2.8, 1.2, boxstyle="round,pad=0.08",
                     facecolor=col, edgecolor=col, linewidth=0))
        ax.text(x+1.4, 7.1, title, ha='center', va='center',
                fontsize=9, fontweight='bold', color='white')
        # Items
        for i, item in enumerate(items):
            y = 5.6 - i * 0.85
            fc = (*col, 0.08) if i % 2 == 0 else BLANC
            ax.add_patch(FancyBboxPatch((x, y), 2.8, 0.75, boxstyle="round,pad=0.04",
                         facecolor=fc, edgecolor=(*col, 0.3), linewidth=0.8))
            ax.text(x+1.4, y+0.375, item, ha='center', va='center',
                    fontsize=8, color='#222222')

    ax.set_title('Stack Technique — MicrofinanceHub',
                 fontsize=13, fontweight='bold', color='#064e3b', pad=10)
    return fig_to_stream(fig)

# ══════════════════════════════════════════════════════════════════════════════
# DIAGRAMME 3 : Diagramme de Cas d'Utilisation (Use Case)
# ══════════════════════════════════════════════════════════════════════════════
def draw_use_case():
    fig, ax = plt.subplots(figsize=(16, 11))
    ax.set_xlim(0, 16); ax.set_ylim(0, 11); ax.axis('off')
    fig.patch.set_facecolor('#f8f9fa')

    def actor(x, y, label, color='#333333'):
        # Tête
        ax.add_patch(plt.Circle((x, y+1.1), 0.25, color=color, zorder=5))
        # Corps
        ax.plot([x, x], [y+0.85, y+0.3], color=color, lw=2, zorder=5)
        ax.plot([x-0.35, x+0.35], [y+0.65, y+0.65], color=color, lw=2, zorder=5)
        ax.plot([x, x-0.3], [y+0.3, y-0.1], color=color, lw=2, zorder=5)
        ax.plot([x, x+0.3], [y+0.3, y-0.1], color=color, lw=2, zorder=5)
        ax.text(x, y-0.35, label, ha='center', va='top', fontsize=8,
                fontweight='bold', color=color)

    def uc(x, y, w, h, label):
        ax.add_patch(mpatches.Ellipse((x, y), w, h, color='white',
                     ec=VERT_L, lw=1.5, zorder=3))
        ax.text(x, y, label, ha='center', va='center', fontsize=7,
                color='#064e3b', zorder=4)

    def assoc(ax_x, ay, bx, by):
        ax.plot([ax_x, bx], [ay, by], 'k-', lw=0.8, zorder=2)

    # Frontière système
    ax.add_patch(FancyBboxPatch((3.0, 0.5), 12.5, 9.8, boxstyle="round,pad=0.1",
                 facecolor='#f0f4f8', edgecolor=BLEU, linewidth=2, zorder=0))
    ax.text(9.25, 10.5, 'Système MicrofinanceHub', ha='center', fontsize=10,
            fontweight='bold', color=BLEU[0] and '#1a70ba', style='italic')

    # Acteurs
    actor(1.2, 8.5, '👑 Admin',         '#7c3aed')
    actor(1.2, 5.5, '🏢 Directeur',     '#064e3b')
    actor(1.2, 2.5, '👔 Agent',         '#0369a1')
    actor(14.8,5.0, '💳 Client',         '#b45309')
    actor(14.8,2.0, '☁ CamPay\n(Ext.)', '#6b7280')

    # Use cases Admin
    cases_admin = [
        (5.5, 9.5, 3.2, 0.55, 'Gérer les utilisateurs'),
        (5.5, 8.7, 3.2, 0.55, 'Créer/Gérer les agences'),
        (5.5, 7.9, 3.2, 0.55, 'Assigner directeurs/agents'),
        (5.5, 7.1, 3.2, 0.55, 'Valider comptes & paiements'),
        (5.5, 6.3, 3.2, 0.55, 'Consulter rapports globaux'),
    ]
    for args in cases_admin:
        uc(*args)
        assoc(2.0, 8.8, args[0]-1.6, args[1])

    # Use cases Directeur
    cases_dir = [
        (9.2, 9.5, 3.2, 0.55, 'Gérer clients (agence)'),
        (9.2, 8.7, 3.2, 0.55, 'Gérer agents (agence)'),
        (9.2, 7.9, 3.2, 0.55, 'Valider comptes (agence)'),
        (9.2, 7.1, 3.2, 0.55, 'Effectuer transactions'),
        (9.2, 6.3, 3.2, 0.55, 'Rapports agence'),
    ]
    for args in cases_dir:
        uc(*args)
        assoc(2.0, 6.0, args[0]-1.6, args[1])

    # Use cases Agent
    cases_agent = [
        (5.5, 5.3, 3.2, 0.55, 'Créer clients'),
        (5.5, 4.5, 3.2, 0.55, 'Ouvrir comptes'),
        (5.5, 3.7, 3.2, 0.55, 'Opérations guichet'),
        (5.5, 2.9, 3.2, 0.55, 'Enregistrer paiements'),
        (5.5, 2.1, 3.2, 0.55, 'Demandes de prêt'),
    ]
    for args in cases_agent:
        uc(*args)
        assoc(2.0, 3.0, args[0]-1.6, args[1])

    # Use cases Client
    cases_client = [
        (9.2, 5.3, 3.2, 0.55, "S'inscrire / Se connecter"),
        (9.2, 4.5, 3.2, 0.55, 'Consulter compte & solde'),
        (9.2, 3.7, 3.2, 0.55, 'Effectuer paiements'),
        (9.2, 2.9, 3.2, 0.55, 'Suivi des prêts'),
        (9.2, 2.1, 3.2, 0.55, 'Paiement Mobile Money'),
    ]
    for args in cases_client:
        uc(*args)
        assoc(14.2, 5.2, args[0]+1.6, args[1])

    # CamPay
    assoc(14.2, 2.3, 10.8, 2.1)
    assoc(14.2, 2.3, 10.8, 2.9)

    ax.set_title('Diagramme de Cas d\'Utilisation — MicrofinanceHub',
                 fontsize=13, fontweight='bold', color='#064e3b', pad=10)
    return fig_to_stream(fig)

# ══════════════════════════════════════════════════════════════════════════════
# DIAGRAMME 4 : Diagramme de Séquence — Processus de Prêt
# ══════════════════════════════════════════════════════════════════════════════
def draw_sequence_pret():
    fig, ax = plt.subplots(figsize=(16, 10))
    ax.set_xlim(0, 16); ax.set_ylim(0, 10); ax.axis('off')
    fig.patch.set_facecolor('#f8f9fa')

    actors = [
        ('Agent/\nDirecteur', 1.5,  BLEU),
        ('Frontend\nReact',   3.5,  VERT_L),
        ('API\nGateway',      5.5,  ORANGE),
        ('Auth\nService',     7.5,  (0.5,0.2,0.6)),
        ('Loan\nService',     9.5,  VERT),
        ('Client\nService',   11.5, (0.6,0.4,0.0)),
        ('Notif.\nService',   13.5, ROUGE),
    ]

    # En-têtes et lignes de vie
    for label, x, col in actors:
        ax.add_patch(FancyBboxPatch((x-0.6, 8.9), 1.2, 0.9, boxstyle="round,pad=0.06",
                     facecolor=col, edgecolor=col, linewidth=0))
        ax.text(x, 9.35, label, ha='center', va='center',
                fontsize=7.5, fontweight='bold', color='white')
        ax.plot([x, x], [0.3, 8.9], color='#aaaaaa', lw=1, linestyle='--', zorder=1)

    def msg(y, x1, x2, label, ret=False, color='#333333', note=''):
        style = '->' if not ret else '<-'
        ax.annotate('', xy=(x2, y), xytext=(x1, y),
            arrowprops=dict(arrowstyle=style, color=color, lw=1.3))
        mx = (x1 + x2) / 2
        ax.text(mx, y + 0.12, label, ha='center', va='bottom', fontsize=7, color=color)
        if note:
            ax.text(mx, y - 0.15, f'({note})', ha='center', va='top',
                    fontsize=6, color='#888888', style='italic')

    def box_act(x, y, h=0.45, col=BLEU):
        ax.add_patch(FancyBboxPatch((x-0.12, y), 0.24, h,
                     boxstyle="square", facecolor=(*col, 0.3), edgecolor=col, lw=1))

    # Séquence
    msg(8.4, 1.5, 3.5, '1. Saisie demande de prêt', color=BLEU)
    msg(7.9, 3.5, 5.5, '2. POST /api/loans/apply', color=VERT_L)
    msg(7.4, 5.5, 7.5, '3. Valide JWT', color=ORANGE)
    msg(6.9, 7.5, 5.5, '✓ Token valide', ret=True, color=(0.5,0.2,0.6))
    msg(6.4, 5.5, 9.5, '4. Traite demande', color=ORANGE)
    msg(5.9, 9.5, 11.5, '5. Vérifie éligibilité client', color=VERT)
    msg(5.4, 11.5, 9.5, '✓ Profil client OK', ret=True, color=(0.6,0.4,0.0))
    msg(4.9, 9.5, 9.5, '6. Calcule amortissement', color=VERT)
    box_act(9.5, 4.55, 0.7, VERT)
    msg(4.4, 9.5, 9.5, '7. Enregistre demande', color=VERT)
    msg(3.9, 9.5, 13.5, '8. Notif. async (RabbitMQ)', color=VERT)
    msg(3.4, 9.5, 5.5, '✓ Demande créée', ret=True, color=VERT)
    msg(2.9, 5.5, 3.5, '✓ Réponse applicant', ret=True, color=ORANGE)
    msg(2.4, 3.5, 1.5, '✓ Confirmation à l\'agent', ret=True, color=VERT_L)
    msg(1.9, 13.5, 1.5, '9. Email notification', color=ROUGE)

    # Légende
    ax.text(0.3, 0.9, 'RabbitMQ = messagerie asynchrone entre services',
            fontsize=7, style='italic', color='#666666')
    ax.text(0.3, 0.6, 'JWT = JSON Web Token pour authentification stateless',
            fontsize=7, style='italic', color='#666666')

    ax.set_title('Diagramme de Séquence — Demande de Prêt',
                 fontsize=13, fontweight='bold', color='#064e3b', pad=10)
    return fig_to_stream(fig)

# ══════════════════════════════════════════════════════════════════════════════
# DIAGRAMME 5 : Diagramme de Séquence — Authentification
# ══════════════════════════════════════════════════════════════════════════════
def draw_sequence_auth():
    fig, ax = plt.subplots(figsize=(14, 8))
    ax.set_xlim(0, 14); ax.set_ylim(0, 8); ax.axis('off')
    fig.patch.set_facecolor('#f8f9fa')

    actors = [
        ('Utilisateur', 1.5,  (0.3,0.3,0.3)),
        ('Frontend\nReact', 3.8, VERT_L),
        ('Auth\nService', 6.5, (0.5,0.2,0.6)),
        ('PostgreSQL\n(Auth DB)', 9.5, (0.3,0.5,0.8)),
        ('Redis\n(Sessions)', 12.2, (0.7,0.1,0.1)),
    ]

    for label, x, col in actors:
        ax.add_patch(FancyBboxPatch((x-0.7, 7.0), 1.4, 0.8, boxstyle="round,pad=0.06",
                     facecolor=col, edgecolor=col))
        ax.text(x, 7.4, label, ha='center', va='center', fontsize=8,
                fontweight='bold', color='white')
        ax.plot([x, x], [0.5, 7.0], color='#bbbbbb', lw=1, linestyle='--')

    def msg(y, x1, x2, label, ret=False, color='#333333'):
        ax.annotate('', xy=(x2, y), xytext=(x1, y),
            arrowprops=dict(arrowstyle=('<-' if ret else '->'), color=color, lw=1.3))
        ax.text((x1+x2)/2, y+0.12, label, ha='center', va='bottom', fontsize=7.5, color=color)

    msg(6.5, 1.5, 3.8, 'Saisit email + mot de passe', color=(0.3,0.3,0.3))
    msg(6.0, 3.8, 6.5, 'POST /auth/login', color=VERT_L)
    msg(5.5, 6.5, 9.5, 'SELECT user WHERE email=?', color=(0.5,0.2,0.6))
    msg(5.0, 9.5, 6.5, 'UserDetails (hash, rôle)', ret=True, color=(0.3,0.5,0.8))
    msg(4.5, 6.5, 6.5, 'BCrypt.verify(password)', color=(0.5,0.2,0.6))
    ax.add_patch(FancyBboxPatch((6.0, 4.1), 1.0, 0.7, facecolor=(*VERT_L, 0.3),
                 edgecolor=VERT_L, linewidth=1))
    ax.text(6.5, 4.45, 'Génère\nJWT', ha='center', va='center', fontsize=7)
    msg(3.9, 6.5, 12.2, 'Stocke session (TTL)', color=(0.5,0.2,0.6))
    msg(3.4, 12.2, 6.5, '✓ Session créée', ret=True, color=(0.7,0.1,0.1))
    msg(2.9, 6.5, 3.8, '{ access_token, role, agencyId }', ret=True, color=(0.5,0.2,0.6))
    msg(2.4, 3.8, 1.5, 'Stocke JWT → localStorage', ret=True, color=VERT_L)
    msg(1.9, 3.8, 1.5, 'Redirige vers /dashboard', ret=True, color=VERT_L)

    ax.set_title('Diagramme de Séquence — Authentification JWT',
                 fontsize=13, fontweight='bold', color='#064e3b', pad=10)
    return fig_to_stream(fig)

# ══════════════════════════════════════════════════════════════════════════════
# DIAGRAMME 6 : Diagramme de Classes simplifié
# ══════════════════════════════════════════════════════════════════════════════
def draw_classes():
    fig, ax = plt.subplots(figsize=(16, 11))
    ax.set_xlim(0, 16); ax.set_ylim(0, 11); ax.axis('off')
    fig.patch.set_facecolor('#f8f9fa')

    def uml_class(x, y, name, attrs, methods=None, color=VERT, w=3.0):
        h_attr = max(len(attrs) * 0.32 + 0.1, 0.5)
        h_meth = max(len(methods or []) * 0.32 + 0.1, 0.4) if methods else 0
        total_h = 0.5 + h_attr + h_meth

        # Classe
        ax.add_patch(FancyBboxPatch((x, y - total_h), w, total_h,
                     boxstyle="square", facecolor='white', edgecolor=color, lw=1.5))
        # En-tête
        ax.add_patch(FancyBboxPatch((x, y - 0.5), w, 0.5,
                     boxstyle="square", facecolor=color, edgecolor=color, lw=1.5))
        ax.text(x + w/2, y - 0.25, name, ha='center', va='center',
                fontsize=8.5, fontweight='bold', color='white')
        # Séparateur
        ax.plot([x, x+w], [y - 0.5, y - 0.5], color=color, lw=1)
        # Attributs
        for i, attr in enumerate(attrs):
            ax.text(x + 0.1, y - 0.65 - i * 0.32, f'• {attr}',
                    va='center', fontsize=6.5, color='#333333')
        # Séparateur méthodes
        if methods:
            sep_y = y - 0.5 - h_attr
            ax.plot([x, x+w], [sep_y, sep_y], color=(*color, 0.4), lw=0.8, linestyle=':')
            for i, meth in enumerate(methods):
                ax.text(x + 0.1, sep_y - 0.2 - i * 0.32, f'+ {meth}',
                        va='center', fontsize=6.5, color='#666666', style='italic')
        return total_h

    def relation(x1, y1, x2, y2, label='', style='->', color='#777777'):
        ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
            arrowprops=dict(arrowstyle=style, color=color, lw=1.2))
        if label:
            ax.text((x1+x2)/2, (y1+y2)/2 + 0.1, label,
                    ha='center', fontsize=6.5, color=color)

    # Classes principales
    uml_class(0.2, 11.0, 'User (Auth)',
              ['- id: String', '- email: String', '- password: String (hash)',
               '- userRoleType: Enum', '- agencyId: String', '- agencyCode: String'],
              ['+ login()', '+ generateToken()'], color=(0.5,0.2,0.6), w=3.2)

    uml_class(0.2, 7.2, 'Client',
              ['- id: UUID', '- email: String', '- firstName: String',
               '- lastName: String', '- agencyId: String', '- creditScore: Integer',
               '- status: ClientStatus'],
              ['+ getClientsByAgent()', '+ updateStatus()'], color=BLEU, w=3.2)

    uml_class(0.2, 3.5, 'Agency',
              ['- id: String', '- code: String', '- name: String',
               '- directorId: String', '- directorEmail: String'],
              ['+ getAgents()', '+ toggleAgent()'], color=VERT, w=3.2)

    uml_class(4.3, 11.0, 'Compte',
              ['- id: Long', '- clientId: String', '- numeroCompte: String',
               '- typeCompte: TypeCompte', '- solde: BigDecimal',
               '- statut: StatutCompte'],
              ['+ crediter()', '+ debiter()', '+ changerStatut()'], color=ORANGE, w=3.2)

    uml_class(4.3, 6.5, 'Loan',
              ['- id: String', '- clientId: String', '- amount: BigDecimal',
               '- interestRate: BigDecimal', '- status: LoanStatus',
               '- monthlyPayment: BigDecimal'],
              ['+ approveLoan()', '+ getAmortization()'], color=VERT, w=3.2)

    uml_class(4.3, 2.8, 'AgentAssignment',
              ['- id: String', '- agentId: String', '- agentEmail: String',
               '- agencyId: String', '- active: boolean'],
              ['+ toggleStatus()'], color=(0.5,0.2,0.6), w=3.2)

    uml_class(8.6, 11.0, 'Transaction',
              ['- id: String', '- compteId: Long', '- type: TypeTransaction',
               '- montant: BigDecimal', '- statut: StatutTransaction'],
              ['+ effectuerDepot()', '+ effectuerRetrait()'], color=BLEU, w=3.2)

    uml_class(8.6, 6.8, 'Payment (Repayment)',
              ['- id: String', '- loanId: String', '- clientId: String',
               '- amount: BigDecimal', '- status: PaymentStatus',
               '- paymentMethod: Enum'],
              ['+ validatePayment()'], color=ROUGE, w=3.2)

    uml_class(8.6, 2.8, 'Notification',
              ['- id: String', '- destinataire: String', '- type: TypeNotif.',
               '- canal: EMAIL/SMS', '- statut: ENVOYE/ECHEC'],
              ['+ envoyer()'], color=(0.7,0.3,0.0), w=3.2)

    uml_class(12.6, 11.0, 'Document (KYC)',
              ['- id: String', '- clientId: String', '- type: DocumentType',
               '- driveFileId: String', '- verificationStatus: Enum'],
              ['+ upload()', '+ verify()'], color=(0.3,0.5,0.2), w=3.2)

    uml_class(12.6, 7.0, 'LoanSchedule',
              ['- id: String', '- loanId: String', '- installmentNumber: Int',
               '- dueAmount: BigDecimal', '- paid: boolean'],
              [], color=VERT, w=3.2)

    uml_class(12.6, 4.0, 'Report',
              ['- id: String', '- agencyId: String', '- type: ReportType',
               '- generatedAt: LocalDateTime'],
              ['+ generate()'], color=(0.4,0.4,0.4), w=3.2)

    # Relations
    relation(3.4, 8.5, 4.3, 9.5, '1..*', color=BLEU)
    relation(3.4, 7.0, 3.4, 5.5, '', color=VERT)
    relation(3.4, 6.0, 4.3, 5.2, '1..*', color=VERT)
    relation(7.5, 8.5, 8.6, 8.5, '', color=ORANGE)
    relation(7.5, 5.0, 8.6, 5.0, '1..*', color=VERT)
    relation(1.8, 7.2, 1.8, 6.8, '', color=BLEU)

    ax.set_title('Diagramme de Classes — Entités Principales',
                 fontsize=13, fontweight='bold', color='#064e3b', pad=10)
    return fig_to_stream(fig)

# ══════════════════════════════════════════════════════════════════════════════
# DIAGRAMME 7 : Communication inter-services (RabbitMQ)
# ══════════════════════════════════════════════════════════════════════════════
def draw_rabbitmq():
    fig, ax = plt.subplots(figsize=(14, 8))
    ax.set_xlim(0, 14); ax.set_ylim(0, 8); ax.axis('off')
    fig.patch.set_facecolor('#f8f9fa')

    def svc_box(x, y, label, color=VERT, w=2.0, h=0.8):
        ax.add_patch(FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.07",
                     facecolor=(*color, 0.12), edgecolor=color, lw=1.5))
        ax.text(x+w/2, y+h/2, label, ha='center', va='center',
                fontsize=8, fontweight='bold', color=color)
        return x+w/2, y+h

    def queue(x, y, label, color=(0.8,0.4,0.0)):
        ax.add_patch(FancyBboxPatch((x, y), 1.6, 0.55, boxstyle="round,pad=0.05",
                     facecolor=(*color, 0.15), edgecolor=color, lw=1.2, linestyle='--'))
        ax.text(x+0.8, y+0.275, label, ha='center', va='center', fontsize=6.5, color=color)

    def publish(x1, y1, x2, y2, label='publish', color=(0.8,0.4,0.0)):
        ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
            arrowprops=dict(arrowstyle='->', color=color, lw=1.3))
        ax.text((x1+x2)/2 + 0.1, (y1+y2)/2 + 0.1, label,
                ha='center', fontsize=6.5, color=color, style='italic')

    # RabbitMQ Exchange (centre)
    ax.add_patch(FancyBboxPatch((5.5, 3.3), 3.0, 1.4, boxstyle="round,pad=0.1",
                 facecolor='#fff7ed', edgecolor=(0.8,0.4,0.0), lw=2))
    ax.text(7.0, 4.3, '🐇 RabbitMQ', ha='center', fontsize=10, fontweight='bold',
            color=(0.8,0.4,0.0))
    ax.text(7.0, 3.8, 'Exchange : mfh.events', ha='center', fontsize=8, color='#666666')

    # Queues
    queues_pos = [
        (5.6, 2.5, 'loan.created'),
        (7.3, 2.5, 'payment.pending'),
        (5.6, 1.7, 'account.opened'),
        (7.3, 1.7, 'mobile.confirmed'),
    ]
    for qx, qy, ql in queues_pos:
        queue(qx, qy, ql)

    # Publishers (gauche)
    publishers = [
        (0.5, 6.5, 'Auth Service', (0.5,0.2,0.6)),
        (0.5, 5.2, 'Loan Service', VERT),
        (0.5, 3.9, 'Account Service', ORANGE),
        (0.5, 2.6, 'Transaction Svc', BLEU),
    ]
    for px, py, pl, pc in publishers:
        svc_box(px, py, pl, pc)
        publish(px+2.0, py+0.4, 5.5, 3.7)

    # Consumers (droite)
    consumers = [
        (11.5, 6.5, 'Notification Svc', ROUGE),
        (11.5, 5.2, 'Repayment Svc', (0.5,0.2,0.6)),
        (11.5, 3.9, 'Loan Service', VERT),
        (11.5, 2.6, 'Client Service', BLEU),
    ]
    for cx, cy, cl, cc in consumers:
        svc_box(cx, cy, cl, cc)
        publish(8.5, 3.7, cx, cy+0.4, 'consume', cc)

    ax.set_title('Communication Asynchrone — RabbitMQ Message Broker',
                 fontsize=13, fontweight='bold', color='#064e3b', pad=10)
    return fig_to_stream(fig)

# ══════════════════════════════════════════════════════════════════════════════
# DIAGRAMME 8 : Flux de Déploiement Docker
# ══════════════════════════════════════════════════════════════════════════════
def draw_deployment():
    fig, ax = plt.subplots(figsize=(14, 9))
    ax.set_xlim(0, 14); ax.set_ylim(0, 9); ax.axis('off')
    fig.patch.set_facecolor('#f8f9fa')

    def container(x, y, w, h, name, port, color=VERT, icon='🐳'):
        ax.add_patch(FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.06",
                     facecolor=(*color, 0.1), edgecolor=color, lw=1.5))
        ax.text(x+w/2, y+h*0.75, f'{icon} {name}', ha='center', va='center',
                fontsize=7.5, fontweight='bold', color=color)
        ax.text(x+w/2, y+h*0.3, port, ha='center', va='center',
                fontsize=6.5, color='#555555')

    def net_arrow(x1, y1, x2, y2):
        ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
            arrowprops=dict(arrowstyle='<->', color='#888888', lw=1))

    # Hôte Docker
    ax.add_patch(FancyBboxPatch((0.2, 0.3), 13.6, 8.4, boxstyle="round,pad=0.1",
                 facecolor='#f0f4ff', edgecolor='#3b82f6', lw=2, linestyle='--'))
    ax.text(7.0, 8.9, '🖥  Machine Hôte Linux — Docker Engine',
            ha='center', fontsize=11, fontweight='bold', color='#3b82f6')

    # Réseau Docker interne
    ax.add_patch(FancyBboxPatch((0.5, 0.5), 13.0, 7.8, boxstyle="round,pad=0.08",
                 facecolor='#f8fff8', edgecolor=VERT_L, lw=1.5, linestyle=':'))
    ax.text(7.0, 8.4, 'Réseau Docker : mfh-network (bridge)',
            ha='center', fontsize=9, color=VERT_L, style='italic')

    # Infrastructure
    container(0.8, 0.8, 2.2, 1.0, 'PostgreSQL', '5433:5432', (0.3,0.5,0.8), '🐘')
    container(3.2, 0.8, 2.2, 1.0, 'RabbitMQ', '5672+15672', (0.8,0.4,0.0), '🐇')
    container(5.6, 0.8, 2.2, 1.0, 'Redis', '6379:6379', (0.7,0.1,0.1), '⚡')
    container(8.0, 0.8, 2.2, 1.0, 'Config Svc', '8000:8000', ORANGE, '⚙')
    container(10.4, 0.8, 2.8, 1.0, 'Eureka Registry', '8761:8761', ORANGE, '📋')

    # Services
    svcs_row1 = [
        ('Auth Svc', ':8080', 0.8, 2.2),
        ('Client Svc', ':8081', 3.2, 2.2),
        ('Account Svc', ':8082', 5.6, 2.2),
        ('Loan Svc', ':8083', 8.0, 2.2),
        ('Repayment', ':8084', 10.4, 2.2),
    ]
    for name, port, x, y in svcs_row1:
        container(x, y, 2.2, 1.0, name, port, VERT)

    svcs_row2 = [
        ('Reporting', ':8085', 0.8, 3.6),
        ('Agency Svc', ':8086', 3.2, 3.6),
        ('Config.MFI', ':8087', 5.6, 3.6),
        ('Transaction', ':8088', 8.0, 3.6),
        ('Notif. Svc', ':8089', 10.4, 3.6),
    ]
    for name, port, x, y in svcs_row2:
        container(x, y, 2.2, 1.0, name, port, VERT)

    # Gateway & Frontend
    container(2.0, 5.2, 4.0, 1.2, 'API Gateway', ':8091:8091', BLEU, '🔀')
    container(8.0, 5.2, 4.0, 1.2, 'Frontend React', '3000:80 (Nginx)', VERT_L, '⚛')

    # Volumes
    container(0.8, 6.8, 3.5, 0.9, 'Volumes persistants', 'postgres_data\nrabbitmq_data\nredis_data', (0.4,0.4,0.4), '💾')

    ax.set_title('Diagramme de Déploiement Docker Compose — MicrofinanceHub',
                 fontsize=13, fontweight='bold', color='#064e3b', pad=12)
    return fig_to_stream(fig)

# ══════════════════════════════════════════════════════════════════════════════
# GÉNÉRATION DU DOCUMENT WORD
# ══════════════════════════════════════════════════════════════════════════════
def add_heading(doc, text, level, color_hex='#064e3b'):
    h = doc.add_heading(text, level=level)
    for run in h.runs:
        run.font.color.rgb = RGBColor.from_string(color_hex.lstrip('#'))
    h.paragraph_format.space_before = Pt(12)
    h.paragraph_format.space_after = Pt(6)
    return h

def add_para(doc, text, bold=False, size=11):
    p = doc.add_paragraph()
    run = p.add_run(text)
    run.bold = bold
    run.font.size = Pt(size)
    p.paragraph_format.space_after = Pt(6)
    return p

def add_bullet(doc, text, level=0):
    p = doc.add_paragraph(style='List Bullet')
    p.add_run(text).font.size = Pt(10.5)
    p.paragraph_format.space_after = Pt(3)

def add_image(doc, stream, width_inches=6.5, caption=''):
    doc.add_picture(stream, width=Inches(width_inches))
    if caption:
        p = doc.add_paragraph(caption)
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.runs[0].font.size = Pt(9)
        p.runs[0].font.italic = True
        p.runs[0].font.color.rgb = RGBColor(0x44, 0x44, 0x44)
    doc.add_paragraph()

def add_table(doc, headers, rows, col_widths=None):
    table = doc.add_table(rows=1+len(rows), cols=len(headers))
    table.style = 'Light Shading Accent 6'
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    # En-têtes
    for i, h in enumerate(headers):
        cell = table.cell(0, i)
        cell.text = h
        cell.paragraphs[0].runs[0].bold = True
        cell.paragraphs[0].runs[0].font.size = Pt(9.5)
    # Données
    for r, row in enumerate(rows):
        for c, val in enumerate(row):
            cell = table.cell(r+1, c)
            cell.text = str(val)
            cell.paragraphs[0].runs[0].font.size = Pt(9)
    doc.add_paragraph()

def make_report():
    doc = Document()

    # ── Styles de base ──────────────────────────────────────────────────────
    style = doc.styles['Normal']
    style.font.name = 'Calibri'
    style.font.size = Pt(11)

    # Marges
    for section in doc.sections:
        section.top_margin    = Cm(2.5)
        section.bottom_margin = Cm(2.5)
        section.left_margin   = Cm(2.8)
        section.right_margin  = Cm(2.8)

    # ══════════════════════════════════════════════════════════════════════
    # PAGE DE GARDE
    # ══════════════════════════════════════════════════════════════════════
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(40)
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run('RAPPORT DE PROJET')
    run.bold = True
    run.font.size = Pt(28)
    run.font.color.rgb = RGBColor(0x06, 0x4e, 0x3b)

    p2 = doc.add_paragraph()
    p2.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r2 = p2.add_run('MicrofinanceHub')
    r2.bold = True
    r2.font.size = Pt(22)
    r2.font.color.rgb = RGBColor(0x06, 0x4e, 0x3b)

    doc.add_paragraph()
    p3 = doc.add_paragraph()
    p3.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p3.add_run('Plateforme Microservices de Gestion de Microfinance').font.size = Pt(14)

    doc.add_paragraph()
    doc.add_paragraph()
    p4 = doc.add_paragraph()
    p4.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p4.add_run(f'Date : {datetime.date.today().strftime("%d %B %Y")}').font.size = Pt(12)

    doc.add_paragraph()
    p5 = doc.add_paragraph()
    p5.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p5.add_run('Architecture : Microservices Spring Boot | Frontend : React').font.size = Pt(11)

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════════
    # TABLE DES MATIÈRES (manuelle)
    # ══════════════════════════════════════════════════════════════════════
    add_heading(doc, 'Table des Matières', 1)
    toc = [
        ('1.', 'Introduction et Contexte'),
        ('2.', 'Présentation du Projet'),
        ('3.', 'Architecture Globale'),
        ('4.', 'Architecture Technique — Stack'),
        ('5.', 'Diagrammes UML'),
        ('    5.1.', 'Cas d\'Utilisation'),
        ('    5.2.', 'Séquence — Authentification JWT'),
        ('    5.3.', 'Séquence — Demande de Prêt'),
        ('    5.4.', 'Diagramme de Classes'),
        ('6.', 'Communication Inter-Services (RabbitMQ)'),
        ('7.', 'Modèle de Données'),
        ('8.', 'Sécurité et Authentification'),
        ('9.', 'Services Développés'),
        ('10.', 'Interface Utilisateur'),
        ('11.', 'Déploiement et Infrastructure'),
        ('12.', 'Conclusion et Perspectives'),
    ]
    for num, title in toc:
        p = doc.add_paragraph()
        p.add_run(f'{num}  {title}').font.size = Pt(11)
        p.paragraph_format.space_after = Pt(2)

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════════
    # 1. INTRODUCTION
    # ══════════════════════════════════════════════════════════════════════
    add_heading(doc, '1. Introduction et Contexte', 1)
    add_para(doc,
        "Le secteur de la microfinance joue un rôle crucial dans l'inclusion financière, "
        "notamment dans les économies émergentes. Les institutions de microfinance (IMF) gèrent "
        "quotidiennement une grande diversité d'opérations : gestion des clients, des comptes "
        "bancaires, des prêts, des remboursements et des paiements mobiles. Ce volume d'opérations "
        "nécessite un système d'information robuste, scalable et sécurisé.")
    add_para(doc,
        "C'est dans ce contexte que s'inscrit MicrofinanceHub : une plateforme complète de gestion "
        "de microfinance construite sur une architecture microservices moderne. Le projet répond aux "
        "exigences de performance, de maintenabilité et d'évolutivité des systèmes d'information "
        "contemporains.")

    add_heading(doc, 'Objectifs du Projet', 2)
    bullets_obj = [
        "Digitaliser l'ensemble du cycle de vie d'une institution de microfinance",
        "Garantir la séparation des responsabilités via une architecture microservices",
        "Assurer une sécurité robuste basée sur JWT et RBAC (4 rôles : Admin, Directeur, Agent, Client)",
        "Intégrer les paiements mobiles (Mobile Money via CamPay)",
        "Offrir des tableaux de bord adaptés à chaque rôle utilisateur",
        "Déployer l'ensemble via Docker Compose pour une portabilité maximale",
    ]
    for b in bullets_obj:
        add_bullet(doc, b)

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════════
    # 2. PRÉSENTATION DU PROJET
    # ══════════════════════════════════════════════════════════════════════
    add_heading(doc, '2. Présentation du Projet', 1)
    add_para(doc,
        "MicrofinanceHub est une application web full-stack composée de 12 microservices "
        "backend et d'un frontend React. Elle couvre l'ensemble du périmètre fonctionnel "
        "d'une IMF moderne : de la création du profil client jusqu'au reporting financier avancé.")

    add_heading(doc, '2.1 Rôles Utilisateurs', 2)
    add_table(doc,
        ['Rôle', 'Responsabilités', 'Accès'],
        [
            ('👑 Administrateur', 'Gestion globale : utilisateurs, agences, validations',
             'Toutes sections, toutes agences'),
            ('🏢 Directeur d\'Agence', 'Supervision agence : clients, agents, validations, prêts',
             'Données de son agence uniquement'),
            ('👔 Agent de Terrain', 'Opérations quotidiennes : clients, comptes, guichet',
             'Ses clients assignés'),
            ('💳 Client', 'Consultation, paiements, suivi des prêts',
             'Son profil uniquement'),
        ]
    )

    add_heading(doc, '2.2 Fonctionnalités Clés', 2)
    features = [
        "Gestion complète des clients (KYC, documents, score crédit)",
        "Ouverture et gestion de comptes bancaires (Courant, Épargne, Micro-épargne)",
        "Cycle de prêt complet : demande → éligibilité → approbation → décaissement → remboursement",
        "Calcul automatique du plan d'amortissement",
        "Paiements Multi-canal : Espèces, Mobile Money (CamPay), Virement, Chèque",
        "Notifications temps réel : Email (Thymeleaf) + SMS (Twilio)",
        "Transactions financières : Dépôt, Retrait, Virement inter-comptes",
        "Rapports et tableaux de bord par agence et globaux",
        "Gestion multi-agences avec hiérarchie Directeur/Agent",
        "Stockage documents KYC sur Google Drive",
    ]
    for f in features:
        add_bullet(doc, f)

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════════
    # 3. ARCHITECTURE GLOBALE
    # ══════════════════════════════════════════════════════════════════════
    add_heading(doc, '3. Architecture Globale', 1)
    add_para(doc,
        "Le système adopte une architecture microservices où chaque service est indépendant, "
        "déployable séparément et responsable d'un domaine métier précis. Les services "
        "communiquent via REST (synchrone) et RabbitMQ (asynchrone).")

    print("  → Génération Architecture Globale...")
    img = draw_architecture_globale()
    add_image(doc, img, 6.5, 'Figure 1 — Architecture Globale MicrofinanceHub')

    add_heading(doc, '3.1 Inventaire des Services', 2)
    add_table(doc,
        ['Service', 'Port', 'Rôle', 'Base de données'],
        [
            ('config-service',       '8000', 'Serveur de configuration centralisé (Spring Cloud Config)', '—'),
            ('registry-service',     '8761', 'Service Discovery (Eureka)', '—'),
            ('api-gateway',          '8091', 'Point d\'entrée unique, routage, CORS', '—'),
            ('auth-service',         '8080', 'Authentification, JWT, sessions Redis', 'microfinancehub_auth'),
            ('client-service',       '8081', 'Profils clients, documents KYC, score crédit', 'microfinancehub_client'),
            ('account-service',      '8082', 'Comptes bancaires, opérations', 'microfinancehub_account'),
            ('loan-service',         '8083', 'Prêts, amortissement, éligibilité', 'microfinancehub_loan'),
            ('repayment-service',    '8084', 'Remboursements, validation paiements', 'microfinancehub_repayment'),
            ('reporting-service',    '8085', 'Rapports Excel/PDF, statistiques', 'microfinancehub_reporting'),
            ('agency-service',       '8086', 'Agences, agents, directeurs', 'microfinancehub_agency'),
            ('configuration-service','8087', 'Paramétrage métier MFI', 'microfinancehub_configuration'),
            ('transaction-service',  '8088', 'Dépôts, retraits, virements, CamPay', 'microfinancehub_transaction'),
            ('notification-service', '8089', 'Emails Thymeleaf, SMS Twilio, WebSocket', 'microfinancehub_notification'),
            ('app-frontend',         '3000', 'SPA React (Nginx en production)', '—'),
        ]
    )

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════════
    # 4. STACK TECHNIQUE
    # ══════════════════════════════════════════════════════════════════════
    add_heading(doc, '4. Architecture Technique — Stack Technologique', 1)

    print("  → Génération Stack Technique...")
    img2 = draw_stack_technique()
    add_image(doc, img2, 6.2, 'Figure 2 — Stack Technologique par couche')

    add_heading(doc, '4.1 Technologies Backend', 2)
    add_table(doc,
        ['Technologie', 'Version', 'Usage'],
        [
            ('Spring Boot',        '3.4.4', 'Framework principal de chaque microservice'),
            ('Spring Cloud',       '2024.0.0', 'Config Server, Eureka, OpenFeign, Gateway'),
            ('Spring Security',    '6.x',   'Authentification, autorisation par rôle (RBAC)'),
            ('Spring Data JPA',    '3.x',   'ORM avec Hibernate, accès base de données'),
            ('JJWT',               '0.12.6', 'Génération et validation des tokens JWT'),
            ('OpenFeign',          '4.x',   'Appels REST inter-services déclaratifs'),
            ('RabbitMQ (AMQP)',    '3.13',  'Messagerie asynchrone entre services'),
            ('PostgreSQL',         '16',    'Base de données relationnelle (une par service)'),
            ('Redis',              '7',     'Cache sessions, rate limiting'),
            ('Lombok',             '1.18',  'Réduction du code boilerplate Java'),
            ('Thymeleaf',          '3.x',   'Templates HTML pour emails'),
        ]
    )

    add_heading(doc, '4.2 Technologies Frontend', 2)
    add_table(doc,
        ['Technologie', 'Usage'],
        [
            ('React 18',        'Framework SPA, gestion état local et hooks'),
            ('Axios',           'Client HTTP pour appels REST, intercepteurs JWT'),
            ('Tailwind CSS',    'Framework CSS utilitaire, design system'),
            ('React Router v6', 'Navigation SPA, routes protégées par rôle'),
            ('Lucide React',    'Bibliothèque d\'icônes SVG'),
            ('LocalStorage',    'Persistance session (token JWT + profil utilisateur)'),
        ]
    )

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════════
    # 5. DIAGRAMMES UML
    # ══════════════════════════════════════════════════════════════════════
    add_heading(doc, '5. Diagrammes UML', 1)

    add_heading(doc, '5.1 Diagramme de Cas d\'Utilisation', 2)
    add_para(doc,
        "Le diagramme suivant représente les interactions entre les 4 acteurs principaux "
        "du système et les fonctionnalités auxquelles ils ont accès.")

    print("  → Génération Diagramme Use Case...")
    img3 = draw_use_case()
    add_image(doc, img3, 6.5, 'Figure 3 — Diagramme de Cas d\'Utilisation')

    doc.add_page_break()

    add_heading(doc, '5.2 Diagramme de Séquence — Authentification JWT', 2)
    add_para(doc,
        "Ce diagramme illustre le flux complet d'authentification : depuis la saisie des "
        "identifiants jusqu'au stockage du JWT et à la redirection vers le dashboard approprié. "
        "Le token contient : l'email, le rôle, les noms et l'agencyId de l'utilisateur.")

    print("  → Génération Séquence Authentification...")
    img4 = draw_sequence_auth()
    add_image(doc, img4, 6.2, 'Figure 4 — Séquence Authentification JWT')

    add_heading(doc, '5.3 Diagramme de Séquence — Demande de Prêt', 2)
    add_para(doc,
        "Ce diagramme montre le flux complet de traitement d'une demande de prêt, "
        "incluant la vérification JWT, la consultation du profil client, le calcul "
        "de l'amortissement et la notification asynchrone via RabbitMQ.")

    print("  → Génération Séquence Prêt...")
    img5 = draw_sequence_pret()
    add_image(doc, img5, 6.5, 'Figure 5 — Séquence Demande de Prêt')

    doc.add_page_break()

    add_heading(doc, '5.4 Diagramme de Classes — Entités Principales', 2)
    add_para(doc,
        "Ce diagramme présente les entités métier principales et leurs relations. "
        "Chaque entité est gérée par son microservice dédié et possède sa propre "
        "base de données PostgreSQL (pattern Database per Service).")

    print("  → Génération Diagramme de Classes...")
    img6 = draw_classes()
    add_image(doc, img6, 6.5, 'Figure 6 — Diagramme de Classes Principales')

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════════
    # 6. COMMUNICATION INTER-SERVICES
    # ══════════════════════════════════════════════════════════════════════
    add_heading(doc, '6. Communication Inter-Services (RabbitMQ)', 1)
    add_para(doc,
        "La communication asynchrone via RabbitMQ permet de découpler les services et "
        "d'assurer la résilience du système. Lorsqu'un service publie un événement, "
        "il n'attend pas la réponse des consommateurs, ce qui améliore les performances "
        "et la disponibilité globale.")

    print("  → Génération Diagramme RabbitMQ...")
    img7 = draw_rabbitmq()
    add_image(doc, img7, 6.2, 'Figure 7 — Communication Asynchrone via RabbitMQ')

    add_heading(doc, '6.1 Événements Principaux', 2)
    add_table(doc,
        ['Événement', 'Producteur', 'Consommateur', 'Déclencheur'],
        [
            ('user.login',           'Auth Service',        'Client Service',     'Connexion utilisateur'),
            ('loan.created',         'Loan Service',        'Notification Svc',   'Nouvelle demande prêt'),
            ('loan.approved',        'Loan Service',        'Notification Svc',   'Prêt approuvé'),
            ('payment.pending',      'Repayment Service',   'Notification Svc',   'Paiement en attente'),
            ('payment.validated',    'Repayment Service',   'Loan Service',       'Paiement validé → MAJ échéance'),
            ('account.opened',       'Account Service',     'Notification Svc',   'Compte ouvert'),
            ('mobile.confirmed',     'Transaction Svc',     'Repayment Service',  'CamPay webhook reçu'),
            ('payment.received',     'Repayment Service',   'Notification Svc',   'Paiement CamPay confirmé'),
        ]
    )

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════════
    # 7. MODÈLE DE DONNÉES
    # ══════════════════════════════════════════════════════════════════════
    add_heading(doc, '7. Modèle de Données', 1)
    add_para(doc,
        "Le projet applique le pattern «Database per Service» : chaque microservice "
        "possède sa propre base de données PostgreSQL, garantissant l'indépendance "
        "et l'isolation des données. Les jointures inter-services se font via des "
        "appels REST (Feign) et non par des clés étrangères croisées.")

    add_heading(doc, '7.1 Bases de Données', 2)
    add_table(doc,
        ['Base de données', 'Service', 'Tables principales', 'Port hôte'],
        [
            ('microfinancehub_auth',          'auth-service',          'users, roles, refresh_tokens, user_sessions', '5433'),
            ('microfinancehub_client',        'client-service',        'clients, documents',                          '5433'),
            ('microfinancehub_account',       'account-service',       'compte, compte_events',                       '5433'),
            ('microfinancehub_loan',          'loan-service',          'loans, loan_applications, loan_schedules',    '5433'),
            ('microfinancehub_repayment',     'repayment-service',     'payments, repayments, schedules',             '5433'),
            ('microfinancehub_reporting',     'reporting-service',     'reports, report_data',                        '5433'),
            ('microfinancehub_agency',        'agency-service',        'agencies, agent_assignments, agency_stats',   '5433'),
            ('microfinancehub_configuration', 'configuration-service', 'mfi_configs, loan_products',                  '5433'),
            ('microfinancehub_transaction',   'transaction-service',   'transactions, campay_webhooks',               '5433'),
            ('microfinancehub_notification',  'notification-service',  'notifications, notification_logs',            '5433'),
        ]
    )

    add_heading(doc, '7.2 Enums et Types Métier', 2)
    add_table(doc,
        ['Enum', 'Service', 'Valeurs'],
        [
            ('UserRoleType',    'Auth',        'ADMIN, DIRECTEUR_AGENCE, AGENT, CLIENT'),
            ('ClientStatus',    'Client',      'ACTIVE, INACTIVE, PENDING, SUSPENDED'),
            ('StatutCompte',    'Account',     'ACTIF, INACTIF, SUSPENDU, BLOQUE, FERME, EN_ATTENTE_VALIDATION, REJETE'),
            ('TypeCompte',      'Account',     'COURANT, EPARGNE, MICRO_EPARGNE, DEPOT_A_TERME, CREDIT'),
            ('LoanStatus',      'Loan',        'PENDING, APPROVED, REJECTED, ACTIVE, COMPLETED, DEFAULT'),
            ('PaymentStatus',   'Repayment',   'PENDING, PENDING_VALIDATION, COMPLETED, FAILED'),
            ('PaymentMethod',   'Repayment',   'CASH, MOBILE_MONEY, BANK_TRANSFER, CHECK'),
            ('TypeNotification','Notification','DEMANDE_PRET, APPROBATION_PRET, CONFIRMATION_REMB, ALERTE_RETARD, ...'),
        ]
    )

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════════
    # 8. SÉCURITÉ
    # ══════════════════════════════════════════════════════════════════════
    add_heading(doc, '8. Sécurité et Authentification', 1)
    add_para(doc,
        "La sécurité est implémentée à plusieurs niveaux pour garantir la confidentialité "
        "et l'intégrité des données financières.")

    add_heading(doc, '8.1 JWT et RBAC', 2)
    add_para(doc,
        "Chaque requête authentifiée inclut un Bearer Token JWT signé avec une clé secrète "
        "partagée (HMAC-SHA256). Le token contient les claims suivants :")
    claims = [
        "sub : email de l'utilisateur (identifiant unique)",
        "role : rôle utilisateur (ADMIN / DIRECTEUR_AGENCE / AGENT / CLIENT)",
        "firstName / lastName : nom complet",
        "agencyId / agencyCode : identifiant de l'agence (pour agents et directeurs)",
        "exp : date d'expiration (24h par défaut)",
    ]
    for c in claims:
        add_bullet(doc, c)

    add_heading(doc, '8.2 Contrôle d\'Accès par Endpoint', 2)
    add_table(doc,
        ['Endpoint', 'Rôles Autorisés'],
        [
            ('POST /api/loans/approve', 'ADMIN, DIRECTEUR_AGENCE'),
            ('GET /api/clients/by-agent/{email}', 'ADMIN, DIRECTEUR_AGENCE'),
            ('POST /api/transactions/depot/**', 'CLIENT, AGENT, ADMIN, DIRECTEUR_AGENCE'),
            ('PATCH /api/comptes/{id}/statut', 'ADMIN, DIRECTEUR_AGENCE'),
            ('GET /api/agency/my-clients', 'DIRECTEUR_AGENCE, ADMIN'),
            ('POST /api/clients/internal/by-agent-emails', 'Public (interne)'),
            ('DELETE /api/comptes/{id}', 'ADMIN uniquement'),
        ]
    )

    add_heading(doc, '8.3 Mesures de Sécurité Additionnelles', 2)
    security_measures = [
        "BCrypt (force 12) pour le hachage des mots de passe",
        "Rate Limiting (via filtre personnalisé) pour limiter les tentatives de connexion",
        "Sessions Redis avec TTL pour l'invalidation centralisée des sessions",
        "CORS configuré par service (origines autorisées explicitement)",
        "Validation des entrées avec Bean Validation (@Valid, @NotNull, etc.)",
        "Tokens de service internes (ADMIN JWT) pour les appels Feign inter-services",
        "Secrets externalisés via variables d'environnement Docker",
    ]
    for s in security_measures:
        add_bullet(doc, s)

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════════
    # 9. SERVICES DÉVELOPPÉS
    # ══════════════════════════════════════════════════════════════════════
    add_heading(doc, '9. Services Développés — Détail Fonctionnel', 1)

    services_detail = [
        ('🔐 Auth Service (8080)',
         "Gère l'inscription, la connexion, et la gestion des tokens. "
         "Supporte 4 rôles (ADMIN, DIRECTEUR_AGENCE, AGENT, CLIENT). "
         "Les tokens JWT incluent le rôle, les noms et l'agencyId. "
         "Les sessions sont stockées dans Redis avec invalidation à la déconnexion. "
         "Inclut la récupération de mot de passe par email.",
         ['POST /auth/login — Authentification + génération JWT',
          'POST /auth/register — Inscription client',
          'POST /auth/agent/create — Création agent (admin)',
          'GET /auth/me — Profil utilisateur connecté',
          'PUT /internal/users/{id}/agency — Mise à jour agence (interne)']),

        ('👤 Client Service (8081)',
         "Gère les profils clients de l\'institution. Chaque client est associé à un agent "
         "(createdBy) et optionnellement à une agence (agencyId). Le service calcule et "
         "met à jour le score de crédit. Les documents KYC sont stockés sur Google Drive.",
         ['POST /api/clients — Création client',
          'GET /api/clients/by-agent/{email} — Clients d\'un agent',
          'GET /api/clients/by-agency/{id} — Clients d\'une agence',
          'POST /api/clients/internal/by-agent-emails — Interne (agency-service)',
          'PATCH /api/clients/{id}/status — Changement statut']),

        ('🏦 Account Service (8082)',
         "Gère les comptes bancaires. Un compte passe par l'état EN_ATTENTE_VALIDATION "
         "avant d'être activé par un directeur ou admin. Le service expose des endpoints "
         "internes pour les opérations de crédit/débit appelés par transaction-service.",
         ['POST /api/comptes — Ouverture de compte',
          'GET /api/comptes/en-attente-validation — File de validation',
          'PATCH /api/comptes/{id}/statut — Activation/Rejet (directeur/admin)',
          'POST /api/comptes/{id}/crediter — Interne (transaction-service)',
          'GET /api/comptes/client/{id} — Comptes d\'un client']),

        ('💳 Loan Service (8083)',
         "Gère le cycle complet du prêt : demande, éligibilité, amortissement, "
         "approbation, décaissement, suivi des échéances. Calcule automatiquement "
         "les plans d'amortissement (méthode des intérêts dégressifs).",
         ['POST /api/loans/apply — Demande de prêt',
          'GET /api/loans/eligibility/{clientId} — Vérification éligibilité',
          'GET /api/loans/{id}/amortization — Plan d\'amortissement',
          'POST /api/loans/approval/{id}/approve — Approbation',
          'POST /api/loans/by-clients — Prêts pour liste de clients']),

        ('💰 Repayment Service (8084)',
         "Gère les remboursements de prêts. Les agents et directeurs enregistrent "
         "des paiements en PENDING_VALIDATION ; l'admin les valide. CamPay déclenche "
         "les confirmations via RabbitMQ (webhook asynchrone).",
         ['POST /api/repayments/pay/record — Enregistrement (agent/directeur)',
          'POST /api/repayments/pay/client — Paiement direct client',
          'GET /api/repayments/pending — Paiements en attente (admin)',
          'POST /api/repayments/{id}/validate — Validation (admin)',
          'POST /api/repayments/stats/by-clients — Stats par agence']),

        ('🏢 Agency Service (8086)',
         "Gère les agences, l'assignation des agents et directeurs, et expose "
         "les données agrégées des clients par agence. La méthode getAgencyClientsWithAccounts "
         "récupère les clients via les emails des agents assignés (endpoint interne client-service).",
         ['GET /api/agency/my-agency — Agence du directeur connecté',
          'GET /api/agency/my-clients — Clients de l\'agence (via agents)',
          'GET /api/agency/my-agents — Agents (actifs + inactifs)',
          'PATCH /api/agency/agents/{id}/toggle-status — Activer/Désactiver agent',
          'POST /api/agency/agents/assign — Assigner un agent']),

        ('💸 Transaction Service (8088)',
         "Gère toutes les opérations financières : dépôts, retraits, virements "
         "inter-comptes et intégration CamPay (Mobile Money). Les transactions "
         "modifient les soldes en appelant account-service via Feign.",
         ['POST /api/transactions/depot/{compteId} — Dépôt',
          'POST /api/transactions/retrait/{compteId} — Retrait',
          'POST /api/transactions/virement/{compteId} — Virement',
          'POST /api/campay/webhook — Webhook CamPay',
          'GET /api/transactions/compte/{compteId} — Historique']),

        ('🔔 Notification Service (8089)',
         "Envoie des notifications aux clients et agents via Email (templates Thymeleaf) "
         "et SMS (Twilio). Les notifications sont déclenchées de manière asynchrone "
         "via RabbitMQ. Supporte les notifications en temps réel via WebSocket.",
         ['Emails : prêt approuvé, compte ouvert, paiement reçu',
          'SMS : alertes critiques, rappels d\'échéance',
          'Templates HTML Thymeleaf avec mise en page professionnelle',
          'WebSocket pour notifications push en temps réel']),
    ]

    for svc_name, svc_desc, svc_endpoints in services_detail:
        add_heading(doc, svc_name, 2)
        add_para(doc, svc_desc)
        add_para(doc, 'Endpoints principaux :', bold=True)
        for ep in svc_endpoints:
            add_bullet(doc, ep)
        doc.add_paragraph()

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════════
    # 10. INTERFACE UTILISATEUR
    # ══════════════════════════════════════════════════════════════════════
    add_heading(doc, '10. Interface Utilisateur', 1)
    add_para(doc,
        "Le frontend React est organisé en espaces distincts selon le rôle de l'utilisateur. "
        "Chaque rôle dispose d'un layout personnalisé avec une navigation adaptée à ses "
        "responsabilités.")

    add_heading(doc, '10.1 Espaces Utilisateurs', 2)
    add_table(doc,
        ['Rôle', 'Route', 'Pages disponibles'],
        [
            ('Admin',        '/admin/**',    'Dashboard, Clients, Prêts, Comptes, Validations, Transactions, Remboursements, Rapports, Notifications, Agents, Directeurs, Agences, Paramètres'),
            ('Directeur',    '/directeur/**', 'Dashboard, Clients (CRUD), Agents (toggle), Validations, Comptes, Prêts, Remboursements, Rapports, Guichet'),
            ('Agent',        '/agent/**',    'Dashboard, Mes Clients, Guichet, Demandes Prêt, Remboursements, Mon Profil'),
            ('Client',       '/client/**',   'Mon Espace, Mes Comptes, Mes Prêts, Transactions, Remboursements, Simulateur'),
        ]
    )

    add_heading(doc, '10.2 Fonctionnalités Clés du Directeur', 2)
    dir_features = [
        "Vue consolidée des clients de son agence (via agents assignés)",
        "Création de nouveaux clients avec assignation d'agence",
        "Modification et changement de statut des clients",
        "Toggle actif/inactif des agents de son agence",
        "Validation des demandes de compte (EN_ATTENTE → ACTIF ou REJETÉ)",
        "Enregistrement de paiements de remboursement (→ PENDING_VALIDATION pour l'admin)",
        "Opérations guichet : Dépôt, Retrait, Virement pour les clients de l'agence",
        "Rapports financiers filtrés par agence",
    ]
    for f in dir_features:
        add_bullet(doc, f)

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════════
    # 11. DÉPLOIEMENT
    # ══════════════════════════════════════════════════════════════════════
    add_heading(doc, '11. Déploiement et Infrastructure', 1)
    add_para(doc,
        "L'ensemble du système est conteneurisé via Docker et orchestré par Docker Compose. "
        "Chaque service dispose de son propre Dockerfile multi-étapes (build Maven + image JRE légère). "
        "Le frontend utilise Nginx pour servir les fichiers React buildés.")

    print("  → Génération Diagramme Déploiement...")
    img8 = draw_deployment()
    add_image(doc, img8, 6.5, 'Figure 8 — Architecture de Déploiement Docker Compose')

    add_heading(doc, '11.1 Variables d\'Environnement (Docker Secrets)', 2)
    add_table(doc,
        ['Variable', 'Usage'],
        [
            ('JWT_SECRET',          'Clé HMAC-SHA256 partagée (256 bits) pour signer les JWT'),
            ('DB_USERNAME / DB_PASSWORD', 'Credentials PostgreSQL'),
            ('RABBITMQ_USERNAME / PASSWORD', 'Credentials RabbitMQ'),
            ('MAIL_HOST / PORT / USERNAME / PASSWORD', 'Serveur SMTP pour emails'),
            ('CAMPAY_USERNAME / PASSWORD', 'Credentials API CamPay (Mobile Money)'),
            ('TWILIO_ACCOUNT_SID / AUTH_TOKEN', 'Credentials Twilio pour SMS'),
            ('GOOGLE_DRIVE_ROOT_FOLDER_ID', 'Dossier Google Drive pour KYC documents'),
            ('ADMIN_EMAIL / ADMIN_PASSWORD', 'Compte admin créé au démarrage'),
        ]
    )

    add_heading(doc, '11.2 Ordre de Démarrage', 2)
    add_para(doc, "Les services respectent un ordre de démarrage via les conditions healthcheck de Docker Compose :")
    startup_order = [
        "1. Infrastructure : postgres, rabbitmq, redis",
        "2. Spring Cloud Core : config-service → registry-service",
        "3. API Gateway",
        "4. Services métier (parallèle) : auth, client, account, agency, loan, repayment, reporting, configuration, transaction, notification",
        "5. Frontend : app-frontend",
    ]
    for s in startup_order:
        add_bullet(doc, s)

    doc.add_page_break()

    # ══════════════════════════════════════════════════════════════════════
    # 12. CONCLUSION
    # ══════════════════════════════════════════════════════════════════════
    add_heading(doc, '12. Conclusion et Perspectives', 1)
    add_para(doc,
        "MicrofinanceHub démontre la faisabilité de la construction d'une application "
        "financière complète selon les principes des architectures microservices modernes. "
        "Le projet intègre les meilleures pratiques du développement logiciel contemporain : "
        "séparation des responsabilités, communication asynchrone, sécurité par défaut, "
        "et déploiement conteneurisé.")

    add_heading(doc, 'Points Forts', 2)
    strengths = [
        "Architecture scalable : chaque service peut être mis à l'échelle indépendamment",
        "Sécurité multicouche : JWT RBAC + BCrypt + Redis sessions + Rate Limiting",
        "Résilience : communication asynchrone RabbitMQ, gestion des erreurs Feign",
        "Expérience utilisateur : interfaces adaptées à chaque rôle, actions contextuelles",
        "Intégration tiers : CamPay (Mobile Money), Twilio (SMS), Google Drive (KYC)",
        "Observabilité : logs structurés, Spring Actuator health checks",
        "Portabilité : déploiement complet en une commande Docker Compose",
    ]
    for s in strengths:
        add_bullet(doc, s)

    add_heading(doc, 'Perspectives d\'Évolution', 2)
    evolutions = [
        "Migration vers Kubernetes pour l'orchestration en production",
        "Centralisation des logs avec ELK Stack (Elasticsearch, Logstash, Kibana)",
        "Mise en place de Keycloak pour la gestion centralisée des identités (SSO)",
        "Circuit Breaker (Resilience4j) pour la tolérance aux pannes entre services",
        "Application mobile native (React Native / Flutter)",
        "Intelligence artificielle pour la notation de crédit (ML credit scoring)",
        "Audit trail complet et conformité réglementaire",
        "API publique documentée (OpenAPI 3.0) pour partenaires tiers",
    ]
    for e in evolutions:
        add_bullet(doc, e)

    # ── Pied de page ──────────────────────────────────────────────────────
    doc.add_paragraph()
    doc.add_paragraph()
    p_final = doc.add_paragraph()
    p_final.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run_final = p_final.add_run('— MicrofinanceHub — Rapport de Présentation —')
    run_final.font.size = Pt(10)
    run_final.font.italic = True
    run_final.font.color.rgb = RGBColor(0x06, 0x4e, 0x3b)

    # ── Sauvegarde ────────────────────────────────────────────────────────
    output_path = '/home/axelle-mbadi/IdeaProjects/MicrofinanceHub/Rapport_MicrofinanceHub.docx'
    doc.save(output_path)
    print(f"\n✅ Rapport généré : {output_path}")
    return output_path

if __name__ == '__main__':
    print("🚀 Génération du rapport MicrofinanceHub...")
    path = make_report()
    print(f"📄 Fichier : {path}")
