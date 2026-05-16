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
VERT     = (0.024, 0.306, 0.243)
VERT_L   = (0.064, 0.618, 0.486)
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

    cloud(0.1, 1.0, 2.4, 7.5, 'CLIENTS', color=(0.4, 0.4, 0.4))
    box(0.3, 7.2, 2.0, 0.7, 'Navigateur', 'React SPA', (0.4,0.4,0.4))
    box(0.3, 6.2, 2.0, 0.7, 'Application', 'Mobile / Tablette', (0.4,0.4,0.4))
    box(0.3, 5.2, 2.0, 0.7, 'Systemes tiers', 'CamPay / Twilio', (0.4,0.4,0.4))

    box(2.9, 6.4, 2.2, 1.2, 'Frontend React', ':3000', VERT_L, fc=(*VERT_L, 0.06))
    box(5.4, 6.4, 2.2, 1.2, 'API Gateway', ':8091', BLEU, fc=(*BLEU, 0.06), fs=8)

    cloud(5.2, 4.5, 2.6, 1.6, 'Spring Cloud', color=ORANGE)
    box(5.4, 4.7, 1.1, 1.1, 'Config', ':8000', ORANGE, fc=(*ORANGE, 0.08), fs=7)
    box(6.6, 4.7, 1.1, 1.1, 'Eureka', ':8761', ORANGE, fc=(*ORANGE, 0.08), fs=7)

    cloud(8.2, 1.0, 7.6, 8.5, 'Microservices Metier', color=VERT)

    services = [
        ('Auth',        ':8080', 8.4,  8.0),
        ('Client',      ':8081', 9.8,  8.0),
        ('Account',     ':8082', 11.2, 8.0),
        ('Loan',        ':8083', 12.6, 8.0),
        ('Repayment',   ':8084', 14.0, 8.0),
        ('Reporting',   ':8085', 8.4,  6.0),
        ('Agency',      ':8086', 9.8,  6.0),
        ('Config.MFI',  ':8087', 11.2, 6.0),
        ('Transaction', ':8088', 12.6, 6.0),
        ('Notif.',      ':8089', 14.0, 6.0),
    ]
    for label, port, sx, sy in services:
        box(sx, sy, 1.3, 0.85, label, port, VERT, fc=(*VERT, 0.05), fs=6.5)

    cloud(8.2, 2.0, 7.6, 3.5, 'Infrastructure de Donnees', color=(0.5, 0.2, 0.6))
    box(8.4,  3.2, 1.5, 1.0, 'PostgreSQL', ':5433', (0.5,0.2,0.6), fc=(0.5,0.2,0.6,0.06), fs=7)
    box(10.1, 3.2, 1.5, 1.0, 'RabbitMQ', ':5672', (0.5,0.2,0.6), fc=(0.5,0.2,0.6,0.06), fs=7)
    box(11.8, 3.2, 1.5, 1.0, 'Redis', ':6379', (0.5,0.2,0.6), fc=(0.5,0.2,0.6,0.06), fs=7)
    box(13.5, 3.2, 1.5, 1.0, 'Google\nDrive', 'API', (0.5,0.2,0.6), fc=(0.5,0.2,0.6,0.06), fs=7)

    arrow(2.5, 7.0, 2.9, 7.0); arrow(2.5, 6.0, 2.9, 6.5)
    arrow(5.1, 7.0, 5.4, 7.0)
    arrow(7.6, 7.0, 8.2, 8.2)
    arrow(6.5, 6.4, 6.8, 6.2, ORANGE); arrow(6.8, 6.2, 8.2, 7.5)
    for sx, sy in [(s[2]+0.65, s[3]) for s in services]:
        ax.plot([sx], [sy-0.85], 'v', color=VERT, markersize=4, alpha=0.5)

    ax.set_title('Architecture Globale — MicrofinanceHub',
                 fontsize=14, fontweight='bold', color='#064e3b', pad=12)
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
        ('MESSAGERIE',     8.0,  ['RabbitMQ 3.13', 'AMQP Protocol', 'Echanges direct/fanout', 'Consumers async', 'Dead Letter Queue', 'Management UI'], (0.7, 0.3, 0.0)),
        ('INFRA / DEVOPS', 11.4, ['Docker + Compose', 'PostgreSQL 16', 'Redis 7', 'Eureka Discovery', 'Config Server', 'GitHub'], (0.4, 0.2, 0.6)),
    ]

    for title, x, items, col in cols:
        ax.add_patch(FancyBboxPatch((x, 6.5), 2.8, 1.2, boxstyle="round,pad=0.08",
                     facecolor=col, edgecolor=col, linewidth=0))
        ax.text(x+1.4, 7.1, title, ha='center', va='center',
                fontsize=9, fontweight='bold', color='white')
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
        ax.add_patch(plt.Circle((x, y+1.1), 0.25, color=color, zorder=5))
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

    ax.add_patch(FancyBboxPatch((3.0, 0.5), 12.5, 9.8, boxstyle="round,pad=0.1",
                 facecolor='#f0f4f8', edgecolor=BLEU, linewidth=2, zorder=0))
    ax.text(9.25, 10.5, 'Systeme MicrofinanceHub', ha='center', fontsize=10,
            fontweight='bold', color='#1a70ba', style='italic')

    actor(1.2, 8.5, 'Admin',      '#7c3aed')
    actor(1.2, 5.5, 'Directeur',  '#064e3b')
    actor(1.2, 2.5, 'Agent',      '#0369a1')
    actor(14.8,5.0, 'Client',     '#b45309')
    actor(14.8,2.0, 'CamPay\n(Ext.)', '#6b7280')

    cases_admin = [
        (5.5, 9.5, 3.2, 0.55, 'Gerer les utilisateurs'),
        (5.5, 8.7, 3.2, 0.55, 'Creer/Gerer les agences'),
        (5.5, 7.9, 3.2, 0.55, 'Assigner directeurs/agents'),
        (5.5, 7.1, 3.2, 0.55, 'Valider comptes & paiements'),
        (5.5, 6.3, 3.2, 0.55, 'Consulter rapports globaux'),
    ]
    for args in cases_admin:
        uc(*args)
        assoc(2.0, 8.8, args[0]-1.6, args[1])

    cases_dir = [
        (9.2, 9.5, 3.2, 0.55, 'Gerer clients (agence)'),
        (9.2, 8.7, 3.2, 0.55, 'Gerer agents (agence)'),
        (9.2, 7.9, 3.2, 0.55, 'Valider comptes (agence)'),
        (9.2, 7.1, 3.2, 0.55, 'Effectuer transactions'),
        (9.2, 6.3, 3.2, 0.55, 'Rapports agence'),
    ]
    for args in cases_dir:
        uc(*args)
        assoc(2.0, 6.0, args[0]-1.6, args[1])

    cases_agent = [
        (5.5, 5.3, 3.2, 0.55, 'Creer clients'),
        (5.5, 4.5, 3.2, 0.55, 'Ouvrir comptes'),
        (5.5, 3.7, 3.2, 0.55, 'Operations guichet'),
        (5.5, 2.9, 3.2, 0.55, 'Enregistrer paiements'),
        (5.5, 2.1, 3.2, 0.55, 'Demandes de pret'),
    ]
    for args in cases_agent:
        uc(*args)
        assoc(2.0, 3.0, args[0]-1.6, args[1])

    cases_client = [
        (9.2, 5.3, 3.2, 0.55, "S'inscrire / Se connecter"),
        (9.2, 4.5, 3.2, 0.55, 'Consulter compte & solde'),
        (9.2, 3.7, 3.2, 0.55, 'Effectuer paiements'),
        (9.2, 2.9, 3.2, 0.55, 'Suivi des prets'),
        (9.2, 2.1, 3.2, 0.55, 'Paiement Mobile Money'),
    ]
    for args in cases_client:
        uc(*args)
        assoc(14.2, 5.2, args[0]+1.6, args[1])

    assoc(14.2, 2.3, 10.8, 2.1)
    assoc(14.2, 2.3, 10.8, 2.9)

    ax.set_title("Diagramme de Cas d'Utilisation — MicrofinanceHub",
                 fontsize=13, fontweight='bold', color='#064e3b', pad=10)
    return fig_to_stream(fig)

# ══════════════════════════════════════════════════════════════════════════════
# DIAGRAMME 4 : Diagramme de Séquence — Authentification
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
    msg(5.0, 9.5, 6.5, 'UserDetails (hash, role)', ret=True, color=(0.3,0.5,0.8))
    msg(4.5, 6.5, 6.5, 'BCrypt.verify(password)', color=(0.5,0.2,0.6))
    ax.add_patch(FancyBboxPatch((6.0, 4.1), 1.0, 0.7, facecolor=(*VERT_L, 0.3),
                 edgecolor=VERT_L, linewidth=1))
    ax.text(6.5, 4.45, 'Genere\nJWT', ha='center', va='center', fontsize=7)
    msg(3.9, 6.5, 12.2, 'Stocke session (TTL)', color=(0.5,0.2,0.6))
    msg(3.4, 12.2, 6.5, 'Session creee', ret=True, color=(0.7,0.1,0.1))
    msg(2.9, 6.5, 3.8, '{ access_token, role, agencyId }', ret=True, color=(0.5,0.2,0.6))
    msg(2.4, 3.8, 1.5, 'Stocke JWT -> localStorage', ret=True, color=VERT_L)
    msg(1.9, 3.8, 1.5, 'Redirige vers /dashboard', ret=True, color=VERT_L)

    ax.set_title('Diagramme de Sequence — Authentification JWT',
                 fontsize=13, fontweight='bold', color='#064e3b', pad=10)
    return fig_to_stream(fig)

