package com.toolrent.backend.services;

import com.toolrent.backend.entities.*;
import com.toolrent.backend.repositories.LoanRepository;
import com.toolrent.backend.repositories.ToolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class LoanService {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private ToolService toolService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private FineService fineService;

    @Autowired
    private RateService rateService;

    @Autowired(required = false) // Hacer opcional en caso de que no esté implementado
    private KardexMovementService kardexMovementService;

    @Autowired(required = false) // Inyectar el servicio de instancias
    private ToolInstanceService toolInstanceService;

    // RF2.5: Check client restrictions - VERSIÓN MEJORADA Y SEGURA
    public Map<String, Object> checkClientRestrictions(Long clientId) {
        Map<String, Object> restrictions = new HashMap<>();

        try {
            if (clientId == null || clientId <= 0) {
                restrictions.put("eligible", false);
                restrictions.put("canRequestLoan", false);
                restrictions.put("restriction", "ID de cliente inválido");
                restrictions.put("clientStatus", "INVALID");
                return restrictions;
            }

            ClientEntity client = clientService.getClientById(clientId);
            if (client == null) {
                restrictions.put("eligible", false);
                restrictions.put("canRequestLoan", false);
                restrictions.put("restriction", "Cliente no encontrado");
                restrictions.put("clientStatus", "NOT_FOUND");
                return restrictions;
            }

            // Verificar estado del cliente
            boolean clientActive = client.getStatus() == ClientEntity.ClientStatus.ACTIVE;

            // Contar préstamos activos
            long activeLoans = loanRepository.countActiveLoansByClient(client);

            // Verificar préstamos vencidos
            List<LoanEntity> overdueLoans = loanRepository.findOverdueLoansByClient(client, LocalDate.now());
            boolean hasOverdueLoans = !overdueLoans.isEmpty();

            // Verificar multas impagas (con manejo de errores)
            boolean hasUnpaidFines = false;
            BigDecimal unpaidAmount = BigDecimal.ZERO;
            try {
                hasUnpaidFines = fineService.clientHasUnpaidFines(client);
                unpaidAmount = fineService.getTotalUnpaidAmount(client);
            } catch (Exception e) {
                // Si hay error con multas, asumir que no hay multas
                System.err.println("Error checking fines for client " + clientId + ": " + e.getMessage());
            }

            // Determinar elegibilidad
            boolean eligible = clientActive &&
                    !hasOverdueLoans &&
                    !hasUnpaidFines &&
                    activeLoans < 5;

            restrictions.put("eligible", eligible);
            restrictions.put("canRequestLoan", eligible);
            restrictions.put("currentActiveLoans", activeLoans);
            restrictions.put("maxAllowed", 5);
            restrictions.put("remainingLoanSlots", Math.max(0, 5 - activeLoans));
            restrictions.put("clientStatus", client.getStatus().toString());

            // Agregar detalles de restricciones
            if (!eligible) {
                StringBuilder restrictionReason = new StringBuilder();

                if (!clientActive) {
                    restrictionReason.append("Cliente no está activo. ");
                }
                if (hasOverdueLoans) {
                    restrictionReason.append("Cliente tiene ").append(overdueLoans.size()).append(" préstamo(s) vencido(s). ");
                }
                if (hasUnpaidFines) {
                    restrictionReason.append("Cliente tiene multas impagas por $").append(unpaidAmount).append(". ");
                }
                if (activeLoans >= 5) {
                    restrictionReason.append("Cliente ha alcanzado el límite de 5 préstamos activos. ");
                }

                restrictions.put("restriction", restrictionReason.toString().trim());
            } else {
                restrictions.put("message", "Cliente elegible para nuevos préstamos");
            }

            // Información adicional
            restrictions.put("overdueLoansCount", overdueLoans.size());
            restrictions.put("unpaidFinesAmount", unpaidAmount);
            restrictions.put("hasUnpaidFines", hasUnpaidFines);
            restrictions.put("hasOverdueLoans", hasOverdueLoans);

        } catch (Exception e) {
            System.err.println("Error in checkClientRestrictions: " + e.getMessage());
            e.printStackTrace();

            // Respuesta de error segura
            restrictions.put("eligible", false);
            restrictions.put("canRequestLoan", false);
            restrictions.put("restriction", "Error al verificar restricciones: " + e.getMessage());
            restrictions.put("clientStatus", "ERROR");
            restrictions.put("error", true);
        }

        return restrictions;
    }

    // Check tool availability - VERSIÓN MEJORADA
    public Map<String, Object> checkToolAvailability(ToolEntity tool, Integer quantity) {
        Map<String, Object> availability = new HashMap<>();

        try {
            if (tool == null) {
                availability.put("available", false);
                availability.put("issue", "Herramienta no encontrada");
                availability.put("toolStatus", "NOT_FOUND");
                availability.put("currentStock", 0);
                return availability;
            }

            if (quantity == null || quantity <= 0) {
                availability.put("available", false);
                availability.put("issue", "Cantidad debe ser mayor a 0");
                availability.put("toolStatus", tool.getStatus().toString());
                availability.put("currentStock", tool.getCurrentStock());
                return availability;
            }

            // Validar que solo se permite cantidad = 1
            if (quantity != 1) {
                availability.put("available", false);
                availability.put("issue", "Solo se permite prestar 1 unidad por préstamo");
                availability.put("toolStatus", tool.getStatus().toString());
                availability.put("currentStock", tool.getCurrentStock());
                return availability;
            }

            // Verificar estado de la herramienta
            boolean toolAvailable = tool.getStatus() == ToolEntity.ToolStatus.AVAILABLE;

            // Verificar stock
            boolean hasStock = tool.getCurrentStock() >= quantity;

            availability.put("available", toolAvailable && hasStock);
            availability.put("currentStock", tool.getCurrentStock());
            availability.put("requestedQuantity", quantity);
            availability.put("toolName", tool.getName());
            availability.put("toolStatus", tool.getStatus().toString());
            availability.put("maxAvailableQuantity", tool.getCurrentStock());

            if (!toolAvailable) {
                availability.put("issue", "Herramienta no está disponible. Estado actual: " + tool.getStatus());
                availability.put("issueType", "TOOL_STATUS");
            } else if (!hasStock) {
                availability.put("issue", "Stock insuficiente. Solicitado: " + quantity + ", Disponible: " + tool.getCurrentStock());
                availability.put("issueType", "INSUFFICIENT_STOCK");
            } else {
                availability.put("message", "Herramienta disponible para préstamo");
            }

        } catch (Exception e) {
            System.err.println("Error in checkToolAvailability: " + e.getMessage());
            availability.put("available", false);
            availability.put("issue", "Error al verificar disponibilidad: " + e.getMessage());
            availability.put("error", true);
        }

        return availability;
    }

    // RF6.1: Get active loans - VERSIÓN SEGURA
    @Transactional(readOnly = true)
    public List<LoanEntity> getActiveLoans() {
        try {
            System.out.println("Attempting to get active loans from repository...");
            List<LoanEntity> loans = loanRepository.findActiveLoans();
            System.out.println("Successfully retrieved " + loans.size() + " active loans");
            return loans;
        } catch (Exception e) {
            System.err.println("Error getting active loans with custom query: " + e.getMessage());
            e.printStackTrace();

            // Fallback: buscar por status usando método simple
            try {
                System.out.println("Trying fallback method with findByStatus...");
                List<LoanEntity> fallbackLoans = loanRepository.findByStatus(LoanEntity.LoanStatus.ACTIVE);
                System.out.println("Fallback method returned " + fallbackLoans.size() + " loans");
                return fallbackLoans;
            } catch (Exception fallbackError) {
                System.err.println("Fallback method also failed: " + fallbackError.getMessage());
                fallbackError.printStackTrace();

                // Último recurso: buscar todos y filtrar
                try {
                    System.out.println("Trying final fallback with findAll...");
                    List<LoanEntity> allLoans = loanRepository.findAll();
                    List<LoanEntity> activeLoans = allLoans.stream()
                            .filter(loan -> loan.getStatus() == LoanEntity.LoanStatus.ACTIVE)
                            .collect(Collectors.toList());
                    System.out.println("Final fallback returned " + activeLoans.size() + " active loans");
                    return activeLoans;
                } catch (Exception finalError) {
                    System.err.println("All methods failed: " + finalError.getMessage());
                    finalError.printStackTrace();
                    return List.of(); // Retornar lista vacía en caso de error
                }
            }
        }
    }

    // RF6.1: Get overdue loans - VERSIÓN SEGURA
    public List<LoanEntity> getOverdueLoans() {
        try {
            return loanRepository.findOverdueLoans(LocalDate.now());
        } catch (Exception e) {
            System.err.println("Error getting overdue loans: " + e.getMessage());
            // Fallback: filtrar manualmente
            try {
                List<LoanEntity> activeLoans = getActiveLoans();
                LocalDate today = LocalDate.now();
                return activeLoans.stream()
                        .filter(loan -> !loan.getAgreedReturnDate().isAfter(today))
                        .collect(Collectors.toList());
            } catch (Exception fallbackError) {
                System.err.println("Fallback for overdue loans failed: " + fallbackError.getMessage());
                return List.of();
            }
        }
    }

    // Get loans by client - VERSIÓN SEGURA
    public List<LoanEntity> getLoansByClient(ClientEntity client) {
        try {
            if (client == null) {
                return List.of();
            }
            return loanRepository.findByClient(client);
        } catch (Exception e) {
            System.err.println("Error getting loans by client: " + e.getMessage());
            return List.of();
        }
    }

    // Check if client has active loan for specific tool - VERSIÓN MEJORADA
    public Map<String, Object> checkClientToolLoan(ClientEntity client, ToolEntity tool) {
        Map<String, Object> check = new HashMap<>();

        try {
            if (client == null) {
                check.put("hasActiveLoanForTool", false);
                check.put("canLoanThisTool", false);
                check.put("error", "Cliente no encontrado");
                check.put("message", "Cliente no válido");
                return check;
            }

            if (tool == null) {
                check.put("hasActiveLoanForTool", false);
                check.put("canLoanThisTool", false);
                check.put("error", "Herramienta no encontrada");
                check.put("message", "Herramienta no válida");
                return check;
            }

            boolean hasActiveLoan = loanRepository.existsActiveLoanByClientAndTool(client, tool);

            check.put("hasActiveLoanForTool", hasActiveLoan);
            check.put("canLoanThisTool", !hasActiveLoan);
            check.put("clientId", client.getId());
            check.put("clientName", client.getName());
            check.put("toolId", tool.getId());
            check.put("toolName", tool.getName());

            if (hasActiveLoan) {
                // Buscar el préstamo activo específico
                List<LoanEntity> activeLoans = loanRepository.findByClient(client).stream()
                        .filter(l -> l.getStatus() == LoanEntity.LoanStatus.ACTIVE &&
                                l.getTool().getId().equals(tool.getId()))
                        .collect(Collectors.toList());

                if (!activeLoans.isEmpty()) {
                    LoanEntity activeLoan = activeLoans.get(0);
                    check.put("activeLoanId", activeLoan.getId());
                    check.put("loanDate", activeLoan.getLoanDate());
                    check.put("agreedReturnDate", activeLoan.getAgreedReturnDate());
                    check.put("quantity", activeLoan.getQuantity());
                }
                check.put("message", "Cliente ya tiene un préstamo activo de esta herramienta");
            } else {
                check.put("message", "Cliente puede solicitar un préstamo de esta herramienta");
            }

        } catch (Exception e) {
            System.err.println("Error checking client tool loan: " + e.getMessage());
            check.put("hasActiveLoanForTool", false);
            check.put("canLoanThisTool", false);
            check.put("error", "Error al verificar préstamo: " + e.getMessage());
            check.put("message", "Error en la verificación");
        }

        return check;
    }

    // Get active loan count for client - VERSIÓN SEGURA
    public Map<String, Object> getActiveLoanCount(Long clientId) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (clientId == null || clientId <= 0) {
                result.put("clientId", clientId);
                result.put("activeLoanCount", 0);
                result.put("maxAllowed", 5);
                result.put("canRequestMore", false);
                result.put("error", "ID de cliente inválido");
                return result;
            }

            ClientEntity client = clientService.getClientById(clientId);
            if (client == null) {
                result.put("clientId", clientId);
                result.put("activeLoanCount", 0);
                result.put("maxAllowed", 5);
                result.put("canRequestMore", false);
                result.put("error", "Cliente no encontrado");
                return result;
            }

            long count = loanRepository.countActiveLoansByClient(client);

            result.put("clientId", clientId);
            result.put("activeLoanCount", count);
            result.put("maxAllowed", 5);
            result.put("canRequestMore", count < 5);

        } catch (Exception e) {
            System.err.println("Error getting active loan count: " + e.getMessage());
            result.put("clientId", clientId);
            result.put("activeLoanCount", 0);
            result.put("maxAllowed", 5);
            result.put("canRequestMore", false);
            result.put("error", "Error al contar préstamos activos: " + e.getMessage());
        }

        return result;
    }

    // Get current rates - VERSIÓN SEGURA
    public Map<String, Object> getCurrentRates() {
        Map<String, Object> rates = new HashMap<>();
        try {
            rates.put("rentalRate", rateService.getCurrentRentalRate());
            rates.put("lateFeeRate", rateService.getCurrentLateFeeRate());
            rates.put("repairRate", rateService.getCurrentRepairRate());
        } catch (Exception e) {
            System.err.println("Error getting current rates: " + e.getMessage());
            // Valores por defecto
            rates.put("rentalRate", BigDecimal.valueOf(100.0));
            rates.put("lateFeeRate", BigDecimal.valueOf(10.0));
            rates.put("repairRate", BigDecimal.valueOf(30.0)); // 30% como valor decimal
            rates.put("error", "Usando valores por defecto");
        }
        return rates;
    }

    // Métodos básicos - VERSIONES SEGURAS
    public List<LoanEntity> getAllLoans() {
        try {
            return loanRepository.findAll();
        } catch (Exception e) {
            System.err.println("Error getting all loans: " + e.getMessage());
            return List.of();
        }
    }

    public LoanEntity getLoanById(Long id) {
        try {
            return loanRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + id));
        } catch (Exception e) {
            throw new RuntimeException("Error getting loan by ID: " + e.getMessage());
        }
    }

    // RF2.1: Create new loan - VERSIÓN MEJORADA CON ESTADOS CORRECTOS E INSTANCIAS
    @Transactional
    public LoanEntity createLoan(LoanEntity loan) {
        try {
            validateLoanCreation(loan);

            if (loan.getLoanDate() == null) {
                loan.setLoanDate(LocalDate.now());
            }

            // Use tool's specific rental rate
            ToolEntity tool = loan.getTool();
            BigDecimal dailyRate = tool.getRentalRate();

            if (dailyRate == null || dailyRate.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("La herramienta debe tener una tarifa de arriendo válida");
            }

            loan.setDailyRate(dailyRate);
            loan.setStatus(LoanEntity.LoanStatus.ACTIVE);

            // 🔧 CORRECCIÓN: Guardar stock ANTES del movimiento para Kardex
            int stockBeforeMovement = tool.getCurrentStock();

            // Update tool stock AND status
            int newStock = tool.getCurrentStock() - loan.getQuantity();
            tool.setCurrentStock(newStock);

            // 🔧 CORRECCIÓN: Actualizar estado de la herramienta según el stock
            // La herramienta solo cambia a LOANED cuando el stock llega a 0 (después del préstamo)
            if (newStock < 0) {
                throw new RuntimeException("Error: Stock negativo detectado");
            } else if (newStock == 0) {
                tool.setStatus(ToolEntity.ToolStatus.LOANED); // Completamente prestada
                System.out.println("Tool " + tool.getName() + " status changed to LOANED (stock: " + newStock + ")");
            } else {
                // Si aún hay stock disponible, mantener como AVAILABLE
                tool.setStatus(ToolEntity.ToolStatus.AVAILABLE);
                System.out.println("Tool " + tool.getName() + " remains AVAILABLE (stock: " + newStock + "/" + tool.getInitialStock() + ")");
            }

            // 🔧 NUEVO: Actualizar instancias individuales de herramientas (opcional)
            // Las instancias son opcionales - si no existen, el préstamo se crea igual usando solo el stock
            ToolInstanceEntity reservedInstance = tryReserveToolInstancesAndGetFirst(tool.getId(), loan.getQuantity());

            // 🔧 CORRECCIÓN: Guardar directamente con el repositorio para evitar validaciones innecesarias
            // updateTool() valida todos los campos, pero aquí solo estamos actualizando stock y estado
            toolRepository.save(tool);
            System.out.println("Tool stock updated: " + tool.getName() + " - New stock: " + newStock);

            LoanEntity savedLoan = loanRepository.save(loan);

            // Create Kardex movement (opcional) - 🔧 CORRECCIÓN: Pasar stock ANTES del movimiento e instancia reservada
            if (kardexMovementService != null) {
                try {
                    kardexMovementService.createMovement(
                            loan.getTool(),
                            reservedInstance,  // 🔧 NUEVO: Pasar la instancia específica reservada
                            KardexMovementEntity.MovementType.LOAN,
                            loan.getQuantity(),
                            "Loan #" + savedLoan.getId() + " - Client: " + loan.getClient().getName(),
                            savedLoan,
                            stockBeforeMovement  // 🔧 NUEVO: Pasar stock antes del movimiento
                    );
                } catch (Exception e) {
                    System.err.println("Error creating kardex movement: " + e.getMessage());
                    // No fallar el préstamo por esto
                }
            }

            return savedLoan;
        } catch (Exception e) {
            System.err.println("Error creating loan: " + e.getMessage());
            throw new RuntimeException("Error al crear préstamo: " + e.getMessage());
        }
    }

    // Validate loan creation - VERSIÓN MEJORADA
    private void validateLoanCreation(LoanEntity loan) {
        if (loan == null) {
            throw new RuntimeException("Datos del préstamo son requeridos");
        }
        if (loan.getClient() == null) {
            throw new RuntimeException("Cliente es requerido para el préstamo");
        }
        if (loan.getTool() == null) {
            throw new RuntimeException("Herramienta es requerida para el préstamo");
        }
        if (loan.getQuantity() == null || loan.getQuantity() <= 0) {
            throw new RuntimeException("La cantidad debe ser mayor a 0");
        }
        // NUEVA VALIDACIÓN: Solo se permite cantidad = 1
        if (loan.getQuantity() != 1) {
            throw new RuntimeException("Solo se permite prestar 1 unidad por préstamo. Un cliente no puede tener múltiples unidades de la misma herramienta simultáneamente.");
        }
        if (loan.getAgreedReturnDate() == null) {
            throw new RuntimeException("Fecha acordada de devolución es requerida");
        }

        // Verificar estado del cliente
        if (loan.getClient().getStatus() != ClientEntity.ClientStatus.ACTIVE) {
            throw new RuntimeException("Cliente está restringido y no puede solicitar préstamos");
        }

        // Verificar préstamos vencidos
        List<LoanEntity> overdueLoans = loanRepository.findOverdueLoansByClient(
                loan.getClient(), LocalDate.now());
        if (!overdueLoans.isEmpty()) {
            throw new RuntimeException("Cliente tiene préstamos vencidos y no puede solicitar nuevos préstamos");
        }

        // Verificar multas impagas (con manejo de errores)
        try {
            if (fineService.clientHasUnpaidFines(loan.getClient())) {
                throw new RuntimeException("Cliente tiene multas impagas y no puede solicitar préstamos");
            }
        } catch (Exception e) {
            System.err.println("Error checking unpaid fines, allowing loan: " + e.getMessage());
        }

        // Verificar estado de herramienta
        if (loan.getTool().getStatus() != ToolEntity.ToolStatus.AVAILABLE) {
            throw new RuntimeException("Herramienta no está disponible para préstamo");
        }

        // Verificar stock
        if (loan.getQuantity() > loan.getTool().getCurrentStock()) {
            throw new RuntimeException("Stock insuficiente. Solicitado: " + loan.getQuantity() +
                    ", Disponible: " + loan.getTool().getCurrentStock());
        }

        // Verificar límite de préstamos
        long activeLoanCount = loanRepository.countActiveLoansByClient(loan.getClient());
        if (activeLoanCount >= 5) {
            throw new RuntimeException("Cliente ha alcanzado el máximo de 5 préstamos activos");
        }

        // Verificar préstamo existente de la misma herramienta
        boolean hasActiveLoanForTool = loanRepository.existsActiveLoanByClientAndTool(
                loan.getClient(), loan.getTool());
        if (hasActiveLoanForTool) {
            throw new RuntimeException("Cliente ya tiene un préstamo activo de esta herramienta");
        }

        // Verificar fecha
        LocalDate loanDate = loan.getLoanDate() != null ? loan.getLoanDate() : LocalDate.now();
        if (loan.getAgreedReturnDate().isBefore(loanDate) ||
                loan.getAgreedReturnDate().isEqual(loanDate)) {
            throw new RuntimeException("La fecha de devolución debe ser posterior a la fecha del préstamo");
        }
    }

    // MÉTODOS FALTANTES QUE NECESITA EL CONTROLADOR:

    // Update loan - NUEVO MÉTODO REQUERIDO
    @Transactional
    public LoanEntity updateLoan(Long id, LoanEntity updatedLoan) {
        try {
            LoanEntity existingLoan = loanRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + id));

            if (existingLoan.getStatus() != LoanEntity.LoanStatus.ACTIVE) {
                throw new RuntimeException("Can only update active loans");
            }

            // Solo permitir actualizar fecha acordada y notas
            if (updatedLoan.getAgreedReturnDate() != null) {
                if (updatedLoan.getAgreedReturnDate().isBefore(LocalDate.now())) {
                    throw new RuntimeException("Agreed return date cannot be in the past");
                }
                existingLoan.setAgreedReturnDate(updatedLoan.getAgreedReturnDate());
            }

            if (updatedLoan.getNotes() != null) {
                existingLoan.setNotes(updatedLoan.getNotes());
            }

            return loanRepository.save(existingLoan);
        } catch (Exception e) {
            System.err.println("Error updating loan: " + e.getMessage());
            throw new RuntimeException("Error al actualizar préstamo: " + e.getMessage());
        }
    }

    // Delete loan - NUEVO MÉTODO REQUERIDO
    @Transactional
    public void deleteLoan(Long id) {
        try {
            LoanEntity loan = loanRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + id));

            if (loan.getStatus() == LoanEntity.LoanStatus.ACTIVE) {
                throw new RuntimeException("Cannot delete active loan. Please return the tool first.");
            }

            // Verificar si hay multas asociadas
            try {
                List<FineEntity> associatedFines = fineService.getFinesByLoan(loan);
                if (!associatedFines.isEmpty()) {
                    throw new RuntimeException("Cannot delete loan with associated fines.");
                }
            } catch (Exception e) {
                System.err.println("Error checking associated fines: " + e.getMessage());
                // Continuar con la eliminación si no se pueden verificar las multas
            }

            // Eliminar movimientos de kardex si existe el servicio
            if (kardexMovementService != null) {
                try {
                    kardexMovementService.deleteMovementsByLoan(id);
                } catch (Exception e) {
                    System.err.println("Error deleting kardex movements: " + e.getMessage());
                }
            }

            loanRepository.deleteById(id);
        } catch (Exception e) {
            System.err.println("Error deleting loan: " + e.getMessage());
            throw new RuntimeException("Error al eliminar préstamo: " + e.getMessage());
        }
    }

    // Return tool - VERSIÓN CORREGIDA CON ESTADOS CORRECTOS E INSTANCIAS Y TIPOS DE DAÑO
    @Transactional
    public LoanEntity returnTool(Long loanId, Boolean damaged, String damageType, String notes) {
        try {
            LoanEntity loan = loanRepository.findById(loanId)
                    .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + loanId));

            if (loan.getStatus() != LoanEntity.LoanStatus.ACTIVE) {
                throw new RuntimeException("Loan is not active and cannot be returned");
            }

            loan.setActualReturnDate(LocalDate.now());

            // Agregar notas si se proporcionan
            if (notes != null && !notes.trim().isEmpty()) {
                String existingNotes = loan.getNotes() != null ? loan.getNotes() : "";
                loan.setNotes(existingNotes.isEmpty() ? notes : existingNotes + "\n" + notes);
            }

            // 🔧 CORRECCIÓN: Actualizar stock Y estado de herramienta según el caso
            ToolEntity tool = loan.getTool();

            // 🔧 Guardar stock ANTES de cualquier modificación (para el Kardex)
            int stockBeforeReturn = tool.getCurrentStock();

            if (damaged != null && damaged) {
                // Herramienta dañada - verificar tipo de daño
                System.out.println("Tool " + tool.getName() + " - processing damaged return. Type: " + damageType);

                // Verificar si es daño irreparable
                boolean isIrreparable = damageType != null && damageType.equals("IRREPARABLE");

                if (isIrreparable) {
                    //  Dar de baja inmediatamente
                    System.out.println("IRREPARABLE DAMAGE - Decommissioning instances immediately");

                    if (toolInstanceService != null) {
                        try {
                            // Cambiar instancias directamente a DECOMMISSIONED
                            List<ToolInstanceEntity> decommissionedInstances =
                                toolInstanceService.decommissionInstances(tool.getId(), loan.getQuantity());
                            System.out.println("Successfully decommissioned " + decommissionedInstances.size() + " instances");

                            // 🆕 REGISTRAR MOVIMIENTO DE BAJA (DECOMMISSION) EN EL KARDEX
                            if (kardexMovementService != null && !decommissionedInstances.isEmpty()) {
                                List<Long> instanceIds = decommissionedInstances.stream()
                                    .map(ToolInstanceEntity::getId)
                                    .collect(java.util.stream.Collectors.toList());

                                String decommissionDescription = "Baja por daño irreparable en devolución - Préstamo #" +
                                    loan.getId() + " - Cliente: " + loan.getClient().getName();

                                // Capturar stock antes de actualizar
                                int stockBefore = tool.getCurrentStock();

                                kardexMovementService.createDecommissionMovement(
                                    tool,
                                    loan.getQuantity(),
                                    decommissionDescription,
                                    instanceIds,
                                    stockBefore
                                );
                                System.out.println("Registered DECOMMISSION movement in kardex for tool " + tool.getName());
                            }
                        } catch (Exception e) {
                            System.err.println("Error decommissioning tool instances: " + e.getMessage());
                        }
                    }

                } else {
                    // 🟡 DAÑO LEVE: Marcar en reparación (se restaurará al pagar multa)
                    System.out.println("MINOR DAMAGE - Marking instances as under repair");

                    if (toolInstanceService != null) {
                        try {
                            List<ToolInstanceEntity> returnedInstances = toolInstanceService.returnInstancesFromLoan(
                                    tool.getId(), loan.getQuantity(), true); // true = damaged
                            System.out.println("Successfully set " + returnedInstances.size() + " instances to UNDER_REPAIR");

                            // 🆕 REGISTRAR MOVIMIENTO DE REPARACIÓN EN EL KARDEX
                            if (kardexMovementService != null && !returnedInstances.isEmpty()) {
                                String repairDescription = "Daño leve detectado en devolución - Préstamo #" + loan.getId() +
                                    " - Cliente: " + loan.getClient().getName();
                                kardexMovementService.createRepairMovement(tool, repairDescription,
                                    returnedInstances.get(0).getId());
                                System.out.println("Registered REPAIR movement in kardex for tool " + tool.getName());
                            }
                        } catch (Exception e) {
                            System.err.println("Error updating tool instances to UNDER_REPAIR: " + e.getMessage());
                        }
                    }

                    // NO devolver stock aún, se devolverá cuando se pague la multa
                }
            } else {
                // Devolución normal - restaurar stock
                int newStock = tool.getCurrentStock() + loan.getQuantity();
                tool.setCurrentStock(newStock);
                System.out.println("Tool " + tool.getName() + " stock restored: " + newStock + "/" + tool.getInitialStock());

                // 🔧 NUEVO: Actualizar instancias individuales a AVAILABLE
                if (toolInstanceService != null) {
                    try {
                        System.out.println("Returning " + loan.getQuantity() + " instances in good condition...");
                        List<ToolInstanceEntity> returnedInstances = toolInstanceService.returnInstancesFromLoan(
                                tool.getId(), loan.getQuantity(), false); // false = not damaged
                        System.out.println("Successfully set " + returnedInstances.size() + " instances to AVAILABLE");
                    } catch (Exception e) {
                        System.err.println("Error updating tool instances to AVAILABLE: " + e.getMessage());
                    }
                } else {
                    System.out.println("ToolInstanceService not available - skipping individual instance updates");
                }
            }

            // 🔧 CORRECCIÓN CRÍTICA: Actualizar estado basado en stock DISPONIBLE
            // Usar el servicio de instancias para determinar el estado correcto
            if (toolInstanceService != null) {
                try {
                    Long availableCount = toolInstanceService.getAvailableCount(tool.getId());
                    Long loanedCount = toolInstanceService.getInstancesByStatus(
                        ToolInstanceEntity.ToolInstanceStatus.LOANED).stream()
                        .filter(i -> i.getTool().getId().equals(tool.getId()))
                        .count();
                    Long decommissionedCount = toolInstanceService.getInstancesByStatus(
                        ToolInstanceEntity.ToolInstanceStatus.DECOMMISSIONED).stream()
                        .filter(i -> i.getTool().getId().equals(tool.getId()))
                        .count();
                    Long underRepairCount = toolInstanceService.getInstancesByStatus(
                        ToolInstanceEntity.ToolInstanceStatus.UNDER_REPAIR).stream()
                        .filter(i -> i.getTool().getId().equals(tool.getId()))
                        .count();

                    if (availableCount > 0) {
                        tool.setStatus(ToolEntity.ToolStatus.AVAILABLE);
                        System.out.println("Tool " + tool.getName() + " status: AVAILABLE (available: " + availableCount + ")");
                    } else if (loanedCount > 0) {
                        tool.setStatus(ToolEntity.ToolStatus.LOANED);
                        System.out.println("Tool " + tool.getName() + " status: LOANED (loaned: " + loanedCount + ")");
                    } else if (underRepairCount > 0 && decommissionedCount == 0) {
                        // Solo en reparación, sin bajas
                        tool.setStatus(ToolEntity.ToolStatus.UNDER_REPAIR);
                        System.out.println("Tool " + tool.getName() + " status: UNDER_REPAIR (under repair: " + underRepairCount + ")");
                    } else if (decommissionedCount > 0) {
                        // Hay instancias dadas de baja - marcar como DECOMMISSIONED
                        // Esto sucede si TODAS están dadas de baja o si la mayoría lo están
                        long totalInstances = availableCount + loanedCount + decommissionedCount + underRepairCount;
                        if (decommissionedCount >= totalInstances ||
                            (availableCount == 0 && loanedCount == 0 && underRepairCount == 0)) {
                            tool.setStatus(ToolEntity.ToolStatus.DECOMMISSIONED);
                            System.out.println("Tool " + tool.getName() + " status: DECOMMISSIONED (all instances decommissioned)");
                        } else {
                            // Algunas dadas de baja pero otras aún operativas
                            tool.setStatus(ToolEntity.ToolStatus.UNDER_REPAIR);
                            System.out.println("Tool " + tool.getName() + " status: UNDER_REPAIR (mixed: " +
                                decommissionedCount + " decommissioned, " + underRepairCount + " under repair)");
                        }
                    } else {
                        // Sin instancias en ningún estado (no debería pasar)
                        tool.setStatus(ToolEntity.ToolStatus.DECOMMISSIONED);
                        System.out.println("Tool " + tool.getName() + " status: DECOMMISSIONED (no instances found)");
                    }
                } catch (Exception e) {
                    System.err.println("Error determining tool status from instances: " + e.getMessage());
                    // Fallback: usar lógica basada en stock
                    if (tool.getCurrentStock() > 0) {
                        tool.setStatus(ToolEntity.ToolStatus.AVAILABLE);
                    } else {
                        tool.setStatus(ToolEntity.ToolStatus.LOANED);
                    }
                }
            } else {
                // Fallback si no hay servicio de instancias
                if (tool.getCurrentStock() > 0) {
                    tool.setStatus(ToolEntity.ToolStatus.AVAILABLE);
                    System.out.println("Tool " + tool.getName() + " status: AVAILABLE (stock: " + tool.getCurrentStock() + ")");
                } else {
                    tool.setStatus(ToolEntity.ToolStatus.LOANED);
                    System.out.println("Tool " + tool.getName() + " status: LOANED (no stock)");
                }
            }

            try {
                toolService.updateTool(tool.getId(), tool);
                System.out.println("Tool updated successfully: " + tool.getName() + " - Status: " + tool.getStatus());
            } catch (Exception e) {
                System.err.println("Error updating tool stock and status: " + e.getMessage());
                // No fallar la devolución por esto
            }

            // Calcular y crear multas automáticamente si es necesario
            try {
                calculateAndCreateFinesAutomatically(loan, damaged != null && damaged, damageType);
            } catch (Exception e) {
                System.err.println("Error calculating fines: " + e.getMessage());
                // No fallar la devolución por esto
            }

            // Actualizar estado del préstamo
            if (damaged != null && damaged) {
                loan.setStatus(LoanEntity.LoanStatus.DAMAGED);
            } else if (isOverdue(loan)) {
                loan.setStatus(LoanEntity.LoanStatus.OVERDUE);
            } else {
                loan.setStatus(LoanEntity.LoanStatus.RETURNED);
            }

            // Crear movimiento de kardex para devolución
            if (kardexMovementService != null) {
                try {
                    kardexMovementService.createMovement(
                            loan.getTool(),
                            KardexMovementEntity.MovementType.RETURN,
                            loan.getQuantity(),
                            "Return loan #" + loan.getId() + " - " + ((damaged != null && damaged) ? "With damage" : "Good condition"),
                            loan,
                            stockBeforeReturn  // 🔧 Pasar el stock ANTES de la devolución
                    );
                } catch (Exception e) {
                    System.err.println("Error creating kardex movement: " + e.getMessage());
                }
            }

            return loanRepository.save(loan);
        } catch (Exception e) {
            System.err.println("Error returning tool: " + e.getMessage());
            throw new RuntimeException("Error al procesar devolución: " + e.getMessage());
        }
    }

    // Get loans by tool - NUEVO MÉTODO REQUERIDO
    public List<LoanEntity> getLoansByTool(ToolEntity tool) {
        try {
            if (tool == null) {
                return List.of();
            }
            return loanRepository.findByTool(tool);
        } catch (Exception e) {
            System.err.println("Error getting loans by tool: " + e.getMessage());
            return List.of();
        }
    }

    // Get loan summary - NUEVO MÉTODO REQUERIDO
    public Map<String, Object> getLoanSummary() {
        try {
            List<LoanEntity> allLoans = getAllLoans();
            List<LoanEntity> activeLoans = getActiveLoans();
            List<LoanEntity> overdueLoans = getOverdueLoans();

            Map<String, Object> summary = new HashMap<>();

            summary.put("totalLoans", allLoans.size());
            summary.put("activeLoans", activeLoans.size());
            summary.put("overdueLoans", overdueLoans.size());

            long returnedLoans = allLoans.stream()
                    .filter(loan -> loan.getStatus() == LoanEntity.LoanStatus.RETURNED)
                    .count();

            long damagedLoans = allLoans.stream()
                    .filter(loan -> loan.getStatus() == LoanEntity.LoanStatus.DAMAGED)
                    .count();

            summary.put("returnedLoans", returnedLoans);
            summary.put("damagedLoans", damagedLoans);

            // Estadísticas del mes actual
            LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
            LocalDate endOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

            long loansThisMonth = allLoans.stream()
                    .filter(loan -> loan.getLoanDate() != null &&
                            loan.getLoanDate().isAfter(startOfMonth.minusDays(1)) &&
                            loan.getLoanDate().isBefore(endOfMonth.plusDays(1)))
                    .count();

            summary.put("loansThisMonth", loansThisMonth);

            // Clientes activos únicos
            long uniqueActiveClients = activeLoans.stream()
                    .map(loan -> loan.getClient().getId())
                    .distinct()
                    .count();

            summary.put("uniqueActiveClients", uniqueActiveClients);

            return summary;
        } catch (Exception e) {
            System.err.println("Error getting loan summary: " + e.getMessage());
            Map<String, Object> errorSummary = new HashMap<>();
            errorSummary.put("error", "Error al obtener resumen de préstamos");
            errorSummary.put("totalLoans", 0);
            errorSummary.put("activeLoans", 0);
            errorSummary.put("overdueLoans", 0);
            return errorSummary;
        }
    }

    // Get validation summary - NUEVO MÉTODO REQUERIDO
    public LoanValidationSummary getLoanValidationSummary(ClientEntity client, ToolEntity tool, Integer quantity) {
        try {
            LoanValidationSummary summary = new LoanValidationSummary();

            // Validar cliente
            try {
                validateClientEligibility(client);
                summary.setClientEligible(true);
            } catch (RuntimeException e) {
                summary.setClientEligible(false);
                summary.setClientIssue(e.getMessage());
            }

            // Validar herramienta
            try {
                validateToolAvailability(tool, quantity);
                summary.setToolAvailable(true);
            } catch (RuntimeException e) {
                summary.setToolAvailable(false);
                summary.setToolIssue(e.getMessage());
            }

            summary.setHasExistingLoanForTool(clientHasActiveLoanForTool(client, tool));

            // Obtener tarifas actuales
            try {
                summary.setCurrentDailyRate(rateService.getCurrentRentalRate());
                summary.setCurrentLateFeeRate(rateService.getCurrentLateFeeRate());
            } catch (Exception e) {
                summary.setCurrentDailyRate(BigDecimal.valueOf(100.0));
                summary.setCurrentLateFeeRate(BigDecimal.valueOf(10.0));
            }

            return summary;
        } catch (Exception e) {
            System.err.println("Error getting loan validation summary: " + e.getMessage());
            LoanValidationSummary errorSummary = new LoanValidationSummary();
            errorSummary.setClientEligible(false);
            errorSummary.setToolAvailable(false);
            errorSummary.setClientIssue("Error en validación: " + e.getMessage());
            return errorSummary;
        }
    }

    // ============================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // ============================================================================

    private void validateClientEligibility(ClientEntity client) {
        if (client == null) {
            throw new RuntimeException("Cliente es requerido");
        }
        if (client.getStatus() != ClientEntity.ClientStatus.ACTIVE) {
            throw new RuntimeException("Cliente no está activo y no puede solicitar préstamos");
        }

        List<LoanEntity> overdueLoans = loanRepository.findOverdueLoansByClient(client, LocalDate.now());
        if (!overdueLoans.isEmpty()) {
            throw new RuntimeException("Cliente tiene " + overdueLoans.size() + " préstamo(s) vencido(s)");
        }

        try {
            if (fineService.clientHasUnpaidFines(client)) {
                throw new RuntimeException("Cliente tiene multas impagas");
            }
        } catch (Exception e) {
            System.err.println("Error checking unpaid fines: " + e.getMessage());
        }

        long activeLoanCount = loanRepository.countActiveLoansByClient(client);
        if (activeLoanCount >= 5) {
            throw new RuntimeException("Cliente ha alcanzado el límite de 5 préstamos activos");
        }
    }

    private void validateToolAvailability(ToolEntity tool, Integer quantity) {
        if (tool == null) {
            throw new RuntimeException("Herramienta no encontrada");
        }
        if (tool.getStatus() != ToolEntity.ToolStatus.AVAILABLE) {
            throw new RuntimeException("Herramienta no está disponible para préstamo");
        }
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Cantidad debe ser mayor a 0");
        }
        if (quantity > tool.getCurrentStock()) {
            throw new RuntimeException("Stock insuficiente");
        }
    }

    private boolean clientHasActiveLoanForTool(ClientEntity client, ToolEntity tool) {
        if (client == null || tool == null) {
            return false;
        }
        return loanRepository.existsActiveLoanByClientAndTool(client, tool);
    }

    private boolean isOverdue(LoanEntity loan) {
        return loan.getActualReturnDate() != null &&
                loan.getActualReturnDate().isAfter(loan.getAgreedReturnDate());
    }

    private void calculateAndCreateFinesAutomatically(LoanEntity loan, boolean damaged, String damageTypeStr) {
        try {
            LocalDate returnDate = loan.getActualReturnDate();
            if (returnDate == null) return;

            // Multa por atraso
            if (returnDate.isAfter(loan.getAgreedReturnDate())) {
                long daysLate = ChronoUnit.DAYS.between(loan.getAgreedReturnDate(), returnDate);
                BigDecimal lateFeeRate = rateService.getCurrentLateFeeRate();
                fineService.createLateFine(loan, daysLate, lateFeeRate);
            }

            // Multa por daño - NUEVO: usar damageType
            if (damaged && loan.getTool().getReplacementValue() != null) {
                // Convertir string a enum
                FineEntity.DamageType damageType;
                try {
                    damageType = FineEntity.DamageType.valueOf(damageTypeStr);
                } catch (Exception e) {
                    System.err.println("Invalid damage type: " + damageTypeStr + ", defaulting to MINOR");
                    damageType = FineEntity.DamageType.MINOR;
                }

                String description = "Herramienta devuelta con daño " +
                    (damageType == FineEntity.DamageType.MINOR ? "leve (reparable)" : "irreparable");

                fineService.createDamageFineWithType(loan, damageType, description);
                System.out.println("Created " + damageType + " damage fine for loan #" + loan.getId());
            }
        } catch (Exception e) {
            System.err.println("Error creating automatic fines: " + e.getMessage());
        }
    }

    // Helper method to try reserving tool instances without failing the main transaction
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    protected void tryReserveToolInstances(Long toolId, Integer quantity) {
        tryReserveToolInstancesAndGetFirst(toolId, quantity);
    }

    // Helper method to try reserving tool instances and return the first one
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    protected ToolInstanceEntity tryReserveToolInstancesAndGetFirst(Long toolId, Integer quantity) {
        if (toolInstanceService == null) {
            System.out.println("ℹ️ ToolInstanceService not available - using only main tool stock counter");
            return null;
        }

        try {
            System.out.println("Attempting to reserve " + quantity + " instances for loan...");
            List<ToolInstanceEntity> reservedInstances = toolInstanceService.reserveInstancesForLoan(
                    toolId, quantity);
            System.out.println("✅ Successfully reserved " + reservedInstances.size() + " instances:");
            for (ToolInstanceEntity instance : reservedInstances) {
                System.out.println("  - Instance ID: " + instance.getId() + " - Status: " + instance.getStatus());
            }
            // Return the first reserved instance for kardex tracking
            return !reservedInstances.isEmpty() ? reservedInstances.get(0) : null;
        } catch (RuntimeException e) {
            // This is expected and normal if tool instances are not configured
            System.err.println("⚠️ Could not reserve tool instances: " + e.getMessage());
            System.err.println("⚠️ This is normal if tool instances are not configured for this tool.");
            System.err.println("✅ Loan will proceed using only the main tool stock counter.");
            // Do NOT rethrow - instances are optional
            return null;
        } catch (Exception e) {
            System.err.println("⚠️ Unexpected error reserving tool instances: " + e.getMessage());
            System.err.println("✅ Loan will proceed using only the main tool stock counter.");
            // Do NOT rethrow - instances are optional
            return null;
        }
    }

    // ============================================================================
    // MÉTODOS PARA MULTAS AUTOMÁTICAS POR PRÉSTAMOS ATRASADOS
    // ============================================================================

    /**
     * Genera o actualiza multas automáticamente para todos los préstamos atrasados
     * Este método debería ser llamado periódicamente (diariamente) o bajo demanda
     *
     * @return Resumen de las operaciones realizadas
     */
    @Transactional
    public Map<String, Object> generateOverdueFinesForAllLoans() {
        Map<String, Object> result = new HashMap<>();

        try {
            System.out.println("========================================");
            System.out.println("🔄 GENERANDO MULTAS POR PRÉSTAMOS ATRASADOS");
            System.out.println("📅 Fecha: " + LocalDate.now());
            System.out.println("========================================");

            // Obtener tarifa de multa actual
            BigDecimal lateFeeRate = rateService.getCurrentLateFeeRate();
            System.out.println("💰 Tarifa de multa diaria: $" + lateFeeRate);

            // Obtener todos los préstamos atrasados
            LocalDate today = LocalDate.now();
            List<LoanEntity> overdueLoans = getOverdueLoans();

            System.out.println("📋 Préstamos atrasados encontrados: " + overdueLoans.size());

            if (overdueLoans.isEmpty()) {
                System.out.println("✅ No hay préstamos atrasados.");
                result.put("success", true);
                result.put("message", "No hay préstamos atrasados");
                result.put("overdueLoans", 0);
                result.put("finesCreated", 0);
                result.put("finesUpdated", 0);
                return result;
            }

            int finesCreated = 0;
            int finesUpdated = 0;
            int errors = 0;

            // Procesar cada préstamo atrasado
            for (LoanEntity loan : overdueLoans) {
                try {
                    boolean wasCreated = generateOrUpdateOverdueFine(loan, lateFeeRate, today);
                    if (wasCreated) {
                        finesCreated++;
                    } else {
                        finesUpdated++;
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error procesando préstamo #" + loan.getId() + ": " + e.getMessage());
                    errors++;
                }
            }

            System.out.println("\n📊 RESUMEN:");
            System.out.println("   ✅ Multas creadas: " + finesCreated);
            System.out.println("   🔄 Multas actualizadas: " + finesUpdated);
            System.out.println("   ⚠️  Errores: " + errors);
            System.out.println("========================================\n");

            result.put("success", true);
            result.put("message", "Multas generadas exitosamente");
            result.put("overdueLoans", overdueLoans.size());
            result.put("finesCreated", finesCreated);
            result.put("finesUpdated", finesUpdated);
            result.put("errors", errors);
            result.put("lateFeeRate", lateFeeRate);

        } catch (Exception e) {
            System.err.println("❌ ERROR en generación de multas: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Genera o actualiza una multa por atraso para un préstamo específico
     *
     * @param loan Préstamo atrasado
     * @param lateFeeRate Tarifa de multa diaria
     * @param currentDate Fecha actual
     * @return true si se creó una nueva multa, false si se actualizó una existente
     */
    @Transactional
    public boolean generateOrUpdateOverdueFine(LoanEntity loan, BigDecimal lateFeeRate, LocalDate currentDate) {
        // Calcular días de atraso
        long daysOverdue = ChronoUnit.DAYS.between(loan.getAgreedReturnDate(), currentDate);

        if (daysOverdue <= 0) {
            return false; // No está atrasado
        }

        // Buscar si ya existe una multa de atraso activa para este préstamo
        List<FineEntity> existingFines = fineService.getFinesByLoan(loan);
        FineEntity existingLateFine = existingFines.stream()
                .filter(fine -> fine.getType() == FineEntity.FineType.LATE_RETURN)
                .filter(fine -> !fine.getPaid())
                .filter(fine -> fine.getDamageType() == null) // Solo multas por atraso, no por daños
                .findFirst()
                .orElse(null);

        if (existingLateFine != null) {
            // ACTUALIZAR multa existente
            updateExistingOverdueFine(existingLateFine, daysOverdue, lateFeeRate, loan);
            return false; // Se actualizó
        } else {
            // CREAR nueva multa
            createNewOverdueFine(loan, daysOverdue, lateFeeRate);
            return true; // Se creó
        }
    }

    /**
     * Actualiza una multa de atraso existente con los días acumulados
     */
    @Transactional
    protected void updateExistingOverdueFine(FineEntity fine, long daysOverdue, BigDecimal lateFeeRate, LoanEntity loan) {
        try {
            // Calcular nuevo monto
            BigDecimal newAmount = lateFeeRate.multiply(BigDecimal.valueOf(daysOverdue));

            // Solo actualizar si el monto cambió (días aumentaron)
            if (newAmount.compareTo(fine.getAmount()) > 0) {
                BigDecimal previousAmount = fine.getAmount();
                fine.setAmount(newAmount);
                fine.setDescription(
                    "Multa por préstamo atrasado - " + daysOverdue + " día(s) de atraso " +
                    "(Préstamo #" + loan.getId() + " - Cliente: " + loan.getClient().getName() + ")"
                );

                fineService.updateFine(fine.getId(), fine.getDescription(), fine.getDueDate());

                System.out.println("🔄 Multa #" + fine.getId() + " actualizada:");
                System.out.println("   Préstamo: #" + loan.getId());
                System.out.println("   Cliente: " + loan.getClient().getName());
                System.out.println("   Días atraso: " + daysOverdue);
                System.out.println("   Monto anterior: $" + previousAmount);
                System.out.println("   Monto nuevo: $" + newAmount);
            }
        } catch (Exception e) {
            System.err.println("Error actualizando multa #" + fine.getId() + ": " + e.getMessage());
            throw new RuntimeException("Error actualizando multa: " + e.getMessage());
        }
    }

    /**
     * Crea una nueva multa de atraso para un préstamo
     */
    @Transactional
    protected void createNewOverdueFine(LoanEntity loan, long daysOverdue, BigDecimal lateFeeRate) {
        try {
            BigDecimal fineAmount = lateFeeRate.multiply(BigDecimal.valueOf(daysOverdue));

            FineEntity fine = new FineEntity();
            fine.setClient(loan.getClient());
            fine.setLoan(loan);
            fine.setType(FineEntity.FineType.LATE_RETURN);
            fine.setAmount(fineAmount);
            fine.setDescription(
                "Multa por préstamo atrasado - " + daysOverdue + " día(s) de atraso " +
                "(Préstamo #" + loan.getId() + " - Cliente: " + loan.getClient().getName() + ")"
            );
            fine.setDueDate(LocalDate.now().plusDays(30)); // 30 días para pagar
            fine.setPaid(false);

            FineEntity savedFine = fineService.createFine(fine);

            System.out.println("✅ Nueva multa creada:");
            System.out.println("   ID: #" + savedFine.getId());
            System.out.println("   Préstamo: #" + loan.getId());
            System.out.println("   Cliente: " + loan.getClient().getName());
            System.out.println("   Días atraso: " + daysOverdue);
            System.out.println("   Monto: $" + fineAmount);

        } catch (Exception e) {
            System.err.println("Error creando nueva multa para préstamo #" + loan.getId() + ": " + e.getMessage());
            throw new RuntimeException("Error creando multa: " + e.getMessage());
        }
    }

    /**
     * Obtiene estadísticas sobre multas automáticas por préstamos atrasados
     */
    public Map<String, Object> getOverdueFinesStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            List<LoanEntity> overdueLoans = getOverdueLoans();

            int loansWithFines = 0;
            int loansWithoutFines = 0;
            BigDecimal totalFineAmount = BigDecimal.ZERO;
            BigDecimal totalPotentialAmount = BigDecimal.ZERO;

            BigDecimal lateFeeRate = rateService.getCurrentLateFeeRate();

            for (LoanEntity loan : overdueLoans) {
                long daysOverdue = ChronoUnit.DAYS.between(
                    loan.getAgreedReturnDate(),
                    LocalDate.now()
                );

                BigDecimal potentialAmount = lateFeeRate.multiply(BigDecimal.valueOf(daysOverdue));
                totalPotentialAmount = totalPotentialAmount.add(potentialAmount);

                // Buscar multa existente
                List<FineEntity> fines = fineService.getFinesByLoan(loan);
                FineEntity lateFine = fines.stream()
                        .filter(fine -> fine.getType() == FineEntity.FineType.LATE_RETURN)
                        .filter(fine -> !fine.getPaid())
                        .filter(fine -> fine.getDamageType() == null)
                        .findFirst()
                        .orElse(null);

                if (lateFine != null) {
                    loansWithFines++;
                    totalFineAmount = totalFineAmount.add(lateFine.getAmount());
                } else {
                    loansWithoutFines++;
                }
            }

            stats.put("overdueLoans", overdueLoans.size());
            stats.put("loansWithActiveFine", loansWithFines);
            stats.put("loansWithoutFine", loansWithoutFines);
            stats.put("totalActiveFinesAmount", totalFineAmount);
            stats.put("totalPotentialAmount", totalPotentialAmount);
            stats.put("lateFeeRate", lateFeeRate);
            stats.put("needsUpdate", loansWithoutFines > 0 ||
                    totalFineAmount.compareTo(totalPotentialAmount) < 0);

        } catch (Exception e) {
            System.err.println("Error obteniendo estadísticas: " + e.getMessage());
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    // CLASE INTERNA PARA VALIDACIÓN - REQUERIDA POR EL CONTROLADOR
    public static class LoanValidationSummary {
        private boolean clientEligible;
        private String clientIssue;
        private boolean toolAvailable;
        private String toolIssue;
        private boolean hasExistingLoanForTool;
        private BigDecimal currentDailyRate;
        private BigDecimal currentLateFeeRate;

        // Getters y setters
        public boolean isClientEligible() { return clientEligible; }
        public void setClientEligible(boolean clientEligible) { this.clientEligible = clientEligible; }

        public String getClientIssue() { return clientIssue; }
        public void setClientIssue(String clientIssue) { this.clientIssue = clientIssue; }

        public boolean isToolAvailable() { return toolAvailable; }
        public void setToolAvailable(boolean toolAvailable) { this.toolAvailable = toolAvailable; }

        public String getToolIssue() { return toolIssue; }
        public void setToolIssue(String toolIssue) { this.toolIssue = toolIssue; }

        public boolean isHasExistingLoanForTool() { return hasExistingLoanForTool; }
        public void setHasExistingLoanForTool(boolean hasExistingLoanForTool) {
            this.hasExistingLoanForTool = hasExistingLoanForTool;
        }

        public BigDecimal getCurrentDailyRate() { return currentDailyRate; }
        public void setCurrentDailyRate(BigDecimal currentDailyRate) {
            this.currentDailyRate = currentDailyRate;
        }

        public BigDecimal getCurrentLateFeeRate() { return currentLateFeeRate; }
        public void setCurrentLateFeeRate(BigDecimal currentLateFeeRate) {
            this.currentLateFeeRate = currentLateFeeRate;
        }

        public boolean canCreateLoan() {
            return clientEligible && toolAvailable && !hasExistingLoanForTool;
        }
    }

    // ============================================================================
}
