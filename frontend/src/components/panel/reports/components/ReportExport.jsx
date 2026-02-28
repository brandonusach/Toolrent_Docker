// ReportExport.jsx - Exportar reportes (frontend puro)
import React, { useState } from 'react';
import { useReports } from '../hooks/useReports';
import { getTodayDate } from '../../../../utils/dateUtils';

const ReportExport = ({ reportData, activeTab }) => {
    const [isExporting, setIsExporting] = useState(false);
    const [exportFormat, setExportFormat] = useState('csv');
    const { exportToCSV } = useReports();

    const exportOptions = [
        { value: 'csv', label: 'CSV', icon: '📄' },
        { value: 'json', label: 'JSON', icon: '📋' }
    ];

    const getExportData = () => {
        if (!reportData || !activeTab) return null;

        switch (activeTab) {
            case 'active-loans':
                return {
                    data: reportData.activeLoans?.loans || [],
                    filename: 'prestamos_activos',
                    displayName: 'Préstamos Activos'
                };
            case 'overdue-clients':
                return {
                    data: reportData.overdueClients?.clients || [],
                    filename: 'clientes_morosos',
                    displayName: 'Clientes con Atrasos'
                };
            case 'popular-tools':
                return {
                    data: reportData.popularTools?.tools || [],
                    filename: 'herramientas_populares',
                    displayName: 'Herramientas Más Prestadas'
                };
            default:
                return null;
        }
    };

    const handleExport = async () => {
        const exportData = getExportData();
        if (!exportData || !exportData.data || exportData.data.length === 0) {
            alert('No hay datos disponibles para exportar');
            return;
        }

        setIsExporting(true);
        try {
            if (exportFormat === 'csv') {
                exportToCSV(exportData.data, exportData.filename);
            } else if (exportFormat === 'json') {
                exportJSON(exportData);
            }

            showSuccessMessage(`✅ Reporte "${exportData.displayName}" exportado exitosamente`);
        } catch (error) {
            alert('Error al exportar los datos. Por favor, inténtalo de nuevo.');
        } finally {
            setIsExporting(false);
        }
    };

    const exportJSON = (exportData) => {
        const jsonContent = JSON.stringify({
            reportType: activeTab,
            exportDate: new Date().toISOString(),
            displayName: exportData.displayName,
            data: exportData.data
        }, null, 2);

        const blob = new Blob([jsonContent], { type: 'application/json;charset=utf-8;' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `${exportData.filename}_${getTodayDate()}.json`;
        link.click();
        URL.revokeObjectURL(link.href);
    };

    const showSuccessMessage = (message) => {
        const notification = document.createElement('div');
        notification.className = 'fixed top-4 right-4 bg-green-500 text-white px-6 py-3 rounded-lg shadow-lg z-50';
        notification.textContent = message;
        document.body.appendChild(notification);

        setTimeout(() => {
            if (document.body.contains(notification)) {
                document.body.removeChild(notification);
            }
        }, 3000);
    };

    const exportData = getExportData();
    const hasData = exportData && exportData.data && exportData.data.length > 0;

    return (
        <div className="flex items-center gap-2">
            <select
                value={exportFormat}
                onChange={(e) => setExportFormat(e.target.value)}
                disabled={isExporting || !hasData}
                className="px-3 py-2 text-sm border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-orange-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
            >
                {exportOptions.map(option => (
                    <option key={option.value} value={option.value}>
                        {option.icon} {option.label}
                    </option>
                ))}
            </select>

            <button
                onClick={handleExport}
                disabled={isExporting || !hasData}
                className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
                    hasData && !isExporting
                        ? 'bg-green-600 text-white hover:bg-green-700'
                        : 'bg-slate-300 text-slate-500 cursor-not-allowed'
                }`}
                title={
                    !hasData
                        ? 'No hay datos para exportar'
                        : `Exportar ${exportData.displayName} como ${exportFormat.toUpperCase()}`
                }
            >
                {isExporting ? (
                    <>
                        <span className="inline-block animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></span>
                        Exportando...
                    </>
                ) : (
                    <>📥 Exportar</>
                )}
            </button>

            {hasData && (
                <div className="hidden lg:flex items-center text-xs text-slate-500">
                    <span>{exportData.data.length} registros</span>
                </div>
            )}

            {!hasData && (
                <div className="hidden lg:flex items-center text-xs text-slate-400">
                    <span>📊 Carga datos para exportar</span>
                </div>
            )}
        </div>
    );
};

export default ReportExport;

