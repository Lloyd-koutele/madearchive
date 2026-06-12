import { useState, useEffect } from 'react';
import { getAllTypeDocuments, deleteTypeDocument} from '../services/document/TypedocumentService';
import type { TypeDocumentDto } from '../services/document/TypedocumentService';
import TypeDocumentDetail from './Typedocumentdetail';
import UpdateTypeDocument from './Updatetypedocument';
import Modal from '../Page/Modal';
import Confirme from '../Page/Confirme';
import '../Style/document/Typedocument.css';

interface TypeDocumentListProps {
    refreshTrigger?: number;
}

function TypeDocumentList({ refreshTrigger }: TypeDocumentListProps) {
    const [typeDocuments, setTypeDocuments] = useState<TypeDocumentDto[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    const [viewingTd, setViewingTd] = useState<TypeDocumentDto | null>(null);
    const [isViewModalOpen, setIsViewModalOpen] = useState(false);

    const [editingTd, setEditingTd] = useState<TypeDocumentDto | null>(null);
    const [isUpdateModalOpen, setIsUpdateModalOpen] = useState(false);

    const [confirmOpen, setConfirmOpen] = useState(false);
    const [tdToDelete, setTdToDelete] = useState<TypeDocumentDto | null>(null);
    const [deleteInProgress, setDeleteInProgress] = useState(false);

    useEffect(() => { fetchAll(); }, [refreshTrigger]);

    useEffect(() => {
        if (error || success) {
            const t = setTimeout(() => { setError(''); setSuccess(''); }, 3000);
            return () => clearTimeout(t);
        }
    }, [error, success]);

    const fetchAll = async () => {
        setIsLoading(true);
        setError('');
        try {
            const data = await getAllTypeDocuments();
            setTypeDocuments(data);
        } catch (err: any) {
            setError(err.message || "Erreur lors du chargement");
        } finally {
            setIsLoading(false);
        }
    };

    const handleDeleteRequest = (td: TypeDocumentDto) => {
        setTdToDelete(td);
        setConfirmOpen(true);
    };

    const handleDeleteConfirm = async () => {
        if (!tdToDelete?.id) return;
        setDeleteInProgress(true);
        try {
            await deleteTypeDocument(tdToDelete.id);
            setSuccess(`"${tdToDelete.nom}" supprimé avec succès`);
            await fetchAll();
        } catch (err: any) {
            setError(err.message || "Erreur lors de la suppression");
        } finally {
            setDeleteInProgress(false);
            setConfirmOpen(false);
            setTdToDelete(null);
        }
    };

    const handleEditSuccess = async () => {
        setIsUpdateModalOpen(false);
        setEditingTd(null);
        setSuccess("Type de document mis à jour avec succès");
        await fetchAll();
    };

    const handleCloseModals = () => {
        setIsViewModalOpen(false);
        setIsUpdateModalOpen(false);
        setViewingTd(null);
        setEditingTd(null);
    };

    return (
        <div className="td-list-wrapper">

            {error && <div className="td-alert td-alert-error">{error}</div>}
            {success && <div className="td-alert td-alert-success">{success}</div>}

            {isLoading ? (
                <div className="td-loading">Chargement...</div>
            ) : typeDocuments.length === 0 ? (
                <div className="td-empty">
                    <p>Aucun type de document créé.</p>
                    <span>Utilisez le bouton "Créer un type" pour commencer.</span>
                </div>
            ) : (
                <div className="td-table-container">
                    <table className="td-table">
                        <thead>
                            <tr>
                                <th>Nom</th>
                                <th>Rétention (ans)</th>
                                <th>Période de grâce (j)</th>
                                <th>Métadonnées</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {typeDocuments.map(td => (
                                <tr key={td.id}>
                                    <td className="td-nom">{td.nom}</td>
                                    <td>{td.retentionYears ?? '—'}</td>
                                    <td>{td.periodGrace ?? '—'}</td>
                                    <td>
                                        <span className="td-meta-count">
                                            {td.metaData?.length ?? 0} champ{(td.metaData?.length ?? 0) > 1 ? 's' : ''}
                                        </span>
                                    </td>
                                    <td>
                                        <div className="td-actions">
                                            <button
                                                className="action-button view"
                                                onClick={() => { setViewingTd(td); setIsViewModalOpen(true); }}
                                            >
                                                Voir
                                            </button>
                                            <button
                                                className="action-button edit"
                                                onClick={() => { setEditingTd(td); setIsUpdateModalOpen(true); }}
                                            >
                                                Modifier
                                            </button>
                                            <button
                                                className="td-delete-btn"
                                                onClick={() => handleDeleteRequest(td)}
                                                disabled={deleteInProgress}
                                            >
                                                Supprimer
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            <Modal isOpen={isViewModalOpen} onClose={handleCloseModals} title="Détail du type de document">
                {viewingTd && <TypeDocumentDetail td={viewingTd} />}
            </Modal>

            <Modal isOpen={isUpdateModalOpen} onClose={handleCloseModals} title="Modifier le type de document">
                {editingTd && (
                    <UpdateTypeDocument
                        initialData={editingTd}
                        onsuccess={handleEditSuccess}
                    />
                )}
            </Modal>

            <Confirme
                isOpen={confirmOpen}
                message={`Supprimer le type "${tdToDelete?.nom}" ? Cette action est irréversible.`}
                onConfirm={handleDeleteConfirm}
                onCancel={() => { setConfirmOpen(false); setTdToDelete(null); }}
            />
        </div>
    );
}

export default TypeDocumentList;