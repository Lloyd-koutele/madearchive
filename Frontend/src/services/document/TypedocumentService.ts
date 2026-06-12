import api from "../api";

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
        const response = await api.get('/admin/types-documents');
        return response.data;
    } catch (error: any) {
        throw error.response?.data?.message
            ? new Error(error.response.data.message)
            : error;
    }
};

export const getTypeDocumentById = async (id: number): Promise<TypeDocumentDto> => {
    try {
        const response = await api.get(`/admin/types-documents/${id}`);
        return response.data;
    } catch (error: any) {
        throw error.response?.data?.message
            ? new Error(error.response.data.message)
            : error;
    }
};

export const createTypeDocument = async (dto: TypeDocumentDto): Promise<TypeDocumentDto> => {
    try {
        const response = await api.post('/admin/types-documents/create', dto);
        return response.data;
    } catch (error: any) {
        throw error.response?.data?.message
            ? new Error(error.response.data.message)
            : error;
    }
};

export const updateTypeDocument = async (id: number, dto: TypeDocumentDto): Promise<TypeDocumentDto> => {
    try {
        const response = await api.put(`/admin/types-documents/${id}`, dto);
        return response.data;
    } catch (error: any) {
        throw error.response?.data?.message
            ? new Error(error.response.data.message)
            : error;
    }
};

export const deleteTypeDocument = async (id: number): Promise<void> => {
    try {
        await api.delete(`/admin/types-documents/${id}`);
    } catch (error: any) {
        throw error.response?.data?.message
            ? new Error(error.response.data.message)
            : error;
    }
};

export const deleteTypeDocumentList = async (ids: number[]): Promise<void> => {
    try {
        await api.delete('/admin/types-documents/delete-list', { data: ids });
    } catch (error: any) {
        throw error.response?.data?.message
            ? new Error(error.response.data.message)
            : error;
    }
};