package com.toolrent.backend.services;

import com.toolrent.backend.entities.*;
import com.toolrent.backend.repositories.DamageRepository;
import com.toolrent.backend.repositories.FineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DamageService {

    @Autowired
    private DamageRepository damageRepository;

    @Autowired
    private FineRepository fineRepository;

    @Autowired
    private ToolInstanceService toolInstanceService;

    @Autowired
    @Lazy
    private KardexMovementService kardexMovementService;

    @Autowired
    @Lazy
    private ToolService toolService;

    // ========== MAIN DAMAGE MANAGEMENT METHODS ==========

    // Report initial damage when tool is returned
    @Transactional
    public DamageEntity reportDamage(LoanEntity loan, ToolInstanceEntity toolInstance,
                                     String description) {
        validateDamageReport(loan, toolInstance);

        DamageEntity damage = new DamageEntity(loan, toolInstance, description);

        // Update tool instance status to UNDER_REPAIR
        toolInstanceService.updateInstanceStatus(toolInstance.getId(),
                ToolInstanceEntity.ToolInstanceStatus.UNDER_REPAIR);

        return damageRepository.save(damage);
    }

    // Assess damage and determine repair cost and feasibility
    @Transactional
    public DamageEntity assessDamage(Long damageId, DamageEntity.DamageType damageType,
                                     String assessmentDescription, BigDecimal repairCost,
                                     Boolean isRepairable) throws Exception {

        DamageEntity damage = getDamageById(damageId);

        if (!damage.isPendingAssessment()) {
            throw new Exception("Damage has already been assessed");
        }

        // Update damage assessment
        damage.assessDamage(damageType, assessmentDescription, repairCost, isRepairable);

        // Handle different damage types
        if (!isRepairable || damageType == DamageEntity.DamageType.IRREPARABLE) {
            handleIrreparableDamage(damage);
        } else {
            // Create repair fine if repairable
            createRepairFine(damage);
        }

        return damageRepository.save(damage);
    }

    // Start repair process
    @Transactional
    public DamageEntity startRepair(Long damageId) throws Exception {
        DamageEntity damage = getDamageById(damageId);

        if (damage.getStatus() != DamageEntity.DamageStatus.ASSESSED) {
            throw new Exception("Damage must be assessed before starting repair");
        }

        if (!damage.getIsRepairable()) {
            throw new Exception("Cannot repair irreparable damage");
        }

        damage.markAsRepairInProgress();

        // Create kardex movement for repair start
        kardexMovementService.createRepairMovement(
                damage.getToolInstance().getTool(),
                "Repair started for damage #" + damage.getId(),
                damage.getToolInstance().getId()
        );

        return damageRepository.save(damage);
    }

    // Complete repair process
    @Transactional
    public DamageEntity completeRepair(Long damageId ) throws Exception {
        DamageEntity damage = getDamageById(damageId);

        if (!damage.isUnderRepair()) {
            throw new Exception("Damage is not under repair");
        }

        damage.markAsRepaired();

        // Update tool instance status back to AVAILABLE
        toolInstanceService.updateInstanceStatus(damage.getToolInstance().getId(),
                ToolInstanceEntity.ToolInstanceStatus.AVAILABLE);

        return damageRepository.save(damage);
    }

    // ========== QUERY METHODS ==========

    // Get damage by ID
    public DamageEntity getDamageById(Long id) throws Exception {
        return damageRepository.findById(id)
                .orElseThrow(() -> new Exception("Damage not found with ID: " + id));
    }

    // Get all damages
    public List<DamageEntity> getAllDamages() {
        return damageRepository.findAll();
    }

    // Get damages by loan
    public List<DamageEntity> getDamagesByLoan(LoanEntity loan) {
        return damageRepository.findByLoanOrderByReportedAtDesc(loan);
    }

    // Get damages by loan ID
    public List<DamageEntity> getDamagesByLoanId(Long loanId) {
        return damageRepository.findByLoanIdOrderByReportedAtDesc(loanId);
    }

    // Get damages by tool instance
    public List<DamageEntity> getDamagesByToolInstance(ToolInstanceEntity toolInstance) {
        return damageRepository.findByToolInstanceOrderByReportedAtDesc(toolInstance);
    }

    // Get damages by tool instance ID
    public List<DamageEntity> getDamagesByToolInstanceId(Long toolInstanceId) {
        return damageRepository.findByToolInstanceIdOrderByReportedAtDesc(toolInstanceId);
    }

    // Get damages by client
    public List<DamageEntity> getDamagesByClient(ClientEntity client) {
        return damageRepository.findByClientOrderByReportedAtDesc(client);
    }

    // Get damages by client ID
    public List<DamageEntity> getDamagesByClientId(Long clientId) {
        return damageRepository.findByClientIdOrderByReportedAtDesc(clientId);
    }

    // Get damages by tool ID
    public List<DamageEntity> getDamagesByToolId(Long toolId) {
        return damageRepository.findByToolIdOrderByReportedAtDesc(toolId);
    }

    // Get damages by status
    public List<DamageEntity> getDamagesByStatus(DamageEntity.DamageStatus status) {
        return damageRepository.findByStatusOrderByReportedAtDesc(status);
    }

    // Get damages by type
    public List<DamageEntity> getDamagesByType(DamageEntity.DamageType type) {
        return damageRepository.findByTypeOrderByReportedAtDesc(type);
    }

    // ========== REPORT AND DASHBOARD METHODS ==========

    // Get pending assessments (damages that need evaluation)
    public List<DamageEntity> getPendingAssessments() {
        return damageRepository.findPendingAssessments();
    }

    // Get damages under repair
    public List<DamageEntity> getDamagesUnderRepair() {
        return damageRepository.findDamagesUnderRepair();
    }

    // Get irreparable damages
    public List<DamageEntity> getIrreparableDamages() {
        return damageRepository.findIrreparableDamages();
    }

    // Get damages by date range
    public List<DamageEntity> getDamagesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return damageRepository.findByDateRange(startDate, endDate);
    }

    // Get urgent damages (requiring immediate attention)
    public List<DamageEntity> getUrgentDamages() {
        LocalDateTime urgentCutoff = LocalDateTime.now().minusDays(3); // 3 days ago
        return damageRepository.findUrgentDamages(urgentCutoff);
    }

    // Get stagnant assessments (assessed but no progress)
    public List<DamageEntity> getStagnantAssessments() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7); // 1 week ago
        return damageRepository.findStagnantAssessments(cutoff);
    }

    // Get overdue repairs
    public List<DamageEntity> getOverdueRepairs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(14); // 2 weeks ago
        return damageRepository.findOverdueRepairs(cutoff);
    }

    // ========== STATISTICS METHODS ==========

    // Get damage statistics by tool
    public List<Object[]> getDamageStatsByTool() {
        return damageRepository.getDamageStatsByTool();
    }

    // Get damage statistics by client
    public List<Object[]> getDamageStatsByClient() {
        return damageRepository.getDamageStatsByClient();
    }

    // Get monthly damage trend
    public List<Object[]> getMonthlyDamageTrend() {
        return damageRepository.getMonthlyDamageTrend();
    }

    // Calculate total repair cost for period
    public BigDecimal calculateTotalRepairCostInPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        return damageRepository.calculateTotalRepairCostInPeriod(startDate, endDate);
    }

    // Count damages by status
    public long countDamagesByStatus(DamageEntity.DamageStatus status) {
        return damageRepository.countByStatus(status);
    }

    // Count damages by client
    public long countDamagesByClient(Long clientId) {
        return damageRepository.countByClientId(clientId);
    }

    // Count irreparable damages by client
    public long countIrreparableDamagesByClient(Long clientId) {
        return damageRepository.countIrreparableDamagesByClientId(clientId);
    }

    // ========== BUSINESS LOGIC METHODS ==========

    // Check if tool instance has pending damages
    public boolean hasPendingDamages(Long toolInstanceId) {
        return damageRepository.hasPendingDamages(toolInstanceId);
    }

    // Check if loan has damages
    public boolean loanHasDamages(Long loanId) {
        return damageRepository.existsByLoanId(loanId);
    }

    // Get active damages for tool instance
    public List<DamageEntity> getActiveDamagesByToolInstance(Long toolInstanceId) {
        return damageRepository.findActiveDamagesByToolInstanceId(toolInstanceId);
    }

    // Get most recent damage for tool instance
    public Optional<DamageEntity> getMostRecentDamageByToolInstance(Long toolInstanceId) {
        return damageRepository.findMostRecentByToolInstanceId(toolInstanceId);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private void validateDamageReport(LoanEntity loan, ToolInstanceEntity toolInstance) {
        if (loan == null) {
            throw new RuntimeException("Loan is required for damage report");
        }
        if (toolInstance == null) {
            throw new RuntimeException("Tool instance is required for damage report");
        }

        // Verify tool instance belongs to the loan's tool
        if (!toolInstance.getTool().getId().equals(loan.getTool().getId())) {
            throw new RuntimeException("Tool instance does not match loan tool");
        }
    }

    private void handleIrreparableDamage(DamageEntity damage) throws Exception {
        // Mark damage as irreparable
        damage.markAsIrreparable();

        // Decommission the tool instance
        toolInstanceService.decommissionInstance(damage.getToolInstance().getId());

        // Capturar stock antes de actualizar
        ToolEntity tool = damage.getToolInstance().getTool();
        int stockBefore = tool.getCurrentStock();

        // Create kardex movement for decommission
        kardexMovementService.createDecommissionMovement(
                tool,
                1,
                "Tool instance decommissioned due to irreparable damage #" + damage.getId(),
                List.of(damage.getToolInstance().getId()),
                stockBefore
        );

        // Update tool stock
        toolService.deleteToolInstanceAndUpdateStock(damage.getToolInstance().getId());

        // Create replacement fine
        createReplacementFine(damage);
    }

    private void createRepairFine(DamageEntity damage) {
        if (damage.getRepairCost() != null && damage.getRepairCost().compareTo(BigDecimal.ZERO) > 0) {
            FineEntity fine = new FineEntity();
            fine.setClient(damage.getLoan().getClient());
            fine.setLoan(damage.getLoan());
            fine.setType(FineEntity.FineType.DAMAGE_REPAIR);
            fine.setAmount(damage.getRepairCost());
            fine.setDescription("Repair cost for damage to " + damage.getToolInstance().getTool().getName());
            fine.setPaid(false);
            fine.setDueDate(java.time.LocalDate.now().plusDays(30));

            fineRepository.save(fine);
        }
    }

    private void createReplacementFine(DamageEntity damage) {
        BigDecimal replacementValue = damage.getToolInstance().getTool().getReplacementValue();

        FineEntity fine = new FineEntity();
        fine.setClient(damage.getLoan().getClient());
        fine.setLoan(damage.getLoan());
        fine.setType(FineEntity.FineType.TOOL_REPLACEMENT);
        fine.setAmount(replacementValue);
        fine.setDescription("Replacement cost for irreparable damage to " +
                damage.getToolInstance().getTool().getName());
        fine.setPaid(false);
        fine.setDueDate(java.time.LocalDate.now().plusDays(30));


        fineRepository.save(fine);
    }

    // ========== DASHBOARD SUMMARY METHOD ==========

    public DamageDashboardSummary getDamageDashboardSummary() {
        long pendingCount = countDamagesByStatus(DamageEntity.DamageStatus.REPORTED);
        long underRepairCount = countDamagesByStatus(DamageEntity.DamageStatus.REPAIR_IN_PROGRESS);
        long irreparableCount = damageRepository.findIrreparableDamages().size();

        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime monthEnd = LocalDateTime.now();
        BigDecimal monthlyRepairCost = calculateTotalRepairCostInPeriod(monthStart, monthEnd);

        return new DamageDashboardSummary(pendingCount, underRepairCount, irreparableCount, monthlyRepairCost);
    }

    // ========== HELPER CLASSES ==========

    public static class DamageDashboardSummary {
        public final long pendingAssessments;
        public final long underRepair;
        public final long irreparable;
        public final BigDecimal monthlyRepairCost;

        public DamageDashboardSummary(long pendingAssessments, long underRepair,
                                      long irreparable, BigDecimal monthlyRepairCost) {
            this.pendingAssessments = pendingAssessments;
            this.underRepair = underRepair;
            this.irreparable = irreparable;
            this.monthlyRepairCost = monthlyRepairCost != null ? monthlyRepairCost : BigDecimal.ZERO;
        }
    }
}