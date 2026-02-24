package com.toolrent.backend.services;

import com.toolrent.backend.entities.*;
import com.toolrent.backend.repositories.KardexMovementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Service
@Transactional
public class KardexMovementService {

    private final KardexMovementRepository kardexMovementRepository;
    private final ToolInstanceService toolInstanceService;

    @Autowired
    @Lazy
    private ToolService toolService; // Inyección de ToolService con @Lazy para romper ciclo

    public KardexMovementService(KardexMovementRepository kardexMovementRepository,
                                 ToolInstanceService toolInstanceService) {
        this.kardexMovementRepository = kardexMovementRepository;
        this.toolInstanceService = toolInstanceService;
    }

    // ========== MÉTODOS PRINCIPALES PARA REGISTRO DE MOVIMIENTOS ==========

    // RF5.1: Create movement with automatic stock tracking, instance management, and specific instance tracking
    @Transactional
    public KardexMovementEntity createMovement(ToolEntity tool, ToolInstanceEntity toolInstance,
                                               KardexMovementEntity.MovementType type,
                                               Integer quantity, String description,
                                               LoanEntity relatedLoan, Integer stockBeforeMovement) {

        validateMovementCreation(tool, type, quantity);

        // Get current stock before movement (use provided value or current stock)
        Integer stockBefore = (stockBeforeMovement != null) ? stockBeforeMovement : tool.getCurrentStock();

        // Calculate stock after movement
        Integer stockAfter = calculateStockAfter(stockBefore, type, quantity);

        // NOTA: No validamos instancias aquí porque eso debe hacerse en los servicios específicos
        // (LoanService, ToolService) ANTES de actualizar el stock y crear el movimiento kardex

        // Create the movement with tool instance
        KardexMovementEntity movement = new KardexMovementEntity(
                tool, toolInstance, type, quantity, stockBefore, stockAfter, description, relatedLoan
        );

        // MEJORA: Actualizar instancias automáticamente según el tipo de movimiento
        updateInstancesBasedOnMovement(tool, type, quantity, movement);

        return kardexMovementRepository.save(movement);
    }

    // RF5.1: Create movement with automatic stock tracking and instance management (without specific instance)
    @Transactional
    public KardexMovementEntity createMovement(ToolEntity tool, KardexMovementEntity.MovementType type,
                                               Integer quantity, String description,
                                               LoanEntity relatedLoan, Integer stockBeforeMovement) {
        return createMovement(tool, null, type, quantity, description, relatedLoan, stockBeforeMovement);
    }

    // Overloaded method without loan relation
    public KardexMovementEntity createMovement(ToolEntity tool, KardexMovementEntity.MovementType type,
                                               Integer quantity, String description) {
        return createMovement(tool, type, quantity, description, null, null);
    }

    // Overloaded method with loan but without explicit stockBefore
    public KardexMovementEntity createMovement(ToolEntity tool, KardexMovementEntity.MovementType type,
                                               Integer quantity, String description,
                                               LoanEntity relatedLoan) {
        return createMovement(tool, type, quantity, description, relatedLoan, null);
    }

    // ========== MÉTODOS ESPECÍFICOS POR TIPO DE MOVIMIENTO ==========

    // RF5.1: Create initial stock movement for new tools
    @Transactional
    public KardexMovementEntity createInitialStockMovement(ToolEntity tool, Integer initialStock) {
        // Las instancias ya deberían estar creadas por el ToolService
        // IMPORTANTE: Para el stock inicial, el stockBefore siempre debe ser 0
        return createMovement(tool, KardexMovementEntity.MovementType.INITIAL_STOCK,
                initialStock, "Initial stock registration for " + tool.getName(), null, 0);
    }

