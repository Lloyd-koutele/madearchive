import React, { useState, useEffect } from 'react';
import { uploadDocument, getAllTypeDocuments, saveMetaData } from '../services/document/DocumentService';
import type { DocumentUploadDto, DocumentUploadResultDto, TypeDocumentDto } from '../services/document/DocumentService';
import { getAllUsers } from '../services/document/DocumentService';
import type { UserDto } from '../services/document/DocumentService';
import { getCurrentUserInfo } from '../auth/authService';
import '../Style/Editor/Editor.css';

interface UploadSimpleProps {
    onsuccess?: (result: DocumentUploadResultDto) => void;
}

type Step = 1 | 2 | 3;

function UploadSimple({ onsuccess }: UploadSimpleProps) {
    // Étape courante
    const [step, setStep] = useState<Step>(1);

    // Étape 1 — Préparation
    const [file, setFile] = useState<File | null>(null);
    const [typeDocuments, setTypeDocuments] = useState<TypeDocumentDto[]>([]);
    const [typeDocumentId, setTypeDocumentId] = useState<number | ''>('');
    const [selectedType, setSelectedType] = useState<TypeDocumentDto | null>(null);
    const [access, setAccess] = useState<'PUBLIC' | 'PRIVE'>('PUBLIC');
    const [integrityLevel, setIntegrityLevel] = useState<'STANDARD' | 'BLOCKCHAIN'>('STANDARD');
    const [groupeNom, setGroupeNom] = useState('');
    const [isDragging, setIsDragging] = useState(false);
    const [users, setUsers] = useState<UserDto[]>([]);
    const [selectedMembres, setSelectedMembres] = useState<string[]>([]);

    // Étape 2 — Traitement
    const [isLoading, setIsLoading] = useState(false);
    const [uploadResult, setUploadResult] = useState<DocumentUploadResultDto | null>(null);
    const [progressSteps, setProgressSteps] = useState<string[]>([]);

    // Étape 3 — Validation métadonnées
    const [metaValues, setMetaValues] = useState<Record<string, string>>({});

    const [error, setError] = useState('');

    useEffect(() => {
        getAllTypeDocuments().then(setTypeDocuments)
            .catch(() => setError("Impossible de charger les types de documents"));
        getAllUsers().then(setUsers)
            .catch(() => {});
    }, []);

    // Quand le type change → mettre à jour selectedType et initialiser metaValues vides
    const handleTypeChange = (id: number) => {
        setTypeDocumentId(id);
        const found = typeDocuments.find(td => td.id === id) || null;
        setSelectedType(found);
        if (found) {
            const initial: Record<string, string> = {};
            found.metaData.forEach(m => { initial[m.nom] = ''; });
            setMetaValues(initial);
        }
    };

    const handleDrop = (e: React.DragEvent) => {
        e.preventDefault();
        setIsDragging(false);
        const dropped = e.dataTransfer.files[0];
        if (dropped) setFile(dropped);
    };

    const toggleMembre = (userId: string) => {
        setSelectedMembres(prev =>
            prev.includes(userId)
                ? prev.filter(id => id !== userId)
                : [...prev, userId]
        );
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

    // Étape 1 → 2 : Lancer l'OCR et archiver
    const handleLancerOcr = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        if (!validate()) return;

        const userInfo = getCurrentUserInfo();
        if (!userInfo?.id) { setError("Session expirée"); return; }

        setStep(2);
        setIsLoading(true);
        setProgressSteps([]);

        try {
            const dto: DocumentUploadDto = {
                titre: file!.name,
                access,
                typeDocumentId: typeDocumentId as number,
                uploadedById: userInfo.id,
                integrityLevel,
                ...(access === 'PRIVE' && {
                    groupeNom,
                    groupeMembresIds: selectedMembres
                })
            };

            // Simuler les étapes de progression
            setProgressSteps(prev => [...prev, "📁 Réception du fichier..."]);
            const res = await uploadDocument(file!, dto);

            setProgressSteps(prev => [...prev,
                "🔄 Conversion PDF/A-3b...",
                "🔐 Calcul SHA-256...",
                "🔍 OCR en cours...",
                "📑 Indexation Meilisearch...",
                "✅ Archivage terminé !"
            ]);

            setUploadResult(res);

            // Pré-remplir les métadonnées avec les suggestions OCR
            if (res.metaDataSuggestions) {
                setMetaValues(prev => ({
                    ...prev,
                    ...res.metaDataSuggestions
                }));
            }

            // Passer à l'étape 3
            setTimeout(() => {
                setStep(3);
                setIsLoading(false);
            }, 800);

        } catch (err: any) {
            setError(err.message || "Erreur lors de l'archivage");
            setStep(1);
            setIsLoading(false);
        }
    };

    // Étape 3 → Finaliser : sauvegarder les métadonnées
    const handleFinaliser = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!uploadResult) return;
        setError('');
        setIsLoading(true);

        try {
            await saveMetaData(uploadResult.documentId, {
                metaData: Object.entries(metaValues).map(([nom, valeur]) => ({
                    nom,
                    valeur,
                    typeValeur: selectedType?.metaData.find(m => m.nom === nom)?.metaDataType || 'STRING'
                }))
            });

            onsuccess?.(uploadResult);
        } catch (err: any) {
            setError(err.message || "Erreur lors de la sauvegarde des métadonnées");
        } finally {
            setIsLoading(false);
        }
    };

    // ─── RENDU ───────────────────────────────────────────────────────────────

    return (
        <div className="upload-wrapper">

            {/* Indicateur d'étapes */}
            <div className="upload-steps">
                {[1, 2, 3].map(s => (
                    <div key={s} className={`upload-step ${step === s ? 'active' : step > s ? 'done' : ''}`}>
                        <span className="step-number">{step > s ? '✓' : s}</span>
                        <span className="step-label">
                            {s === 1 ? 'Préparation' : s === 2 ? 'Traitement' : 'Métadonnées'}
                        </span>
                    </div>
                ))}
            </div>

            {error && <div className="up-alert up-alert-error">{error}</div>}

            {/* ── ÉTAPE 1 — Préparation ── */}
            {step === 1 && (
                <form onSubmit={handleLancerOcr}>

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
                            aria-label="Fichier"
                            onChange={e => setFile(e.target.files?.[0] || null)}
                        />
                        {file ? (
                            <div className="drop-zone-file">
                                <i className="fa-solid fa-file drop-icon"></i>
                                <span className="drop-filename">{file.name}</span>
                                <span className="drop-filesize">
                                    {(file.size / 1024 / 1024).toFixed(2)} Mo
                                </span>
                                <button
                                    type="button"
                                    className="drop-remove"
                                    onClick={e => { e.stopPropagation(); setFile(null); }}
                                >✕ Retirer</button>
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
                                onChange={e => handleTypeChange(Number(e.target.value))}
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
                                    <option value="BLOCKCHAIN">Blockchain</option>
                                </select>
                            </div>
                        </div>

                        {/* Groupe si PRIVE */}
                        {access === 'PRIVE' && (
                            <div className="groupe-section">
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

                                {/* Sélection des membres */}
                                {users.length > 0 && (
                                    <div className="membres-section">
                                        <p className="membres-label">
                                            Membres du groupe (optionnel) :
                                        </p>
                                        <div className="membres-list">
                                            {users.map(u => (
                                                <label key={u.id} className="membre-item">
                                                    <input
                                                        type="checkbox"
                                                        checked={selectedMembres.includes(u.id)}
                                                        onChange={() => toggleMembre(u.id)}
                                                    />
                                                    <span>{u.prenom} {u.nom}</span>
                                                    <span className="membre-email">{u.email}</span>
                                                </label>
                                            ))}
                                        </div>
                                    </div>
                                )}
                            </div>
                        )}
                    </div>

                    <button type="submit" className="form-submit-btn up-submit">
                        <i className="fa-solid fa-rocket"></i> Lancer l'OCR et archiver
                    </button>
                </form>
            )}

            {/* ── ÉTAPE 2 — Traitement ── */}
            {step === 2 && (
                <div className="upload-progress">
                    <div className="progress-steps">
                        {progressSteps.map((s, i) => (
                            <div key={i} className="progress-step">
                                <span>{s}</span>
                            </div>
                        ))}
                        {isLoading && (
                            <div className="progress-step loading">
                                <i className="fa-solid fa-spinner fa-spin"></i>
                                <span>Traitement en cours...</span>
                            </div>
                        )}
                    </div>
                </div>
            )}

            {/* ── ÉTAPE 3 — Validation des métadonnées ── */}
            {step === 3 && uploadResult && (
                <form onSubmit={handleFinaliser}>
                    <div className="up-result-header">
                        <div className="details-row">
                            <strong>Document archivé :</strong> {uploadResult.documentId}
                        </div>
                        <div className="details-row">
                            <strong>SHA-256 :</strong>
                            <span className="up-hash">{uploadResult.sha256Hash}</span>
                        </div>
                    </div>

                    <div className="meta-fields">
                        <p className="meta-fields-title">
                            Vérifiez et complétez les métadonnées :
                        </p>
                        {selectedType?.metaData.map(meta => (
                            <div key={meta.nom} className="form-field">
                                <input
                                    id={`meta-${meta.nom}`}
                                    type="text"
                                    className="form-field-input"
                                    placeholder=" "
                                    value={metaValues[meta.nom] || ''}
                                    onChange={e => setMetaValues(prev => ({
                                        ...prev,
                                        [meta.nom]: e.target.value
                                    }))}
                                    required={meta.obligatoire}
                                />
                                <label htmlFor={`meta-${meta.nom}`}>
                                    {meta.nom}
                                    {meta.obligatoire && (
                                        <span className="required-star"> *</span>
                                    )}
                                    {metaValues[meta.nom] && (
                                        <span className="meta-prefilled"> (pré-rempli)</span>
                                    )}
                                </label>
                            </div>
                        ))}
                    </div>

                    <button
                        type="submit"
                        className="form-submit-btn up-submit"
                        disabled={isLoading}
                    >
                        {isLoading ? (
                            <><i className="fa-solid fa-spinner fa-spin"></i> Sauvegarde...</>
                        ) : (
                            <><i className="fa-solid fa-check"></i> Valider et finaliser l'archivage</>
                        )}
                    </button>
                </form>
            )}
        </div>
    );
}

export default UploadSimple;