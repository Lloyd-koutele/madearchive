import { useState, useEffect } from 'react';
import Sidebar from '../Page/Sidebar';
import Profile from '../Page/Profil';
import Modal from '../Page/Modal';
import type { SearchResultItemDto, SearchResultDto } from '../services/document/DocumentUserService';
import { searchDocuments } from '../services/document/DocumentUserService';
import type { TypeDocumentDto } from '../services/document/TypedocumentService';
import { getAllTypeDocuments } from '../services/document/TypedocumentService';
import { getCurrentUserInfo } from '../auth/authService';
import '../style/User/User.css';

type UserView = 'documents' | 'search' | 'profile';

const STATUS_LABELS: Record<string, string> = {
    ACTIVE: 'Actif',
    PENDING: 'En attente',
    ACTIVE_WARNING: 'Avertissement',
    CORRUPTED: 'Corrompu',
    DELETED: 'Supprimé'
};

// ─── Composant détail document ───
function DocumentDetail({ doc, onClose }: { doc: SearchResultItemDto; onClose: () => void }) {
    return (
        <div className="doc-detail">
            <div className="details-row"><strong>Titre :</strong> {doc.titre}</div>
            <div className="details-row"><strong>Type :</strong> {doc.typeDocument}</div>
            <div className="details-row">
                <strong>Accès :</strong>
                <span className={`doc-access-tag ${doc.access === 'PUBLIC' ? 'public' : 'prive'}`}>
                    {doc.access === 'PUBLIC' ? 'Public' : 'Privé'}
                </span>
            </div>
            <div className="details-row">
                <strong>Statut :</strong>
                <span className={`status-tag ${doc.status === 'ACTIVE' ? 'active' : 'inactive'}`}>
                    {STATUS_LABELS[doc.status] || doc.status}
                </span>
            </div>
            <div className="details-row">
                <strong>Rétention jusqu'au :</strong> {doc.retentionUntil}
            </div>
            <div className="details-row">
                <strong>ID :</strong>
                <span className="up-hash">{doc.documentId}</span>
            </div>
            <button className="details-close-btn" onClick={onClose} style={{ marginTop: '1.25rem' }}>
                Fermer
            </button>
        </div>
    );
}