    // RF5.1: Create loan movement with instance tracking
    @Transactional
    public KardexMovementEntity createLoanMovement(ToolEntity tool, Integer quantity,
                                                   String description,
                                                   LoanEntity loan) {

        // NOTA: No validamos ni reservamos instancias aquí porque eso ya se hace en LoanService
        // Este método solo registra el movimiento en el kardex
        KardexMovementEntity movement = createMovement(tool, KardexMovementEntity.MovementType.LOAN,
                quantity, description, loan);


        return kardexMovementRepository.save(movement);
    }

    // RF5.1: Create return movement with instance tracking
    @Transactional
    public KardexMovementEntity createReturnMovement(ToolEntity tool, Integer quantity,
                                                     String description,
                                                     LoanEntity loan, List<Long> instanceIds,
                                                     boolean isDamaged) {

        KardexMovementEntity movement = createMovement(tool, KardexMovementEntity.MovementType.RETURN,
                quantity, description, loan);

        // MEJORA: Manejar devolución de instancias específicas
        if (instanceIds != null && !instanceIds.isEmpty()) {
            try {
                List<ToolInstanceEntity> returnedInstances =
                        toolInstanceService.returnMultipleInstances(instanceIds, isDamaged);

                // Agregar información de las instancias devueltas
                movement.setDescription(description + " - Instancias devueltas: " + instanceIds +
                        (isDamaged ? " (CON DAÑOS)" : " (SIN DAÑOS)"));

            } catch (Exception e) {
                throw new RuntimeException("Error al procesar devolución de instancias: " + e.getMessage());
            }
        }

        return kardexMovementRepository.save(movement);
    }

    @Transactional
    public void deleteMovementsByLoan(Long loanId) {
        List<KardexMovementEntity> movements = getMovementsByLoanId(loanId);
        kardexMovementRepository.deleteAll(movements);
    }

    // RF5.1: Create decommission movement with instance tracking
    @Transactional
    public KardexMovementEntity createDecommissionMovement(ToolEntity tool, Integer quantity,
                                                           String description,
                                                           List<Long> instanceIds,
                                                           Integer stockBeforeMovement) {

        // IMPORTANTE: No dar de baja las instancias aquí porque ToolService.decommissionTool() ya lo hizo
        // Solo registrar el movimiento en el kardex

        // Agregar información de las instancias a la descripción
        String finalDescription = description;
        if (instanceIds != null && !instanceIds.isEmpty()) {
            finalDescription = description + " - Instancias dadas de baja: " + instanceIds;
        }

        KardexMovementEntity movement = createMovement(tool, KardexMovementEntity.MovementType.DECOMMISSION,
                quantity, finalDescription, null, stockBeforeMovement);

        return kardexMovementRepository.save(movement);
    }

    // RF5.1: Create restock movement
    @Transactional
    public KardexMovementEntity createRestockMovement(ToolEntity tool, Integer quantity,
                                                      String description, Integer stockBeforeMovement) {

        // IMPORTANTE: No crear instancias aquí porque ToolService.addToolStock() ya las crea
        // Este método solo registra el movimiento en el kardex
        KardexMovementEntity movement = createMovement(tool, KardexMovementEntity.MovementType.RESTOCK,
                quantity, description, null, stockBeforeMovement);

        return kardexMovementRepository.save(movement);
    }

    // RF5.1: Create repair movement
    public KardexMovementEntity createRepairMovement(ToolEntity tool, String description
            , Long instanceId) {

        KardexMovementEntity movement = createMovement(tool, KardexMovementEntity.MovementType.REPAIR,
                0, description); // Repair doesn't change stock quantity

        // MEJORA: Marcar instancia específica como en reparación
        if (instanceId != null) {
            try {
                toolInstanceService.updateInstanceStatus(instanceId,
                        ToolInstanceEntity.ToolInstanceStatus.UNDER_REPAIR);
                movement.setDescription(description + " - Instancia en reparación: " + instanceId);
            } catch (Exception e) {
                throw new RuntimeException("Error al marcar instancia en reparación: " + e.getMessage());
            }
        }

        return kardexMovementRepository.save(movement);
    }

