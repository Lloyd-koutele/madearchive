import React, { useState, useEffect } from 'react';
import { uploadBulkSameType } from '../services/document/DocumentService';
import  { getAllTypeDocuments } from '../services/document/TypedocumentService';
import type { BulkUploadReportDto } from '../services/document/DocumentService';
import type { TypeDocumentDto } from '../services/document/TypedocumentService';
import { getCurrentUserInfo } from '../auth/authService';
import '../Style/Editor/Editor.css';

interface UploadBulkSameTypeProps {
    onsuccess?: (report: BulkUploadReportDto) => void;
}

function UploadBulkSameType({ onsuccess }: UploadBulkSameTypeProps) {
    const [files, setFiles] = useState<File[]>([]);
    const [typeDocuments, setTypeDocuments] = useState<TypeDocumentDto[]>([]);
    const [typeDocumentId, setTypeDocumentId] = useState<number | ''>('');
    const [isDragging, setIsDragging] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    const [report, setReport] = useState<BulkUploadReportDto | null>(null);

    useEffect(() => {
        getAllTypeDocuments()
            .then(setTypeDocuments)
            .catch(() => setError("Impossible de charger les types de documents"));
    }, []);

    const addFiles = (newFiles: FileList | null) => {
        if (!newFiles) return;
        const arr = Array.from(newFiles);
        setFiles(prev => {
            const existing = prev.map(f => f.name);
            return [...prev, ...arr.filter(f => !existing.includes(f.name))];
        });
    };

    const removeFile = (name: string) =>
        setFiles(prev => prev.filter(f => f.name !== name));

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        if (files.length === 0) { setError("Ajoutez au moins un fichier"); return; }
        if (!typeDocumentId) { setError("Choisissez un type de document"); return; }

        const userInfo = getCurrentUserInfo();
        if (!userInfo?.id) { setError("Session expirée"); return; }

        setIsLoading(true);
        try {
            const rep = await uploadBulkSameType(files, typeDocumentId as number, userInfo.id);
            setReport(rep);
            setFiles([]);
            onsuccess?.(rep);
        } catch (err: any) {
            setError(err.message || "Erreur lors de l'upload en masse");
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="upload-wrapper">
            {error && <div className="up-alert up-alert-error">{error}</div>}

            <form onSubmit={handleSubmit}>

                {/* Zone de dépôt multi-fichiers */}
                <div
                    className={`drop-zone ${isDragging ? 'dragging' : ''}`}
                    onDragOver={e => { e.preventDefault(); setIsDragging(true); }}
                    onDragLeave={() => setIsDragging(false)}
                    onDrop={e => { e.preventDefault(); setIsDragging(false); addFiles(e.dataTransfer.files); }}
                    onClick={() => document.getElementById('bulk-files-input')?.click()}
                >
                    <input
                        id="bulk-files-input"
                        type="file"
                        multiple
                        className="sr-only"
                        aria-label="Sélectionner les fichiers à téléverser"
                        onChange={e => addFiles(e.target.files)}
                    />
                    <div className="drop-zone-placeholder">
                        <i className="fa-solid fa-files drop-icon-lg"></i>
                        <p>Glissez vos fichiers ici</p>
                        <span>ou cliquez pour parcourir (sélection multiple)</span>
                    </div>
                </div>

                {/* Liste des fichiers sélectionnés */}
                {files.length > 0 && (
                    <div className="bulk-file-list">
                        <p className="bulk-file-count">
                            <strong>{files.length}</strong> fichier{files.length > 1 ? 's' : ''} sélectionné{files.length > 1 ? 's' : ''}
                        </p>
                        <div className="bulk-file-items">
                            {files.map(f => (
                                <div key={f.name} className="bulk-file-item">
                                    <i className="fa-solid fa-file bulk-file-icon"></i>
                                    <span className="bulk-file-name">{f.name}</span>
                                    <span className="bulk-file-size">
                                        {(f.size / 1024).toFixed(1)} Ko
                                    </span>
                                    <button
                                        type="button"
                                        className="td-remove-btn"
                                        onClick={() => removeFile(f.name)}
                                        aria-label="Retirer"
                                    >✕</button>
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {/* Type de document */}
                <div className="upload-options">
                    <div className="form-field">
                        <select
                            className="form-field-input up-select"
                            value={typeDocumentId}
                            onChange={e => setTypeDocumentId(Number(e.target.value))}
                            required
                            aria-label="Type de document"
                        >
                            <option value="">-- Type de document commun --</option>
                            {typeDocuments.map(td => (
                                <option key={td.id} value={td.id}>{td.nom}</option>
                            ))}
                        </select>
                    </div>
                </div>

                <button type="submit" className="form-submit-btn up-submit" disabled={isLoading || files.length === 0}>
                    {isLoading
                        ? <><i className="fa-solid fa-spinner fa-spin"></i> Upload en cours...</>
                        : <><i className="fa-solid fa-upload"></i> Uploader {files.length > 0 ? `${files.length} fichier${files.length > 1 ? 's' : ''}` : ''}</>
                    }
                </button>
            </form>

            {/* Rapport */}
            {report && <BulkReport report={report} />}
        </div>
    );
}

function BulkReport({ report }: { report: BulkUploadReportDto }) {
    return (
        <div className="bulk-report">
            <div className="bulk-report-summary">
                <div className="bulk-stat total">
                    <span className="bulk-stat-value">{report.total}</span>
                    <span className="bulk-stat-label">Total</span>
                </div>
                <div className="bulk-stat success">
                    <span className="bulk-stat-value">{report.success}</span>
                    <span className="bulk-stat-label">Succès</span>
                </div>
                <div className="bulk-stat failed">
                    <span className="bulk-stat-value">{report.failed}</span>
                    <span className="bulk-stat-label">Échecs</span>
                </div>
            </div>
            {report.details.length > 0 && (
                <div className="bulk-report-details">
                    {report.details.map((item, i) => (
                        <div key={i} className={`bulk-report-item ${item.status === 'SUCCESS' ? 'ok' : 'ko'}`}>
                            <i className={`fa-solid ${item.status === 'SUCCESS' ? 'fa-check' : 'fa-xmark'}`}></i>
                            <span className="bulk-item-name">{item.nomFichier}</span>
                            {item.erreur && <span className="bulk-item-error">{item.erreur}</span>}
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

export { BulkReport };
export default UploadBulkSameType;