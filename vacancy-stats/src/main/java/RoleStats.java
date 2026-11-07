import java.util.List;

/** Ответ GET /roles/{id}/stats. Формат согласован с Алёной. */
public record RoleStats(
        Long roleId,
        int vacancyCount,
        Integer salaryMedian, // null, если ни в одной вакансии нет зарплаты
        String currency,
        List<SkillCount> skills
) {}
