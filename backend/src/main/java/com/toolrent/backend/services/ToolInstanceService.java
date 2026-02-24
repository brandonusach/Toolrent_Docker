package com.toolrent.backend.services;

import com.toolrent.backend.entities.ToolEntity;
import com.toolrent.backend.entities.ToolInstanceEntity;
import com.toolrent.backend.entities.ToolInstanceEntity.ToolInstanceStatus;
import com.toolrent.backend.repositories.ToolInstanceRepository;
import com.toolrent.backend.repositories.ToolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ToolInstanceService {

    private final ToolInstanceRepository toolInstanceRepository;
    private final ToolRepository toolRepository;

    @Autowired
    public ToolInstanceService(
            ToolInstanceRepository toolInstanceRepository,
            ToolRepository toolRepository) {
        this.toolInstanceRepository = toolInstanceRepository;
        this.toolRepository = toolRepository;
    }

    // Create multiple instances when a tool is created
    public List<ToolInstanceEntity> createInstances(ToolEntity tool, int quantity) {
        if (quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }

        List<ToolInstanceEntity> instances = new java.util.ArrayList<>();

        for (int i = 1; i <= quantity; i++) {
            ToolInstanceEntity instance = new ToolInstanceEntity(tool);
            instances.add(toolInstanceRepository.save(instance));
        }

        return instances;
    }

    // Get available count for a tool
    public Long getAvailableCount(Long toolId) {
        return toolInstanceRepository.countAvailableByToolId(toolId);
    }

    // Get first available instance of a tool
    public Optional<ToolInstanceEntity> getAvailableInstance(Long toolId) {
        return toolInstanceRepository.findFirstAvailableByToolId(toolId);
    }

    // Update instance status
    public ToolInstanceEntity updateInstanceStatus(Long instanceId, ToolInstanceStatus status) {
        ToolInstanceEntity instance = toolInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new RuntimeException("Tool instance not found with ID: " + instanceId));

        instance.setStatus(status);
        return toolInstanceRepository.save(instance);
    }

    // Reserve an instance for loan (change status from AVAILABLE to LOANED)
    public ToolInstanceEntity reserveInstanceForLoan(Long toolId) {
        Optional<ToolInstanceEntity> availableInstance = getAvailableInstance(toolId);

        if (availableInstance.isEmpty()) {
            throw new RuntimeException("No available instances for tool ID: " + toolId);
        }

        ToolInstanceEntity instance = availableInstance.get();
        instance.setStatus(ToolInstanceStatus.LOANED);

        return toolInstanceRepository.save(instance);
    }

    // Return an instance from loan (change status from LOANED to AVAILABLE or UNDER_REPAIR)
    public ToolInstanceEntity returnInstanceFromLoan(Long instanceId, boolean isDamaged) {
        ToolInstanceEntity instance = toolInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new RuntimeException("Tool instance not found with ID: " + instanceId));

        if (instance.getStatus() != ToolInstanceStatus.LOANED) {
            throw new RuntimeException("Instance is not currently loaned");
        }

        // Set status based on condition
        ToolInstanceStatus newStatus = isDamaged ? ToolInstanceStatus.UNDER_REPAIR : ToolInstanceStatus.AVAILABLE;
        instance.setStatus(newStatus);

        return toolInstanceRepository.save(instance);
    }

    // Decommission an instance (irreparable damage)
    public ToolInstanceEntity decommissionInstance(Long instanceId) {
        ToolInstanceEntity instance = toolInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new RuntimeException("Tool instance not found with ID: " + instanceId));

        instance.setStatus(ToolInstanceStatus.DECOMMISSIONED);
        return toolInstanceRepository.save(instance);
    }

    // Get all instances of a tool
    public List<ToolInstanceEntity> getInstancesByTool(Long toolId) {
        return toolInstanceRepository.findByToolIdOrderByStatus(toolId);
    }

    // Get instances by status
    public List<ToolInstanceEntity> getInstancesByStatus(ToolInstanceStatus status) {
        return toolInstanceRepository.findByStatus(status);
    }

    // Check if enough instances are available
    public boolean isAvailable(Long toolId, int quantity) {
        Long availableCount = getAvailableCount(toolId);
        return availableCount >= quantity;
    }

    // Get statistics for a tool
    public ToolInstanceStats getToolInstanceStats(Long toolId) {
        List<ToolInstanceEntity> instances = getInstancesByTool(toolId);

        long available = instances.stream().mapToLong(i -> i.isAvailable() ? 1 : 0).sum();
        long loaned = instances.stream().mapToLong(i -> i.isLoaned() ? 1 : 0).sum();
        long underRepair = instances.stream().mapToLong(i -> i.isUnderRepair() ? 1 : 0).sum();
        long decommissioned = instances.stream().mapToLong(i -> i.isDecommissioned() ? 1 : 0).sum();

        return new ToolInstanceStats(available, loaned, underRepair, decommissioned, instances.size());
    }

    // Reserve multiple instances for batch operations
    public List<ToolInstanceEntity> reserveMultipleInstances(Long toolId, int quantity) {
        if (quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }

        List<ToolInstanceEntity> availableInstances = toolInstanceRepository.findAvailableInstancesByToolId(toolId);

        if (availableInstances.size() < quantity) {
            throw new RuntimeException("Not enough available instances. Requested: " + quantity + ", Available: " + availableInstances.size());
        }

        List<ToolInstanceEntity> reservedInstances = new java.util.ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            ToolInstanceEntity instance = availableInstances.get(i);
            instance.setStatus(ToolInstanceStatus.LOANED);
            reservedInstances.add(toolInstanceRepository.save(instance));
        }

        return reservedInstances;
    }

    // Return multiple instances from loan
    public List<ToolInstanceEntity> returnMultipleInstances(List<Long> instanceIds, boolean isDamaged) {
        List<ToolInstanceEntity> returnedInstances = new java.util.ArrayList<>();

        for (Long instanceId : instanceIds) {
            returnedInstances.add(returnInstanceFromLoan(instanceId, isDamaged));
        }

        return returnedInstances;
    }

    // Get instances under repair
    public List<ToolInstanceEntity> getInstancesUnderRepair() {
        return toolInstanceRepository.findInstancesUnderRepair();
    }

    // Get loaned instances
    public List<ToolInstanceEntity> getLoanedInstances() {
        return toolInstanceRepository.findLoanedInstances();
    }

    // Repair instance (change status from UNDER_REPAIR to AVAILABLE)
    @Transactional
    public ToolInstanceEntity repairInstance(Long instanceId) {
        ToolInstanceEntity instance = toolInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new RuntimeException("Tool instance not found with ID: " + instanceId));

        if (instance.getStatus() != ToolInstanceStatus.UNDER_REPAIR) {
            throw new RuntimeException("Instance is not under repair");
        }

        instance.setStatus(ToolInstanceStatus.AVAILABLE);
        ToolInstanceEntity repairedInstance = toolInstanceRepository.save(instance);

        // Actualizar el stock de la herramienta y su estado
        try {
            ToolEntity tool = instance.getTool();
            if (tool != null) {
                // Incrementar el stock disponible
                tool.setCurrentStock(tool.getCurrentStock() + 1);

                // Actualizar el estado de la herramienta basado en instancias disponibles
                Long availableCount = getAvailableCount(tool.getId());
                if (availableCount > 0) {
                    tool.setStatus(ToolEntity.ToolStatus.AVAILABLE);
                    System.out.println("Tool " + tool.getName() + " status updated to AVAILABLE after repair (available instances: " + availableCount + ")");
                }

                // Guardar la herramienta actualizada
                toolRepository.save(tool);
                System.out.println("Tool " + tool.getName() + " stock updated: " + tool.getCurrentStock());
            }
        } catch (Exception e) {
            System.err.println("Error updating tool status after repair: " + e.getMessage());
            // No fallar la reparación por esto
        }

        return repairedInstance;
    }

    // Delete all instances of a tool (for cascade deletion)
    public void deleteAllInstancesByTool(Long toolId) {
        toolInstanceRepository.deleteByToolId(toolId);
    }

    // Delete individual instance
    public void deleteInstance(Long instanceId) {
        ToolInstanceEntity instance = toolInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new RuntimeException("Tool instance not found with ID: " + instanceId));

        toolInstanceRepository.delete(instance);
    }

    public ToolInstanceEntity getInstanceById(Long instanceId) {
        return toolInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new RuntimeException("Tool instance not found with ID: " + instanceId));
    }

    // Decommission multiple instances (for bulk operations)
    @Transactional
    public List<ToolInstanceEntity> decommissionMultipleInstances(Long toolId, int quantity) {
        if (quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }

        List<ToolInstanceEntity> availableInstances = toolInstanceRepository.findAvailableInstancesByToolId(toolId);

        if (availableInstances.size() < quantity) {
            throw new RuntimeException("Not enough available instances to decommission. Requested: " + quantity + ", Available: " + availableInstances.size());
        }

        List<ToolInstanceEntity> decommissionedInstances = new java.util.ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            ToolInstanceEntity instance = availableInstances.get(i);
            instance.setStatus(ToolInstanceStatus.DECOMMISSIONED);
            decommissionedInstances.add(toolInstanceRepository.save(instance));
        }

        return decommissionedInstances;
    }

    // Reserve multiple instances for loan
    @Transactional
    public List<ToolInstanceEntity> reserveInstancesForLoan(Long toolId, int quantity) {
        if (quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }

        List<ToolInstanceEntity> availableInstances = toolInstanceRepository.findAvailableInstancesByToolId(toolId);

        if (availableInstances.size() < quantity) {
            throw new RuntimeException("Not enough available instances for loan. Requested: " + quantity + ", Available: " + availableInstances.size());
        }

        List<ToolInstanceEntity> reservedInstances = new java.util.ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            ToolInstanceEntity instance = availableInstances.get(i);
            instance.setStatus(ToolInstanceStatus.LOANED);
            reservedInstances.add(toolInstanceRepository.save(instance));
        }

        return reservedInstances;
    }

    // Return multiple instances from loan
    @Transactional
    public List<ToolInstanceEntity> returnInstancesFromLoan(Long toolId, int quantity, boolean isDamaged) {
        if (quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }

        List<ToolInstanceEntity> loanedInstances = toolInstanceRepository.findLoanedInstancesByToolId(toolId);

        if (loanedInstances.size() < quantity) {
            throw new RuntimeException("Not enough loaned instances to return. Requested: " + quantity + ", Loaned: " + loanedInstances.size());
        }

        List<ToolInstanceEntity> returnedInstances = new java.util.ArrayList<>();
        ToolInstanceStatus newStatus = isDamaged ? ToolInstanceStatus.UNDER_REPAIR : ToolInstanceStatus.AVAILABLE;

        for (int i = 0; i < quantity; i++) {
            ToolInstanceEntity instance = loanedInstances.get(i);
            instance.setStatus(newStatus);
            returnedInstances.add(toolInstanceRepository.save(instance));
        }

        return returnedInstances;
    }

    //Reparar múltiples instancias (UNDER_REPAIR → AVAILABLE) - usado al pagar multa por daño leve
    @Transactional
    public List<ToolInstanceEntity> repairInstances(Long toolId, int quantity) {
        if (quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }

        List<ToolInstanceEntity> underRepairInstances = toolInstanceRepository.findByToolIdAndStatus(
            toolId, ToolInstanceStatus.UNDER_REPAIR);

        if (underRepairInstances.isEmpty()) {
            System.out.println("No instances under repair for tool ID: " + toolId);
            return List.of();
        }

        // Reparar hasta la cantidad especificada o todas las que estén en reparación
        int toRepair = Math.min(quantity, underRepairInstances.size());
        List<ToolInstanceEntity> repairedInstances = new java.util.ArrayList<>();

        for (int i = 0; i < toRepair; i++) {
            ToolInstanceEntity instance = underRepairInstances.get(i);
            instance.setStatus(ToolInstanceStatus.AVAILABLE);
            repairedInstances.add(toolInstanceRepository.save(instance));
            System.out.println("Instance ID " + instance.getId() + " repaired and marked as AVAILABLE");
        }

        return repairedInstances;
    }

    // Dar de baja múltiples instancias - usado al devolver con daño irreparable o al pagar multa
    @Transactional
    public List<ToolInstanceEntity> decommissionInstances(Long toolId, int quantity) {
        if (quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }

        // Primero buscar instancias prestadas (para devoluciones con daño irreparable)
        List<ToolInstanceEntity> loanedInstances = toolInstanceRepository.findByToolIdAndStatus(
            toolId, ToolInstanceStatus.LOANED);

        // Si no hay prestadas, buscar en reparación (para pago de multas)
        List<ToolInstanceEntity> underRepairInstances = toolInstanceRepository.findByToolIdAndStatus(
            toolId, ToolInstanceStatus.UNDER_REPAIR);

        // Combinar ambas listas (prioridad a prestadas)
        List<ToolInstanceEntity> instancesToDecommission = new java.util.ArrayList<>();
        instancesToDecommission.addAll(loanedInstances);
        instancesToDecommission.addAll(underRepairInstances);

        if (instancesToDecommission.isEmpty()) {
            System.out.println("No instances available to decommission for tool ID: " + toolId);
            return List.of();
        }

        // Dar de baja hasta la cantidad especificada
        int toDecommission = Math.min(quantity, instancesToDecommission.size());
        List<ToolInstanceEntity> decommissionedInstances = new java.util.ArrayList<>();

        for (int i = 0; i < toDecommission; i++) {
            ToolInstanceEntity instance = instancesToDecommission.get(i);
            ToolInstanceStatus previousStatus = instance.getStatus();
            instance.setStatus(ToolInstanceStatus.DECOMMISSIONED);
            decommissionedInstances.add(toolInstanceRepository.save(instance));
            System.out.println("Instance ID " + instance.getId() + " decommissioned (irreparable damage) - previous status: " + previousStatus);
        }

        return decommissionedInstances;
    }

    // Inner class for statistics
    public static class ToolInstanceStats {
        private final long available;
        private final long loaned;
        private final long underRepair;
        private final long decommissioned;
        private final long total;

        public ToolInstanceStats(long available, long loaned, long underRepair, long decommissioned, long total) {
            this.available = available;
            this.loaned = loaned;
            this.underRepair = underRepair;
            this.decommissioned = decommissioned;
            this.total = total;
        }

        // Getters
        public long getAvailable() { return available; }
        public long getLoaned() { return loaned; }
        public long getUnderRepair() { return underRepair; }
        public long getDecommissioned() { return decommissioned; }
        public long getTotal() { return total; }
    }
}