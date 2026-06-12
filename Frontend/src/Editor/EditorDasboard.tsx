import { useState } from 'react';
import Sidebar from '../Page/Sidebar';
import Profile from '../Page/Profil';
import Modal from '../Page/Modal';
import UploadSimple from '../document/UploadSimple';
import UploadBulkSameType from '../document/UploadeBlukSameType';
import UploadBulkMultiType from '../document/UploadeBlukMultiType';
import DownloadTemplate from '../document/DownloadTemplate';
import MesDocumentsEditor from './MesDocumentsEditor';
import { getCurrentUserInfo } from '../auth/authService';
import '../style/Editor/Editor.css';

type EditorView = 'documents' | 'profile';
type UploadMode = 'simple' | 'bulk-same' | 'bulk-multi';

function EditorDashboard() {
    const userInfo = getCurrentUserInfo();
    const [currentView, setCurrentView] = useState<EditorView>('documents');
    const [uploadMode, setUploadMode] = useState<UploadMode>('simple');

    const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
    const [isTemplateModalOpen, setIsTemplateModalOpen] = useState(false);

    const [refreshDocs, setRefreshDocs] = useState(0);
    const [success, setSuccess] = useState('');
    const [error, setError] = useState('');

    const handleUploadSuccess = () => {
        setIsUploadModalOpen(false);
        setSuccess("Document(s) uploadé(s) avec succès");
        setRefreshDocs(r => r + 1);
        setTimeout(() => setSuccess(''), 3000);
    };

    const openUpload = (mode: UploadMode) => {
        setUploadMode(mode);
        setIsUploadModalOpen(true);
    };

    const UPLOAD_TITLES: Record<UploadMode, string> = {
        'simple': 'Uploader un document',
        'bulk-same': 'Import en masse — même type',
        'bulk-multi': 'Import en masse — multi-type'
    };

    return (
        <div className="admin-dashboard">
            <div className="admin-body">

                <Sidebar title={userInfo?.role || "ÉDITEUR"}>
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

                            <div className="sidebar-section-label">Upload</div>
                            <div className="main-header">
                                <button className="sidebar-btn" onClick={() => openUpload('simple')}>
                                    Uploader un document
                                </button>
                            </div>
                            <div className="main-header">
                                <button className="sidebar-btn" onClick={() => openUpload('bulk-same')}>
                                    Import masse — même type
                                </button>
                            </div>
                            <div className="main-header">
                                <button className="sidebar-btn" onClick={() => openUpload('bulk-multi')}>
                                    Import masse — multi-type
                                </button>
                            </div>

                            <div className="sidebar-section-label">Documents</div>
                            <div className="main-header">
                                <button
                                    className={`sidebar-btn ${currentView === 'documents' ? 'active-tab' : ''}`}
                                    onClick={() => setCurrentView('documents')}
                                >
                                    Mes documents
                                </button>
                            </div>
                            <div className="main-header">
                                <button
                                    className="sidebar-btn"
                                    onClick={() => setIsTemplateModalOpen(true)}
                                >
                                    Télécharger template
                                </button>
                            </div>

                            {error && <div className="alert alert-error">{error}</div>}
                            {success && <div className="alert alert-success">{success}</div>}
                        </div>
                    </nav>
                </Sidebar>

                <div className="main-content">
                    {currentView === 'profile' && (
                        <Profile userId={userInfo?.id} />
                    )}
                    {currentView === 'documents' && (
                        <MesDocumentsEditor refreshTrigger={refreshDocs} />
                    )}
                </div>

                {/* Modal upload */}
                <Modal
                    isOpen={isUploadModalOpen}
                    onClose={() => setIsUploadModalOpen(false)}
                    title={UPLOAD_TITLES[uploadMode]}
                >
                    {uploadMode === 'simple' && (
                        <UploadSimple onsuccess={handleUploadSuccess} />
                    )}
                    {uploadMode === 'bulk-same' && (
                        <UploadBulkSameType onsuccess={handleUploadSuccess} />
                    )}
                    {uploadMode === 'bulk-multi' && (
                        <UploadBulkMultiType onsuccess={handleUploadSuccess} />
                    )}
                </Modal>

                {/* Modal template */}
                <Modal
                    isOpen={isTemplateModalOpen}
                    onClose={() => setIsTemplateModalOpen(false)}
                    title="Télécharger un template d'import"
                >
                    <DownloadTemplate />
                </Modal>
            </div>
        </div>
    );
}

export default EditorDashboard;