# ══════════════════════════════════════════════════════════════════════════════
# DIAGRAMME 5 : Diagramme de Séquence — Processus de Prêt
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

    msg(8.4, 1.5, 3.5, '1. Saisie demande de pret', color=BLEU)
    msg(7.9, 3.5, 5.5, '2. POST /api/loans/apply', color=VERT_L)
    msg(7.4, 5.5, 7.5, '3. Valide JWT', color=ORANGE)
    msg(6.9, 7.5, 5.5, 'Token valide', ret=True, color=(0.5,0.2,0.6))
    msg(6.4, 5.5, 9.5, '4. Traite demande', color=ORANGE)
    msg(5.9, 9.5, 11.5, '5. Verifie eligibilite client', color=VERT)
    msg(5.4, 11.5, 9.5, 'Profil client OK', ret=True, color=(0.6,0.4,0.0))
    msg(4.9, 9.5, 9.5, '6. Calcule amortissement', color=VERT)
    box_act(9.5, 4.55, 0.7, VERT)
    msg(4.4, 9.5, 9.5, '7. Enregistre demande', color=VERT)
    msg(3.9, 9.5, 13.5, '8. Notif. async (RabbitMQ)', color=VERT)
    msg(3.4, 9.5, 5.5, 'Demande creee', ret=True, color=VERT)
    msg(2.9, 5.5, 3.5, 'Reponse applicant', ret=True, color=ORANGE)
    msg(2.4, 3.5, 1.5, "Confirmation a l'agent", ret=True, color=VERT_L)
    msg(1.9, 13.5, 1.5, '9. Email notification', color=ROUGE)

    ax.text(0.3, 0.9, 'RabbitMQ = messagerie asynchrone entre services',
            fontsize=7, style='italic', color='#666666')
    ax.text(0.3, 0.6, 'JWT = JSON Web Token pour authentification stateless',
            fontsize=7, style='italic', color='#666666')

    ax.set_title('Diagramme de Sequence — Demande de Pret',
                 fontsize=13, fontweight='bold', color='#064e3b', pad=10)
    return fig_to_stream(fig)

# ══════════════════════════════════════════════════════════════════════════════
# DIAGRAMME 6 : Séquence — Dépôt d'argent (NOUVEAU)
# ══════════════════════════════════════════════════════════════════════════════
def draw_sequence_transaction():
    fig, ax = plt.subplots(figsize=(16, 10))
    ax.set_xlim(0, 16); ax.set_ylim(0, 10); ax.axis('off')
    fig.patch.set_facecolor('#f8f9fa')

    actors = [
        ('Agent/\nDirecteur', 1.2,  BLEU),
        ('Frontend\nReact',   3.2,  VERT_L),
        ('API\nGateway',      5.2,  ORANGE),
        ('Auth\nService',     7.2,  (0.5,0.2,0.6)),
        ('Transaction\nService', 9.5, VERT),
        ('Account\nService',  12.0, (0.3,0.5,0.8)),
        ('Notification\nService', 14.5, ROUGE),
    ]

    for label, x, col in actors:
        ax.add_patch(FancyBboxPatch((x-0.65, 8.9), 1.3, 0.9, boxstyle="round,pad=0.06",
                     facecolor=col, edgecolor=col, linewidth=0))
        ax.text(x, 9.35, label, ha='center', va='center',
                fontsize=7, fontweight='bold', color='white')
        ax.plot([x, x], [0.5, 8.9], color='#aaaaaa', lw=1, linestyle='--', zorder=1)

    def msg(y, x1, x2, label, ret=False, color='#333333', note=''):
        style = '->' if not ret else '<-'
        ax.annotate('', xy=(x2, y), xytext=(x1, y),
            arrowprops=dict(arrowstyle=style, color=color, lw=1.3))
        mx = (x1 + x2) / 2
        ax.text(mx, y + 0.13, label, ha='center', va='bottom', fontsize=6.8, color=color)
        if note:
            ax.text(mx, y - 0.12, f'({note})', ha='center', va='top',
                    fontsize=6, color='#888888', style='italic')

    def activation(x, y_start, y_end, col):
        ax.add_patch(FancyBboxPatch((x-0.1, y_end), 0.2, y_start - y_end,
                     boxstyle="square", facecolor=(*col, 0.25), edgecolor=col, lw=0.8))

    msg(8.4, 1.2, 3.2, '1. Saisie montant + compteId', color=BLEU)
    msg(7.9, 3.2, 5.2, '2. POST /api/transactions/depot/{compteId}', color=VERT_L)
    msg(7.4, 5.2, 7.2, '3. Validation JWT', color=ORANGE)
    msg(6.9, 7.2, 5.2, 'Token OK + roles extraits', ret=True, color=(0.5,0.2,0.6))
    msg(6.4, 5.2, 9.5, '4. Route vers Transaction Service', color=ORANGE)
    activation(9.5, 6.4, 4.0, VERT)
    msg(5.9, 9.5, 9.5, '5. Verifie compte actif + solde', color=VERT)
    msg(5.4, 9.5, 12.0, '6. POST /api/comptes/{id}/crediter (interne)', color=VERT)
    activation(12.0, 5.4, 4.5, (0.3,0.5,0.8))
    msg(4.9, 12.0, 12.0, '7. Maj solde en base PostgreSQL', color=(0.3,0.5,0.8))
    msg(4.4, 12.0, 9.5, 'Nouveau solde confirme', ret=True, color=(0.3,0.5,0.8))
    msg(3.9, 9.5, 9.5, '8. Enregistre transaction (type=DEPOT)', color=VERT)
    msg(3.4, 9.5, 14.5, '9. Publish event (RabbitMQ async)', color=VERT,
        note='transaction.completed')
    msg(2.9, 9.5, 5.2, 'Reponse: transaction creee', ret=True, color=VERT)
    msg(2.4, 5.2, 3.2, 'HTTP 201 Created', ret=True, color=ORANGE)
    msg(1.9, 3.2, 1.2, 'Affiche confirmation + nouveau solde', ret=True, color=VERT_L)
    msg(1.4, 14.5, 1.2, '10. Email/SMS notification client', color=ROUGE)

    ax.text(0.3, 1.1, 'Appel interne (Feign) entre Transaction Service et Account Service',
            fontsize=6.5, style='italic', color='#666666')
    ax.text(0.3, 0.8, 'Notification envoyee de maniere asynchrone via RabbitMQ',
            fontsize=6.5, style='italic', color='#666666')

    ax.set_title("Diagramme de Sequence — Depot d'Argent",
                 fontsize=13, fontweight='bold', color='#064e3b', pad=10)
    return fig_to_stream(fig)

# ══════════════════════════════════════════════════════════════════════════════
# HELPER UML  (partagé entre tous les diagrammes de classes)
# ══════════════════════════════════════════════════════════════════════════════
def _uml(ax, ROW_H=0.30, H_HDR=0.48):
    """Retourne cls(), enu(), arr() prêts à dessiner."""

    def cls(x, y, name, attrs, color, w=4.0):
        h_body = max(len(attrs) * ROW_H + 0.12, 0.35)
        total  = H_HDR + h_body
        ax.add_patch(FancyBboxPatch((x, y - total), w, total,
            boxstyle='square', facecolor='white', edgecolor=color, lw=1.4))
        ax.add_patch(FancyBboxPatch((x, y - H_HDR), w, H_HDR,
            boxstyle='square', facecolor=color, edgecolor=color, lw=1.4))
        ax.text(x + w/2, y - H_HDR/2, name,
                ha='center', va='center', fontsize=8.5, fontweight='bold', color='white')
        ax.plot([x, x+w], [y - H_HDR, y - H_HDR], color=color, lw=0.8)
        for i, attr in enumerate(attrs):
            ax.text(x + 0.12, y - H_HDR - 0.15 - i*ROW_H, attr,
                    va='center', fontsize=7, color='#333333', family='monospace')
        return y - total   # bottom y

    def enu(x, y, name, values, color, w=3.5):
        h_body = max(len(values) * 0.26 + 0.10, 0.28)
        total  = H_HDR + h_body
        ax.add_patch(FancyBboxPatch((x, y - total), w, total, boxstyle='square',
            facecolor='#fafafa', edgecolor=color, lw=1.0, linestyle='--'))
        ax.add_patch(FancyBboxPatch((x, y - H_HDR), w, H_HDR, boxstyle='square',
            facecolor=(*color, 0.65), edgecolor=color, lw=1.0))
        ax.text(x + w/2, y - H_HDR/2, f'«enumeration»\n{name}',
                ha='center', va='center', fontsize=7.2, fontweight='bold', color='white')
        ax.plot([x, x+w], [y - H_HDR, y - H_HDR], color=color, lw=0.7, linestyle='--')
        for i, v in enumerate(values):
            ax.text(x + w/2, y - H_HDR - 0.12 - i*0.26, v,
                    ha='center', va='center', fontsize=6.8, color='#444444')
        return y - total

    def arr(x1, y1, x2, y2, label='', style='->', color='#555555'):
        ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
            arrowprops=dict(arrowstyle=style, color=color, lw=1.1))
        if label:
            ax.text((x1+x2)/2, (y1+y2)/2 + 0.13, label,
                    ha='center', fontsize=7, color=color,
                    bbox=dict(boxstyle='round,pad=0.1', fc='white', ec='none', alpha=0.85))

    return cls, enu, arr