    // ========== MÉTODOS DE CONSULTA (RF5.2 y RF5.3) ==========

    // RF5.2: Get movement history by tool
    public List<KardexMovementEntity> getMovementHistoryByTool(Long toolId) {
        if (toolId == null || toolId <= 0) {
            throw new RuntimeException("Invalid tool ID");
        }
        return kardexMovementRepository.findByToolIdOrderByCreatedAtDesc(toolId);
    }

    // RF5.2: Get movement history by tool entity
    public List<KardexMovementEntity> getMovementHistoryByTool(ToolEntity tool) {
        if (tool == null) {
            throw new RuntimeException("Tool cannot be null");
        }
        return kardexMovementRepository.findByToolOrderByCreatedAtDesc(tool);
    }

    // RF5.3: Get movements by date range
    public List<KardexMovementEntity> getMovementsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        validateDateRange(startDate, endDate);
        return kardexMovementRepository.findByDateRangeOrderByCreatedAtDesc(startDate, endDate);
    }

    // RF5.3: Get movements by date range (using LocalDate)
    public List<KardexMovementEntity> getMovementsByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        return getMovementsByDateRange(startDateTime, endDateTime);
    }

    // ========== MÉTODOS DE VALIDACIÓN Y CONSISTENCIA ==========

    // MEJORA: Validar disponibilidad de instancias antes de movimientos
    private void validateInstanceAvailability(ToolEntity tool, Integer quantity) {
        if (!toolInstanceService.isAvailable(tool.getId(), quantity)) {
            Long availableCount = toolInstanceService.getAvailableCount(tool.getId());
            throw new RuntimeException(
                    String.format("Instancias insuficientes. Requeridas: %d, Disponibles: %d",
                            quantity, availableCount));
        }
    }

    // MEJORA: Actualizar instancias basado en el tipo de movimiento
    private void updateInstancesBasedOnMovement(ToolEntity tool, KardexMovementEntity.MovementType type,
                                                Integer quantity, KardexMovementEntity movement) {

        switch (type) {
            case LOAN:
                // Las instancias se reservan en LoanService.createLoan(), no aquí
                break;
            case RETURN:
                // Las instancias se manejan en createReturnMovement
                break;
            case DECOMMISSION:
                // Las instancias se manejan en createDecommissionMovement
                break;
            case RESTOCK:
                // Las instancias se manejan en createRestockMovement
                break;
            case REPAIR:
                // Las instancias se manejan en createRepairMovement
                break;
            case INITIAL_STOCK:
                // Las instancias ya están creadas por ToolService
                break;
        }
    }

    // Verify stock consistency between tool and instances
    public boolean verifyStockConsistency(ToolEntity tool) {
        Long availableInstances = toolInstanceService.getAvailableCount(tool.getId());

        // El stock actual debe coincidir con las instancias disponibles
        return tool.getCurrentStock().equals(availableInstances.intValue());
    }

    // MEJORA: Auditoría completa de consistencia - CORREGIDO
    @Transactional(readOnly = true)
    public KardexAuditReport generateAuditReport(Long toolId) {
        try {
            ToolEntity tool = getToolById(toolId);

            // Obtener estadísticas de instancias
            ToolInstanceService.ToolInstanceStats instanceStats =
                    toolInstanceService.getToolInstanceStats(toolId);

            // Obtener último stock del kardex - CORREGIDO para usar lista
            Integer lastKardexStock = null;
            List<Integer> stockList = kardexMovementRepository.getLastStockByToolList(toolId);
            if (!stockList.isEmpty()) {
                lastKardexStock = stockList.get(0); // Tomar el primer elemento (más reciente)
            }

            // Generar reporte
            return new KardexAuditReport(
                    tool,
                    instanceStats,
                    lastKardexStock,
                    verifyStockConsistency(tool),
                    getMovementHistoryByTool(toolId)
            );
        } catch (Exception e) {
            System.err.println("Error generating audit report: " + e.getMessage());
            throw new RuntimeException("Error al generar reporte de auditoría: " + e.getMessage());
        }
    }

    // ========== MÉTODOS DE UTILIDAD ==========

    // Get all movements (mantenido del código original)
    public List<KardexMovementEntity> getAllMovements() {
        return kardexMovementRepository.findAll();
    }

    // Get movement by ID (mantenido del código original)
    public KardexMovementEntity getMovementById(Long id) {
        return kardexMovementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kardex movement not found with ID: " + id));
    }

    // Métodos de consulta adicionales (mantenidos del código original)
    public List<KardexMovementEntity> getMovementsByType(KardexMovementEntity.MovementType type) {
        return kardexMovementRepository.findByTypeOrderByCreatedAtDesc(type);
    }

    public List<KardexMovementEntity> getMovementsByLoanId(Long loanId) {
        return kardexMovementRepository.findByRelatedLoanIdOrderByCreatedAtDesc(loanId);
    }

    // ========== MÉTODOS PRIVADOS DE VALIDACIÓN ==========

    private void validateMovementCreation(ToolEntity tool, KardexMovementEntity.MovementType type,
                                          Integer quantity) {
        if (tool == null) {
            throw new RuntimeException("Tool is required for kardex movement");
        }

        if (type == null) {
            throw new RuntimeException("Movement type is required");
        }

        if (quantity == null || quantity < 0) {
            throw new RuntimeException("Quantity must be greater than or equal to 0");
        }


        // MEJORA: Validación más estricta para movimientos que afectan stock
        // Nota: No validamos aquí porque el stock ya fue actualizado antes de crear el movimiento
        // La validación debe hacerse en el servicio de préstamos antes de actualizar el stock
        /*
        if (type == KardexMovementEntity.MovementType.LOAN ||
                type == KardexMovementEntity.MovementType.DECOMMISSION) {
            if (quantity > tool.getCurrentStock()) {
                throw new RuntimeException("Cannot create movement: insufficient stock");
            }
        }
        */
    }

    private Integer calculateStockAfter(Integer stockBefore, KardexMovementEntity.MovementType type, Integer quantity) {
        switch (type) {
            case INITIAL_STOCK:
            case RETURN:
            case RESTOCK:
                return stockBefore + quantity;
            case LOAN:
            case DECOMMISSION:
                return stockBefore - quantity;
            case REPAIR:
                return stockBefore; // Repair doesn't change stock
            default:
                return stockBefore;
        }
    }

    private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) {
            throw new RuntimeException("Start date cannot be null");
        }
        if (endDate == null) {
            throw new RuntimeException("End date cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new RuntimeException("Start date cannot be after end date");
        }
    }

    // ========== CLASES DE APOYO ==========

    // Clase para reporte de auditoría
    public static class KardexAuditReport {
        public final ToolEntity tool;
        public final ToolInstanceService.ToolInstanceStats instanceStats;
        public final Integer lastKardexStock;
        public final boolean isConsistent;
        public final List<KardexMovementEntity> recentMovements;

        public KardexAuditReport(ToolEntity tool, ToolInstanceService.ToolInstanceStats instanceStats,
                                 Integer lastKardexStock, boolean isConsistent,
                                 List<KardexMovementEntity> recentMovements) {
            this.tool = tool;
            this.instanceStats = instanceStats;
            this.lastKardexStock = lastKardexStock;
            this.isConsistent = isConsistent;
            this.recentMovements = recentMovements;
        }
    }

    // Método auxiliar para obtener tool - CORREGIDO
    private ToolEntity getToolById(Long toolId) {
        return toolService.getToolById(toolId)
                .orElseThrow(() -> new RuntimeException("Tool not found with ID: " + toolId));
    }
}

