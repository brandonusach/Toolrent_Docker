package com.toolrent.backend.controllers;

import com.toolrent.backend.entities.ToolEntity;
import com.toolrent.backend.services.ToolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tools")
@CrossOrigin("*")
public class ToolController {
    @Autowired
    ToolService toolService;

    // Quitar autenticación temporalmente para desarrollo
    @GetMapping("/")
    public ResponseEntity<List<ToolEntity>> listTools() {
        List<ToolEntity> tools = toolService.getAllTools();
        return ResponseEntity.ok(tools);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ToolEntity> getToolById(@PathVariable Long id) {
        // Adaptar a tu service method actual
        ToolEntity tool = toolService.getToolById(id).orElse(null);
        if (tool != null) {
            return ResponseEntity.ok(tool);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/")
    public ResponseEntity<ToolEntity> saveTool(@RequestBody ToolEntity tool) {
        ToolEntity toolNew = toolService.createTool(tool);
        return ResponseEntity.ok(toolNew);
    }

    // Patrón del profesor - PUT sin ID en la URL, enviando objeto completo con ID
    @PutMapping("/")
    public ResponseEntity<ToolEntity> updateTool(@RequestBody ToolEntity tool){
        ToolEntity toolUpdated = toolService.updateTool(tool.getId(), tool);
        return ResponseEntity.ok(toolUpdated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteToolById(@PathVariable Long id) throws Exception {
        toolService.deleteTool(id);
        return ResponseEntity.noContent().build();
    }

    // Endpoints específicos de tu negocio
    @PostMapping("/{id}/add-stock")
    public ResponseEntity<ToolEntity> addStock(@PathVariable Long id, @RequestParam Integer quantity) {
        ToolEntity updatedTool = toolService.addToolStock(id, quantity);
        return ResponseEntity.ok(updatedTool);
    }

    @PutMapping("/{id}/decommission")
    public ResponseEntity<ToolEntity> decommissionTool(@PathVariable Long id, @RequestParam Long instanceId) {
        ToolEntity updatedTool = toolService.decommissionToolInstance(id, instanceId);
        return ResponseEntity.ok(updatedTool);
    }
}