# ══════════════════════════════════════════════════════════════════════════════
# DIAGRAMME 7 : Diagramme de Classes Global — tous services
# ══════════════════════════════════════════════════════════════════════════════
def draw_classes():
    fig, ax = plt.subplots(figsize=(24, 18))
    ax.set_xlim(0, 24); ax.set_ylim(0, 18); ax.axis('off')
    fig.patch.set_facecolor('#f4f6f8')

    RH = 0.27; HH = 0.44   # row height, header height

    def uml_class(x, y, name, attrs, color, w=3.6):
        h_attr = max(len(attrs) * 0.34 + 0.1, 0.5)
        h_meth = max(len(methods or []) * 0.34 + 0.1, 0.4) if methods else 0
        total_h = 0.55 + h_attr + h_meth

        ax.add_patch(FancyBboxPatch((x, y - total_h), w, total_h,
                     boxstyle="square", facecolor='white', edgecolor=color, lw=1.5))
        ax.add_patch(FancyBboxPatch((x, y - 0.55), w, 0.55,
                     boxstyle="square", facecolor=color, edgecolor=color, lw=1.5))
        ax.text(x + w/2, y - 0.275, name, ha='center', va='center',
                fontsize=9, fontweight='bold', color='white')
        ax.plot([x, x+w], [y - 0.55, y - 0.55], color=color, lw=1)
        for i, attr in enumerate(attrs):
            ax.text(x + 0.12, y - 0.72 - i * 0.34, f'• {attr}',
                    va='center', fontsize=7, color='#333333')
        if methods:
            sep_y = y - 0.55 - h_attr
            ax.plot([x, x+w], [sep_y, sep_y], color=(*color, 0.4), lw=0.8, linestyle=':')
            for i, meth in enumerate(methods):
                ax.text(x + 0.12, sep_y - 0.22 - i * 0.34, f'+ {meth}',
                        va='center', fontsize=7, color='#666666', style='italic')
        return total_h

    def relation(x1, y1, x2, y2, label='', style='->', color='#777777'):
        ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
            arrowprops=dict(arrowstyle=style, color=color, lw=1.2))
        if label:
            ax.text((x1+x2)/2, (y1+y2)/2 + 0.12, label,
                    ha='center', fontsize=7, color=color)

    uml_class(0.2, 13.0, 'User (Auth)',
              ['- id: String', '- email: String', '- password: String (hash)',
               '- userRoleType: Enum', '- agencyId: String', '- agencyCode: String'],
              ['+ login()', '+ generateToken()'], color=(0.5,0.2,0.6), w=3.6)

    uml_class(0.2, 8.5, 'Client',
              ['- id: UUID', '- email: String', '- firstName: String',
               '- lastName: String', '- agencyId: String', '- creditScore: Integer',
               '- status: ClientStatus'],
              ['+ getClientsByAgent()', '+ updateStatus()'], color=BLEU, w=3.6)

    uml_class(0.2, 4.2, 'Agency',
              ['- id: String', '- code: String', '- name: String',
               '- directorId: String', '- directorEmail: String'],
              ['+ getAgents()', '+ toggleAgent()'], color=VERT, w=3.6)

    uml_class(5.0, 13.0, 'Compte',
              ['- id: Long', '- clientId: String', '- numeroCompte: String',
               '- typeCompte: TypeCompte', '- solde: BigDecimal',
               '- statut: StatutCompte'],
              ['+ crediter()', '+ debiter()', '+ changerStatut()'], color=ORANGE, w=3.6)

    uml_class(5.0, 8.0, 'Loan',
              ['- id: String', '- clientId: String', '- amount: BigDecimal',
               '- interestRate: BigDecimal', '- status: LoanStatus',
               '- monthlyPayment: BigDecimal'],
              ['+ approveLoan()', '+ getAmortization()'], color=VERT, w=3.6)

    uml_class(5.0, 3.5, 'AgentAssignment',
              ['- id: String', '- agentId: String', '- agentEmail: String',
               '- agencyId: String', '- active: boolean'],
              ['+ toggleStatus()'], color=(0.5,0.2,0.6), w=3.6)

    uml_class(10.0, 13.0, 'Transaction',
              ['- id: String', '- compteId: Long', '- type: TypeTransaction',
               '- montant: BigDecimal', '- statut: StatutTransaction'],
              ['+ effectuerDepot()', '+ effectuerRetrait()'], color=BLEU, w=3.6)

    uml_class(10.0, 8.2, 'Payment (Repayment)',
              ['- id: String', '- loanId: String', '- clientId: String',
               '- amount: BigDecimal', '- status: PaymentStatus',
               '- paymentMethod: Enum'],
              ['+ validatePayment()'], color=ROUGE, w=3.6)

    uml_class(10.0, 3.5, 'Notification',
              ['- id: String', '- destinataire: String', '- type: TypeNotif.',
               '- canal: EMAIL/SMS', '- statut: ENVOYE/ECHEC'],
              ['+ envoyer()'], color=(0.7,0.3,0.0), w=3.6)

    uml_class(14.2, 13.0, 'Document (KYC)',
              ['- id: String', '- clientId: String', '- type: DocumentType',
               '- driveFileId: String', '- verificationStatus: Enum'],
              ['+ upload()', '+ verify()'], color=(0.3,0.5,0.2), w=3.6)

    uml_class(14.2, 8.5, 'LoanSchedule',
              ['- id: String', '- loanId: String', '- installmentNumber: Int',
               '- dueAmount: BigDecimal', '- paid: boolean'],
              [], color=VERT, w=3.6)

    uml_class(14.2, 4.5, 'Report',
              ['- id: String', '- agencyId: String', '- type: ReportType',
               '- generatedAt: LocalDateTime'],
              ['+ generate()'], color=(0.4,0.4,0.4), w=3.6)

    relation(3.8, 10.0, 5.0, 11.0, '1..*', color=BLEU)
    relation(3.8, 8.0, 3.8, 6.0, '', color=VERT)
    relation(3.8, 6.5, 5.0, 6.0, '1..*', color=VERT)
    relation(8.6, 10.5, 10.0, 10.5, '', color=ORANGE)
    relation(8.6, 6.0, 10.0, 6.0, '1..*', color=VERT)
    relation(1.8, 8.5, 1.8, 7.5, '', color=BLEU)

    ax.set_title('Diagramme de Classes — Entites Principales',
                 fontsize=14, fontweight='bold', color='#064e3b', pad=10)
    return fig_to_stream(fig)

# ══════════════════════════════════════════════════════════════════════════════
# DIAGRAMME 8 : Architecture en couches (service typique) — NOUVEAU
# ══════════════════════════════════════════════════════════════════════════════
def draw_classes_par_service():
    fig, ax = plt.subplots(figsize=(14, 9))
    ax.set_xlim(0, 14); ax.set_ylim(0, 9); ax.axis('off')
    fig.patch.set_facecolor('#f8f9fa')

    # Titre
    ax.text(7, 8.65, 'Architecture en Couches — Service Typique (ex: client-service)',
            ha='center', va='center', fontsize=12, fontweight='bold', color='#064e3b')

    # Couches (bandes horizontales)
    layers = [
        # (y_bottom, height, label_couche, color, elements)
        (6.8, 1.2, 'COUCHE PRESENTATION\n(Controller)', BLEU,
         [('ClientController', 3.0, 7.25),
          ('AccountController', 7.0, 7.25),
          ('LoanController', 11.0, 7.25)]),
        (5.2, 1.2, 'COUCHE SERVICE\n(Business Logic)', VERT,
         [('ClientService', 3.0, 5.75),
          ('AccountService', 7.0, 5.75),
          ('LoanService', 11.0, 5.75)]),
        (3.6, 1.2, 'COUCHE PERSISTANCE\n(Repository)', ORANGE,
         [('ClientRepository\n(JpaRepository)', 3.0, 4.18),
          ('AccountRepository\n(JpaRepository)', 7.0, 4.18),
          ('LoanRepository\n(JpaRepository)', 11.0, 4.18)]),
        (2.0, 1.2, 'COUCHE DOMAINE\n(Entity / DTO)', (0.5,0.2,0.6),
         [('Client\n@Entity', 2.5, 2.58),
          ('ClientRequest\n(DTO)', 4.5, 2.58),
          ('AccountEntity\n@Entity', 7.0, 2.58),
          ('LoanEntity\n@Entity', 11.0, 2.58)]),
    ]

    for (y_bot, h, layer_label, col, elements) in layers:
        # Fond de bande
        ax.add_patch(FancyBboxPatch((0.1, y_bot), 13.8, h, boxstyle="round,pad=0.05",
                     facecolor=(*col, 0.08), edgecolor=col, lw=2))
        # Label couche à gauche
        ax.text(0.55, y_bot + h/2, layer_label, ha='center', va='center',
                fontsize=7.5, fontweight='bold', color=col, rotation=0,
                bbox=dict(boxstyle='round,pad=0.3', facecolor=(*col, 0.15),
                          edgecolor=col, lw=1))
        # Boites dans la couche
        for (elem_label, ex, ey) in elements:
            bw, bh = 2.2, 0.7
            ax.add_patch(FancyBboxPatch((ex - bw/2, ey - bh/2), bw, bh,
                         boxstyle="round,pad=0.06",
                         facecolor=(*col, 0.2), edgecolor=col, lw=1.3))
            ax.text(ex, ey, elem_label, ha='center', va='center',
                    fontsize=7.5, fontweight='bold', color=col)

    # Flèches entre couches (dépendances)
    arrow_xs = [3.0, 7.0, 11.0]
    layer_ys = [(8.0, 6.95), (6.4, 5.85), (4.8, 4.75)]
    for x_arr in arrow_xs:
        for (y_from, y_to) in layer_ys:
            ax.annotate('', xy=(x_arr, y_to), xytext=(x_arr, y_from),
                arrowprops=dict(arrowstyle='->', color='#888888', lw=1.5))

    # Éléments transverses (droite)
    transverses = [
        (12.0, 7.25, 'SecurityConfig\n(Spring Security)', (0.7,0.1,0.1)),
        (12.0, 5.75, 'FeignClient\n(Inter-services)', (0.4,0.1,0.6)),
        (12.0, 4.18, 'GlobalException\nHandler', (0.3,0.3,0.3)),
    ]
    for (tx, ty, tlabel, tcol) in transverses:
        ax.add_patch(FancyBboxPatch((tx - 1.0, ty - 0.38), 2.0, 0.76,
                     boxstyle="round,pad=0.06",
                     facecolor=(*tcol, 0.15), edgecolor=tcol, lw=1.2, linestyle='--'))
        ax.text(tx, ty, tlabel, ha='center', va='center',
                fontsize=7, color=tcol)

    # Base de données en bas
    ax.add_patch(FancyBboxPatch((4.5, 0.5), 5.0, 1.0, boxstyle="round,pad=0.08",
                 facecolor=(0.3,0.5,0.8,0.15), edgecolor=(0.3,0.5,0.8), lw=2))
    ax.text(7.0, 1.0, 'PostgreSQL — Base de donnees du service',
            ha='center', va='center', fontsize=9, fontweight='bold', color=(0.3,0.5,0.8))
    # Flèche Repository → DB
    ax.annotate('', xy=(7.0, 1.5), xytext=(7.0, 2.0),
        arrowprops=dict(arrowstyle='->', color=(0.3,0.5,0.8), lw=1.5))

    # Légende
    legend_items = [
        (BLEU, 'Controller — reçoit les requetes HTTP, retourne les reponses'),
        (VERT, 'Service — logique metier, regles de gestion'),
        (ORANGE, 'Repository — accès base de donnees (Spring Data JPA)'),
        ((0.5,0.2,0.6), 'Entity/DTO — modele de donnees et objets de transfert'),
    ]
    for i, (lcol, ltext) in enumerate(legend_items):
        lx, ly = 0.3, 1.7 - i * 0.3
        ax.add_patch(plt.Rectangle((lx, ly-0.1), 0.25, 0.2, color=lcol, alpha=0.7))
        ax.text(lx + 0.35, ly, ltext, va='center', fontsize=6.5, color='#444444')

    return fig_to_stream(fig)

