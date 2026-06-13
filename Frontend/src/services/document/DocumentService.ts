import api from "../api";

export type TypeAccess = 'PUBLIC' | 'PRIVE';
export type IntegrityLevel = 'STANDARD' | 'BLOCKCHAIN';
export type DocumentStatus = 'PENDING' | 'ACTIVE' | 'ACTIVE_WARNING' | 'CORRUPTED' | 'DELETED';

export interface DocumentUploadDto {
    titre: string;
    access: TypeAccess;
    typeDocumentId: number;
    uploadedById: string;
    integrityLevel: IntegrityLevel;
    groupeNom?: string;
    groupeMembresIds?: string[];
}

export interface DocumentUploadResultDto {
    documentId: string;
    status: DocumentStatus;
    sha256Hash: string;
    storageKey: string;
    originalStorageKey: string;
    metaDataSuggestions: Record<string, string>;
}

export interface BulkUploadItemResultDto {
    nomFichier: string;
    typeDocument: string;
    status: 'SUCCESS' | 'FAILED';
    documentId?: string;
    erreur?: string;
}

export interface BulkUploadReportDto {
    total: number;
    success: number;
    failed: number;
    details: BulkUploadItemResultDto[];
}

// Upload simple — multipart/form-data
export const uploadDocument = async (
    file: File,
    dto: DocumentUploadDto
): Promise<DocumentUploadResultDto> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('dto', new Blob([JSON.stringify(dto)], { type: 'application/json' }));
    try {
        const response = await api.post('/editor/docs/upload', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
        return response.data;
    } catch (error: any) {
        throw new Error(error.response?.data || error.message || "Erreur lors de l'upload");
    }
};

export type MetaDataType = 'CHAR' | 'STRING' | 'INTEGER' | 'FLOAT' | 'DOUBLE' | 'BOOLEAN' | 'DATE' | 'TEXT';

export interface MetaDataDto {
    id?: number;
    nom: string;
    obligatoire: boolean;
    metaDataType: MetaDataType;
}

export interface TypeDocumentDto {
    id?: number;
    nom: string;
    metaData: MetaDataDto[];
    userId: string;
    retentionYears: number;
    periodGrace: number;
}

export const getAllTypeDocuments = async (): Promise<TypeDocumentDto[]> => {
    try {
        const response = await api.get('/editor/types-documents');
        return response.data;
    } catch (error: any) {
        throw error.response?.data?.message
            ? new Error(error.response.data.message)
            : error;
    }
};

export const getTypeDocumentById = async (id: number): Promise<TypeDocumentDto> => {
    try {
        const response = await api.get(`/editor/types-documents/${id}`);
        return response.data;
    } catch (error: any) {
        throw error.response?.data?.message
            ? new Error(error.response.data.message)
            : error;
    }
};

// Bulk même type — plusieurs fichiers, même typeDocumentId
export const uploadBulkSameType = async (
    files: File[],
    typeDocumentId: number,
    uploadedById: string
): Promise<BulkUploadReportDto> => {
    const formData = new FormData();
    files.forEach(f => formData.append('files', f));
    formData.append('typeDocumentId', String(typeDocumentId));
    formData.append('uploadedById', uploadedById);
    try {
        const response = await api.post('/editor/docs/bulk/same-type', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
        return response.data;
    } catch (error: any) {
        throw new Error(error.response?.data || error.message || "Erreur upload en masse");
    }
};

// Bulk multi-type — CSV/Excel + ZIP
export const uploadBulkMultiType = async (
    metaFile: File,
    zipFile: File,
    uploadedById: string
): Promise<BulkUploadReportDto> => {
    const formData = new FormData();
    formData.append('metaFile', metaFile);
    formData.append('zipFile', zipFile);
    formData.append('uploadedById', uploadedById);
    try {
        const response = await api.post('/editor/docs/bulk/multi-type', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
        return response.data;
    } catch (error: any) {
        throw new Error(error.response?.data || error.message || "Erreur upload multi-type");
    }
};

// Mes documents (uploadés par l'éditeur connecté)
export const getMyDocuments = async (): Promise<any[]> => {
    try {
        const response = await api.get('/editor/docs/mes-documents');
        return response.data;
    } catch (error: any) {
        throw new Error(error.response?.data || error.message || "Erreur chargement documents");
    }
};

// Sauvegarder les métadonnées validées
export const saveMetaData = async (
    documentId: string,
    dto: { metaData: { nom: string; valeur: string; typeValeur: string }[] }
): Promise<void> => {
    try {
        await api.post(`/editor/docs/${documentId}/metadata`, dto);
    } catch (error: any) {
        throw new Error(
            error.response?.data || error.message || "Erreur sauvegarde métadonnées"
        );
    }
};

// Récupérer tous les utilisateurs pour le choix des membres
export interface UserDto {
    id: string;
    nom: string;
    prenom: string;
    email: string;
}

export const getAllUsers = async (): Promise<UserDto[]> => {
    try {
        const response = await api.get('/editor/users');
        return response.data;
    } catch (error: any) {
        throw new Error(
            error.response?.data || error.message || "Erreur récupération utilisateurs"
        );
    }
};