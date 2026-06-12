import React, { useState, useEffect } from 'react';
import { updateTypeDocument } from '../services/document/TypedocumentService';
import type { MetaDataDto, MetaDataType, TypeDocumentDto } from '../services/document/TypedocumentService';
import { getCurrentUserInfo } from '../auth/authService';
import '../Style/document/Typedocument.css';

interface UpdateTypeDocumentProps {
    initialData: TypeDocumentDto;
    onsuccess?: () => void;
}

const META_TYPES: MetaDataType[] = ['CHAR', 'STRING', 'INTEGER', 'FLOAT', 'DOUBLE', 'BOOLEAN', 'DATE', 'TEXT'];

const emptyMeta = (): MetaDataDto => ({ nom: '', obligatoire: false, metaDataType: 'STRING' });

function UpdateTypeDocument({ initialData, onsuccess }: UpdateTypeDocumentProps) {
    const [nom, setNom] = useState('');
    const [retentionYears, setRetentionYears] = useState<number>(1);
    const [periodGrace, setPeriodGrace] = useState<number>(30);
    const [metaData, setMetaData] = useState<MetaDataDto[]>([emptyMeta()]);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    // Pré-remplissage depuis initialData
    useEffect(() => {
        if (initialData) {
            setNom(initialData.nom || '');
            setRetentionYears(initialData.retentionYears ?? 1);
            setPeriodGrace(initialData.periodGrace ?? 30);
            setMetaData(
                initialData.metaData && initialData.metaData.length > 0
                    ? initialData.metaData.map(m => ({
                        id: m.id,
                        nom: m.nom,
                        obligatoire: m.obligatoire,
                        metaDataType: m.metaDataType
                    }))
                    : [emptyMeta()]
            );
        }
    }, [initialData]);

    const handleMetaChange = (index: number, field: keyof MetaDataDto, value: any) => {
        const updated = [...metaData];
        updated[index] = { ...updated[index], [field]: value };
        setMetaData(updated);
    };

    const addMeta = () => setMetaData([...metaData, emptyMeta()]);

    const removeMeta = (index: number) => {
        if (metaData.length === 1) return;
        setMetaData(metaData.filter((_, i) => i !== index));
    };

    const validate = (): boolean => {
        if (!nom.trim()) { setError("Le nom du type de document est obligatoire"); return false; }
        if (retentionYears < 1) { setError("La durée de rétention doit être d'au moins 1 an"); return false; }
        for (const m of metaData) {
            if (!m.nom.trim()) { setError("Chaque métadonnée doit avoir un nom"); return false; }
        }
        setError('');
        return true;
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setSuccess('');
        if (!validate()) return;

        const userInfo = getCurrentUserInfo();
        if (!userInfo?.id) { setError("Session expirée, veuillez vous reconnecter"); return; }
        if (!initialData.id) { setError("ID du type de document manquant"); return; }

        setIsLoading(true);
        try {
            const dto: TypeDocumentDto = {
                nom: nom.trim(),
                retentionYears,
                periodGrace,
                userId: userInfo.id,
                metaData
            };
            await updateTypeDocument(initialData.id, dto);
            setSuccess("Type de document mis à jour avec succès");
            setTimeout(() => onsuccess?.(), 1800);
        } catch (err: any) {
            setError(err.message || "Erreur lors de la mise à jour");
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="td-form-wrapper">
            {error && <div className="td-alert td-alert-error">{error}</div>}
            {success && <div className="td-alert td-alert-success">{success}</div>}

            <form onSubmit={handleSubmit}>

                {/* Infos générales */}
                <div className="td-section">
                    <h3 className="td-section-title">Informations générales</h3>
                    <div className="td-form-grid">
                        <div className="form-field td-span2">
                            <input
                                id="tdu-nom"
                                type="text"
                                className="form-field-input"
                                placeholder=" "
                                value={nom}
                                onChange={e => setNom(e.target.value)}
                                required
                            />
                            <label htmlFor="tdu-nom">Nom du type de document</label>
                        </div>
                        <div className="form-field">
                            <input
                                id="tdu-retention"
                                type="number"
                                className="form-field-input"
                                placeholder=" "
                                min={1}
                                value={retentionYears}
                                onChange={e => setRetentionYears(Number(e.target.value))}
                                required
                            />
                            <label htmlFor="tdu-retention">Durée de rétention (années)</label>
                        </div>
                        <div className="form-field">
                            <input
                                id="tdu-grace"
                                type="number"
                                className="form-field-input"
                                placeholder=" "
                                min={0}
                                value={periodGrace}
                                onChange={e => setPeriodGrace(Number(e.target.value))}
                            />
                            <label htmlFor="tdu-grace">Période de grâce (jours)</label>
                        </div>
                    </div>
                </div>

                {/* Métadonnées */}
                <div className="td-section">
                    <div className="td-section-header">
                        <h3 className="td-section-title">Métadonnées</h3>
                        <button type="button" className="td-add-btn" onClick={addMeta}>
                            + Ajouter un champ
                        </button>
                    </div>

                    <div className="td-meta-list">
                        {metaData.map((meta, index) => (
                            <div key={index} className="td-meta-row">
                                <div className="td-meta-index">{index + 1}</div>

                                <div className="form-field td-meta-nom">
                                    <input
                                        type="text"
                                        id={`meta-nom-${index}`}
                                        className="form-field-input"
                                        placeholder=" "
                                        value={meta.nom}
                                        onChange={e => handleMetaChange(index, 'nom', e.target.value)}
                                        required
                                    />
                                    <label htmlFor={`meta-nom-${index}`}>Nom du champ</label>
                                </div>

                                <select
                                    className="td-select"
                                    value={meta.metaDataType}
                                    onChange={e => handleMetaChange(index, 'metaDataType', e.target.value as MetaDataType)}
                                    aria-label="Type de la métadonnée"
                                >
                                    {META_TYPES.map(t => (
                                        <option key={t} value={t}>{t}</option>
                                    ))}
                                </select>

                                <label className="td-toggle" aria-label="Champ obligatoire">
                                    <input
                                        type="checkbox"
                                        checked={meta.obligatoire}
                                        onChange={e => handleMetaChange(index, 'obligatoire', e.target.checked)}
                                    />
                                    <span className="td-toggle-track">
                                        <span className="td-toggle-thumb" />
                                    </span>
                                    <span className="td-toggle-label">Obligatoire</span>
                                </label>

                                <button
                                    type="button"
                                    className="td-remove-btn"
                                    onClick={() => removeMeta(index)}
                                    disabled={metaData.length === 1}
                                    aria-label="Supprimer ce champ"
                                >
                                    ✕
                                </button>
                            </div>
                        ))}
                    </div>
                </div>

                <button type="submit" className="form-submit-btn td-submit" disabled={isLoading}>
                    {isLoading ? 'Mise à jour en cours...' : 'Mettre à jour'}
                </button>
            </form>
        </div>
    );
}

export default UpdateTypeDocument;