# ══════════════════════════════════════════════════════════════════════════════
# DIAGRAMME 9 : États/transitions d'un compte bancaire — NOUVEAU
# ══════════════════════════════════════════════════════════════════════════════
def draw_state_compte():
    fig, ax = plt.subplots(figsize=(14, 8))
    ax.set_xlim(0, 14); ax.set_ylim(0, 8); ax.axis('off')
    fig.patch.set_facecolor('#f8f9fa')

    # Couleurs des états
    col_attente = (0.7, 0.55, 0.0)
    col_actif   = VERT
    col_bloque  = ORANGE
    col_ferme   = (0.4, 0.4, 0.4)
    col_rejete  = ROUGE

    def state(x, y, label, col, w=2.2, h=0.75):
        ax.add_patch(FancyBboxPatch((x - w/2, y - h/2), w, h,
                     boxstyle="round,pad=0.12",
                     facecolor=(*col, 0.85), edgecolor=col, lw=2))
        ax.text(x, y, label, ha='center', va='center',
                fontsize=9, fontweight='bold', color='white')

    def transition(x1, y1, x2, y2, label, col='#444444',
                   conn='arc3,rad=0.0', lx=None, ly=None):
        ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
            arrowprops=dict(arrowstyle='->', color=col, lw=1.5,
                            connectionstyle=conn))
        mlx = (x1+x2)/2 if lx is None else lx
        mly = (y1+y2)/2 + 0.18 if ly is None else ly
        ax.text(mlx, mly, label, ha='center', va='bottom',
                fontsize=7.5, color=col,
                bbox=dict(boxstyle='round,pad=0.15', facecolor='white',
                          edgecolor=col, lw=0.7, alpha=0.85))

    # État initial
    ax.add_patch(plt.Circle((2.0, 7.0), 0.22, color='black', zorder=5))

    # États
    state(5.0, 7.0, 'EN_ATTENTE\nVALIDATION', col_attente)
    state(9.0, 7.0, 'ACTIF', col_actif)
    state(11.5, 4.5, 'BLOQUE', col_bloque)
    state(9.0, 2.0, 'FERME', col_ferme)
    state(5.0, 2.0, 'REJETE', col_rejete)

    # État final (double cercle autour de FERME)
    ax.add_patch(plt.Circle((9.0, 2.0), 0.62, color=col_ferme, fill=False, lw=2.5))

    # Transitions
    transition(2.22, 7.0, 3.88, 7.0, 'creation()')
    transition(6.12, 7.0, 7.88, 7.0, 'approbation()\n[directeur/admin]', col=col_actif)
    transition(9.0, 6.62, 9.0, 6.62, '', col='white')  # placeholder
    transition(10.12, 7.0, 10.72, 5.25, 'blocage()\n[admin/directeur]',
               col=ORANGE, conn='arc3,rad=-0.2', lx=11.2, ly=6.4)
    transition(10.88, 4.12, 9.62, 6.62, 'deblocage()\n[admin]',
               col=VERT, conn='arc3,rad=-0.2', lx=8.8, ly=5.5)
    transition(9.0, 6.62, 9.0, 2.38, 'fermeture()\n[admin]',
               col=col_ferme, conn='arc3,rad=0.3', lx=10.5, ly=4.5)
    transition(3.88, 7.0, 5.95, 2.38, 'rejet()\n[admin/directeur]',
               col=col_rejete, conn='arc3,rad=0.3', lx=3.8, ly=4.5)

    # Légende
    ax.text(7.0, 0.4, 'Etat initial : cercle noir  |  Etat final : double cercle  |  Transitions : fleches etiquetees',
            ha='center', fontsize=7.5, style='italic', color='#666666')

    ax.set_title('Diagramme d\'Etats — Compte Bancaire (StatutCompte)',
                 fontsize=13, fontweight='bold', color='#064e3b', pad=10)
    return fig_to_stream(fig)

# ══════════════════════════════════════════════════════════════════════════════
# DIAGRAMME 10 : États/transitions d'un prêt — NOUVEAU
# ══════════════════════════════════════════════════════════════════════════════
def draw_state_loan():
    fig, ax = plt.subplots(figsize=(14, 8))
    ax.set_xlim(0, 14); ax.set_ylim(0, 8); ax.axis('off')
    fig.patch.set_facecolor('#f8f9fa')

    col_pending   = (0.7, 0.55, 0.0)
    col_approved  = BLEU
    col_rejected  = ROUGE
    col_active    = VERT
    col_completed = (0.1, 0.5, 0.2)
    col_defaulted = (0.6, 0.1, 0.5)

    def state(x, y, label, col, w=2.4, h=0.78):
        ax.add_patch(FancyBboxPatch((x - w/2, y - h/2), w, h,
                     boxstyle="round,pad=0.12",
                     facecolor=(*col, 0.85), edgecolor=col, lw=2))
        ax.text(x, y, label, ha='center', va='center',
                fontsize=9, fontweight='bold', color='white')

    def tr(x1, y1, x2, y2, label, col='#444444',
           conn='arc3,rad=0.0', lx=None, ly=None):
        ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
            arrowprops=dict(arrowstyle='->', color=col, lw=1.5,
                            connectionstyle=conn))
        mlx = (x1+x2)/2 if lx is None else lx
        mly = (y1+y2)/2 + 0.2 if ly is None else ly
        ax.text(mlx, mly, label, ha='center', va='bottom',
                fontsize=7.5, color=col,
                bbox=dict(boxstyle='round,pad=0.15', facecolor='white',
                          edgecolor=col, lw=0.7, alpha=0.85))

    # État initial
    ax.add_patch(plt.Circle((1.2, 6.0), 0.22, color='black', zorder=5))

    # États
    state(4.0, 6.0,  'PENDING',   col_pending)
    state(7.5, 7.2,  'APPROVED',  col_approved)
    state(7.5, 4.8,  'REJECTED',  col_rejected)
    state(11.0, 7.2, 'ACTIVE',    col_active)
    state(11.0, 4.8, 'COMPLETED', col_completed)
    state(11.0, 2.5, 'DEFAULTED', col_defaulted)

    # État final (double cercle)
    ax.add_patch(plt.Circle((11.0, 4.8), 0.62, color=col_completed, fill=False, lw=2.5))

    # Transitions
    tr(1.42, 6.0, 2.78, 6.0, 'demande()')
    tr(5.2, 6.3, 6.32, 7.0, 'approbation()\n[admin/directeur]', col=col_approved)
    tr(5.2, 5.7, 6.32, 5.0, 'rejet()\n[admin/directeur]', col=col_rejected)
    tr(8.72, 7.2, 9.78, 7.2, 'decaissement()\n[admin]', col=col_active)
    tr(11.0, 6.82, 11.0, 5.18, 'remboursement\ncomplet()', col=col_completed)
    tr(11.0, 4.42, 11.0, 2.89, 'defaut\npaiement()', col=col_defaulted)
    tr(9.78, 7.0, 5.2, 6.3, 'annulation()\n[admin]',
       col='#888888', conn='arc3,rad=0.3', lx=7.0, ly=7.8)

    ax.text(7.0, 0.4, 'Etat initial : cercle noir  |  Etat final : double cercle (COMPLETED)',
            ha='center', fontsize=7.5, style='italic', color='#666666')

    ax.set_title('Diagramme d\'Etats — Pret (LoanStatus)',
                 fontsize=13, fontweight='bold', color='#064e3b', pad=10)
    return fig_to_stream(fig)

# ══════════════════════════════════════════════════════════════════════════════
# DIAGRAMME 11 : États/transitions d'un paiement — NOUVEAU
# ══════════════════════════════════════════════════════════════════════════════
def draw_state_payment():
    fig, ax = plt.subplots(figsize=(14, 8))
    ax.set_xlim(0, 14); ax.set_ylim(0, 8); ax.axis('off')
    fig.patch.set_facecolor('#f8f9fa')

    col_pending     = (0.7, 0.55, 0.0)
    col_pend_val    = BLEU
    col_completed   = VERT
    col_failed      = ROUGE

    def state(x, y, label, col, w=2.6, h=0.82):
        ax.add_patch(FancyBboxPatch((x - w/2, y - h/2), w, h,
                     boxstyle="round,pad=0.12",
                     facecolor=(*col, 0.85), edgecolor=col, lw=2))
        ax.text(x, y, label, ha='center', va='center',
                fontsize=9, fontweight='bold', color='white')

    def tr(x1, y1, x2, y2, label, col='#444444',
           conn='arc3,rad=0.0', lx=None, ly=None):
        ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
            arrowprops=dict(arrowstyle='->', color=col, lw=1.5,
                            connectionstyle=conn))
        mlx = (x1+x2)/2 if lx is None else lx
        mly = (y1+y2)/2 + 0.2 if ly is None else ly
        ax.text(mlx, mly, label, ha='center', va='bottom',
                fontsize=7.5, color=col,
                bbox=dict(boxstyle='round,pad=0.15', facecolor='white',
                          edgecolor=col, lw=0.7, alpha=0.85))

    # État initial
    ax.add_patch(plt.Circle((1.2, 5.5), 0.22, color='black', zorder=5))

    # États
    state(4.5, 6.5,  'PENDING',             col_pending)
    state(4.5, 4.5,  'PENDING\nVALIDATION', col_pend_val)
    state(9.5, 5.5,  'COMPLETED',           col_completed)
    state(9.5, 3.0,  'FAILED',              col_failed)

    # État final (double cercle autour de COMPLETED)
    ax.add_patch(plt.Circle((9.5, 5.5), 0.65, color=col_completed, fill=False, lw=2.5))

    # Transitions
    tr(1.42, 5.5, 3.18, 6.3, 'agent/directeur\nenregistre()', col=col_pending)
    tr(1.42, 5.5, 3.18, 4.7, 'client\nenregistre()', col=col_pend_val)

    tr(5.82, 6.5, 8.18, 5.7, 'admin valide()\n[PENDING -> COMPLETED]',
       col=col_completed, lx=7.0, ly=7.0)
    tr(5.82, 4.5, 8.18, 5.3, 'admin valide()\n[PENDING_VAL -> COMPLETED]',
       col=col_completed, lx=7.0, ly=4.6)

    tr(5.82, 6.3, 8.18, 3.3, 'echec()\n[timeout/erreur]',
       col=col_failed, conn='arc3,rad=0.2', lx=5.5, ly=4.5)
    tr(5.82, 4.3, 8.18, 3.1, 'echec()\n[rejet admin]',
       col=col_failed, conn='arc3,rad=-0.2', lx=8.0, ly=3.3)

    # Flux CamPay (webhook direct -> COMPLETED)
    ax.annotate('', xy=(8.18, 5.5), xytext=(1.42, 5.5),
        arrowprops=dict(arrowstyle='->', color='#888888', lw=1.3, linestyle='dashed',
                        connectionstyle='arc3,rad=-0.5'))
    ax.text(4.8, 2.8, 'CamPay webhook\n(direct -> COMPLETED)', ha='center',
            fontsize=7, color='#888888', style='italic',
            bbox=dict(boxstyle='round,pad=0.2', facecolor='#f5f5f5',
                      edgecolor='#aaaaaa', lw=0.8))

    ax.text(7.0, 0.4,
            'Etat initial : cercle noir  |  Etat final : COMPLETED (double cercle)  '
            '|  Flux CamPay = tirete',
            ha='center', fontsize=7, style='italic', color='#666666')

    ax.set_title('Diagramme d\'Etats — Paiement/Remboursement (PaymentStatus)',
                 fontsize=13, fontweight='bold', color='#064e3b', pad=10)
    return fig_to_stream(fig)

