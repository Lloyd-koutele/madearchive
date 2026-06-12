import React, { useState, useEffect } from 'react';
import { uploadDocument } from '../services/document/DocumentService';
import type { DocumentUploadDto, DocumentUploadResultDto } from '../services/document/DocumentService';
import  { getAllTypeDocuments } from '../services/document/TypedocumentService';
import type { TypeDocumentDto } from '../services/document/TypedocumentService';
import { getCurrentUserInfo } from '../auth/authService';
import '../Style/Editor/Editor.css';

interface UploadSimpleProps {
    onsuccess?: (result: DocumentUploadResultDto) => void;
}

function UploadSimple({ onsuccess }: UploadSimpleProps) {
    const [file, setFile] = useState<File | null>(null);
    const [typeDocuments, setTypeDocuments] = useState<TypeDocumentDto[]>([]);
    const [typeDocumentId, setTypeDocumentId] = useState<number | ''>('');
    const [access, setAccess] = useState<'PUBLIC' | 'PRIVE'>('PUBLIC');
    const [integrityLevel, setIntegrityLevel] = useState<'STANDARD' | 'ADVANCED'>('STANDARD');
    const [groupeNom, setGroupeNom] = useState('');
    const [isDragging, setIsDragging] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [result, setResult] = useState<DocumentUploadResultDto | null>(null);

    useEffect(() => {
        getAllTypeDocuments()
            .then(setTypeDocuments)
            .catch(() => setError("Impossible de charger les types de documents"));
    }, []);

    const handleDrop = (e: React.DragEvent) => {
        e.preventDefault();
        setIsDragging(false);
        const dropped = e.dataTransfer.files[0];
        if (dropped) setFile(dropped);
    };

    const validate = (): boolean => {
        if (!file) { setError("Veuillez sélectionner un fichier"); return false; }
        if (!typeDocumentId) { setError("Veuillez choisir un type de document"); return false; }
        if (access === 'PRIVE' && !groupeNom.trim()) {
            setError("Le nom du groupe est obligatoire pour un document privé");
            return false;
        }
        return true;
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(''); setSuccess('');
        if (!validate()) return;

        const userInfo = getCurrentUserInfo();
        if (!userInfo?.id) { setError("Session expirée"); return; }

        setIsLoading(true);
        try {
            const dto: DocumentUploadDto = {
                titre: file!.name,
                access,
                typeDocumentId: typeDocumentId as number,
                uploadedById: userInfo.id,
                integrityLevel,
                ...(access === 'PRIVE' && { groupeNom })
            };
            const res = await uploadDocument(file!, dto);
            setResult(res);
            setSuccess("Document uploadé avec succès");
            setFile(null);
            onsuccess?.(res);
        } catch (err: any) {
            setError(err.message || "Erreur lors de l'upload");
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="upload-wrapper">
            {error && <div className="up-alert up-alert-error">{error}</div>}
            {success && <div className="up-alert up-alert-success">{success}</div>}

            <form onSubmit={handleSubmit}>

                {/* Zone de dépôt */}
                <div
                    className={`drop-zone ${isDragging ? 'dragging' : ''} ${file ? 'has-file' : ''}`}
                    onDragOver={e => { e.preventDefault(); setIsDragging(true); }}
                    onDragLeave={() => setIsDragging(false)}
                    onDrop={handleDrop}
                    onClick={() => document.getElementById('file-input')?.click()}
                >
                    <input
                        id="file-input"
                        type="file"
                        className="sr-only"
                        aria-label="Sélectionner un fichier à téléverser" // 💡 Ajout ici pour l'accessibilité
                        onChange={e => setFile(e.target.files?.[0] || null)}
                    />
                    {file ? (
                        <div className="drop-zone-file">
                            <i className="fa-solid fa-file-pdf drop-icon"></i>
                            <span className="drop-filename">{file.name}</span>
                            <span className="drop-filesize">
                                {(file.size / 1024 / 1024).toFixed(2)} Mo
                            </span>
                            <button
                                type="button"
                                className="drop-remove"
                                onClick={e => { e.stopPropagation(); setFile(null); }}
                            >
                                ✕ Retirer
                            </button>
                        </div>
                    ) : (
                        <div className="drop-zone-placeholder">
                            <i className="fa-solid fa-cloud-arrow-up drop-icon-lg"></i>
                            <p>Glissez votre fichier ici</p>
                            <span>ou cliquez pour parcourir</span>
                        </div>
                    )}
                </div>

                {/* Options */}
                <div className="upload-options">
                    <div className="form-field">
                        <select
                            className="form-field-input up-select"
                            value={typeDocumentId}
                            onChange={e => setTypeDocumentId(Number(e.target.value))}
                            required
                            aria-label="Type de document"
                        >
                            <option value="">-- Type de document --</option>
                            {typeDocuments.map(td => (
                                <option key={td.id} value={td.id}>{td.nom}</option>
                            ))}
                        </select>
                    </div>

                    <div className="up-row">
                        <div className="form-field">
                            <select
                                className="form-field-input up-select"
                                value={access}
                                onChange={e => setAccess(e.target.value as any)}
                                aria-label="Niveau d'accès"
                            >
                                <option value="PUBLIC">Public</option>
                                <option value="PRIVE">Privé</option>
                            </select>
                        </div>
                        <div className="form-field">
                            <select
                                className="form-field-input up-select"
                                value={integrityLevel}
                                onChange={e => setIntegrityLevel(e.target.value as any)}
                                aria-label="Niveau d'intégrité"
                            >
                                <option value="STANDARD">Standard</option>
                                <option value="ADVANCED">Avancé</option>
                            </select>
                        </div>
                    </div>

                    {access === 'PRIVE' && (
                        <div className="form-field">
                            <input
                                id="groupe-nom"
                                type="text"
                                className="form-field-input"
                                placeholder=" "
                                value={groupeNom}
                                onChange={e => setGroupeNom(e.target.value)}
                                required
                            />
                            <label htmlFor="groupe-nom">Nom du groupe d'accès</label>
                        </div>
                    )}
                </div>

                <button type="submit" className="form-submit-btn up-submit" disabled={isLoading}>
                    {isLoading ? (
                        <><i className="fa-solid fa-spinner fa-spin"></i> Upload en cours...</>
                    ) : (
                        <><i className="fa-solid fa-upload"></i> Uploader le document</>
                    )}
                </button>
            </form>

            {/* Résultat upload */}
            {result && (
                <div className="up-result">
                    <h4 className="up-result-title">Document archivé</h4>
                    <div className="details-row"><strong>ID :</strong> {result.documentId}</div>
                    <div className="details-row"><strong>Statut :</strong>
                        <span className={`status-tag ${result.status === 'ACTIVE' ? 'active' : 'inactive'}`}>
                            {result.status}
                        </span>
                    </div>
                    <div className="details-row">
                        <strong>SHA-256 :</strong>
                        <span className="up-hash">{result.sha256Hash}</span>
                    </div>
                    {result.metaDataSuggestions && Object.keys(result.metaDataSuggestions).length > 0 && (
                        <div className="up-suggestions">
                            <p className="up-suggestions-title">Suggestions OCR :</p>
                            {Object.entries(result.metaDataSuggestions).map(([k, v]) => (
                                <div key={k} className="details-row">
                                    <strong>{k} :</strong> {v}
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}

export default UploadSimple;