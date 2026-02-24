package com.toolrent.backend.services;

import com.toolrent.backend.entities.ToolEntity;
import com.toolrent.backend.entities.CategoryEntity;
import com.toolrent.backend.entities.ToolInstanceEntity;
import com.toolrent.backend.repositories.ToolRepository;
import com.toolrent.backend.repositories.CategoryRepository;
import com.toolrent.backend.repositories.ToolInstanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ToolService {

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ToolInstanceRepository toolInstanceRepository;

    @Autowired
    @Lazy
    private KardexMovementService kardexMovementService;

    // Constantes de validación
    private static final BigDecimal MIN_REPLACEMENT_VALUE = new BigDecimal("1000");
    private static final BigDecimal MAX_REPLACEMENT_VALUE = new BigDecimal("1000000");
    private static final BigDecimal MIN_RENTAL_RATE = new BigDecimal("1");
    private static final BigDecimal MAX_RENTAL_RATE = new BigDecimal("10000");
    private static final int MIN_STOCK = 1;
    private static final int MAX_STOCK = 999;
    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 100;

    // Get all tools WITH categories
    public List<ToolEntity> getAllTools() {
        return toolRepository.findAllWithCategories();
    }

    // Get tool by ID WITH category
    public Optional<ToolEntity> getToolById(Long id) {
        return toolRepository.findByIdWithCategory(id);
    }

    // Validar datos de herramienta
    private void validateToolData(ToolEntity tool) {
        // Validar nombre
        if (tool.getName() == null || tool.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la herramienta es requerido");
        }

        String trimmedName = tool.getName().trim();
        if (trimmedName.length() < MIN_NAME_LENGTH || trimmedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("El nombre debe tener entre %d y %d caracteres", MIN_NAME_LENGTH, MAX_NAME_LENGTH)
            );
        }

        // Validar categoría
        if (tool.getCategory() == null || tool.getCategory().getId() == null) {
            throw new IllegalArgumentException("La categoría es requerida");
        }

        // Validar stock inicial
        if (tool.getInitialStock() == null) {
            throw new IllegalArgumentException("El stock inicial es requerido");
        }
        if (tool.getInitialStock() < MIN_STOCK || tool.getInitialStock() > MAX_STOCK) {
            throw new IllegalArgumentException(
                    String.format("El stock inicial debe estar entre %d y %d", MIN_STOCK, MAX_STOCK)
            );
        }

        // Validar valor de reposición
        if (tool.getReplacementValue() == null) {
            throw new IllegalArgumentException("El valor de reposición es requerido");
        }
        if (tool.getReplacementValue().compareTo(MIN_REPLACEMENT_VALUE) < 0) {
            throw new IllegalArgumentException(
                    String.format("El valor de reposición debe ser al menos $%s",
                            MIN_REPLACEMENT_VALUE.toPlainString())
            );
        }
        if (tool.getReplacementValue().compareTo(MAX_REPLACEMENT_VALUE) > 0) {
            throw new IllegalArgumentException(
                    String.format("El valor de reposición no puede exceder $%s",
                            MAX_REPLACEMENT_VALUE.toPlainString())
            );
        }
    }

    // Create new tool
    @Transactional
    public ToolEntity createTool(ToolEntity tool) {
        // Validar datos
        validateToolData(tool);

        // Limpiar nombre
        tool.setName(tool.getName().trim());

        // Load full category from database
        CategoryEntity category = categoryRepository.findById(tool.getCategory().getId())
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

        tool.setCategory(category);

        // Validar duplicados: mismo nombre y categoría
        Optional<ToolEntity> existingTool = toolRepository.findByNameIgnoreCaseAndCategory(tool.getName(), category);
        if (existingTool.isPresent()) {
            throw new IllegalArgumentException(
                    String.format("Ya existe una herramienta con el nombre '%s' en la categoría '%s'",
                            tool.getName(), category.getName())
            );
        }

        // Set initial values
        tool.setCurrentStock(tool.getInitialStock());
        tool.setStatus(ToolEntity.ToolStatus.AVAILABLE);

        // Save tool
        ToolEntity savedTool = toolRepository.save(tool);

        // Create tool instances
        for (int i = 0; i < savedTool.getInitialStock(); i++) {
            ToolInstanceEntity instance = new ToolInstanceEntity();
            instance.setTool(savedTool);
            instance.setStatus(ToolInstanceEntity.ToolInstanceStatus.AVAILABLE);
            toolInstanceRepository.save(instance);
        }

        // Registrar movimiento inicial en el kardex
        kardexMovementService.createInitialStockMovement(
                savedTool,
                savedTool.getInitialStock()
        );

        // Reload tool with category to return complete object
        return toolRepository.findByIdWithCategory(savedTool.getId())
                .orElse(savedTool);
    }

    // Update tool
    @Transactional
    public ToolEntity updateTool(Long id, ToolEntity toolDetails) {
        ToolEntity tool = toolRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new IllegalArgumentException("Herramienta no encontrada con ID: " + id));

        // Validar nombre
        if (toolDetails.getName() == null || toolDetails.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la herramienta es requerido");
        }

        String trimmedName = toolDetails.getName().trim();
        if (trimmedName.length() < MIN_NAME_LENGTH || trimmedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("El nombre debe tener entre %d y %d caracteres", MIN_NAME_LENGTH, MAX_NAME_LENGTH)
            );
        }

        // Validar valor de reposición
        if (toolDetails.getReplacementValue() == null) {
            throw new IllegalArgumentException("El valor de reposición es requerido");
        }
        if (toolDetails.getReplacementValue().compareTo(MIN_REPLACEMENT_VALUE) < 0) {
            throw new IllegalArgumentException(
                    String.format("El valor de reposición debe ser al menos $%s",
                            MIN_REPLACEMENT_VALUE.toPlainString())
            );
        }
        if (toolDetails.getReplacementValue().compareTo(MAX_REPLACEMENT_VALUE) > 0) {
            throw new IllegalArgumentException(
                    String.format("El valor de reposición no puede exceder $%s",
                            MAX_REPLACEMENT_VALUE.toPlainString())
            );
        }

        // Validar tarifa de arriendo
        if (toolDetails.getRentalRate() == null) {
            throw new IllegalArgumentException("La tarifa de arriendo es requerida");
        }
        if (toolDetails.getRentalRate().compareTo(MIN_RENTAL_RATE) < 0) {
            throw new IllegalArgumentException(
                    String.format("La tarifa de arriendo debe ser al menos $%s",
                            MIN_RENTAL_RATE.toPlainString())
            );
        }
        if (toolDetails.getRentalRate().compareTo(MAX_RENTAL_RATE) > 0) {
            throw new IllegalArgumentException(
                    String.format("La tarifa de arriendo no puede exceder $%s",
                            MAX_RENTAL_RATE.toPlainString())
            );
        }

        // Update basic fields
        tool.setName(trimmedName);
        tool.setReplacementValue(toolDetails.getReplacementValue());
        tool.setRentalRate(toolDetails.getRentalRate());

        // Update category if changed
        if (toolDetails.getCategory() != null && toolDetails.getCategory().getId() != null) {
            CategoryEntity category = categoryRepository.findById(toolDetails.getCategory().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
            tool.setCategory(category);
        }

        // Validar duplicados: mismo nombre y categoría (excepto esta misma herramienta)
        Optional<ToolEntity> existingTool = toolRepository.findByNameIgnoreCaseAndCategory(trimmedName, tool.getCategory());
        if (existingTool.isPresent() && !existingTool.get().getId().equals(id)) {
            throw new IllegalArgumentException(
                    String.format("Ya existe otra herramienta con el nombre '%s' en la categoría '%s'",
                            trimmedName, tool.getCategory().getName())
            );
        }

        // Save
        ToolEntity savedTool = toolRepository.save(tool);

        // Reload tool with category to return complete object
        return toolRepository.findByIdWithCategory(savedTool.getId())
                .orElse(savedTool);
    }

    // Delete tool
    @Transactional
    public void deleteTool(Long id) {
        ToolEntity tool = toolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Herramienta no encontrada con ID: " + id));

        // Delete all instances first
        toolInstanceRepository.deleteByToolId(id);

        // Delete tool
        toolRepository.delete(tool);
    }

    // Add stock
    @Transactional
    public ToolEntity addToolStock(Long toolId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }

        if (quantity > MAX_STOCK) {
            throw new IllegalArgumentException(
                    String.format("La cantidad no puede exceder %d unidades", MAX_STOCK)
            );
        }

        ToolEntity tool = toolRepository.findByIdWithCategory(toolId)
                .orElseThrow(() -> new IllegalArgumentException("Herramienta no encontrada con ID: " + toolId));

        // IMPORTANTE: Capturar el stock ANTES de agregar para el registro del kardex
        int stockBeforeAdding = tool.getCurrentStock();

        // Validar que no exceda el máximo de stock
        int newInitialStock = tool.getInitialStock() + quantity;
        int newCurrentStock = tool.getCurrentStock() + quantity;

        if (newInitialStock > MAX_STOCK) {
            throw new IllegalArgumentException(
                    String.format("El stock total no puede exceder %d unidades. Stock actual: %d",
                            MAX_STOCK, tool.getInitialStock())
            );
        }

        // Update stocks
        tool.setInitialStock(newInitialStock);
        tool.setCurrentStock(newCurrentStock);

        // Actualizar estado a AVAILABLE cuando se agrega stock
        // Si la herramienta estaba en LOANED o DECOMMISSIONED, ahora tiene stock disponible
        if (newCurrentStock > 0 && (tool.getStatus() == ToolEntity.ToolStatus.LOANED ||
                                     tool.getStatus() == ToolEntity.ToolStatus.DECOMMISSIONED)) {
            tool.setStatus(ToolEntity.ToolStatus.AVAILABLE);
            System.out.println("Tool " + tool.getName() + " status changed to AVAILABLE after adding stock (new stock: " + newCurrentStock + ")");
        }

        // Create new instances
        for (int i = 0; i < quantity; i++) {
            ToolInstanceEntity instance = new ToolInstanceEntity();
            instance.setTool(tool);
            instance.setStatus(ToolInstanceEntity.ToolInstanceStatus.AVAILABLE);
            toolInstanceRepository.save(instance);
        }

        ToolEntity savedTool = toolRepository.save(tool);

        // Create Kardex movement for restock - pasando el stock ANTES de agregar
        try {
            String description = String.format("Reposición de stock desde gestión de inventario - %d unidad(es)", quantity);
            kardexMovementService.createRestockMovement(savedTool, quantity, description, stockBeforeAdding);
        } catch (Exception e) {
            System.err.println("Error creando movimiento Kardex para reposición: " + e.getMessage());
            // No lanzar excepción para no afectar el proceso principal
        }

        // Reload tool with category to return complete object
        return toolRepository.findByIdWithCategory(savedTool.getId())
                .orElse(savedTool);
    }

    // Decommission specific tool instance
    @Transactional
    public ToolEntity decommissionToolInstance(Long toolId, Long instanceId) {
        if (instanceId == null) {
            throw new IllegalArgumentException("El ID de la instancia es requerido");
        }

        ToolEntity tool = toolRepository.findByIdWithCategory(toolId)
                .orElseThrow(() -> new IllegalArgumentException("Herramienta no encontrada con ID: " + toolId));

        // Find the specific instance
        ToolInstanceEntity instance = toolInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Instancia no encontrada con ID: " + instanceId));

        // Verify the instance belongs to this tool
        if (!instance.getTool().getId().equals(toolId)) {
            throw new IllegalArgumentException("La instancia no pertenece a esta herramienta");
        }

        // Verify the instance is available
        if (instance.getStatus() != ToolInstanceEntity.ToolInstanceStatus.AVAILABLE) {
            throw new IllegalArgumentException(
                    String.format("La instancia #%d no está disponible para dar de baja. Estado actual: %s",
                            instanceId, instance.getStatus())
            );
        }

        // IMPORTANTE: Capturar el stock ANTES de dar de baja para el registro del kardex
        int stockBeforeDecommission = tool.getCurrentStock();

        // Decommission the specific instance
        instance.setStatus(ToolInstanceEntity.ToolInstanceStatus.DECOMMISSIONED);
        toolInstanceRepository.save(instance);

        // Update current stock
        tool.setCurrentStock(tool.getCurrentStock() - 1);

        ToolEntity savedTool = toolRepository.save(tool);

        // Create Kardex movement for decommission - con stock correcto
        List<Long> instanceIds = new java.util.ArrayList<>();
        instanceIds.add(instanceId);
        String description = String.format("Baja de herramienta desde gestión de inventario - 1 unidad(es) - Instancias dadas de baja: [%d]", instanceId);

        System.out.println("=== DEBUG: Registrando movimiento DECOMMISSION en kardex para herramienta ID " + savedTool.getId() + " ===");
        System.out.println("=== DEBUG: Stock ANTES: " + stockBeforeDecommission + ", Stock DESPUÉS: " + savedTool.getCurrentStock() + " ===");

        kardexMovementService.createDecommissionMovement(savedTool, 1, description, instanceIds, stockBeforeDecommission);

        System.out.println("=== DEBUG: Movimiento DECOMMISSION registrado exitosamente ===");

        // Reload tool with category to return complete object
        return toolRepository.findByIdWithCategory(savedTool.getId())
                .orElse(savedTool);
    }

    // Decommission tool (legacy method - kept for compatibility)
    @Transactional
    public ToolEntity decommissionTool(Long toolId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }

        ToolEntity tool = toolRepository.findByIdWithCategory(toolId)
                .orElseThrow(() -> new IllegalArgumentException("Herramienta no encontrada con ID: " + toolId));

        // IMPORTANTE: Capturar el stock ANTES de dar de baja
        int stockBeforeDecommission = tool.getCurrentStock();

        // Get available instances
        List<ToolInstanceEntity> availableInstances = toolInstanceRepository
                .findByToolAndStatus(tool, ToolInstanceEntity.ToolInstanceStatus.AVAILABLE);

        if (availableInstances.size() < quantity) {
            throw new IllegalArgumentException(
                    String.format("No hay suficientes instancias disponibles para dar de baja. Disponibles: %d, Solicitadas: %d",
                            availableInstances.size(), quantity)
            );
        }

        // Collect instance IDs to decommission
        List<Long> instanceIds = new java.util.ArrayList<>();

        // Decommission instances
        for (int i = 0; i < quantity; i++) {
            ToolInstanceEntity instance = availableInstances.get(i);
            instanceIds.add(instance.getId());
            instance.setStatus(ToolInstanceEntity.ToolInstanceStatus.DECOMMISSIONED);
            toolInstanceRepository.save(instance);
        }

        // Update current stock
        tool.setCurrentStock(tool.getCurrentStock() - quantity);

        ToolEntity savedTool = toolRepository.save(tool);

        // Create Kardex movement for decommission - MEJORADO con stock correcto
        String description = String.format("Baja de herramienta desde gestión de inventario - %d unidad(es)", quantity);
        System.out.println("=== DEBUG: Registrando movimiento DECOMMISSION en kardex para herramienta ID " + savedTool.getId() + " ===");
        kardexMovementService.createDecommissionMovement(savedTool, quantity, description, instanceIds, stockBeforeDecommission);
        System.out.println("=== DEBUG: Movimiento DECOMMISSION registrado exitosamente ===");

        // Reload tool with category to return complete object
        return toolRepository.findByIdWithCategory(savedTool.getId())
                .orElse(savedTool);
    }

    // Delete tool instance and update stock
    @Transactional
    public void deleteToolInstanceAndUpdateStock(Long instanceId) {
        ToolInstanceEntity instance = toolInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Instancia de herramienta no encontrada con ID: " + instanceId));

        ToolEntity tool = instance.getTool();

        // Only decrease stock if instance was AVAILABLE
        if (instance.getStatus() == ToolInstanceEntity.ToolInstanceStatus.AVAILABLE) {
            tool.setCurrentStock(tool.getCurrentStock() - 1);
            tool.setInitialStock(tool.getInitialStock() - 1);
            toolRepository.save(tool);
        }

        // Delete instance
        toolInstanceRepository.delete(instance);
    }

    // Search tools by name
    public List<ToolEntity> searchToolsByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El término de búsqueda no puede estar vacío");
        }
        return toolRepository.findByNameContainingIgnoreCase(name.trim());
    }

    // Get tools by category
    public List<ToolEntity> getToolsByCategory(Long categoryId) {
        CategoryEntity category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
        return toolRepository.findByCategory(category);
    }

    // Get available tools
    public List<ToolEntity> getAvailableTools() {
        return toolRepository.findAvailableTools();
    }

    // Get low stock tools
    public List<ToolEntity> getLowStockTools(Integer threshold) {
        if (threshold == null || threshold < 0) {
            throw new IllegalArgumentException("El umbral debe ser un número positivo");
        }
        return toolRepository.findLowStockTools(threshold);
    }
}