# ══════════════════════════════════════════════════════════════════════════════
# DIAGRAMME 12 : Communication inter-services (RabbitMQ)
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

    ax.add_patch(FancyBboxPatch((5.5, 3.3), 3.0, 1.4, boxstyle="round,pad=0.1",
                 facecolor='#fff7ed', edgecolor=(0.8,0.4,0.0), lw=2))
    ax.text(7.0, 4.3, 'RabbitMQ', ha='center', fontsize=10, fontweight='bold',
            color=(0.8,0.4,0.0))
    ax.text(7.0, 3.8, 'Exchange : mfh.events', ha='center', fontsize=8, color='#666666')

    queues_pos = [
        (5.6, 2.5, 'loan.created'),
        (7.3, 2.5, 'payment.pending'),
        (5.6, 1.7, 'account.opened'),
        (7.3, 1.7, 'mobile.confirmed'),
    ]
    for qx, qy, ql in queues_pos:
        queue(qx, qy, ql)

    publishers = [
        (0.5, 6.5, 'Auth Service', (0.5,0.2,0.6)),
        (0.5, 5.2, 'Loan Service', VERT),
        (0.5, 3.9, 'Account Service', ORANGE),
        (0.5, 2.6, 'Transaction Svc', BLEU),
    ]
    for px, py, pl, pc in publishers:
        svc_box(px, py, pl, pc)
        publish(px+2.0, py+0.4, 5.5, 3.7)

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
# DIAGRAMME 13 : Flux de Déploiement Docker
# ══════════════════════════════════════════════════════════════════════════════
def draw_deployment():
    fig, ax = plt.subplots(figsize=(16, 10))
    ax.set_xlim(0, 16); ax.set_ylim(0, 10); ax.axis('off')
    fig.patch.set_facecolor('#f8f9fa')

    def container(x, y, w, h, name, port, color=VERT, icon=''):
        ax.add_patch(FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.06",
                     facecolor=(*color, 0.1), edgecolor=color, lw=1.5))
        ax.text(x+w/2, y+h*0.72, f'{icon} {name}' if icon else name,
                ha='center', va='center', fontsize=7.5, fontweight='bold', color=color)
        ax.text(x+w/2, y+h*0.28, port, ha='center', va='center',
                fontsize=6.5, color='#555555')

    # Hote Docker
    ax.add_patch(FancyBboxPatch((0.2, 0.3), 15.6, 9.2, boxstyle="round,pad=0.1",
                 facecolor='#f0f4ff', edgecolor='#3b82f6', lw=2, linestyle='--'))
    ax.text(8.0, 9.6, 'Machine Hote Linux — Docker Engine',
            ha='center', fontsize=11, fontweight='bold', color='#3b82f6')

    # Reseau Docker interne
    ax.add_patch(FancyBboxPatch((0.5, 0.5), 15.0, 8.8, boxstyle="round,pad=0.08",
                 facecolor='#f8fff8', edgecolor=VERT_L, lw=1.5, linestyle=':'))
    ax.text(8.0, 9.2, 'Reseau Docker : mfh-network (bridge)',
            ha='center', fontsize=9, color=VERT_L, style='italic')

    # Infrastructure (ligne 1)
    container(0.8, 0.8, 2.4, 1.0, 'PostgreSQL', '5433:5432', (0.3,0.5,0.8), 'DB')
    container(3.4, 0.8, 2.4, 1.0, 'RabbitMQ', '5672+15672', (0.8,0.4,0.0), 'MQ')
    container(6.0, 0.8, 2.4, 1.0, 'Redis', '6379:6379', (0.7,0.1,0.1), 'Cache')
    container(8.6, 0.8, 2.4, 1.0, 'Config Svc', '8000:8000', ORANGE, 'Cfg')
    container(11.2, 0.8, 3.5, 1.0, 'Eureka Registry', '8761:8761', ORANGE, 'Reg')

    # Ligne 2 — services metier 1/2
    svcs_row1 = [
        ('Auth Svc',    ':8080', 0.8,  2.2),
        ('Client Svc',  ':8081', 3.4,  2.2),
        ('Account Svc', ':8082', 6.0,  2.2),
        ('Loan Svc',    ':8083', 8.6,  2.2),
        ('Repayment',   ':8084', 11.2, 2.2),
    ]
    for name, port, x, y in svcs_row1:
        container(x, y, 2.4, 1.0, name, port, VERT)

    # Ligne 3 — services metier 2/2
    svcs_row2 = [
        ('Reporting',   ':8085', 0.8,  3.5),
        ('Agency Svc',  ':8086', 3.4,  3.5),
        ('Config.MFI',  ':8087', 6.0,  3.5),
        ('Transaction', ':8088', 8.6,  3.5),
        ('Notif. Svc',  ':8089', 11.2, 3.5),
    ]
    for name, port, x, y in svcs_row2:
        container(x, y, 2.4, 1.0, name, port, VERT)

    # Gateway & Frontend
    container(1.5, 5.0, 5.0, 1.4, 'API Gateway', ':8091', BLEU, 'GW')
    container(9.0, 5.0, 5.0, 1.4, 'Frontend React', '3000:80 (Nginx)', VERT_L, 'UI')

    # Volumes
    container(0.8, 7.0, 5.0, 1.0, 'Volumes persistants',
              'postgres_data | rabbitmq_data | redis_data', (0.4,0.4,0.4), 'VOL')

    # Labels couches
    for label, y_pos, col in [
        ('Infrastructure', 1.3, (0.3,0.5,0.8)),
        ('Services Metier', 2.7, VERT),
        ('Gateway / Frontend', 5.7, BLEU),
    ]:
        ax.text(15.4, y_pos, label, ha='right', va='center',
                fontsize=7, color=col, fontweight='bold', style='italic')

    ax.set_title('Diagramme de Deploiement Docker Compose — MicrofinanceHub',
                 fontsize=13, fontweight='bold', color='#064e3b', pad=12)
    return fig_to_stream(fig)

# ══════════════════════════════════════════════════════════════════════════════
# UTILITAIRES WORD
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
    for i, h in enumerate(headers):
        cell = table.cell(0, i)
        cell.text = h
        cell.paragraphs[0].runs[0].bold = True
        cell.paragraphs[0].runs[0].font.size = Pt(9.5)
    for r, row in enumerate(rows):
        for c, val in enumerate(row):
            cell = table.cell(r+1, c)
            cell.text = str(val)
            cell.paragraphs[0].runs[0].font.size = Pt(9)
    doc.add_paragraph()

