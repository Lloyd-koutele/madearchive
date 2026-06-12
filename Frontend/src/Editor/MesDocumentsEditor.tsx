import { useState, useEffect } from 'react';
import { searchDocuments } from '../services/document/DocumentUserService';
import type {  SearchResultItemDto } from '../services/document/DocumentUserService';
import Modal from '../Page/Modal';
import GestionGroupe from '../document/GestionGroupe';
import '../Style/Editor/Editor.css';

interface MesDocumentsEditorProps {
    refreshTrigger?: number;
}

const STATUS_LABELS: Record<string, string> = {
    ACTIVE: 'Actif',
    PENDING: 'En attente',
    ACTIVE_WARNING: 'Avertissement',
    CORRUPTED: 'Corrompu',
    DELETED: 'Supprimé'
};

function MesDocumentsEditor({ refreshTrigger }: MesDocumentsEditorProps) {
    const [documents, setDocuments] = useState<SearchResultItemDto[]>([]);
    const [query, setQuery] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    const [totalHits, setTotalHits] = useState(0);
    const [page, setPage] = useState(1);
    const [totalPages, setTotalPages] = useState(1);

    const [groupeDocId, setGroupeDocId] = useState<string | null>(null);
    const [groupeDocTitre, setGroupeDocTitre] = useState('');
    const [isGroupeModalOpen, setIsGroupeModalOpen] = useState(false);

    useEffect(() => {
        fetchDocuments(1);
    }, [refreshTrigger]);

    const fetchDocuments = async (p: number) => {
        setIsLoading(true);
        setError('');
        try {
            const result = await searchDocuments({
                query: query.trim() || '',
                page: p,
                hitsPerPage: 10
            });
            setDocuments(result.results);
            setTotalHits(result.totalHits);
            setTotalPages(result.totalPages);
            setPage(p);
        } catch (err: any) {
            setError(err.message || "Erreur lors du chargement");
        } finally {
            setIsLoading(false);
        }
    };

    const handleSearch = (e: React.FormEvent) => {
        e.preventDefault();
        fetchDocuments(1);
    };

    const openGroupe = (doc: SearchResultItemDto) => {
        setGroupeDocId(doc.documentId);
        setGroupeDocTitre(doc.titre);
        setIsGroupeModalOpen(true);
    };

    return (
        <div className="mes-docs-wrapper">
            <div className="mes-docs-header">
                <h2 className="mes-docs-title">Mes documents</h2>
                <form className="mes-docs-search" onSubmit={handleSearch}>
                    <input
                        type="text"
                        className="filter-input"
                        placeholder="Rechercher dans mes documents..."
                        value={query}
                        onChange={e => setQuery(e.target.value)}
                    />
                    <button 
                        type="submit" 
                        className="form-submit-btn mes-docs-search-btn"
                        aria-label='Lancer la recherche'>
                        <i className="fa-solid fa-magnifying-glass"></i>
                    </button>
                </form>
            </div>

            {error && <div className="up-alert up-alert-error">{error}</div>}

            {isLoading ? (
                <div className="td-loading">Chargement...</div>
            ) : documents.length === 0 ? (
                <div className="td-empty">
                    <p>Aucun document trouvé.</p>
                    <span>Uploadez votre premier document via le menu de gauche.</span>
                </div>
            ) : (
                <>
                    <p className="users-count">
                        <span>{totalHits}</span> document{totalHits > 1 ? 's' : ''}
                    </p>
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
                                            <div className="td-actions">
                                                {doc.access === 'PRIVE' && (
                                                    <button
                                                        className="action-button edit"
                                                        onClick={() => openGroupe(doc)}
                                                    >
                                                        Groupe
                                                    </button>
                                                )}
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>

                    {/* Pagination simple */}
                    {totalPages > 1 && (
                        <div className="pagination">
                            <button
                                className="pagination-btn pagination-nav"
                                onClick={() => fetchDocuments(page - 1)}
                                disabled={page === 1}
                            >‹</button>
                            <span className="pagination-btn pagination-active">
                                {page} / {totalPages}
                            </span>
                            <button
                                className="pagination-btn pagination-nav"
                                onClick={() => fetchDocuments(page + 1)}
                                disabled={page === totalPages}
                            >›</button>
                        </div>
                    )}
                </>
            )}

            <Modal
                isOpen={isGroupeModalOpen}
                onClose={() => setIsGroupeModalOpen(false)}
                title="Gestion du groupe d'accès"
            >
                {groupeDocId && (
                    <GestionGroupe
                        documentId={groupeDocId}
                        documentTitre={groupeDocTitre}
                        onClose={() => setIsGroupeModalOpen(false)}
                    />
                )}
            </Modal>
        </div>
    );
}

export default MesDocumentsEditor;