// ─── Tableau documents réutilisable ───
function DocumentTable({
    documents,
    onViewDetail
}: {
    documents: SearchResultItemDto[];
    onViewDetail: (doc: SearchResultItemDto) => void;
}) {
    if (documents.length === 0) {
        return (
            <div className="td-empty">
                <p>Aucun document trouvé.</p>
            </div>
        );
    }

    return (
        <div className="td-table-container">
            <table className="td-table">
                <thead>
                    <tr>
                        <th>Titre</th>
                        <th>Type</th>
                        <th>Accès</th>
                        <th>Statut</th>
                        <th>Rétention</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    {documents.map(doc => (
                        <tr key={doc.documentId}>
                            <td className="td-nom">{doc.titre}</td>
                            <td>{doc.typeDocument}</td>
                            <td>
                                <span className={`doc-access-tag ${doc.access === 'PUBLIC' ? 'public' : 'prive'}`}>
                                    {doc.access === 'PUBLIC' ? 'Public' : 'Privé'}
                                </span>
                            </td>
                            <td>
                                <span className={`status-tag ${doc.status === 'ACTIVE' ? 'active' : 'inactive'}`}>
                                    {STATUS_LABELS[doc.status] || doc.status}
                                </span>
                            </td>
                            <td>{doc.retentionUntil}</td>
                            <td>
                                <button
                                    className="action-button view"
                                    onClick={() => onViewDetail(doc)}
                                >
                                    Voir
                                </button>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

// ─── Mes documents ───
function MesDocumentsUser({ onViewDetail }: { onViewDetail: (doc: SearchResultItemDto) => void }) {
    const [documents, setDocuments] = useState<SearchResultItemDto[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const [page, setPage] = useState(1);
    const [totalPages, setTotalPages] = useState(1);
    const [totalHits, setTotalHits] = useState(0);

    useEffect(() => { fetchDocs(1); }, []);

    const fetchDocs = async (p: number) => {
        setIsLoading(true);
        setError('');
        try {
            const result = await searchDocuments({ query: '', page: p, hitsPerPage: 10 });
            setDocuments(result.results);
            setTotalHits(result.totalHits);
            setTotalPages(result.totalPages);
            setPage(p);
        } catch (err: any) {
            setError(err.message || "Erreur chargement");
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="mes-docs-wrapper">
            <div className="mes-docs-header">
                <h2 className="mes-docs-title">Mes documents</h2>
            </div>
            {error && <div className="up-alert up-alert-error">{error}</div>}
            {isLoading ? (
                <div className="td-loading">Chargement...</div>
            ) : (
                <>
                    <p className="users-count">
                        <span>{totalHits}</span> document{totalHits > 1 ? 's' : ''}
                    </p>
                    <DocumentTable documents={documents} onViewDetail={onViewDetail} />
                    {totalPages > 1 && (
                        <div className="pagination">
                            <button className="pagination-btn pagination-nav" onClick={() => fetchDocs(page - 1)} disabled={page === 1}>‹</button>
                            <span className="pagination-btn pagination-active">{page} / {totalPages}</span>
                            <button className="pagination-btn pagination-nav" onClick={() => fetchDocs(page + 1)} disabled={page === totalPages}>›</button>
                        </div>
                    )}
                </>
            )}
        </div>
    );
}

// ─── Recherche ───
function RechercheDocuments({ onViewDetail }: { onViewDetail: (doc: SearchResultItemDto) => void }) {
    const [query, setQuery] = useState('');
    const [typeDocumentId, setTypeDocumentId] = useState<number | ''>('');
    const [typeDocuments, setTypeDocuments] = useState<TypeDocumentDto[]>([]);
    const [result, setResult] = useState<SearchResultDto | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    const [page, setPage] = useState(1);

    useEffect(() => {
        getAllTypeDocuments().then(setTypeDocuments).catch(() => {});
    }, []);

    const handleSearch = async (p = 1) => {
        if (!query.trim()) return;
        setIsLoading(true);
        setError('');
        try {
            const res = await searchDocuments({
                query: query.trim(),
                typeDocumentId: typeDocumentId || null,
                page: p,
                hitsPerPage: 10
            });
            setResult(res);
            setPage(p);
        } catch (err: any) {
            setError(err.message || "Erreur lors de la recherche");
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="mes-docs-wrapper">
            <div className="mes-docs-header">
                <h2 className="mes-docs-title">Recherche</h2>
            </div>

            {/* Barre de recherche */}
            <div className="search-bar">
                <input
                    type="text"
                    className="filter-input search-input"
                    placeholder="Recherche plein texte..."
                    value={query}
                    onChange={e => setQuery(e.target.value)}
                    onKeyDown={e => e.key === 'Enter' && handleSearch(1)}
                />
                <select
                    className="filter-input search-type-select"
                    value={typeDocumentId}
                    onChange={e => setTypeDocumentId(e.target.value ? Number(e.target.value) : '')}
                    aria-label="Filtrer par type"
                >
                    <option value="">Tous les types</option>
                    {typeDocuments.map(td => (
                        <option key={td.id} value={td.id}>{td.nom}</option>
                    ))}
                </select>
                <button
                    className="form-submit-btn mes-docs-search-btn"
                    onClick={() => handleSearch(1)}
                    disabled={isLoading || !query.trim()}
                >
                    {isLoading
                        ? <i className="fa-solid fa-spinner fa-spin"></i>
                        : <i className="fa-solid fa-magnifying-glass"></i>
                    }
                </button>
            </div>

            {error && <div className="up-alert up-alert-error">{error}</div>}

            {result && (
                <>
                    <p className="users-count">
                        <span>{result.totalHits}</span> résultat{result.totalHits > 1 ? 's' : ''}
                    </p>
                    <DocumentTable documents={result.results} onViewDetail={onViewDetail} />
                    {result.totalPages > 1 && (
                        <div className="pagination">
                            <button className="pagination-btn pagination-nav" onClick={() => handleSearch(page - 1)} disabled={page === 1}>‹</button>
                            <span className="pagination-btn pagination-active">{page} / {result.totalPages}</span>
                            <button className="pagination-btn pagination-nav" onClick={() => handleSearch(page + 1)} disabled={page === result.totalPages}>›</button>
                        </div>
                    )}
                </>
            )}
        </div>
    );
}

// ─── UserDashboard principal ───
function UserDashboard() {
    const userInfo = getCurrentUserInfo();
    const [currentView, setCurrentView] = useState<UserView>('documents');
    const [detailDoc, setDetailDoc] = useState<SearchResultItemDto | null>(null);
    const [isDetailOpen, setIsDetailOpen] = useState(false);

    const handleViewDetail = (doc: SearchResultItemDto) => {
        setDetailDoc(doc);
        setIsDetailOpen(true);
    };

    return (
        <div className="admin-dashboard">
            <div className="admin-body">

                <Sidebar title={userInfo?.role || "UTILISATEUR"}>
                    <nav className="sidebar-nav">
                        <div>
                            <div className="main-header">
                                <button
                                    onClick={() => setCurrentView('profile')}
                                    className={`sidebar-btn ${currentView === 'profile' ? 'active-tab' : ''}`}
                                >
                                    👤 Mon Profil
                                </button>
                            </div>

                            <div className="sidebar-section-label">Documents</div>
                            <div className="main-header">
                                <button
                                    onClick={() => setCurrentView('documents')}
                                    className={`sidebar-btn ${currentView === 'documents' ? 'active-tab' : ''}`}
                                >
                                    Mes documents
                                </button>
                            </div>
                            <div className="main-header">
                                <button
                                    onClick={() => setCurrentView('search')}
                                    className={`sidebar-btn ${currentView === 'search' ? 'active-tab' : ''}`}
                                >
                                    Recherche
                                </button>
                            </div>
                        </div>
                    </nav>
                </Sidebar>

                <div className="main-content">
                    {currentView === 'profile' && <Profile userId={userInfo?.id} />}
                    {currentView === 'documents' && <MesDocumentsUser onViewDetail={handleViewDetail} />}
                    {currentView === 'search' && <RechercheDocuments onViewDetail={handleViewDetail} />}
                </div>

                <Modal
                    isOpen={isDetailOpen}
                    onClose={() => setIsDetailOpen(false)}
                    title="Détail du document"
                >
                    {detailDoc && (
                        <DocumentDetail
                            doc={detailDoc}
                            onClose={() => setIsDetailOpen(false)}
                        />
                    )}
                </Modal>
            </div>
        </div>
    );
}

export default UserDashboard;