# ══════════════════════════════════════════════════════════════════════════════
# GÉNÉRATION DU DOCUMENT WORD
# ══════════════════════════════════════════════════════════════════════════════
def make_report():
    doc = Document()

    style = doc.styles['Normal']
    style.font.name = 'Calibri'
    style.font.size = Pt(11)

    for section in doc.sections:
        section.top_margin    = Cm(2.5)
        section.bottom_margin = Cm(2.5)
        section.left_margin   = Cm(2.8)
        section.right_margin  = Cm(2.8)

    # ── PAGE DE GARDE ──────────────────────────────────────────────────────
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(40)
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run('RAPPORT DE PROJET')
    run.bold = True; run.font.size = Pt(28)
    run.font.color.rgb = RGBColor(0x06, 0x4e, 0x3b)

    p2 = doc.add_paragraph(); p2.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r2 = p2.add_run('MicrofinanceHub')
    r2.bold = True; r2.font.size = Pt(22)
    r2.font.color.rgb = RGBColor(0x06, 0x4e, 0x3b)

    doc.add_paragraph()
    p3 = doc.add_paragraph(); p3.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p3.add_run('Plateforme Microservices de Gestion de Microfinance').font.size = Pt(14)

    doc.add_paragraph(); doc.add_paragraph()
    p4 = doc.add_paragraph(); p4.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p4.add_run(f'Date : {datetime.date.today().strftime("%d %B %Y")}').font.size = Pt(12)

    doc.add_paragraph()
    p5 = doc.add_paragraph(); p5.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p5.add_run('Architecture : Microservices Spring Boot | Frontend : React').font.size = Pt(11)

    doc.add_page_break()

    # ── TABLE DES MATIERES ─────────────────────────────────────────────────
    add_heading(doc, 'Table des Matieres', 1)
    toc = [
        ('1.',      'Introduction et Contexte'),
        ('2.',      'Presentation du Projet'),
        ('3.',      'Architecture Globale'),
        ('4.',      'Architecture Technique — Stack'),
        ('5.',      'Diagrammes UML'),
        ('  5.1.',  "Cas d'Utilisation"),
        ('  5.2.',  'Sequence — Authentification JWT'),
        ('  5.3.',  'Sequence — Demande de Pret'),
        ('  5.4.',  "Sequence — Transaction (depot)"),
        ('  5.5.',  'Classes Globales'),
        ('  5.6.',  'Architecture par Couche (service typique)'),
        ('  5.7.',  'Etats — Compte Bancaire'),
        ('  5.8.',  'Etats — Pret'),
        ('  5.9.',  'Etats — Paiement/Remboursement'),
        ('6.',      'Communication Inter-Services (RabbitMQ)'),
        ('7.',      'Modele de Donnees'),
        ('8.',      'Securite et Authentification'),
        ('9.',      'Services Developpes'),
        ('10.',     'Interface Utilisateur'),
        ('11.',     'Deploiement et Infrastructure'),
        ('12.',     'Conclusion et Perspectives'),
    ]
    for num, title in toc:
        p = doc.add_paragraph()
        p.add_run(f'{num}  {title}').font.size = Pt(11)
        p.paragraph_format.space_after = Pt(2)

    doc.add_page_break()

    # ── 1. INTRODUCTION ────────────────────────────────────────────────────
    add_heading(doc, '1. Introduction et Contexte', 1)
    add_para(doc,
        "Le secteur de la microfinance joue un role crucial dans l'inclusion financiere, "
        "notamment dans les economies emergentes. Les institutions de microfinance (IMF) gerent "
        "quotidiennement une grande diversite d'operations : gestion des clients, des comptes "
        "bancaires, des prets, des remboursements et des paiements mobiles. Ce volume d'operations "
        "necessite un systeme d'information robuste, scalable et securise.")
    add_para(doc,
        "C'est dans ce contexte que s'inscrit MicrofinanceHub : une plateforme complete de gestion "
        "de microfinance construite sur une architecture microservices moderne. Le projet repond aux "
        "exigences de performance, de maintenabilite et d'evolutivite des systemes d'information "
        "contemporains.")

    add_heading(doc, 'Objectifs du Projet', 2)
    for b in [
        "Digitaliser l'ensemble du cycle de vie d'une institution de microfinance",
        "Garantir la separation des responsabilites via une architecture microservices",
        "Assurer une securite robuste basee sur JWT et RBAC (4 roles : Admin, Directeur, Agent, Client)",
        "Integrer les paiements mobiles (Mobile Money via CamPay)",
        "Offrir des tableaux de bord adaptes a chaque role utilisateur",
        "Deployer l'ensemble via Docker Compose pour une portabilite maximale",
    ]:
        add_bullet(doc, b)

    doc.add_page_break()

    # ── 2. PRESENTATION ────────────────────────────────────────────────────
    add_heading(doc, '2. Presentation du Projet', 1)
    add_para(doc,
        "MicrofinanceHub est une application web full-stack composee de 12 microservices "
        "backend et d'un frontend React. Elle couvre l'ensemble du perimetre fonctionnel "
        "d'une IMF moderne : de la creation du profil client jusqu'au reporting financier avance.")

    add_heading(doc, '2.1 Roles Utilisateurs', 2)
    add_table(doc,
        ['Role', 'Responsabilites', 'Acces'],
        [
            ('Administrateur', 'Gestion globale : utilisateurs, agences, validations',
             'Toutes sections, toutes agences'),
            ("Directeur d'Agence", 'Supervision agence : clients, agents, validations, prets',
             'Donnees de son agence uniquement'),
            ('Agent de Terrain', 'Operations quotidiennes : clients, comptes, guichet',
             'Ses clients assignes'),
            ('Client', 'Consultation, paiements, suivi des prets',
             'Son profil uniquement'),
        ]
    )

    add_heading(doc, '2.2 Fonctionnalites Cles', 2)
    for f in [
        "Gestion complete des clients (KYC, documents, score credit)",
        "Ouverture et gestion de comptes bancaires (Courant, Epargne, Micro-epargne)",
        "Cycle de pret complet : demande -> eligibilite -> approbation -> decaissement -> remboursement",
        "Calcul automatique du plan d'amortissement",
        "Paiements Multi-canal : Especes, Mobile Money (CamPay), Virement, Cheque",
        "Notifications temps reel : Email (Thymeleaf) + SMS (Twilio)",
        "Transactions financieres : Depot, Retrait, Virement inter-comptes",
        "Rapports et tableaux de bord par agence et globaux",
        "Gestion multi-agences avec hierarchie Directeur/Agent",
        "Stockage documents KYC sur Google Drive",
    ]:
        add_bullet(doc, f)

    doc.add_page_break()

    # ── 3. ARCHITECTURE GLOBALE ────────────────────────────────────────────
    add_heading(doc, '3. Architecture Globale', 1)
    add_para(doc,
        "Le systeme adopte une architecture microservices ou chaque service est independant, "
        "deployable separement et responsable d'un domaine metier precis. Les services "
        "communiquent via REST (synchrone) et RabbitMQ (asynchrone).")

    print("  -> Generation Architecture Globale...")
    add_image(doc, draw_architecture_globale(), 6.5,
              'Figure 1 — Architecture Globale MicrofinanceHub')

    add_heading(doc, '3.1 Inventaire des Services', 2)
    add_table(doc,
        ['Service', 'Port', 'Role', 'Base de donnees'],
        [
            ('config-service',        '8000', 'Serveur de configuration centralise (Spring Cloud Config)', '—'),
            ('registry-service',      '8761', 'Service Discovery (Eureka)', '—'),
            ('api-gateway',           '8091', "Point d'entree unique, routage, CORS", '—'),
            ('auth-service',          '8080', 'Authentification, JWT, sessions Redis', 'microfinancehub_auth'),
            ('client-service',        '8081', 'Profils clients, documents KYC, score credit', 'microfinancehub_client'),
            ('account-service',       '8082', 'Comptes bancaires, operations', 'microfinancehub_account'),
            ('loan-service',          '8083', 'Prets, amortissement, eligibilite', 'microfinancehub_loan'),
            ('repayment-service',     '8084', 'Remboursements, validation paiements', 'microfinancehub_repayment'),
            ('reporting-service',     '8085', 'Rapports Excel/PDF, statistiques', 'microfinancehub_reporting'),
            ('agency-service',        '8086', 'Agences, agents, directeurs', 'microfinancehub_agency'),
            ('configuration-service', '8087', 'Parametrage metier MFI', 'microfinancehub_configuration'),
            ('transaction-service',   '8088', 'Depots, retraits, virements, CamPay', 'microfinancehub_transaction'),
            ('notification-service',  '8089', 'Emails Thymeleaf, SMS Twilio, WebSocket', 'microfinancehub_notification'),
            ('app-frontend',          '3000', 'SPA React (Nginx en production)', '—'),
        ]
    )

    doc.add_page_break()

    # ── 4. STACK TECHNIQUE ─────────────────────────────────────────────────
    add_heading(doc, '4. Architecture Technique — Stack Technologique', 1)

    print("  -> Generation Stack Technique...")
    add_image(doc, draw_stack_technique(), 6.2,
              'Figure 2 — Stack Technologique par couche')

    add_heading(doc, '4.1 Technologies Backend', 2)
    add_table(doc,
        ['Technologie', 'Version', 'Usage'],
        [
            ('Spring Boot',      '3.4.4',    'Framework principal de chaque microservice'),
            ('Spring Cloud',     '2024.0.0', 'Config Server, Eureka, OpenFeign, Gateway'),
            ('Spring Security',  '6.x',      'Authentification, autorisation par role (RBAC)'),
            ('Spring Data JPA',  '3.x',      'ORM avec Hibernate, acces base de donnees'),
            ('JJWT',             '0.12.6',   'Generation et validation des tokens JWT'),
            ('OpenFeign',        '4.x',      'Appels REST inter-services declaratifs'),
            ('RabbitMQ (AMQP)', '3.13',      'Messagerie asynchrone entre services'),
            ('PostgreSQL',       '16',       'Base de donnees relationnelle (une par service)'),
            ('Redis',            '7',        'Cache sessions, rate limiting'),
            ('Lombok',           '1.18',     'Reduction du code boilerplate Java'),
            ('Thymeleaf',        '3.x',      'Templates HTML pour emails'),
        ]
    )

    add_heading(doc, '4.2 Technologies Frontend', 2)
    add_table(doc,
        ['Technologie', 'Usage'],
        [
            ('React 18',        'Framework SPA, gestion etat local et hooks'),
            ('Axios',           'Client HTTP pour appels REST, intercepteurs JWT'),
            ('Tailwind CSS',    'Framework CSS utilitaire, design system'),
            ('React Router v6', 'Navigation SPA, routes protegees par role'),
            ('Lucide React',    "Bibliotheque d'icones SVG"),
            ('LocalStorage',    'Persistance session (token JWT + profil utilisateur)'),
        ]
    )

    doc.add_page_break()

    # ── 5. DIAGRAMMES UML ──────────────────────────────────────────────────
    add_heading(doc, '5. Diagrammes UML', 1)

    # 5.1 Use Case
    add_heading(doc, "5.1 Diagramme de Cas d'Utilisation", 2)
    add_para(doc,
        "Le diagramme suivant represente les interactions entre les 4 acteurs principaux "
        "du systeme et les fonctionnalites auxquelles ils ont acces.")
    print("  -> Generation Use Case...")
    add_image(doc, draw_use_case(), 6.5,
              "Figure 3 — Diagramme de Cas d'Utilisation")

    doc.add_page_break()

    # 5.2 Sequence Auth
    add_heading(doc, '5.2 Diagramme de Sequence — Authentification JWT', 2)
    add_para(doc,
        "Ce diagramme illustre le flux complet d'authentification : depuis la saisie des "
        "identifiants jusqu'au stockage du JWT et a la redirection vers le dashboard approprie. "
        "Le token contient : l'email, le role, les noms et l'agencyId de l'utilisateur.")
    print("  -> Generation Sequence Authentification...")
    add_image(doc, draw_sequence_auth(), 6.2,
              'Figure 4 — Sequence Authentification JWT')

    # 5.3 Sequence Pret
    add_heading(doc, '5.3 Diagramme de Sequence — Demande de Pret', 2)
    add_para(doc,
        "Ce diagramme montre le flux complet de traitement d'une demande de pret, "
        "incluant la verification JWT, la consultation du profil client, le calcul "
        "de l'amortissement et la notification asynchrone via RabbitMQ.")
    print("  -> Generation Sequence Pret...")
    add_image(doc, draw_sequence_pret(), 6.5,
              'Figure 5 — Sequence Demande de Pret')

    doc.add_page_break()

    # 5.4 Sequence Transaction (NOUVEAU)
    add_heading(doc, "5.4 Diagramme de Sequence — Transaction (Depot d'Argent)", 2)
    add_para(doc,
        "Ce diagramme illustre le flux d'un depot d'argent effectue par un agent ou directeur. "
        "La requete transite par l'API Gateway (validation JWT), puis le Transaction Service "
        "credits le compte via un appel interne Feign vers l'Account Service, enregistre "
        "la transaction et publie un evenement asynchrone vers le Notification Service.")
    print("  -> Generation Sequence Transaction...")
    add_image(doc, draw_sequence_transaction(), 6.5,
              "Figure 6 — Sequence Depot d'Argent (Transaction Service)")

    doc.add_page_break()

    # 5.5 Classes
    add_heading(doc, '5.5 Diagramme de Classes — Entites Principales', 2)
    add_para(doc,
        "Ce diagramme presente les entites metier principales et leurs relations. "
        "Chaque entite est geree par son microservice dedie et possede sa propre "
        "base de donnees PostgreSQL (pattern Database per Service).")
    print("  -> Generation Diagramme de Classes...")
    add_image(doc, draw_classes(), 6.5,
              'Figure 7 — Diagramme de Classes Principales')

    doc.add_page_break()

    # 5.6 Architecture par couche (NOUVEAU)
    add_heading(doc, '5.6 Architecture par Couche — Service Typique', 2)
    add_para(doc,
        "Ce diagramme illustre l'organisation interne de chaque microservice selon "
        "le pattern en couches : Controller (presentation), Service (logique metier), "
        "Repository (persistance JPA) et Entity/DTO (domaine). Les composants transverses "
        "(SecurityConfig, FeignClient, GlobalExceptionHandler) sont communs a tous les services.")
    print("  -> Generation Architecture par Couche...")
    add_image(doc, draw_classes_par_service(), 6.5,
              'Figure 8 — Architecture en Couches (service typique)')

    doc.add_page_break()

    # 5.7 Etats Compte (NOUVEAU)
    add_heading(doc, '5.7 Diagramme d\'Etats — Compte Bancaire', 2)
    add_para(doc,
        "Un compte bancaire suit un cycle de vie strict : cree en EN_ATTENTE_VALIDATION, "
        "il doit etre approuve par un directeur ou admin (-> ACTIF). Il peut ensuite etre "
        "bloque, debloque ou ferme. Un rejet lors de la validation le place en etat REJETE.")
    print("  -> Generation Etats Compte...")
    add_image(doc, draw_state_compte(), 6.2,
              'Figure 9 — Diagramme d\'Etats : Compte Bancaire (StatutCompte)')

    # 5.8 Etats Pret (NOUVEAU)
    add_heading(doc, "5.8 Diagramme d'Etats — Pret", 2)
    add_para(doc,
        "Le cycle de vie d'un pret commence a l'etat PENDING apres soumission de la demande. "
        "Un admin ou directeur peut l'approuver (-> APPROVED) ou le rejeter (-> REJECTED). "
        "Apres decaissement, il passe ACTIVE, puis COMPLETED a remboursement total ou "
        "DEFAULTED en cas de defaut de paiement.")
    print("  -> Generation Etats Pret...")
    add_image(doc, draw_state_loan(), 6.2,
              "Figure 10 — Diagramme d'Etats : Pret (LoanStatus)")

    doc.add_page_break()

    # 5.9 Etats Paiement (NOUVEAU)
    add_heading(doc, "5.9 Diagramme d'Etats — Paiement/Remboursement", 2)
    add_para(doc,
        "Un paiement enregistre par un agent ou directeur passe en PENDING_VALIDATION "
        "et attend la validation de l'admin. Un paiement direct client ou un webhook "
        "CamPay peuvent passer directement en COMPLETED. En cas de probleme, le statut "
        "passe en FAILED.")
    print("  -> Generation Etats Paiement...")
    add_image(doc, draw_state_payment(), 6.2,
              "Figure 11 — Diagramme d'Etats : Paiement/Remboursement (PaymentStatus)")

    doc.add_page_break()

    # ── 6. RABBITMQ ────────────────────────────────────────────────────────
    add_heading(doc, '6. Communication Inter-Services (RabbitMQ)', 1)
    add_para(doc,
        "La communication asynchrone via RabbitMQ permet de decoupler les services et "
        "d'assurer la resilience du systeme. Lorsqu'un service publie un evenement, "
        "il n'attend pas la reponse des consommateurs, ce qui ameliore les performances "
        "et la disponibilite globale.")

    print("  -> Generation Diagramme RabbitMQ...")
    add_image(doc, draw_rabbitmq(), 6.2,
              'Figure 12 — Communication Asynchrone via RabbitMQ')

    add_heading(doc, '6.1 Evenements Principaux', 2)
    add_table(doc,
        ['Evenement', 'Producteur', 'Consommateur', 'Declencheur'],
        [
            ('user.login',        'Auth Service',      'Client Service',    'Connexion utilisateur'),
            ('loan.created',      'Loan Service',      'Notification Svc',  'Nouvelle demande pret'),
            ('loan.approved',     'Loan Service',      'Notification Svc',  'Pret approuve'),
            ('payment.pending',   'Repayment Service', 'Notification Svc',  'Paiement en attente'),
            ('payment.validated', 'Repayment Service', 'Loan Service',      'Paiement valide -> MAJ echeance'),
            ('account.opened',    'Account Service',   'Notification Svc',  'Compte ouvert'),
            ('mobile.confirmed',  'Transaction Svc',   'Repayment Service', 'CamPay webhook recu'),
            ('payment.received',  'Repayment Service', 'Notification Svc',  'Paiement CamPay confirme'),
        ]
    )

    doc.add_page_break()

    # ── 7. MODELE DE DONNEES ────────────────────────────────────────────────
    add_heading(doc, '7. Modele de Donnees', 1)
    add_para(doc,
        "Le projet applique le pattern 'Database per Service' : chaque microservice "
        "possede sa propre base de donnees PostgreSQL, garantissant l'independance "
        "et l'isolation des donnees. Les jointures inter-services se font via des "
        "appels REST (Feign) et non par des cles etrangeres croisees.")

    add_heading(doc, '7.1 Bases de Donnees', 2)
    add_table(doc,
        ['Base de donnees', 'Service', 'Tables principales', 'Port hote'],
        [
            ('microfinancehub_auth',          'auth-service',          'users, roles, refresh_tokens, user_sessions', '5433'),
            ('microfinancehub_client',        'client-service',        'clients, documents', '5433'),
            ('microfinancehub_account',       'account-service',       'compte, compte_events', '5433'),
            ('microfinancehub_loan',          'loan-service',          'loans, loan_applications, loan_schedules', '5433'),
            ('microfinancehub_repayment',     'repayment-service',     'payments, repayments, schedules', '5433'),
            ('microfinancehub_reporting',     'reporting-service',     'reports, report_data', '5433'),
            ('microfinancehub_agency',        'agency-service',        'agencies, agent_assignments, agency_stats', '5433'),
            ('microfinancehub_configuration', 'configuration-service', 'mfi_configs, loan_products', '5433'),
            ('microfinancehub_transaction',   'transaction-service',   'transactions, campay_webhooks', '5433'),
            ('microfinancehub_notification',  'notification-service',  'notifications, notification_logs', '5433'),
        ]
    )

    add_heading(doc, '7.2 Enums et Types Metier', 2)
    add_table(doc,
        ['Enum', 'Service', 'Valeurs'],
        [
            ('UserRoleType',     'Auth',        'ADMIN, DIRECTEUR_AGENCE, AGENT, CLIENT'),
            ('ClientStatus',     'Client',      'ACTIVE, INACTIVE, PENDING, SUSPENDED'),
            ('StatutCompte',     'Account',     'ACTIF, INACTIF, SUSPENDU, BLOQUE, FERME, EN_ATTENTE_VALIDATION, REJETE'),
            ('TypeCompte',       'Account',     'COURANT, EPARGNE, MICRO_EPARGNE, DEPOT_A_TERME, CREDIT'),
            ('LoanStatus',       'Loan',        'PENDING, APPROVED, REJECTED, ACTIVE, COMPLETED, DEFAULT'),
            ('PaymentStatus',    'Repayment',   'PENDING, PENDING_VALIDATION, COMPLETED, FAILED'),
            ('PaymentMethod',    'Repayment',   'CASH, MOBILE_MONEY, BANK_TRANSFER, CHECK'),
            ('TypeNotification', 'Notification','DEMANDE_PRET, APPROBATION_PRET, CONFIRMATION_REMB, ALERTE_RETARD, ...'),
        ]
    )

    doc.add_page_break()

    # ── 8. SECURITE ─────────────────────────────────────────────────────────
    add_heading(doc, '8. Securite et Authentification', 1)
    add_para(doc,
        "La securite est implementee a plusieurs niveaux pour garantir la confidentialite "
        "et l'integrite des donnees financieres.")

    add_heading(doc, '8.1 JWT et RBAC', 2)
    add_para(doc,
        "Chaque requete authentifiee inclut un Bearer Token JWT signe avec une cle secrete "
        "partagee (HMAC-SHA256). Le token contient les claims suivants :")
    for c in [
        "sub : email de l'utilisateur (identifiant unique)",
        "role : role utilisateur (ADMIN / DIRECTEUR_AGENCE / AGENT / CLIENT)",
        "firstName / lastName : nom complet",
        "agencyId / agencyCode : identifiant de l'agence (pour agents et directeurs)",
        "exp : date d'expiration (24h par defaut)",
    ]:
        add_bullet(doc, c)

    add_heading(doc, "8.2 Controle d'Acces par Endpoint", 2)
    add_table(doc,
        ['Endpoint', 'Roles Autorises'],
        [
            ('POST /api/loans/approve',                 'ADMIN, DIRECTEUR_AGENCE'),
            ('GET /api/clients/by-agent/{email}',       'ADMIN, DIRECTEUR_AGENCE'),
            ('POST /api/transactions/depot/**',         'CLIENT, AGENT, ADMIN, DIRECTEUR_AGENCE'),
            ('PATCH /api/comptes/{id}/statut',          'ADMIN, DIRECTEUR_AGENCE'),
            ('GET /api/agency/my-clients',              'DIRECTEUR_AGENCE, ADMIN'),
            ('POST /api/clients/internal/by-agent-emails', 'Public (interne)'),
            ('DELETE /api/comptes/{id}',                'ADMIN uniquement'),
        ]
    )

    add_heading(doc, '8.3 Mesures de Securite Additionnelles', 2)
    for s in [
        "BCrypt (force 12) pour le hachage des mots de passe",
        "Rate Limiting (via filtre personnalise) pour limiter les tentatives de connexion",
        "Sessions Redis avec TTL pour l'invalidation centralisee des sessions",
        "CORS configure par service (origines autorisees explicitement)",
        "Validation des entrees avec Bean Validation (@Valid, @NotNull, etc.)",
        "Tokens de service internes (ADMIN JWT) pour les appels Feign inter-services",
        "Secrets externalises via variables d'environnement Docker",
    ]:
        add_bullet(doc, s)

    doc.add_page_break()

    # ── 9. SERVICES ─────────────────────────────────────────────────────────
    add_heading(doc, '9. Services Developpes — Detail Fonctionnel', 1)

    services_detail = [
        ('Auth Service (8080)',
         "Gere l'inscription, la connexion, et la gestion des tokens. "
         "Supporte 4 roles (ADMIN, DIRECTEUR_AGENCE, AGENT, CLIENT). "
         "Les tokens JWT incluent le role, les noms et l'agencyId. "
         "Les sessions sont stockees dans Redis avec invalidation a la deconnexion. "
         "Inclut la recuperation de mot de passe par email.",
         ['POST /auth/login — Authentification + generation JWT',
          'POST /auth/register — Inscription client',
          'POST /auth/agent/create — Creation agent (admin)',
          'GET /auth/me — Profil utilisateur connecte',
          'PUT /internal/users/{id}/agency — Mise a jour agence (interne)']),

        ('Client Service (8081)',
         "Gere les profils clients de l'institution. Chaque client est associe a un agent "
         "(createdBy) et optionnellement a une agence (agencyId). Le service calcule et "
         "met a jour le score de credit. Les documents KYC sont stockes sur Google Drive.",
         ['POST /api/clients — Creation client',
          'GET /api/clients/by-agent/{email} — Clients d\'un agent',
          'GET /api/clients/by-agency/{id} — Clients d\'une agence',
          'POST /api/clients/internal/by-agent-emails — Interne (agency-service)',
          'PATCH /api/clients/{id}/status — Changement statut']),

        ('Account Service (8082)',
         "Gere les comptes bancaires. Un compte passe par l'etat EN_ATTENTE_VALIDATION "
         "avant d'etre active par un directeur ou admin. Le service expose des endpoints "
         "internes pour les operations de credit/debit appeles par transaction-service.",
         ['POST /api/comptes — Ouverture de compte',
          'GET /api/comptes/en-attente-validation — File de validation',
          'PATCH /api/comptes/{id}/statut — Activation/Rejet (directeur/admin)',
          'POST /api/comptes/{id}/crediter — Interne (transaction-service)',
          'GET /api/comptes/client/{id} — Comptes d\'un client']),

        ('Loan Service (8083)',
         "Gere le cycle complet du pret : demande, eligibilite, amortissement, "
         "approbation, decaissement, suivi des echeances. Calcule automatiquement "
         "les plans d'amortissement (methode des interets degressifs).",
         ['POST /api/loans/apply — Demande de pret',
          'GET /api/loans/eligibility/{clientId} — Verification eligibilite',
          "GET /api/loans/{id}/amortization — Plan d'amortissement",
          'POST /api/loans/approval/{id}/approve — Approbation',
          'POST /api/loans/by-clients — Prets pour liste de clients']),

        ('Repayment Service (8084)',
         "Gere les remboursements de prets. Les agents et directeurs enregistrent "
         "des paiements en PENDING_VALIDATION ; l'admin les valide. CamPay declenche "
         "les confirmations via RabbitMQ (webhook asynchrone).",
         ['POST /api/repayments/pay/record — Enregistrement (agent/directeur)',
          'POST /api/repayments/pay/client — Paiement direct client',
          'GET /api/repayments/pending — Paiements en attente (admin)',
          'POST /api/repayments/{id}/validate — Validation (admin)',
          'POST /api/repayments/stats/by-clients — Stats par agence']),

        ('Agency Service (8086)',
         "Gere les agences, l'assignation des agents et directeurs, et expose "
         "les donnees agregees des clients par agence. La methode getAgencyClientsWithAccounts "
         "recupere les clients via les emails des agents assignes (endpoint interne client-service).",
         ['GET /api/agency/my-agency — Agence du directeur connecte',
          "GET /api/agency/my-clients — Clients de l'agence (via agents)",
          'GET /api/agency/my-agents — Agents (actifs + inactifs)',
          'PATCH /api/agency/agents/{id}/toggle-status — Activer/Desactiver agent',
          'POST /api/agency/agents/assign — Assigner un agent']),

        ('Transaction Service (8088)',
         "Gere toutes les operations financieres : depots, retraits, virements "
         "inter-comptes et integration CamPay (Mobile Money). Les transactions "
         "modifient les soldes en appelant account-service via Feign.",
         ['POST /api/transactions/depot/{compteId} — Depot',
          'POST /api/transactions/retrait/{compteId} — Retrait',
          'POST /api/transactions/virement/{compteId} — Virement',
          'POST /api/campay/webhook — Webhook CamPay',
          'GET /api/transactions/compte/{compteId} — Historique']),

        ('Notification Service (8089)',
         "Envoie des notifications aux clients et agents via Email (templates Thymeleaf) "
         "et SMS (Twilio). Les notifications sont declenchees de maniere asynchrone "
         "via RabbitMQ. Supporte les notifications en temps reel via WebSocket.",
         ['Emails : pret approuve, compte ouvert, paiement recu',
          "SMS : alertes critiques, rappels d'echeance",
          'Templates HTML Thymeleaf avec mise en page professionnelle',
          'WebSocket pour notifications push en temps reel']),
    ]

    for svc_name, svc_desc, svc_endpoints in services_detail:
        add_heading(doc, svc_name, 2)
        add_para(doc, svc_desc)
        add_para(doc, 'Endpoints principaux :', bold=True)
        for ep in svc_endpoints:
            add_bullet(doc, ep)
        doc.add_paragraph()

    doc.add_page_break()

    # ── 10. INTERFACE UTILISATEUR ───────────────────────────────────────────
    add_heading(doc, '10. Interface Utilisateur', 1)
    add_para(doc,
        "Le frontend React est organise en espaces distincts selon le role de l'utilisateur. "
        "Chaque role dispose d'un layout personnalise avec une navigation adaptee a ses "
        "responsabilites.")

    add_heading(doc, '10.1 Espaces Utilisateurs', 2)
    add_table(doc,
        ['Role', 'Route', 'Pages disponibles'],
        [
            ('Admin',     '/admin/**',     'Dashboard, Clients, Prets, Comptes, Validations, Transactions, Remboursements, Rapports, Notifications, Agents, Directeurs, Agences, Parametres'),
            ('Directeur', '/directeur/**', 'Dashboard, Clients (CRUD), Agents (toggle), Validations, Comptes, Prets, Remboursements, Rapports, Guichet'),
            ('Agent',     '/agent/**',     'Dashboard, Mes Clients, Guichet, Demandes Pret, Remboursements, Mon Profil'),
            ('Client',    '/client/**',    'Mon Espace, Mes Comptes, Mes Prets, Transactions, Remboursements, Simulateur'),
        ]
    )

    add_heading(doc, '10.2 Fonctionnalites Cles du Directeur', 2)
    for f in [
        "Vue consolidee des clients de son agence (via agents assignes)",
        "Creation de nouveaux clients avec assignation d'agence",
        "Modification et changement de statut des clients",
        "Toggle actif/inactif des agents de son agence",
        "Validation des demandes de compte (EN_ATTENTE -> ACTIF ou REJETE)",
        "Enregistrement de paiements de remboursement (-> PENDING_VALIDATION pour l'admin)",
        "Operations guichet : Depot, Retrait, Virement pour les clients de l'agence",
        "Rapports financiers filtres par agence",
    ]:
        add_bullet(doc, f)

    doc.add_page_break()

    # ── 11. DEPLOIEMENT ─────────────────────────────────────────────────────
    add_heading(doc, '11. Deploiement et Infrastructure', 1)
    add_para(doc,
        "L'ensemble du systeme est conteneurise via Docker et orchestre par Docker Compose. "
        "Chaque service dispose de son propre Dockerfile multi-etapes (build Maven + image JRE legere). "
        "Le frontend utilise Nginx pour servir les fichiers React buildes.")

    print("  -> Generation Diagramme Deploiement...")
    add_image(doc, draw_deployment(), 6.5,
              'Figure 13 — Architecture de Deploiement Docker Compose')

    add_heading(doc, "11.1 Variables d'Environnement (Docker Secrets)", 2)
    add_table(doc,
        ['Variable', 'Usage'],
        [
            ('JWT_SECRET',                        'Cle HMAC-SHA256 partagee (256 bits) pour signer les JWT'),
            ('DB_USERNAME / DB_PASSWORD',         'Credentials PostgreSQL'),
            ('RABBITMQ_USERNAME / PASSWORD',      'Credentials RabbitMQ'),
            ('MAIL_HOST / PORT / USERNAME / PASSWORD', 'Serveur SMTP pour emails'),
            ('CAMPAY_USERNAME / PASSWORD',        'Credentials API CamPay (Mobile Money)'),
            ('TWILIO_ACCOUNT_SID / AUTH_TOKEN',   'Credentials Twilio pour SMS'),
            ('GOOGLE_DRIVE_ROOT_FOLDER_ID',       'Dossier Google Drive pour KYC documents'),
            ('ADMIN_EMAIL / ADMIN_PASSWORD',      'Compte admin cree au demarrage'),
        ]
    )

    add_heading(doc, '11.2 Ordre de Demarrage', 2)
    add_para(doc, "Les services respectent un ordre de demarrage via les conditions healthcheck de Docker Compose :")
    for s in [
        "1. Infrastructure : postgres, rabbitmq, redis",
        "2. Spring Cloud Core : config-service -> registry-service",
        "3. API Gateway",
        "4. Services metier (parallele) : auth, client, account, agency, loan, repayment, reporting, configuration, transaction, notification",
        "5. Frontend : app-frontend",
    ]:
        add_bullet(doc, s)

    doc.add_page_break()

    # ── 12. CONCLUSION ──────────────────────────────────────────────────────
    add_heading(doc, '12. Conclusion et Perspectives', 1)
    add_para(doc,
        "MicrofinanceHub demontre la faisabilite de la construction d'une application "
        "financiere complete selon les principes des architectures microservices modernes. "
        "Le projet integre les meilleures pratiques du developpement logiciel contemporain : "
        "separation des responsabilites, communication asynchrone, securite par defaut, "
        "et deploiement conteneurise.")

    add_heading(doc, 'Points Forts', 2)
    for s in [
        "Architecture scalable : chaque service peut etre mis a l'echelle independamment",
        "Securite multicouche : JWT RBAC + BCrypt + Redis sessions + Rate Limiting",
        "Resilience : communication asynchrone RabbitMQ, gestion des erreurs Feign",
        "Experience utilisateur : interfaces adaptees a chaque role, actions contextuelles",
        "Integration tiers : CamPay (Mobile Money), Twilio (SMS), Google Drive (KYC)",
        "Observabilite : logs structures, Spring Actuator health checks",
        "Portabilite : deploiement complet en une commande Docker Compose",
    ]:
        add_bullet(doc, s)

    add_heading(doc, "Perspectives d'Evolution", 2)
    for e in [
        "Migration vers Kubernetes pour l'orchestration en production",
        "Centralisation des logs avec ELK Stack (Elasticsearch, Logstash, Kibana)",
        "Mise en place de Keycloak pour la gestion centralisee des identites (SSO)",
        "Circuit Breaker (Resilience4j) pour la tolerance aux pannes entre services",
        "Application mobile native (React Native / Flutter)",
        "Intelligence artificielle pour la notation de credit (ML credit scoring)",
        "Audit trail complet et conformite reglementaire",
        "API publique documentee (OpenAPI 3.0) pour partenaires tiers",
    ]:
        add_bullet(doc, e)

    doc.add_paragraph(); doc.add_paragraph()
    p_final = doc.add_paragraph()
    p_final.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run_final = p_final.add_run('— MicrofinanceHub — Rapport de Presentation —')
    run_final.font.size = Pt(10)
    run_final.font.italic = True
    run_final.font.color.rgb = RGBColor(0x06, 0x4e, 0x3b)

    output_path = '/home/axelle-mbadi/IdeaProjects/MicrofinanceHub/Rapport_MicrofinanceHub.docx'
    doc.save(output_path)
    print(f"\nRapport genere : {output_path}")
    return output_path

if __name__ == '__main__':
    print("Generation du rapport MicrofinanceHub...")
    path = make_report()
    print(f"Fichier : {path}")
