// FineService.java - VERSION CORREGIDA
package com.toolrent.backend.services;

import com.toolrent.backend.entities.FineEntity;
import com.toolrent.backend.entities.ClientEntity;
import com.toolrent.backend.entities.LoanEntity;
import com.toolrent.backend.entities.ToolEntity;
import com.toolrent.backend.entities.ToolInstanceEntity;
import com.toolrent.backend.repositories.FineRepository;
import com.toolrent.backend.repositories.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class FineService {

    @Autowired
    private FineRepository fineRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientService clientService;

    @Autowired(required = false)
    private ToolService toolService;

    @Autowired(required = false)
    private ToolInstanceService toolInstanceService;

    @Autowired
    private RateService rateService;

    // Verificar si el cliente tiene multas impagas - VERSIÓN SEGURA
    public boolean clientHasUnpaidFines(ClientEntity client) {
        try {
            if (client == null) {
                return false;
            }
            long unpaidCount = fineRepository.countUnpaidFinesByClient(client);
            return unpaidCount > 0;
        } catch (Exception e) {
            System.err.println("Error checking unpaid fines for client: " + e.getMessage());
            return false; // En caso de error, permitir el préstamo
        }
    }

    // Obtener total de multas impagas - VERSIÓN SEGURA
    public BigDecimal getTotalUnpaidAmount(ClientEntity client) {
        try {
            if (client == null) {
                return BigDecimal.ZERO;
            }
            BigDecimal total = fineRepository.getTotalUnpaidAmountByClient(client);
            return total != null ? total : BigDecimal.ZERO;
        } catch (Exception e) {
            System.err.println("Error getting total unpaid amount for client: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    // Verificar restricciones del cliente - NUEVO MÉTODO REQUERIDO POR EL FRONTEND
    public Map<String, Object> checkClientRestrictions(Long clientId) {
        Map<String, Object> restrictions = new HashMap<>();

        try {
            if (clientId == null || clientId <= 0) {
                restrictions.put("canRequestLoan", false);
                restrictions.put("isRestricted", true);
                restrictions.put("restrictionReason", "ID de cliente inválido");
                restrictions.put("clientStatus", "INVALID");
                return restrictions;
            }

            ClientEntity client = clientService.getClientById(clientId);
            if (client == null) {
                restrictions.put("canRequestLoan", false);
                restrictions.put("isRestricted", true);
                restrictions.put("restrictionReason", "Cliente no encontrado");
                restrictions.put("clientStatus", "NOT_FOUND");
                return restrictions;
            }

            // Obtener multas impagas
            List<FineEntity> unpaidFines = getUnpaidFinesByClient(client);
            BigDecimal totalUnpaidAmount = getTotalUnpaidAmount(client);

            // Contar multas vencidas
            long overdueFinesCount = unpaidFines.stream()
                    .filter(fine -> fine.getDueDate().isBefore(LocalDate.now()))
                    .count();

            boolean canRequestLoan = unpaidFines.isEmpty();
            boolean isRestricted = !unpaidFines.isEmpty();

            restrictions.put("canRequestLoan", canRequestLoan);
            restrictions.put("isRestricted", isRestricted);
            restrictions.put("hasUnpaidFines", !unpaidFines.isEmpty());
            restrictions.put("unpaidFinesCount", unpaidFines.size());
            restrictions.put("totalUnpaidAmount", totalUnpaidAmount);
            restrictions.put("overdueFinesCount", overdueFinesCount);
            restrictions.put("clientStatus", isRestricted ? "RESTRICTED" : "ACTIVE");

            if (isRestricted) {
                restrictions.put("restrictionReason",
                        "Cliente tiene " + unpaidFines.size() + " multa(s) impaga(s) por $" + totalUnpaidAmount);
            } else {
                restrictions.put("message", "Cliente no tiene restricciones de multas");
            }

            // Agregar detalles de multas impagas
            if (!unpaidFines.isEmpty()) {
                restrictions.put("unpaidFines", unpaidFines);
            }

        } catch (Exception e) {
            System.err.println("Error checking client restrictions: " + e.getMessage());
            // En caso de error, no restringir al cliente
            restrictions.put("canRequestLoan", true);
            restrictions.put("isRestricted", false);
            restrictions.put("error", "Error al verificar restricciones: " + e.getMessage());
            restrictions.put("clientStatus", "ERROR");
        }

        return restrictions;
    }

    // Obtener multas por cliente - VERSIÓN CORREGIDA
    public List<FineEntity> getFinesByClient(ClientEntity client) {
        try {
            if (client == null) {
                System.out.println("Client is null, returning empty list");
                return List.of();
            }

            System.out.println("Getting fines for client ID: " + client.getId());

            // Usar consulta directa del repositorio en lugar de filtrar en memoria
            List<FineEntity> fines = fineRepository.findByClient(client);
            System.out.println("Found " + fines.size() + " fines for client " + client.getId());

            return fines;
        } catch (Exception e) {
            System.err.println("Error getting fines by client: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    // Obtener multas impagas por cliente
    public List<FineEntity> getUnpaidFinesByClient(ClientEntity client) {
        try {
            if (client == null) {
                return List.of();
            }
            return fineRepository.findByClientAndPaidFalse(client);
        } catch (Exception e) {
            System.err.println("Error getting unpaid fines by client: " + e.getMessage());
            return List.of();
        }
    }

    // Obtener multas por préstamo
    public List<FineEntity> getFinesByLoan(LoanEntity loan) {
        try {
            if (loan == null) {
                return List.of();
            }
            return fineRepository.findByLoan(loan);
        } catch (Exception e) {
            System.err.println("Error getting fines by loan: " + e.getMessage());
            return List.of();
        }
    }

    // Pagar multa
    @Transactional
    public FineEntity payFine(Long fineId) {
        try {
            FineEntity fine = fineRepository.findById(fineId)
                    .orElseThrow(() -> new RuntimeException("Multa no encontrada con ID: " + fineId));

            if (fine.getPaid()) {
                throw new RuntimeException("La multa ya ha sido pagada");
            }

            fine.markAsPaid();
            FineEntity paidFine = fineRepository.save(fine);

            // 🔧 ACTUALIZACIÓN DEL ESTADO DE LA HERRAMIENTA SEGÚN TIPO DE DAÑO
            if (paidFine.getDamageType() != null && paidFine.getLoan() != null && paidFine.getLoan().getTool() != null) {
                try {
                    handleToolStatusAfterFinePayment(paidFine);
                } catch (Exception e) {
                    System.err.println("Error updating tool status after fine payment: " + e.getMessage());
                    // No fallar el pago de multa por esto
                }
            }

            // 🔧 ACTUALIZACIÓN DEL ESTADO DEL CLIENTE
            if (paidFine.getClient() != null) {
                try {
                    ClientEntity client = paidFine.getClient();

                    // Verificar si aún tiene otras multas impagas
                    List<FineEntity> unpaidFines = getUnpaidFinesByClient(client);

                    // Si no tiene más multas impagas, cambiar estado a ACTIVE
                    if (unpaidFines.isEmpty() && client.getStatus() == ClientEntity.ClientStatus.RESTRICTED) {
                        client.setStatus(ClientEntity.ClientStatus.ACTIVE);
                        clientRepository.save(client);
                        System.out.println("Client " + client.getName() + " status changed to ACTIVE - all fines paid");
                    } else if (!unpaidFines.isEmpty()) {
                        System.out.println("Client " + client.getName() + " still has " + unpaidFines.size() + " unpaid fine(s)");
                    }
                } catch (Exception e) {
                    System.err.println("Error updating client status after fine payment: " + e.getMessage());
                    // No fallar el pago de multa por esto
                }
            }

            return paidFine;
        } catch (Exception e) {
            System.err.println("Error paying fine: " + e.getMessage());
            throw new RuntimeException("Error al pagar la multa: " + e.getMessage());
        }
    }

    // Cancelar multa (solo admin)
    @Transactional
    public void cancelFine(Long fineId) {
        try {
            FineEntity fine = fineRepository.findById(fineId)
                    .orElseThrow(() -> new RuntimeException("Multa no encontrada con ID: " + fineId));

            fineRepository.delete(fine);
        } catch (Exception e) {
            System.err.println("Error cancelling fine: " + e.getMessage());
            throw new RuntimeException("Error al cancelar la multa: " + e.getMessage());
        }
    }

    // Crear multa
    @Transactional
    public FineEntity createFine(FineEntity fine) {
        try {
            if (fine.getCreatedAt() == null) {
                fine.setCreatedAt(LocalDateTime.now());
            }
            FineEntity savedFine = fineRepository.save(fine);

            // 🔧 NUEVO: Actualizar estado del cliente a RESTRICTED si tiene multas impagas
            if (savedFine.getClient() != null && !savedFine.getPaid()) {
                try {
                    ClientEntity client = savedFine.getClient();
                    if (client.getStatus() != ClientEntity.ClientStatus.RESTRICTED) {
                        client.setStatus(ClientEntity.ClientStatus.RESTRICTED);
                        clientRepository.save(client);
                        System.out.println("Client " + client.getName() + " status changed to RESTRICTED due to unpaid fine");
                    }
                } catch (Exception e) {
                    System.err.println("Error updating client status after fine creation: " + e.getMessage());
                    // No fallar la creación de multa por esto
                }
            }

            return savedFine;
        } catch (Exception e) {
            System.err.println("Error creating fine: " + e.getMessage());
            throw new RuntimeException("Error al crear la multa: " + e.getMessage());
        }
    }

    // Actualizar multa
    @Transactional
    public FineEntity updateFine(Long id, String description, LocalDate dueDate) {
        try {
            FineEntity fine = fineRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Multa no encontrada con ID: " + id));

            if (description != null) {
                fine.setDescription(description);
            }
            if (dueDate != null) {
                fine.setDueDate(dueDate);
            }

            return fineRepository.save(fine);
        } catch (Exception e) {
            System.err.println("Error updating fine: " + e.getMessage());
            throw new RuntimeException("Error al actualizar la multa: " + e.getMessage());
        }
    }

    // Eliminar multa
    @Transactional
    public void deleteFine(Long id) {
        try {
            FineEntity fine = fineRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Multa no encontrada con ID: " + id));

            if (fine.getPaid()) {
                throw new RuntimeException("No se puede eliminar una multa que ya ha sido pagada");
            }

            fineRepository.delete(fine);
        } catch (Exception e) {
            System.err.println("Error deleting fine: " + e.getMessage());
            throw new RuntimeException("Error al eliminar la multa: " + e.getMessage());
        }
    }

    // Obtener todas las multas - VERSIÓN MEJORADA PARA EVITAR ERROR 500
    @Transactional(readOnly = true)
    public List<FineEntity> getAllFines() {
        try {
            System.out.println("Attempting to get all fines from repository...");
            List<FineEntity> fines = fineRepository.findAll();
            System.out.println("Successfully retrieved " + fines.size() + " fines");
            return fines;
        } catch (Exception e) {
            System.err.println("Error getting all fines: " + e.getMessage());
            e.printStackTrace();
            return List.of(); // Retornar lista vacía en caso de error
        }
    }

    // Obtener multa por ID
    public FineEntity getFineById(Long id) {
        try {
            return fineRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Multa no encontrada con ID: " + id));
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener la multa: " + e.getMessage());
        }
    }

    // Obtener todas las multas impagas
    public List<FineEntity> getAllUnpaidFines() {
        try {
            return fineRepository.findByPaidFalse();
        } catch (Exception e) {
            System.err.println("Error getting all unpaid fines: " + e.getMessage());
            return List.of();
        }
    }

    // Obtener multas vencidas
    public List<FineEntity> getOverdueFines() {
        try {
            return fineRepository.findOverdueFines(LocalDate.now());
        } catch (Exception e) {
            System.err.println("Error getting overdue fines: " + e.getMessage());
            return List.of();
        }
    }

    // Obtener multas por tipo
    public List<FineEntity> getFinesByType(FineEntity.FineType type) {
        try {
            if (type == null) {
                return List.of();
            }
            return fineRepository.findByType(type);
        } catch (Exception e) {
            System.err.println("Error getting fines by type: " + e.getMessage());
            return List.of();
        }
    }

    // Obtener estadísticas de multas
    public Map<String, Object> getFineStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        try {
            long totalFines = fineRepository.count();
            long unpaidFines = fineRepository.countByPaidFalse();
            long paidFines = fineRepository.countByPaidTrue();
            long overdueFines = fineRepository.countOverdueFines(LocalDate.now());

            BigDecimal totalUnpaidAmount = fineRepository.getTotalUnpaidAmount();
            BigDecimal totalPaidAmount = fineRepository.getTotalPaidAmount();

            statistics.put("totalFines", totalFines);
            statistics.put("unpaidFines", unpaidFines);
            statistics.put("paidFines", paidFines);
            statistics.put("overdueFines", overdueFines);
            statistics.put("totalUnpaidAmount", totalUnpaidAmount != null ? totalUnpaidAmount : BigDecimal.ZERO);
            statistics.put("totalPaidAmount", totalPaidAmount != null ? totalPaidAmount : BigDecimal.ZERO);

        } catch (Exception e) {
            System.err.println("Error getting fine statistics: " + e.getMessage());
            statistics.put("error", "Error al obtener estadísticas de multas");
            statistics.put("totalFines", 0);
            statistics.put("unpaidFines", 0);
            statistics.put("paidFines", 0);
            statistics.put("overdueFines", 0);
            statistics.put("totalUnpaidAmount", BigDecimal.ZERO);
            statistics.put("totalPaidAmount", BigDecimal.ZERO);
        }

        return statistics;
    }

    // Obtener multas en rango de fechas
    public List<FineEntity> getFinesInDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            if (startDate == null || endDate == null) {
                return List.of();
            }
            return fineRepository.findByDateRange(startDate, endDate);
        } catch (Exception e) {
            System.err.println("Error getting fines in date range: " + e.getMessage());
            return List.of();
        }
    }

    // NUEVO: Manejar estado de herramienta después del pago de multa por daño
    private void handleToolStatusAfterFinePayment(FineEntity fine) {
        if (fine.getDamageType() == null || fine.getLoan() == null) {
            return;
        }

        ToolEntity tool = fine.getLoan().getTool();
        Integer quantity = fine.getLoan().getQuantity();

        System.out.println("Processing tool status after fine payment - Tool: " + tool.getName() +
                         ", Damage Type: " + fine.getDamageType());

        if (fine.getDamageType() == FineEntity.DamageType.MINOR) {
            // Daño leve: NO HACER NADA AUTOMÁTICAMENTE
            // El administrador debe marcar manualmente la herramienta como reparada desde el inventario
            // El pago de la multa NO cambia el estado de la herramienta
            System.out.println("Minor damage fine paid - Tool instances remain in their current state");
            System.out.println("Administrator must manually mark tool instances as repaired from inventory");

            // NO restaurar stock automáticamente
            // NO cambiar estado de instancias automáticamente
            // La herramienta puede estar ya reparada (AVAILABLE) o aún en reparación (UNDER_REPAIR)
            // dependiendo de si el admin ya la marcó como reparada


        } else if (fine.getDamageType() == FineEntity.DamageType.IRREPARABLE) {
            // Daño irreparable: Dar de baja (NO restaurar stock)
            System.out.println("Irreparable damage - decommissioning tool");

            // NO restaurar stock - la herramienta se pierde

            // Actualizar instancias individuales a DECOMMISSIONED
            if (toolInstanceService != null) {
                try {
                    List<ToolInstanceEntity> decommissionedInstances =
                        toolInstanceService.decommissionInstances(tool.getId(), quantity);
                    System.out.println("Successfully decommissioned " + decommissionedInstances.size() + " instances");
                } catch (Exception e) {
                    System.err.println("Error decommissioning tool instances: " + e.getMessage());
                }
            }

            // Verificar si todas las instancias están dadas de baja
            if (toolInstanceService != null) {
                try {
                    Long availableCount = toolInstanceService.getAvailableCount(tool.getId());
                    Long loanedCount = toolInstanceService.getInstancesByStatus(
                        ToolInstanceEntity.ToolInstanceStatus.LOANED).stream()
                        .filter(i -> i.getTool().getId().equals(tool.getId()))
                        .count();

                    if (availableCount == 0 && loanedCount == 0) {
                        // Todas las instancias están dadas de baja o en reparación
                        tool.setStatus(ToolEntity.ToolStatus.DECOMMISSIONED);
                        System.out.println("All instances decommissioned - tool marked as DECOMMISSIONED");
                    }
                } catch (Exception e) {
                    System.err.println("Error checking tool instance status: " + e.getMessage());
                }
            }
        }

        // Guardar cambios en la herramienta
        if (toolService != null) {
            try {
                toolService.updateTool(tool.getId(), tool);
                System.out.println("Tool status updated successfully: " + tool.getStatus() +
                                 " (Stock: " + tool.getCurrentStock() + "/" + tool.getInitialStock() + ")");
            } catch (Exception e) {
                System.err.println("Error saving tool changes: " + e.getMessage());
            }
        }
    }

    // Crear multa por atraso - método auxiliar para el LoanService
    public FineEntity createLateFine(LoanEntity loan, long daysLate, BigDecimal lateFeeRate) {
        try {
            if (loan == null || daysLate <= 0 || lateFeeRate == null) {
                throw new RuntimeException("Parámetros inválidos para crear multa por atraso");
            }

            BigDecimal fineAmount = lateFeeRate.multiply(BigDecimal.valueOf(daysLate));

            FineEntity fine = new FineEntity();
            fine.setClient(loan.getClient());
            fine.setLoan(loan);
            fine.setType(FineEntity.FineType.LATE_RETURN);
            fine.setAmount(fineAmount);
            fine.setDescription("Multa por devolución tardía - " + daysLate + " día(s) de atraso");
            fine.setDueDate(LocalDate.now().plusDays(30)); // 30 días para pagar
            fine.setPaid(false);

            return createFine(fine);
        } catch (Exception e) {
            System.err.println("Error creating late fine: " + e.getMessage());
            throw new RuntimeException("Error al crear multa por atraso: " + e.getMessage());
        }
    }

    // Crear multa por daño - método auxiliar para el LoanService
    public FineEntity createDamageFine(LoanEntity loan, BigDecimal repairCost, String description) {
        try {
            if (loan == null || repairCost == null || repairCost.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Parámetros inválidos para crear multa por daño");
            }

            FineEntity fine = new FineEntity();
            fine.setClient(loan.getClient());
            fine.setLoan(loan);
            fine.setType(FineEntity.FineType.DAMAGE_REPAIR);
            fine.setAmount(repairCost);
            fine.setDescription(description != null ? description : "Multa por daño a herramienta");
            fine.setDueDate(LocalDate.now().plusDays(30)); // 30 días para pagar
            fine.setPaid(false);

            return createFine(fine);
        } catch (Exception e) {
            System.err.println("Error creating damage fine: " + e.getMessage());
            throw new RuntimeException("Error al crear multa por daño: " + e.getMessage());
        }
    }

    // Crear multa por daño con tipo de daño especificado - NUEVO MÉTODO
    public FineEntity createDamageFineWithType(LoanEntity loan, FineEntity.DamageType damageType, String description) {
        try {
            if (loan == null || damageType == null) {
                throw new RuntimeException("Parámetros inválidos para crear multa por daño");
            }

            ToolEntity tool = loan.getTool();
            if (tool == null || tool.getReplacementValue() == null) {
                throw new RuntimeException("Herramienta o valor de reposición no encontrado");
            }

            BigDecimal fineAmount;
            FineEntity.FineType fineType;
            String fineDescription;

            if (damageType == FineEntity.DamageType.MINOR) {
                // Daño leve: usar tarifa de reparación del sistema (% del valor de reposición)
                BigDecimal repairRate = rateService.getCurrentRepairRate(); // Retorna el porcentaje (ej: 30.0)
                fineAmount = tool.getReplacementValue().multiply(repairRate.divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP));
                fineType = FineEntity.FineType.DAMAGE_REPAIR;
                fineDescription = "Multa por reparación (daño leve) - " + (description != null ? description : "");
            } else {
                // Daño irreparable: valor completo de reposición
                fineAmount = tool.getReplacementValue();
                fineType = FineEntity.FineType.TOOL_REPLACEMENT;
                fineDescription = "Multa por reposición (daño irreparable) - " + (description != null ? description : "");
            }

            FineEntity fine = new FineEntity();
            fine.setClient(loan.getClient());
            fine.setLoan(loan);
            fine.setType(fineType);
            fine.setDamageType(damageType);
            fine.setAmount(fineAmount);
            fine.setDescription(fineDescription.trim());
            fine.setDueDate(LocalDate.now().plusDays(30)); // 30 días para pagar
            fine.setPaid(false);

            System.out.println("Creating " + damageType + " damage fine: " + fineType + " - Amount: $" + fineAmount);

            return createFine(fine);
        } catch (Exception e) {
            System.err.println("Error creating damage fine with type: " + e.getMessage());
            throw new RuntimeException("Error al crear multa por daño: " + e.getMessage());
        }
    }
}